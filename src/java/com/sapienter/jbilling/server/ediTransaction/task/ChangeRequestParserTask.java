package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.fileGenerator.FlatFileGenerator;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.batch.core.*;

import java.math.BigDecimal;
import java.util.*;


public class ChangeRequestParserTask extends AbstractScheduledTransactionProcessor implements StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ChangeRequestParserTask.class));
    private static final String UNDERSCORE_SEPARATOR = "_";
    private static final String DOT_SEPARATOR = ".";
    private String randomNumber = "" + System.currentTimeMillis();

    public static enum ChangeRequestRead implements FileStructure {
        KEY, HDR, NME, ACT, DTE, MTR, CDE;
    }

    public static enum ChangeRequestReadField {
        TRANS_REF_NR("TRANS_REF_NR"),
        UTILITY_CUST_ACCT_NR("UTILITY_CUST_ACCT_NR"),
        UTILITY_OLD_CUST_ACCT_NR("UTILITY_OLD_CUST_ACCT_NR"),
        METER_CYCLE("METER_CYCLE"),
        CODE("CODE"),
        DROP_META_FIELD_NAME("Termination"),
        DROPPED("Dropped"),
        TRANSACTION_SET("814"),
        RECORD_KEY("rec-id"),
        TRANSCTION_SUBSET("CS"),
        ACCEPT_REJ_FLAG("ACCEPT_REJ_FLAG"),
        REC_TYPE("REC_TYPE"),
        CYCLE_NUMBER("CYCLE_NUMBER"),
        PEAK_LOAD_CONTRIBUTION("PEAK_LOAD_CONTRIBUTION"),
        TRANSMISSION_CONTRIBUTION("TRANSMISSION_CONTRIBUTION");

        String val;

        ChangeRequestReadField(String value) {
            val = value;
        }

        public String getValue() {
            return val;
        }


    }

    protected static final ParameterDescription DONE_STATUS_NAME =
            new ParameterDescription("done_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INVALID_DATA_STATUS_NAME =
            new ParameterDescription("invalid_data_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REJECTED_STATUS_NAME =
            new ParameterDescription("rejected_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription SENT_TO_LDC_STATUS_NAME =
            new ParameterDescription("sent_to_ldc_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription READY_TO_SEND_STATUS_NAME =
            new ParameterDescription("ready_to_send_status", true, ParameterDescription.Type.STR);



    {
        descriptions.add(DONE_STATUS_NAME);
        descriptions.add(INVALID_DATA_STATUS_NAME);
        descriptions.add(REJECTED_STATUS_NAME);
        descriptions.add(SENT_TO_LDC_STATUS_NAME);
        descriptions.add(READY_TO_SEND_STATUS_NAME);
    }


    //Batch processor instance variable needs to initialize

    IWebServicesSessionBean webServicesSessionSpringBean;
    IOrderSessionBean orderSessionBean;
    IEDITransactionBean ediTransactionBean;

    private String DONE_STATUS;
    private String INVALID_DATA_STATUS;
    private String REJECTED_STATUS;
    private String SENT_TO_LDC_STATUS;
    private String READY_TO_SEND_STATUS;

    private Integer ediTypeId;
    private String supplierDUNS;
    private String utilityDUNS;
    private EDITypeWS ediType=null;

    private int companyId;
    private String exceptionCode;
    private String status;
    private String comment;

    public static final String CHANGE_REQUEST_FOR_ACCOUNT_NUMBER="REF12";
    public static final String CHANGE_REQUEST_FOR_CYCLE_NUMBER="REFTZ";
    public static final String CHANGE_REQUEST_FOR_PEAK_LOAD_CONTRIBUTION="AMTKC";
    public static final String CHANGE_REQUEST_FOR_TRANSMISSION_CONTRIBUTION="AMTKZ";

    @Override
    public String getTaskName() {
        return "Change Request parser: " + getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    protected String getJobName() {
        return Context.Name.BATCH_EDI_CHANGE_REQUEST_PROCESS.getName();
    }

    @Override
    public void preBatchConfiguration(Map jobParams) {
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        IOrderSessionBean orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);

        LOG.debug("Execute Change Request parser  plugin.");

        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME);
        if(EDI_TYPE_ID == null) {
            throw new SessionInternalError("EDI type id not valid : " + EDI_TYPE_ID);
        }

        EDITypeWS ediType = null;
        //todo : refactor
        ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);
        if (ediType == null)
            throw new SessionInternalError("EDI type id not found : " + EDI_TYPE_ID);


        jobParams.put("DONE_STATUS", new JobParameter(parameters.get(DONE_STATUS_NAME.getName())));
        jobParams.put("INVALID_DATA_STATUS", new JobParameter(parameters.get(INVALID_DATA_STATUS_NAME.getName())));
        jobParams.put("REJECTED_STATUS", new JobParameter(parameters.get(REJECTED_STATUS_NAME.getName())));
        jobParams.put("SENT_TO_LDC_STATUS", new JobParameter(parameters.get(SENT_TO_LDC_STATUS_NAME.getName())));
        jobParams.put("READY_TO_SEND_STATUS", new JobParameter(parameters.get(READY_TO_SEND_STATUS_NAME.getName())));

        jobParams.put("ediTypeId", new JobParameter(EDI_TYPE_ID.longValue()));
        jobParams.put("supplierDUNS", new JobParameter(SUPPLIER_DUNS));
        jobParams.put("utilityDUNS", new JobParameter(UTILITY_DUNS));
//        Set transaction type from suffix.
        jobParams.put("TRANSACTION_SET", new JobParameter(ediType.getEdiSuffix()));
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("EDI File Invoice Item Processor : Before Step");
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        JobParameters jobParameters = stepExecution.getJobParameters();

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        utilityDUNS = jobParameters.getString("utilityDUNS");
        supplierDUNS = jobParameters.getString("supplierDUNS");


        DONE_STATUS = jobParameters.getString("DONE_STATUS");
        INVALID_DATA_STATUS = jobParameters.getString("INVALID_DATA_STATUS");
        REJECTED_STATUS = jobParameters.getString("REJECTED_STATUS");
        SENT_TO_LDC_STATUS = jobParameters.getString("SENT_TO_LDC_STATUS");
        companyId = jobParameters.getLong("companyId").intValue();
        READY_TO_SEND_STATUS = jobParameters.getString("READY_TO_SEND_STATUS");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    private void clearData(){

        comment=null;
        userDTO=null;
        ediTypeId=null;
        status=null;
        customerAccountNumber=null;

        ediFile=null;
        escapeExceptionStatus=null;
        recordFields=null;
        recordFieldsList=null;
        recordLevelExceptionCode=null;
        exceptionCode=null;
    }

    public EDIFileWS process(EDIFileWS ediFileWS) throws Exception {
        LOG.debug("Change request Parser Task");

        try {
            if (ediFileWS.getEdiFileStatusWS().getId() == FileConstants.EDI_STATUS_PROCESSED) {
                this.ediFile = ediFileWS;
                processChangeRequestFile();
            } else {
                LOG.debug("EDIFile status set Error Detected");
                status = ediFileWS.getEdiFileStatusWS().getName();
                comment = ediFileWS.getComment();
            }
        } catch (Exception ex) {
            LOG.error(ex);
            status = (status == null) ? INVALID_DATA_STATUS : status;
            comment= EDITransactionHelper.getExceptionMessage(ex);
        }

        ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        updateStatus(ediFileWS, ediType, status);
        ediFileWS.setComment(comment);

        generateEDIFile();
        if(exceptionCode!=null)ediFileWS.setExceptionCode(exceptionCode);
        bindFields(ediFileWS);
        clearData();
        return ediFileWS;
    }

    public void processChangeRequestFile(){
        List<String> maidenRecords = new LinkedList<String>();
        maidenRecords.add(ChangeRequestRead.KEY.toString());
        maidenRecords.add(ChangeRequestRead.HDR.toString());
        maidenRecords.add(ChangeRequestRead.ACT.toString());
        maidenRecords.add(ChangeRequestRead.MTR.toString());
        parseRecords(maidenRecords, ediFile.getEDIFileRecordWSes());
        UserDAS userDAS=new UserDAS();
        validateChangeRequest();

        List<String> changeRequestsFor=getChangeRequestCodes(ediFile);

        if(changeRequestsFor.size()==0){
            exceptionCode=FileConstants.REQUIRED_INFORMATION_MISSING;
            throw new SessionInternalError("No CDE record found for in change request");
        }

        for(String changeRequestType:changeRequestsFor){
            switch (changeRequestType){
                case CHANGE_REQUEST_FOR_ACCOUNT_NUMBER:
                    updateAccountNumber();
                    break;
                case CHANGE_REQUEST_FOR_CYCLE_NUMBER:
                    validateUser();
                    updateMeterCycle();
                    break;
                case CHANGE_REQUEST_FOR_PEAK_LOAD_CONTRIBUTION:
                    validateUser();
                    updatePeakLoadContribution();
                    break;
                case CHANGE_REQUEST_FOR_TRANSMISSION_CONTRIBUTION:
                    validateUser();
                    updateTransmissionContribution();
                    break;
            }
        }

        userDAS.save(userDTO);
        status=DONE_STATUS;
    }

    private void validateUser(){
        try {
            userDTO = ediTransactionBean.findUserByAccountNumber(companyId, ChangeRequestReadField.UTILITY_CUST_ACCT_NR.getValue(), customerAccountNumber);
            ediFile.setUserId(userDTO.getId());
        }catch (Exception e){
            status=REJECTED_STATUS;
            exceptionCode=FileConstants.CONTRACT_NUMBER_INVALID;
            throw e;
        }
    }
    private void updatePeakLoadContribution(){
        String peakLoadContribution = findField(ChangeRequestRead.ACT, ChangeRequestReadField.PEAK_LOAD_CONTRIBUTION.getValue());
        if(peakLoadContribution==null){
            exceptionCode=FileConstants.REQUIRED_INFORMATION_MISSING;
            throw new SessionInternalError(ChangeRequestReadField.PEAK_LOAD_CONTRIBUTION.getValue()+" cannot be blank");
        }
        MetaFieldValue peakLoadContributionMetaField=userDTO.getCustomer().getMetaField(ChangeRequestReadField.PEAK_LOAD_CONTRIBUTION.getValue());
        userDTO.getCustomer().setMetaField(peakLoadContributionMetaField.getField(), new BigDecimal(peakLoadContribution));
    }

    private void updateTransmissionContribution(){
        String transmissionContribution = findField(ChangeRequestRead.ACT, ChangeRequestReadField.TRANSMISSION_CONTRIBUTION.getValue());
        if(transmissionContribution==null){
            exceptionCode=FileConstants.REQUIRED_INFORMATION_MISSING;
            throw new SessionInternalError(ChangeRequestReadField.TRANSMISSION_CONTRIBUTION.getValue()+" cannot be blank");
        }

        MetaFieldValue transmissionContributionMetaField=userDTO.getCustomer().getMetaField(ChangeRequestReadField.TRANSMISSION_CONTRIBUTION.getValue());
        userDTO.getCustomer().setMetaField(transmissionContributionMetaField.getField(), new BigDecimal(transmissionContribution));
    }


    private void validateChangeRequest(){

        LOG.info("Validating Change Request");
        //Check Transfer_NR
        String TRANS_REF_NR = findField(ChangeRequestRead.HDR, ChangeRequestReadField.TRANS_REF_NR.getValue());
        LOG.debug(ChangeRequestReadField.TRANS_REF_NR.getValue() + "value found : " + TRANS_REF_NR);
        try {
            ediTransactionBean.isUniqueKeyExistForFile(companyId, ediTypeId, ediFile.getId(), ChangeRequestReadField.TRANS_REF_NR.getValue(), TRANS_REF_NR, TransactionType.INBOUND);
        } catch (Exception e) {
            exceptionCode=FileConstants.DUPLICATE_REQUEST_RECEIVED;
            /*todo fix the exception code*/
            throw new SessionInternalError(e.getMessage());
        }

        LOG.info("Validating is customer utility account number");
        customerAccountNumber = findField(ChangeRequestRead.ACT, ChangeRequestReadField.UTILITY_CUST_ACCT_NR.getValue());
        if (customerAccountNumber == null) {
            exceptionCode = FileConstants.REQUIRED_INFORMATION_MISSING;
            throw new SessionInternalError(ChangeRequestReadField.UTILITY_CUST_ACCT_NR.getValue() + " cannot be blank");
        }
    }

    private void checkCustomerDropped(UserDTO userDTO){
        MetaFieldValue metaFieldValue=userDTO.getCustomer().getMetaField(ChangeRequestReadField.DROP_META_FIELD_NAME.getValue());
        if(metaFieldValue!=null){
            String terminated=(String)metaFieldValue.getValue();
            if(terminated.equals(ChangeRequestReadField.DROPPED.getValue())){
                exceptionCode=FileConstants.ACCOUNT_HAS_BEEN_DROPPED;
                throw new SessionInternalError("Customer has marked as dropped");
            }
        }
    }

    /*this method return the codes of change request through which we can identify the purpose of change request */
    private List<String> getChangeRequestCodes(EDIFileWS ediFile){

        EDIFileRecordWS[] recordList = ediFile.getEDIFileRecordWSes();
        List<String> result=new ArrayList<>();
        for(EDIFileRecordWS ediFileRecordWS: recordList){
            if(ediFileRecordWS.getHeader().equals(ChangeRequestRead.CDE.toString())){
                Map<String, String> CDEFields = parseRecord(ediFileRecordWS);
                result.add(findField(CDEFields, ChangeRequestReadField.CODE.getValue(), true));
            }
        }
        return result;
    }

    private void updateAccountNumber(){
        LOG.debug("Updating customer account number ");
        String oldAccountNumber=findField(ChangeRequestRead.ACT, ChangeRequestReadField.UTILITY_OLD_CUST_ACCT_NR.getValue());
        if(oldAccountNumber==null){
            exceptionCode=FileConstants.REQUIRED_INFORMATION_MISSING;
            throw new SessionInternalError(ChangeRequestReadField.UTILITY_OLD_CUST_ACCT_NR.getValue()+" cannot be blank");
        }

        LOG.debug("Updating customer old account number "+oldAccountNumber);
        //checking is user already available for change account number
        UserDTO existingUser = new UserDAS().findUserByAccountNumber(companyId, ChangeRequestReadField.UTILITY_CUST_ACCT_NR.getValue(), customerAccountNumber);
        if(existingUser!=null){
            status=REJECTED_STATUS;
            exceptionCode=FileConstants.CONTRACT_NUMBER_INVALID;
            throw new SessionInternalError("User already exist for account number "+customerAccountNumber);
        }
        try{
            userDTO = ediTransactionBean.findUserByAccountNumber(companyId, ChangeRequestReadField.UTILITY_CUST_ACCT_NR.getValue(), oldAccountNumber);
        }catch (Exception e){
            status=REJECTED_STATUS;
            exceptionCode=FileConstants.CONTRACT_NUMBER_INVALID;
            throw e;
        }
        checkCustomerDropped(userDTO);
        //Code for updating ait metafield.
        for(CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField:userDTO.getCustomer().getCustomerAccountInfoTypeMetaFields()){
            if(customerAccountInfoTypeMetaField.getMetaFieldValue().getField().getName().equals(ChangeRequestReadField.UTILITY_CUST_ACCT_NR.getValue())){
                MetaFieldValue accountNumberMetaField=customerAccountInfoTypeMetaField.getMetaFieldValue();
                LOG.debug("Updating customer account number to  "+customerAccountNumber);
                accountNumberMetaField.setValue(customerAccountNumber);
                userDTO.getCustomer().setAitMetaField(accountNumberMetaField, null);
                break;
            }
        }

    }

    private void updateMeterCycle(){
        try{
            userDTO = ediTransactionBean.findUserByAccountNumber(companyId, ChangeRequestReadField.UTILITY_CUST_ACCT_NR.getValue(), customerAccountNumber);
            LOG.debug("Find customer " + userDTO);
        }catch (Exception e){
            status=REJECTED_STATUS;
            exceptionCode=FileConstants.CONTRACT_NUMBER_INVALID;
            LOG.error("Customer not found in the system. EDI File Id :" + ediFile.getId());
            throw e;
        }

        MetaFieldValue cycleNumberMetaField=userDTO.getCustomer().getMetaField(ChangeRequestReadField.CYCLE_NUMBER.getValue());
        Integer cycleNumber = (Integer)findField(ChangeRequestRead.MTR, ChangeRequestReadField.METER_CYCLE.getValue(), "Integer", true);
        userDTO.getCustomer().setMetaField(cycleNumberMetaField.getField(), cycleNumber);
        checkCustomerDropped(userDTO);

        DateTime currentDate=new DateTime();
        DateTime rateChangeDate=ediTransactionBean.getRateChangeDate(currentDate, cycleNumber, ediFile.getEntityId());

        MetaFieldValue rateChangeDateMetaField=userDTO.getCustomer().getMetaField(FileConstants.CUSTOMER_RATE_CHANGE_DATE_META_FIELD_NAME);
        userDTO.getCustomer().setMetaField(rateChangeDateMetaField.getField(), rateChangeDate.toDate());
    }

    void generateEDIFile(){
        if (recordFields == null) {
            LOG.debug("recordField null");
            return;
        }
        List<Map<String, String>> recordMapList=new ArrayList<>();
        Map<String, String> keyRecord=recordFields.get(ChangeRequestRead.KEY.toString());
        keyRecord.put(ChangeRequestReadField.RECORD_KEY.getValue(), ChangeRequestRead.KEY.toString());
        keyRecord.put(ChangeRequestReadField.TRANSCTION_SUBSET.name(), ChangeRequestReadField.TRANSCTION_SUBSET.getValue());

        Map<String, String> hdrRecord=recordFields.get(ChangeRequestRead.HDR.toString());
        hdrRecord.put(ChangeRequestReadField.RECORD_KEY.getValue(), ChangeRequestRead.HDR.toString());
        Map<String, String> actRecord=recordFields.get(ChangeRequestRead.ACT.toString());
        actRecord.put(ChangeRequestReadField.RECORD_KEY.getValue(), ChangeRequestRead.ACT.toString());
        /*TODO need to fix DTE and MTR Record should be reoved*/
        Map<String, String> dteRecord=new HashMap<>();
        dteRecord.put(ChangeRequestReadField.RECORD_KEY.getValue(), ChangeRequestRead.DTE.toString());
        Map<String, String> mtrRecord=recordFields.get(ChangeRequestRead.MTR.toString());
        mtrRecord.put(ChangeRequestReadField.RECORD_KEY.getValue(), ChangeRequestRead.MTR.toString());
        Map<String, String> cdeRecord=new HashMap<>();
        cdeRecord.put(ChangeRequestReadField.RECORD_KEY.getValue(), ChangeRequestRead.CDE.toString());
        actRecord.put(ChangeRequestReadField.ACCEPT_REJ_FLAG.getValue(), exceptionCode==null? "A" :"R");
        cdeRecord.put(ChangeRequestReadField.REC_TYPE.getValue(), exceptionCode==null? "C" :"R");

        if(exceptionCode!=null){
            cdeRecord.put(ChangeRequestReadField.CODE.getValue(), exceptionCode);
        }

        recordMapList.add(keyRecord);
        recordMapList.add(hdrRecord);
        recordMapList.add(actRecord);
        recordMapList.add(dteRecord);
        recordMapList.add(mtrRecord);
        recordMapList.add(cdeRecord);

        String FILE_NAME = utilityDUNS + UNDERSCORE_SEPARATOR + supplierDUNS + UNDERSCORE_SEPARATOR + randomNumber + DOT_SEPARATOR + ediType.getEdiSuffix();
        FileFormat fileFormat = FileFormat.getFileFormat(ediTypeId);
        FlatFileGenerator generator = new FlatFileGenerator(fileFormat, companyId, FILE_NAME, recordMapList);
        EDIFileDTO ediFileDTO = generator.validateAndSaveInput();
        EDIFileWS fileWS = ediTransactionBean.getEDIFileWS(ediFileDTO.getId());
        fileWS.setUtilityAccountNumber(customerAccountNumber);
        if(userDTO!=null){
            fileWS.setUserId(userDTO.getUserId());
        }
        updateStatus(fileWS, ediType, READY_TO_SEND_STATUS);
//        fileWS.setComment(comment);
        webServicesSessionSpringBean.saveEDIFileRecord(fileWS);

    }

    public void updateStatus(EDIFileWS ediFileWS, EDITypeWS ediType, String status){
        EDIFileStatusWS statusWS = null;
        for(EDIFileStatusWS ediStatus : ediType.getEdiStatuses()){
            if(ediStatus.getName().equals(status)){
                statusWS = ediStatus;
            }
        }
        ediFileWS.setEdiFileStatusWS(statusWS);
    }

}
