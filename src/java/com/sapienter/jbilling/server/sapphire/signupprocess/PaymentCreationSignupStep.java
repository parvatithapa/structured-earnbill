package com.sapienter.jbilling.server.sapphire.signupprocess;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.CharMetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
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
import com.sapienter.jbilling.server.payment.db.PaymentInstrumentInfoDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.event.AbstractPaymentEvent;
import com.sapienter.jbilling.server.process.signup.PaymentRequestWS;
import com.sapienter.jbilling.server.process.signup.PaymentResult;
import com.sapienter.jbilling.server.process.signup.SignupPlaceHolder;
import com.sapienter.jbilling.server.process.signup.SignupResponseWS;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * Create Payment for all one time charges.
 * @author Krunal Bhavsar
 *
 */
public class PaymentCreationSignupStep extends AbstractSapphireSignupStep {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public PaymentCreationSignupStep(IWebServicesSessionBean service, IMethodTransactionalWrapper txAction, boolean useNewTx, boolean isAsync) {
        super(service, txAction, useNewTx, isAsync);
    }

    @Override
    public void doExecute(SignupPlaceHolder holder) {
        createPaymentFromPaymentRequest(holder); // Creating payment
    }

    @SuppressWarnings("unused")
    private void makePayment(SignupPlaceHolder holder) {
        SignupResponseWS response = holder.getSignUpResponse();
        try {
            Integer userId = response.getUserId();
            logger.debug("Executing Payment step for user {} for entity {}", userId, holder.getEntityId());
            IWebServicesSessionBean service = getService();
            UserDTO user = new UserDAS().find(userId);
            List<PaymentInformationWS> userPaymentInstruments = convertPaymentInformationDTOtoWS(user.getPaymentInstruments());
            if(CollectionUtils.isEmpty(userPaymentInstruments)) {
                logger.debug("Payment Instrument not found on user!");
                response.setPaymentResponse("NO-INSTRUMENT-FOUND-ON-CUSTOMER");
                return ;
            }
            BigDecimal amount = calculateAmountForOneTimeCharges(userId);
            if(null == amount || amount.compareTo(BigDecimal.ZERO) <= 0) {
                logger.debug("Payment Amount is Zero for user {}", userId);
                response.setPaymentResponse("NO-ONE-TIME-CHARGES-FOUND");
                return ;
            }
            logger.debug("Payment Amount is {} for user {}", amount, userId);
            PaymentWS paymentWS = new PaymentWS();
            paymentWS.setUserId(userId);
            paymentWS.setPaymentInstruments(userPaymentInstruments);
            paymentWS.setCreateDatetime(Calendar.getInstance().getTime());
            paymentWS.setCurrencyId(user.getCurrencyId());
            paymentWS.setPaymentInstruments(userPaymentInstruments);
            paymentWS.setUserId(userId);
            paymentWS.setPaymentDate(Calendar.getInstance().getTime());
            paymentWS.setIsRefund(0);
            paymentWS.setAmount(amount);
            PaymentDTOEx dto = new PaymentDTOEx(paymentWS);
            PaymentBL paymentBL = new PaymentBL();
            Integer paymentResult = paymentBL.processPayment(holder.getEntityId(), dto, service.getCallerId());
            PaymentDTO savedPayment = paymentBL.getDTO();
            response.setPaymentId(savedPayment.getId());
            if(paymentResult.equals(Constants.RESULT_OK)) {
                response.setPaymentResponse("Payment Successful");
                response.setPaymentResult("Payment Done");
            } else {
                response.setPaymentResult("Payment Failed");
                response.setPaymentResponse(response.getPaymentResult());
                Set<PaymentAuthorizationDTO> auths = savedPayment.getPaymentAuthorizations();
                if(CollectionUtils.isNotEmpty(auths)) {
                    response.setPaymentResponse(auths.iterator().next().getResponseMessage());
                }
            }
        } catch (Exception ex) {
            logger.error("Error during make Payment", ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.setPaymentId(null);
            response.setPaymentResult("SAPP-ERROR-PAYMENT-FAILED");
            response.setPaymentResponse("SAPP-ERROR-PAYMENT-FAILED");
            logger.debug("Rollabck tx");
        }
    }

    /**
     * Calculates All one time active order total amount for payment.
     * @param userId
     * @return
     */
    private BigDecimal calculateAmountForOneTimeCharges(Integer userId) {
        OrderDAS orderDAS = new OrderDAS();
        List<Integer> oneTimeOrders = orderDAS.getUserOneTimeActiveOrderIdsForUser(userId);
        if(oneTimeOrders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return oneTimeOrders.stream()
                .map(orderDAS::find)
                .map(OrderDTO::getTotal)
                .reduce((a1, a2) -> a1.add(a2))
                .get();
    }

    /**
     * Convert {@link PaymentInformationDTO} to {@link PaymentInformationWS}
     * @param dtos
     * @return
     */
    private List<PaymentInformationWS> convertPaymentInformationDTOtoWS(List<PaymentInformationDTO> dtos) {
        List<PaymentInformationWS> result = new ArrayList<>();
        if (null != dtos && !dtos.isEmpty()) {
            result = dtos.stream()
                    .sorted(Comparator.comparing(PaymentInformationDTO::getProcessingOrder))
                    .map(PaymentInformationBL::getWS)
                    .collect(Collectors.toList());
        }
        return result;
    }

    /**
     * returns first payment instrument attached on user on basis of instrument's processig order.
     * @param user
     * @return {@link PaymentInformationDTO}
     */
    private PaymentInformationDTO getCreditCardFromUser(UserDTO user) {
        List<PaymentInformationDTO> instruments = user.getPaymentInstruments();
        if(CollectionUtils.isEmpty(instruments)) {
            return null;
        }
        return instruments.stream()
                .sorted(Comparator.comparing(PaymentInformationDTO::getProcessingOrder))
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates {@link PaymentAuthorizationDTO} and {@link PaymentResult} for given payment
     * @param payment
     * @param paymentRequestWS
     * @param creditCard
     */
    private void createPaymentAuthorizationAndStorePaymentResult(PaymentDTO payment, PaymentRequestWS paymentRequestWS, PaymentInformationDTO creditCard) {
        PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
        paymentAuthDTO.setProcessor("WorldPay");
        paymentAuthDTO.setTransactionId(paymentRequestWS.getTransactionId());
        paymentAuthDTO.setCode1(paymentRequestWS.getTransactionId());
        if(PaymentResult.SUCCESS.equals(paymentRequestWS.getPaymentResult())) {
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_OK));
            payment.setBalance(payment.getAmount());
            paymentAuthDTO.setResponseMessage(PaymentResult.SUCCESS.name());
        } else {
            payment.setBalance(BigDecimal.ZERO);
            payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_FAIL));
            paymentAuthDTO.setResponseMessage(PaymentResult.FAILED.name());
        }
        // Creating payment Instrument Info
        payment.getPaymentInstrumentsInfo().add(new PaymentInstrumentInfoDTO(payment, payment.getPaymentResult(),
                payment.getPaymentMethod(), creditCard.getSaveableDTO()));

