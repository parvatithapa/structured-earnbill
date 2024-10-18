package com.sapienter.jbilling.server.earlyTermination.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.task.AbstractScheduledTransactionProcessor;
import com.sapienter.jbilling.server.ediTransaction.task.FileStructure;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.batch.core.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by vivek on 28/9/15.
 */
public class CustomerTerminationTask extends AbstractScheduledTransactionProcessor implements StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CustomerTerminationTask.class));



    public static final String TRANSACTION_SET = "t814";

    private String SUCCESS_STATUS;
    private String ACCEPT_STATUS;
    private String REJECT_STATUS;
    private String INVALID_FILE_STATUS;
    private String INTERNAL_ERROR;
    private int companyId;

    private Integer ediTypeId;
    private String supplierDUNS;
    private String utilityDUNS;
    private EDIFileWS generatedFileWS = null;
    private UserWS userWS = null;
    /*Status, comment, exception code for inbound edi file*/
    private String status = null;
    private String comment;
    private String exceptionCode;
    Map<String, String> codeRecordMap = null;
    private String outboundStatus = null;
    private String outboundComment = null;


    public static enum CustomerTerminationField {
        TRANS_REF_NR,
        UTILITY_CUST_ACCT_NR,
        REC_TYPE,
        CODE,
        END_SERVICE_DT,
        CUST_LIFE_SUPPORT,
    }

    public static enum CustomerTermination implements FileStructure {
        KEY, HDR, NME, ACT, DTE, MTR, CDE
    }


      protected static final ParameterDescription ACCEPT_STATUS_NAME =
            new ParameterDescription("accept_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REJECT_STATUS_NAME =
            new ParameterDescription("reject_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription SUCCESS_STATUS_NAME =
            new ParameterDescription("success_status", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription INVALID_FILE_STATUS_NAME =
            new ParameterDescription("invalid_file_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INTERNAL_ERROR_NAME =
            new ParameterDescription("internal_error", false, ParameterDescription.Type.STR);
    protected static final ParameterDescription READY_TO_SEND_STATUS_NAME =
            new ParameterDescription("ready_to_send_status", true, ParameterDescription.Type.STR);

    {
        descriptions.add(ACCEPT_STATUS_NAME);
        descriptions.add(REJECT_STATUS_NAME);
        descriptions.add(SUCCESS_STATUS_NAME);
        descriptions.add(INVALID_FILE_STATUS_NAME);
        descriptions.add(INTERNAL_ERROR_NAME);
        descriptions.add(READY_TO_SEND_STATUS_NAME);
    }

    IWebServicesSessionBean webServicesSessionSpringBean;
    IOrderSessionBean orderSessionBean;
    IEDITransactionBean ediTransactionBean;
    private String READY_TO_SEND_STATUS;

    public String getTaskName() {
        return "Customer termination task: " + getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    protected String getJobName() {
        return Context.Name.BATCH_EDI_CUSTOMER_TERMINATION_PROCESS.getName();
    }

    @Override
    public void preBatchConfiguration(Map jobParams) {
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        LOG.debug("Execute InvoiceReadTask  plugin.");

        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.TERMINATION_EDI_TYPE_ID_META_FIELD_NAME);
        if(EDI_TYPE_ID == null) {
            throw new SessionInternalError("EDI type id not valid : " + EDI_TYPE_ID);
        }


        EDITypeWS ediType = null;
        //todo : refactor
        ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);
        if (ediType == null)
            throw new SessionInternalError("EDI type id not found : " + EDI_TYPE_ID);


        jobParams.put("ACCEPT_STATUS", new JobParameter(parameters.get(ACCEPT_STATUS_NAME.getName())));

        jobParams.put("REJECT_STATUS", new JobParameter(parameters.get(REJECT_STATUS_NAME.getName())));
        jobParams.put("SUCCESS_STATUS", new JobParameter(parameters.get(SUCCESS_STATUS_NAME.getName())));
        jobParams.put("INVALID_FILE_STATUS", new JobParameter(parameters.get(INVALID_FILE_STATUS_NAME.getName())));
        jobParams.put("INTERNAL_ERROR", new JobParameter(parameters.get(INTERNAL_ERROR_NAME.getName())));

        jobParams.put("ediTypeId", new JobParameter(EDI_TYPE_ID.longValue()));
        jobParams.put("supplierDUNS", new JobParameter(SUPPLIER_DUNS));
        jobParams.put("utilityDUNS", new JobParameter(UTILITY_DUNS));
//        Set transaction type from suffix.
        jobParams.put("TRANSACTION_SET", new JobParameter(ediType.getEdiSuffix()));
        jobParams.put("READY_TO_SEND_STATUS", new JobParameter(parameters.get(READY_TO_SEND_STATUS_NAME.getName())));
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {

        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);

        LOG.debug("EDI File Invoice Item Processor : Before Step");
        JobParameters jobParameters = stepExecution.getJobParameters();

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        utilityDUNS = jobParameters.getString("utilityDUNS");
        supplierDUNS = jobParameters.getString("supplierDUNS");

        SUCCESS_STATUS = jobParameters.getString("SUCCESS_STATUS");
        ACCEPT_STATUS = jobParameters.getString("ACCEPT_STATUS");
        REJECT_STATUS = jobParameters.getString("REJECT_STATUS");
        INTERNAL_ERROR = jobParameters.getString("INTERNAL_ERROR");
        INVALID_FILE_STATUS = jobParameters.getString("INVALID_FILE_STATUS");
        companyId = jobParameters.getLong("companyId").intValue();
        READY_TO_SEND_STATUS = jobParameters.getString("READY_TO_SEND_STATUS");

    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    private void clearData(){

        customerAccountNumber=null;
        userDTO=null;
        comment=null;
        ediTypeId=null;
        status=null;
        generatedFileWS = null;
        userWS = null;
        codeRecordMap = null;
        outboundStatus = null;
        outboundComment = null;

        ediFile=null;
        escapeExceptionStatus=null;
        recordFields=null;
        recordFieldsList=null;
        recordLevelExceptionCode=null;
        exceptionCode=null;
    }

    public EDIFileWS process(EDIFileWS ediFileWS) throws Exception {
        LOG.debug("Invoice Read Task Process Method  " + ediFileWS);
        try {
            if (ediFileWS.getEdiFileStatusWS().getId() == FileConstants.EDI_STATUS_PROCESSED) {
                this.ediFile = ediFileWS;

                processEnrollmentResponseFile();

                validateCustomerTermination();

                saveOutboundFileAndUser();

            } else {
                // Change the status to Error Detected
                LOG.error("Error");
                status = ediFileWS.getEdiFileStatusWS().getName();
            }
        } catch (Exception ex) {
            LOG.error("---------------Final exception in Customer termination task------------------------- " + ex);
            status = (status == null) ? INVALID_FILE_STATUS : status;
            comment= EDITransactionHelper.getExceptionMessage(ex);
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
        maidenRecords.add(CustomerTermination.KEY.toString());
        maidenRecords.add(CustomerTermination.HDR.toString());
        maidenRecords.add(CustomerTermination.ACT.toString());
        maidenRecords.add(CustomerTermination.MTR.toString());
        maidenRecords.add(CustomerTermination.DTE.toString());
        maidenRecords.add(CustomerTermination.CDE.toString());
//        Parse maiden records
        parseRecords(maidenRecords, ediFile.getEDIFileRecordWSes());

    }

    private void validateCustomerTermination() {
        /*TODO: Do all validation here.*/

       LOG.debug("Binding account number and user to edi file");
        userWS = getUserIfExist();

        try {
//            Check if TRANS_REF_NR is unique
            String transferNRValue = findField(CustomerTermination.HDR, CustomerTerminationField.TRANS_REF_NR.toString());
            ediTransactionBean.isUniqueKeyExistForFile(getEntityId(), ediTypeId, ediFile.getId(), CustomerTerminationField.TRANS_REF_NR.toString(), transferNRValue, TransactionType.INBOUND);
        } catch (Exception ex) {
            LOG.error("Duplicate trans ref nr.");
            status = INVALID_FILE_STATUS;
            comment = ex.getMessage();
            exceptionCode = FileConstants.DUPLICATE_TRANSACTION_EXP_CODE;
        }

        parseRecordsList(ediFile.getEDIFileRecordWSes());
        codeRecordMap = getRecordMap(recordFieldsList, FileConstants.CDE);
//        If CDE record already in termination record, it should be remove first.
        recordFieldsList.remove(codeRecordMap);




//        If user is already in process with drop request or dropped, throw exception and reject the file.
        checkIfUserAlreadyTerminatedOrDropped();

        try {
            validateTerminationFields();
        } catch (SessionInternalError sie) {
            LOG.error("Could not validate termination fields. " + sie.getMessage());
            status = REJECT_STATUS;
            comment = sie.getMessage();

            //outboundStatus = REJECT_STATUS;
            //outboundComment = sie.getMessage();

            createCodeRecordMap("R", comment);
            generateFile();
            saveOutboundFileAndUser();
            throw sie;
        }

//        Drop request has been approved. Send success outbound file to LDC.

        try {
            generateFile();

            status = ACCEPT_STATUS;
            comment = "Drop Request has been accepted.";

            //outboundStatus = ACCEPT_STATUS;
            //outboundComment = "Drop Request has been accepted.";

            Date endServiceDate = (Date) findField(CustomerTermination.DTE, CustomerTerminationField.END_SERVICE_DT.toString(), "Date", true);

            addMetaField(userWS, FileConstants.CUSTOMER_TERMINATION_DATE_METAFIELD , endServiceDate);

            AbstractScheduledTransactionProcessor.updateTerminationMetaField(userWS, FileConstants.TERMINATION_PROCESSING, endServiceDate);

            moveFileToInboundFolder();

        } catch (IOException ioex) {
          LOG.error("Unable to move file from LDC folder to Internal inbound folder.");
            throw new SessionInternalError(ioex.getMessage());
        } catch (Exception ex) {
            LOG.error("Something went wrong while trying to save Accept Status.");
            throw new SessionInternalError(ex.getMessage());
        }
    }

    private void checkIfUserAlreadyTerminatedOrDropped() {
        if (!blank(userWS)) {
            for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
                if (metaFieldValueWS.getFieldName().equals(FileConstants.TERMINATION_META_FIELD)) {
                    LOG.debug("Termination meta field exist.");
                    if (metaFieldValueWS.getValue().equals(FileConstants.TERMINATION_PROCESSING) || metaFieldValueWS.getValue().equals(FileConstants.DROPPED)) {
                        LOG.error("Customer" + userWS.getUserName() + " is either in process or dropped.");

                        //outboundComment = "Customer " + userWS.getUserName() + " is either in process or dropped.";
                        //outboundStatus = REJECT_STATUS;

                        status = REJECT_STATUS;
                        comment = "Customer " + userWS.getUserName() + " is either in process or dropped.";
                        exceptionCode = FileConstants.CUSTOMER_EITHER_IN_PROCESS_OR_DROPPED_EXP_CODE;
//                        If file is reject due to already iin process or dropped user, create CDE record and generate file for this.
                        createCodeRecordMap("R", FileConstants.CUSTOMER_EITHER_IN_PROCESS_OR_DROPPED_EXP_CODE);
                        generateFile();
                        saveOutboundFileAndUser();
                        throw new SessionInternalError("Customer " + userWS.getUserName() + " is either in process or dropped.");
                    }
                }
            }
        }
    }

    private void moveFileToInboundFolder() throws IOException {
        LOG.debug("Try to move file from temp to inbound folder.");
//        EDIFileWS ediFileWS = orderSessionBean.getEDIFileWS(ediFileDTO);
        EDITypeWS ediTypeWS = webServicesSessionSpringBean.getEDIType(ediTypeId);
        File sourceFile = new File(FileConstants.getEDITypePath(ediTypeWS.getEntityId(), ediTypeWS.getPath(), File.separator + generatedFileWS.getType().toString().toLowerCase()) + File.separator + generatedFileWS.getName());
        File targetFile = new File(ediTransactionBean.getEDICommunicationPath(companyId,TransactionType.OUTBOUND) + File.separator + generatedFileWS.getName());
        FileUtils.copyFile(sourceFile, targetFile);
    }

    private UserWS getUserIfExist() {
        customerAccountNumber = findField(CustomerTermination.ACT, CustomerTerminationField.UTILITY_CUST_ACCT_NR.toString());
        if (blank(customerAccountNumber)) {
            status = INVALID_FILE_STATUS;
            exceptionCode = FileConstants.CUSTOMER_NOT_FOUND;
            throw new SessionInternalError(CustomerTerminationField.UTILITY_CUST_ACCT_NR.toString() + " value must not be null");
        }
        try{
            userDTO = ediTransactionBean.findUserByAccountNumber(companyId, CustomerTerminationField.UTILITY_CUST_ACCT_NR.toString(), customerAccountNumber);
            ediFile.setUserId(userDTO.getId());
        }catch (SessionInternalError e){
            status = INVALID_FILE_STATUS;
            exceptionCode = FileConstants.CUSTOMER_NOT_FOUND;
            throw e;
        }
        return webServicesSessionSpringBean.getUserWS(userDTO.getUserId());
    }

    private void generateFile() {
        String fileName = utilityDUNS + FileConstants.UNDERSCORE_SEPARATOR + supplierDUNS + FileConstants.UNDERSCORE_SEPARATOR + System.currentTimeMillis() + FileConstants.DOT_SEPARATOR + TRANSACTION_SET;
        try {
            int generatedFileId = webServicesSessionSpringBean.generateEDIFile(ediTypeId, companyId, fileName, recordFieldsList);
            generatedFileWS = ediTransactionBean.getEDIFileWS(generatedFileId);
            generatedFileWS.setUtilityAccountNumber(customerAccountNumber);
            generatedFileWS.setUserId(userDTO.getUserId());
        } catch (SessionInternalError sie) {
            LOG.debug("Could not create outbound file for inbound file successfully " + sie.getMessage());
            status = INVALID_FILE_STATUS;
            comment = "Could not create outbound file for inbound file successfully";
            throw sie;
        }
    }

    private void createCodeRecordMap(String recType, String code) {
        codeRecordMap = new HashMap<String, String>();
        codeRecordMap.put(FileConstants.TAG_NAME_REC_ID, CustomerTermination.CDE.toString());
        codeRecordMap.put(CustomerTerminationField.REC_TYPE.toString(), recType);
        codeRecordMap.put(CustomerTerminationField.CODE.toString(), code);
        recordFieldsList.add(codeRecordMap);
    }

    public void validateTerminationFields() throws SessionInternalError {
//        See if customer exist
        String error = null;
        String exceptionCode = null;
//        UserDTO userDTO = findUserFromEDIFile(ediFileDTO);
       /* if (userDTO == null) {
            error = "Customer does not exist for this file.";
            exceptionCode = FileConstants.A76;
            SessionInternalError sie = new SessionInternalError(error);
            if(exceptionCode != null) {
                String[] errorCode = new String[]{exceptionCode};
                sie.setErrorMessages(errorCode);
            }
            throw sie;
        }*/

//        check whether reason code exist for Edi type or not
        String reasonCodeValue = findField(CustomerTermination.CDE, CustomerTerminationField.CODE.toString());
        if (blank(reasonCodeValue)) {
//            Set Exception code here.
            throw new SessionInternalError("Reason code is missing");
        }

//      Drop date is in past
        Date endServiceDate = null;
        try {
            endServiceDate = (Date) findField(CustomerTermination.DTE, CustomerTerminationField.END_SERVICE_DT.toString(), "Date", true);
            Date currentDate = companyCurrentDate();
            if (endServiceDate.before(currentDate)) {
//                    Set Exception code here.
                throw new SessionInternalError("Drop date should not be in past");
            }
        } catch (SessionInternalError sie) {
            if (sie.getMessage().contains("Mandatory field not found ")) {
                LOG.error("Drop Date is missing.");
                throw new SessionInternalError("Drop Date is missing");
            }
            LOG.error("parser exception while parsing " + CustomerTerminationField.END_SERVICE_DT);
            throw sie;
        }

        //        Drop Request with Within 5 Days of Switch Date
        if (!blank(endServiceDate)) {
            long days = Days.daysBetween(new DateTime(userWS.getCreateDatetime()).toLocalDate(), new DateTime(endServiceDate).toLocalDate()).getDays();
            if (days > 0 && days <= 5) {
                throw new SessionInternalError("Drop Request should not come within 5 days of customer creation.");
            }
        }
//        Drop Request with Within 2 Days of Meter Read
         /*TODO:fix it after confirmation.*/
        /*try {
            Date meterReadDate = (Date) findField(CustomerTermination.DTE, CustomerTerminationField.CODE.toString(), "Date", true);
        } catch (SessionInternalError sie) {
            if (sie.getMessage().contains("Mandatory field not found ")) {
                LOG.error("Meter Read Date is missing.");
                throw new SessionInternalError("Drop Date is missing");
            }
            LOG.error("parser exception while parsing " + CustomerTerminationField.END_SERVICE_DT);
            throw sie;
        }*/


//        Customer has critical care equipment on the premises.
        try {
            String criticalCareEquipment = findField(CustomerTermination.ACT, CustomerTerminationField.CUST_LIFE_SUPPORT.toString(), true);
            if (criticalCareEquipment.equals("Y")) {
                throw new SessionInternalError("Customer has critical care equipment on the premises.");
            }
        } catch (SessionInternalError sie) {
            if (sie.getMessage().contains("Mandatory field not found ")) {
                LOG.debug(CustomerTerminationField.CUST_LIFE_SUPPORT.toString() + " is mandatory");
                throw new SessionInternalError(CustomerTerminationField.CUST_LIFE_SUPPORT.toString() + " is mandatory");
            }
            throw sie;
        }
    }

    private Object getParamValue(ParameterDescription descriptionName) {
        Object value = null;
        if (descriptionName.getType().equals(ParameterDescription.Type.STR)) {
            return StringUtils.trimToNull(parameters.get(descriptionName.getName()));
        } else if (descriptionName.getType().equals(ParameterDescription.Type.INT)) {
            try {
                value = Integer.parseInt(parameters.get(descriptionName.getName()));
            } catch (NumberFormatException nfe) {
                LOG.debug("Unable to convert " + descriptionName.getName() + " to integer");
                throw new SessionInternalError("Unable to convert " + descriptionName.getName() + " to integer");
            }
        }
        return value;
    }

    Map<String, String> getRecordMap(List<Map<String, String>> recordMapList, String recordName) {
        Map<String, String> map = null;
        for (Map<String, String> recordMap : recordMapList) {
            if (recordMap.get(FileConstants.TAG_NAME_REC_ID).equals(recordName)) {
                map = recordMap;
            }
        }
        return map;
    }

    private void saveOutboundFileAndUser() {
        if (generatedFileWS != null) {
            outboundStatus=READY_TO_SEND_STATUS;
            //generatedFileWS.setComment(outboundComment);
            changeEdiFileStatus(outboundStatus, generatedFileWS);
        }

        if (userWS != null) {
            webServicesSessionSpringBean.updateUserWithCompanyId(userWS, companyId);
        }
    }

    private void changeEdiFileStatus(String statusName, EDIFileWS ediFileWS) {
        LOG.debug("In EnrollmentResponseParserTask before fetching edit status  " + statusName);
        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for (EDIFileStatusWS ediStatus : ediType.getEdiStatuses()) {
            if (ediStatus.getName().equals(statusName)) {
                statusWS = ediStatus;
            }
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

    private boolean blank(Object o) {
        return o == null;
    }
}
