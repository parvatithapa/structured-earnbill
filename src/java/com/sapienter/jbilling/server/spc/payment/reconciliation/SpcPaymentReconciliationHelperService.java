package com.sapienter.jbilling.server.spc.payment.reconciliation;

import static com.sapienter.jbilling.server.spc.payment.reconciliation.SpcPaymentReconciliationRecord.SETTLEMENT_DATE_FORMAT;
import static com.sapienter.jbilling.server.spc.payment.reconciliation.SpcPaymentReconciliationRecord.TRANSACTION_DATE_TIME_FORMAT;
import static com.sapienter.jbilling.server.spc.payment.reconciliation.PaymentReconciliationScheduledTask.PARAM_SETTLEMENT_DATE;
import static com.sapienter.jbilling.server.spc.payment.reconciliation.PaymentReconciliationScheduledTask.PARAM_TRANSACTION_DATE_TIME;
import static com.sapienter.jbilling.server.spc.payment.reconciliation.PaymentReconciliationScheduledTask.SPC_PARAM_UNALLOCATED_PAYMENT_ACCOUNT;
import static com.sapienter.jbilling.server.spc.payment.reconciliation.PaymentReconciliationScheduledTask.AGL_PARAM_UNALLOCATED_PAYMENT_ACCOUNT;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import com.opencsv.CSVWriter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Transactional
@Component
public class SpcPaymentReconciliationHelperService {

    private static final String SPC_PAYMENT_PROCESSOR_BPAY = "Payment-Reconciliation";
    private static final String AGL_PAYMENT_PROCESSOR_BPAY = "BPAY-AGL";

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name = "jBillingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Resource
    private IPaymentSessionBean paymentBean;
    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;
    @Autowired
    private ApplicationContext applicationContext;
    
