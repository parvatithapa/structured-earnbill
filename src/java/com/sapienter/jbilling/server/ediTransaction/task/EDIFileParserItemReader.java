package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import com.sapienter.jbilling.server.ediTransaction.EDITypeWS;
import com.sapienter.jbilling.server.ediTransaction.IEDITransactionBean;
import com.sapienter.jbilling.server.ediTransaction.TransactionType;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.fileParser.FlatFileParser;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Created by aman on 16/10/15.
 */
public class EDIFileParserItemReader implements ItemReader<EDIFileWS>, StepExecutionListener, ItemReadListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EDIFileParserItemReader.class));

    //    private String utilityDUNS;
//    private String supplierDUNS;
//    private String transactionSet;
    private Integer companyId;

    private EDITypeWS ediTypeWS;

    private IEDITransactionBean transactionBean;
    private IWebServicesSessionBean webServicesSessionBean;

    private File[] files = null;
    private int index;
    FileFormat fileFormat;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("EDI File Parser Item Reader : Before Step");
        JobParameters jobParameters = stepExecution.getJobParameters();

        Integer ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        fileFormat = FileFormat.getFileFormat(ediTypeId);
        companyId = jobParameters.getLong("companyId").intValue();
        String utilityDUNS = jobParameters.getString("utilityDUNS");
        String supplierDUNS = jobParameters.getString("supplierDUNS");
        String transactionSet = jobParameters.getString("TRANSACTION_SET");

        transactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTypeWS = webServicesSessionBean.getEDIType(ediTypeId);
        files = pickFiles(utilityDUNS, supplierDUNS, transactionSet);
        index = 0;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        LOG.debug("EDI File Parser Item Reader : After Step");
        return null;
    }

    @Override
    public void beforeRead() {
        LOG.debug("EDI File Parser Item Reader : Before Read");
    }

    @Override
    public void afterRead(Object o) {
        LOG.debug("EDI File Parser Item Reader : After Read");
    }

    @Override
    public void onReadError(Exception e) {
        LOG.debug("EDI File Parser Item Reader : on Read Error");
    }

    @Override
    public EDIFileWS read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

        LOG.debug("EDI File Parser Item Reader");
        File fileForParse = nextFile();
        if(fileForParse!=null) {
            FlatFileParser fileParser = new FlatFileParser(fileFormat, fileForParse, companyId);
            return transactionBean.parseEDIFile(fileParser);
        }
        else return null;
    }

    private File nextFile() throws IOException {
        if (index >= files.length) {
            LOG.debug("EDI File Parser Item Reader : No file found");
            return null;
        } else {
            File fileForParse = files[index];
            File file = moveFileToInboundFolder(fileForParse);
            if (file!=null) {
                index++;
                return file;
            }

            try {
                fileForParse.renameTo(new File(fileForParse.getAbsolutePath() + ".error"));
            } catch (Exception e) {
                LOG.error("Unable to rename the file : " + fileForParse.getName());
            }
            index++;
            nextFile();
        }
        return null;
    }

    private File[] pickFiles(String utilityDUNS, String supplierDUNS, String transactionSet) {
        LOG.debug("EDI File Parser Item Reader => Pick Files : " + "UtilityDUNS : " + utilityDUNS
                + ", supplierDUNS : " + supplierDUNS + ", transactionSet : " + transactionSet);
        String matchingPattern = utilityDUNS + "_" + supplierDUNS + "_.*.[.]" + transactionSet + "$";
        String path = transactionBean.getEDICommunicationPath(companyId, TransactionType.INBOUND);
        File dir = new File(path);

        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().matches(matchingPattern);
            }
        });
        LOG.debug("Files selected for parsing : " + files.length);
        //todo : Need to maintain the suffix for files which are under processing
        return files;
    }

    private File moveFileToInboundFolder(File movedFile) throws IOException {
        LOG.debug("Try to move file from temp to inbound folder.");
        File dir = new File(FileConstants.getEDITypePath(fileFormat.getEdiTypeDTO().getEntity().getId(), fileFormat.getEdiTypeDTO().getPath(), FileConstants.INBOUND_PATH));

        try {
            if(!dir.exists()){
                if(!dir.mkdirs()) throw new SessionInternalError("Error while creating dir : "+dir.getAbsolutePath());
            }
            File file = new File(dir.getAbsolutePath()+File.separator + movedFile.getName());
            movedFile.renameTo(file);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Error Occurred while moving file.");
        }
        return null;
    }
}
