package com.sapienter.jbilling.server.payment.tasks;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.IExternalACHStorage;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.westpac.PaymentResponse;
import com.sapienter.jbilling.server.payment.tasks.westpac.TokenResponse;
import com.sapienter.jbilling.server.payment.tasks.westpac.WestPacService;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class PaymentWestPacTask extends PaymentTaskWithTimeout implements IExternalCreditCardStorage, IExternalACHStorage {

    enum PaymentMethodType {
        CREDIT_CARD, BANK_ACCOUNT, NONE
    }
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PAYMENT_PROCESSOR_NAME = "WestPac GateWay";
    private static final String AGL_PAYMENT_PROCESSOR_ACH = "BANKACCOUNT-AGL";
    private static final String AGL_PAYMENT_PROCESSOR_CC = "CREDITCARD-AGL";
    @SuppressWarnings("unused")
    private static final String PAYMENT_TYPE_PRE_AUTH = "preAuth";
    private static final String PAYMENT_TYPE_NORMAL = "payment";
    private static final String PAYMENT_TYPE_REFUND = "refund";
    private static final String KEY_PREFIX = "CUS-";

    /* Plugin parameters */
    private static final ParameterDescription PARAMETER_WEST_PAC_MERCHANT_ID =
            new ParameterDescription("Merchant Id", true, ParameterDescription.Type.STR, false);

    private static final ParameterDescription PARAMETER_REST_SECRET_KEY =
            new ParameterDescription("Secret Key", true, ParameterDescription.Type.STR, false);

    private static final ParameterDescription PARAMETER_REST_PUBLISHABLE_KEY =
            new ParameterDescription("Publishable API key", true, ParameterDescription.Type.STR, false);

    private static final ParameterDescription PARAMETER_REST_URL =
            new ParameterDescription("Url", true, ParameterDescription.Type.STR, false);

    private static final ParameterDescription PARAMETER_CONTACT_SECTION_NAME =
            new ParameterDescription("Customer Contact Section Name", false, ParameterDescription.Type.STR, false);

    private static final ParameterDescription PARAMETER_INVOICE_DESIGN_NAME =
            new ParameterDescription("Invoice Design Name", false, ParameterDescription.Type.STR, false);

    private static final ParameterDescription PARAMETER_BANK_ACCOUNT_ID =
            new ParameterDescription("Bank Account Id", true, ParameterDescription.Type.STR, false);

    public PaymentWestPacTask() {
        descriptions.add(PARAMETER_REST_PUBLISHABLE_KEY);
        descriptions.add(PARAMETER_REST_SECRET_KEY);
        descriptions.add(PARAMETER_WEST_PAC_MERCHANT_ID);
        descriptions.add(PARAMETER_REST_URL);
        descriptions.add(PARAMETER_CONTACT_SECTION_NAME);
        descriptions.add(PARAMETER_BANK_ACCOUNT_ID);
        descriptions.add(PARAMETER_INVOICE_DESIGN_NAME);
    }

    @Override
    public boolean process(PaymentDTOEx paymentInfo) throws PluggableTaskException {
        if(!isValidTask(paymentInfo.getUserId())) {
          //returning true when the task is not valid, since this is used for calling other processors
            return true;
        }
        if(isRefund(paymentInfo)) {
            return processRefund(paymentInfo).shouldCallOtherProcessors();
        } else if(isGateWayKeyStored(paymentInfo.getInstrument())) {
            return processPaymentForStoredGateWayKey(paymentInfo).shouldCallOtherProcessors();
        }
        return processOneTimePayment(paymentInfo).shouldCallOtherProcessors();
    }

    @Override
    public void failure(Integer userId, Integer retry) {
        logger.debug("User {} failed", userId);
    }

    @Override
    public boolean preAuth(PaymentDTOEx paymentInfo) throws PluggableTaskException {
        logger.debug("In side preAuth");
        return false;
    }

    @Override
    public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx paymentInfo) throws PluggableTaskException {
        return false;
    }

    @Override
    public String storeCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        logger.debug("creating credit card customer profile for user {}", instrument.getUser().getId());
        return isValidTask(instrument.getUser().getId()) ? generateGateWaykey(instrument) : null;
    }

    @Override
    public boolean isValidTask(Integer userId) {
        return isValidTask(UserBL.getUserEntity(userId));
    }

    private boolean isValidTask(UserDTO user) {
        String invoiceDesignName = Objects.toString(parameters.get(PARAMETER_INVOICE_DESIGN_NAME.getName()), "");
        String invoiceDesignFromCustomerObject = getInvoiceDesignFromUserObject(user);
        String invoiceDesign = Objects.toString(fetchInvoiceDesign(user.getId(), invoiceDesignFromCustomerObject), "");
        return Objects.equals(invoiceDesignName, invoiceDesign);
    }

    private String getInvoiceDesignFromUserObject(UserDTO user){
        return Objects.isNull(user) ? null :
            Objects.isNull(user.getCustomer()) ? null :
                Objects.toString(user.getCustomer().getInvoiceDesign(),"");
    }

    /**
     * This is helper method to fetch invoice design directly from DB
     * This method uses the SPCHelperService class to fetch invoice design using JDBCTemplate
     * @param userId User for which the invoice design is required
     * @return invoiceDesign field value
     */
    private String fetchInvoiceDesign(Integer userId, String invoiceDesign) {
        return Context.getBean(SpcHelperService.class).getCustomerInvoiceDesign(userId, invoiceDesign);
    }

    private static boolean isRefund(PaymentDTOEx payment) {
        return BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || payment.getIsRefund() != 0;
    }

    /**
     * Processes store credit card payment
     * @param payment
     * @return
     * @throws PluggableTaskException
     */
    private Result processPaymentForStoredGateWayKey(PaymentDTOEx payment) throws PluggableTaskException {
        try {
            Integer userId = payment.getUserId();
            PaymentInformationDTO instrument = payment.getInstrument();
            MultiValueMap<String, String> paymentParams = collectPaymentInfo(getGateWayKey(instrument),
                    payment, PAYMENT_TYPE_NORMAL, resolvepaymentMethodTypeFromInstrument(instrument));
            logger.debug("Payment params created {} for user {}", paymentParams, userId);
            String processorName = resolveProcessorNameFromInstrument(instrument, userId);
            WestPacService gateWayService = getPaymentService();
            PaymentResponse paymentResponse = gateWayService.createPayment(paymentParams);
            PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(paymentResponse, processorName);
            storeResultInDB(paymentResponse.isPassed(), payment, paymentAuthorization);
            return new Result(paymentAuthorization, false);
        } catch(PluggableTaskException ex) {
            logger.error("Couldn't handle recurring payment request due to error", ex);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            logger.error("Error in processPaymentForStoredCreditCard ", ex);
            throw new PluggableTaskException("Error processPaymentForStoredCreditCard", ex);
        }
    }

    private String resolveProcessorNameFromInstrument(PaymentInformationDTO instrument, Integer userId) {
        if(isAGL(userId, getInvoiceDesignFromUserObject(UserBL.getUserEntity(userId)))) {
            if(isACH(instrument)) {
                return AGL_PAYMENT_PROCESSOR_ACH;
            } else if(isCreditCard(instrument)) {
                return AGL_PAYMENT_PROCESSOR_CC;
            }
        }
        return PAYMENT_PROCESSOR_NAME;
    }

    private PaymentMethodType resolvepaymentMethodTypeFromInstrument(PaymentInformationDTO instrument) {
        if(isACH(instrument)) {
            return PaymentMethodType.BANK_ACCOUNT;
        } else if(isCreditCard(instrument)) {
            return PaymentMethodType.CREDIT_CARD;
        }
        return PaymentMethodType.NONE;
    }
    /**
     * Processes one time payment
     * @param payment
     * @return
     * @throws PluggableTaskException
     */
    private Result processOneTimePayment(PaymentDTOEx payment) throws PluggableTaskException {
        try {
            PaymentInformationDTO instrument = payment.getInstrument();
            if(isACH(instrument)) {
                logger.error("only credit card one time payment allowed");
                throw new PluggableTaskException("only credit card one time payment is allowed by WestPac gateway");
            }
            UserDTO user = new UserDAS().find(payment.getUserId());
            WestPacService gateWayService = getPaymentService();
            MultiValueMap<String, String> cardParams = collectCardInfo(instrument);
            logger.debug("Card info {} from user {}", cardParams, user.getId());
            TokenResponse token = gateWayService.generateOneTimeToken(cardParams);
            logger.debug("One time payment token id {} for user {}", token.getSingleUseTokenId(), user.getId());
            String gateWayKey = generateCustomerNumber();
            MultiValueMap<String, String> paymentParams = collectPaymentInfo(gateWayKey, payment, PAYMENT_TYPE_NORMAL, PaymentMethodType.CREDIT_CARD);
            paymentParams.add("singleUseTokenId", token.getSingleUseTokenId());
            logger.debug("Payment params created {} for user {}", paymentParams, user.getId());
            String processorName = resolveProcessorNameFromInstrument(instrument, user.getId());
            PaymentResponse paymentResponse = gateWayService.createPayment(paymentParams);
            PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(paymentResponse, processorName);
            storeResultInDB(paymentResponse.isPassed(), payment, paymentAuthorization);
            return new Result(paymentAuthorization, false);
        } catch(PluggableTaskException ex) {
            logger.error("Couldn't handle one time payment request due to error", ex);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        } catch(Exception ex) {
            logger.error("Error in processOneTimePayment ", ex);
            throw new PluggableTaskException("Error processOneTimePayment", ex);
        }
    }

    /**
     * stores Result in  DataBase
     * @param result
     * @param payment
     * @param paymentAuthorization
     */
    private void storeResultInDB(boolean isPassed, PaymentDTOEx payment, PaymentAuthorizationDTO paymentAuthorization) {
        Integer resultId = isPassed ? Constants.RESULT_OK : Constants.RESULT_FAIL;
        payment.setPaymentResult(new PaymentResultDAS().find(resultId));
        new PaymentAuthorizationBL().create(paymentAuthorization, payment.getId());
        payment.setAuthorization(paymentAuthorization);
    }

    private static PaymentAuthorizationDTO getParentPaymentAuthorization(PaymentDTOEx payment) {
        return isRefund(payment) ? new PaymentDAS().findNow(payment.getPayment().getId())
                .getPaymentAuthorizations()
                .iterator()
                .next() : null;
    }

    private MultiValueMap<String, String> collectPaymentInfo(String gateWaykey, PaymentDTOEx payment,
            String paymentType, PaymentMethodType methodType) throws PluggableTaskException {
        MultiValueMap<String, String> paymentParams = new LinkedMultiValueMap<>();
        BigDecimal amount = payment.getAmount().setScale(CommonConstants.BIGDECIMAL_SCALE_STR, CommonConstants.BIGDECIMAL_ROUND);
        paymentParams.add("transactionType", paymentType);
        paymentParams.add("orderNumber", UUID.randomUUID().toString().substring(0, 20));
        paymentParams.add("principalAmount", amount.toString());
        if(!paymentType.equals(PAYMENT_TYPE_REFUND)) {
            paymentParams.add("customerNumber", gateWaykey);
            paymentParams.add("currency", "aud");
            if(PaymentMethodType.CREDIT_CARD.equals(methodType)) {
                paymentParams.add("merchantId", parameters.get(PARAMETER_WEST_PAC_MERCHANT_ID.getName()));
            } else if(PaymentMethodType.BANK_ACCOUNT.equals(methodType)) {
                paymentParams.add("bankAccountId", parameters.get(PARAMETER_BANK_ACCOUNT_ID.getName()));
            }
        } else {
            PaymentAuthorizationDTO parentPaymentAuthorization = getParentPaymentAuthorization(payment);
            if(null == parentPaymentAuthorization) {
                throw new PluggableTaskException("Parent Transaction Id not found");
            }
            paymentParams.add("parentTransactionId", parentPaymentAuthorization.getTransactionId());
        }
        return paymentParams;
    }

    private Result processRefund(PaymentDTOEx payment) throws PluggableTaskException {
        try {
            Integer userId = payment.getUserId();
            MultiValueMap<String, String> paymentParams = collectPaymentInfo(null, payment, PAYMENT_TYPE_REFUND, PaymentMethodType.NONE);
            logger.debug("Payment params created {} for user {}", paymentParams, userId);
            String processorName = resolveProcessorNameFromInstrument(payment.getInstrument(), userId);
            WestPacService gateWayService = getPaymentService();
            PaymentResponse paymentResponse = gateWayService.createPayment(paymentParams);
            PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(paymentResponse, processorName);
            storeResultInDB(paymentResponse.isPassed(), payment, paymentAuthorization);
            return new Result(paymentAuthorization, false);
        } catch(PluggableTaskException ex) {
            logger.error("Couldn't handle refund payment request due to error", ex);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
            return NOT_APPLICABLE;
        } catch(Exception ex) {
            logger.error("Error in processRefund ", ex);
            throw new PluggableTaskException("Error processRefund", ex);
        }
    }

    @SuppressWarnings("unused")
    private PaymentDTOEx createPreAuthPayment(UserDTO user, PaymentInformationDTO creditCard) {
        try {
            PaymentDTO payment = new PaymentDTO();
            payment.setBaseUser(user);
            payment.setCurrency(user.getCurrency());
            payment.setAmount(CommonConstants.BIGDECIMAL_ONE_CENT);
            try (PaymentInformationBL piBl = new PaymentInformationBL()) {
                payment.setPaymentMethod(new PaymentMethodDAS().find(Util.getPaymentMethod(piBl.getCharMetaFieldByType(creditCard, MetaFieldType.PAYMENT_CARD_NUMBER))));
            }
            creditCard.setPaymentMethod(payment.getPaymentMethod());
            payment.setIsRefund(0);
            payment.setIsPreauth(1);
            payment.setDeleted(1);
            payment.setAttempt(1);
            payment.setPaymentDate(new Date());
            payment.setCreateDatetime(new Date());
            PaymentDTOEx paymentEx = new PaymentDTOEx(new PaymentDAS().save(payment));
            paymentEx.setInstrument(creditCard);
            return paymentEx;
        } catch(Exception ex) {
            logger.error("PreAuthPayment Creation Failed", ex);
            throw new SessionInternalError("PreAuth Payment creation Failed!", ex);
        }
    }

    private MultiValueMap<String, String> collectCardInfo(PaymentInformationDTO creditCard) {
        Map<String, String> cardValueMap = getPaymentInstrumentMetaFields(creditCard);
        String expiryDateFieldValue = cardValueMap.get(MetaFieldType.DATE.name());
        String creditCardNumber = cardValueMap.get(MetaFieldType.PAYMENT_CARD_NUMBER.name());
        String cardHolderName = cardValueMap.get(MetaFieldType.INITIAL.name());
        String expiryYear = null;
        String expiryMonth = null;
        if(StringUtils.isNotEmpty(expiryDateFieldValue) && expiryDateFieldValue.split("/").length == 2) {
            expiryMonth = expiryDateFieldValue.split("/")[0];
            expiryYear = expiryDateFieldValue.split("/")[1];
        }

        if(StringUtils.isEmpty(cardHolderName)) {
            cardHolderName = cardValueMap.get(MetaFieldType.TITLE.name());
        }
        MultiValueMap<String, String> cardParam = new LinkedMultiValueMap<>();
        cardParam.add("paymentMethod", "creditCard");
        cardParam.add("cardNumber", creditCardNumber);
        cardParam.add("cardholderName", cardHolderName);
        if(null!= creditCard.getCvv()) {
            cardParam.add("cvn", creditCard.getCvv().toString());
        }
        cardParam.add("expiryDateMonth", expiryMonth);
        cardParam.add("expiryDateYear", expiryYear);
        return cardParam;
    }

    private MultiValueMap<String, String> collectBankAccountInfo(PaymentInformationDTO accountDetails) {
        Map<String, String> accountInfoMap = getPaymentInstrumentMetaFields(accountDetails);
        MultiValueMap<String, String> bankAccountParam = new LinkedMultiValueMap<>();
        bankAccountParam.add("bsb", accountInfoMap.get(MetaFieldType.BANK_ROUTING_NUMBER.name()));
        bankAccountParam.add("accountNumber", accountInfoMap.get(MetaFieldType.BANK_ACCOUNT_NUMBER.name()));
        bankAccountParam.add("accountName", accountInfoMap.get(MetaFieldType.INITIAL.name()));
        bankAccountParam.add("paymentMethod", "bankAccount");
        return bankAccountParam;
    }

    /**
     * Generate 20 char unique number for customer
     * @return
     */
    private String generateCustomerNumber() {
        Random random = new Random(System.nanoTime() % 10);
        return  KEY_PREFIX + RandomStringUtils.random(5, true, true) + random.nextInt();
    }

    private MultiValueMap<String, String> collectCustomerInfo(String tokenId, UserDTO user, PaymentMethodType type) {
        MultiValueMap<String, String> customerParam = new LinkedMultiValueMap<>();
        String sectionName = parameters.get(PARAMETER_CONTACT_SECTION_NAME.getName());
        if(StringUtils.isNotEmpty(sectionName)) {
            Integer accountTypeId = user.getCustomer().getAccountType().getId();
            AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
            Map<Integer, String> accountAITSectionIdAndNameMap = accountInformationTypeDAS.getInformationTypeIdAndNameMapForAccountType(accountTypeId);
            Optional<Integer> sectionId = Optional.empty();
            for(Entry<Integer, String> aitSectionIdNameEntry : accountAITSectionIdAndNameMap.entrySet()) {
                if(aitSectionIdNameEntry.getValue().equals(sectionName.trim())) {
                    sectionId = Optional.of(aitSectionIdNameEntry.getKey());
                }
            }
            CustomerDTO customer = user.getCustomer();
            Integer entityId = user.getEntity().getId();
            if(sectionId.isPresent()) {
                Map<String, String> aitFieldNameAndValueMap = getCustomerAITMetaFields(customer, sectionId.get(),
                        TimezoneHelper.companyCurrentDate(entityId));
                String firstName = aitFieldNameAndValueMap.get(MetaFieldType.FIRST_NAME.name());
                String lastName = aitFieldNameAndValueMap.get(MetaFieldType.LAST_NAME.name());
                String emailAddress = aitFieldNameAndValueMap.get(MetaFieldType.EMAIL.name());
                String billingEmailAddress = aitFieldNameAndValueMap.get(MetaFieldType.BILLING_EMAIL.name());
                String street1 = aitFieldNameAndValueMap.get(MetaFieldType.ADDRESS1.name());
                String street2 = aitFieldNameAndValueMap.get(MetaFieldType.ADDRESS2.name());
                String cityName = aitFieldNameAndValueMap.get(MetaFieldType.CITY.name());
                String phoneNumber = aitFieldNameAndValueMap.get(MetaFieldType.PHONE_NUMBER.name());
                String postalCode = aitFieldNameAndValueMap.get(MetaFieldType.POSTAL_CODE.name());

                //TODO use enumration type for state meta field
                String state = null;

                if(StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName)) {
                    customerParam.add("customerName", firstName + " " + lastName);
                }
                if(StringUtils.isNotEmpty(emailAddress)) {
                    customerParam.add("emailAddress", emailAddress);
                } else if(StringUtils.isNotEmpty(billingEmailAddress)) {
                    customerParam.add("emailAddress", billingEmailAddress);
                }
                if(StringUtils.isNotEmpty(street1)) {
                    customerParam.add("street1", street1);
                }
                if(StringUtils.isNotEmpty(street2)) {
                    customerParam.add("street2", street2);
                }
                if(StringUtils.isNotEmpty(cityName)) {
                    customerParam.add("cityName", cityName);
                }
                if(StringUtils.isNotEmpty(state)) {
                    customerParam.add("state", state);
                }
                if(StringUtils.isNotEmpty(phoneNumber)) {
                    customerParam.add("phoneNumber", phoneNumber);
                }
                if(StringUtils.isNotEmpty(postalCode)) {
                    customerParam.add("postalCode", postalCode);
                }
            }
        }
        if(PaymentMethodType.CREDIT_CARD.equals(type)) {
            customerParam.add("merchantId", parameters.get(PARAMETER_WEST_PAC_MERCHANT_ID.getName()));
        } else if(PaymentMethodType.BANK_ACCOUNT.equals(type)) {
            customerParam.add("bankAccountId", parameters.get(PARAMETER_BANK_ACCOUNT_ID.getName()));
        }
        customerParam.add("singleUseTokenId", tokenId);
        return customerParam;
    }

    private Map<String, String> getPaymentInstrumentMetaFields(PaymentInformationDTO creditCard) {
        Map<String, String> creditCardFieldMap = new HashMap<>();
        creditCard.getMetaFields().forEach(metaFieldValue -> {
            MetaFieldType type = metaFieldValue.getField().getFieldUsage();
            Object value = metaFieldValue.getValue();
            if(null!=type && null!=value) {
                if(metaFieldValue.getField().getDataType().equals(DataType.CHAR)) {
                    creditCardFieldMap.put(type.name(), new String((char[])value));
                } else {
                    creditCardFieldMap.put(type.name(), value.toString());
                }
            }
        });
        return creditCardFieldMap;
    }

    private boolean isGateWayKeyStored(PaymentInformationDTO instrument) {
        return StringUtils.isNotEmpty(getGateWayKey(instrument));
    }

    private String getGateWayKey(PaymentInformationDTO instrument) {
        try(PaymentInformationBL piBL = new PaymentInformationBL()) {
            char[] gatewayKey  = piBL.getCharMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY);
            return ArrayUtils.isNotEmpty(gatewayKey) ? new String(gatewayKey) : "";
        } catch (Exception ex) {
            logger.error("error in isCardStored", ex);
            throw new SessionInternalError(ex);
        }
    }

    private boolean isCreditCard(PaymentInformationDTO instrument) {
        try(PaymentInformationBL piBL = new PaymentInformationBL()) {
            return piBL.isCreditCard(instrument);
        } catch (Exception ex) {
            logger.error("error in isCreditCard", ex);
            throw new SessionInternalError(ex);
        }
    }

    private boolean isACH(PaymentInformationDTO instrument) {
        try(PaymentInformationBL piBL = new PaymentInformationBL()) {
            return piBL.isACH(instrument);
        } catch (Exception ex) {
            logger.error("error in isACH", ex);
            throw new SessionInternalError(ex);
        }
    }

    private boolean isAGL(Integer userId, String invoiceDesign) {
        return Context.getBean(SpcHelperService.class).isAGL(userId, invoiceDesign);
    }

    @Override
    public char[] deleteCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
        return new char[0];
    }

    private WestPacService getPaymentService() {
        return new WestPacService(parameters.get(PARAMETER_REST_URL.getName()), parameters.get(PARAMETER_REST_PUBLISHABLE_KEY.getName()),
                parameters.get(PARAMETER_REST_SECRET_KEY.getName()), 1000 * getTimeoutSeconds());
    }

    @SuppressWarnings("rawtypes")
    private Map<String,String> getCustomerAITMetaFields(CustomerDTO customer, Integer groupId, Date effectiveDate) {
        Map<Date, List<MetaFieldValue>> aitMetaFields = customer.getAitTimelineMetaFieldsMap().getOrDefault(groupId, Collections.emptyMap());
        Map<String, String> aitFieldNameValueMap = new HashMap<>();
        for(Entry<Date, List<MetaFieldValue>> aitEntry : aitMetaFields.entrySet()) {
            if(aitEntry.getKey().compareTo(effectiveDate)<=0) {
                for(MetaFieldValue aitFieldValue: aitEntry.getValue()) {
                    Object value = aitFieldValue.getValue();
                    MetaFieldType usage = aitFieldValue.getField().getFieldUsage();
                    if(null!= usage) {
                        aitFieldNameValueMap.put(usage.name(), value!=null ? value.toString() : "");
                    }
                }
                return aitFieldNameValueMap;
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Creates {@link PaymentAuthorizationDTO} from {@link PaymentResponse}
     * @param paymentResponse
     * @return
     */
    private PaymentAuthorizationDTO buildPaymentAuthorization(PaymentResponse paymentResponse, String processorName) {
        PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
        paymentAuthDTO.setProcessor(processorName);

        String txID = StringUtils.isNotEmpty(paymentResponse.getTransactionId()) ? paymentResponse.getTransactionId() : "N/A"; // Transaction Id
        paymentAuthDTO.setTransactionId(txID);
        logger.debug("transactionId/code1 [{}]", txID);
        paymentAuthDTO.setCode1(txID);
        paymentAuthDTO.setResponseMessage(paymentResponse.getResponseText());
        String orderNumber = paymentResponse.getOrderNumber(); // Unique Order Number
        if(orderNumber!=null) {
            paymentAuthDTO.setCode2(orderNumber);
        }
        if(paymentResponse.getResponseCode()!=null) {
            paymentAuthDTO.setApprovalCode(paymentResponse.getResponseCode());
        }
        paymentAuthDTO.setCode3(paymentResponse.getStatus());
        return paymentAuthDTO;
    }

    private String generateGateWaykey(PaymentInformationDTO instrument) {
        if(null == instrument) {
            logger.debug("No Instrument Found ");
            return null;
        }
        if(isGateWayKeyStored(instrument)) {
            logger.debug("Customer profile alreday created on gateway");
            return null;
        }
        try {
            logger.debug("Payment processing for {} gateway", PAYMENT_PROCESSOR_NAME);
            UserDTO user = instrument.getUser();
            MultiValueMap<String, String> instrumentParam = null;
            PaymentMethodType type = null;
            if(isCreditCard(instrument)) {
                instrumentParam = collectCardInfo(instrument);
                type = PaymentMethodType.CREDIT_CARD;
                logger.debug("Card info {} from user {}", instrumentParam, user.getId());
            } else if(isACH(instrument)) {
                instrumentParam = collectBankAccountInfo(instrument);
                type = PaymentMethodType.BANK_ACCOUNT;
                logger.debug("Bank Account info {} from user {}", instrumentParam, user.getId());
            } else {
                logger.error("unknow payment instrument found for user {}", user.getId());
                throw new SessionInternalError("unknow payment instrument provided, plugin "
                        + "only supports (Credit Card and Bank Account(ACH)) Payment instruments");
            }
            WestPacService gateWayService = getPaymentService();
            // generating token id for user
            TokenResponse token = gateWayService.generateOneTimeToken(instrumentParam);
            logger.debug("Token {} for user {}", token.getSingleUseTokenId(), user.getId());

            MultiValueMap<String, String> customerParams = collectCustomerInfo(token.getSingleUseTokenId(), user, type);
            logger.debug("Collected Customer info {} from user {}", customerParams, user.getId());

            // Creating customer on westpac gateway.
            String gateWayKey = generateCustomerNumber();
            gateWayService.createCustomer(gateWayKey, customerParams);

            // Creating pre-auth jbilling payment
            /* PaymentDTOEx preAuthPayment = createPreAuthPayment(user, instrument);
            logger.debug("Pre auth payment {} for user {}", preAuthPayment, user.getId());

            MultiValueMap<String, String> paymentParams = collectPaymentInfo(gateWayKey, preAuthPayment, PAYMENT_TYPE_PRE_AUTH);
            logger.debug("Collected payment info {} for user {}", paymentParams, user.getId());
            PaymentResponse paymentResponse = gateWayService.createPayment(paymentParams);
            Integer resultId = paymentResponse.isPassed() ? Constants.PAYMENT_RESULT_SUCCESSFUL : Constants.PAYMENT_RESULT_FAILED;
            updatePaymentResult(preAuthPayment, resultId);*/
            return gateWayKey;
        } catch(SessionInternalError error) {
            throw error;
        } catch(Exception ex) {
            throw new SessionInternalError("WestPac customer profile creation Failed!", ex);
        }
    }

    @Override
    public String storeACH(ContactDTO contact, PaymentInformationDTO instrument, boolean updateKey) {
        logger.debug("creating bank account customer profile for user {}", instrument.getUser().getId());
        return isValidTask(instrument.getUser().getId()) ? generateGateWaykey(instrument) : null;
    }

    @Override
    public String deleteACH(ContactDTO contact, PaymentInformationDTO instrument) {
        return null;
    }
}
