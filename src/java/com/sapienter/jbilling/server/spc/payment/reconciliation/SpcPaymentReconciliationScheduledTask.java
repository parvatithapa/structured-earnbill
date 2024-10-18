package com.sapienter.jbilling.server.spc.payment.reconciliation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;

import com.google.common.collect.Maps;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;

import au.com.bytecode.opencsv.CSVWriter;

public class SpcPaymentReconciliationScheduledTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_RECONCILIATION_FILE_DIR_PATH =
            new ParameterDescription("Reconciliation directory path", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_UNALLOCATED_PAYMENT_ACCOUNT =
            new ParameterDescription("Unallocated Payment Account", true, ParameterDescription.Type.INT);

    static final ParameterDescription PARAM_SETTLEMENT_DATE =
            new ParameterDescription("Settlement Date Meta Field Name", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_TRANSACTION_DATE_TIME =
            new ParameterDescription("Transaction Date/time Meta Field Name", true, ParameterDescription.Type.STR);
    
    static final String ERROR_PATH = "errorPath";

    public SpcPaymentReconciliationScheduledTask() {
        descriptions.add(PARAM_RECONCILIATION_FILE_DIR_PATH);
        descriptions.add(PARAM_UNALLOCATED_PAYMENT_ACCOUNT);
        descriptions.add(PARAM_SETTLEMENT_DATE);
        descriptions.add(PARAM_TRANSACTION_DATE_TIME);
    }

    @Override
    public String getTaskName() {
        return "SpcPaymentReconciliationScheduledTask: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        synchronized (SpcPaymentReconciliationScheduledTask.class) {
            try {
                _init(context);
                String reconciliationDirPath = getParameter(PARAM_RECONCILIATION_FILE_DIR_PATH.getName(), StringUtils.EMPTY);
                if(StringUtils.isEmpty(reconciliationDirPath)) {
                    logger.debug("please provide {} for entity {}", PARAM_RECONCILIATION_FILE_DIR_PATH.getName(), getEntityId());
                    return;
                }
                File reconciliationDir = new File(reconciliationDirPath);
                if(!reconciliationDir.exists()) {
                    logger.debug("invalid {} provided for entity {}", PARAM_RECONCILIATION_FILE_DIR_PATH.getName(), getEntityId());
                    return;
                }
                
				File errorLogDir = new File(reconciliationDirPath
						+ File.separator + "Error-" + getEntityId());
				if (!errorLogDir.exists() && reconciliationDir.listFiles(File::isFile).length > 0) {
					errorLogDir.mkdir();
				}
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				String todaysDate = dateFormat.format(new Date());
				File errorLogFile = new File(errorLogDir.getAbsolutePath()
						+ File.separator + "error_log-" + todaysDate + ".csv");
				if (!errorLogFile.exists() && reconciliationDir.listFiles(File::isFile).length > 0) {
					try (FileWriter outputfile = new FileWriter(errorLogFile, true);
							 CSVWriter writer = new CSVWriter(outputfile)) {
						String[] header = { "bpay reference number",
								"receipt number", "reason for rejection",
								"date created" };
						writer.writeNext(header);
					}
				}

                SpcPaymentReconciliationHelperService reconciliationHelperService = Context.getBean(SpcPaymentReconciliationHelperService.class);
                for(File file : reconciliationDir.listFiles(File::isFile)) {
                    logger.debug("Reading records from file {} for entity {}", file.getName(), getEntityId());
                    for(SpcPaymentReconciliationRecord record : readRecordsFromFile(file)) {
                        logger.debug("processing record {} for entity {} from file {}", record, getEntityId(), file.getName());
                        Map<String, String> parameters = getParameters();
                        parameters.put(ERROR_PATH, errorLogFile.getAbsolutePath());
                        reconciliationHelperService.reconcilePayment(record, parameters);
                    }
                    moveFileToDirectory(file, new File(reconciliationDir.getAbsolutePath() + File.separator + "Done-" + getEntityId()));
                }
            } catch(Exception ex) {
                logger.error("exception during payment reconciliation for entity {}", getEntityId(), ex);
            }
        }
    }
    
    /**
     * Creates {@link SpcPaymentReconciliationRecord} for given line
     * @param line
     * @return
     */
    private SpcPaymentReconciliationRecord convertLineToPaymentRecord(String line) {
        DelimitedLineTokenizer paymentTokenizer = Context.getBean("spcCsvPaymentRecordTokenizer");
        FieldSet paymentRecordInfo = paymentTokenizer.tokenize(line);
        SpcPaymentReconciliationRecord paymentReconciliationRecord = new SpcPaymentReconciliationRecord(getEntityId());
        paymentReconciliationRecord.setPaymentFields(Maps.fromProperties(paymentRecordInfo.getProperties()));
        logger.debug("{} created from line {} for entity {}", paymentReconciliationRecord, line, getEntityId());
        return paymentReconciliationRecord;
    }

    private static void moveFileToDirectory(File file, File dir) {
        if(!dir.exists()) {
            dir.mkdir();
        }
        if(file.renameTo(new File(dir, file.getName() + "-" + System.currentTimeMillis()))) {
            logger.debug("file {} moved to dir {}", file.getName(), dir.getAbsolutePath());
        } else {
            logger.debug("file {} not moved", file.getName());
        }
    }

    private List<SpcPaymentReconciliationRecord> readRecordsFromFile(File file) throws IOException {
        Map<String, SpcPaymentReconciliationRecord> txIds = new HashMap<>();
        try (Stream<String> lines = Files.lines(file.toPath())) {
            return lines.filter(StringUtils::isNotEmpty).map(line -> {
                if(line.contains("PayWayClientNumber")) {
                    logger.debug("header line {} for file {}", line, file.getName());
                }
                return line;
            }).filter(line -> !line.contains("PayWayClientNumber"))
            .filter(line -> !isEmptyField(line, ","))
            .map(this::convertLineToPaymentRecord)
            .filter(record -> {
                String txId = record.getTransactionId();
                if(txIds.containsKey(txId)) {
                    logger.debug("duplicate transaction id {} found on record {}, "
                            + "transaction id {} belongs to record {}", txId, record, txId, txIds.get(txId));
                    return false;
                }
                txIds.put(txId, record);
                return true;
            })
            .collect(Collectors.toList());
        }
    }

    private boolean isEmptyField(String line, String delemeter) {
        String[] fields = line.split(delemeter);
        if(ArrayUtils.isEmpty(fields) || StringUtils.isEmpty(fields[0])) {
            return true;
        }
        return false;
    }

}
