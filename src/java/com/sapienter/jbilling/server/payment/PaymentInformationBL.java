package com.sapienter.jbilling.server.payment;

import java.io.Closeable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.BooleanMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTemplateDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.event.AbstractPaymentEvent;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.CreditCardSQL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;

public class PaymentInformationBL extends ResultList implements AutoCloseable, CreditCardSQL {

    private static final Logger logger = LoggerFactory.getLogger(PaymentInformationBL.class);

    private PaymentInformationDTO paymentInstrument;
    private PaymentInformationDAS piDas;

    public static final String TOKEN = "TOKEN";
    public static final String PAYER_ID = "PAYER_ID";
    public static final String BT_ID = "BT_ID";
    public static final String BT_ID2 = "BT ID";
    private static final String TYPE = "Type";

    public PaymentInformationBL() {
        init();
    }

    public PaymentInformationBL(Integer id) {
        init();
        set(id);
    }

    public void init() {
        piDas = new PaymentInformationDAS();
    }

    public void set(Integer id) {
        paymentInstrument = piDas.find(id);
    }

    public PaymentInformationDTO get() {
        return paymentInstrument;
    }

    public PaymentInformationDTO create(PaymentInformationDTO dto) {
        return piDas.create(dto, dto.getPaymentMethodType().getEntity().getId());
    }

    public void delete(Integer id) {
        if (id != null) {
            piDas.delete(piDas.findNow(id));
        }
    }

    public static final PaymentInformationWS getWS(PaymentInformationDTO dto) {
        PaymentInformationWS ws = new PaymentInformationWS();
        if(null != dto.getUser()) {
            ws.setUserId(dto.getUser().getUserId());
        }
        ws.setId(dto.getId());
        ws.setProcessingOrder(dto.getProcessingOrder());
        ws.setPaymentMethodTypeId(dto.getPaymentMethodType().getId());
        ws.setPaymentMethodId(dto.getPaymentMethodId());

        List<MetaFieldValueWS> metaFields = new ArrayList<>(0);
        for (MetaFieldValue value : dto.getMetaFields()) {
            metaFields.add(MetaFieldBL.getWS(value));
        }

        ws.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
        return ws;
    }

    /**
     * Gets payment method of credit card if one exist for company, if do not then create it.
     * Use that payment method to create a credit card and sets in template meta field values
     *
     * @param cardNumber Payment card number
     * @param company    Company of credit card
     * @return PaymentInformationDTO
     */
    public PaymentInformationDTO getCreditCardObject(char[] cardNumber, CompanyDTO company) {
        PaymentMethodTypeDTO pmtDto = getPaymentMethodTypeByTemplate(Constants.PAYMENT_CARD, company);

        PaymentInformationDTO piDto = new PaymentInformationDTO();
        piDto.setPaymentMethodType(pmtDto);

        // set specific values
        DateTimeFormatter dateFormat = DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT);
        updateCharMetaField(piDto, cardNumber, MetaFieldType.PAYMENT_CARD_NUMBER);
        updateCharMetaField(piDto, dateFormat.print(new Date().getTime()).toCharArray(), MetaFieldType.DATE);