    /**
     * reconcilePayment
     * @param record
     */
    @Async("asyncTaskExecutor")
    public Future<Boolean> reconcilePayment(SpcPaymentReconciliationRecord record, Map<String, String> parameters, boolean isAGLFile) {
        String errorPath = parameters.get(PaymentReconciliationScheduledTask.ERROR_PATH);
        try {
            validateRequriedPaymentLevelMfForEntity(record.getEntityId(), parameters);
            CompanyDTO entity = new CompanyDAS().findNow(record.getEntityId());
            Locale entityLocale = entity.getLanguage().asLocale();
            String datePrettyFormat = applicationContext.getMessage("date.pretty.format", null, entityLocale);
            logger.debug("date format {} for entity {} with locale {}", datePrettyFormat, entity.getId(), entityLocale);
            String dateTimeFormat =  applicationContext.getMessage("date.time.format", null, entity.getLanguage().asLocale());
            logger.debug("date time format {} for entity {} with locale {}", dateTimeFormat, entity.getId(), entityLocale);
            Date txDate = convertDate(record.getTransactionDateTime(), TRANSACTION_DATE_TIME_FORMAT, dateTimeFormat);
            logger.debug("Payment Create Date {}", txDate);

            if(record.isBPay()) {
            	String refNumber = record.getPaymentFieldByName("BPAY Ref");
                logger.debug("processing bpay record for ref number {} ", refNumber);
                Optional<Integer> userId = findUserIdByBpayRefNumber(record);
                String paramName = null;
                if(isAGLFile) {
                    paramName = AGL_PARAM_UNALLOCATED_PAYMENT_ACCOUNT.getName();
                } else {
                    paramName =  SPC_PARAM_UNALLOCATED_PAYMENT_ACCOUNT.getName();
                }
                Integer unAllocatedBPayUserId = null;
                if(!userId.isPresent()) {
                    if(!parameters.containsKey(paramName)) {
                        logger.debug("skipping record since {} not configured for entity {}", paramName, record.getEntityId());
                        writeErrorLog(record.getPaymentFieldByName("BPAY Ref"), record.getTransactionId(), String.format("skipping record since {%s} not configured for entity {%d}", paramName, record.getEntityId()), errorPath);
                        return new AsyncResult<Boolean> (Boolean.FALSE);
                    }
                    unAllocatedBPayUserId = Integer.parseInt(parameters.get(paramName));
                    logger.debug("adding record {} on unallocated bpay payment account {}", record, unAllocatedBPayUserId);
                    userId = Optional.of(unAllocatedBPayUserId);
                }
                String txId = record.getTransactionId();
                Optional<Integer> payment = findPaymentByTransactionId(record);
                if(payment.isPresent()) {
                    logger.debug("skipping bpay record {} since payment {} found for transaction id {}", record, payment.get(), txId);
                    writeErrorLog(record.getPaymentFieldByName("BPAY Ref"), record.getTransactionId(), String.format("skipping bpay record since payment {%d} found for transaction id/receipt number: {%s}", payment.get(), txId), errorPath);
                    return new AsyncResult<Boolean> (Boolean.FALSE);
                }
                // lock user to avoid any concurrency issue.
                UserDTO user = new UserDAS().findForUpdate(userId.get());
                record.setUserId(userId.get());
                PaymentInformationDTO bpayInstrument = null;

                if(!userId.get().equals(unAllocatedBPayUserId)) {
                    for(PaymentInformationDTO instrument : user.getPaymentInstruments()) {
                        if(instrument.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName().equals("BPAY") &&
                           (refNumber.equals(new PaymentInformationBL().getStringMetaFieldByType(instrument, MetaFieldType.BPAY_REF)))) {
                           bpayInstrument = instrument;
                           break;
                       }
                    }
                } else if(Objects.nonNull(unAllocatedBPayUserId) && unAllocatedBPayUserId.equals(userId.get())) {
                	for(PaymentInformationDTO instrument : user.getPaymentInstruments()) {
                         if(instrument.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName().equals("BPAY")) {
                            bpayInstrument = instrument;
                            break;
                        }
                    }
                }

                if(null == bpayInstrument) {
                    logger.debug("skipping record {} since no bpay instrument found on user {}", record, userId);
                    writeErrorLog(record.getPaymentFieldByName("BPAY Ref"), record.getTransactionId(), String.format("skipping record since no bpay instrument found on user: %d", userId.get()), errorPath);
                    return new AsyncResult<Boolean> (Boolean.FALSE);
                }
                BigDecimal amount = new BigDecimal(record.getPaymentFieldByName("Amount"));
                PaymentDTOEx bpayPaymentRecord = createPaymentRecord(amount, bpayInstrument, user, null, Constants.RESULT_ENTERED, txDate);
                Integer paymentId = paymentBean.applyPayment(bpayPaymentRecord, null, user.getId());
                String settlementDate = convertDateToString(record.getSettlementDate(), SETTLEMENT_DATE_FORMAT, datePrettyFormat);
                logger.debug("settlementDate {}", settlementDate);
                String transactionDate = convertDateToString(record.getTransactionDateTime(), TRANSACTION_DATE_TIME_FORMAT, dateTimeFormat);
                logger.debug("transactionDate {}", transactionDate);
                setMetaFieldsOnPayment(paymentId, record.getEntityId(), transactionDate, settlementDate, parameters);
                logger.debug("bpay payment {} created for record {} for user {}", paymentId, record, userId);
                PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(record, resolveProcessorNameFromInstrument(userId.get()));
                logger.debug("bpay paymentAuthorization {} build for payment {}", paymentAuthorization, paymentId);
                new PaymentAuthorizationBL().create(paymentAuthorization, paymentId);
                logger.debug("bpay paymentAuthorization {} added on payment {}", paymentAuthorization, paymentId);
            } else if(record.isBankAccountPay()) {
                logger.debug("processing direct debit payment {}", record);
                if(!record.isDeclined()) {
                    logger.debug("skipping direct debit record {} for entity {} since only processing Declined payment", record, record.getEntityId());
                    return new AsyncResult<Boolean> (Boolean.FALSE);
                }
                Optional<Integer> paymentId = findPaymentByTransactionId(record);
                if(!paymentId.isPresent()) {
                    return new AsyncResult<Boolean> (Boolean.FALSE);
                }
                Optional<Integer> refundId = findAvailableRefundForPayment(paymentId.get());
                if(refundId.isPresent()) {
                    logger.debug("refund payment {} found payment {}", refundId.get(), paymentId.get());
                    return new AsyncResult<Boolean> (Boolean.FALSE);
                }
                PaymentDTO payment = new PaymentDAS().findNow(paymentId.get());
                List<Integer> linkedInvoiceIds = payment.getAllLinkdInvoices();
                logger.debug("removing invoices {} linked to payment {}", linkedInvoiceIds, paymentId.get());
                api.removeAllPaymentLinks(paymentId.get());
                logger.debug("removed linked invoice to payment {}", paymentId.get());
                PaymentInformationDTO instrument = payment.getPaymentInstrumentsInfo().get(0).getPaymentInformation().getSaveableDTO();
                BigDecimal amount = new BigDecimal(record.getPaymentFieldByName("Amount"));
                PaymentDTOEx reversalPayment = createPaymentRecord(amount.abs(), instrument, payment.getBaseUser(),
                        payment.getId(), Constants.RESULT_ENTERED, txDate);
                try(PaymentInformationBL piBl = new PaymentInformationBL()) {
                    instrument.setPaymentMethod(new PaymentMethodDAS().find(piBl.getPaymentMethodForPaymentMethodType(instrument)));
                }
                reversalPayment.setPaymentNotes("Reversal payment of payment "+ payment.getId());
                reversalPayment.setInstrument(instrument);
                reversalPayment.setInvoiceIds(linkedInvoiceIds);
                PaymentBL paymentBL = new PaymentBL();
                // creating reversal payment.
                paymentBL.create(reversalPayment, payment.getBaseUser().getId());
                payment.setBalance(payment.getBalance().subtract(reversalPayment.getAmount()));
                Integer reversalPaymentId = paymentBL.getDTO().getId();
                String settlementDate = convertDateToString(record.getSettlementDate(), SETTLEMENT_DATE_FORMAT, datePrettyFormat);
                String transactionDate = convertDateToString(record.getTransactionDateTime(), TRANSACTION_DATE_TIME_FORMAT, dateTimeFormat);
                setMetaFieldsOnPayment(reversalPaymentId, record.getEntityId(), transactionDate, settlementDate, parameters);
                logger.debug("reversal payment {} created for payment {}", reversalPaymentId, paymentId.get());
                reversalPayment.setId(reversalPaymentId);
                EventManager.process(new PaymentSuccessfulEvent(record.getEntityId(), reversalPayment));
            }
        } catch(NumberFormatException ex) {
            logger.debug("BPAY Ref number is out of range exception {} ", record, ex);
            writeErrorLog(record.getPaymentFieldByName("BPAY Ref"), record.getTransactionId(), String.format("payment Reconciliation failed for receipt number: {%s} and reason is: BPAY Ref number is out of range : {%s}", record.getTransactionId(), record.getPaymentFieldByName("BPAY Ref")), errorPath);
            TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        } catch(Exception ex) {
            logger.debug("payment Reconciliation failed for record {} ", record, ex);
            writeErrorLog(record.getPaymentFieldByName("BPAY Ref"), record.getTransactionId(), String.format("payment Reconciliation failed for receipt number: {%s} and reason is: {%s}", record.getTransactionId(), ex.toString()), errorPath);
            TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
        }
        return new AsyncResult<Boolean> (Boolean.TRUE);
    }

