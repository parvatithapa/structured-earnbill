package com.sapienter.jbilling.server.payment.tasks;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.tasks.paysafe.PaySafeProcessedPaymentEvent;
import com.sapienter.jbilling.server.payment.tasks.paysafe.PaySafeResultType;
import com.sapienter.jbilling.server.payment.tasks.paysafe.PaySafeStatus;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.paysafe.PaysafeApiClient;
import com.paysafe.cardpayments.Authorization;
import com.paysafe.cardpayments.Refund;
import com.paysafe.cardpayments.Settlement;
import com.paysafe.cardpayments.Settlement.SettlementBuilder;
import com.paysafe.cardpayments.Refund.RefundBuilder;
import com.paysafe.common.Id;
import com.paysafe.common.Locale;
import com.paysafe.common.PaysafeException;
import com.paysafe.customervault.Address;
import com.paysafe.customervault.Card;
import com.paysafe.customervault.Profile;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.IExternalCreditCardStorage;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.tasks.paysafe.PaySafePayerInfo;
import com.sapienter.jbilling.server.payment.tasks.paysafe.PaySafeResult;
import com.sapienter.jbilling.server.pluggableTask.PaymentTaskWithTimeout;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * 
 * @author krunal Bhavsar
 *
 */
public class PaymentPaySafeTask extends PaymentTaskWithTimeout implements IExternalCreditCardStorage {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/* Plugin parameters */
	public static final ParameterDescription PARAMETER_PAY_SAFE_MERCHANT_ACCOUNT_NUMBER =
			new ParameterDescription("MerchantAccountNumber", true, ParameterDescription.Type.STR, false);

	public static final ParameterDescription PARAMETER_PAY_SAFE_USERNAME =
			new ParameterDescription("UserName", true, ParameterDescription.Type.STR, false);
	public static final ParameterDescription PARAMETER_PAY_SAFE_PASSWORD =
			new ParameterDescription("Password", true, ParameterDescription.Type.STR, true);

	public static final ParameterDescription PARAMETER_PAY_SAFE_URL =
			new ParameterDescription("Url", true, ParameterDescription.Type.STR, false);
	
	public static final ParameterDescription PARAMETER_PAY_SAFE_CURRENCY_CODE =
			new ParameterDescription("CurrencyCode", true, ParameterDescription.Type.STR, false);
	
	public static final ParameterDescription PARAMETER_PAY_SAFE_CONTACT_SECTION_NAME =
			new ParameterDescription("Customer Contact Section Name", false, ParameterDescription.Type.STR, false);
	
	public static final ParameterDescription PARAMETER_PAY_SAFE_BILLING_SECTION_NAME =
			new ParameterDescription("Customer Billing Address Section Name", false, ParameterDescription.Type.STR, false);

	public static final ParameterDescription PARAMETER_PAYSAFE_NO_RETRY_CODES =
			new ParameterDescription("No Retry Codes", true, ParameterDescription.Type.STR, false);

	public static final ParameterDescription PARAMETER_PAYSAFE_RETRY_LIMIT =
			new ParameterDescription("Retry Limit", true, ParameterDescription.Type.INT, false);

	public static final String PAYMENT_ATTEMPT_COUNT= "Payment Attempt Count";

	public static final String PAYMENT_STATUS= "Status";

	private static final String PAYMENT_PROCESSOR_NAME = "Pay Safe";
	public static final String PAYSAFE_PROFILE_ID = "Paysafe Profile Id";

	//initializer for pluggable params
	{
		descriptions.add(PARAMETER_PAY_SAFE_MERCHANT_ACCOUNT_NUMBER);
		descriptions.add(PARAMETER_PAY_SAFE_USERNAME);
		descriptions.add(PARAMETER_PAY_SAFE_PASSWORD);
		descriptions.add(PARAMETER_PAY_SAFE_URL);
		descriptions.add(PARAMETER_PAY_SAFE_CURRENCY_CODE);
		descriptions.add(PARAMETER_PAY_SAFE_CONTACT_SECTION_NAME);
		descriptions.add(PARAMETER_PAY_SAFE_BILLING_SECTION_NAME);
		descriptions.add(PARAMETER_PAYSAFE_NO_RETRY_CODES);
		descriptions.add(PARAMETER_PAYSAFE_RETRY_LIMIT);
	}
	    
