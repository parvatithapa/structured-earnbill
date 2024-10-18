package com.sapienter.jbilling.server.boa.batch;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.boa.batch.db.BoaBaiProcessedFileDAS;
import com.sapienter.jbilling.server.boa.batch.db.BoaBaiProcessedFileDTO;
import com.sapienter.jbilling.server.boa.batch.db.BoaBaiProcessingErrorDAS;
import com.sapienter.jbilling.server.boa.batch.db.BoaBaiProcessingErrorDTO;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class BOAFileReader implements ResourceAwareItemReaderItemStream<Object> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BOAFileReader.class));
    private static final String RECORD_TYPE_02 = "02";
    private static final String RECORD_TYPE_03 = "03";
    private static final String RECORD_TYPE_16 = "16";
    private static final String RECORD_TYPE_88 = "88";
    private static final String RECORD_PREFIX_KEY = "recordType";
    private static final String RECORD_TYPE_02_TXN_DATE_KEY = "date";
    private static final String RECORD_TYPE_02_TXN_TIME_KEY = "time";
    private static final String RECORD_TYPE_03_ACT_NUMBER_KEY = "customerAccountNumber";
    private static final String RECORD_TYPE_16_AMOUNT_KEY = "amount";
    private static final String RECORD_TYPE_16_TXN_TYPE_KEY = "typeCode";
    private static final String RECORD_TYPE_16_BANK_REF_NO_KEY = "bankRefNumber";
    private static final String RECORD_TYPE_16_CUST_REF_NO_KEY = "custRefNumber";
    private static final String RECORD_TYPE_16_RAW_DATA = "rawData";
    private static final String RECORD_TYPE_88_DETAILS_KEY = "details";
    private FieldSet curItem;
    private FlatFileItemReader<FieldSet> delegate;
    private Date date;
    private int time;
    private String customerAccountNumber;
    private String moveToDailyFilesDirectory;
    private String moveToIntradayFilesDirectory;
    private String readFromDailyFilesDirectory;
    private String readFromIntradayFilesDirectory;
    private Resource currentResource;

    public Object read() throws Exception {

        BOAFileRecord bankRecord;
        FieldSet line = null;

        try {

            while((line = readItem()) != null) {

                switch (line.readString(RECORD_PREFIX_KEY)) {
                    case RECORD_TYPE_02:
                        date = line.readDate(RECORD_TYPE_02_TXN_DATE_KEY, "yyMMdd");
                        try {
                            time = line.readInt(RECORD_TYPE_02_TXN_TIME_KEY);
                        } catch (NumberFormatException nfe) {
                            time = 0;
                        }
                        break;

                    case RECORD_TYPE_03:
                        customerAccountNumber = line.readString(RECORD_TYPE_03_ACT_NUMBER_KEY);
                        break;

                    case RECORD_TYPE_16:
                        String buffer = String.valueOf(line.readString(RECORD_TYPE_16_RAW_DATA));
                        bankRecord = new BOAFileRecord();
                        bankRecord.setTransactionDate(date);
                        bankRecord.setTransactionTime(time);
                        bankRecord.setFundingAccountId(customerAccountNumber);
                        BigDecimal amount = line.readBigDecimal(RECORD_TYPE_16_AMOUNT_KEY);
                        if (null != amount && amount.compareTo(BigDecimal.ZERO) > 0) {
                            bankRecord.setAmount(amount.divide(new BigDecimal(100.0)));
                        }

                        bankRecord.setTransactionType(line.readInt(RECORD_TYPE_16_TXN_TYPE_KEY));
                        bankRecord.setBankReferenceNo(line.readString(RECORD_TYPE_16_BANK_REF_NO_KEY));
                        bankRecord.setCustReferenceNo(line.readString(RECORD_TYPE_16_CUST_REF_NO_KEY));
                        bankRecord.setDetails(new ArrayList<String>());
                        bankRecord.setDepositFileName(currentResource.getFile().getAbsoluteFile().getName());
                        bankRecord.setDepositFileDirectory(currentResource.getFile().getAbsoluteFile().getParent());

                        while (peek().readString(RECORD_PREFIX_KEY).equals(RECORD_TYPE_88)) {
                            bankRecord.getDetails().add(curItem.readString(RECORD_TYPE_88_DETAILS_KEY));
                            curItem = null;
                        }

                        bankRecord.setRawData(buffer);
                        return bankRecord;
                }
            }

            close();
            moveFile();

        } catch (Exception e) {

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();

            String filename = currentResource.getFile().getAbsoluteFile().getName();
            LOG.debug("Error while processing file: " + filename + ": " + sw.toString());

            String rawData;
            if (line != null) {
                rawData = line.readString(line.hasNames() && Arrays.asList(line.getNames()).contains(RECORD_TYPE_16_RAW_DATA)
                        ? RECORD_TYPE_16_RAW_DATA
                        : RECORD_PREFIX_KEY);
            }
            else {
                rawData = "N/A";
            }

            new BoaBaiProcessingErrorDAS().save(new BoaBaiProcessingErrorDTO(filename, rawData, sw.toString().substring(0, 500)));

            close();
            moveFile();
        }

        return null;
    }

    private void moveFile() throws Exception {
        if (currentResource.getFile().getAbsoluteFile().getParent().equals(readFromDailyFilesDirectory)){
            moveDailyFile();
        } else if (currentResource.getFile().getAbsoluteFile().getParent().equals(readFromIntradayFilesDirectory)){
            moveIntradayFile();
        }
    }

    private FieldSet readItem () throws UnexpectedInputException, ParseException, NonTransientResourceException, Exception {
        if (curItem != null) {
            FieldSet item = curItem;
            curItem = null;
            return item;
        } else {
            return this.delegate.read();
        }
    }

    public FieldSet peek() throws Exception, UnexpectedInputException,
            ParseException {
        if (curItem == null) {
            curItem = delegate.read();
        }
        return curItem;
    }
    public void setDelegate(FlatFileItemReader<FieldSet> delegate) {
        this.delegate = delegate;
    }
    public void close() throws ItemStreamException {
        delegate.close();
    }
    public void open(ExecutionContext arg0) throws ItemStreamException {
        delegate.open(arg0);
    }
    public void update(ExecutionContext arg0) throws ItemStreamException {
        delegate.update(arg0);
    }

    @Override
    public void setResource(Resource resource) {
        try {
            LOG.info("Processing BOA file: " + resource.getFile().getAbsoluteFile().getAbsolutePath());
            currentResource = resource;
            delegate.setResource(resource);
        } catch (IOException e) {
            throw new SessionInternalError(e);
        }
    }

    public void setMoveToDailyFilesDirectory(String moveToDailyFilesDirectory) {
        this.moveToDailyFilesDirectory = moveToDailyFilesDirectory;
    }

    public void setMoveToIntradayFilesDirectory(String moveToIntradayFilesDirectory) {
        this.moveToIntradayFilesDirectory = moveToIntradayFilesDirectory;
    }

    public void setReadFromDailyFilesDirectory(String readFromDailyFilesDirectory) { this.readFromDailyFilesDirectory = readFromDailyFilesDirectory; }

    public void setReadFromIntradayFilesDirectory(String readFromIntradayFilesDirectory) { this.readFromIntradayFilesDirectory = readFromIntradayFilesDirectory; }

    private void moveDailyFile() {
        try {
            LOG.info("Moving file [From] - " + currentResource.getFile().getAbsoluteFile().getAbsolutePath() +
                    " [To] - " + moveToDailyFilesDirectory + File.separator + currentResource.getFile().getAbsoluteFile().getName() + ".done");
            if (new BoaBaiProcessingErrorDAS().isProcessedWithError(currentResource.getFile().getAbsoluteFile().getName())){
                FileUtils.moveFile(currentResource.getFile().getAbsoluteFile(), new File(moveToDailyFilesDirectory + File.separator +
                        currentResource.getFile().getAbsoluteFile().getName() + ".failed"));
            } else {
                FileUtils.moveFile(currentResource.getFile().getAbsoluteFile(), new File(moveToDailyFilesDirectory + File.separator +
                        currentResource.getFile().getAbsoluteFile().getName() + ".done"));
                saveBoaBaiProcessedFile(currentResource.getFile().getAbsoluteFile().getName());
            }
        } catch (IOException e) {
            throw new SessionInternalError(e);
        }
    }
    private void moveIntradayFile() {
        try {
            LOG.info("Moving file [From] - " + currentResource.getFile().getAbsoluteFile().getAbsolutePath() +
                    " [To] - " + moveToIntradayFilesDirectory + File.separator +  currentResource.getFile().getAbsoluteFile().getName() + ".done");
            if(new BoaBaiProcessingErrorDAS().isProcessedWithError(currentResource.getFile().getAbsoluteFile().getName())){
                FileUtils.moveFile(currentResource.getFile().getAbsoluteFile(), new File(moveToIntradayFilesDirectory + File.separator +
                        currentResource.getFile().getAbsoluteFile().getName() + ".failed"));
            }else {
                FileUtils.moveFile(currentResource.getFile().getAbsoluteFile(), new File(moveToIntradayFilesDirectory + File.separator +
                        currentResource.getFile().getAbsoluteFile().getName() + ".done"));
                saveBoaBaiProcessedFile(currentResource.getFile().getAbsoluteFile().getName());
            }

        } catch (IOException e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * Save file in database after successful processed.
     * @param fileName
     */
    private void saveBoaBaiProcessedFile(String fileName) {
        BoaBaiProcessedFileDTO boaBaiProcessedFile = new BoaBaiProcessedFileDTO();
        boaBaiProcessedFile.setFileName(fileName);
        new BoaBaiProcessedFileDAS().save(boaBaiProcessedFile);
    }
}