    /**
     * Write data into CSV file
     * @param referenceNo
     * @param receiptNo
     * @param reason
     * @param errorPath
     */
	public void writeErrorLog(String referenceNo, String receiptNo, String reason, String errorPath) {
		DateFormat currentTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String currentTime = currentTimeFormat.format(new Date());
		File errorLogFile = new File(errorPath);
		try (FileWriter outputfile = new FileWriter(errorLogFile, true);
			CSVWriter writer = new CSVWriter(outputfile)) {
			writer.writeNext(new String []{referenceNo, receiptNo, reason, currentTime});
		} catch (IOException ex) {
			logger.error("Exception while writing data into error log csv {}", ex);
		}
	}

    /**
     * Converts String date value from one format to other format
     * @param date
     * @param actualFormat
     * @param targetFormat
     * @return
     * @throws ParseException
     */
    private static Date convertDate(String date, String actualFormat, String targetFormat) throws ParseException {
        logger.debug("converting {} from {} to {} format", date, actualFormat, targetFormat);
        DateFormat actualDateFormat = new SimpleDateFormat(actualFormat);
        Date actualDate = actualDateFormat.parse(date);
        DateFormat targateDateFormat = new SimpleDateFormat(targetFormat);
        Date targetDate = targateDateFormat.parse(targateDateFormat.format(actualDate));
        logger.debug("converted string date {} to date {} with format {}", date, targetDate, targetFormat);
        return targetDate;
    }


