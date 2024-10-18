package com.sapienter.jbilling.server.user;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper.*;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateMidnight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;

/**
 * JBFC-684
 * Automation Test Coverage for Cancellation on Request Feature 
 *
 * @author Neelabh Dubey, 
 * @author Sampada Kulkarni
 * @since 26-DEC-2016
 */

@Test(groups = { "web-services", "customer" }, testName = "CancellationRequestAPITest")
public class CancellationRequestAPITest{

    private TestBuilder testBuilder;
    private EnvironmentHelper environmentHelper;

	private final String reasonText = "User has requested to cancel subscription orders";
	private static final String CATEGORY_CODE = "CRTestCategory";
    private static final String PRODUCT_CODE = "CRTestProduct";
    private static final String ACCOUNT_TYPE_CODE = "CRTestAccountType";
    private static final String CUSTOMER_CODE = "CRTestCustomer";
    
    
    private static final String CUSTOMER_CODE1 = "CRTestCustomer1";
    private static final String CUSTOMER_CODE2 = "CRTestCustomer2";
    private static final String CUSTOMER_CODE3 = "CRTestCustomer3";
    private static final String CUSTOMER_CODE4 = "CRTestCustomer4";
    
    
    private Integer CATEGORY_ID;
    private Integer PRODUCT_ID;
    private Integer ACCOUNT_ID;
       
