package com.sapienter.jbilling.server.spc.payment.reconciliation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opencsv.CSVWriter;
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

public class PaymentReconciliationScheduledTask extends AbstractCronTask {

    SpcPaymentReconciliationHelperService reconciliationHelperService = Context.getBean(SpcPaymentReconciliationHelperService.class);

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription SPC_PARAM_RECONCILIATION_FILE_DIR_PATH =
            new ParameterDescription("SPC Reconciliation directory path", true, ParameterDescription.Type.STR);

    static final ParameterDescription SPC_PARAM_UNALLOCATED_PAYMENT_ACCOUNT =
            new ParameterDescription("SPC Unallocated Payment Account", true, ParameterDescription.Type.INT);

    private static final ParameterDescription AGL_PARAM_RECONCILIATION_FILE_DIR_PATH =
            new ParameterDescription("AGL Reconciliation directory path", true, ParameterDescription.Type.STR);

    static final ParameterDescription AGL_PARAM_UNALLOCATED_PAYMENT_ACCOUNT =
            new ParameterDescription("AGL Unallocated Payment Account", true, ParameterDescription.Type.INT);

    static final ParameterDescription PARAM_SETTLEMENT_DATE =
            new ParameterDescription("Settlement Date Meta Field Name", true, ParameterDescription.Type.STR);

    static final ParameterDescription PARAM_TRANSACTION_DATE_TIME =
            new ParameterDescription("Transaction Date/time Meta Field Name", true, ParameterDescription.Type.STR);

    static final String ERROR_PATH = "errorPath";

    public PaymentReconciliationScheduledTask() {
        descriptions.add(SPC_PARAM_RECONCILIATION_FILE_DIR_PATH);
        descriptions.add(SPC_PARAM_UNALLOCATED_PAYMENT_ACCOUNT);
        descriptions.add(AGL_PARAM_RECONCILIATION_FILE_DIR_PATH);
        descriptions.add(AGL_PARAM_UNALLOCATED_PAYMENT_ACCOUNT);
        descriptions.add(PARAM_SETTLEMENT_DATE);
        descriptions.add(PARAM_TRANSACTION_DATE_TIME);
    }

    @Override
    public String getTaskName() {
        return "PaymentReconciliationScheduledTask: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        synchronized (PaymentReconciliationScheduledTask.class) {
            try {
                _init(context);
                processFile(SPC_PARAM_RECONCILIATION_FILE_DIR_PATH);
                processFile(AGL_PARAM_RECONCILIATION_FILE_DIR_PATH);
            } catch(Exception ex) {
                logger.error("exception during payment reconciliation for entity {}", getEntityId(), ex);
            }
        }
    }

    private void processFile(ParameterDescription paramReconciliationFileDirPath) throws IOException {
        String reconciliationDirPath = getParameter(paramReconciliationFileDirPath.getName(), StringUtils.EMPTY);
        if (StringUtils.isEmpty(reconciliationDirPath)) {
            logger.debug("please provide {} for entity {}", paramReconciliationFileDirPath.getName(), getEntityId());
            return;
        }
        File reconciliationDir = new File(reconciliationDirPath);
        if (!reconciliationDir.exists()) {
            logger.debug("invalid {} provided for entity {}", paramReconciliationFileDirPath.getName(), getEntityId());
            return;
        }

        boolean isAGLFile = false;
        String fileName = "SPC";
        if (AGL_PARAM_RECONCILIATION_FILE_DIR_PATH.equals(paramReconciliationFileDirPath)) {
            isAGLFile = true;
            fileName = "AGL";
        }

        File errorLogDir = new File(reconciliationDirPath + File.separator + "Error-" + getEntityId());
        if (!errorLogDir.exists() && reconciliationDir.listFiles(File::isFile).length > 0) {
            errorLogDir.mkdir();
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String todaysDate = dateFormat.format(new Date());
        File errorLogFile = new File(errorLogDir.getAbsolutePath() + File.separator + fileName +"_error_log-" + todaysDate + ".csv");
        if (!errorLogFile.exists() && reconciliationDir.listFiles(File::isFile).length > 0) {
            try (FileWriter outputfile = new FileWriter(errorLogFile, true); CSVWriter writer = new CSVWriter(outputfile)) {
                String[] header = { "bpay reference number", "receipt number", "reason for rejection", "date created" };
                writer.writeNext(header);
            }
        }

        List<Future<Boolean>> isAllRecordsProcessed = new ArrayList<>();
        String errorPath = errorLogFile.getAbsolutePath();
        for (File file : reconciliationDir.listFiles(File::isFile)) {
            logger.debug("Reading records from file {} for entity {}", file.getName(), getEntityId());
            for (SpcPaymentReconciliationRecord record : readRecordsFromFile(file, errorPath)) {
                logger.debug("processing record {} for entity {} from file {}", record, getEntityId(), file.getName());
                Map<String, String> parameters = getParameters();
                parameters.put(ERROR_PATH, errorPath);
                isAllRecordsProcessed.add(reconciliationHelperService.reconcilePayment(record, parameters, isAGLFile));
            }
            moveFileToDirectory(file, new File(reconciliationDir.getAbsolutePath() + File.separator + "Done-" + getEntityId()));
        }

        isAllRecordsProcessed.forEach(record -> {
            try {
                record.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

    private List<SpcPaymentReconciliationRecord> readRecordsFromFile(File file, String errorPath) throws IOException {
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
                    reconciliationHelperService.writeErrorLog(record.getPaymentFieldByName("BPAY Ref"), txId,
                            String.format("skipping bpay record since payment found for transaction id/receipt number: {%s}", txId), errorPath);

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