    /**
     * converts date to target format
     * @param date
     * @param actualFormat
     * @param targetFormat
     * @return
     * @throws ParseException
     */
    private static String convertDateToString(String date, String actualFormat, String targetFormat) throws ParseException {
        logger.debug("converting {} from {} to {} format", date, actualFormat, targetFormat);
        DateFormat actualDateFormat = new SimpleDateFormat(actualFormat);
        Date actualDate = actualDateFormat.parse(date);
        DateFormat targateDateFormat = new SimpleDateFormat(targetFormat);
        String targetDate = targateDateFormat.format(actualDate);
        logger.debug("converted string date {} to date {} with format {}", date, targetDate, targetFormat);
        return targetDate;
    }

    private static final String FIND_REFUND_AVAILABLE_FOR_PAYMENT_ID_SQL = "SELECT id FROM payment WHERE payment_id = ? AND deleted = 0";
    private Optional<Integer> findAvailableRefundForPayment(Integer paymentId) {
        List<Integer> refunds = jdbcTemplate.queryForList(FIND_REFUND_AVAILABLE_FOR_PAYMENT_ID_SQL, Integer.class, paymentId);
        if(CollectionUtils.isNotEmpty(refunds)) {
            if(refunds.size()!=1) {
                logger.error("refund payments {} found for payment id {}", refunds, paymentId);
                throw new SessionInternalError("more then one refund payment found for payment id "+ paymentId);
            }
            logger.debug("refund payment id {} found for payment Id {} ", refunds, paymentId);
            return Optional.of(refunds.get(0));
        }
        return Optional.empty();

    }

    private static final String FIND_PAYMENT_BY_TRANSACTION_ID_SQL = "SELECT id FROM payment WHERE id IN "
            + "(SELECT payment_id FROM payment_authorization WHERE transaction_id = ?) AND deleted = 0";

    /**
     * Finds payment id for given transaction id
     * @param txId
     * @return
     */
    private Optional<Integer> findPaymentByTransactionId(SpcPaymentReconciliationRecord record) {
    	String txId = record.getTransactionId();
        List<Integer> payments = jdbcTemplate.queryForList(FIND_PAYMENT_BY_TRANSACTION_ID_SQL, Integer.class, txId);
        if(CollectionUtils.isNotEmpty(payments)) {
            if(payments.size()!= 1) {
                logger.error("payments {} found for transaction id {}", payments, txId);
                throw new SessionInternalError("more then one payment found for transaction id "+ txId);
            }
            logger.debug("payment id {} found for Transaction Id {} ", payments, txId);
            return Optional.of(payments.get(0));
        }
        return Optional.empty();
    }

    private static final String FIND_USER_BY_BPAY_REF_SQL = "SELECT id FROM base_user WHERE id IN "
            + "(SELECT user_id FROM payment_information WHERE id IN "
            + "(SELECT payment_information_id FROM payment_information_meta_fields_map "
            + "WHERE meta_field_value_id IN (SELECT id "
            + "FROM meta_field_value WHERE string_value= ? AND meta_field_name_id "
            + "IN (SELECT id FROM meta_field_name WHERE entity_id = ? AND field_usage = ?))) "
            + "AND user_id IS NOT NULL AND deleted = 0) AND deleted = 0";

