package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;
import org.springframework.batch.core.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;


public class PaymentParserTask extends AbstractScheduledTransactionProcessor implements StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaymentParserTask.class));

    public static enum PaymentRead implements FileStructure {
        KEY, HDR, ACT, QTY, REA;
    }

    public static enum PaymentReadField {
        TRACE_NR,
        UTILITY_CUST_ACCT_NR,
        DISCOUNT_AMT,
        ORG_INVOICE_AMT,
        PYMT_AMOUNT,
        INVOICE_NR,
        TOTAL_TRANS_AMT

    }

    protected static final ParameterDescription INCONSISTENT_PAYMENT_STATUS_NAME =
            new ParameterDescription("inconsistent_payment_status", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription DUPLICATE_TRANSACTION_STATUS_NAME =
            new ParameterDescription("duplication_transaction", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription DONE_STATUS_NAME =
            new ParameterDescription("done", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription INVALID_DATA_STATUS_NAME =
            new ParameterDescription("invalid_data", true, ParameterDescription.Type.STR);
    protected static final ParameterDescription REJECTED_STATUS_NAME =
            new ParameterDescription("rejected", true, ParameterDescription.Type.STR);

    public static final Long TRANSACTION_SET = new Long(820);

    private List<EDIFileRecordWS> actRecords=new ArrayList<>();
    private String INCONSISTENT_PAYMENT_STATUS;
    private String DUPLICATE_TRANSACTION_STATUS;
    private String DONE_STATUS;
    private String INVALID_DATA_STATUS;
    private String REJECTED_STATUS;

    private int companyId;
    private String comment;

    private Integer ediTypeId;
    private String supplierDUNS;
    private String utilityDUNS;

    private IWebServicesSessionBean webServicesSessionSpringBean;
    private IEDITransactionBean ediTransactionBean;
    private IPaymentSessionBean paymentSessionBean;

    private EDIFileRecordBL ediFileRecordBL;
    private InvoiceDAS invoiceDAS;
    private PaymentMethodTypeDAS paymentMethodTypeDAS;
    private Map<String, Map<String, Object>> paymentDetails = new HashMap<>();

    {
        descriptions.add(INCONSISTENT_PAYMENT_STATUS_NAME);
        descriptions.add(DUPLICATE_TRANSACTION_STATUS_NAME);
        descriptions.add(DONE_STATUS_NAME);
        descriptions.add(INVALID_DATA_STATUS_NAME);
        descriptions.add(REJECTED_STATUS_NAME);
    }


    @Override
    public String getTaskName() {
        return "Payment parser: " + getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    protected String getJobName() {
        return Context.Name.BATCH_EDI_PAYMENT_TRANSACTION_PROCESS.getName();
    }

    @Override
    public void preBatchConfiguration(Map jobParams) {
        IWebServicesSessionBean webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

        LOG.debug("Execute PaymentReadParserTask  plugin.");

        EDI_TYPE_ID = (Integer) companyMetaFieldValueMap.get(FileConstants.PAYMENT_EDI_TYPE_ID_META_FIELD_NAME);
        if(EDI_TYPE_ID == null) {
            throw new SessionInternalError("EDI type id not valid : " + EDI_TYPE_ID);
        }

        EDITypeWS ediType = null;
        //todo : refactor
        ediType = webServicesSessionSpringBean.getEDIType(EDI_TYPE_ID);
        if (ediType == null)
            throw new SessionInternalError("EDI type id not found : " + EDI_TYPE_ID);


        jobParams.put("INCONSISTENT_PAYMENT_STATUS", new JobParameter(parameters.get(INCONSISTENT_PAYMENT_STATUS_NAME.getName())));
        jobParams.put("DUPLICATE_TRANSACTION_STATUS", new JobParameter(parameters.get(DUPLICATE_TRANSACTION_STATUS_NAME.getName())));
        jobParams.put("DONE_STATUS", new JobParameter(parameters.get(DONE_STATUS_NAME.getName())));
        jobParams.put("INVALID_DATA_STATUS", new JobParameter(parameters.get(INVALID_DATA_STATUS_NAME.getName())));
        jobParams.put("REJECTED_STATUS", new JobParameter(parameters.get(REJECTED_STATUS_NAME.getName())));

        jobParams.put("ediTypeId", new JobParameter(EDI_TYPE_ID.longValue()));
        jobParams.put("supplierDUNS", new JobParameter(SUPPLIER_DUNS));
        jobParams.put("utilityDUNS", new JobParameter(UTILITY_DUNS));
//        Set transaction type from suffix.
        jobParams.put("TRANSACTION_SET", new JobParameter(ediType.getEdiSuffix()));
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("EDI File Invoice Item Processor : Before Step");
        paymentSessionBean = (IPaymentSessionBean) Context.getBean(Context.Name.PAYMENT_SESSION);
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
        JobParameters jobParameters = stepExecution.getJobParameters();

        ediTypeId = jobParameters.getLong("ediTypeId").intValue();
        utilityDUNS = jobParameters.getString("utilityDUNS");
        supplierDUNS = jobParameters.getString("supplierDUNS");


        INCONSISTENT_PAYMENT_STATUS = jobParameters.getString("INCONSISTENT_PAYMENT_STATUS");
        DUPLICATE_TRANSACTION_STATUS = jobParameters.getString("DUPLICATE_TRANSACTION_STATUS");
        DONE_STATUS = jobParameters.getString("DONE_STATUS");
        INVALID_DATA_STATUS = jobParameters.getString("INVALID_DATA_STATUS");
        REJECTED_STATUS = jobParameters.getString("REJACTED_STATUS");
        companyId = jobParameters.getLong("companyId").intValue();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return null;
    }

    public void processFile(EDIFileWS ediFileWS, String escapeExceptionStatus) throws Exception{
        this.escapeExceptionStatus=escapeExceptionStatus;
        this.ediFile = ediFileWS;
        setMetaFieldValues(companyId);

        try {
            processPaymentReadFile();
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
                break;
            }
        }
        ediFileWS.setEdiFileStatusWS(statusWS);
        ediFileWS.setComment(comment);
        if(exceptionCode!=null)ediFileWS.setExceptionCode(exceptionCode);
    }

    public void bindPluginParameter(Map<String, String> pluginParameter){
        paymentSessionBean = (IPaymentSessionBean) Context.getBean(Context.Name.PAYMENT_SESSION);
        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);

        companyId = Integer.parseInt(pluginParameter.get("companyId"));
        INCONSISTENT_PAYMENT_STATUS = pluginParameter.get("inconsistent_payment_status");
        DUPLICATE_TRANSACTION_STATUS = pluginParameter.get("duplication_transaction");
        DONE_STATUS = pluginParameter.get("done");
        INVALID_DATA_STATUS = pluginParameter.get("invalid_data");
        REJECTED_STATUS = pluginParameter.get("rejected");
        setMetaFieldValues(companyId);
    }

    private void clearData(){

        comment=null;
        status=null;
        paymentDetails = new HashMap<>();

        ediFile=null;
        escapeExceptionStatus=null;
        recordFields=null;
        recordFieldsList=null;
        recordLevelExceptionCode=null;
        exceptionCode=null;
    }

    public EDIFileWS process(EDIFileWS ediFileWS) throws Exception {
        LOG.debug("Payment Read Task Process Method");
        LOG.debug("Payment Read : "+ediFileWS.getName());
        try {
            if (ediFileWS.getEdiFileStatusWS().getId() == FileConstants.EDI_STATUS_PROCESSED) {
                this.ediFile = ediFileWS;
                setMetaFieldValues(companyId);
                processPaymentReadFile();
            }
        } catch (Exception ex) {
            LOG.error(ex);
            status = (status == null) ? INVALID_DATA_STATUS : status;
            comment = ex.getMessage();
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
        clearData();
        return ediFileWS;
    }




    public void processPaymentReadFile() {
        ediTypeId = (Integer) companyMetaFieldValueMap.get(FileConstants.PAYMENT_EDI_TYPE_ID_META_FIELD_NAME);
        invoiceDAS = new InvoiceDAS();
        ediFileRecordBL=new EDIFileRecordBL();
        paymentMethodTypeDAS = new PaymentMethodTypeDAS();

        List<String> maidenRecords = new LinkedList<String>();
        maidenRecords.add(PaymentRead.KEY.toString());
        maidenRecords.add(PaymentRead.HDR.toString());
        parseRecords(maidenRecords, ediFile.getEDIFileRecordWSes());

        validatePaymentRead();

        LOG.info("Validating and saving each Payment record");

        Boolean inconsistentPaymentRead = false;
        for (EDIFileRecordWS ediFileRecordWS : actRecords) {
            try{
                //validating and making payment of each record. If any record invalid or exception through, move to next record
                Map<String, Object> paymentDetail=paymentDetails.get(ediFileRecordWS.getId() + "");
                recordLevelExceptionCode=null;
                validateAndSavePayment(paymentDetail);
            }catch (Exception e){
                LOG.debug("Unable to making payment ACT record "+ediFileRecordWS.getId());
                LOG.debug("Exception on processing ACT record "+e.getMessage());
                inconsistentPaymentRead=true;
                ediFileRecordWS.setComment(e.getMessage());

                if(recordLevelExceptionCode!=null){
                    setRecordLevelExceptionCode(ediFileRecordWS);
                }
            }
        }

        if(inconsistentPaymentRead){
            status=INCONSISTENT_PAYMENT_STATUS;
            exceptionCode=FileConstants.PAYMENT_INCONSISTENT_RECORD_EXP_CODE;
        }else{
            status = DONE_STATUS;
        }
    }


    private void validatePaymentRead() {


        String TRACE_NR = findField(PaymentRead.HDR, PaymentReadField.TRACE_NR.toString());
        LOG.debug(PaymentReadField.TRACE_NR.toString() + "value found : " + TRACE_NR);
        try {
            ediTransactionBean.isUniqueKeyExistForFile(companyId, ediTypeId, ediFile.getId(), PaymentReadField.TRACE_NR.toString(), TRACE_NR, TransactionType.INBOUND);
        } catch (Exception e) {
            throwException(e.getMessage(), FileConstants.PAYMENT_DUPLICATE_TRANSACTION_EXP_CODE, INVALID_DATA_STATUS);
        }


        LOG.info("Validating whole file record");
        BigDecimal totalTransactionAmount = (BigDecimal) findField(PaymentRead.HDR, PaymentReadField.TOTAL_TRANS_AMT.toString(), "BigDecimal", true);

        getPaymentACTData();

        BigDecimal sumOfTotalPayment = new BigDecimal(0);
        for (EDIFileRecordWS  ediFileRecordWS : actRecords) {
            sumOfTotalPayment = sumOfTotalPayment.add((BigDecimal) (paymentDetails.get(ediFileRecordWS.getId()+"").get(PaymentReadField.PYMT_AMOUNT.toString())));
        }

        if (totalTransactionAmount.compareTo(sumOfTotalPayment) != 0) {
            throwException("Payment Validation fail: totalTransactionAmount " + totalTransactionAmount + " is not equal to the " + sumOfTotalPayment, FileConstants.PAYMENT_INVALID_PAYMENT_TALLY_EXP_CODE, INVALID_DATA_STATUS);
        }
    }


    private void getPaymentACTData() {

        EDIFileRecordWS[] recordList = ediFile.getEDIFileRecordWSes();
        int i = 0;
        for (EDIFileRecordWS ediFileRecordWS : recordList) {
            if (ediFileRecordWS.getHeader().equals("ACT")) {
                actRecords.add(ediFileRecordWS);
                Map<String, Object> paymentFields = new HashMap<String, Object>();
                Map<String, String> ACTFields = parseRecord(recordList[i]);
                paymentFields.put("RECORD_ID", ediFileRecordWS.getId());
                paymentFields.put(PaymentReadField.PYMT_AMOUNT.toString(), findField(ACTFields, PaymentReadField.PYMT_AMOUNT.toString(), "BigDecimal", false));
                paymentFields.put(PaymentReadField.DISCOUNT_AMT.toString(), findField(ACTFields, PaymentReadField.DISCOUNT_AMT.toString(), "BigDecimal", false));
                paymentFields.put(PaymentReadField.ORG_INVOICE_AMT.toString(), findField(ACTFields, PaymentReadField.ORG_INVOICE_AMT.toString(), "BigDecimal", false));
                paymentFields.put(PaymentReadField.INVOICE_NR.toString(), findField(ACTFields, PaymentReadField.INVOICE_NR.toString(), false));
                paymentFields.put(PaymentReadField.UTILITY_CUST_ACCT_NR.toString(), findField(ACTFields, PaymentReadField.UTILITY_CUST_ACCT_NR.toString(), true));
                paymentDetails.put(ediFileRecordWS.getId() + "", paymentFields);
            }
            i++;
        }
    }

    public void validateAndSavePayment(Map<String, Object> paymentDetail) throws SessionInternalError {
        String customerAccountNumber = (String) paymentDetail.get(PaymentReadField.UTILITY_CUST_ACCT_NR.toString());
        UserDTO userDTO=null;
        try {
            userDTO = ediTransactionBean.findUserByAccountNumber(companyId, PaymentReadField.UTILITY_CUST_ACCT_NR.toString(), customerAccountNumber);
        }catch (SessionInternalError e){
            recordLevelExceptionCode=FileConstants.PAYMENT_UNKNOWN_ACCOUNT_EXP_CODE;
            throwException(e.getMessage(), null, REJECTED_STATUS);
        }

        BigDecimal paymentAmount = (paymentDetail.get(PaymentReadField.PYMT_AMOUNT.toString())!=null? (BigDecimal)paymentDetail.get(PaymentReadField.PYMT_AMOUNT.toString()):BigDecimal.ZERO).abs();
        BigDecimal discountAmount = (paymentDetail.get(PaymentReadField.DISCOUNT_AMT.toString())!=null? (BigDecimal)paymentDetail.get(PaymentReadField.DISCOUNT_AMT.toString()):BigDecimal.ZERO).abs();
        BigDecimal originalInvoiceAmount =(paymentDetail.get(PaymentReadField.ORG_INVOICE_AMT.toString())!=null? (BigDecimal)paymentDetail.get(PaymentReadField.ORG_INVOICE_AMT.toString()):BigDecimal.ZERO).abs();

        if (originalInvoiceAmount.subtract(discountAmount).compareTo(paymentAmount) != 0) {
            recordLevelExceptionCode=FileConstants.PAYMENT_INCORRECT_CLCULTION_EXP_CODE;
            throwException("Payment Details is Inconsistent", null, INVALID_DATA_STATUS);
        }

        try{
            PaymentWS payment = new PaymentWS();
            payment.setAmount(originalInvoiceAmount);
            payment.setIsRefund(new Integer(0));
            payment.setPaymentDate(Calendar.getInstance().getTime());
            payment.setResultId(com.sapienter.jbilling.server.util.Constants.RESULT_ENTERED);
            payment.setCurrencyId(userDTO.getCompany().getCurrencyId());
            payment.setUserId(userDTO.getId());

            try(PaymentInformationWS ediPaymentInfo = new PaymentInformationWS()) {
                ediPaymentInfo.setPaymentMethodTypeId(paymentMethodTypeDAS.getPaymentMethodTypeByTemplate(CommonConstants.EDI, userDTO.getEntity().getId()).getId());
                ediPaymentInfo.setPaymentMethodId(com.sapienter.jbilling.server.util.Constants.PAYMENT_EDI);
                ediPaymentInfo.setProcessingOrder(new Integer(1));
                payment.getPaymentInstruments().add(ediPaymentInfo);
            }catch (Exception exception){
                LOG.debug("Exception: " +exception);
                throw new SessionInternalError(exception);
            }

            Integer invoiceId = null;
            if(paymentDetail.get(PaymentReadField.INVOICE_NR.toString()) == null){
                paymentSessionBean.applyPayment(new PaymentDTOEx(payment), invoiceId, null);
                return;
            }

            invoiceId = invoiceDAS.findInvoiceByMetaFieldValue(companyId, PaymentReadField.INVOICE_NR.toString(), (String)paymentDetail.get(PaymentReadField.INVOICE_NR.toString()));
            if (invoiceId == null) {
                throwException("No Invoice found for INVOICE_NR " + paymentDetail.get(PaymentReadField.INVOICE_NR.toString()), null, null);
            }

            paymentSessionBean.applyPayment(new PaymentDTOEx(payment), invoiceId, null);
            payment.close();
        }
        catch(Exception exp){
            LOG.debug("Exception: "+exp.getMessage());
            throw new SessionInternalError(exp);
        }
    }
}