        return piDto;
    }

    public PaymentMethodTypeDTO getPaymentMethodTypeByTemplate(String template, CompanyDTO company) {
        PaymentMethodTypeDAS pmtDas = new PaymentMethodTypeDAS();
        PaymentMethodTypeDTO pmtDto = pmtDas.getPaymentMethodTypeByTemplate(template, company.getId());

        if (pmtDto == null) {
            pmtDto = new PaymentMethodTypeDTO();

            pmtDto.setEntity(company);
            pmtDto.setIsRecurring(false);
            pmtDto.setMethodName(template);

            PaymentMethodTemplateDTO paymentTemplate = new PaymentMethodTemplateDAS().findByName(template);
            pmtDto.setPaymentMethodTemplate(paymentTemplate);

            for (MetaField field : paymentTemplate.getPaymentTemplateMetaFields()) {
                MetaField mf = new MetaField();
                mf.setEntityId(company.getId());
                mf.setName(field.getName());
                mf.setDataType(field.getDataType());
                mf.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
                mf.setDisabled(field.isDisabled());
                mf.setMandatory(field.isMandatory());
                mf.setDefaultValue(field.getDefaultValue());
                mf.setFieldUsage(field.getFieldUsage());
                mf.setDisplayOrder(field.getDisplayOrder());
                mf.setPrimary(field.getPrimary());
                mf.setValidationRule(field.getValidationRule());
                mf.setFilename(field.getFilename());
                pmtDto.getMetaFields().add(mf);
            }

            pmtDto = pmtDas.save(pmtDto);
        }

        return pmtDto;
    }

    /**
     * Only used from the API, thus the usage of PaymentAuthorizationDTOEx
     *
     * @param entityId
     * @param userId
     * @param cc
     * @param amount
     * @param currencyId
     * @return
     * @throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException
     */
    public PaymentAuthorizationDTOEx validatePreAuthorization(Integer entityId, Integer userId, PaymentInformationDTO cc,
            BigDecimal amount, Integer currencyId, Integer executorUserId) throws PluggableTaskException {

        // create a new payment record
        PaymentDTOEx paymentDto = new PaymentDTOEx();
        paymentDto.setAmount(amount);
        paymentDto.setCurrency(new CurrencyDAS().find(currencyId));

        PaymentMethodDTO paymentMethod = new PaymentMethodDAS().find(getPaymentMethodForPaymentMethodType(cc));
        cc.setPaymentMethod(paymentMethod);
        paymentDto.getPaymentInstruments().clear();
        paymentDto.getPaymentInstruments().add(cc);
        paymentDto.setInstrument(cc);

        paymentDto.setUserId(userId);
        paymentDto.setIsPreauth(1);

        // filler fields, required
        paymentDto.setIsRefund(0);
        paymentDto.setPaymentMethod(paymentMethod);
        paymentDto.setAttempt(1);
        paymentDto.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_ENTERED)); // to be updated later
        paymentDto.setPaymentDate(Calendar.getInstance().getTime());
        paymentDto.setBalance(amount);

        PaymentBL payment = new PaymentBL();
        payment.create(paymentDto, executorUserId); // this updates the id

        // use the payment processor configured
        PluggableTaskManager taskManager = new PluggableTaskManager(entityId, Constants.PLUGGABLE_TASK_PAYMENT);
        PaymentTask task = (PaymentTask) taskManager.getNextClass();

        boolean processNext = true;
        while (task != null && processNext) {
            processNext = task.preAuth(paymentDto);
            // get the next task
            task = (PaymentTask) taskManager.getNextClass();

            // at the time, a pre-auth acts just like a normal payment for events
            AbstractPaymentEvent event = AbstractPaymentEvent.forPaymentResult(entityId, paymentDto);
            if (event != null) {
                EventManager.process(event);
            }
        }

        // update the result
        payment.getEntity().setPaymentResult(paymentDto.getPaymentResult());

        //create the return value
        PaymentAuthorizationDTOEx retValue = null;

        if (paymentDto.getAuthorization() != null) {
            retValue = new PaymentAuthorizationDTOEx(paymentDto.getAuthorization().getOldDTO());
            if (paymentDto.getPaymentResult().getId() != Constants.RESULT_OK) {
                // if it was not successfull, it should not have balance
                payment.getEntity().setBalance(BigDecimal.ZERO);
                retValue.setResult(false);
            } else {
                retValue.setResult(true);
            }
        }

        return retValue;
    }

    /**
     * Returns true if it makes sense to send this cc to the processor.
     * Otherwise false (like when the card is now expired).
     */
    public boolean validateCreditCard(Date expiryDate, char[] creditCardNumber) {
        boolean retValue = true;

        if (expiryDate.before(com.sapienter.jbilling.server.util.Util.getDateWithoutTimeUsingCalendar())) {
            retValue = false;
        } else {
            if (Util.getPaymentMethod(creditCardNumber) == null) {
                retValue = false;
            }
        }

        return retValue;
    }

    public void notifyExipration(Date today)
            throws SQLException, SessionInternalError {

        logger.debug("Sending credit card expiration notifications. Today " + today);
        prepareStatement(CreditCardSQL.expiring);
        cachedResults.setDate(1, new java.sql.Date(today.getTime()));

        execute();
        while (cachedResults.next()) {
            Integer userId = cachedResults.getInt(1);
            Integer paymentInstrumentId = cachedResults.getInt(2);
            Date ccExpiryDate = cachedResults.getDate(3);

            set(paymentInstrumentId);
            NotificationBL notif = new NotificationBL();
            UserBL user = new UserBL(userId);

            try {
                PaymentInformationDTO instrument = piDas.find(paymentInstrumentId);
                MessageDTO message = notif.getCreditCardMessage(user.getEntity().
                        getEntity().getId(), user.getEntity().getLanguageIdField(),
                        userId, ccExpiryDate, instrument);

                INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
                notificationSess.notify(userId, message);
                instrument.close();
            } catch (NotificationNotFoundException e) {
                logger.warn("credit card message not set to user " + userId +
                        " because the entity lacks notification text");
            } catch (Exception exception) {
                logger.debug("Exception: " + exception);
            }
        }
        conn.close();

    }

    /**
     * Gets a list of instruments that exist in database and returns a dto if its a credit card
     *
     * @param paymentInstruments list of instruments
     * @return payment instrument that is credit card
     */
    public PaymentInformationDTO findCreditCard(List<PaymentInformationDTO> paymentInstruments) {
        if (paymentInstruments.size() > 0) {
            for (PaymentInformationDTO dto : paymentInstruments) {
                if (piDas.isCreditCard(dto.getId())) {
                    return dto;
                }
            }
        }
        return null;
    }

    /**
     * Gets a list of instruments that exist in database and returns all credit cards
     *
     * @param paymentInstruments list of instruments
     * @return payment instrument that is credit card
     */
    public List<PaymentInformationDTO> findAllCreditCards(List<PaymentInformationDTO> paymentInstruments) {
        List<PaymentInformationDTO> instruments = new ArrayList<>();
        if (paymentInstruments.size() > 0) {
            for (PaymentInformationDTO dto : paymentInstruments) {
                if (piDas.isCreditCard(dto.getId())) {
                    instruments.add(dto);
                }
            }
        }
        if (instruments.isEmpty()) {
            return null;
        } else {
            return instruments;
        }
    }

    /**
     * Verifies if payment instrument is a credit card
     *
     * @param instrument PaymentInformationDTO
     * @return true if payment instrument is credit card
     */
    public boolean isCreditCard(PaymentInformationDTO instrument) {
        return ArrayUtils.isNotEmpty(getCharMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER));
    }

    /**
     * Verifies if payment has TOKEN & PAYER_ID values
     *
     * @param account
     * @return true if payment has TOKEN & PAYER_ID values
     */
    public boolean isExpressCheckoutPayment(PaymentDTOEx account) {
        if (null == account) {
            return false;
        }

        for (PaymentInformationDTO instrument : account.getPaymentInstruments()) {
            for (MetaFieldValue metaFieldValue : instrument.getMetaFields()) {
                if (metaFieldValue.getField().getName().equals(TOKEN)
                        || metaFieldValue.getField().getName().equals(PAYER_ID)) {
                    return true;
                }

            }
        }
        return false;

    }

    /**
     * Verifies if payment instrument is a ach
     *
     * @param instrument PaymentInformationDTO
     * @return true if payment instrument is an ach
     */
    public boolean isACH(PaymentInformationDTO instrument) {
        return ArrayUtils.isNotEmpty(getCharMetaFieldByType(instrument, MetaFieldType.BANK_ACCOUNT_NUMBER)) &&
                ArrayUtils.isNotEmpty(getCharMetaFieldByType(instrument, MetaFieldType.BANK_ROUTING_NUMBER));
    }

    /**
     * Verifies if payment instrument is a cheque
     *
     * @param instrument PaymentInformationDTO
     * @return true if payment instrument is an ach
     */
    public boolean isCheque(PaymentInformationDTO instrument) {
        return getStringMetaFieldByType(instrument, MetaFieldType.CHEQUE_NUMBER) != null;
    }

    public boolean useGatewayKey(PaymentInformationDTO instrument) {
        return Constants.PAYMENT_METHOD_GATEWAY_KEY.equals(instrument.getPaymentMethod().getId()) ||
                ArrayUtils.isNotEmpty(getCharMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY));
    }

    public boolean isPaypalIPN(PaymentInformationDTO instrument) {
        return getStringMetaFieldByType(instrument, MetaFieldType.TRANSACTION_ID) != null;
    }

    public boolean isPaypalECO(PaymentInformationDTO instrument) {
        return getStringMetaFieldByType(instrument, MetaFieldType.TRANSACTION_ID) != null;
    }

    public boolean isBpay(PaymentInformationDTO instrument) {
        return getStringMetaFieldByType(instrument, MetaFieldType.BPAY_REF) != null;
    }

    public boolean updateStringMetaField(PaymentInformationDTO instrument, String metaFieldValue, MetaFieldType metaFieldName) {
        MetaFieldValue value = getMetaField(instrument, metaFieldName);
        if (value != null) {
            value.setValue(metaFieldValue);
            return true;
        } else {
            int entityId = instrument.getPaymentMethodType().getEntity().getId();
            for (MetaField field : instrument.getPaymentMethodType().getPaymentMethodTemplate().getPaymentTemplateMetaFields()) {
                if (field.getEntityId() == entityId && field.getFieldUsage() == metaFieldName) {
                    value = field.createValue();
                    break;
                }
            }

            if (value != null) {
                instrument.getMetaFields().add(value);
                return true;
            }
        }
        return false;
    }

    public boolean updateCharMetaField(PaymentInformationDTO instrument, char[] metaFieldValue, MetaFieldType metaFieldName) {
        MetaFieldValue value = getMetaField(instrument, metaFieldName);
        if (value != null) {
            value.setValue(metaFieldValue);
            return true;
        } else {
            int entityId = instrument.getPaymentMethodType().getEntity().getId();
            for (MetaField field : instrument.getPaymentMethodType().getPaymentMethodTemplate().getPaymentTemplateMetaFields()) {
                if (field.getEntityId() == entityId && field.getFieldUsage() == metaFieldName) {
                    value = field.createValue();
                    break;
                }
            }

            if (value != null) {
                instrument.getMetaFields().add(value);
                return true;
            }
        }
        return false;
    }

    public Integer getPaymentMethod(char[] credtiCardNumber) {
        if (credtiCardNumber != null) {
            return Util.getPaymentMethod(credtiCardNumber);
        }
        return null;
    }

    /**
     * Gets a payment instrument and then obscures the card number if the instrument is a credit card
     *
     * @param instrument PaymentInforamtionDTO
     */
    public void obscureCreditCardNumber(PaymentInformationDTO instrument) {
        MetaFieldValue cardNumber = getMetaField(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        if (cardNumber != null && cardNumber.getValue() != null) {
            char[] creditCardNumber = (char[]) cardNumber.getValue();
            if (null != cardNumber.getField() && null != cardNumber.getField().getValidationRule()
                    && cardNumber.getField().getValidationRule().getRuleType().toString() == "PAYMENT_CARD_OBSCURED") {
                cardNumber.setValue(Util.getObscuredCardNumberNew(creditCardNumber).toCharArray());
            } else {
                cardNumber.setValue(Util.getObscuredCardNumber(creditCardNumber).toCharArray());
            }
        }
    }

    public String getObscureCreditCardNumber(PaymentInformationDTO instrument) {
        MetaFieldValue cardNumber = getMetaField(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        String number = null;
        if (cardNumber != null && cardNumber.getValue() != null) {
            char[] creditCardNumber = (char[]) cardNumber.getValue();
            number = Constants.OBSCURED_NUMBER_FORMAT + new String(creditCardNumber, creditCardNumber.length - 4, 4);
        }
        return number;
    }

    public void obscureCreditCardNumber(PaymentInformationWS instrument) {
        MetaFieldValueWS cardNumber = getMetaField(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        if (cardNumber != null && cardNumber.getValue() != null) {
            char[] creditCardNumber = (char[]) cardNumber.getValue();
            if (null != cardNumber.getMetaField() && null != cardNumber.getMetaField().getValidationRule()
                    && cardNumber.getMetaField().getValidationRule().getRuleType() == "PAYMENT_CARD_OBSCURED") {
                cardNumber.setValue(Util.getObscuredCardNumberNew(creditCardNumber).toCharArray());
            } else {
                cardNumber.setValue(Util.getObscuredCardNumber(creditCardNumber).toCharArray());
            }
        }
    }
    /**
     * Gets a payment instrument and then obscures the bank account number if the instrument is an ach
     *
     * @param instrument PaymentInforamtionDTO
     */
    public void obscureBankAccountNumber(PaymentInformationDTO instrument) {
        MetaFieldValue baNumber = getMetaField(instrument, MetaFieldType.BANK_ACCOUNT_NUMBER);
        if (baNumber != null && baNumber.getValue() != null) {
            char[] number = (char[]) baNumber.getValue();

            boolean contains = false;
            for (char c : number) {
                if (c == '*') {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                MetaFieldValueWS metaValueWS = new MetaFieldValueWS();
                metaValueWS.getMetaField().setDataType(DataType.STRING);
                metaValueWS.setStringValue(encryptString(new String(number)));
                metaValueWS.setFieldName(Constants.ACH_ACCOUNT_NUMBER_ENCRYPTED);
                MetaFieldValueWS[] values = {metaValueWS};
                if (null != instrument.getUser()) {
                    MetaFieldBL.fillMetaFieldsFromWS(instrument.getUser().getCompany().getId(), instrument, values);
                }
            }

            // obscure bank account number
            int len = number.length - 4;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < len; i++) {
                sb.append('*');
            }

            baNumber.setValue((sb.toString() + new String(number, number.length - 4, 4)).toCharArray());
        }
    }

    /**
     * This method gets a payment information object and then depending upon its type returns a payment method id
     *
     * @param instrument PaymentInformationDTO
     * @return Integer, payment method id
     */
    public Integer getPaymentMethodForPaymentMethodType(PaymentInformationDTO instrument) {
        if (instrument.getPaymentMethod() == null) {
            if (isCreditCard(instrument)) {
                return getPaymentMethod(getCharMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER));
            } else if (isACH(instrument)) {
                return Constants.PAYMENT_METHOD_ACH;
            } else if (isCheque(instrument)) {
                return Constants.PAYMENT_METHOD_CHEQUE;
            } else if (isPaypalIPN(instrument)) {
                return Constants.PAYMENT_METHOD_PAYPALIPN;
            } else if (isPaypalECO(instrument)) {
                return Constants.PAYMENT_METHOD_PAYPAL_ECO;
            } else if(isBpay(instrument)) {
                return Constants.PAYMENT_METHOD_BPAY;
            } else {
                return Constants.PAYMENT_METHOD_CUSTOM;
            }
        } else {
            return instrument.getPaymentMethod().getId();
        }
    }

    public void updatePaymentMethodInPaymentInformation(PaymentInformationDTO instrument) {
        PaymentInformationDTO infoDto = new PaymentInformationDAS().find(instrument.getId());
        char[] cardNumber = getCharMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        if(null != infoDto) {
            infoDto.setPaymentMethodId(getPaymentMethod(cardNumber));
            new PaymentInformationDAS().save(infoDto);
        }
    }

    public boolean isCreditCardObscurred(PaymentInformationDTO instrument) {
        char[] cardNumber = getCharMetaFieldByType(instrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        return ArrayUtils.isEmpty(cardNumber) ? false : new String(cardNumber).contains("*");
    }

    public String getStringMetaFieldByType(PaymentInformationDTO instrument, MetaFieldType type) {
        MetaFieldValue value = getMetaField(instrument, type);

        if (value != null) {
            return (String) value.getValue();
        }
        return null;
    }

    public StringMetaFieldValue getStringMetaFieldByType(PaymentDTOEx account) {
        return null != getMetaField(account) ? (StringMetaFieldValue) getMetaField(account) : null;
    }

    private MetaFieldValue getMetaField(PaymentDTOEx account) {
        if (null != account && null != account.getMetaFields()) {
            for (MetaFieldValue value : account.getMetaFields()) {
                if (null != value.getId()) {
                    return value;
                }
            }
        }
        return null;
    }

    public String getStringMetaFieldByType(PaymentInformationWS instrument, MetaFieldType type) {
        MetaFieldValueWS value = getMetaField(instrument, type);
        if (value != null) {
            return value.getStringValue();
        }
        return null;
    }

    public char[] getCharMetaFieldByType(PaymentInformationDTO instrument, MetaFieldType type) {
        MetaFieldValue value = getMetaField(instrument, type);

        if (value != null) {
            return (char[]) value.getValue();
        }

        return ArrayUtils.EMPTY_CHAR_ARRAY;
    }

    public char[] getCharMetaFieldByType(PaymentInformationWS instrument, MetaFieldType type) {
        MetaFieldValueWS value = getMetaField(instrument, type);
        if (value != null) {
            return value.getCharValue();
        }
        return null;
    }

    /**
     * This method checks if Meta field values are spaces only
     * @param value
     * @return boolean
     */
    public boolean hasCharMetaFieldOnlySpaces(char[] value) {
        if(null == value) {
            return true;
        }
        return !(new String(value).chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList())
                .stream()
                .anyMatch(c -> !Character.isWhitespace(c)));
    }

    private MetaFieldValueWS getMetaField(PaymentInformationWS instrument, MetaFieldType type) {
        PaymentInformationDTO paymentInformationDTO = new PaymentInformationDAS().find(instrument.getId());
        MetaFieldValue metaField = getMetaField(paymentInformationDTO, type);
        if (type != null && instrument.getMetaFields() != null) {
            for (MetaFieldValueWS value : instrument.getMetaFields()) {
                if (value.getId() == metaField.getId()) {
                    return value;
                }
            }
        }
        return null;
    }

    public Date getDateMetaFieldByType(PaymentInformationDTO instrument, MetaFieldType type) {
        MetaFieldValue value = getMetaField(instrument, type);
        try {
            if (value != null && !value.isEmpty()) {
                String ccExpiryDate = new String((char[]) value.getValue()).trim();
                if (!ccExpiryDate.isEmpty()) {
                    return DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).parseDateTime(ccExpiryDate).toDate();
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred while parsing credit card date object: %s", e);
        }
        return null;
    }

    private Date getDateMetaFieldByType(PaymentInformationWS cc, MetaFieldType date) {
        char[] value = getCharMetaFieldByType(cc, date);
        try {
            if (value != null) {
                String ccExpiryDate = new String(value).trim();
                if (!ccExpiryDate.isEmpty()) {
                    return DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).parseDateTime(ccExpiryDate).toDate();
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred while parsing credit card date object: %s", e);
        }
        return null;
    }

    public Integer getIntegerMetaFieldByType(PaymentInformationDTO instrument, MetaFieldType type) {
        MetaFieldValue value = getMetaField(instrument, type);
        if (value != null) {
            return (Integer) value.getValue();
        }
        return null;
    }

    public MetaFieldValue getMetaField(PaymentInformationDTO instrument, MetaFieldType type) {
        if (type != null && instrument.getMetaFields() != null) {
            for (MetaFieldValue value : instrument.getMetaFields()) {
                MetaFieldType fieldType = value.getField().getFieldUsage();
                if (null != fieldType && fieldType == type) {
                    return value;
                }
            }
        }
        return null;
    }

    public Date getCardExpiryDate(PaymentInformationDTO cc) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(getDateMetaFieldByType(cc, MetaFieldType.DATE));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }


    public String get4digitExpiry(PaymentInformationDTO cc) {
        GregorianCalendar cal = new GregorianCalendar();
        Date ccExpiryDate = getDateMetaFieldByType(cc, MetaFieldType.DATE);
        if (ccExpiryDate == null) {
            return StringUtils.EMPTY;
        }
        cal.setTime(ccExpiryDate);
        String expiry = String.valueOf(
                cal.get(GregorianCalendar.MONTH) + 1) + String.valueOf(
                        cal.get(GregorianCalendar.YEAR)).substring(2);
        if (expiry.length() == 3) {
            expiry = "0" + expiry;
        }

        return expiry;
    }

    public String get4digitExpiry(PaymentInformationWS cc) {
        GregorianCalendar cal = new GregorianCalendar();
        Date ccExpiryDate = getDateMetaFieldByType(cc, MetaFieldType.DATE);
        if (ccExpiryDate == null) {
            return StringUtils.EMPTY;
        }
        cal.setTime(ccExpiryDate);
        String expiry = String.valueOf(
                cal.get(GregorianCalendar.MONTH) + 1) + String.valueOf(
                        cal.get(GregorianCalendar.YEAR)).substring(2);
        if (expiry.length() == 3) {
            expiry = "0" + expiry;
        }

        return expiry;
    }

    /**
     * Compares saved credit card with the changed one and verifies if
     * credit card values are changed.
     *
     * @param changed new payment instrument
     * @return true if credit card values has changed
     */
    public boolean isCCUpdated(PaymentInformationDTO changed) {

        char[] oldTitle = getCharMetaFieldByType(paymentInstrument, MetaFieldType.TITLE);
        char[] newTitle = getCharMetaFieldByType(changed, MetaFieldType.TITLE);

        char[] oldNumber = getCharMetaFieldByType(paymentInstrument, MetaFieldType.PAYMENT_CARD_NUMBER);
        char[] newNumber = getCharMetaFieldByType(changed, MetaFieldType.PAYMENT_CARD_NUMBER);

        Date oldDate = getDateMetaFieldByType(paymentInstrument, MetaFieldType.DATE);
        Date newDate = getDateMetaFieldByType(changed, MetaFieldType.DATE);

        logger.debug("Verifying if the credit card is updated." + oldTitle + "." + newTitle + "." + oldDate + "." + newDate);
        if(validateNull(oldTitle, newTitle) || validateNull(oldNumber, newNumber) || validateNull(oldDate, newDate)){
            return true;
        }
        if(isCardTitleChanged(oldTitle, newTitle)) {
            return true;
        }
        if(isCardNumberChanged(oldNumber, newNumber)) {
            return true;
        }
        if(isExpiryDateChanged(oldDate, newDate)){
            return true;
        }
        return false;
    }

    /**
     * @param oldDate
     * @param newDate
     * @return
     */
    private static boolean isExpiryDateChanged(Date oldDate, Date newDate) {
        if ((null == oldDate && null == newDate)
                || (null != oldDate && null != newDate && oldDate.equals(newDate))) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @param oldValue
     * @param newValue
     */
    private static boolean isCardTitleChanged(char[] oldValue, char[] newValue) {
        if ((null == oldValue && null == newValue)
                || (null != oldValue && null != newValue && String.valueOf(oldValue).equals(String.valueOf(newValue)))) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @param oldValue
     * @param newValue
     */
    private static boolean isCardNumberChanged(char[] oldValue, char[] newValue) {
        if ((null == oldValue && null == newValue)
                || (null != oldValue && null != newValue && String.valueOf(oldValue).equals(String.valueOf(newValue)))) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @param oldValue
     * @param newValue
     */
    private static boolean validateNull(Object oldValue, Object newValue) {
        if ((null == oldValue && null != newValue)
                || (null != oldValue && null == newValue)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * {@code try}-with-resources statement.
     * <p>
     * <p>While this interface method is declared to throw {@code
     * Exception}, implementers are <em>strongly</em> encouraged to
     * declare concrete implementations of the {@code close} method to
     * throw more specific exceptions, or to throw no exception at all
     * if the close operation cannot fail.
     * <p>
     * <p> Cases where the close operation may fail require careful
     * attention by implementers. It is strongly advised to relinquish
     * the underlying resources and to internally <em>mark</em> the
     * resource as closed, prior to throwing the exception. The {@code
     * close} method is unlikely to be invoked more than once and so
     * this ensures that the resources are released in a timely manner.
     * Furthermore it reduces problems that could arise when the resource
     * wraps, or is wrapped, by another resource.
     * <p>
     * <p><em>Implementers of this interface are also strongly advised
     * to not have the {@code close} method throw {@link
     * InterruptedException}.</em>
     * <p>
     * This exception interacts with a thread's interrupted status,
     * and runtime misbehavior is likely to occur if an {@code
     * InterruptedException} is {@linkplain Throwable#addSuppressed
     * suppressed}.
     * <p>
     * More generally, if it would cause problems for an
     * exception to be suppressed, the {@code AutoCloseable.close}
     * method should not throw it.
     * <p>
     * <p>Note that unlike the {@link Closeable#close close}
     * method of {@link Closeable}, this {@code close} method
     * is <em>not</em> required to be idempotent.  In other words,
     * calling this {@code close} method more than once may have some
     * visible side effect, unlike {@code Closeable.close} which is
     * required to have no effect if called more than once.
     * <p>
     * However, implementers of this interface are strongly encouraged
     * to make their {@code close} methods idempotent.
     *
     * @throws Exception if this resource cannot be closed
     */
    @Override
    public void close() throws Exception {
        // Close PaymentInformationDTO
        if (null != paymentInstrument) {
            paymentInstrument.close();
        }
    }

    public static boolean paymentCardObscured(char[] cardNumber) {
        boolean result = false;
        if (cardNumber != null) {
            for (int i = 0; i < Constants.OBSCURED_NUMBER_FORMAT.length() && i < cardNumber.length; i++) {
                if (cardNumber[i] == Constants.OBSCURED_NUMBER_FORMAT.charAt(i)) {
                    result = true;
                } else {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }


    private String encryptString(String source) {
        try {
            CipherBean cipherBean = Context.getBean(Context.Name.CIPHER_BEAN);
            return cipherBean.encryptString(source);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String decryptString(String encrypted) {
        try {
            CipherBean cipherBean = Context.getBean(Context.Name.CIPHER_BEAN);
            return cipherBean.decryptString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * This method converts PaymentInformationDTO object to PaymentInformationWS object
     *
     * @param dtos list of DTO objects
     * @return result list of converted WS objects
     */
    public static List<PaymentInformationWS> convertPaymentInformationDTOtoWS(List<PaymentInformationDTO> dtos) {
        List<PaymentInformationWS> result = new ArrayList<>();
        if (null != dtos && !dtos.isEmpty()) {
            for (PaymentInformationDTO paymentInformation : dtos) {
                result.add(PaymentInformationBL.getWS(paymentInformation));
            }
        }
        return result;
    }

    /**
     * Verifies if payment has BT_ID values
     *
     * @param payment
     * @return true if payment has BT_ID values
     */
    public boolean isBTPayment(PaymentInformationDTO instrument) {
        if(null == instrument){
            return false;
        }
        for (MetaFieldValue metaFieldValue : instrument.getMetaFields()) {
            if (StringUtils.equalsIgnoreCase(metaFieldValue.getField().getName(),BT_ID)
                    || StringUtils.equalsIgnoreCase(metaFieldValue.getField().getName(),BT_ID2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method gets a payment information object and then depending upon its type returns a payment method type
     *
     * @param instrument
     *            PaymentInformationDTO
     * @return String, payment method type
     */
    @SuppressWarnings("rawtypes")
    public String getPaymentMethodType(PaymentInformationDTO instrument) {
        String type = StringUtils.EMPTY;
        for (MetaFieldValue metaFieldValue : instrument.getMetaFields()) {
            if (StringUtils.equalsIgnoreCase(metaFieldValue.getField().getName(), TYPE)) {
                type = null != metaFieldValue.getValue() ? metaFieldValue.getValue().toString() : metaFieldValue
                        .getField().getDefaultValue().getValue().toString().trim();
            }
        }
        if (StringUtils.isBlank(type)) {
            for (MetaField metaField : instrument.getPaymentMethodType().getMetaFields()) {
                if (StringUtils.equalsIgnoreCase(metaField.getName(), TYPE)) {
                    type = metaField.getDefaultValue().getValue().toString().trim();
                }
            }
        }
        return type;
    }

    public static List<PaymentInformationDTO> filterForAutoAuthorization(List<PaymentInformationDTO> dtos) {
        if (CollectionUtils.isNotEmpty(dtos)) {
            return dtos.stream()
                    .filter(pi -> pi.getProcessingOrder() != null &&
                                  pi.getProcessingOrder() > 0 &&
                                  pi.getMetaFields().stream().anyMatch(mf -> MetaFieldType.AUTO_PAYMENT_AUTHORIZATION.equals(mf.getField().getFieldUsage()) &&
                                        ((BooleanMetaFieldValue) mf).getValue()))
                    .sorted(Comparator.comparing(PaymentInformationDTO::getProcessingOrder))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static boolean isPaymentAuthorizationPreferenceEnabled(Integer entityId) {
        try {
            Integer preferenceValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.REQUIRE_PAYMENT_AUTHORIZATION_FOR_COLLECTION_PROCESS);
            return (null != preferenceValue && 1 == preferenceValue);
        } catch (EmptyResultDataAccessException e) {
            logger.debug("Preference Id : {} not defined for company {}", CommonConstants.REQUIRE_PAYMENT_AUTHORIZATION_FOR_COLLECTION_PROCESS, entityId);
        } catch (Exception e) {
            logger.error("exception occured while identifying autoPaymentAuthorization check", e);
        }
        return false;
    }
}