    /**
     * finds user id from given bpay ref number
     * @param refNumber
     * @param entityId
     * @return
     */
    private Optional<Integer> findUserIdByBpayRefNumber(SpcPaymentReconciliationRecord record) {
    	String refNumber = record.getPaymentFieldByName("BPAY Ref");
    	Integer entityId = record.getEntityId();
        List<Integer> users = jdbcTemplate.queryForList(FIND_USER_BY_BPAY_REF_SQL, Integer.class, refNumber, entityId, MetaFieldType.BPAY_REF.name());
        if(CollectionUtils.isNotEmpty(users)) {
            if(users.size()!=1) {
                logger.error("multiple users {} found with bpay ref number {} for entity {}", users, refNumber, entityId);
                return Optional.empty();
            }
            Integer userId = users.get(0);
            logger.debug("user id {} found for bpay ref {} ", userId, refNumber);
            return Optional.of(userId);
        }
        logger.debug("no user found for bpay ref number {} for entity {}", refNumber, entityId);
        return Optional.empty();
    }

    /**
     * Creates {@link PaymentDTOEx}
     * @param amount
     * @param paymentInstrument
     * @param user
     * @param parentPaymentId
     * @param paymentResultId
     * @return
     */
    private PaymentDTOEx createPaymentRecord(BigDecimal amount, PaymentInformationDTO paymentInstrument, UserDTO user, Integer parentPaymentId, Integer paymentResultId, Date paymentDate) {
        PaymentWS paymentWS = new PaymentWS();
        paymentWS.setAmount(amount);
        paymentWS.setBalance(amount);
        paymentWS.setCreateDatetime(Calendar.getInstance().getTime());
        paymentWS.setCurrencyId(user.getCurrencyId());
        PaymentInformationWS instrument = PaymentInformationBL.getWS(paymentInstrument);
        instrument.setPaymentMethodId(null);
        List<PaymentInformationWS> instruments = Arrays.asList(instrument);
        paymentWS.setPaymentInstruments(instruments);
        paymentWS.setUserId(user.getId());
        paymentWS.setUserPaymentInstruments(instruments);
        paymentWS.setPaymentDate(paymentDate);
        if(null!= parentPaymentId) {
            paymentWS.setIsRefund(1);
            paymentWS.setPaymentId(parentPaymentId);
        } else {
            paymentWS.setIsRefund(0);
        }
        paymentWS.setResultId(paymentResultId);
        return new PaymentDTOEx(paymentWS);
    }

    /**
     * Creates {@link PaymentAuthorizationDTO} from {@link SpcPaymentReconciliationRecord}
     * @param record
     * @return
     */
    private PaymentAuthorizationDTO buildPaymentAuthorization(SpcPaymentReconciliationRecord record, String processorName) {
        PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
        paymentAuthDTO.setProcessor(processorName);

        String txID = record.getPaymentFieldByName("ReceiptNumber");
        paymentAuthDTO.setTransactionId(txID);
        logger.debug("transactionId/code1 [{}]", txID);
        paymentAuthDTO.setCode1(txID);
        paymentAuthDTO.setResponseMessage(record.getPaymentFieldByName("ResponseText"));
        String orderNumber = record.getPaymentFieldByName("OrderNumber"); // Unique Order Number
        if(StringUtils.isNotEmpty(orderNumber)) {
            paymentAuthDTO.setCode2(orderNumber);
        }
        String responseCode = record.getPaymentFieldByName("ResponseCode");
        if(StringUtils.isNotEmpty(responseCode)) {
            paymentAuthDTO.setApprovalCode(responseCode);
        }
        paymentAuthDTO.setCode3(record.getPaymentFieldByName("Status"));
        return paymentAuthDTO;
    }

    private String resolveProcessorNameFromInstrument(Integer userId) {
        return isAGL(userId) ? AGL_PAYMENT_PROCESSOR_BPAY: SPC_PAYMENT_PROCESSOR_BPAY;
    }

    private String getInvoiceDesignFromUserObject(UserDTO user) {
        return Objects.isNull(user.getCustomer()) ? null : Objects.toString(user.getCustomer().getInvoiceDesign(),"");
    }

