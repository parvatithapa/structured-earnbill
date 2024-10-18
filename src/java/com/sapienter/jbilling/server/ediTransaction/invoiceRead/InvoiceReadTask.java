package com.sapienter.jbilling.server.ediTransaction.invoiceRead;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileFieldDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileRecordDTO;
import com.sapienter.jbilling.server.ediTransaction.task.AbstractScheduledTransactionProcessor;
import com.sapienter.jbilling.server.ediTransaction.task.FileStructure;
import com.sapienter.jbilling.server.ediTransaction.task.MeterReadParserTask;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.IInvoiceSessionBean;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.batch.core.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.sapienter.jbilling.server.fileProcessing.FileConstants.CUSTOMER_ACCOUNT_KEY;
import static com.sapienter.jbilling.server.fileProcessing.FileConstants.INVOICE_TOTAL;

/**
 * Created by aman on 27/9/15.
 */

public class InvoiceReadTask extends AbstractScheduledTransactionProcessor implements StepExecutionListener{
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(InvoiceReadTask.class));

    protected static final ParameterDescription CANCELLATION_STATUS_NAME =
            new ParameterDescription("cancellation_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REPLACEMENT_STATUS_NAME =
            new ParameterDescription("replacement_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INVALID_FILE_STATUS_NAME =
            new ParameterDescription("invalid_file_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription FILE_MISMATCH_STATUS_NAME =
            new ParameterDescription("file_mismatch_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription ACCEPTED_STATUS_NAME =
            new ParameterDescription("accepted_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription DEPRECATED_STATUS_NAME =
            new ParameterDescription("deprecated_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REJECTED_STATUS_NAME =
            new ParameterDescription("rejected_status", true, ParameterDescription.Type.STR);


    protected static final ParameterDescription METER_FILE_STATUS_NAME =
            new ParameterDescription("meter_file_status", true, ParameterDescription.Type.STR);
    public static final Long TRANSACTION_SET = new Long(810);

    //Batch processor instance variable needs to initialize

    IWebServicesSessionBean webServicesSessionSpringBean;
    IOrderSessionBean orderSessionBean;
    IInvoiceSessionBean invoiceSessionBean;
    IEDITransactionBean ediTransactionBean;
    private String INVALID_FILE_STATUS;
    private String FILE_MISMATCH_STATUS;
    private String ACCEPTED_STATUS;
    private String METER_FILE_STATUS;
    private String REJECTED_STATUS;
    private String CANCELLATION_STATUS;
    private String REPLACEMENT_STATUS;
    private String DEPRECATED_STATUS;
    private int companyId;
    private Integer METER_READ_EDI_TYPE_ID;

    private Integer ediTypeId;
    private String supplierDUNS;
    private String utilityDUNS;
    private String INVOICE_NR = null;
    private BigDecimal totalAmount;
    private String comment;
    private List<Map<String, Object>> invoiceReadChargeDetails = null;
    private String invoiceStartDate;
    private String invoiceEndDate;
    private BigDecimal totalDetailBandAmount=BigDecimal.ZERO;


    String[] statuses=new String[]{"MisMatch", "EXP001", "EXP002","Accepted", "Deprecated"};
    String[] meterReadStatus=new String[]{"Done", "Deprecated", "EXP001", "EXP002"};

    public static enum InvoiceRead implements FileStructure {
        KEY, HDR, NME, SRV, CHG, TAX;
    }

    public static enum InvoiceReadField {
        START_SERVICE_DT,
        END_SERVICE_DT,
        AMOUNT,
        UNIT_RATE,
        UOM,
        QUANTITY,
        INVOICE_PURPOSE_CD,
        INVOICE_NR,
        COMMODITY

    }
    public static enum InvoiceReadConstants {
        ORIGINAL_METER_READ_PURPOSE_CD("00"),
        CANCELLATION_METER_READ_PURPOSE_CD("01"),
        REPLACEMENT_METER_READ_PURPOSE_CD("05"),
        ORIGINAL_INVOICE_READ_PURPOSE_CD("00"),
        CANCELLATION_INVOICE_READ_PURPOSE_CD("01"),
        FILE_ID("FILE_ID"),
        FILE_STATUS("FILE_STATUS"),
        METER_READ_RECORD_TYPE("867_PURPOSE_CD"),
        METER_READ_META_FIELD_NAME("edi_file_id"),
        METER_TRANS_NR("867_TRANS_NR"),
        ORG_INVOICE_NR("ORG_INVOICE_NR"),
        ORG_METER_READ_DONE_STATUS("Done"),
        REPLACEMENT_INVOICE_READ_PURPOSE_CD("18");

        String val;

        InvoiceReadConstants(String value) {
            val = value;
        }

        String getValue() {
            return val;
        }


    }

    public static DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyyMMdd");

    {
        //Not Used Yet
        descriptions.add(REPLACEMENT_STATUS_NAME);
        descriptions.add(CANCELLATION_STATUS_NAME);
        descriptions.add(INVALID_FILE_STATUS_NAME);
        descriptions.add(FILE_MISMATCH_STATUS_NAME);
        descriptions.add(ACCEPTED_STATUS_NAME);
        //Not Used Yet
        descriptions.add(DEPRECATED_STATUS_NAME);

        descriptions.add(METER_FILE_STATUS_NAME);
        descriptions.add(REJECTED_STATUS_NAME);

    }

    public void processFile(EDIFileWS ediFileWS, String escapeExceptionStatus) throws Exception{
        this.escapeExceptionStatus=escapeExceptionStatus;
        this.ediFile = ediFileWS;
        setMetaFieldValues(companyId);

        try {
            processInvoiceReadFile();
        } catch (Exception ex) {
            LOG.error(ex);
            status = (status == null) ? INVALID_FILE_STATUS : status;
            comment = ex.getMessage();
        }

        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for(EDIFileStatusWS ediStatus : ediType.getEdiStatuses()){
            if(ediStatus.getName().equals(status)){
                statusWS = ediStatus;
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
        invoiceSessionBean = Context.getBean(Context.Name.INVOICE_SESSION);
        orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);

        companyId = Integer.parseInt(pluginParameter.get("companyId"));
//        METER_READ_EDI_TYPE_ID = Integer.parseInt(pluginParameter.get("METER_READ_EDI_TYPE_ID"));
        INVALID_FILE_STATUS = pluginParameter.get("invalid_file_status");
        FILE_MISMATCH_STATUS = pluginParameter.get("file_mismatch_status");
        ACCEPTED_STATUS = pluginParameter.get("accepted_status");
        REJECTED_STATUS = pluginParameter.get("rejected_status");
        CANCELLATION_STATUS = pluginParameter.get("cancellation_status");
        REPLACEMENT_STATUS = pluginParameter.get("replacement_status");
        METER_FILE_STATUS = pluginParameter.get("meter_file_status");
        DEPRECATED_STATUS = pluginParameter.get("deprecated_status");

        setMetaFieldValues(companyId);
    }


    @Override
    public String getTaskName() {
        return "Invoice Read task: " + companyId + ", task Id:" + getTaskId();
    }

    @Override
    protected String getJobName() {
        return Context.Name.BATCH_EDI_INVOICE_TRANSACTION_PROCESS.getName();
    }

    @Override
    public void preBatchConfiguration(Map jobParams) {
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        LOG.debug("Execute InvoiceReadTask  plugin.");

        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME);
        METER_READ_EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME);
        if(EDI_TYPE_ID == null) {
            throwException("EDI type id not valid", null, REJECTED_STATUS);
        }

        EDITypeWS ediType = null;
        //todo : refactor
        ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);
        if (ediType == null)
            throwException("EDI type id not found "+EDI_TYPE_ID, null, REJECTED_STATUS);


        jobParams.put("INVALID_FILE_STATUS", new JobParameter(parameters.get(INVALID_FILE_STATUS_NAME.getName())));

        jobParams.put("FILE_MISMATCH_STATUS", new JobParameter(parameters.get(FILE_MISMATCH_STATUS_NAME.getName())));
        jobParams.put("ACCEPTED_STATUS", new JobParameter(parameters.get(ACCEPTED_STATUS_NAME.getName())));
        jobParams.put("METER_FILE_STATUS", new JobParameter(parameters.get(METER_FILE_STATUS_NAME.getName())));

//        jobParams.put("METER_READ_EDI_TYPE_ID", new JobParameter(METER_READ_EDI_TYPE_ID.toString()));
        jobParams.put("REJECTED_STATUS", new JobParameter(parameters.get(REJECTED_STATUS_NAME.getName())));
        jobParams.put("CANCELLATION_STATUS", new JobParameter(parameters.get(CANCELLATION_STATUS_NAME.getName())));
        jobParams.put("REPLACEMENT_STATUS", new JobParameter(parameters.get(REPLACEMENT_STATUS_NAME.getName())));
        jobParams.put("DEPRECATED_STATUS", new JobParameter(parameters.get(DEPRECATED_STATUS_NAME.getName())));

        jobParams.put("ediTypeId", new JobParameter(EDI_TYPE_ID.longValue()));
        jobParams.put("supplierDUNS", new JobParameter(SUPPLIER_DUNS));
        jobParams.put("utilityDUNS", new JobParameter(UTILITY_DUNS));
//        Set transaction type from suffix.
        jobParams.put("TRANSACTION_SET", new JobParameter(ediType.getEdiSuffix()));
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {

        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        invoiceSessionBean = Context.getBean(Context.Name.INVOICE_SESSION);
        orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);

        LOG.debug("EDI File Invoice Item Processor : Before Step");
        JobParameters jobParameters = stepExecution.getJobParameters();

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        utilityDUNS = jobParameters.getString("utilityDUNS");
        supplierDUNS = jobParameters.getString("supplierDUNS");

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        INVALID_FILE_STATUS = jobParameters.getString("INVALID_FILE_STATUS");
        FILE_MISMATCH_STATUS = jobParameters.getString("FILE_MISMATCH_STATUS");
        ACCEPTED_STATUS = jobParameters.getString("ACCEPTED_STATUS");
        REJECTED_STATUS = jobParameters.getString("REJECTED_STATUS");
        CANCELLATION_STATUS = jobParameters.getString("CANCELLATION_STATUS");
        REPLACEMENT_STATUS = jobParameters.getString("REPLACEMENT_STATUS");
        METER_FILE_STATUS = jobParameters.getString("METER_FILE_STATUS");
        companyId = jobParameters.getLong("companyId").intValue();
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
        ediTypeId=null;
        customerAccountNumber = null;
        INVOICE_NR = null;
        totalAmount=null;
        invoiceReadChargeDetails = null;
        status=null;
        invoiceEndDate=null;
        invoiceStartDate=null;

        ediFile=null;
        escapeExceptionStatus=null;
        recordFields=null;
        recordFieldsList=null;
        recordLevelExceptionCode=null;
        exceptionCode=null;
        totalDetailBandAmount=null;
    }

    public EDIFileWS process(EDIFileWS ediFileWS) throws Exception {
        LOG.debug("Invoice Read Task Process Method");
        LOG.debug("Invoice Read : "+ediFileWS.getName());
        try {
            if (ediFileWS.getEdiFileStatusWS().getId() == FileConstants.EDI_STATUS_PROCESSED) {
                this.ediFile = ediFileWS;
                setMetaFieldValues(companyId);
                processInvoiceReadFile();
            } else {
                // Change the status to Error Detected
                LOG.error("Error");
                status = ediFileWS.getEdiFileStatusWS().getName();
            }
        } catch (Exception ex) {
            LOG.error(ex);
            status = (status == null) ? INVALID_FILE_STATUS : status;
            comment = (comment==null)? EDITransactionHelper.getExceptionMessage(ex) : comment;
        }

        EDITypeWS ediType = webServicesSessionSpringBean.getEDIType(ediTypeId);
        EDIFileStatusWS statusWS = null;
        for(EDIFileStatusWS ediStatus : ediType.getEdiStatuses()){
            if(ediStatus.getName().equals(status)){
                statusWS = ediStatus;
            }
        }

        if (statusWS != null) ediFileWS.setEdiFileStatusWS(statusWS);
        if (comment != null) ediFileWS.setComment(comment);
        if(exceptionCode!=null)ediFileWS.setExceptionCode(exceptionCode);
        bindFields(ediFileWS);
        clearData();
        return ediFileWS;
    }


    private void processInvoiceReadFile() throws Exception {
        List<String> maidenRecords = new LinkedList<String>();
        maidenRecords.add(InvoiceRead.KEY.toString());
        maidenRecords.add(InvoiceRead.HDR.toString());
        maidenRecords.add(InvoiceRead.TAX.toString());
        //Parse maiden records
        parseRecords(maidenRecords, ediFile.getEDIFileRecordWSes());

        ediTypeId = (Integer) companyMetaFieldValueMap.get(FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME);
        METER_READ_EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME);
        if(ediTypeId == null) {
            throwException("EDI type id not valid", null, REJECTED_STATUS);
        }
        customerAccountNumber = findField(InvoiceRead.HDR, CUSTOMER_ACCOUNT_KEY);
        if(customerAccountNumber == null){
            throwException("Account Id not found in Invoice read file", FileConstants.INVOICE_READ_UNKNOWN_ACCOUNT_EXP_CODE, REJECTED_STATUS);
        }

        LOG.debug("Processing invoice read detail band");
        parseDetailBand();
        LOG.debug("Binding start date and end date to EDI file");
        LOG.debug("Start Date : "+invoiceStartDate);
        if(invoiceStartDate!=null){
            startDate=dateFormat.parseDateTime(invoiceStartDate).toDate();
        }

        LOG.debug("End Date : " + invoiceEndDate);
        if(invoiceEndDate!=null){
            endDate=dateFormat.parseDateTime(invoiceEndDate).toDate();
        }
        validateInvoiceReadFile();
        processInvoiceDetailBand();

        if (ediTransactionBean.hasPlanSendRateChangeDaily(userDTO)) {
            status = ediFile.getEdiFileStatusWS().getName();
            comment = "Can't Generate Invoice Because Customer Subscribed Day Ahead Product Plan";
            return;
        }

        // Check the status of Invoice file : Cancellation
        String invoiceReadType = findField(InvoiceRead.HDR, InvoiceReadField.INVOICE_PURPOSE_CD.toString());
        if (invoiceReadType != null) {

            if (invoiceReadType.equals(InvoiceReadConstants.ORIGINAL_INVOICE_READ_PURPOSE_CD.getValue())) {
                LOG.debug("Processing original Invoice Read");
                processOriginalInvoiceRead();
            } else if (invoiceReadType.equals(InvoiceReadConstants.CANCELLATION_INVOICE_READ_PURPOSE_CD.getValue())) {
                LOG.debug("Processing Cancellation Invoice Read");
                Map<String, String> cancellationInvoiceRead = getExistingInvoiceRead(InvoiceReadConstants.CANCELLATION_INVOICE_READ_PURPOSE_CD.getValue(),  InvoiceReadField.INVOICE_PURPOSE_CD.toString());
                if (cancellationInvoiceRead != null && !cancellationInvoiceRead.get(InvoiceReadConstants.FILE_ID.getValue()).equals(ediFile.getId()+"")) {
                    throwException("Cancellation Invoice read is already found for period " + invoiceStartDate + " to " + invoiceEndDate, FileConstants.CANCELLATION_INVOICE_READ_INVOICE_ALLREADY_CANCELLED_EXP_CODE, INVALID_FILE_STATUS, true);
                }

                Map<String, String> originalInvoiceRead = findOriginalInvoiceRead();
                if (escapeExceptionStatus == null) {
                    status = CANCELLATION_STATUS;
                } else {
                    //code for deleting Invoice
                    if(originalInvoiceRead!=null){
                        deleteInvoiceRead(originalInvoiceRead);
                        //Update original invoice read status to depricated
                        updateFileStatus(Integer.parseInt(originalInvoiceRead.get(InvoiceReadConstants.FILE_ID.getValue())), DEPRECATED_STATUS);
                    }
                    status=ACCEPTED_STATUS;
                }
            } else if (invoiceReadType.equals(InvoiceReadConstants.REPLACEMENT_INVOICE_READ_PURPOSE_CD.getValue())) {
                LOG.debug("Processing Replacement Invoice Read");
                processReplacementInvoiceRead();
            } else {
                LOG.error("Invoice read has invalid value of  " + InvoiceReadField.INVOICE_PURPOSE_CD.toString() + " field");
                throwException("Invoice read has invalid value of  " + InvoiceReadField.INVOICE_PURPOSE_CD.toString() + " field", null, REJECTED_STATUS);
            }
        } else {
            LOG.error("Invoice do not have " + InvoiceReadField.INVOICE_PURPOSE_CD.toString() + " field");
            throwException("Invoice do not have " + InvoiceReadField.INVOICE_PURPOSE_CD.toString() + " field", null, REJECTED_STATUS);
        }

        //Validate Invoice data

    }

    private void validateInvoiceReadFile() throws Exception{

        try{
            String commodity = findField(InvoiceRead.KEY, InvoiceReadField.COMMODITY.toString());
            LOG.debug("Commodity code :"+ commodity);
            try{
                commodity=EDITransactionHelper.getCommodityFromCode(commodity);
            }catch (SessionInternalError sie){
                throwException(sie.getMessage(), null, REJECTED_STATUS);
            }
            LOG.debug("Commodity :"+ commodity);
            userDTO = ediTransactionBean.findUserByAccountNumber(companyId, CUSTOMER_ACCOUNT_KEY, customerAccountNumber, commodity);
        }catch (SessionInternalError e){
            throwException(e.getMessage(), FileConstants.INVOICE_READ_UNKNOWN_ACCOUNT_EXP_CODE, REJECTED_STATUS);
        }


        LOG.debug("Customer found " + userDTO.getUserName());

        //Check Transfer_NR
        INVOICE_NR = findField(InvoiceRead.HDR, InvoiceReadField.INVOICE_NR.toString());
        LOG.debug(InvoiceReadField.INVOICE_NR.toString() + "value found : " + INVOICE_NR);
        try {
            ediTransactionBean.isUniqueKeyExistForFile(companyId, ediTypeId, ediFile.getId(), InvoiceReadField.INVOICE_NR.toString(), INVOICE_NR, TransactionType.INBOUND);
        } catch (Exception e) {
            throwException(e.getMessage(), FileConstants.INVOICE_READ_DUPLICATE_TRANSACTION_EXP_CODE, INVALID_FILE_STATUS);
        }

        LOG.debug("Check is customer is Rate ready");
        UserWS user = new UserBL(userDTO.getId()).getUserWS();
        String billingModal = getCustomerType(user);
        if(!billingModal.equals(FileConstants.BILLING_MODEL_RATE_READY)){
            throwException("Cannot upload invoice read for "+billingModal+" customer", null, REJECTED_STATUS);
        }
        validateTotalAmount();

    }

    /**
     * find existing meter read according to the type of the Meter read
     * @param type its value will be 00, 01, 05 for original, cancellation and replacement meter read
     * @param recordKey
     * @return
     */
    private Map<String, String> getExistingMeterRead(String type,  String recordKey){
        EDIFileDTO ediFileDTO=getExistingRecord(type, recordKey, METER_READ_EDI_TYPE_ID, meterReadStatus);
        if(ediFileDTO==null){
            return null;
        }
        EDIFileWS meterRead = ediTransactionBean.getEDIFileWS(ediFileDTO.getId());
        Map<String, String> meterData = new HashMap<String, String>();
        meterData.put(InvoiceReadConstants.FILE_ID.getValue(), meterRead.getId()+"");
        meterData.put(InvoiceReadConstants.FILE_STATUS.getValue(), meterRead.getEdiFileStatusWS().getName());
        EDITransactionHelper ediTransactionHelper = new EDITransactionHelper();
        EDIFileRecordWS headerRecord = ediTransactionHelper.getHeaderRecord(meterRead);
        for (EDIFileFieldWS ediFileField : headerRecord.getEdiFileFieldWSes()) {
            if (ediFileField.getKey().equals(FileConstants.TRANS_REF_NR)) {
                meterData.put(FileConstants.TRANS_REF_NR, ediFileField.getValue());
            }
            if (ediFileField.getKey().equals(InvoiceReadConstants.METER_READ_RECORD_TYPE.getValue())) {
                meterData.put(InvoiceReadConstants.METER_READ_RECORD_TYPE.getValue(), ediFileField.getValue());
            }
        }
        return meterData;
    }

    /**
     * find existing Invoice read according to the type of the Invoice read
     * @param type its value will be 00, 01, 18 for original, cancellation and replacement Invoice read
     * @param recordKey
     * @return
     */
    private Map<String, String> getExistingInvoiceRead(String type,  String recordKey){
        EDIFileDTO invoiceReadFile= getExistingRecord(type, recordKey, ediTypeId, statuses);
        if(invoiceReadFile==null){
            return null;
        }
        EDIFileWS invoiceRead = ediTransactionBean.getEDIFileWS(invoiceReadFile.getId());
        Map<String, String> invoiceData = new HashMap<String, String>();
        invoiceData.put(InvoiceReadConstants.FILE_ID.getValue(), invoiceRead.getId() + "");
        invoiceData.put(InvoiceReadConstants.FILE_STATUS.getValue(), invoiceRead.getEdiFileStatusWS().getName());
        EDITransactionHelper ediTransactionHelper = new EDITransactionHelper();
        EDIFileRecordWS headerRecord = ediTransactionHelper.getHeaderRecord(invoiceRead);
        for (EDIFileFieldWS ediFileField : headerRecord.getEdiFileFieldWSes()) {
            if (ediFileField.getKey().equals(FileConstants.INVOICE_NR)) {
                invoiceData.put(FileConstants.INVOICE_NR, ediFileField.getValue());
            }
            if (ediFileField.getKey().equals(InvoiceReadField.INVOICE_PURPOSE_CD.toString())) {
                invoiceData.put(InvoiceReadField.INVOICE_PURPOSE_CD.toString(), ediFileField.getValue());
            }
        }

        return invoiceData;
    }

    private EDIFileDTO getExistingRecord(String recordType, String recordTypeKey, Integer ediTypeId, String[] statuses){
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("ediType.id", ediTypeId));
        conjunction.add(Restrictions.eq("entity.id", companyId));
        conjunction.add(Restrictions.eq("type", TransactionType.INBOUND));
        conjunction.add(Restrictions.in("status.name", statuses));
        conjunction.add(Restrictions.eq("utilityAccountNumber", customerAccountNumber));
        conjunction.add(Restrictions.eq("startDate", startDate));
        conjunction.add(Restrictions.eq("endDate", endDate));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", recordTypeKey));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", recordType));
        conjunction.add(Restrictions.not(Restrictions.eq("id", ediFile.getId())));
        List<EDIFileDTO> ediFileDTOList= new EDIFileDAS().findEDIFiles(conjunction);
        if(ediFileDTOList.size()>0){
            return ediFileDTOList.get(0);
        }
        return null;
    }

    private void processOriginalInvoiceRead() {
        LOG.debug("Start processing meter read found for Invoice read");

        Map<String, String> originalInvoiceRead  = getExistingInvoiceRead(InvoiceReadConstants.ORIGINAL_INVOICE_READ_PURPOSE_CD.getValue(), InvoiceReadField.INVOICE_PURPOSE_CD.toString());
        if(originalInvoiceRead!=null){
            throwException(String.format("Original Invoice read already exists for period %s to %s", invoiceStartDate, invoiceEndDate), null, INVALID_FILE_STATUS);
        }

        Map<String, String> replacementMeterRead  = getExistingMeterRead(InvoiceReadConstants.REPLACEMENT_METER_READ_PURPOSE_CD.getValue(),  InvoiceReadConstants.METER_READ_RECORD_TYPE.getValue());
        Map<String, String> cancellationMeterRead  = getExistingMeterRead(InvoiceReadConstants.CANCELLATION_METER_READ_PURPOSE_CD.getValue(),  InvoiceReadConstants.METER_READ_RECORD_TYPE.getValue());
        if(replacementMeterRead!=null && replacementMeterRead.get(InvoiceReadConstants.FILE_STATUS.getValue()).equals(InvoiceReadConstants.ORG_METER_READ_DONE_STATUS.getValue())){
            LOG.debug("Replacement Meter Read : " + replacementMeterRead);
            createInvoice(replacementMeterRead);
        }else if(cancellationMeterRead!=null && cancellationMeterRead.get(InvoiceReadConstants.FILE_STATUS.getValue()).equals(InvoiceReadConstants.ORG_METER_READ_DONE_STATUS.getValue())){
            throwException("Waiting for replacement Meter read of period" + invoiceStartDate + " to " + invoiceEndDate, null, INVALID_FILE_STATUS, true);
        }else{
            String meterReadTransferNR=findField(InvoiceRead.HDR, InvoiceReadConstants.METER_TRANS_NR.getValue());
            Map<String, String> originalMeterData=null;
            //if invoice read contains meter read TRANS_REF_NR
            LOG.debug("Meter Read TRANS_REF_NR "+meterReadTransferNR);

            // Find original meter read based on dates.
            originalMeterData = getExistingMeterRead(InvoiceReadConstants.ORIGINAL_METER_READ_PURPOSE_CD.getValue(), InvoiceReadConstants.METER_READ_RECORD_TYPE.getValue());
            if (originalMeterData == null) {
                throwException("No original Meter Read found for " + invoiceStartDate + " to " + invoiceEndDate, FileConstants.INVOICE_READ_DOES_NOT_MATCH_METER_READ_EXP_CODE, INVALID_FILE_STATUS, true);
            }
            // If invoice read contains meter read Ref NR then it should be matched with meter read fetched based on dates.
            if (StringUtils.isNotBlank(meterReadTransferNR)) {
                String meterReadRefNr = originalMeterData.get(FileConstants.TRANS_REF_NR);

                if (meterReadRefNr == null || !meterReadTransferNR.equals(meterReadRefNr)) {
                    throwException("No matching meter found for Invoice Read", FileConstants.METER_READ_NOT_FOUND_FOR_INVOICE_RECORD_EXP_CODE, INVALID_FILE_STATUS, true);
                }
            }

            createInvoice(originalMeterData);
        }
        //Successful status
        status = FILE_MISMATCH_STATUS.equals(status) ? FILE_MISMATCH_STATUS : ACCEPTED_STATUS;

    }

    private void processInvoiceDetailBand() {
        //Find Min(Start_DT) & Max(END_Start_DT)
        LinkedList<Date> dates = new LinkedList<Date>();
        for (Map<String, Object> record : invoiceReadChargeDetails) {
            dates.add((Date) record.get(InvoiceReadField.START_SERVICE_DT.toString()));
            dates.add((Date) record.get(InvoiceReadField.END_SERVICE_DT.toString()));
        }
        Collections.sort(dates);

    }


    private void parseDetailBand() {
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        Map<String, Object> invoiceFields = null;
        EDIFileRecordWS[] recordList = ediFile.getEDIFileRecordWSes();

        for (int i = 0; i < recordList.length; i++) {
            if (recordList[i].getHeader().equals(InvoiceRead.SRV.toString())) {
                if (recordList[i + 1].getHeader().equals(InvoiceRead.CHG.toString())) {
                    invoiceFields = new HashMap<String, Object>();

                    Map<String, String> fields = parseRecord(recordList[i]);

                    invoiceStartDate=findField(fields, InvoiceReadField.START_SERVICE_DT.toString(), true);
                    invoiceEndDate=findField(fields, InvoiceReadField.END_SERVICE_DT.toString(), true);

                    invoiceFields.put(InvoiceReadField.START_SERVICE_DT.toString(), findField(fields, InvoiceReadField.START_SERVICE_DT.toString(), "Date", true));
                    invoiceFields.put(InvoiceReadField.END_SERVICE_DT.toString(), findField(fields, InvoiceReadField.END_SERVICE_DT.toString(), "Date", true));

                    fields = parseRecord(recordList[i + 1]);
                    invoiceFields.put(InvoiceReadField.AMOUNT.toString(), findField(fields, InvoiceReadField.AMOUNT.toString(), "BigDecimal", true));
                    invoiceFields.put(InvoiceReadField.UNIT_RATE.toString(), findField(fields, InvoiceReadField.UNIT_RATE.toString(), "BigDecimal", true));
                    invoiceFields.put(InvoiceReadField.UOM.toString(), findField(fields, InvoiceReadField.UOM.toString(), false));
                    invoiceFields.put(InvoiceReadField.QUANTITY.toString(), findField(fields, InvoiceReadField.QUANTITY.toString(), "Integer", true));
                    parseRecord(recordList[i + 1]);

                    list.add(invoiceFields);
                    i = i + 2;
                } else {
                    throwException("Format is not valid. Do not have proper pair of SVC and CHG record.", null, INVALID_FILE_STATUS);
                }
            }
        }
        if (list.size() < 1) {
            throwException("Format is not valid. Do not have record for SVC and CHG.", null, INVALID_FILE_STATUS);
        }
        Integer totalQuantity = 0;
        for (Map<String, Object> data : list) {
            Integer usage=(Integer)data.get(InvoiceReadField.QUANTITY.toString());
            if(usage < 0){
                throwException("Usage cannot be negative", FileConstants.INVOICE_READ_INVALID_USAGE_EXP_CODE, INVALID_FILE_STATUS);
            }
            totalDetailBandAmount = totalDetailBandAmount.add((BigDecimal) data.get(InvoiceReadField.AMOUNT.toString()));
            totalQuantity = totalQuantity + usage;
        }

        Map<String, String> taxFields = recordFields.get(InvoiceRead.TAX.toString());
        BigDecimal taxAmount = new BigDecimal(BigInteger.ZERO);
        if (taxFields != null) {
            taxAmount = (BigDecimal) findField(InvoiceRead.TAX, "AMOUNT", "BigDecimal", true);
        }
        totalDetailBandAmount = totalDetailBandAmount.add(taxAmount);
        invoiceReadChargeDetails = list;
    }

    private void validateTotalAmount(){
        BigDecimal totalInvoiceAmount = (BigDecimal) findField(InvoiceRead.HDR, INVOICE_TOTAL, "BigDecimal", true);
        if (totalInvoiceAmount.compareTo(totalDetailBandAmount) == 0) {
            this.totalAmount = totalInvoiceAmount;
            //todo do we need to match quantity
//            this.totalQuantity = totalQuantity;
        } else {
            LOG.error("Amount field in HDR record do not match with sum of Detail record's amount : " + totalInvoiceAmount + "<>" + totalDetailBandAmount);
            throwException("Amount field in HDR record do not match with sum of Detail record's amount : " + totalInvoiceAmount + "<>" + totalDetailBandAmount, FileConstants.INVOICE_READ_INVALID_CALCULATION_EXP_CODE, INVALID_FILE_STATUS);
        }
    }

    private void validateInvoice(Integer invoiceId, BigDecimal totalAmount) {
        InvoiceDTO invoice = new InvoiceDAS().findNow(invoiceId);
        if (invoice.getBalance().compareTo(totalAmount) != 0) {
            LOG.error("Amount did not match with System's Invoice. Invoice Id : " + invoiceId);
            throwException("Amount did not match with System's Invoice. Invoice Id : " + invoiceId, null, FILE_MISMATCH_STATUS);
        }
    }

    private void processReplacementInvoiceRead() {

        LOG.debug("Processing Replacement read");
        Map<String, String> replacementInvoiceRead  = getExistingInvoiceRead(InvoiceReadConstants.REPLACEMENT_INVOICE_READ_PURPOSE_CD.getValue(), InvoiceReadField.INVOICE_PURPOSE_CD.toString());
        if(replacementInvoiceRead!=null && !replacementInvoiceRead.get(InvoiceReadConstants.FILE_ID.getValue()).equals(ediFile.getId()+"")){
            throwException("Replacement Invoice read is already exist for period "+invoiceStartDate+" to "+ invoiceEndDate, FileConstants.REPLACEMENT_INVOICE_READ_DUPLICATE_REPLACEMENT_READ_EXP_CODE, INVALID_FILE_STATUS);
        }

        Map<String, String> originalInvoiceRead  = findOriginalInvoiceRead();

        Map<String, String> cancellationInvoiceRead  = getExistingInvoiceRead(InvoiceReadConstants.CANCELLATION_INVOICE_READ_PURPOSE_CD.getValue(), InvoiceReadField.INVOICE_PURPOSE_CD.toString());
        LOG.debug("Cancellation Invoice Read "+ cancellationInvoiceRead);
        if(cancellationInvoiceRead==null && escapeExceptionStatus==null){
            //if cancellation invoice read not exist, mark status of EDI file to EXP002
            status=REPLACEMENT_STATUS;
            exceptionCode = FileConstants.REPLACEMENT_INVOICE_READ_MISSING_CANCELLATION_INVOICE_MATCH_EXP_CODE;
            return;
        }

        Map<String, String> replacementMeterRead  = getExistingMeterRead(InvoiceReadConstants.REPLACEMENT_METER_READ_PURPOSE_CD.getValue(),  InvoiceReadConstants.METER_READ_RECORD_TYPE.getValue());
        LOG.debug("Replacement Meter Read : " + replacementMeterRead);
        if(replacementMeterRead!=null && !replacementMeterRead.get(InvoiceReadConstants.FILE_STATUS.getValue()).equals(InvoiceReadConstants.ORG_METER_READ_DONE_STATUS.getValue())){
            //If replacement meter read exit but its status is not done then throw mandatory exception that replacement meter read is mandatory
            throwException("No replacement Meter Read found for period "+ invoiceStartDate+" to "+ invoiceEndDate, null, INVALID_FILE_STATUS,true);
        }
        Map<String, String> cancellationMeterRead  = getExistingMeterRead(InvoiceReadConstants.CANCELLATION_METER_READ_PURPOSE_CD.getValue(), InvoiceReadConstants.METER_READ_RECORD_TYPE.getValue());
        if(replacementMeterRead==null && cancellationMeterRead!=null){
            // if replacement meter read not exist but cancellation invoice read exit, throw mandatory exception is cancellation meter read status is Done else throw a non mandatory exception
            throwException("Replacement meter read is required in done status for period " + invoiceStartDate + " to " + invoiceEndDate, null, INVALID_FILE_STATUS, cancellationMeterRead.get(InvoiceReadConstants.FILE_STATUS.getValue()).equals(InvoiceReadConstants.ORG_METER_READ_DONE_STATUS.getValue()));
        }

        if(replacementMeterRead!=null && replacementMeterRead.get(InvoiceReadConstants.FILE_STATUS.getValue()).equals(InvoiceReadConstants.ORG_METER_READ_DONE_STATUS.getValue())){
            //if replacement invoice read exit then create invoice for replacement meter read
            createInvoice(replacementMeterRead);
        }else{
            //create invoice for original Invoice read.
            Map<String, String> originalMeterData  = getExistingMeterRead(InvoiceReadConstants.ORIGINAL_METER_READ_PURPOSE_CD.getValue(),  InvoiceReadConstants.METER_READ_RECORD_TYPE.getValue());
            if(originalMeterData==null){
                throwException("No original Meter Read found for "+invoiceStartDate+" to "+ invoiceEndDate, FileConstants.INVOICE_READ_DOES_NOT_MATCH_METER_READ_EXP_CODE, REJECTED_STATUS);
            }
            createInvoice(originalMeterData);
        }


        if(cancellationInvoiceRead==null){
            //if cancellation invoice read not exist, delete original Invoice and make original Invoice read to depricated
            updateFileStatus(Integer.parseInt(originalInvoiceRead.get(InvoiceReadConstants.FILE_ID.getValue())), DEPRECATED_STATUS);
        }else if(!cancellationInvoiceRead.get(InvoiceReadConstants.FILE_STATUS.getValue()).equals(ACCEPTED_STATUS)){
            updateFileStatus(Integer.parseInt(originalInvoiceRead.get(InvoiceReadConstants.FILE_ID.getValue())), DEPRECATED_STATUS);
            updateFileStatus(Integer.parseInt(cancellationInvoiceRead.get(InvoiceReadConstants.FILE_ID.getValue())), ACCEPTED_STATUS);
        }

        status = FILE_MISMATCH_STATUS.equals(status) ? FILE_MISMATCH_STATUS : ACCEPTED_STATUS;

    }

    private Map<String, String> findOriginalInvoiceRead(){
        String invoiceReadType = findField(InvoiceRead.HDR, InvoiceReadField.INVOICE_PURPOSE_CD.toString());
        String invoiceReadTransferNR=findField(InvoiceRead.HDR, InvoiceReadConstants.ORG_INVOICE_NR.getValue());

        LOG.debug("Invoice NR : "+invoiceReadTransferNR);
        Map<String, String> originalInvoiceRead=null;
        if(invoiceReadTransferNR!=null){
            originalInvoiceRead=findInvoiceReadByInvoiceNR(invoiceReadTransferNR);
            LOG.debug("Original Invoice read : " + originalInvoiceRead);
            //checking is a valid original invoice read exit in the system for the given TRANS_REF_NR.
            if(originalInvoiceRead==null || (originalInvoiceRead.get(InvoiceReadField.INVOICE_PURPOSE_CD.toString()) !=null && !(originalInvoiceRead.get(InvoiceReadField.INVOICE_PURPOSE_CD.toString()).equals(InvoiceReadConstants.ORIGINAL_INVOICE_READ_PURPOSE_CD.getValue())))){
                LOG.debug("No original EDI file found for TRANS_REF_NR : "+invoiceReadTransferNR);
                String exceptionCode=(invoiceReadType.equals(InvoiceReadConstants.REPLACEMENT_INVOICE_READ_PURPOSE_CD.getValue())? FileConstants.REPLACEMENT_INVOICE_READ_ORIGINAL_INVOICE_READ_NOT_EXIST_EXP_CODE:FileConstants.CANCELLATION_INVOICE_READ_INVOICE_TO_CANCEL_NOT_EXIST_EXP_CODE);
                throwException("Original invoice read not found for TRANS_NR "+ invoiceReadTransferNR, exceptionCode, INVALID_FILE_STATUS);
            }else{
                if(!isFileMatch(originalInvoiceRead)){
                    throwException("Original invoice Read not found of customer "+customerAccountNumber+" for period "+invoiceStartDate+" to "+ invoiceEndDate , invoiceReadType.equals(InvoiceReadConstants.REPLACEMENT_INVOICE_READ_PURPOSE_CD.getValue())? FileConstants.REPLACEMENT_INVOICE_READ_ORIGINAL_INVOICE_READ_NOT_MATCH_EXP_CODE:FileConstants.CANCELLATION_INVOICE_READ_ORIGINAL_INVOICE_READ_NOT_MATCH_EXP_CODE,INVALID_FILE_STATUS);
                }
            }
        }else{
            originalInvoiceRead  = getExistingInvoiceRead(InvoiceReadConstants.ORIGINAL_INVOICE_READ_PURPOSE_CD.getValue(),  InvoiceReadField.INVOICE_PURPOSE_CD.toString());
            LOG.debug("Original Invoice Read : "+originalInvoiceRead);
            if(originalInvoiceRead==null){
                throwException("Original invoice Read not found for period "+invoiceStartDate+" to "+ invoiceEndDate, invoiceReadType.equals(InvoiceReadConstants.REPLACEMENT_INVOICE_READ_PURPOSE_CD.getValue())? FileConstants.REPLACEMENT_INVOICE_READ_ORIGINAL_INVOICE_READ_NOT_EXIST_EXP_CODE:FileConstants.CANCELLATION_INVOICE_READ_INVOICE_TO_CANCEL_NOT_EXIST_EXP_CODE, INVALID_FILE_STATUS);
            }
        }
        return originalInvoiceRead;
    }

    private Map<String, String> findInvoiceReadByInvoiceNR(String invoiceReadTransferNR){
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("ediType.id", ediFile.getEdiTypeWS().getId()));
        conjunction.add(Restrictions.eq("entity.id", companyId));
        conjunction.add(Restrictions.eq("type", TransactionType.INBOUND));
        conjunction.add(Restrictions.in("status.name", statuses));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", InvoiceReadField.INVOICE_NR.toString()));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", invoiceReadTransferNR));
        EDIFileDTO originalEdiFile = new EDIFileDAS().findEDIFile(conjunction);
        LOG.debug("originalEdiFile : "+originalEdiFile);
        if(originalEdiFile==null){
            return null;
        }

        Map<String, String> invoiceData=new HashMap<>();
        invoiceData.put(InvoiceReadConstants.FILE_ID.getValue(), originalEdiFile.getId() + "");
        invoiceData.put(InvoiceReadConstants.FILE_STATUS.getValue(), originalEdiFile.getFileStatus().getName());

        //binding required data in the invoiceData
        originalEdiFile.getEdiFileRecords().stream().filter((EDIFileRecordDTO ediFileRecord) -> ediFileRecord.getEdiFileRecordHeader().equals(InvoiceRead.HDR.toString()) || ediFileRecord.getEdiFileRecordHeader().equals(InvoiceRead.SRV.toString())).forEach((EDIFileRecordDTO ediFileRecord) -> {
            ediFileRecord.getFileFields().stream().forEach((EDIFileFieldDTO fileField) -> invoiceData.put(fileField.getEdiFileFieldKey(), fileField.getEdiFileFieldValue()));
        });

        return invoiceData;
    }

    private Boolean isFileMatch(Map<String, String> originalInvoiceRead){
        LOG.debug("Matching the EDI files");
        String accountNumber=originalInvoiceRead.get(CUSTOMER_ACCOUNT_KEY);
        String oldMeterStartDate=originalInvoiceRead.get(InvoiceReadField.START_SERVICE_DT.toString());
        String oldMeterEndDate=originalInvoiceRead.get(InvoiceReadField.END_SERVICE_DT.toString());

        if(findField(InvoiceRead.HDR, CUSTOMER_ACCOUNT_KEY).equals(accountNumber) && invoiceStartDate.equals(oldMeterStartDate) && invoiceEndDate.equals(oldMeterEndDate)){
            return true;
        }

        return false;
    }

    private void deleteInvoiceRead(Map<String, String> originalInvoiceRead){
        InvoiceDAS invoiceDAS=new InvoiceDAS();
        Integer originalInvoiceId=invoiceDAS.findInvoiceByMetaFieldValue(companyId, InvoiceReadField.INVOICE_NR.toString(), originalInvoiceRead.get(InvoiceReadField.INVOICE_NR.toString()));
        LOG.debug("Original Invoice id : "+originalInvoiceId);
        if(originalInvoiceId!=null){
            LOG.debug("deleting invoice for invoice id :  " + originalInvoiceId);
            //deleting invoice and making order explicitly Suspended so that we can processed the original Meter
            //in the above code we first deleting the finding the invoice's order, then deleting the invoice and after that updating order's status to suspended.
            InvoiceDTO invoiceDTO=invoiceSessionBean.getInvoice(originalInvoiceId);

            OrderDTO orderDTO=null;
            for(OrderProcessDTO orderProcessDTO:invoiceDTO.getOrderProcesses()){
                orderDTO=orderProcessDTO.getPurchaseOrder();
                break;
            }
            invoiceSessionBean.delete(originalInvoiceId, null);

            if(orderDTO!=null){
                Integer orderStatus = new OrderStatusDAS().getDefaultOrderStatusId(
                        OrderStatusFlag.NOT_INVOICE,  userDTO.getEntity().getId());
                orderDTO.setStatusId(orderStatus);
                new OrderDAS().save(orderDTO);
            }

        }else{
            throwException("No original Invoice found for period " + invoiceStartDate + " to " + invoiceEndDate, null, INVALID_FILE_STATUS);
        }
    }

    private Map<String, String> getMeterReadByTransRefNR(){
        String meterReadTransferNR=findField(InvoiceRead.HDR, InvoiceReadConstants.METER_TRANS_NR.getValue());
        if(meterReadTransferNR==null){
            return null;
        }

        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq("ediType.id", METER_READ_EDI_TYPE_ID));
        conjunction.add(Restrictions.eq("entity.id", companyId));
        conjunction.add(Restrictions.eq("type", TransactionType.INBOUND));
        conjunction.add(Restrictions.in("status.name", meterReadStatus));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldKey", MeterReadParserTask.MeterReadField.TRANS_REF_NR.toString()));
        conjunction.add(Restrictions.eq("fileFields.ediFileFieldValue", meterReadTransferNR));
        EDIFileDTO originalEdiFile = new EDIFileDAS().findEDIFile(conjunction);

        if(originalEdiFile==null){
            return null;
        }

        //code for checking is the utility account number of invoice read match with the meter read utilit account number
        Map<String, String> meterReadData=new HashMap<>();
        originalEdiFile.getEdiFileRecords().stream().filter((EDIFileRecordDTO ediFileRecord) -> ediFileRecord.getEdiFileRecordHeader().equals(MeterReadParserTask.MeterRead.HDR.name())).forEach((EDIFileRecordDTO ediFileRecord) -> {
            ediFileRecord.getFileFields().stream().filter((EDIFileFieldDTO ediFileField) -> ediFileField.getEdiFileFieldKey().equals(MeterReadParserTask.MeterReadField.UTILITY_CUST_ACCT_NR.name())).forEach((EDIFileFieldDTO ediFileField) ->{
                if(!ediFileField.getEdiFileFieldValue().equals(customerAccountNumber)){
                   throwException("Invoice read "+CUSTOMER_ACCOUNT_KEY+"("+customerAccountNumber+") does not match with meter read "+CUSTOMER_ACCOUNT_KEY+"("+ediFileField.getEdiFileFieldValue()+"", null, REJECTED_STATUS);
                }
                meterReadData.put(InvoiceReadConstants.FILE_ID.getValue(), originalEdiFile.getId()+"");
                meterReadData.put(InvoiceReadConstants.FILE_STATUS.getValue(), originalEdiFile.getFileStatus().getName());
            });
        });

        if(meterReadData.keySet().size()>0){
            return meterReadData;
        }

        return null;
    }

    private void createInvoice(Map<String, String> originalMeterData){
        String originalMeterReadStatus=originalMeterData.get(InvoiceReadConstants.FILE_STATUS.getValue());
        LOG.debug("Processing Meter read : " + originalMeterData.get(FileConstants.TRANS_REF_NR));
        LOG.debug("Original Meter Read Status : "+originalMeterReadStatus);
        Integer invoiceId=null;
        // if replacement meter read is not come
        if(!originalMeterReadStatus.equals("Done")){
            throwException("Meter Read status should be Done", null,INVALID_FILE_STATUS);
        }
        invoiceId = ediTransactionBean.generateInvoice(originalMeterData.get(InvoiceReadConstants.FILE_ID.getValue()),companyId, INVOICE_NR);
        try {
            validateInvoice(invoiceId, totalAmount);
        } catch (SessionInternalError e) {
            status = FILE_MISMATCH_STATUS;
            comment=e.getMessage();
        }
    }

}