        // Creating payment authorization
        PaymentAuthorizationBL paymentAuthorizationBL = new PaymentAuthorizationBL();
        paymentAuthorizationBL.create(paymentAuthDTO, payment.getId());

        // Adding authorization on payment
        payment.getPaymentAuthorizations().add(paymentAuthorizationBL.getEntity());
    }

    /**
     * Returns Payment Method id based on cc type of {@link PaymentInformationDTO}
     * @param creditCard
     * @return
     */
    private Integer findPaymentMethodTypeFromCreditCard(PaymentInformationDTO creditCard) {
        Optional<MetaFieldValue<?>> ccType = creditCard.getMetaFieldByType(MetaFieldType.CC_TYPE);
        Integer userId = creditCard.getUser().getId();
        Integer languageId = creditCard.getUser().getLanguageIdField();
        Integer defaultPaymentMethodId = SapphirePaymentMethodType.OTHER.getPaymentMethod(languageId);
        if(!ccType.isPresent()) {
            logger.debug("No cc type field found on payment instrument for user {}, so using default payment method id {} ", userId, defaultPaymentMethodId);
            return defaultPaymentMethodId;
        }
        MetaFieldValue<?> ccTypeMetaFieldValue = ccType.get();
        String ccTypeStr = null;
        if(ccTypeMetaFieldValue.getValue() instanceof char[] &&
                ccTypeMetaFieldValue.getField().getDataType().equals(DataType.CHAR)) {
            CharMetaFieldValue charCCType = (CharMetaFieldValue) ccTypeMetaFieldValue;
            ccTypeStr = new String(charCCType.getValue());
        } else if(ccTypeMetaFieldValue.getValue() instanceof String &&
                ccTypeMetaFieldValue.getField().getDataType().equals(DataType.STRING)) {
            ccTypeStr = (String) ccTypeMetaFieldValue.getValue();
        }
        if(StringUtils.isEmpty(ccTypeStr)) {
            logger.debug("Data Type of cc type meta field is not correct, so using default payment method id {} for user {}", defaultPaymentMethodId, userId);
            return defaultPaymentMethodId;
        }
        return SapphirePaymentMethodType.findPaymentMethodIdByTypeAndLanguageId(ccTypeStr, languageId);
    }

    private void createPaymentFromPaymentRequest(SignupPlaceHolder holder) {
        SignupResponseWS response = holder.getSignUpResponse();
        try {
            PaymentRequestWS paymentRequest = holder.getSignUpRequest().getPaymentRequest();
            UserDTO user = new UserDAS().find(response.getUserId());
            if(null == paymentRequest) {
                logger.debug("No payment request found on user {}", user.getId());
                response.setPaymentResponse("NO-PAYMENT-REQUEST-FOUND");
                return;
            }
            PaymentInformationDTO creditCard = getCreditCardFromUser(user);
            if(null == creditCard) {
                logger.debug("No payment instrument found on user {}", user.getId());
                response.setPaymentResponse("NO-INSTRUMENT-FOUND-ON-CUSTOMER");
                return;
            }

            if(StringUtils.isEmpty(paymentRequest.getAmount())) {
                logger.debug("Payment Amount not found on payment rquest {} for user {}", paymentRequest, user.getId());
                response.setPaymentResponse("NO-PAYMENT-AMOUNT-FOUND");
                return;
            }

            BigDecimal amount = new BigDecimal(paymentRequest.getAmount()).setScale(CommonConstants.BIGDECIMAL_SCALE_STR, CommonConstants.BIGDECIMAL_ROUND);

            if(amount.compareTo(BigDecimal.ZERO) <= 0) {
                logger.debug("Payment Amount is Zero for user {}", user.getId());
                response.setPaymentResponse("NO-ONE-TIME-CHARGES-FOUND");
            }

            try (PaymentDTO payment = new PaymentDTO()) {
                payment.setBaseUser(user);
                payment.setCurrency(user.getCurrency());
                payment.setAmount(amount);
                // Set PaymentMethod id on payment and credit card.
                Integer paymentMethodId = findPaymentMethodTypeFromCreditCard(creditCard);
                payment.setPaymentMethod(new PaymentMethodDAS().find(paymentMethodId));
                creditCard.setPaymentMethodId(paymentMethodId);
                payment.setIsRefund(0);
                payment.setIsPreauth(0);
                payment.setDeleted(0);
                payment.setAttempt(1);
                payment.setPaymentDate(new Date());
                payment.setCreateDatetime(new Date());
                PaymentDTO savedPayment = new PaymentDAS().save(payment);
                createPaymentAuthorizationAndStorePaymentResult(savedPayment, paymentRequest, creditCard);
                response.setPaymentId(savedPayment.getId());
                response.setPaymentResponse(paymentRequest.getPaymentResult().name());
                response.setPaymentResult(paymentRequest.getPaymentResult().name());

                // trigger an event
                AbstractPaymentEvent event = AbstractPaymentEvent.forPaymentResult(
                            holder.getEntityId(), new PaymentDTOEx(savedPayment), true);
                if (event != null) {
                    EventManager.process(event);
                }
            }

        } catch(Exception ex) {
            logger.error("Error during make Payment", ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.setPaymentId(null);
            response.setPaymentResult("SAPP-ERROR-PAYMENT-FAILED");
            response.setPaymentResponse("SAPP-ERROR-PAYMENT-FAILED");
            logger.debug("Rollback tx");
        }

    }
}