	@Override
	public boolean process(PaymentDTOEx paymentInfo) throws PluggableTaskException {
		if(isRefund(paymentInfo)) {
			return processRefund(paymentInfo).shouldCallOtherProcessors();
		} else if(isCreditCardStored(paymentInfo)) {
			return processPaymentForStoredCreditCard(paymentInfo).shouldCallOtherProcessors();
		}  
		return processOneTimePayment(paymentInfo).shouldCallOtherProcessors();
	}

	@Override
	public void failure(Integer userId, Integer retry) {
		logger.debug("Payment failed for user {} {}", userId,retry);
	}

	@Override
	public boolean preAuth(PaymentDTOEx paymentInfo) throws PluggableTaskException {
		return false;
	}

	@Override
	public boolean confirmPreAuth(PaymentAuthorizationDTO auth, PaymentDTOEx paymentInfo) throws PluggableTaskException {
		return false;
	}

	@Override
	public String storeCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
		try(PaymentInformationBL piBL = new PaymentInformationBL()) {
			char[] paymentToken  = piBL.getCharMetaFieldByType(instrument, MetaFieldType.GATEWAY_KEY);
			
			if(null != paymentToken && paymentToken.length > 0) {
				return new String(paymentToken);
			}
			
			PaysafeApiClient apiClient = getAPI();
			PaySafePayerInfo payer = createPayerFromPaymentInformation(instrument, null);
			
			// Create a profile
	    	 Profile customerProfile  = Profile.builder()
	    		          					   .merchantCustomerId(payer.getMerchantCustomerId())
	    		          					   .locale(Locale.EN_US)
	    		          					   .firstName(payer.getFirstName()) // optional
	    		          					   .lastName(payer.getLastName())   // optional
	    		          					   .email(payer.getEmail()) // optional
	    		          					   .build();
	    	 Id<Profile> profileId = apiClient.customerVaultService().create(customerProfile).getId();
	    	 Id<Address> addressId = null;
	    	 
	    	 if(payer.getZip()!=null && payer.getCountryCode()!=null) {
	    		 
	         Address serviceAddress = Address.builder()
	        		 						 .profileId(profileId)
	        		 						 .street(payer.getStreet()) //optional
	        		 						 .city(payer.getCity())      //optional
	        		 						 .state(payer.getState())  //optional
	        		 						 .country(payer.getCountryCode())
	        		 						 .zip(payer.getZip())
	        		 						 .build();
	         	addressId = apiClient.customerVaultService().create(serviceAddress).getId();
	    	 }
	         
	        

	         // Add card to profile
	         Card card = Card.builder()
	        		 		 .profileId(profileId)
	        		 		 .holderName(payer.getCardHolderName())
	        		 		 .cardNum(payer.getCreditCardNumber())
	        		 		 .billingAddressId(addressId)
	        		 		 .cardExpiry()
	        		 		 .month(Integer.valueOf(payer.getExpiryMonth()))
	        		 		 .year(Integer.valueOf(payer.getExpiryYear()))
	        		 		 .done()
	        		 		 .build();
	         
			 instrument.getMetaFields().stream()
					 .filter(mfv -> mfv.getField().getName().equals(PAYSAFE_PROFILE_ID))
					 .findFirst()
					 .get().setValue(profileId.toString());
			
	         return apiClient.customerVaultService().create(card).getPaymentToken();
		} catch(Exception ex) {
			 logger.error("Could not create customer profile on paysafe gateway {}", ex);
			 throw new SessionInternalError(ex);
		}
	}

	@Override
	public char[] deleteCreditCard(ContactDTO contact, PaymentInformationDTO instrument) {
		return new char[0];
	}
	
	private static boolean isRefund(PaymentDTOEx payment) {
        return BigDecimal.ZERO.compareTo(payment.getAmount()) > 0 || payment.getIsRefund() != 0;
    }
	
	@SuppressWarnings("resource")
	private static boolean isCreditCardStored(PaymentDTOEx payment) {
		PaymentInformationBL piBL = new PaymentInformationBL();
		PaymentInformationDTO instrument = payment.getInstrument();
		char [] token = piBL.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY); 
        return piBL.useGatewayKey(instrument) && (token!=null && token.length > 0);
    }
	
	private Result processOneTimePayment(PaymentDTOEx payment) {
		try(PaymentInformationDTO card = payment.getInstrument()) {
			PaySafePayerInfo payer = createPayerFromPaymentInformation(card, payment.getUserId());
						
			BigDecimal amount = convertAmount(payment.getAmount(), payment.getCurrency().getId(), payment.getPaymentDate());
			
			Authorization oneTimePayment = Authorization.builder()
					 									 .merchantRefNum(UUID.randomUUID().toString())
					 									 .amount(amount.multiply(new BigDecimal("100")).setScale(0))
					 									 .settleWithAuth(true)
					 									 .card()
					 									 .cardNum(payer.getCreditCardNumber())
					 									 	.cardExpiry()
					 									 	.month(Integer.valueOf(payer.getExpiryMonth()))
					 									 	.year(Integer.valueOf(payer.getExpiryYear()))
					 									 	.done()
					 									 .done()
					 									 .billingDetails()
					 									 	.zip(payer.getZip())
					 									 	.state(payer.getState())
					 									 	.city(payer.getCity())
					 									 	.country(payer.getCountryCode())
					 									 	.done()
					 									 .build();
			PaySafeResult result = sendPaymentRequestToGateWay(oneTimePayment);
			PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
			storePaySafeResult(result, payment, paymentAuthorization);
			obscureCreditCardNumber(payment);
			handlePaymentResult(payment, result.isSucceeded() ? PaySafeResultType.SUCESSFUL : PaySafeResultType.FAILURE);
			return new Result(paymentAuthorization, false);
		} catch(Exception ex) {
			logger.error("Couldn't handle one time payment request due to error {}", ex);
			payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
			return NOT_APPLICABLE;
		}
	}
	
	private Result processRefund(PaymentDTOEx payment) {
		try {
			PaySafePayerInfo payer = createPayerForRefund(payment);
			Refund refund = new RefundBuilder()
								.amount(payer.getAmount())
								.merchantRefNum(payer.getMerchantRefNumber())
								.settlementId(new SettlementBuilder().id(Id.create(payer.getParentPaymentTransactionId(), Settlement.class)).build().getId())
								.build();
			
			PaySafeResult result = sendRefundRequestToGateWay(refund);
			PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
			storePaySafeResult(result, payment, paymentAuthorization);
			obscureCreditCardNumber(payment);
			if (!result.isSucceeded()) {
				managePaySafeRetryLimits(result, payment);
			} else {
				handlePaymentResult(payment, PaySafeResultType.SUCESSFUL);
			}
			return new Result(paymentAuthorization, false);
		} catch(Exception ex) {
			logger.error("Couldn't handle refund payment request due to error {}", ex);
			payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
			return NOT_APPLICABLE;
		}
	}
	
	private Result processPaymentForStoredCreditCard(PaymentDTOEx payment) {
		try {
			PaySafePayerInfo payer = createPayerForPayment(payment);
			if(payer == null){
                payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_BILLING_INFORMATION_NOT_FOUND));
        		return new Result(null, false);
        	}
			
			Authorization auth = Authorization.builder()
			               						.merchantRefNum(payer.getMerchantRefNumber())
			               						.amount(payer.getAmount())
			               						.settleWithAuth(true)
			               						.card()
			               							.paymentToken(payer.getTokenId())
			               							.done()
			               						.build();
			PaySafeResult result = sendPaymentRequestToGateWay(auth);
			PaymentAuthorizationDTO paymentAuthorization = buildPaymentAuthorization(result);
			storePaySafeResult(result, payment, paymentAuthorization);
			if (!result.isSucceeded()) {
				managePaySafeRetryLimits(result, payment);
			} else {
				handlePaymentResult(payment, PaySafeResultType.SUCESSFUL);
			}
			setPaySafeProfileId(payment);
			return new Result(paymentAuthorization, false);
		} catch(Exception ex) {
			logger.error("Couldn't handle payment request due to error {}", ex);
			payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_UNAVAILABLE));
			return NOT_APPLICABLE;
		}
		
	}
	
	private void setPaySafeProfileId(PaymentDTOEx payment) {
		MetaFieldValue paySafeProfileIdMF = payment.getInstrument().getMetaField(PAYSAFE_PROFILE_ID);
		if (paySafeProfileIdMF != null) {
			PaymentDAS paymentDas = new PaymentDAS();
			PaymentDTO paymentDTO = paymentDas.findNow(payment.getId());
			paymentDTO.setMetaField(getEntityId(), null, PAYSAFE_PROFILE_ID,  paySafeProfileIdMF.getValue());
			paymentDas.save(paymentDTO);
		}		
	}
	
	private void managePaySafeRetryLimits(PaySafeResult result, PaymentDTOEx payment){
		try {
			MetaFieldValue mf = payment.getInstrument().getMetaFields().stream()
					.filter(metafieldValue -> PAYMENT_ATTEMPT_COUNT.equals(metafieldValue.getField().getName()))
					.findFirst().get();
			Integer attemptCount = mf.getValue() != null ? (Integer) mf.getValue() : Integer.valueOf(0);
			attemptCount++;
			mf.setValue(attemptCount);
			PaySafeStatus paymentStatus = PaySafeStatus.ACTIVE;
			PaySafeResultType resultType = PaySafeResultType.FAILURE;
			if (getPaysafeNoRetryCodes().contains(result.getErrorCode())) {
				paymentStatus = PaySafeStatus.CANCELLED;
				resultType = PaySafeResultType.CANCELLED;
			} else if (attemptCount.compareTo(Integer.valueOf(getPaysafeRetryLimit())) >= 0) {
				paymentStatus = PaySafeStatus.DISABLED;
				resultType = PaySafeResultType.DISABLED;
			}
			handlePaymentResult(payment, resultType);
	
	        for (MetaFieldValue metaFieldValue : payment.getInstrument().getMetaFields()) {
	            String mfname = metaFieldValue.getField().getName();
	            if (PAYMENT_STATUS.equals(mfname)) {
	                metaFieldValue.setValue(paymentStatus.getName());
	            }
	            if (!PaySafeStatus.ACTIVE.equals(paymentStatus) &&
	                MetaFieldType.AUTO_PAYMENT_AUTHORIZATION.equals(metaFieldValue.getField().getFieldUsage())) {
	                metaFieldValue.setValue(Boolean.FALSE);
	            }
	        }
	
			PaymentDAS paymentDas = new PaymentDAS();
			PaymentDTO paymentDTO = paymentDas.find(payment.getId());
			paymentDTO.setMetaField(getEntityId(), null, PAYMENT_ATTEMPT_COUNT, attemptCount);
			paymentDTO.setMetaField(getEntityId(), null, PAYMENT_STATUS, paymentStatus.getName());
			paymentDas.save(paymentDTO);
		} catch (Exception e) {
			// We have shallow exception because we don't want rollback payment transaction
            logger.error("Error in managePaySafeRetryLimits {}", e);
		}
	}
	
	/**
     * stores PaySafe Result in  DataBase
     * @param result
     * @param payment
     * @param paymentAuthorization
     */
    private void storePaySafeResult(PaySafeResult result, PaymentDTOEx payment, PaymentAuthorizationDTO paymentAuthorization) {
    	if(result.isSucceeded()) {
    		payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_OK));
    	} else {
    		payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_FAIL));
    	}
    	new PaymentAuthorizationBL().create(paymentAuthorization, payment.getId());
		payment.setAuthorization(paymentAuthorization);
    }
    
	 private PaymentAuthorizationDTO buildPaymentAuthorization(PaySafeResult result) {
	        logger.debug("Payment authorization result of {}", getProcessorName() + " gateway parsing....");

	        PaymentAuthorizationDTO paymentAuthDTO = new PaymentAuthorizationDTO();
	        paymentAuthDTO.setProcessor(getProcessorName());

	        String txID = result.getTransactionId(); // Transaction Id
	        if (txID != null) {
	            paymentAuthDTO.setTransactionId(txID);
	            logger.debug("transactionId/code1 [{}", txID + "]");
	        }
	        
	        if (txID != null) {
	            paymentAuthDTO.setCode1(txID);
	        }
	        
	        if(result.getErrorMessage()!=null) {
	        	paymentAuthDTO.setResponseMessage(result.getErrorMessage());
	        }
	        
	        String merchantRefNumber = result.getMerchantRefNumber(); // MerchantRefNumber
	        if(merchantRefNumber!=null) {
	        	paymentAuthDTO.setCode2(merchantRefNumber);
	        }
	        if(result.getAuthCode()!=null) {
	        	paymentAuthDTO.setApprovalCode(result.getAuthCode());
	        }
	 
		    if (result.getErrorCode() != null) {
				paymentAuthDTO.setCode3(result.getErrorCode());
			}
	        
	        String avs = result.getAvs();
	        if(avs != null) {
	            paymentAuthDTO.setAvs(avs);
	            logger.debug("avs [{}", avs + "]");
	        }
	        return paymentAuthDTO;
	    }
	 
	 private PaySafeResult sendPaymentRequestToGateWay(Authorization paymentRequest) throws IOException {
		 try {
			 PaySafeResult result = new PaySafeResult();
			 Authorization gateWayResponse = getAPI().cardPaymentService().authorize(paymentRequest);
			 result.setAuthCode(gateWayResponse.getAuthCode());
			 result.setTransactionId(gateWayResponse.getId().toString());
			 result.setSucceeded(true);
			 result.setMerchantRefNumber(gateWayResponse.getMerchantRefNum());
			 result.setAvs(gateWayResponse.getAvsResponse()!=null ? gateWayResponse.getAvsResponse().name() : null);
			 return result;
		 } catch(PaysafeException exception) {
			 Authorization authResponse = (Authorization) exception.getRawResponse();
			 PaySafeResult result = new PaySafeResult();
			 result.setSucceeded(false);
			 result.setTransactionId(authResponse.getId() != null ? authResponse.getId().toString() : "NO TRANSACTION");
			 result.setMerchantRefNumber(authResponse.getMerchantRefNum());
			 result.setErrorMessage(exception.getMessage());
			 result.setErrorCode(exception.getCode());
			 logger.debug("Error occurred while sending payment request to gateway {}", exception);
			 return result;

		 }
	 }
		
	
	
	private PaySafeResult sendRefundRequestToGateWay(Refund refund) throws IOException {
		try {
			Refund refundResponse = getAPI().cardPaymentService().refund(refund);
			
			PaySafeResult result = new PaySafeResult();
			result.setMerchantRefNumber(refundResponse.getMerchantRefNum());
			result.setSucceeded(true);
			result.setTransactionId(refundResponse.getId().toString());
			return result;
		} catch(PaysafeException exception) {
			Refund refundResponse = (Refund) exception.getRawResponse();
			PaySafeResult result = new PaySafeResult();
			result.setSucceeded(false);
			result.setTransactionId(refundResponse.getId().toString());
			result.setMerchantRefNumber(refundResponse.getMerchantRefNum());
			result.setErrorMessage(exception.getMessage());
			result.setErrorCode(exception.getCode());
			logger.debug("Error occurred while sending refund request to gateway {}", exception);
			return result;
		}
		
	}
	
	private PaySafePayerInfo createPayerForPayment(PaymentDTOEx payment) {
		PaySafePayerInfo payer = new PaySafePayerInfo();
		BigDecimal amount = convertAmount(payment.getAmount(), payment.getCurrency().getId(), payment.getPaymentDate());
		
		try(PaymentInformationBL piBl = new PaymentInformationBL()) {
			payer.addTokenId(new String(piBl.getCharMetaFieldByType(payment.getInstrument(), MetaFieldType.GATEWAY_KEY)))
				 .addAmount(amount.multiply(new BigDecimal("100")).setScale(0))
				 .addMerchantRefNumber(UUID.randomUUID().toString());
		} catch (Exception e) {
			logger.debug("Error occurred while creating payer for payment {}", e);
		}
		return payer;
	}
	
	private PaySafePayerInfo createPayerForRefund(PaymentDTOEx payment) {
		PaySafePayerInfo payer = new PaySafePayerInfo();
		BigDecimal amount = convertAmount(payment.getAmount(), payment.getCurrency().getId(), payment.getPaymentDate());
		PaymentAuthorizationDTO parentPaymentAuthorization = getParentPaymentAuthorization(payment);
		
		payer.addAmount(amount.multiply(new BigDecimal("100")).setScale(0));
		if(null!=parentPaymentAuthorization) {
			 payer.addMerchantRefNumber(UUID.randomUUID().toString())
			 	  .addParentPaymentTransactionId(parentPaymentAuthorization.getTransactionId());
		}
		return payer;
	}
	
	private static PaymentAuthorizationDTO getParentPaymentAuthorization(PaymentDTOEx payment) {
        return isRefund(payment) ? new PaymentDAS().findNow(payment.getPayment().getId())
        										   .getPaymentAuthorizations()
        										   .stream()
        										   .findFirst()
        										   .get() : null;
    }
	
	private PaySafePayerInfo createPayerFromPaymentInformation(PaymentInformationDTO creditCard, Integer userId) {
		PaySafePayerInfo payer = new PaySafePayerInfo();
		
		 UserDTO user = creditCard.getUser();
		 
		 if(user == null) {
			 user = new UserDAS().findNow(userId);
		 }
		 
		 Integer entityId= user.getCompany().getId();
	        
		 CustomerDTO customer = user.getCustomer();
		 int accountTypeId = customer.getAccountType().getId();
		 AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
		 Map<String, String> billingAddressMetaFieldMapByMetaFieldType = new HashMap<>();
		 Map<String, String> contactMetaFieldMapByMetaFieldType = new HashMap<>();
		 Map<Integer, String> accountAITSectionIdAndNameMap = accountInformationTypeDAS.getInformationTypeIdAndNameMapForAccountType(accountTypeId);
		 
		 Optional<Entry<Integer, String>> contactSectionIdAndName = getKeyByValue(accountAITSectionIdAndNameMap, getContactSectionName());
		 Optional<Entry<Integer, String>> billingAddressSectionIdAndName = getKeyByValue(accountAITSectionIdAndNameMap, getBillingSectionName());
		 
		 if(contactSectionIdAndName.isPresent()) {
			 contactMetaFieldMapByMetaFieldType.putAll(getCustomerAITMetaFields(customer, contactSectionIdAndName.get().getKey(), TimezoneHelper.companyCurrentDate(entityId)));
		 }
		 
		 if(billingAddressSectionIdAndName.isPresent()) {
			 billingAddressMetaFieldMapByMetaFieldType.putAll(getCustomerAITMetaFields(customer, billingAddressSectionIdAndName.get().getKey(), TimezoneHelper.companyCurrentDate(entityId)));
		 }
		 
		 if("true".equals(billingAddressMetaFieldMapByMetaFieldType.getOrDefault(MetaFieldType.INITIAL.name(), " "))) {
			 billingAddressMetaFieldMapByMetaFieldType.clear();
			 billingAddressMetaFieldMapByMetaFieldType.putAll(contactMetaFieldMapByMetaFieldType);
		 }
		 
		 Map<String, String> paymentMetaFieldMap = getPaymentInstrumentMetaFields(creditCard);
		 
		 String expiryDateFieldValue = paymentMetaFieldMap.get(MetaFieldType.DATE.name());
		 String creditCardNumber = paymentMetaFieldMap.get(MetaFieldType.PAYMENT_CARD_NUMBER.name());
		 String cardHolderName = paymentMetaFieldMap.get(MetaFieldType.INITIAL.name());
		 String expiryYear = null;
		 String expiryMonth = null;

		 if(null!= expiryDateFieldValue && !expiryDateFieldValue.isEmpty() && expiryDateFieldValue.split("/").length ==2) {
			 expiryYear = expiryDateFieldValue.split("/")[1];
			 expiryMonth = expiryDateFieldValue.split("/")[0];
		 }

		 payer.addFirstName(contactMetaFieldMapByMetaFieldType.get(MetaFieldType.FIRST_NAME.toString()))
		 	  .addLastName(contactMetaFieldMapByMetaFieldType.get(MetaFieldType.LAST_NAME.toString()))
		 	  .addCity(billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.CITY.toString()))
		 	  .addEmail(billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.BILLING_EMAIL.toString()))
		 	  .addCountryCode(billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.COUNTRY_CODE.toString()))
		 	  .addZip(billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.POSTAL_CODE.toString()))
		 	  .addCreditCardNumber(creditCardNumber)
		 	  .addExpiryMonth(expiryMonth)
		 	  .addExpiryYear(expiryYear)
		 	  .addState(billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.STATE_PROVINCE.toString()))
		 	  .addStreet(billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.ADDRESS1.toString()))
		 	  .addCardHolderName(cardHolderName)
		 	  .addMerchantCustomerId(UUID.randomUUID().toString());

		 if(payer.getFirstName() == null) {
			 payer.addFirstName(billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.ORGANIZATION.toString()));
		 } 
		 if(payer.getEmail() == null) {
			 payer.addEmail(billingAddressMetaFieldMapByMetaFieldType.get(MetaFieldType.EMAIL.toString()));
		 } 
		 if(payer.getLastName() == null) {
			 payer.addLastName(cardHolderName);
		 }

		 return payer;
	}
	 
	 public  String getUserName() {
		 return  parameters.get(PARAMETER_PAY_SAFE_USERNAME.getName());
	 }
	 
	 public String getContactSectionName() {
		 return parameters.get(PARAMETER_PAY_SAFE_CONTACT_SECTION_NAME.getName());
	 }
	 
	 public String getBillingSectionName() {
		return parameters.get(PARAMETER_PAY_SAFE_BILLING_SECTION_NAME.getName());
	 }

	 public  String getPassword() {
		 return  parameters.get(PARAMETER_PAY_SAFE_PASSWORD.getName());
	 }

	 public  String getMerchantAccountNumber() {
		 return  parameters.get(PARAMETER_PAY_SAFE_MERCHANT_ACCOUNT_NUMBER.getName());
	 }

	 public  String getUrl() {
		 return  parameters.get(PARAMETER_PAY_SAFE_URL.getName());
	 }
	 
	 public  String getCurrencyCode() {
		 return  parameters.get(PARAMETER_PAY_SAFE_CURRENCY_CODE.getName());
	 }

	public  String getPaysafeNoRetryCodes() {
		return  parameters.get(PARAMETER_PAYSAFE_NO_RETRY_CODES.getName());
	}

	public  String getPaysafeRetryLimit() {
		return  parameters.get(PARAMETER_PAYSAFE_RETRY_LIMIT.getName());
	}
	 
	private PaysafeApiClient getAPI() { 
		 return  new PaysafeApiClient(getUserName(), getPassword(), getUrl(), getMerchantAccountNumber(),getTimeoutSeconds()*1000);
	}
	
	/**
     * Returns the name of this payment processor.
     * @return payment processor name
     */
    private String getProcessorName() {
        return PAYMENT_PROCESSOR_NAME;
    }
    
    private void obscureCreditCardNumber(PaymentDTOEx payment) {
        try(PaymentInformationBL piBL = new PaymentInformationBL()) {
            // obscure new credit card numbers
        	PaymentInformationDTO card = payment.getInstrument();
            if (!com.sapienter.jbilling.common.Constants.PAYMENT_METHOD_GATEWAY_KEY.equals(card.getPaymentMethod().getId())) {
                piBL.obscureCreditCardNumber(card);
            }

        }catch (Exception exception){
            logger.debug("Error occurred while obscuring credit card {} ", exception);
        }
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
    
    private BigDecimal convertAmount(BigDecimal amount, Integer fromCurrencyId, Date paymentDate) {
    	CurrencyBL bl = new CurrencyBL();
    	return bl.convert(fromCurrencyId, bl.findCurrencyByCode(getCurrencyCode()).getId(), amount, paymentDate, getEntityId())
    			 .setScale(2, RoundingMode.HALF_UP);
    }
    
    private static Optional<Entry<Integer, String>> getKeyByValue(Map<Integer, String> map, String value) {
        return map.entrySet()
        		  .stream()
        		  .filter(mapEntry -> mapEntry.getValue().equals(value))
        		  .findFirst();
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

	private void handlePaymentResult(PaymentDTOEx payment, PaySafeResultType resultType) {
		try {
			PaySafeProcessedPaymentEvent paySafeProcessedPaymentEvent = new PaySafeProcessedPaymentEvent(getEntityId(), resultType, payment, true);
			EventManager.process(paySafeProcessedPaymentEvent);
		} catch (Exception e) {
			// We have shallow exception because we don't want rollback payment transaction
			logger.error("Error in handlePaymentResult {}", e);
		}
	}
}
