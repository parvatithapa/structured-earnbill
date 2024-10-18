package com.sapienter.jbilling.server.invoiceSummary;

import static org.junit.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;

public class InvoiceSummaryScenarioBuilder {
	private TestBuilder testBuilder;
	private String userName;
	private List<String> orderCodes ;
	private final static Integer CC_PM_ID = 5;

	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	private final static String CREDIT_CARD_NUMBER = "5257279846844529";
	
	public InvoiceSummaryScenarioBuilder(TestBuilder testBuilder) {
		this.testBuilder = testBuilder;
		this.orderCodes = new ArrayList<String>();
	}
	
	public List<String> getOrderCodes() {
		return this.orderCodes;
	}
	
	public String getUserName() {
		return this.userName;
	}
	
	public InvoiceSummaryScenarioBuilder createUser(String userName, Integer accountypeId, Date nextInvoiceDate, 
				Integer billingPeriod, Integer nextInvoiceDay) {
		this.testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			UserWS userWS = envBuilder.customerBuilder(api)
					.withUsername(userName)
					.withAccountTypeId(accountypeId)
					.addTimeToUsername(false)
					.withNextInvoiceDate(nextInvoiceDate)
					.withMainSubscription(new MainSubscriptionWS(billingPeriod, nextInvoiceDay))
					.build();

			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);
		}).test((testEnv, envBuilder) -> {
			assertNotNull("Customer Creation Failed", envBuilder.idForCode(userName));
			this.userName = userName;
		});
		
		return this;
	}

	public InvoiceSummaryScenarioBuilder createUser(JbillingAPI api,String userName, Integer accountypeId, Date nextInvoiceDate,
			Integer billingPeriod, Integer nextInvoiceDay) {
	this.testBuilder.given(envBuilder -> {
		UserWS userWS = envBuilder.customerBuilder(api)
				.withUsername(userName)
				.withAccountTypeId(accountypeId)
				.addTimeToUsername(false)
				.withNextInvoiceDate(nextInvoiceDate)
				.withMainSubscription(new MainSubscriptionWS(billingPeriod, nextInvoiceDay))
				.build();

		userWS.setNextInvoiceDate(nextInvoiceDate);
		api.updateUser(userWS);
	}).test((testEnv, envBuilder) -> {
		assertNotNull("Customer Creation Failed", envBuilder.idForCode(userName));
		this.userName = userName;
	});

	return this;
}
	
	public InvoiceSummaryScenarioBuilder createUser(String userName, Integer accountypeId, Date nextInvoiceDate,
			Integer billingPeriod, Integer nextInvoiceDay,Integer groupId) {
		this.testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			UserWS userWS = envBuilder.customerBuilder(api)
					.withUsername(userName)
					.withAccountTypeId(accountypeId)
					.addTimeToUsername(false)
					.withNextInvoiceDate(nextInvoiceDate)
					.withMainSubscription(new MainSubscriptionWS(billingPeriod, nextInvoiceDay))
					.withMetaField("Email Address", "test@jbilling.com")
					.withMetaField("First Name", "TestUser")
					.withMetaField("Last Name", "TestLname")
					.withMetaField("City", "Testcity")
					.withMetaField("State/Province", "TestState")
					.withMetaField("Postal Code", "40110")
					.withMetaField("Country", "US")
					.build();

			userWS.setNextInvoiceDate(nextInvoiceDate);

			api.updateUser(userWS);
		}).test((testEnv, envBuilder) -> {
			assertNotNull("Customer Creation Failed", envBuilder.idForCode(userName));
			this.userName = userName;
		});

		return this;
	}
	public InvoiceSummaryScenarioBuilder selectUserByName(String userName) {
		this.testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			if(testBuilder.getTestEnvironment().idForCode(userName) == null) {
				testBuilder.getTestEnvironment().add(userName, api.getUserId(userName), userName, api, TestEntityType.CUSTOMER);
			}
		}).test((testEnv, envBuilder) -> {
			assertNotNull("Invalid Customer ", envBuilder.idForCode(userName));
			this.userName = userName;
		});
		
		return this;
	}
	
	public InvoiceSummaryScenarioBuilder generateInvoice(Date runDate, boolean isRecurringOrder) {
		Calendar nextInvoiceDate = Calendar.getInstance();
		nextInvoiceDate.setTime(runDate);
		nextInvoiceDate.add(Calendar.MONTH, 1);

		this.testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			api.createInvoiceWithDate(envBuilder.idForCode(userName),runDate, PeriodUnitDTO.MONTH, 21, isRecurringOrder);
			
			UserWS userWS = api.getUserWS(envBuilder.idForCode(userName));
			userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
			api.updateUser(userWS);
			
		});
		
		return this;
	}
	
	public InvoiceSummaryScenarioBuilder generateInvoice(JbillingAPI api,Integer userId,Integer orderId,Integer invoiceId) {
		Calendar nextInvoiceDate = Calendar.getInstance();
		nextInvoiceDate.add(Calendar.MONTH, 1);

		this.testBuilder.given(envBuilder -> {
			api.createInvoiceFromOrder(orderId,invoiceId);
		});

		return this;
	}

	public InvoiceSummaryScenarioBuilder makePayment(String amount, Date paymentDate, boolean isRefund) {
		
		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal(amount));
		payment.setIsRefund(isRefund ? 1 : 0);
		payment.setPaymentDate(paymentDate);
		payment.setCreateDatetime(paymentDate);
		payment.setCurrencyId(Integer.valueOf(1));
		payment.setUserId(testBuilder.getTestEnvironment().idForCode(userName));
		PaymentWS lastPayment = testBuilder.getTestEnvironment().getPrancingPonyApi().getLatestPayment(payment.getUserId());
		if(isRefund && null!=lastPayment) {
		    payment.setPaymentId(lastPayment.getId());
		}
		payment.setResultId(Constants.RESULT_ENTERED);
		Calendar expiryDate = Calendar.getInstance();
		expiryDate.add(Calendar.YEAR, 10);

		payment.setPaymentInstruments(Arrays.asList(createCreditCard(UUID.randomUUID().toString(), CREDIT_CARD_NUMBER, expiryDate.getTime(), 2)));

		Integer paymentId = testBuilder.getTestEnvironment().getPrancingPonyApi().applyPayment( payment, null);
		assertNotNull("Payment should not null", paymentId);
		return this;
	}
	
	public InvoiceSummaryScenarioBuilder makeCreditPayment(String amount, Date paymentDate) {
			
			PaymentWS payment = new PaymentWS();
			payment.setAmount(new BigDecimal(amount));
			payment.setIsRefund(0);
			payment.setPaymentDate(paymentDate);
			payment.setCreateDatetime(paymentDate);
			payment.setCurrencyId(Integer.valueOf(1));
			payment.setUserId(testBuilder.getTestEnvironment().idForCode(userName));
			payment.setResultId(Constants.RESULT_ENTERED);
			Calendar expiryDate = Calendar.getInstance();
			expiryDate.add(Calendar.YEAR, 10);
	
			payment.setPaymentInstruments(Arrays.asList(createCreditCard(UUID.randomUUID().toString(), CREDIT_CARD_NUMBER, expiryDate.getTime(), 15)));
	
			Integer paymentId = testBuilder.getTestEnvironment().getPrancingPonyApi().createPayment(payment);
			assertNotNull("Payment result not null", paymentId);
			return this;
		}

	public InvoiceSummaryScenarioBuilder generateInvoiceForOrder(String code) {
		this.testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			api.createInvoiceFromOrder(envBuilder.idForCode(code), null);
		}).test((testEnv, envBuilder) -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			assertNotNull("Invoice Generation Failed", api.getLatestInvoice(envBuilder.idForCode(userName)));
			
		});
		
		return this;
	}
	
	public InvoiceSummaryScenarioBuilder createOrder(String code,Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId, int statusId, 
			boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, Integer> productAssetMap, boolean createNegativeOrder) {
		this.testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			List<OrderLineWS> lines = productQuantityMap.entrySet()
					  .stream()
					  .map((lineItemQuatityEntry) -> {
						  OrderLineWS line = new OrderLineWS();
							line.setItemId(lineItemQuatityEntry.getKey());
							line.setTypeId(Integer.valueOf(1));
							ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
							line.setDescription(item.getDescription());
							line.setQuantity(lineItemQuatityEntry.getValue());
							line.setUseItem(true);
							if(createNegativeOrder) {
								line.setUseItem(false);
								line.setPrice(item.getPriceAsDecimal().negate());
								line.setAmount(line.getQuantityAsDecimal().multiply(line.getPriceAsDecimal()));
							}
							if(null!=productAssetMap && !productAssetMap.isEmpty() 
									&& productAssetMap.containsKey(line.getItemId())) {
								line.setAssetIds(new Integer[] {productAssetMap.get(line.getItemId())});
							}
							return line;
					  }).collect(Collectors.toList());
			
			envBuilder.orderBuilder(api)
					.withCodeForTests(code)
					.forUser(envBuilder.idForCode(userName))
					.withActiveSince(activeSince)
					.withActiveUntil(activeUntil)
					.withEffectiveDate(activeSince)
					.withPeriod(orderPeriodId)
					.withBillingTypeId(billingTypeId)
					.withProrate(prorate)
					.withOrderLines(lines)
					.withOrderChangeStatus(statusId)
					.build();
		
		}).test((testEnv, envBuilder) -> {
			assertNotNull("Order Creation Failed", envBuilder.idForCode(code));
			this.orderCodes.add(code);
			
		});
		
		return this;
		
	}

	public InvoiceSummaryScenarioBuilder createOrder(JbillingAPI api,Integer userId,String code,Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId, int statusId,
			boolean prorate, Map<Integer, BigDecimal> productQuantityMap) {
		this.testBuilder.given(envBuilder -> {
			List<OrderLineWS> lines = productQuantityMap.entrySet()
					  .stream()
					  .map((lineItemQuatityEntry) -> {
						  OrderLineWS line = new OrderLineWS();
							line.setItemId(lineItemQuatityEntry.getKey());
							line.setTypeId(Integer.valueOf(1));
							ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
							line.setDescription(item.getDescription());
							line.setQuantity(lineItemQuatityEntry.getValue());
							line.setUseItem(true);
							return line;
					  }).collect(Collectors.toList());

			envBuilder.orderBuilder(api)
					.withCodeForTests(code)
					.forUser(userId)
					.withActiveSince(activeSince)
					.withActiveUntil(activeUntil)
					.withEffectiveDate(activeSince)
					.withPeriod(orderPeriodId)
					.withBillingTypeId(billingTypeId)
					.withProrate(prorate)
					.withOrderLines(lines)
					.withOrderChangeStatus(statusId)
					.build();

		}).test((testEnv, envBuilder) -> {
			assertNotNull("Order Creation Failed", envBuilder.idForCode(code));
			this.orderCodes.add(code);

		});

		return this;
	}

	
	public InvoiceSummaryScenarioBuilder createOrderWithPrice(String code,Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId, int statusId, 
			boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, BigDecimal> productPriceMap) {
		this.testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			List<OrderLineWS> lines = productQuantityMap.entrySet()
					  .stream()
					  .map((lineItemQuatityEntry) -> {
						  OrderLineWS line = new OrderLineWS();
							line.setItemId(lineItemQuatityEntry.getKey());
							line.setTypeId(Integer.valueOf(1));
							ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
							line.setDescription(item.getDescription());
							line.setQuantity(lineItemQuatityEntry.getValue());
							line.setUseItem(true);
							
							if(null!=productPriceMap && !productPriceMap.isEmpty() 
									&& productPriceMap.containsKey(line.getItemId())) {
								line.setUseItem(false);
								line.setPrice(productPriceMap.get(line.getItemId()));
								line.setAmount(line.getQuantityAsDecimal().multiply(productPriceMap.get(line.getItemId())));
							}
							return line;
					  }).collect(Collectors.toList());
			
			envBuilder.orderBuilder(api)
					.withCodeForTests(code)
					.forUser(envBuilder.idForCode(userName))
					.withActiveSince(activeSince)
					.withActiveUntil(activeUntil)
					.withEffectiveDate(activeSince)
					.withPeriod(orderPeriodId)
					.withBillingTypeId(billingTypeId)
					.withProrate(prorate)
					.withOrderLines(lines)
					.withOrderChangeStatus(statusId)
					.build();
		
		}).test((testEnv, envBuilder) -> {
			assertNotNull("Order Creation Failed", envBuilder.idForCode(code));
			this.orderCodes.add(code);
			
		});
		return this;
	}
			
	public InvoiceSummaryScenarioBuilder triggerMediation(String jobConfigName, List<String> cdr) {
		
		testBuilder.given(envBuilder -> {
			JbillingAPI api = envBuilder.getPrancingPonyApi();
			api.processCDR(getMediationConfiguration(api, jobConfigName), cdr);
			
		}).test((testEnv, envBuilder) -> {
			JbillingAPI api = envBuilder.getPrancingPonyApi();
			MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            assertEquals("Mediation Failed", new Integer(cdr.size()), mediationProcess.getDoneAndBillable());
			
		});
		return this;
	}
	
	 private Integer getMediationConfiguration(JbillingAPI api, String mediationJobLauncher) {

	        MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
	        for (MediationConfigurationWS mediationConfigurationWS: allMediationConfigurations) {
	            if (null != mediationConfigurationWS.getMediationJobLauncher() &&
	                    (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
	                return mediationConfigurationWS.getId();
	            }
	        }
	        return null;
	    }
	private PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date, Integer methodId) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Integer.valueOf(methodId));
		
		//cc

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, new SimpleDateFormat(Constants.CC_DATE_FORMAT).format(date).toCharArray());

		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, false, false,
				DataType.STRING, 4, CreditCardType.MASTER_CARD);
		addMetaField(metaFields, "cc.gateway.key" ,false, true, DataType.CHAR, 5, null );
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}

	private void addMetaField(List<MetaFieldValueWS> metaFields,
			String fieldName, boolean disabled, boolean mandatory,
			DataType dataType, Integer displayOrder, Object value) {
		MetaFieldValueWS ws = new MetaFieldValueWS();
		ws.setFieldName(fieldName);
		ws.getMetaField().setDisabled(disabled);
		ws.getMetaField().setMandatory(mandatory);
		ws.getMetaField().setDataType(dataType);
		ws.getMetaField().setDisplayOrder(displayOrder);
		ws.setValue(value);

		metaFields.add(ws);
	}
	
}
