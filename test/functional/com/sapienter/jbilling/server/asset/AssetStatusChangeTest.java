package com.sapienter.jbilling.server.asset;

import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;

/**
 * JB-2247
 * Automation Test Coverage for Asset status change at Order status finish
 *
 * @author Harshad Pathan
 */

@Test(groups = { "web-services", "asset" }, testName = "AssetStatusChangeTest")
public class AssetStatusChangeTest{

	private static final Logger logger = LoggerFactory.getLogger(AssetStatusChangeTest.class);
	
    private TestBuilder testBuilder;
    private EnvironmentHelper environmentHelper;

	private static final String CATEGORY_CODE = "CRTestCategory";
    private static final String PRODUCT_CODE = "CRTestProduct";
    private static final String ACCOUNT_TYPE_CODE = "CRTestAccountType";
    private static final String CUSTOMER_CODE = "CRTestCustomer";
    private static final Integer ACTIVE_STATUS = Integer.valueOf(1);
    
    private Integer CATEGORY_ID;
    private Integer PRODUCT_ID;
    private Integer ACCOUNT_ID;
    private Integer ASSET_ID;
    private Integer ORDER_ID;
    private Integer AVAILABLE;
    private Integer IN_USE;
    private Integer finishedOrderStatus;
       
    @BeforeClass
    public void initializeTests(){
        testBuilder = getTestEnvironment();
    }

    
    @AfterClass
    public void tearDown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        environmentHelper = null;
        testBuilder = null;
    }
    
    private TestBuilder getTestEnvironment() {
    	return TestBuilder.newTest(true).givenForMultiple(envCreator -> {
    		final JbillingAPI api = envCreator.getPrancingPonyApi();
    		environmentHelper = EnvironmentHelper.getInstance(api);


    		CATEGORY_ID = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE)
    								.global(true).allowAssetManagement(1)
    								.build();
    		
    		PRODUCT_ID = envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(true).withType(CATEGORY_ID)
    							   .withAssetManagementEnabled(1)
    							   .withFlatPrice("0.50").build();

    		ASSET_ID =  envCreator.assetBuilder(api).withCode("1236547895").withItemId(PRODUCT_ID)
    							  .withAssetStatusId(101)
    							  .build();
    		AVAILABLE = api.getItemCategoryById(CATEGORY_ID).getAssetStatuses().stream()
    						.filter(status -> "Available".equals(status.getDescription()))
    						.findAny()
    						.orElse(null).getId();

    		IN_USE = api.getItemCategoryById(CATEGORY_ID).getAssetStatuses().stream()
    						.filter(status -> "InOrder".equals(status.getDescription()))
    						.findAny()
    						.orElse(null).getId();
    		
    		finishedOrderStatus = api.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, api.getCallerCompanyId());

    		ACCOUNT_ID = envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();
    	});
    }

    @Test(priority=1)
	public void test001CheckAssetStatusAtOrderFinish() {
	
    final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
    testBuilder.given(envBuilder -> {
    	
    	final JbillingAPI api = envBuilder.getPrancingPonyApi();
    	final Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
    	
    	OrderLineWS lineWS = new OrderLineWS();
    	lineWS.setAmountAsDecimal(BigDecimal.ONE);
    	lineWS.setAssetIds(new Integer[]{ASSET_ID});
    	lineWS.setQuantityAsDecimal(BigDecimal.ONE);
    	lineWS.setItemId(PRODUCT_ID);
    	lineWS.setTypeId(CATEGORY_ID);
    	lineWS.setDescription("Test Asset Line");
    	
    	ORDER_ID = envBuilder.orderBuilder(api)
	  				.forUser(userId)
	  				.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
	  				.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
	  				.withActiveSince(activeSince)
	  				.withEffectiveDate(activeSince)
	  				.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
	  				.withDueDateValue(Integer.valueOf(1))
	  				.withCodeForTests("CR Order")
	  				.withOrderLine(lineWS)
	  				.withPeriod(environmentHelper.getOrderPeriodMonth(api))
	  				.build();
        
    	}).test((env)-> {
    	
    			JbillingAPI api = env.getPrancingPonyApi();
        
    			Integer userId = env.idForCode(CUSTOMER_CODE);
    			assertNotNull("Userid of customer should not be null",userId);
    			Integer customerId = api.getUserWS(userId).getCustomerId();
    			assertNotNull("CustomerId of customer should not be null",customerId);
        
    			OrderWS orderWS = api.getOrder(ORDER_ID);
    			logger.debug("Order Status Id: {}", orderWS.getStatusId());
    			assertEquals("Order status must me active .",ACTIVE_STATUS , orderWS.getStatusId());
    			assertEquals("Asset status must be available.", IN_USE, api.getAsset(ASSET_ID).getAssetStatusId());
    			
    			assertEquals("Order line id should be populated ", Integer.valueOf(api.getOrder(ORDER_ID).getOrderLines()[0].getId()) , api.getAsset(ASSET_ID).getOrderLineId());
    			
    			OrderStatusWS orderStatusWS = new OrderStatusWS();
                orderStatusWS.setId(finishedOrderStatus);
                orderStatusWS.setEntity(api.getCompany());
                orderStatusWS.setDescription("Finished");
                orderWS.setOrderStatusWS(orderStatusWS);
    			api.updateOrder(orderWS, null);
    			OrderWS orderWS2 = api.getOrder(orderWS.getId());
    			assertNull(api.getAsset(ASSET_ID).getOrderLineId());
    			logger.debug("Order Status Id: {}", orderWS2.getStatusId());
    			assertEquals("Order status must me finished .", finishedOrderStatus, orderWS2.getStatusId());
    			assertEquals("Asset status must be available.", AVAILABLE, api.getAsset(ASSET_ID).getAssetStatusId());
    			
    	});
    }

    private Integer createCustomer(TestEnvironmentBuilder envBuilder,String code, Integer accountTypeId, Date nid){
    	final JbillingAPI api = envBuilder.getPrancingPonyApi();

    	CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
    			.withUsername(code).withAccountTypeId(accountTypeId)
    			.withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), getDay(nid)));

    	UserWS user = customerBuilder.build();
    	user.setNextInvoiceDate(nid);
    	api.updateUser(user);
    	return user.getId();
    }

    private static Integer getDay(Date inputDate) {
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(inputDate);
    	return Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH));
    }
}
	