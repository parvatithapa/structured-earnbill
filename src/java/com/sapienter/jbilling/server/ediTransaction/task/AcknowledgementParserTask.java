package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.springframework.batch.core.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.sapienter.jbilling.server.fileProcessing.FileConstants.CUSTOMER_ACCOUNT_KEY;

/**
 * Created by vivek on 10/11/15.
 */
public class AcknowledgementParserTask extends AbstractScheduledTransactionProcessor implements StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AcknowledgementParserTask.class));

    public static enum AcknowledgementRead implements FileStructure {
        KEY, HDR, NME, DTL, CDE
    }

    public static enum AcknowledgementField {
        INVOICE_NR,
        ORG_TRANS_NR,
        TRANSCTION_SUBSET,
        REC_TYPE,
        UTILITY_CUST_ACCT_NR,
        CODE,
        TRANS_REF_NR
    }

    Integer invoiceTransactionSubset = 810;

    protected static final ParameterDescription ACKNOWLEDGE_STATUS_NAME =
            new ParameterDescription("acknowledge_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INVALID_DATA_STATUS_NAME =
            new ParameterDescription("invalid_file_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REJECTED_STATUS_NAME =
            new ParameterDescription("reject_status", true, ParameterDescription.Type.STR);

    {
        descriptions.add(ACKNOWLEDGE_STATUS_NAME);
        descriptions.add(INVALID_DATA_STATUS_NAME);
        descriptions.add(REJECTED_STATUS_NAME);
    }

    IWebServicesSessionBean webServicesSessionSpringBean;
    IEDITransactionBean ediTransactionBean;
    IPaymentSessionBean paymentSessionBean;

    private Integer ediTypeId;
    private Integer invoiceEDITypeId;
    private String supplierDUNS;
    private String utilityDUNS;



    private int companyId;
    private String exceptionCode;
    private String status;
    private String comment;

    private String originalFileStatus = null;
    private String originalFileComment = null;
    private String originalExceptionCode = null;
    private EDIFileWS originalEDIFile = null;


    private String INVALID_DATA_STATUS;
    private String REJECTED_STATUS;
    private String ACKNOWLEDGE_STATUS;


    @Override
    public String getTaskName() {
        return "Acknowledgement parser: " + getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    protected String getJobName() {
        return Context.Name.BATCH_EDI_ACKNOWLEDGEMENT_PROCESS.getName();
    }

    @Override
    public void preBatchConfiguration(Map jobParams) {
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

        LOG.debug("Execute Acknowledgement ParserTask  plugin.");

        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.ACKNOWLEDGE_EDI_TYPE_ID_META_FIELD_NAME);
        invoiceEDITypeId = (Integer) companyMetaFieldValueMap.get(FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME);
        if (EDI_TYPE_ID == null) {
            throw new SessionInternalError("EDI type id not valid : " + EDI_TYPE_ID);
        }

        if(invoiceEDITypeId == null) {
            throw new SessionInternalError("In Acknowledge parser task Invoice EDI type id not valid : " + invoiceEDITypeId);
        }

        EDITypeWS ediType = null;
        //todo : refactor
        ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);
        if (ediType == null)
            throw new SessionInternalError("EDI type id not found : " + EDI_TYPE_ID);


        jobParams.put("acknowledge_status", new JobParameter(parameters.get(ACKNOWLEDGE_STATUS_NAME.getName())));
        jobParams.put("invalid_file_status", new JobParameter(parameters.get(INVALID_DATA_STATUS_NAME.getName())));
        jobParams.put("reject_status", new JobParameter(parameters.get(REJECTED_STATUS_NAME.getName())));

        jobParams.put("ediTypeId", new JobParameter(EDI_TYPE_ID.longValue()));
        jobParams.put("invoiceEDITypeId", new JobParameter(invoiceEDITypeId.longValue()));
        jobParams.put("supplierDUNS", new JobParameter(SUPPLIER_DUNS));
        jobParams.put("utilityDUNS", new JobParameter(UTILITY_DUNS));
//        Set transaction type from suffix.
        jobParams.put("TRANSACTION_SET", new JobParameter(ediType.getEdiSuffix()));
    }

    private void clearData(){

        comment=null;
        ediTypeId=null;
        status=null;

        originalFileStatus = null;
        originalFileComment = null;
        originalExceptionCode = null;
        originalEDIFile = null;

        ediFile=null;
        escapeExceptionStatus=null;
        recordFields=null;
        recordFieldsList=null;
        recordLevelExceptionCode=null;
        exceptionCode=null;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("EDI File Acknowledgement Item Processor : Before Step");
        paymentSessionBean = (IPaymentSessionBean) Context.getBean(Context.Name.PAYMENT_SESSION);
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        JobParameters jobParameters = stepExecution.getJobParameters();

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        invoiceEDITypeId = jobParameters.getLong("invoiceEDITypeId").intValue();
        utilityDUNS = jobParameters.getString("utilityDUNS");
        supplierDUNS = jobParameters.getString("supplierDUNS");


        INVALID_DATA_STATUS = jobParameters.getString("invalid_file_status");
        REJECTED_STATUS = jobParameters.getString("reject_status");
        ACKNOWLEDGE_STATUS = jobParameters.getString("acknowledge_status");
        companyId = jobParameters.getLong("companyId").intValue();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    public EDIFileWS process(EDIFileWS ediFileWS) throws Exception {
        LOG.debug("Acknowledge Read Task Process Method");
        //TODO code repeted
        try {
            if (ediFileWS.getEdiFileStatusWS().getId() == FileConstants.EDI_STATUS_PROCESSED) {
                this.ediFile = ediFileWS;
//                Set file field values in record map.
                processEnrollmentResponseFile();

//                Validate Acknowledge record.
                validateAcknowledgeRecord();
//              Process acknowledge and read file.
                processAcknowledgeReadFile();
            }
        } catch (Exception ex) {
            LOG.error("------------------final exception------  " + ex);
            status = (status == null) ? INVALID_DATA_STATUS : status;
            comment= EDITransactionHelper.getExceptionMessage(ex);
        }

        //        Save Original edi file here.
        if(originalEDIFile != null) {
            originalEDIFile.setComment(originalFileComment);
            if(originalFileStatus != null) {
                originalEDIFile.setExceptionCode(exceptionCode);
                try {
//                    Try to save status for invoice EDI file.
                    changeEdiFileStatus(originalFileStatus, originalEDIFile);
                } catch (SessionInternalError sie) {
//                    Caught exception here while tried to save corresponding invoice EDI file status.
                    status = REJECTED_STATUS;
                    exceptionCode = null;
                    comment = sie.getMessage();
                }
            }
        }

        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for (EDIFileStatusWS ediStatus : ediType.getEdiStatuses()) {
            if (ediStatus.getName().equals(status)) {
                statusWS = ediStatus;
            }
        }

        ediFileWS.setEdiFileStatusWS(statusWS);
        ediFileWS.setComment(comment);
        if (exceptionCode != null) ediFileWS.setExceptionCode(exceptionCode);
        bindFields(ediFileWS);
        clearData();
        return ediFileWS;
    }

    void processEnrollmentResponseFile() throws Exception {
        List<String> maidenRecords = new LinkedList<String>();
        maidenRecords.add(AcknowledgementRead.KEY.toString());
        maidenRecords.add(AcknowledgementRead.HDR.toString());
        maidenRecords.add(AcknowledgementRead.NME.toString());
        maidenRecords.add(AcknowledgementRead.DTL.toString());
        maidenRecords.add(AcknowledgementRead.CDE.toString());

        //Parse maiden records
        parseRecords(maidenRecords, ediFile.getEDIFileRecordWSes());

    }

    private void validateAcknowledgeRecord() {

        LOG.debug("Binding customer account number");
        customerAccountNumber = findField(AcknowledgementRead.HDR, AcknowledgementField.UTILITY_CUST_ACCT_NR.toString());
        LOG.info("Validating customer exist");
        if(customerAccountNumber==null){
            throwException("Account Id not found in Meter read file. EDI File Id: " + ediFile.getId(), null, REJECTED_STATUS);
            LOG.error("Account Id not found in Meter read file. EDI File Id: " + ediFile.getId());
        }

        userDTO=new UserDAS().findUserByAccountNumber(companyId, AcknowledgementField.UTILITY_CUST_ACCT_NR.name(), customerAccountNumber);
        LOG.debug("User "+userDTO);
        if(userDTO==null){
            throwException("User not found for utility account number " + customerAccountNumber, null, REJECTED_STATUS);
        }

        String transferRefNR = findField(AcknowledgementRead.HDR, AcknowledgementField.TRANS_REF_NR.toString());
        LOG.debug(AcknowledgementField.TRANS_REF_NR.toString() + "value found : " + transferRefNR);
        try {
            ediTransactionBean.isUniqueKeyExistForFile(companyId, ediTypeId, ediFile.getId(), AcknowledgementField.TRANS_REF_NR.toString(), transferRefNR, TransactionType.INBOUND);
        } catch (SessionInternalError e) {
            status=INVALID_DATA_STATUS;
            exceptionCode = FileConstants.INVOICE_READ_DUPLICATE_TRANSACTION_EXP_CODE;
            throw e;
        }
    }


    private void processAcknowledgeReadFile() {
//        Find invoice bind with file

//        check if transaction subset is exist for invoice which is 810. TODO: We need to make it configurable for different -2 edi type.
        try {
            String transactionSubSet = findField(AcknowledgementRead.KEY, AcknowledgementField.TRANSCTION_SUBSET.name(), true);

            if (!transactionSubSet.equals(invoiceTransactionSubset.toString())) {
                status = INVALID_DATA_STATUS;
                comment = "Please provide correct transaction subset.";
                throw new SessionInternalError(comment);
            }
        } catch (SessionInternalError sie) {
            status = INVALID_DATA_STATUS;
            comment = "Please provide " + AcknowledgementField.TRANSCTION_SUBSET.name();
            throw new SessionInternalError(comment);
        }

//        Check if ORG_TRANS_NR exist for this file.

        try {
            String ORG_TRANS_NR_VAL = findField(AcknowledgementRead.DTL, AcknowledgementField.ORG_TRANS_NR.name(), true);

            Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(Restrictions.eq("ediType.id", invoiceEDITypeId));
            conjunction.add(Restrictions.eq("entity.id", companyId));
            conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", AcknowledgementField.INVOICE_NR.name()));
            conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", ORG_TRANS_NR_VAL));

            List<Integer> originalEDIFileIds = new EDIFileDAS().findFileByData(conjunction);

//            In case no original file exist for ORG_TRANS_NR
            if(originalEDIFileIds.isEmpty()) {
                status = INVALID_DATA_STATUS;
                comment = "No original file found for " + AcknowledgementField.ORG_TRANS_NR.name() + " with value " + ORG_TRANS_NR_VAL;
                throw new SessionInternalError(comment);
            }
            Integer originalEDIFileId = originalEDIFileIds.get(0);
//        Check If any EDI file exist for this ORG_TRANS_NR.
            originalEDIFile = ediTransactionBean.getEDIFileWS(originalEDIFileId);

//            In case no original file exist for ORG_TRANS_NR. Double check
            if(originalEDIFile == null) {
                status = INVALID_DATA_STATUS;
                comment = "No original file found for " + AcknowledgementField.ORG_TRANS_NR.name() + " with value " + ORG_TRANS_NR_VAL;
                throw new SessionInternalError(comment);
            }

            if(originalEDIFile.getEdiFileStatusWS().getName().equals(REJECTED_STATUS)) {
                /*TODO: Need to change reject status name which belongs to invoice rejection.*/
                status = INVALID_DATA_STATUS;
                comment = "Corresponding original file " + originalEDIFile.getName() + " has already been rejected.";
                throw new SessionInternalError(comment);
            }

        } catch (SessionInternalError sie) {
            status = INVALID_DATA_STATUS;
//            comment = "Please provide " + AcknowledgementField.ORG_TRANS_NR.name();
            throw new SessionInternalError(sie.getMessage());
        }


//        If original invoice file found set its status as REJECTED.

//        Check if Code record exist in file.

//        If code record exist then set file status as REJECTED for both file. Also set same exception code for both file.
        try {
            String recType = findField(AcknowledgementRead.CDE, AcknowledgementField.REC_TYPE.name(), true);

            if(recType.equals("R")) {
                String code = findField(AcknowledgementRead.CDE, AcknowledgementField.CODE.name());
                status = REJECTED_STATUS;

                if(code != null) {
                    exceptionCode = code;
                    originalExceptionCode = code;
                } else {
                    comment = "Invoice has been rejected. No Exception code send by LDC.";
                    originalFileComment = "Invoice has been rejected. No Exception code send by LDC.";
                }

                /*TODO: Need to change reject status name which belongs to invoice rejection.*/
                originalFileStatus = REJECTED_STATUS;

            }

        } catch (SessionInternalError sie) {
            LOG.error("Invalid data status. " + sie);
            status = INVALID_DATA_STATUS;
            comment = sie.getMessage();
            throw new SessionInternalError(comment);
        }
    }

    private void changeEdiFileStatus(String statusName, EDIFileWS ediFileWS) {
        LOG.debug("In EnrollmentResponseParserTask before fetching edit status  " + statusName);
        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(invoiceEDITypeId);
        EDIFileStatusWS statusWS = null;
        for (EDIFileStatusWS ediStatus : ediType.getEdiStatuses()) {
            if (ediStatus.getName().equals(status)) {
                statusWS = ediStatus;
            }
        }
        if(statusWS == null) {
            LOG.error("The status " + statusName +  " provided to invoiced file is not matched any status in invoice EDI type. Please provide valid status.");
        }
        ediFileWS.setEdiFileStatusWS(statusWS);
        LOG.debug("In EnrollmentResponseParserTask completing loop here:  " + statusWS);
        try {
            webServicesSessionSpringBean.saveEDIFileRecord(ediFileWS);

        } catch (SessionInternalError sie) {
            LOG.error("Unable to save edi file status " + sie);
            throw new SessionInternalError("Problem in saving outbound edi file");
        }
        LOG.debug("statusWS after save edi file is:  " + statusWS);
    }
}
