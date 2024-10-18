package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.charges.AbstractCharges;
import com.sapienter.jbilling.server.ediTransaction.charges.PassThroughLineLossCharge;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileFieldDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileRecordDTO;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.IInvoiceSessionBean;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import jbilling.RouteService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.batch.core.*;
import org.springframework.batch.item.ExecutionContext;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MeterReadParserTask extends AbstractScheduledTransactionProcessor implements StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(MeterReadParserTask.class));

    private ExecutionContext executionContext = null;


    public static enum MeterRead implements FileStructure {
        KEY, HDR, UMR, QTY, REA, MTR;
    }

    public static enum MeterReadField {
        START_SERVICE_DT,
        END_SERVICE_DT,
        TRANS_REF_NR,
        UTILITY_CUST_ACCT_NR,
        USAGE_TYPE,
        READ_CONSUMPTION,       // REA usage field
        TOTAL_CONSUMPTION,     //QTY usage field (this one should be used for calculation)
        FINAL_IND,
        COMMODITY,
        SUM,
        SUPPLIER_RATE_CD,
        edi_file_id,
        ORG_867_TRAN_NR,
        INTERVAL_DT,
        INTERVAL_TIME,
        INTERVAL_TYPE,
        QTY_QUALIFIER,
        NEXT_READ_DT
    }

    protected static final ParameterDescription DONE_STATUS_NAME =
            new ParameterDescription("done_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REPLACEMENT_STATUS_NAME =
            new ParameterDescription("replacement_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INVALID_DATA_STATUS_NAME =
            new ParameterDescription("invalid_data_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription CANCELLATION_STATUS_NAME =
            new ParameterDescription("cancellation_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription HISTORICAL_RECORD_STATUS_NAME =
            new ParameterDescription("historical_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REJECTED_STATUS_NAME =
            new ParameterDescription("rejected_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription DEPRECATED_STATUS_NAME =
            new ParameterDescription("deprecated_status", true, ParameterDescription.Type.STR);

    public static final String  METER_READ_ORIGINAL_RECORD = "00";
    public static final String METER_READ_CANCELLATION_RECORD = "01";
    public static final String METER_READ_REPLACEMENT_RECORD = "05";
    public static final String METER_READ_HISTORICAL_RECORD = "52";
    public static final Integer METER_READ_DATE_TOLERANCE = 3;
    public static final String METER_READ_RECORD_TYPE = "867_PURPOSE_CD";

    {
        descriptions.add(DONE_STATUS_NAME);
        descriptions.add(REPLACEMENT_STATUS_NAME);
        descriptions.add(INVALID_DATA_STATUS_NAME);
        descriptions.add(CANCELLATION_STATUS_NAME);
        descriptions.add(HISTORICAL_RECORD_STATUS_NAME);
        descriptions.add(REJECTED_STATUS_NAME);
        descriptions.add(DEPRECATED_STATUS_NAME);
    }


    //Batch processor instance variable needs to initialize

    IWebServicesSessionBean webServicesSessionSpringBean;
    IEDITransactionBean ediTransactionBean;
    IOrderSessionBean orderSessionBean;
    private RouteService routeService;
    IInvoiceSessionBean invoiceSessionBean=Context.getBean(Context.Name.INVOICE_SESSION);
    private String DONE_STATUS;
    private String REPLACEMENT_STATUS;
    private String INVALID_DATA_STATUS;
    private String CANCELLATION_STATUS;
    private String HISTORICAL_RECORD_STATUS;
    private String REJECTED_STATUS;
    private String DEPRECATED_STATUS;

    private Integer ediTypeId;
    private String supplierDUNS;
    private String utilityDUNS;

    private int companyId;
    private String comment;
    private String customerType;
    private Map<String, Object> summaryDetail=new HashMap<String, Object>();
    protected Integer changeRequestTypeId;
    private Boolean isFinalMeterRead=false;
    private ItemDTO item;
    private String commodity;

    // valid meter read status
    private String[] statuses=new String[]{"Done", "EXP001", "EXP002", "Deprecated", "Historical Meter Read"};

    @Override
    public String getTaskName() {
        return "Meter read parser: " + getEntityId() + ", task Id: " + getTaskId();
    }

    @Override
    protected String getJobName() {
        return Context.Name.BATCH_EDI_METER_TRANSACTION_PROCESS.getName();
    }

    @Override
    public void preBatchConfiguration(Map jobParams) {
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

        LOG.debug("Execute MeadReadParserTask plugin.");

        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME);
        changeRequestTypeId = (Integer) companyMetaFieldValueMap.get(FileConstants.CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME);
        if(EDI_TYPE_ID == null) {
            throwException("EDI type id not valid", null, REJECTED_STATUS);
        }

        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);
        if (ediType == null)
            throwException("EDI type id not found: " + EDI_TYPE_ID, null, REJECTED_STATUS);

        jobParams.put("DONE_STATUS", new JobParameter(parameters.get(DONE_STATUS_NAME.getName())));
        jobParams.put("REPLACEMENT_STATUS", new JobParameter(parameters.get(REPLACEMENT_STATUS_NAME.getName())));
        jobParams.put("INVALID_DATA_STATUS", new JobParameter(parameters.get(INVALID_DATA_STATUS_NAME.getName())));
        jobParams.put("CANCELLATION_STATUS", new JobParameter(parameters.get(CANCELLATION_STATUS_NAME.getName())));
        jobParams.put("HISTORICAL_RECORD_STATUS", new JobParameter(parameters.get(HISTORICAL_RECORD_STATUS_NAME.getName())));
        jobParams.put("REJECTED_STATUS", new JobParameter(parameters.get(REJECTED_STATUS_NAME.getName())));
        jobParams.put("DEPRECATED_STATUS", new JobParameter(parameters.get(DEPRECATED_STATUS_NAME.getName())));

        jobParams.put("ediTypeId", new JobParameter(EDI_TYPE_ID.longValue()));
        jobParams.put("changeRequestTypeId", new JobParameter(changeRequestTypeId.longValue()));
        jobParams.put("supplierDUNS", new JobParameter(SUPPLIER_DUNS));
        jobParams.put("utilityDUNS", new JobParameter(UTILITY_DUNS));
//        Set transaction type from suffix.
        jobParams.put("TRANSACTION_SET", new JobParameter(ediType.getEdiSuffix()));
    }

    public void processFile(EDIFileWS ediFileWS, String escapeExceptionStatus) throws Exception{
        this.escapeExceptionStatus=escapeExceptionStatus;
        this.ediFile = ediFileWS;
        setMetaFieldValues(companyId);

        try {
            processMeterReadFile();
        } catch (Exception ex) {
            LOG.error(ex);
            status = (status == null) ? INVALID_DATA_STATUS : status;
            comment = ex.getMessage();
        }
        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for(EDIFileStatusWS ediStatus : ediType.getEdiStatuses()){
            if(ediStatus.getName().equals(status)){
                statusWS = ediStatus;
                break;
            }
        }
        ediFileWS.setEdiFileStatusWS(statusWS);
        ediFileWS.setComment(comment);
        bindFields(ediFileWS);
        if(exceptionCode!=null)ediFileWS.setExceptionCode(exceptionCode);
    }

    public void bindPluginParameter(Map<String, String> pluginParameter){
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);
        routeService = Context.getBean(Context.Name.ROUTE_SERVICE);

        companyId = Integer.parseInt(pluginParameter.get("companyId"));
        DONE_STATUS = pluginParameter.get("done_status");
        REPLACEMENT_STATUS = pluginParameter.get("replacement_status");
        INVALID_DATA_STATUS = pluginParameter.get("invalid_data_status");
        CANCELLATION_STATUS = pluginParameter.get("cancellation_status");
        HISTORICAL_RECORD_STATUS = pluginParameter.get("historical_status");
        REJECTED_STATUS = pluginParameter.get("rejected_status");
        DEPRECATED_STATUS = pluginParameter.get("deprecated_status");
        setMetaFieldValues(companyId);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("EDI File Invoice Item Processor: Before Step");
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);
        routeService = Context.getBean(Context.Name.ROUTE_SERVICE);
        JobParameters jobParameters = stepExecution.getJobParameters();

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        changeRequestTypeId = jobParameters.getLong("changeRequestTypeId").intValue();
        utilityDUNS = jobParameters.getString("utilityDUNS");
        supplierDUNS = jobParameters.getString("supplierDUNS");


        DONE_STATUS = jobParameters.getString("DONE_STATUS");
        REPLACEMENT_STATUS = jobParameters.getString("REPLACEMENT_STATUS");
        INVALID_DATA_STATUS = jobParameters.getString("INVALID_DATA_STATUS");
        CANCELLATION_STATUS = jobParameters.getString("CANCELLATION_STATUS");
        HISTORICAL_RECORD_STATUS = jobParameters.getString("HISTORICAL_RECORD_STATUS");
        REJECTED_STATUS = jobParameters.getString("REJECTED_STATUS");
        DEPRECATED_STATUS = jobParameters.getString("DEPRECATED_STATUS");
        companyId = jobParameters.getLong("companyId").intValue();
        executionContext = stepExecution.getExecutionContext();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    private void clearData(){

        comment=null;
        userDTO=null;
        startDate=null;
        endDate=null;
        customerAccountNumber=null;
        summaryDetail=new HashMap<String, Object>();
        changeRequestTypeId=null;
        isFinalMeterRead=false;
        item=null;
        commodity=null;
        status=null;

        ediFile=null;
        escapeExceptionStatus=null;
        recordFields=null;
        recordFieldsList=null;
        recordLevelExceptionCode=null;
        exceptionCode=null;
        customerType=null;
    }

    public EDIFileWS process(EDIFileWS ediFileWS) throws Exception {

        LOG.debug("Meter Read Task Process Method");
        LOG.debug("Meter Read : "+ediFileWS.getName());
        try {
            if (ediFileWS.getEdiFileStatusWS().getId() == FileConstants.EDI_STATUS_PROCESSED) {
                this.ediFile = ediFileWS;
                setMetaFieldValues(companyId);
                processMeterReadFile();
            }
        } catch (Exception ex) {
            LOG.error(ex);
            status = (status == null) ? INVALID_DATA_STATUS : status;
            comment= EDITransactionHelper.getExceptionMessage(ex);
        }

        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for(EDIFileStatusWS ediStatus : ediType.getEdiStatuses()){
            if(ediStatus.getName().equals(status)){
                statusWS = ediStatus;
            }
        }
        if(statusWS!=null) ediFileWS.setEdiFileStatusWS(statusWS);
        if(comment!=null) ediFileWS.setComment(comment);
        if(exceptionCode!=null) ediFileWS.setExceptionCode(exceptionCode);

        // Set parameters required to reprocess invoice read if waiting for 867.
        LOG.debug("Start Date : " + executionContext.get(MeterReadField.START_SERVICE_DT.toString()));
        executionContext.put(MeterReadField.START_SERVICE_DT.toString(), startDate);
        LOG.debug("end Date : " + executionContext.get(MeterReadField.END_SERVICE_DT.toString()));
        executionContext.put(MeterReadField.END_SERVICE_DT.toString(), endDate);
        LOG.debug("customer Account Number : "+ executionContext.get(MeterReadField.UTILITY_CUST_ACCT_NR.toString()));
        executionContext.put(MeterReadField.UTILITY_CUST_ACCT_NR.toString(), customerAccountNumber);
        if(statusWS!=null) executionContext.put("FILE_STATUS", statusWS.getName());

        String newRecordType=findField(MeterRead.HDR, METER_READ_RECORD_TYPE, true);
        executionContext.put("RECORD_TYPE", newRecordType);
        executionContext.put("FILE_ID", ediFileWS.getId());
        executionContext.put("ediFile", ediFileWS);

        bindFields(ediFileWS);
        clearData();
        return ediFileWS;
    }

    public void processMeterReadFile(){
        ediTypeId = (Integer) companyMetaFieldValueMap.get(FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME);
        changeRequestTypeId=(Integer) companyMetaFieldValueMap.get(FileConstants.CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME);
        if(ediTypeId == null) {
            throwException("Configuration issue: EDI type id not configured for meter read: " + EDI_TYPE_ID, null, REJECTED_STATUS);
        }

        List<String> maidenRecords = new LinkedList<String>();
        maidenRecords.add(MeterRead.KEY.toString());
        maidenRecords.add(MeterRead.HDR.toString());
        parseRecords(maidenRecords, ediFile.getEDIFileRecordWSes());


        LOG.debug("Binding customer account number");
        customerAccountNumber = findField(MeterRead.HDR, MeterReadField.UTILITY_CUST_ACCT_NR.toString());
        LOG.info("Validating customer exist");
        if(customerAccountNumber==null){
            throwException("Account Id not found in Meter read file. EDI File Id: " + ediFile.getId(), FileConstants.METER_READ_UNKNOWN_ACCOUNT_EXP_CODE, REJECTED_STATUS);
            LOG.error("Account Id not found in Meter read file. EDI File Id: " + ediFile.getId());
        }

        /*binding meter read start and end date*/
        findMeterReadPeriod();
        /* Binding user */
        userDTO=findUser();

        //Validating meter read record
        validateMeterRecord();

        if (ediTransactionBean.hasPlanSendRateChangeDaily(userDTO)) {
            status = ediFile.getEdiFileStatusWS().getName();
            comment = "Can't Create Order Because Customer Subscribed Day Ahead Product Plan";
            return;
        }
        UserWS user = new UserBL(userDTO.getId()).getUserWS();
        customerType = getCustomerType(user);
        String newRecordType=findField(MeterRead.HDR, METER_READ_RECORD_TYPE, true); // Original, cancellation, replacement
        if(newRecordType.equals(METER_READ_ORIGINAL_RECORD)){
            finalRecord();
            Integer orderId=createOriginalOrder();
            LOG.debug("User : "+userDTO.getId()+"==File="+ediFile.getName()+"==Statue : Done===order :"+orderId);

            postMeterReadProcess();
            status=DONE_STATUS;
        }else if(newRecordType.equals(METER_READ_CANCELLATION_RECORD)){
            LOG.info("Cancellation record");

            LOG.debug(" binding existing meter read records");

            EDIFileDTO originalRecord=findOriginalRecord(METER_READ_ORIGINAL_RECORD);

            EDIFileDTO cancellationRecord=getExistingRecord(METER_READ_CANCELLATION_RECORD);
            if(cancellationRecord!=null){
                throwException("There is duplicate cancellation request", FileConstants.METER_READ_DUPLICATE_CANCELLATION_EXP_CODE, INVALID_DATA_STATUS);
            }
            EDIFileDTO replacementRecord=getExistingRecord(METER_READ_REPLACEMENT_RECORD);
            if(replacementRecord!=null){
                exceptionCode=FileConstants.METER_READ_CANCELLATION_AFTER_PANDING_REPLACEMENT_EXP_CODE;
            }

            if(escapeExceptionStatus==null){
                status=CANCELLATION_STATUS;
            }else{
                status=DONE_STATUS;
            }

        }else if(newRecordType.equals(METER_READ_REPLACEMENT_RECORD)){
            LOG.info("Replacement Record");

            LOG.debug("binding existing meter read records");
            EDIFileDTO replacementRecord=getExistingRecord(METER_READ_REPLACEMENT_RECORD);
            if(replacementRecord!=null){
                throwException("Duplicate meter read replacement transaction.", FileConstants.METER_READ_DUPLICATE_REPLACEMENT_EXP_CODE, INVALID_DATA_STATUS);
            }

            EDIFileDTO cancellationRequest=getExistingRecord(METER_READ_CANCELLATION_RECORD);

            if(cancellationRequest==null && escapeExceptionStatus==null) {
                exceptionCode = FileConstants.MISSING_CANCELLATION_FOR_REPLACEMENT_EXP_CODE;
                status = REPLACEMENT_STATUS;
            }else{
                if(cancellationRequest!=null){
                    updateFileStatus(cancellationRequest.getId(), DONE_STATUS);
                }
                createReplacementOrder();
                status=DONE_STATUS;
            }
        }else if(newRecordType.equals(METER_READ_HISTORICAL_RECORD)){
            LOG.info("Historical Record");
            status=HISTORICAL_RECORD_STATUS;
        }else{
            status=INVALID_DATA_STATUS;
            comment="Record Type is not a valid record type. It should be 00, 01, 05 or 52 ";
        }
    }

    private void validateMeterRecord() {

        LOG.info("Validating Meter Read record");
        //Check Transfer_NR
        String TRANS_REF_NR = findField(MeterRead.HDR, MeterReadField.TRANS_REF_NR.toString());
        LOG.debug(MeterReadField.TRANS_REF_NR.toString() + "value found: " + TRANS_REF_NR);
        try {
            ediTransactionBean.isUniqueKeyExistForFile(companyId, ediTypeId, ediFile.getId(), MeterReadField.TRANS_REF_NR.toString(), TRANS_REF_NR, TransactionType.INBOUND);
        } catch (Exception e) {
            throwException(e.getMessage(), FileConstants.METER_READ_DUPLICATE_TRANSACTION_EXP_CODE, REJECTED_STATUS);
        }

        isFinalMeterRead=isFinalMeterRead();

        //if meter type is historical then ignore Total drop customer, Consumption, OverlapOrOvergap validation check
        String newRecordType=findField(MeterRead.HDR, METER_READ_RECORD_TYPE, true);
        if(newRecordType.equals(METER_READ_HISTORICAL_RECORD)){
            return;
        }

        LOG.debug("Meter read start date "+startDate);
        if(startDate==null){
            throwException("Meter read should contains start date", null, REJECTED_STATUS, true);
        }

        LOG.debug("Meter read end date "+ endDate);
        if(endDate==null){
            throwException("Meter read should contains end date", null, REJECTED_STATUS, true);
        }

        MetaFieldValue metaFieldValue=userDTO.getCustomer().getMetaField("Termination");
        if(metaFieldValue!=null && metaFieldValue.getValue()!=null){
            String terminatedValue=(String)metaFieldValue.getValue();
            if(terminatedValue.equals("Dropped") || terminatedValue.equals("Esco Rejected")){
                throwException("Customer is dropped. We can not process meter read for dropped customer", FileConstants.METER_READ_DROP_CUSTOMER_EXP_CODE, INVALID_DATA_STATUS);
            }
        }

        validateOverlapOrOvergap();
    }

    private Map<String, Object> getSummaryMeterData(EDIFileWS ediFile){

        EDIFileRecordWS[] recordList = ediFile.getEDIFileRecordWSes();
        Map<String, String> UMRFields=new HashMap<String, String>();
        Map<String, String> QTYFields=new HashMap<String, String>();
        Map<String, Object> meterFields = new HashMap<String, Object>();

        Boolean hasSummaryRecord=false;

        //checking is file contains summary record.
        for(EDIFileRecordWS ediFileRecordWS: recordList){
            if(ediFileRecordWS.getHeader().equals(MeterRead.UMR.toString())){
                UMRFields = parseRecord(ediFileRecordWS);
                if(UMRFields.get(MeterReadField.USAGE_TYPE.toString()).equals(MeterReadField.SUM.toString())){
                    QTYFields = parseRecord(recordList[ediFileRecordWS.getRecordOrder()]);
                    hasSummaryRecord=true;
                    break;
                }
            }
        }

        if(!hasSummaryRecord){
            throwException("No Summary Record found ", null, REJECTED_STATUS, true);
        }

        meterFields.put(MeterReadField.USAGE_TYPE.toString(), findField(UMRFields, MeterReadField.USAGE_TYPE.toString(), true));
        meterFields.put(MeterReadField.TOTAL_CONSUMPTION.toString(), findField(QTYFields, MeterReadField.TOTAL_CONSUMPTION.toString(), "BigDecimal", true));
        meterFields.put(MeterReadField.START_SERVICE_DT.toString(), findField(UMRFields, MeterReadField.START_SERVICE_DT.toString(), "Date", true));
        meterFields.put(MeterReadField.END_SERVICE_DT.toString(), findField(UMRFields, MeterReadField.END_SERVICE_DT.toString(),"Date", true));
        meterFields.put(MeterReadField.QTY_QUALIFIER.toString(), findField(QTYFields, MeterReadField.QTY_QUALIFIER.toString(), false));

        return meterFields;
    }

    private BigDecimal getTotalConsumption(EDIFileWS ediFile){
        //finding interval QTY record
        List<EDIFileRecordWS> ediFileRecordWSes=Arrays.asList(ediFile.getEDIFileRecordWSes()).stream().filter((EDIFileRecordWS ediFileRecordWS) -> ediFileRecordWS.getHeader().equals("QTY")).collect(Collectors.toList());
        ediFileRecordWSes.remove(0);
        BigDecimal totalConsumption=BigDecimal.ZERO;

        // calculating total of interval record consumption
        for(EDIFileRecordWS ediFileRecordWS: ediFileRecordWSes){
            for(EDIFileFieldWS ediFileFieldWS:ediFileRecordWS.getEdiFileFieldWSes()){
                if (ediFileFieldWS.getKey().equals(MeterReadField.TOTAL_CONSUMPTION.toString())){
                    totalConsumption = totalConsumption.add(new BigDecimal(ediFileFieldWS.getValue()));
                }
            }
        }

        return totalConsumption;
    }


    public void finalRecord(){
        if(isFinalMeterRead){
            LOG.debug("Final Meter Read");
            UserWS userWS = webServicesSessionSpringBean.getUserWS(userDTO.getId());
            Boolean isTerminatedOrDropped= isUserAlreadyTerminatedOrDropped(userWS);
            if(isTerminatedOrDropped){
                // making customer dropped
                userWS=AbstractScheduledTransactionProcessor.updateTerminationMetaField(userWS, FileConstants.DROPPED, null);
                updateUser(userWS);
                List<OrderDTO> subscriptions = new OrderDAS()
                        .findByUserSubscriptions(userDTO.getId());
                if(subscriptions.size()>0){
                    //Need to uncomment
                    //TODO need to be refactor: as we know there is one subscription order for one LDC Company. But we need to add check for plan .
                    OrderDTO orderDTO=subscriptions.get(0);
                    orderDTO.setActiveUntil(endDate);
                    new OrderDAS().save(orderDTO);
                }

                EventManager.process(new CustomerDroppedEvent(userWS.getEntityId(), userWS.getCustomerId(), endDate));

                try {
                    userWS.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isFinalMeterRead() {
        String finalField = findField(MeterRead.HDR, MeterReadField.FINAL_IND.toString(), false);
        if (finalField != null && finalField.equals("F")) {
            return true;
        }
        return false;
    }

    public EDIFileDTO getExistingRecord(String recordType){
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("ediType.id", ediTypeId));
        conjunction.add(Restrictions.eq("entity.id", companyId));
        conjunction.add(Restrictions.eq("type", TransactionType.INBOUND));
        conjunction.add(Restrictions.in("status.name", statuses));
        conjunction.add(Restrictions.eq("utilityAccountNumber", customerAccountNumber));
        conjunction.add(Restrictions.eq("startDate", startDate));
        conjunction.add(Restrictions.eq("endDate", endDate));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", FileConstants.METER_READ_RECORD_TYPE));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", recordType));
        conjunction.add(Restrictions.not(Restrictions.eq("id", ediFile.getId())));
        List<EDIFileDTO> ediFileDTOList= new EDIFileDAS().findEDIFiles(conjunction);
        if(ediFileDTOList.size()>0){
            return ediFileDTOList.get(0);
        }
        return null;
    }

    public void validateOverlapOrOvergap(){
        LOG.info("Validating record date with Previous record date");
        String newRecordType=findField(MeterRead.HDR, METER_READ_RECORD_TYPE, true);
        //validating a meter read service date is not before the customer enrollment agreement date

        UserBL userBL=new UserBL(userDTO);
        UserWS userWS=userBL.getUserWS();

        for(MetaFieldValueWS metaFieldValueWS:userWS.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals(FileConstants.ACTUAL_START_DATE)){
                Date customerEnrollmentAgreementDate=(Date)metaFieldValueWS.getValue();
                if(customerEnrollmentAgreementDate.compareTo(startDate)>0){
                    throwException("MeterRead start date should be greater than the customer enrollment agreement date", FileConstants.METER_READ_ENROLLMENT_DATE_SHOULD_BE_GREATER_THEN_AGREEMENT_DATE_EXP_CODE, INVALID_DATA_STATUS);
                }
                break;
            }
        }

        try {
            userWS.close();
        } catch (Exception e) {
            LOG.debug("Exception while clean up: "+e);
        }

        DateTime currentRecordStartDateTime = new DateTime(startDate);
        DateTime currentRecordEndDateTime =new DateTime(endDate);
        Integer tolerance=METER_READ_DATE_TOLERANCE;
        if(Days.daysBetween(currentRecordStartDateTime.toLocalDate(), currentRecordEndDateTime.toLocalDate()).getDays() <= tolerance ){
            throwException("Difference between  MeterRead start date and end date should be greater than the tolerance("+tolerance+")", FileConstants.METER_READ_DIFFRENCE_BETRWEEN_START_AND_END_DATE_GREATER_THEN_TOLLERANCE_EXP_CODE, INVALID_DATA_STATUS);

        }

        //finding item by commodity metafield.
        ItemDAS itemDAS=new ItemDAS();

        for(EDIFileRecordWS ediFileRecordWS:ediFile.getEDIFileRecordWSes()){
            if(ediFileRecordWS.getHeader().equals(MeterRead.KEY.toString())){
                Map<String, String> keyRecord= parseRecord(ediFileRecordWS);
                commodity=findField(keyRecord, MeterReadField.COMMODITY.toString(), true);
                item = itemDAS.findByMetaFieldNameAndValue(companyId, MeterReadField.COMMODITY.toString(), commodity);
                break;
            }
        }

        // Overlap and Overgap condition should be check if it is an original meter read and not final meter read. Because final meter read can come for drop customer in past period
        if(newRecordType.equals(METER_READ_ORIGINAL_RECORD) && !isFinalMeterRead()){

            OrderDTO latestOrder=new OrderDAS().findLastOrder(userDTO.getId(), item.getId());
            //if order is null order its active since and active until date is null then escape overlap and overgap
            if(latestOrder==null || latestOrder.getActiveSince()==null || latestOrder.getActiveUntil()==null){
                return;
            }

            Integer days=Days.daysBetween(new DateTime(latestOrder.getActiveUntil()).toLocalDate(), currentRecordStartDateTime.toLocalDate()).getDays();

            if(days > 0 && Math.abs(days) > tolerance){
                comment="There is gap with last meter read. Last Meter Read period was "+dateFormat.print(new DateTime(latestOrder.getActiveSince()))+" to "+dateFormat.print(new DateTime(latestOrder.getActiveUntil()));
//                throwException("There is gap with last meter read. Last Meter Read period was "+dateFormat.print(new DateTime(latestOrder.getActiveSince()))+" to "+dateFormat.print(new DateTime(latestOrder.getActiveUntil())), FileConstants.METER_READ_GAP_EXP_CODE, INVALID_DATA_STATUS);
            }
            if(days < 0 && Math.abs(days) > tolerance){
                throwException("There is overlap with last meter read. Last Meter Read period was "+dateFormat.print(new DateTime(latestOrder.getActiveSince()))+" to "+dateFormat.print(new DateTime(latestOrder.getActiveUntil())), FileConstants.METER_READ_OVERLAP_EXP_CODE, INVALID_DATA_STATUS);
            }
        }
    }

    /*This method create order for original meter read. If the customer is DUAL then it create order in active status else Suspended status */
    private Integer createOriginalOrder(){
        OrderDTO order=createOrderDTO();

        //create order with active status if Billing modal is DUAL else create order with SUSPENDED Status
        OrderStatusFlag orderStatusFlag=OrderStatusFlag.NOT_INVOICE;
        if(customerType.equals(FileConstants.BILLING_MODEL_DUAL)){
            //if billing modal is dual than generate Active order
            orderStatusFlag=OrderStatusFlag.INVOICE;
        }
        Integer orderStatus = new OrderStatusDAS().getDefaultOrderStatusId(
                orderStatusFlag,  userDTO.getEntity().getId());
        order.setStatusId(orderStatus);

        Integer languageId = userDTO.getLanguageIdField();
        String description = item.getDescription(languageId);

        OrderLineDTO orderLineDTO=createOrderLine(order, description, calculateTotalConsumption(ediFile), null);
        //passes through charges
        chargesProcessing(order);
        orderLineDTO.setPrice(getUnitPrice(order));
        OrderBL orderBL = new OrderBL();
        orderBL.set(order);
        return orderBL.create(userDTO.getEntity().getId(), null, order);
    }

    /**
     * This method used for do the charge configuration and apply charges..
     *
     * @param order
     * @return Nothing.
     */
    private void chargesProcessing(OrderDTO order) {
        LOG.debug("Execute charge processing");
        AbstractCharges charges = new PassThroughLineLossCharge();
        charges.doChargeConfiguration(userDTO, order, companyId, calculateTotalConsumption(ediFile), customerType);
        charges.applyCharge();
    }

    /*This method create order from the corresponding the meter read. */
    private OrderDTO createOrderDTO(){
        LOG.debug("Item : "+item.getId());
        if (item==null ){
            throwException("Item not found for commodity "+commodity, null, REJECTED_STATUS);
        }

        Map<String, Object> serviceDetail=getSummaryMeterData(ediFile);
        OrderDTO order = new OrderDTO();
        OrderPeriodDTO period = new OrderPeriodDAS().find(Constants.ORDER_PERIOD_ONCE);
        order.setOrderPeriod(period);
        OrderBillingTypeDTO type = new OrderBillingTypeDTO();
        type.setId(com.sapienter.jbilling.server.util.Constants.ORDER_BILLING_POST_PAID);
        order.setOrderBillingType(type);
        order.setCreateDate(Calendar.getInstance().getTime());
        order.setCurrency(userDTO.getCurrency());
        order.setActiveSince(startDate);
        order.setActiveUntil(endDate);
        order.setBaseUserByUserId(userDTO);

        // Order note
        Object qtyFromMeterRead = serviceDetail.get(MeterReadField.QTY_QUALIFIER.toString());
        String qtyQualifier = qtyFromMeterRead!=null?(String)qtyFromMeterRead:null;
        String orderNote = getOrderNote(qtyQualifier);
        order.setNotes(orderNote);
        order.setNotesInInvoice(1);
        EntityType[] entityType = {EntityType.ORDER};
        MetaField ediFileMetaField=new MetaFieldDAS().getFieldByName(companyId, entityType, MeterReadField.edi_file_id.toString());

        if(ediFileMetaField==null){
            throwException("Configuration issue: order should have edi_file_id meta field", null, REJECTED_STATUS);
        }

        order.setMetaField(ediFileMetaField, ediFile.getId()+"");
        return order;

    }

    /*Its return the unit price for the item. This method is only used for calcualte rate orginal meter read
     * for replacment we used price from original order */
    private BigDecimal getUnitPrice(OrderDTO order) {
        BigDecimal rate = BigDecimal.ZERO;
        LOG.debug("Customer : %s has type : %s", userDTO.getUserName(), customerType);
        Integer itemId = item.getId();
        //if billing modal is rate ready then, first get rate from latest rate-change request.
        // If there is not rate-change then get rate from customer rate metafield.
        // else get price from the customer subscribed plan
        if (customerType.equals(FileConstants.BILLING_MODEL_RATE_READY)) {
            LOG.debug("finding rate for RateReady Customer");
            rate = findRateForRateReady();

            LOG.debug("Rate from rate change : " + rate);
            if (rate != null) {
                return rate;
            }
            MetaFieldValue metaFieldValue = userDTO.getCustomer().getMetaField(FileConstants.CUSTOMER_RATE_METAFIELD_NAME);
            if (metaFieldValue != null && metaFieldValue.getValue() != null) {
                LOG.debug("Rate from customer level MF : " + metaFieldValue.getValue());
                return (BigDecimal) metaFieldValue.getValue();
            }
        }

        LOG.debug("finding rate for Customer through plan product");
        OrderBL orderBL = new OrderBL();
        orderBL.processLines(order, userDTO.getLanguage().getId(), companyId, userDTO.getId(), userDTO.getCurrencyId(), "");
        if (order.getLine(itemId) == null) {
            throwException("Order should contains item " + item.getInternalNumber(), null, REJECTED_STATUS, true);
        }
        return order.getLine(itemId).getPrice();
    }

    /*this method return the order line*/
    private OrderLineDTO createOrderLine(OrderDTO order, String description, BigDecimal quantity, BigDecimal price){

        OrderLineDTO line = new OrderLineDTO();
        line.setDescription(description);
        line.setItemId(item.getId());
        line.setQuantity(quantity);
        line.setTypeId(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ITEM);
        line.setPurchaseOrder(order);
        order.getLines().add(line);
        if(price!=null){
            line.setPrice(price);
        }

        return line;
    }

    /* This method create order for replacement meter read. If an invoice for the original meter read has benn generated then it create
     * rebill order with the suspended status else for Dual it create order in the active stauts while for other type of customer it create order in the suspended status */
    private Integer createReplacementOrder(){
        OrderDTO replacementOrder=createOrderDTO();

        Integer languageId = userDTO.getLanguageIdField();
        String description = item.getDescription(languageId);
        String cancelDescription=description+" (cancel "+dateFormat.print(new DateTime(startDate))+" to "+dateFormat.print(new DateTime(endDate))+" )";
        String rebillDescription=description+ " (rebill "+dateFormat.print(new DateTime(startDate))+" to "+dateFormat.print(new DateTime(endDate))+" )";

        EDIFileDTO originalRecord=findOriginalRecord(METER_READ_ORIGINAL_RECORD);

        OrderStatusFlag orderStatusFlag=OrderStatusFlag.NOT_INVOICE;
        OrderDTO originalOrder=new OrderDAS().findOrderByMetaFieldValue(companyId, "edi_file_id", originalRecord.getId()+"");
        if(originalOrder==null){
            throwException("No order found for meter read "+originalRecord.getId(), null, REJECTED_STATUS);
        }
        //checking is order has an generated invoice by real bill run.
        if(originalOrder.getOrderProcesses()!=null && originalOrder.getOrderProcesses().size()>0 && originalOrder.getOrderProcesses().iterator().next().getInvoice()!=null &&  originalOrder.getOrderProcesses().iterator().next().getInvoice().getIsReview()==0){
            LOG.debug("Order "+originalOrder.getId()+" has generated invoice");
            EDIFileDTO cancellationRequest=getExistingRecord(METER_READ_CANCELLATION_RECORD);
            if(cancellationRequest!=null){
                //adding cancelation line in rebill order
                EDIFileWS cancellationRequestWS=ediTransactionBean.getEDIFileWS(cancellationRequest.getId());
                createOrderLine(replacementOrder, cancelDescription, calculateTotalConsumption(cancellationRequestWS).negate(), originalOrder.getLine(item.getId()).getPrice());
            }
            //adding rebill line in rebill order
            createOrderLine(replacementOrder, rebillDescription, calculateTotalConsumption(ediFile), originalOrder.getLine(item.getId()).getPrice());
        }else{
            LOG.debug("Order "+originalOrder.getId()+" has no generated invoice");
            //adding rebill line in rebill order
            createOrderLine(replacementOrder, rebillDescription, calculateTotalConsumption(ediFile), originalOrder.getLine(item.getId()).getPrice());
            if(!customerType.equals(FileConstants.BILLING_MODEL_RATE_READY)){
                //if billing modal is other than rate ready then generate Active order
                orderStatusFlag=OrderStatusFlag.INVOICE;
            }
            finishOrder(originalRecord);
        }
        EntityType[] entityType={EntityType.ORDER};
        MetaField metaField=MetaFieldBL.getFieldByName(companyId, entityType, FileConstants.IS_REBILL_ORDER);
        if(metaField!=null){
            replacementOrder.setMetaField(metaField ,true);
        }

        Integer orderStatus = new OrderStatusDAS().getDefaultOrderStatusId(
                orderStatusFlag,  userDTO.getEntity().getId());
        replacementOrder.setStatusId(orderStatus);

        OrderBL orderBL = new OrderBL();
        orderBL.set(replacementOrder);
        return orderBL.create(userDTO.getEntity().getId(), null, replacementOrder);

    }

    /*This method return the total consumption of the meter read file*/
    private BigDecimal calculateTotalConsumption(EDIFileWS ediFileWS){

        BigDecimal totalConsumption = BigDecimal.ZERO;

        Map<String, String> headerRecord=new HashMap<>();
        getHeaderRecord(ediFileWS, headerRecord, MeterRead.HDR.toString());

        Map<String, Object> serviceDetail=getSummaryMeterData(ediFileWS);

        if(serviceDetail.get(MeterReadField.USAGE_TYPE.toString()).toString().equals(MeterReadField.SUM.toString())){
            totalConsumption=((BigDecimal)serviceDetail.get(MeterReadField.TOTAL_CONSUMPTION.toString()));
        }

        return totalConsumption;
    }

    /**
     * There is field in summary QTY record called QTY_QUALIFIER. Based on value of this field,
     * an order note should exist.
     *
     * @param qtyQualifier Field in summary QTY record. Possible values
     *                     <p/>
     *                     17 - Multi-meter roll-up where one meter unavailable
     *                     19 - Net Metering Actual
     *                     20 - unavailable (used when interval data is not available)
     *                     87 - Actual co-gen
     *                     96 - quantity/interval outside bill period
     *                     9H - Net Metering Estimated
     *                     D1 - Billed
     *                     QD - Actual
     *                     KA - Estimated
     *                     AO - Verified as Actual
     *                     CQ - Calculated Quantity
     */
    public String getOrderNote(String qtyQualifier) {
        String qualifierMeaning = null;
        if (qtyQualifier != null) {
            switch (qtyQualifier) {
                case "17":
                    qualifierMeaning = "Multi-meter roll-up where one meter unavailable";
                    break;
                case "19":
                    qualifierMeaning = "Net Metering Actual";
                    break;
                case "20":
                    qualifierMeaning = "unavailable (used when interval data is not available)";
                    break;
                case "87":
                    qualifierMeaning = "Actual co-gen";
                    break;
                case "96":
                    qualifierMeaning = "quantity/interval outside bill period";
                    break;
                case "9H":
                    qualifierMeaning = "Net Metering Estimated";
                    break;
                case "D1":
                    qualifierMeaning = "Billed";
                    break;
                case "QD":
                    qualifierMeaning = "Actual usage";
                    break;
                case "KA":
                    qualifierMeaning = "Estimated by Utility";
                    break;
                case "AO":
                    qualifierMeaning = "Verified as Actual";
                    break;
                case "CQ":
                    qualifierMeaning = "Calculated Quantity";
                    break;
            }
        }

        if (qualifierMeaning != null) {

            return qualifierMeaning + "(" + qtyQualifier + ")'";
        } else {
            return "Meter read do not have valid QTY_QUALIFIER";
        }
    }

    private BigDecimal findRateForRateReady(){
        LOG.debug("Finding rate for rate ready customer");
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("ediType.id", changeRequestTypeId));
        conjunction.add(Restrictions.eq("entity.id", companyId));
        conjunction.add(Restrictions.le("createDatetime", endDate));
        conjunction.add(Restrictions.eq("type", TransactionType.OUTBOUND));
        conjunction.add(Restrictions.eq("status.name", "Done"));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", MeterReadField.UTILITY_CUST_ACCT_NR.toString()));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", customerAccountNumber));

        EDIFileDTO changeRequest = new EDIFileDAS().findEDIFile(conjunction);
        LOG.debug("Find Change Request: "+changeRequest);

        if(changeRequest!=null){
            EDIFileWS changRequestWS=ediTransactionBean.getEDIFileWS(changeRequest.getId());
            Map<String, String> meterRecord=new HashMap<>();
            getHeaderRecord(changRequestWS, meterRecord, MeterRead.MTR.toString());
            String rateCode=findField(meterRecord, MeterReadField.SUPPLIER_RATE_CD.toString(), true);
            LOG.debug("Searched rate code: "+rateCode);
            if(rateCode!=null){
               return ediTransactionBean.getRateByRateCode(companyId, rateCode);
            }
        }

        return null;
    }


    private EDIFileDTO findOriginalRecord(String recordType){

        LOG.debug("finding original Meter read");
        EDIFileDTO originalEdiFile=null;
        String originalMeterReadId=findField(MeterRead.HDR, MeterReadField.ORG_867_TRAN_NR.toString());
        LOG.debug("Original Meter Read TRANS_REF_NR : " + originalMeterReadId);
        if(originalMeterReadId!=null){

            Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(Restrictions.eq("ediType.id", ediFile.getEdiTypeWS().getId()));
            conjunction.add(Restrictions.eq("entity.id", companyId));
            conjunction.add(Restrictions.eq("type", TransactionType.INBOUND));
            conjunction.add(Restrictions.in("status.name", statuses));
            conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", MeterReadField.TRANS_REF_NR.toString()));
            conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", originalMeterReadId));
            originalEdiFile = new EDIFileDAS().findEDIFile(conjunction);


            EDIFileWS originalMeterReadWS=null;
            String oldEDIUsageType=null;

            if(originalEdiFile!=null){
                originalMeterReadWS=new EDIFileBL(originalEdiFile).getWS();
                oldEDIUsageType=EdiUtil.findRecord(originalMeterReadWS, MeterRead.HDR.toString(), METER_READ_RECORD_TYPE);
            }

            LOG.debug("Original Meter read : "+originalEdiFile);

            //checking is a valid original meter read exit in the system for the given TRANS_REF_NR.
            if(originalEdiFile==null || (oldEDIUsageType !=null && !oldEDIUsageType.equals(METER_READ_ORIGINAL_RECORD))){
                LOG.debug("No original EDI file found for TRANS_REF_NR : "+originalMeterReadId);
                String exceptionCode=(recordType.equals(METER_READ_CANCELLATION_RECORD)? FileConstants.METER_READ_CANCELLATION_WITHOUT_ORIGINAL_CODE_EXP_CODE:FileConstants.ORIGINAL_METER_READ_FOR_REPLACEMENT_DOES_NOT_EXIST_EXP_CODE);
                throwException("No original Meter Read found for TRANS_REF_NR : "+originalMeterReadId, exceptionCode, INVALID_DATA_STATUS);
            }else{
                if(!isFileMatch(originalEdiFile)){
                    String exceptionCode=(recordType.equals(METER_READ_CANCELLATION_RECORD)? FileConstants.METER_READ_CANCELLATION_NOT_MATCH_WITH_ORIGINAL_CODE_EXP_CODE:FileConstants.MISMATCH_ORIGINAL_FOR_REPLACEMENT_EXP_CODE);
                    throwException("Original Meter Read not found of customer "+customerAccountNumber+" for period "+startDate+" to "+ endDate, exceptionCode, INVALID_DATA_STATUS);
                }
            }
        }else{
            LOG.debug("Missing original meter read trans_ref_nr ");
            originalEdiFile=getExistingRecord(METER_READ_ORIGINAL_RECORD);
            if(originalEdiFile==null){
                //NO Matching record found for customer
                String exceptionCode=(recordType.equals(METER_READ_CANCELLATION_RECORD)? FileConstants.METER_READ_CANCELLATION_WITHOUT_ORIGINAL_CODE_EXP_CODE:FileConstants.ORIGINAL_METER_READ_FOR_REPLACEMENT_DOES_NOT_EXIST_EXP_CODE);
                throwException("No original meter read found for the given period", exceptionCode, INVALID_DATA_STATUS);
            }
        }

        return originalEdiFile;

    }

    //this method match account number, start date and end date of  the current meter read with given meter read
    private Boolean isFileMatch(EDIFileDTO ediFileDTO){
        String accountNumber=null;
        String oldMeterStartDate=null;
        String oldMeterEndDate=null;

        for(EDIFileRecordDTO ediFileRecordDTO:ediFileDTO.getEdiFileRecords()){
            //matching account number
            if(ediFileRecordDTO.getEdiFileRecordHeader().equals(MeterRead.HDR.toString())){
                for(EDIFileFieldDTO ediFileFieldDTO:ediFileRecordDTO.getFileFields()){
                    if(ediFileFieldDTO.getEdiFileFieldKey().equals(MeterReadField.UTILITY_CUST_ACCT_NR.toString())){
                        accountNumber=ediFileFieldDTO.getEdiFileFieldValue();
                    }
                }
            }

            //matching start date and end date
            if(ediFileRecordDTO.getEdiFileRecordHeader().equals(MeterRead.UMR.toString())){

                for(EDIFileFieldDTO ediFileFieldDTO:ediFileRecordDTO.getFileFields()){
                    if(ediFileFieldDTO.getEdiFileFieldKey().equals(MeterReadField.START_SERVICE_DT.toString())){
                        oldMeterStartDate=ediFileFieldDTO.getEdiFileFieldValue();
                    }

                    if(ediFileFieldDTO.getEdiFileFieldKey().equals(MeterReadField.END_SERVICE_DT.toString())){
                        oldMeterEndDate=ediFileFieldDTO.getEdiFileFieldValue();
                    }
                }
                break;
            }
        }

        if(findField(MeterRead.HDR, MeterReadField.UTILITY_CUST_ACCT_NR.toString()).equals(accountNumber) && startDate.equals(dateFormat.parseDateTime(oldMeterStartDate).toDate()) && endDate.equals(dateFormat.parseDateTime(oldMeterEndDate).toDate()) ){
            return true;
        }

        LOG.debug("Current file did not match with original meter read");
        return false;
    }

    private void finishOrder(EDIFileDTO originalRecord){
        OrderDAS orderDAS=new OrderDAS();
        //Find order for original meter read and check is invoice is generated for it
        OrderDTO orderDTO=orderDAS.findOrderByMetaFieldValue(companyId, "edi_file_id", originalRecord.getId() + "");

        if(orderDTO==null){
            throwException("No order find for original meter read " + originalRecord.getId() + " in the system", null, REJECTED_STATUS, true);
        }

        //Marking order of finish status
        OrderStatusFlag orderStatusFlag=OrderStatusFlag.FINISHED;
        Integer orderStatus = new OrderStatusDAS().getDefaultOrderStatusId(
                orderStatusFlag,  userDTO.getEntity().getId());
        orderDTO.setStatusId(orderStatus);
        orderDTO.setNotes("Order is marked as depricated because corresponding replacement comes before the invoice generation for order");
        orderDAS.save(orderDTO);

        updateFileStatus(originalRecord.getId(), DEPRECATED_STATUS);
    }

    /*method will be called after successful origianl meter read process*/
     private void postMeterReadProcess(){

         //Updating customer NEXT_READ_DT when a original meter read is successfully processed
         Date nextDate = null;
         MetaField nextReadDateMetafield=MetaFieldBL.getFieldByName(companyId, new EntityType[]{EntityType.CUSTOMER}, FileConstants.CUSTOMER_NEXT_READ_DT_META_FIELD);
         if(nextReadDateMetafield!=null){
             String nextMeterReadDate=findField(MeterRead.HDR, MeterReadField.NEXT_READ_DT.toString());
             try {
                 if (StringUtils.isNotBlank(nextMeterReadDate)) {
                     nextDate = dateFormat.parseDateTime(nextMeterReadDate).toDate();
                 }
             } catch (Exception e) {
                 comment = "Invalid Date format for " + FileConstants.CUSTOMER_NEXT_READ_DT_META_FIELD + " field : " + nextMeterReadDate + ". The format should be: " + DATE_FORMAT_PATTERN;
                 return;
             }
             if (nextDate != null) userDTO.getCustomer().setMetaField(nextReadDateMetafield, nextDate);
         }
    }

    /**
     * This method find customer by utility account number and commodity.
     * @return user
     */
    private UserDTO findUser(){
        try{
            String commodity = findField(MeterRead.KEY, MeterReadField.COMMODITY.toString());
            LOG.debug("Commodity code :"+ commodity);
            try{
                commodity=EDITransactionHelper.getCommodityFromCode(commodity);
            }catch (SessionInternalError sie){
                throwException(sie.getMessage(), null, REJECTED_STATUS);
            }
            LOG.debug("Commodity :"+ commodity);
            return ediTransactionBean.findUserByAccountNumber(companyId, MeterReadField.UTILITY_CUST_ACCT_NR.toString(), customerAccountNumber, isFinalMeterRead, commodity);
        }catch (SessionInternalError e){
            LOG.error("Customer not found in the system. EDI File Id: " + ediFile.getId());
            throwException(e.getMessage(), FileConstants.METER_READ_UNKNOWN_ACCOUNT_EXP_CODE, REJECTED_STATUS);
        }
        return null;
    }

    /**
     * This method find start and end date from the summery record of the meter read.
     */
    private void findMeterReadPeriod(){
        Optional<EDIFileRecordWS> summaryOptional = Arrays.asList(ediFile.getEDIFileRecordWSes()).stream()
                .filter((EDIFileRecordWS fileRecord) -> fileRecord.getHeader().equals(MeterRead.UMR.toString()) && Arrays.asList(fileRecord.getEdiFileFieldWSes()).stream()
                        .filter((EDIFileFieldWS fileFiled)->fileFiled.getKey().equals(MeterReadField.USAGE_TYPE.toString()) && fileFiled.getValue().equals(MeterReadField.SUM.toString())).count()==1)
                .findFirst();

        EDIFileRecordWS summaryRecord=summaryOptional.isPresent()? summaryOptional.get():null;

        LOG.debug("Summery record : "+summaryRecord);
        if(summaryRecord!=null){
            for(EDIFileFieldWS ediFileFieldWS:summaryRecord.getEdiFileFieldWSes()){
                if(ediFileFieldWS.getKey().equals(MeterReadField.START_SERVICE_DT.toString())){
                    startDate=ediFileFieldWS.getValue()!=null? dateFormat.parseDateTime(ediFileFieldWS.getValue()).toDate():null;
                }
                if(ediFileFieldWS.getKey().equals(MeterReadField.END_SERVICE_DT.toString())){
                    endDate=ediFileFieldWS.getValue()!=null? dateFormat.parseDateTime(ediFileFieldWS.getValue()).toDate():null;
                }
            }
        }
    }
}
