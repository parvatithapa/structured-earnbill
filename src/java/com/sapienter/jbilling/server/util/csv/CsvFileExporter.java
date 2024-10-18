package com.sapienter.jbilling.server.util.csv;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.csv.export.event.ReportExportNotificationEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.converter.BigDecimalConverter;
import com.sapienter.jbilling.server.util.converter.TimestampConverter;

/**
 * CsvFileExporter
 *
 * @author Ashok Kale
 * @since 02/11/14
 */
public class CsvFileExporter<T extends Exportable> implements Exporter<T> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int THREAD_POOL_SIZE = 4;
    private static final String TEMPORARY_FILE_NAME = "export-data-part";

    private Class<T> type;
    private Integer entityId;
    private String csvType;
    private Integer exporterUserId;
    private ExecutorService service;

     static {
            ConvertUtils.register(new BigDecimalConverter(), BigDecimal.class);
            ConvertUtils.register(new TimestampConverter(), Timestamp.class);
     }

    private CsvFileExporter(Class<T> type, Integer entityId, String csvType, Integer exporterUserId) {
        this.type = type;
        this.csvType = csvType;
        this.entityId = entityId;
        this.exporterUserId=exporterUserId;
        this.service = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public static <T extends Exportable > CsvFileExporter<T> createExporter(Class<T> type, Integer entityId, String csvType, Integer exporterUserId) {
        return new CsvFileExporter<>(type, entityId, csvType,exporterUserId);
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    private String doWriteDataInFile(String fileName, List<T> list, FileWriter out) {
        try(final CSVWriter writer = new CSVWriter(out)) {
            IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
            String[] header = txAction.execute(()-> list.get(0).getFieldNames());
            writer.writeNext(header);
            int size = (list.size()-1) - 1;
            int targetSize = size / THREAD_POOL_SIZE + 1;
            List<List<T>> subLists = partition(list, targetSize);
            List<Future<ThreadResult>> futures = new ArrayList<>();
            int counter = 1;
            for(final List<T> subList: subLists) {
                Future<ThreadResult> future = executeCsvExportInNewThread(subList, counter++);
                futures.add(future);
            }

            // Waiting to execute all thread
            int totalRecordSkipped = 0;
            for(Future<ThreadResult> future: futures) {
                //Retrieve temporary file name and skipped records from ThreadResult
                ThreadResult tr = future.get();
                totalRecordSkipped = totalRecordSkipped + tr.getNoOfRowsSkipped();

                //Read & append temporary files data into main csv file
                CSVReader csvReader = new CSVReader(new FileReader(tr.getTmpCsvFileName()));
                writer.writeAll(csvReader.readAll());
                csvReader.close();

                //Removing temporary files
                boolean isTempFileDeleted = new File(tr.getTmpCsvFileName()).delete();
                if (!isTempFileDeleted) {
                    logger.debug("Temporary CSV file [{}] deletion is failed.", tr.getTmpCsvFileName());
                }
            }

            if(totalRecordSkipped > 0) {
                logger.debug("Count Of Records got Skipped during CSV Export [{}] ", totalRecordSkipped);
            }
            File file= new File(createFile(csvType, entityId, false));
            FileUtils.moveFile(new File(fileName), file);

            new Thread(()-> txAction.execute(()-> {
                try {
                    if(PreferenceBL.getPreferenceValueAsIntegerOrZero(entityId, Constants.PREFERENCE_BACKGROUND_CSV_EXPORT) != Integer.valueOf(0)) {
                        EventManager.process(new ReportExportNotificationEvent(this.entityId , this.exporterUserId,
                                file.getName(), ReportExportNotificationEvent.NotificationStatus.PASSED));
                    }
                } catch(Exception ex) {
                    logger.error("Notifcation failed!", ex);
                }
            })).start();

            return fileName;
        } catch(Exception ex) {
            logger.error("Exception occurred during CSV export process for entity Id {} and CSV type csvType {}", entityId, csvType);
            return StringUtils.EMPTY;
        } finally {
            logger.debug("Shutting Down Thread Pool Service");
            service.shutdown();
        }
    }

    @Override
    public String export(List<T> list) {
        if(CollectionUtils.isNotEmpty(list)) {
            String fileName = createFile(csvType, entityId, true);
            try (FileWriter out = new FileWriter(fileName, true)) {
                return doWriteDataInFile(fileName, list, out);
            } catch (Exception e) {
                logger.error("CSV File Export Failed!", e);
                return StringUtils.EMPTY;
            }
        }
        return StringUtils.EMPTY;

    }

    private String[] convertToString(Object[] objects) {
        String[] values = new String[objects.length];
        int i=0;
        for(Object object: objects) {
            if(object!=null) {
                Converter converter = ConvertUtils.lookup(object.getClass());
                if (converter != null) {
                    values[i++] = converter.convert(object.getClass(),object).toString();
                } else {
                    values[i++] = object.toString();
                }
            } else {
                values[i++] = StringUtils.EMPTY;
            }
        }
            return values;
    }

    /**
     * Create file name using csv type and take absolute file path from jbilling.properties file.
     */
    private String createFile(String type, Integer entityId, boolean isInProgress) {
        SimpleDateFormat formater = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        String fileName = new StringBuilder().append(Util.getSysProp(Constants.PROPERTY_GENERATE_CSV_FILE_PATH))
                .append(entityId)
                .append("-")
                .append(type)
                .append("-")
                .append(formater.format(TimezoneHelper.serverCurrentDate())).toString();
                if(isInProgress) {
                 fileName = fileName+ "-Inprogress";
                }
                fileName = fileName +".csv";
                return fileName;
    }

    private String getTemporaryFileName(String type, Integer entityId, int counter) {
        return new StringBuilder().append(Util.getSysProp(Constants.PROPERTY_GENERATE_CSV_FILE_PATH))
                .append(entityId)
                .append("-")
                .append(type)
                .append("-")
                .append(TEMPORARY_FILE_NAME)
                .append("-")
                .append(counter)
                .append(".tmp").toString();
    }

    /**
     * returns count of records got skipped during csv generation process.
     * and performs csv export in separate thread
     * @param list
     * @param writer
     * @return
     */
    private Future<ThreadResult> executeCsvExportInNewThread(final List<T> list, final int counter) {
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
         return service.submit(() -> txAction.executeInReadOnlyTx(() -> {
             String tmpCsvFileName = getTemporaryFileName(csvType, entityId, counter);
             try (CSVWriter tmpCsvWriter = new CSVWriter(new FileWriter(tmpCsvFileName))) {
                 int noOfRowsSkipped =0;
                 int count = 0;
                 for(Exportable exportable: list) {
                     try {
                         Object[][] row = exportable.getFieldValues();
                      for (Object[] values : row) {
                             tmpCsvWriter.writeNext(convertToString(values));
                      }
                     } catch(Exception ex) {
                          noOfRowsSkipped ++;
                         logger.error("Record got Skipped: ", ex);
                      }

                     if(++count % 100 == 0) {
                         SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
                         sf.getCurrentSession().clear();
                     }
                 }
                 return new ThreadResult(tmpCsvFileName, noOfRowsSkipped);
             }
         }));
    }

    /**
    *
    * @param list
    * @param batchSize
    * @return
    */
   public static <T> List<List<T>> partition(List<T> list, int batchSize) {
       List<List<T>> parts = new ArrayList<>();
       int size = list.size();
       for (int i = 0; i < size; i += batchSize) {
           parts.add(new ArrayList<T>(
                   list.subList(i, Math.min(size, i + batchSize)))
                   );
       }
       return parts;
   }

   private class ThreadResult {
       private String tmpCsvFileName;
       private Integer noOfRowsSkipped;

       public ThreadResult(String fileName, Integer skippedRows) {
           this.tmpCsvFileName = fileName;
           this.noOfRowsSkipped = skippedRows;
       }

       public String getTmpCsvFileName() {
           return tmpCsvFileName;
       }

       public Integer getNoOfRowsSkipped() {
           return noOfRowsSkipped;
       }
   }

}