    @BeforeClass
    public void initializeTests(){
        testBuilder = getTestEnvironment();
    }

    
    @AfterClass
    public void tearDown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        if (null != environmentHelper){
            environmentHelper = null;
        }
        if (null != testBuilder){
            testBuilder = null;
        }
    }
    
  
    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest().givenForMultiple(envCreator -> {
            final JbillingAPI api = envCreator.getPrancingPonyApi();
            environmentHelper = EnvironmentHelper.getInstance(api);
            
            CATEGORY_ID = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(true).build();
            PRODUCT_ID = envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(true).withType(CATEGORY_ID)
                    .withFlatPrice("0.50").build();
            ACCOUNT_ID = envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();
        });
    }

    
    @Test(priority = 1)
    public void test001CreateCancellationRequestWithSingleOrder(){
        
    	final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
        testBuilder.given(envBuilder -> {

        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
        assertNotNull("UserId should not be null",userId);
        
        envBuilder.orderBuilder(api)
        		  .forUser(userId)
        		  .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
        		  .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
        		  .withActiveSince(activeSince)
        		  .withEffectiveDate(activeSince)
        		  .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
        		  .withDueDateValue(Integer.valueOf(1))
        		  .withCodeForTests("CR Order")
        		  .withPeriod(environmentHelper.getOrderPeriodMonth(api))
        		  .build();
        
         }).test((env)-> {
            JbillingAPI api = env.getPrancingPonyApi();
            
            Integer userId = env.idForCode(CUSTOMER_CODE);
            assertNotNull("UserId should not be null",userId);
            
            Integer customerId = api.getUserWS(userId).getCustomerId();
            assertNotNull("CustomerId should not be null",customerId);
                      
            CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
            assertEquals("Cancellation date was not set correctly for customer",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
               
            Integer cancellationRequestId = api.createCancellationRequest(crWS);
            assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
            
            OrderWS[] orderWS = api.getUserSubscriptions(userId);
            assertEquals("Orders should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
            assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
            assertEquals("Order's active until date should be equal to cancellation date: ",
    					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
    					TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
            
            api.deleteCancellationRequest(cancellationRequestId);
        });
    }
  

    @Test(priority=2)
    public void test002CancellationRequestWithMultipleOrders(){
    	   	
    	final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
    	testBuilder.given(envBuilder -> {
    		
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            final Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
            assertNotNull("UserId should not be null",userId);
            
            envBuilder.orderBuilder(api)
            		  .forUser(userId)
            		  .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
            		  .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
            		  .withActiveSince(activeSince)
            		  .withEffectiveDate(activeSince)
            		  .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
            		  .withDueDateValue(Integer.valueOf(1))
            		  .withCodeForTests("CR Order")
            		  .withPeriod(environmentHelper.getOrderPeriodMonth(api))
            		  .build();
            
            envBuilder.orderBuilder(api)
	  				   .forUser(userId)
	  				   .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
	  				   .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
	  				   .withActiveSince(addDays(activeSince,5))
	  				   .withEffectiveDate(addDays(activeSince,5))
	  				   .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
	  				   .withDueDateValue(Integer.valueOf(1))
	  				   .withCodeForTests("CR Order")
	  				   .withPeriod(environmentHelper.getOrderPeriodMonth(api))
	  				   .build();
                
    		}).test((env)-> {
                       	
            	JbillingAPI api = env.getPrancingPonyApi();
            	            	
                Integer userId = env.idForCode(CUSTOMER_CODE);
                assertNotNull("UserId should not be null",userId);
                
                Integer customerId = api.getUserWS(userId).getCustomerId();
                assertNotNull("CustomerId should not be null",customerId);
                                               
                CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince,15),customerId,reasonText);
                assertEquals("Cancellation date is created correctly for customer",
    					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
    					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
                
                Integer cancellationRequestId = api.createCancellationRequest(crWS);
                assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
                                                              
                OrderWS[] ordersWS = api.getUserSubscriptions(userId);
                assertEquals("Number of subscription orders should be equal to 2.", 2, (ordersWS != null ? ordersWS.length : 0));
                                                
                for (OrderWS orderWS : ordersWS)
                {
                	assertNotNull("Order's active untill date should not be null after Cancellation Request", orderWS.getActiveUntil());
                	assertEquals("Order's active until date should be equal to cancellation date: ",
                			TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
                			TestConstants.DATE_FORMAT.format(orderWS.getActiveUntil()));	                        	
                }
            
                api.deleteCancellationRequest(cancellationRequestId);
    		});
    	}   
   	
    	@Test(priority=3)
    	public void test003DeleteCancellationRequest(){
    		    		  		
    		final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
            testBuilder.given(envBuilder -> {
            	
            	final JbillingAPI api = envBuilder.getPrancingPonyApi();
                final Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
                assertNotNull("UserId should not be null",userId);
                               
                envBuilder.orderBuilder(api)
            		  .forUser(userId)
            		  .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
            		  .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
            		  .withActiveSince(activeSince)
            		  .withEffectiveDate(activeSince)
            		  .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
            		  .withDueDateValue(Integer.valueOf(1))
            		  .withCodeForTests("CR Order")
            		  .withPeriod(environmentHelper.getOrderPeriodMonth(api))
            		  .build();
            
            }).test((env)-> {
            	
                JbillingAPI api = env.getPrancingPonyApi();
                
                Integer userId = env.idForCode(CUSTOMER_CODE);
                assertNotNull("UserId should not be null",userId );
                
                Integer customerId = api.getUserWS(userId).getCustomerId();
                assertNotNull("CustomerId should not be null",customerId );
                          
                CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
                assertEquals("Cancellation date did not set correctly for customer",
    					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
    					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
                
                Integer cancellationRequestId = api.createCancellationRequest(crWS);
                assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
                                
                CancellationRequestWS cancellationRequestWS = api.getCancellationRequestById(cancellationRequestId);
                assertNotNull("CancellationRequestWS should not be null", cancellationRequestWS);                
                
                OrderWS[] orderWS = api.getUserSubscriptions(userId);
                assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
                assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
                assertEquals("Order's active until date should be equal to cancellation date: ",
        					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
        					TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
                
                api.deleteCancellationRequest(cancellationRequestId);
                orderWS = api.getUserSubscriptions(userId);
                assertEquals("Order should remain 1 after deleting cancellation request.", 1, (orderWS != null ? orderWS.length : 0));
				assertNull("Order's active untill date should be null after deleting cancellation request",orderWS[0].getActiveUntil());
            });
    	}

		@Test(priority=4)
		public void test004getCancellationRequestForDateRange(){
			
			final Date activeSince1 = FullCreativeUtil.getDate(11,01,2016);
			final Date activeSince2 = addDays(activeSince1,10);
			final Date activeSince3 = addDays(activeSince1,15);
			final Date activeSince4 = addDays(activeSince1,20);
			
            testBuilder.given(envBuilder -> {
            	
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                                    
                final Integer userId1 = createCustomer(envBuilder,CUSTOMER_CODE1,envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince1);
                final Integer userId2 = createCustomer(envBuilder,CUSTOMER_CODE2,envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince2);
                final Integer userId3 = createCustomer(envBuilder,CUSTOMER_CODE3,envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince3);
                final Integer userId4 = createCustomer(envBuilder,CUSTOMER_CODE4,envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince4);
                                    
                envBuilder.orderBuilder(api)
      		  					.forUser(userId1)
      		  					.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
      		  					.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
      		  					.withActiveSince(activeSince1)
      		  					.withEffectiveDate(activeSince1)
      		  					.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
      		  					.withDueDateValue(Integer.valueOf(1))
      		  					.withCodeForTests("CR Order")
      		  					.withPeriod(environmentHelper.getOrderPeriodMonth(api))
      		  					.build();
      
                envBuilder.orderBuilder(api)
                				.forUser(userId2)
                				.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                				.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                				.withActiveSince(activeSince2)
                				.withEffectiveDate(activeSince2)
                				.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                				.withDueDateValue(Integer.valueOf(1))
                				.withCodeForTests("CR Order")
                				.withPeriod(environmentHelper.getOrderPeriodMonth(api))
                				.build();

                envBuilder.orderBuilder(api)
                				.forUser(userId3)
      		  					.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
      		  					.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
      		  					.withActiveSince(activeSince3)
      		  					.withEffectiveDate(activeSince3)
      		  					.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
      		  					.withDueDateValue(Integer.valueOf(1))
      		  					.withCodeForTests("CR Order")
      		  					.withPeriod(environmentHelper.getOrderPeriodMonth(api))
      		  					.build();
      	
                envBuilder.orderBuilder(api)
                				.forUser(userId4)
      		  					.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
      		  					.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
      		  					.withActiveSince(activeSince4)
      		  					.withEffectiveDate(activeSince4)
      		  					.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
      		  					.withDueDateValue(Integer.valueOf(1))
      		  					.withCodeForTests("CR Order")
      		  					.withPeriod(environmentHelper.getOrderPeriodMonth(api))
      		  					.build();
     		
            }).test((env)-> {
            	
                JbillingAPI api = env.getPrancingPonyApi();
                
                assertEquals("ActiveSince2 date is created correctly for customer",
    					TestConstants.DATE_FORMAT.format(addDays(activeSince1,10)),
    					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,11,2016)));
                   
                assertEquals("ActiveSince3 date is created correctly for customer",
    					TestConstants.DATE_FORMAT.format(addDays(activeSince1,15)),
    					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
                                       
                assertEquals("ActiveSince4 date is created correctly for customer",
    					TestConstants.DATE_FORMAT.format(addDays(activeSince1,20)),
    					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,21,2016)));
                
                Integer userId1 = env.idForCode(CUSTOMER_CODE1);
                assertNotNull("Userid1 of customer should not be null",userId1 );
                Integer customerId1 = api.getUserWS(userId1).getCustomerId();
                assertNotNull("Customerid1 of customer should not be null",customerId1 );

                Integer userId2 = env.idForCode(CUSTOMER_CODE2);
                assertNotNull("Userid2 of customer should not be null",userId2 );
                Integer customerId2 = api.getUserWS(userId2).getCustomerId();
                assertNotNull("Customerid of customer should not be null",customerId2 );
                
                Integer userId3 = env.idForCode(CUSTOMER_CODE3);
                assertNotNull("Userid3 of customer should not be null",userId3 );
                Integer customerId3 = api.getUserWS(userId3).getCustomerId();
                assertNotNull("Customerid3 of customer should not be null",customerId3 );
                
                Integer userId4 = env.idForCode(CUSTOMER_CODE4);
                assertNotNull("Userid4 of customer should not be null",userId4 );
                Integer customerId4 = api.getUserWS(userId4).getCustomerId();
                assertNotNull("Customerid4 of customer should not be null",customerId4 );
                   
                //Cancellation Request #1
                CancellationRequestWS crWS1 = constructCancellationRequestWS(activeSince1, customerId1, reasonText);
                Integer cancellationRequestId1 = api.createCancellationRequest(crWS1);
                assertNotNull("CancellationRequestId should not be null", cancellationRequestId1);
                
                OrderWS[] ordersWS1 = api.getUserSubscriptions(userId1);
                assertEquals("Order should be equal to 1.", 1, (ordersWS1 != null ? ordersWS1.length : 0));
                assertNotNull("Order's active until date should not be null", ordersWS1[0].getActiveUntil());
                assertEquals("Order's active until date should be equal to cancellation date: ",
        					TestConstants.DATE_FORMAT.format(crWS1.getCancellationDate()),
        					TestConstants.DATE_FORMAT.format(ordersWS1[0].getActiveUntil()));
                
                //Cancellation Request #2
                CancellationRequestWS crWS2 = constructCancellationRequestWS(activeSince2, customerId2, reasonText);
                Integer cancellationRequestId2 = api.createCancellationRequest(crWS2);
                assertNotNull("CancellationRequestId should not be null", cancellationRequestId2);
                
                OrderWS[] ordersWS2 = api.getUserSubscriptions(userId2);
                assertEquals("Order should be equal to 1.", 1, (ordersWS2 != null ? ordersWS2.length : 0));
                assertNotNull("Order's active until date should not be null", ordersWS2[0].getActiveUntil());
                assertEquals("Order's active until date should be equal to cancellation date: ",
        					TestConstants.DATE_FORMAT.format(crWS2.getCancellationDate()),
        					TestConstants.DATE_FORMAT.format(ordersWS2[0].getActiveUntil()));
                
                //Cancellation Request #3
                CancellationRequestWS crWS3 = constructCancellationRequestWS(activeSince3, customerId3, reasonText);
                Integer cancellationRequestId3 = api.createCancellationRequest(crWS3);
                assertNotNull("CancellationRequestId should not be null", cancellationRequestId3);
                
                OrderWS[] ordersWS3 = api.getUserSubscriptions(userId3);
                assertEquals("Order should be equal to 1.", 1, (ordersWS3 != null ? ordersWS3.length : 0));
                assertNotNull("Order's active until date should not be null", ordersWS3[0].getActiveUntil());
                assertEquals("Order's active until date should be equal to cancellation date: ",
        					TestConstants.DATE_FORMAT.format(crWS3.getCancellationDate()),
        					TestConstants.DATE_FORMAT.format(ordersWS3[0].getActiveUntil()));
                
                //Cancellation Request #4
                CancellationRequestWS crWS4 = constructCancellationRequestWS(activeSince4, customerId4, reasonText);
                Integer cancellationRequestId4 = api.createCancellationRequest(crWS4);
                assertNotNull("CancellationRequestId should not be null", cancellationRequestId4);
                
                OrderWS[] ordersWS4 = api.getUserSubscriptions(userId4);
                assertEquals("Order should be equal to 1.", 1, (ordersWS4 != null ? ordersWS4.length : 0));
                assertNotNull("Order's active until date should not be null", ordersWS4[0].getActiveUntil());
                assertEquals("Expected order's active until date should be equal to cancellation date: ",
        					TestConstants.DATE_FORMAT.format(crWS4.getCancellationDate()),
        					TestConstants.DATE_FORMAT.format(ordersWS4[0].getActiveUntil()));
                                                                          
                CancellationRequestWS[] canReqs = api.getAllCancellationRequests(1,activeSince1,new Date());
        
                assertEquals("Number of cancellation requests should be equal to 4", Integer.valueOf(4), Integer.valueOf(canReqs.length));
                        
	            api.deleteCancellationRequest(cancellationRequestId1);
	            api.deleteCancellationRequest(cancellationRequestId2);
	            api.deleteCancellationRequest(cancellationRequestId3);
	            api.deleteCancellationRequest(cancellationRequestId4);
            });
		}        
      
        @Test(priority=5)
    	public void test005UpdateCancellationRequest(){
    		
    		final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
            testBuilder.given(envBuilder -> {
            	
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                final Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
                               
                envBuilder.orderBuilder(api)
            		  .forUser(userId)
            		  .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
            		  .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
            		  .withActiveSince(activeSince)
            		  .withEffectiveDate(activeSince)
            		  .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
            		  .withDueDateValue(Integer.valueOf(1))
            		  .withCodeForTests("CR Order")
            		  .withPeriod(environmentHelper.getOrderPeriodMonth(api))
            		  .build();
            
            }).test((env)-> {
            	
                JbillingAPI api = env.getPrancingPonyApi();
                
                Integer userId = env.idForCode(CUSTOMER_CODE);
                assertNotNull("Userid of customer should not be null",userId );
                
                Integer customerId = api.getUserWS(userId).getCustomerId();
                assertNotNull("CustomerId of customer should not be null",customerId);
                          
                CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
                assertEquals("ActiveSince date is created correctly for customer",
    					TestConstants.DATE_FORMAT.format(addDays(activeSince,15)),
    					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
                                   
                Integer cancellationRequestId = api.createCancellationRequest(crWS);
                assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
                                                
                OrderWS[] orderWS = api.getUserSubscriptions(userId);
                assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
                assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
                assertEquals("Order's active until date should be equal to cancellation date: ",
        					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
        					TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
                
                CancellationRequestWS crWS1 = constructCancellationRequestWS(addDays(activeSince, 15), customerId, "UPDATED REQUEST");
                crWS1.setId(cancellationRequestId);
                assertEquals("UPDATED REQUEST",crWS1.getReasonText());               
                                              
                api.updateCancellationRequest(crWS1);
                assertEquals("UPDATED REQUEST",crWS1.getReasonText());
                
                orderWS = api.getUserSubscriptions(userId);
                assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
                assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
                assertEquals("Order's active until date should be equal to cancellation date: ",
    					TestConstants.DATE_FORMAT.format(crWS1.getCancellationDate()),
    					TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
                
                api.deleteCancellationRequest(cancellationRequestId);
            });
        }

        @Test(priority=6)
		public void test006getCancellationRequestById() {
		
        final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
        testBuilder.given(envBuilder -> {
        	
        	final JbillingAPI api = envBuilder.getPrancingPonyApi();
        	final Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
        	            			                
            envBuilder.orderBuilder(api)
  		  			.forUser(userId)
  		  			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
  		  			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
  		  			.withActiveSince(activeSince)
  		  			.withEffectiveDate(activeSince)
  		  			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
  		  			.withDueDateValue(Integer.valueOf(1))
  		  			.withCodeForTests("CR Order")
  		  			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
  		  			.build();
            
        	}).test((env)-> {
        	
        			JbillingAPI api = env.getPrancingPonyApi();
            
        			Integer userId = env.idForCode(CUSTOMER_CODE);
        			assertNotNull("Userid of customer should not be null",userId);
        			Integer customerId = api.getUserWS(userId).getCustomerId();
        			assertNotNull("CustomerId of customer should not be null",customerId);
            
        			OrderWS[] orderWS = api.getUserSubscriptions(userId);
					assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
        			assertNotNull("Order id should not be null", orderWS[0].getId());
					assertNull("Order active untill date should be null before cancellation",orderWS[0].getActiveUntil());
        			            			
        			CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
        			assertEquals("ActiveSince date was not set correctly for customer",
        					TestConstants.DATE_FORMAT.format(addDays(activeSince,15)),
        					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
                       
        			Integer cancellationRequestId = api.createCancellationRequest(crWS);
        			assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
                        			                                               
        			orderWS = api.getUserSubscriptions(userId);
					assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
        			assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
        			assertEquals("Order's active until date should be equal to cancellation date: ",
        						  TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
        						  TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
                                          
        			CancellationRequestWS cancellationRequestWS = api.getCancellationRequestById(cancellationRequestId);
        			assertEquals("Expected cancellation request id : ",cancellationRequestId,cancellationRequestWS.getId());
        			
        			api.deleteCancellationRequest(cancellationRequestId);
        	});
        }

        @Test(priority=7)
		public void test007getCancellationRequestByUserId() {
		
		final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
        testBuilder.given(envBuilder -> {
        	
        	final JbillingAPI api = envBuilder.getPrancingPonyApi();
        	final Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
        			                
            envBuilder.orderBuilder(api)
  		  			.forUser(userId)
  		  			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
  		  			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
  		  			.withActiveSince(activeSince)
  		  			.withEffectiveDate(activeSince)
  		  			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
  		  			.withDueDateValue(Integer.valueOf(1))
  		  			.withCodeForTests("CR Order")
  		  			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
  		  			.build();
            
        	}).test((env)-> {
        	
        			JbillingAPI api = env.getPrancingPonyApi();
            
        			Integer userId = env.idForCode(CUSTOMER_CODE);
        			assertNotNull("UserId of customer should not be null",userId);
        			Integer customerId = api.getUserWS(userId).getCustomerId();
        			assertNotNull("customerId of customer should not be null",customerId);
            
        			OrderWS[] orderWS = api.getUserSubscriptions(userId);
					assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
        			assertNotNull("Order id should not be null", orderWS[0].getId());
					assertNull("Order active untill date should be null before cancellation",orderWS[0].getActiveUntil());
        			            			
        			CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
        			assertEquals("Cancellation date was not set correctly for customer",
        					TestConstants.DATE_FORMAT.format(addDays(activeSince,15)),
        					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
        			
        			Integer cancellationRequestId = api.createCancellationRequest(crWS);
        			assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
        			                                                
        			orderWS = api.getUserSubscriptions(userId);
            
					assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
        			assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
        			assertEquals("Order's active until date should be equal to cancellation date: ",
        						  TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
        						  TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
                                          
        			CancellationRequestWS[] cancelByUser = api.getCancellationRequestsByUserId(userId);
        			assertNotNull("CancellationRequest by userId should not be null", cancelByUser);
        			assertEquals("Expected cancellation request id : ",cancellationRequestId,cancelByUser[0].getId());
        			
        			api.deleteCancellationRequest(cancellationRequestId);
        	});
        }

    @Test(priority = 8)
    public void test008GenerateInvoiceAfterCancellationRequest(){

		final Date activeSince = FullCreativeUtil.getDate(10,01,2016);
		final Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2016);
        testBuilder.given(envBuilder -> {

        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
        assertNotNull("UserId should not be null",userId);

        envBuilder.orderBuilder(api)
				  .forUser(userId)
				  .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
				  .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
				  .withActiveSince(activeSince)
				  .withEffectiveDate(activeSince)
				  .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
				  .withDueDateValue(Integer.valueOf(1))
				  .withCodeForTests("CR Order")
				  .withPeriod(environmentHelper.getOrderPeriodMonth(api))
				  .build();

         }).test((env)-> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer cancellationRequestId = null;
            Integer invoiceId = null;

            try {
				Integer userId = env.idForCode(CUSTOMER_CODE);
				assertNotNull("UserId should not be null",userId);
	            Integer customerId = api.getUserWS(userId).getCustomerId();
	            assertNotNull("CustomerId should not be null",customerId);

	            CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
	            assertEquals("Cancellation date was not set correctly for customer.",
						TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
						TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(10,16,2016)));

	            cancellationRequestId = api.createCancellationRequest(crWS);
	            assertNotNull("CancellationRequestId should not be null", cancellationRequestId);

	            OrderWS[] orderWS = api.getUserSubscriptions(userId);
	            assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
	            assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
	            assertEquals("Order's active until date should be equal to cancellation date: ",
							TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
							TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));

	            //Generating invoice
	            Date billingDate = new DateMidnight(2016, 12, 1).toDate();
	            Integer[] invoiceIds = api.createInvoiceWithDate(userId,billingDate, PeriodUnitDTO.MONTH, 21, false);
	            assertEquals("Generated invoice should be equal to 1.", 1, (invoiceIds != null ? invoiceIds.length : 0));

	            invoiceId = invoiceIds[0];
	            InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
	            assertNotNull("InvoiceWS should not be null", invoiceWS);

	            InvoiceLineDTO[] invoiceLinesDTOs = invoiceWS.getInvoiceLines();
	            assertEquals("There should be ONE InvoiceLine", 1, (invoiceLinesDTOs != null ? invoiceLinesDTOs.length : 0));

	            InvoiceLineDTO invoiceLinesDTO = invoiceLinesDTOs[0];
	            assertEquals("Invoice line quantity should be equal to 1", "1.0000000000", invoiceLinesDTO.getQuantity());

	            int index = invoiceLinesDTOs[0].getDescription().indexOf("from");
	            String invLineDescTruncated = invoiceLinesDTOs[0].getDescription().substring(index);
	            assertEquals("Invoice line period did not match.", "from 11/01/2016 to 11/16/2016", invLineDescTruncated);

            } finally {
				//Cleanup
                if (invoiceId != null) {
					api.deleteInvoice(invoiceId);
                }
                if (cancellationRequestId != null) {
					api.deleteCancellationRequest(cancellationRequestId);
                }
            }
        });
    }

    /**
     * JBFC-724
     * Create Cancellation Request without Any Orders for the Customer.
     */
    @Test(priority = 9)
    public void test009CreateCancellationRequestWithOutAnyOrder(){

    	final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
        testBuilder.given(envBuilder -> {

        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
        assertNotNull("UserId should not be null",userId);
        
         }).test((env)-> {
            JbillingAPI api = env.getPrancingPonyApi();
            
            Integer userId = env.idForCode(CUSTOMER_CODE);
            assertNotNull("UserId should not be null",userId);
            
            Integer customerId = api.getUserWS(userId).getCustomerId();
            assertNotNull("CustomerId should not be null",customerId);
                      
            CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
            assertEquals("Cancellation date was not set correctly for customer",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
            Integer cancellationRequestId = null;
            try{
            cancellationRequestId = api.createCancellationRequest(crWS);
            assertNull("CancellationRequestId should be null", cancellationRequestId);
            } catch (SessionInternalError e) {
                assertTrue("Should Throw SessionInternalError as no active subscription orders are there for the Customer",e.getMessage().contains("No Active Subscription Orders for the Customer"));
            } finally {
				//Cleanup
                if (cancellationRequestId != null) {
					api.deleteCancellationRequest(cancellationRequestId);
                }
            }
        });
    }

    /**
     * JBFC-724
     * Create Cancellation Request when the customer has an existing pending Cancellation Request.
     */
    @Test(priority = 10)
    public void test010CreateAnotherCancellationRequestWhenOneCancellationRequestIsPresent(){
        
    	final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
        testBuilder.given(envBuilder -> {

        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
        assertNotNull("UserId should not be null",userId);
        
        envBuilder.orderBuilder(api)
        		  .forUser(userId)
        		  .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
        		  .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
        		  .withActiveSince(activeSince)
        		  .withEffectiveDate(activeSince)
        		  .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
        		  .withDueDateValue(Integer.valueOf(1))
        		  .withCodeForTests("CR Order")
        		  .withPeriod(environmentHelper.getOrderPeriodMonth(api))
        		  .build();
        
         }).test((env)-> {
            JbillingAPI api = env.getPrancingPonyApi();
            
            Integer userId = env.idForCode(CUSTOMER_CODE);
            assertNotNull("UserId should not be null",userId);
            
            Integer customerId = api.getUserWS(userId).getCustomerId();
            assertNotNull("CustomerId should not be null",customerId);
                      
            CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
            assertEquals("Cancellation date was not set correctly for customer",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
               
            Integer cancellationRequestId = api.createCancellationRequest(crWS);
            assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
            
            OrderWS[] orderWS = api.getUserSubscriptions(userId);
            assertEquals("Orders should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
            assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
            assertEquals("Order's active until date should be equal to cancellation date: ",
    					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
    					TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
            
            Integer cancellationRequestId2 = null;
            try{
            	cancellationRequestId2 = api.createCancellationRequest(crWS);
            assertNull("CancellationRequestId should be null", cancellationRequestId2);
            } catch (SessionInternalError e) {
                assertTrue("Should Throw SessionInternalError as Cancellation Request is already in Pending State.",e.getMessage().contains("Cancellation Request is already in Pending State"));
            } finally {
				//Cleanup
            	if (cancellationRequestId != null) {
					api.deleteCancellationRequest(cancellationRequestId);
                }
                if (cancellationRequestId2 != null) {
					api.deleteCancellationRequest(cancellationRequestId2);
                }
            }
        });
    }
  
    /**
     * Create Cancellation Request with cancellation date lower than Last Invoice Date for the User
     */
    @Test(priority = 11)
    public void test011CancellationRequestLowerThanInvoiceDate(){

		final Date activeSince = FullCreativeUtil.getDate(10,01,2016);
		final Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2016);
        testBuilder.given(envBuilder -> {

        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
        assertNotNull("UserId should not be null",userId);

        envBuilder.orderBuilder(api)
				  .forUser(userId)
				  .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
				  .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
				  .withActiveSince(activeSince)
				  .withEffectiveDate(activeSince)
				  .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
				  .withDueDateValue(Integer.valueOf(1))
				  .withCodeForTests("CR Order")
				  .withPeriod(environmentHelper.getOrderPeriodMonth(api))
				  .build();

         }).test((env)-> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer cancellationRequestId = null;
            Integer invoiceId = null;

            try {
				Integer userId = env.idForCode(CUSTOMER_CODE);
				assertNotNull("UserId should not be null",userId);
	            Integer customerId = api.getUserWS(userId).getCustomerId();
	            assertNotNull("CustomerId should not be null",customerId);

	            CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
	            assertEquals("Cancellation date was not set correctly for customer.",
						TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
						TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(10,16,2016)));

	            OrderWS[] orderWS = api.getUserSubscriptions(userId);
	            assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
	            
	            //Generating invoice
	            Date billingDate = new DateMidnight(2016, 12, 1).toDate();
	            Integer[] invoiceIds = api.createInvoiceWithDate(userId,billingDate, PeriodUnitDTO.MONTH, 21, false);
	            assertEquals("Generated invoice should be equal to 1.", 1, (invoiceIds != null ? invoiceIds.length : 0));

	            invoiceId = invoiceIds[0];
	            InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
	            assertNotNull("InvoiceWS should not be null", invoiceWS);

	            InvoiceLineDTO[] invoiceLinesDTOs = invoiceWS.getInvoiceLines();
	            assertEquals("There should be ONE InvoiceLine", 1, (invoiceLinesDTOs != null ? invoiceLinesDTOs.length : 0));

	            InvoiceLineDTO invoiceLinesDTO = invoiceLinesDTOs[0];
	            assertEquals("Invoice line quantity should be equal to 1", "1.0000000000", invoiceLinesDTO.getQuantity());

	            int index = invoiceLinesDTOs[0].getDescription().indexOf("from");
	            String invLineDescTruncated = invoiceLinesDTOs[0].getDescription().substring(index);
	            assertEquals("Invoice line period did not match.", "from 11/01/2016 to 11/30/2016", invLineDescTruncated);
	            
	            cancellationRequestId = api.createCancellationRequest(crWS);
	            assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
	            assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
	            assertEquals("Order's active until date should be equal to cancellation date: ",
	    					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
	    					TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
            } catch (SessionInternalError e) {
                assertTrue("Should Throw SessionInternalError as Cancellation Request Date should be higher than Last Invoice Date",e.getMessage().contains("Cancellation Request Date should be higher than Last Invoice Date"));
            } finally {
				//Cleanup
                if (invoiceId != null) {
					api.deleteInvoice(invoiceId);
                }
                if (cancellationRequestId != null) {
					api.deleteCancellationRequest(cancellationRequestId);
                }
            }
        });
    }
    
    /**
     * Create Cancellation Request with cancellation date higher than Last Invoice Date for the User
     */
    @Test(priority = 12)
    public void test012CancellationRequestHigherThanInvoiceDate(){

		final Date activeSince = FullCreativeUtil.getDate(10,01,2016);
		final Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2016);
        testBuilder.given(envBuilder -> {

        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
        assertNotNull("UserId should not be null",userId);

        envBuilder.orderBuilder(api)
				  .forUser(userId)
				  .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
				  .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
				  .withActiveSince(activeSince)
				  .withEffectiveDate(activeSince)
				  .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
				  .withDueDateValue(Integer.valueOf(1))
				  .withCodeForTests("CR Order")
				  .withPeriod(environmentHelper.getOrderPeriodMonth(api))
				  .build();

         }).test((env)-> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer cancellationRequestId = null;
            Integer invoiceId = null;

            try {
				Integer userId = env.idForCode(CUSTOMER_CODE);
				assertNotNull("UserId should not be null",userId);
	            Integer customerId = api.getUserWS(userId).getCustomerId();
	            assertNotNull("CustomerId should not be null",customerId);

	            CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 45), customerId, reasonText);
	            assertEquals("Cancellation date was not set correctly for customer.",
						TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
						TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));

	            OrderWS[] orderWS = api.getUserSubscriptions(userId);
	            assertEquals("Order should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
	            
	            //Generating invoice
	            Date billingDate = new DateMidnight(2016, 12, 1).toDate();
	            Integer[] invoiceIds = api.createInvoiceWithDate(userId,billingDate, PeriodUnitDTO.MONTH, 21, false);
	            assertEquals("Generated invoice should be equal to 1.", 1, (invoiceIds != null ? invoiceIds.length : 0));

	            invoiceId = invoiceIds[0];
	            InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
	            assertNotNull("InvoiceWS should not be null", invoiceWS);

	            InvoiceLineDTO[] invoiceLinesDTOs = invoiceWS.getInvoiceLines();
	            assertEquals("There should be ONE InvoiceLine", 1, (invoiceLinesDTOs != null ? invoiceLinesDTOs.length : 0));

	            InvoiceLineDTO invoiceLinesDTO = invoiceLinesDTOs[0];
	            assertEquals("Invoice line quantity should be equal to 1", "1.0000000000", invoiceLinesDTO.getQuantity());

	            int index = invoiceLinesDTOs[0].getDescription().indexOf("from");
	            String invLineDescTruncated = invoiceLinesDTOs[0].getDescription().substring(index);
	            assertEquals("Invoice line period did not match.", "from 11/01/2016 to 11/30/2016", invLineDescTruncated);
	            
	            cancellationRequestId = api.createCancellationRequest(crWS);
	            assertNotNull("CancellationRequestId should not be null", cancellationRequestId);
            } finally {
				//Cleanup
                if (invoiceId != null) {
					api.deleteInvoice(invoiceId);
                }
                if (cancellationRequestId != null) {
					api.deleteCancellationRequest(cancellationRequestId);
                }
            }
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