    private boolean isAGL(Integer userId) {
        return Context.getBean(SpcHelperService.class).isAGL(userId, getInvoiceDesignFromUserObject(UserBL.getUserEntity(userId)));
    }

    /**
     * Validates required payment level meta fields for entity.
     * @param entityId
     * @param parameters
     */
    private void validateRequriedPaymentLevelMfForEntity(Integer entityId, Map<String, String> parameters) {
        String settlementMfName = parameters.get(PARAM_SETTLEMENT_DATE.getName());
        if(StringUtils.isEmpty(settlementMfName)) {
            logger.debug("Parameter {} not configured on plugin for entity {}", PARAM_SETTLEMENT_DATE.getName(), entityId);
            throw new SessionInternalError("parameter "+ PARAM_SETTLEMENT_DATE.getName() +
                    " not configured for plugin for entity "+ entityId);
        }

        MetaField settlementMetaField = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.PAYMENT }, settlementMfName);
        if( null == settlementMetaField ) {
            logger.debug("MetaField {} not configured on payment for entity {}", settlementMfName, entityId);
            throw new SessionInternalError("Meta Field "+ settlementMfName + " not found for entity " + entityId + " on payment level");
        }
        logger.debug("MetaField {} found for entity {} on payment level", settlementMfName, settlementMetaField);
        String transactionDateTimeMfName = parameters.get(PARAM_TRANSACTION_DATE_TIME.getName());
        if(StringUtils.isEmpty(transactionDateTimeMfName)) {
            logger.debug("Parameter {} not configured on plugin for entity {}", PARAM_TRANSACTION_DATE_TIME.getName(), entityId);
            throw new SessionInternalError("parameter "+ PARAM_TRANSACTION_DATE_TIME.getName() +
                    " not configured for plugin for entity "+ entityId);
        }
        MetaField transactionDateTime = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.PAYMENT }, transactionDateTimeMfName);
        if( null == transactionDateTime ) {
            logger.debug("MetaField {} not configured on payment for entity {}", transactionDateTimeMfName, entityId);
            throw new SessionInternalError("Meta Field "+ transactionDateTimeMfName + " not found for entity " + entityId + " on payment level");
        }
        logger.debug("MetaField {} found for entity {} on payment level", transactionDateTimeMfName, transactionDateTime);
    }

    /**
     * sets settlementDate and txDateTime on payment level metaField
     * @param paymentId
     * @param entityId
     * @param txDateTime
     * @param settlementDate
     * @param parameters
     */
    private void setMetaFieldsOnPayment(Integer paymentId, Integer entityId, String txDateTime, String settlementDate, Map<String, String> parameters) {
        PaymentDAS paymentDAS = new PaymentDAS();
        PaymentDTO payment = paymentDAS.findNow(paymentId);
        String transactionDateTimeMfName = parameters.get(PARAM_TRANSACTION_DATE_TIME.getName());
        MetaField transactionDateTime = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.PAYMENT }, transactionDateTimeMfName);
        StringMetaFieldValue txDateTimeMetaFieldValue = new StringMetaFieldValue(transactionDateTime);
        txDateTimeMetaFieldValue.setValue(txDateTime);
        logger.debug("{} created for payment {} ", txDateTimeMetaFieldValue, paymentId);
        String settlementMfName = parameters.get(PARAM_SETTLEMENT_DATE.getName());
        MetaField settlementMetaField = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.PAYMENT }, settlementMfName);
        StringMetaFieldValue settlementMetaFieldValue = new StringMetaFieldValue(settlementMetaField);
        settlementMetaFieldValue.setValue(settlementDate);
        logger.debug("{} created for payment {} ", settlementMetaFieldValue, paymentId);
        List<MetaFieldValue<?>> paymentMfs = Arrays.asList(txDateTimeMetaFieldValue, settlementMetaFieldValue);
        payment.getMetaFields().addAll(paymentMfs);
        paymentDAS.save(payment);
        logger.debug("added {} on payment {}", paymentMfs, paymentId);
    }
}
