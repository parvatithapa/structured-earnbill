/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.fc.mediation;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationRatingSchemeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.JBillingTestUtils;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;

@Test(groups = { "fullcreative" }, testName = "FullCreativeMediationTest")
public class FullCreativeMediationTest {

	private static final Logger logger = LoggerFactory.getLogger(FullCreativeMediationTest.class);
    private static String INBOUND_MEDIATION_LAUNCHER;
    private static String ACTIVE_MEDIATION_LAUNCHER;
    private static String CHAT_MEDIATION_LAUNCHER;
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private JbillingAPI api;
    private JbillingAPI rootApi;
    private int product8XXTollFreeId;
    private int productchatAccountId;
    private int productActiveResponseAccountId;
    private Integer afbestvaluePlanId;
    private Integer basicItemManagerPlugInId;
    private Integer ratingSchemeId;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        rootApi = JbillingAPIFactory.getAPI("apiClient");
        product8XXTollFreeId = FullCreativeTestConstants.TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID;
        productchatAccountId = FullCreativeTestConstants.CHAT_ACCOUNT_ASSET_PRODUCT_ID;
        productActiveResponseAccountId = FullCreativeTestConstants.ACTIVE_RESPONSE_ACCOUNT_ASSET_PRODUCT_ID;
        afbestvaluePlanId = FullCreativeTestConstants.AF_BEST_VALUE_PLAN_ID;
        INBOUND_MEDIATION_LAUNCHER = FullCreativeTestConstants.INBOUND_MEDIATION_LAUNCHER;
        ACTIVE_MEDIATION_LAUNCHER = FullCreativeTestConstants.ACTIVE_MEDIATION_LAUNCHER;
        CHAT_MEDIATION_LAUNCHER = FullCreativeTestConstants.CHAT_MEDIATION_LAUNCHER;
        basicItemManagerPlugInId = FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID;
        FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);

        logger.debug("Creating Global Rating Scheme...");
        MediationRatingSchemeWS ratingScheme = FullCreativeUtil.getRatingScheme("Prancing Pony Rating Scheme");
            logger.debug("Creating Rating Scheme with initial increment 30 and main increment 6.");
            if(api.getRatingSchemesForEntity().length==0) { 
            	ratingSchemeId =  api.createRatingScheme(ratingScheme);
            	assertNotNull("Rating Scheme should not be null",ratingSchemeId);
            	logger.debug("Rating Scheme created with Id : {}", ratingSchemeId);
            } else {
            	ratingSchemeId = -1;
            }
    }
    
    /**
     *  User Created through testcase and take uppermost Available asset of product by product id.
     *  Assets
     *  ID 		Identifier
	 *	10568  	1215956534  - Active Response
	 *	10566  	8774009503  - Chat
	 *	6916  	8442896773  - Inbound Calls
     * 
     */

    @Test
    public void test01InboundCallsMediationTest() throws Exception {
    	
    	// setup a BasicFilter which will be used to filter assets on Available status
		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(1);
		criteria.setOffset(0);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});
		
		// get an available asset's id for plan subscription item (id = 603)
		AssetSearchResult assetsResult603 = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
		assertNotNull("No available asset found for product "+product8XXTollFreeId, assetsResult603);
		AssetWS[] available603Assets = assetsResult603.getObjects();
		assertTrue("No assets found for product 603.", null != available603Assets && available603Assets.length != 0);
		Integer assetIdProduct603 = available603Assets[0].getId();
		logger.debug("Asset Available for product {} = {}", product8XXTollFreeId, assetIdProduct603);
		
		// get an available asset's id for product id = 606
		AssetSearchResult assetsResult606 = api.findProductAssetsByStatus(productchatAccountId, criteria);
		assertNotNull("No available asset found for product "+productchatAccountId, assetsResult606);
		AssetWS[] available606Assets = assetsResult606.getObjects();
		assertTrue("No assets found for product 606.", null != available606Assets && available606Assets.length != 0);
		Integer assetIdProduct606 = available606Assets[0].getId();
		logger.debug("Asset Available for product {} = {}", productchatAccountId, assetIdProduct606);
		
		// get an available asset's id for product id = 607
		AssetSearchResult assetsResult607 = api.findProductAssetsByStatus(productActiveResponseAccountId, criteria);
		assertNotNull("No available asset found for product "+productActiveResponseAccountId, assetsResult607);
		AssetWS[] available607Assets = assetsResult607.getObjects();
		assertTrue("No assets found for product 607.", null != available607Assets && available607Assets.length != 0);
		Integer assetIdProduct607 = available607Assets[0].getId();
		logger.debug("Asset Available for product {} = {}", productActiveResponseAccountId, assetIdProduct607);
    			
		logger.debug("Creating user...");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		UserWS user = FullCreativeUtil.createUser(calendar.getTime());
		logger.debug("User created : {}", user.getId());
		
		logger.debug("Creating order...");
		OrderWS order = new OrderWS();
		order.setUserId(user.getId());
                order.setActiveSince(new Date());
                order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
                order.setPeriod(new Integer(2)); // monthly
		order.setCurrencyId(new Integer(1));
		
		PlanWS planWS = api.getPlanWS(afbestvaluePlanId); // AF Best Value Plan
		
		OrderLineWS line1 = new OrderLineWS();
		line1.setItemId(planWS.getItemId());
		line1.setAmount("225.00");
		line1.setPrice("225.00");
		line1.setTypeId(Integer.valueOf(1));
		line1.setDescription("AF Best Value Plan");
		line1.setQuantity("1");
		line1.setUseItem(true);
		
		OrderLineWS line2 = new OrderLineWS();
		line2.setItemId(Integer.valueOf(productActiveResponseAccountId));
		line2.setDescription("Active Response Account");
		line2.setQuantity("1.0000000000");
		line2.setAmount("0.00");
		line2.setTypeId(Integer.valueOf(1));
		line2.setPrice("0.00");
		line2.setAssetIds(new Integer[]{
				Integer.valueOf(assetIdProduct607)
		});
	
		OrderLineWS line3 = new OrderLineWS();
		line3.setItemId(Integer.valueOf(productchatAccountId));
		line3.setDescription("Chat Account");
		line3.setQuantity("1.0000000000");
		line3.setAmount("0.00");
		line3.setTypeId(Integer.valueOf(1));
		line3.setPrice("0.00");
		line3.setAssetIds(new Integer[]{
				Integer.valueOf(assetIdProduct606)
		});

                OrderLineWS line4 = new OrderLineWS();
                line4.setItemId(Integer.valueOf(product8XXTollFreeId));
                line4.setDescription("Inbound");
                line4.setQuantity("1.0000000000");
                line4.setAmount("0.00");
                line4.setTypeId(Integer.valueOf(1));
                line4.setPrice("0.00");
                line4.setAssetIds(new Integer[]{
                        Integer.valueOf(assetIdProduct603)
                });
		
		order.setOrderLines(new OrderLineWS[]{line1, line2, line3});
		
		//Setting up the asset with the plan
		OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		for (OrderChangeWS ws : orderChanges) {
			if (ws.getItemId().intValue() == FullCreativeTestConstants.AF_BEST_VALUE_PLAN_SUBSCRIPTION_ID) {
				
				OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                orderChangePlanItem.setItemId(product8XXTollFreeId);
                orderChangePlanItem.setId(0);
                orderChangePlanItem.setOptlock(0);
                orderChangePlanItem.setBundledQuantity(1);
                orderChangePlanItem.setDescription("DID-8XX");
                orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);
                
                orderChangePlanItem.setAssetIds(new int[]{assetIdProduct603});
				
                ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
			}
		}
		
		Integer orderId	= api.createOrder(order, orderChanges);
		logger.debug("Order {} created with asset", orderId);
		
		
		UserWS userWs = api.getUserWS(user.getId());
		String localResourceDir = Util.getSysProp("base_dir") + "mediation/data/";
		File aFile=new File(localResourceDir+FullCreativeTestConstants.INBOUND_FILE_NAME);
		logger.debug("ACMediationTest  ############## File Name is:::::: {}", aFile);
		UUID mediationProcessId = api.triggerMediationByConfigurationByFile(getMediationConfiguration(INBOUND_MEDIATION_LAUNCHER), aFile);
		logger.debug("mediationProcessId is:::::::::::: {}", mediationProcessId);
		FullCreativeUtil.waitForMediationComplete(api, 90 * 90 * 100);
	        
	        ProcessStatusWS processStatus = api.getMediationProcessStatus();
	        // check previous calls
	        assertNotNull("Mediation should be triggered at first time!", mediationProcessId);
	        // todo: possible fail if incorrect mediation configuration or 'quick' machines
	        // in common case 2d-4d calls to api should be parall to processing first call
	        // assertTrue("In common case first process should be in running state. Try again on your machine or comment assert statement", isProcessing);
	        logger.debug("ProcessStatus {}", processStatus);
	        assertNotNull("Status should be retrieved!", processStatus);
	        assertNotNull("Start date should be presented", processStatus.getStart());

	        logger.debug("After Wait mediation is running : {} ", api.isMediationProcessRunning());
	        ProcessStatusWS completedStatus = api.getMediationProcessStatus();
	        logger.debug("Completed status : {}", completedStatus);
	        assertNotNull("Status should be retrieved", completedStatus);
	        if (completedStatus.getMediationProcessId().equals(mediationProcessId)) { // Inbound test
	          //  assertEquals("Inbound calls test has error records, status should be Finished", ProcessStatusWS.State.FINISHED, completedStatus1.getState());
	            logger.debug("Status : {}", completedStatus);
	        }
	        
	        MediationProcess process = api.getMediationProcess(completedStatus.getMediationProcessId());
	        assertNotNull("Mediation process should be retrieved", process);
	        assertEquals("Mediation process should be filled", process.getId(), completedStatus.getMediationProcessId());
	        assertEquals("Done and Billable records", new Integer(5), process.getDoneAndBillable());
	        assertEquals("Error Detected records", new Integer(0), process.getErrors());
	        assertEquals("Total CDR records process", new Integer(5), process.getRecordsProcessed());
	        
	        List<Integer> orderIds = Arrays.asList(process.getOrderIds());
	        assertEquals("Total Order Created By Process ", new Integer(1), process.getOrderAffectedCount());
	        assertNotNull("Order Created through mediation", orderIds);
	        Integer orId = orderIds.get(0);
	        OrderWS usageOrder = api.getOrder(orId);
	        assertTrue("Mediated Order Should have isMediated Flag checked: ", usageOrder.getIsMediated());
            quantityInOrderLines(usageOrder, "7.2000000000");
    }

    private void quantityInOrderLines(OrderWS usageOrder, String quantity) {
        boolean foundQuantity = false;
        for (OrderLineWS orderLine: usageOrder.getOrderLines()) {
            if (quantity.equals(orderLine.getQuantity()))
                foundQuantity = true;
        }
        assertTrue("Expected Order line Quantity: " + quantity, foundQuantity);
    }

    @Test
    public void test02ActiveResponseMediationTest() throws Exception {

        logger.debug("testTrigger-Active");
        String localResourceDir = Util.getSysProp("base_dir") + "mediation/data/";
        File aFile=new File(localResourceDir+FullCreativeTestConstants.ACTIVE_RESPONSE_FILE_NAME);
        logger.debug("ACMediationTest  ############## File Name is:::::: {}", aFile);
        Integer ACTIVE_CFG_ID =  getMediationConfiguration(ACTIVE_MEDIATION_LAUNCHER);
        UUID processId = api.triggerMediationByConfigurationByFile(ACTIVE_CFG_ID,aFile);

        // check previous calls
        assertNotNull("Mediation should be triggered at first time!", processId);
        // todo: possible fail if incorrect mediation configuration or 'quick' machines
        // in common case 2d-4d calls to api should be parall to processing first call
        // assertTrue("In common case first process should be in running state. Try again on your machine or comment assert statement", isProcessing);
        
        FullCreativeUtil.waitForMediationComplete(api, 70 * 70 * 100);
        
        ProcessStatusWS processStatus = api.getMediationProcessStatus();
        assertNotNull("Status should be retrieved!", processStatus);
        assertNotNull("Start date should be presented", processStatus.getStart());


        ProcessStatusWS completedStatus = api.getMediationProcessStatus();
        assertNotNull("Status should be retrieved", completedStatus);
        if (completedStatus.getMediationProcessId().equals(processId)) { // Inbound test
          //  assertEquals("Inbound calls test has error records, status should be Finished", ProcessStatusWS.State.FINISHED, completedStatus1.getState());
            logger.debug("Mediation Status : {}", completedStatus);
        }
        
        MediationProcess process = api.getMediationProcess(completedStatus.getMediationProcessId());
        assertNotNull("Mediation process should be retrieved", process);
        assertEquals("Mediation process should be filled", process.getId(), completedStatus.getMediationProcessId());
        assertEquals("Done and Billable records", new Integer(5), process.getDoneAndBillable());
        assertEquals("Error Detected records", new Integer(0), process.getErrors());
        assertEquals("Total CDR records process", new Integer(5), process.getRecordsProcessed());
        
        List<Integer> orderIds = Arrays.asList(process.getOrderIds());
        assertNotNull("Order Created through mediation", orderIds.size());
        assertEquals("Total Order Created By Process ", new Integer(1), process.getOrderAffectedCount());
        Integer orId = orderIds.get(0);
        OrderWS usageOrder = api.getOrder(orId);
        assertTrue("Mediated Order Should have isMediated Flag checked: ", usageOrder.getIsMediated());
        quantityInOrderLines(usageOrder, "9.5000000000");
    }

    @Test
    public void test03ChatMediationTest() throws Exception {

        logger.debug("testTrigger-Chat");
        String localResourceDir = Util.getSysProp("base_dir") + "mediation/data/";
        File cFile=new File(localResourceDir+FullCreativeTestConstants.CHAT_FILE_NAME);
        logger.debug("ACMediationTest  ############## File Name is:::::: {}", cFile);
        Integer CHAT_CFG_ID =  getMediationConfiguration(CHAT_MEDIATION_LAUNCHER);
        UUID processId = api.triggerMediationByConfigurationByFile(CHAT_CFG_ID,cFile);

        // check previous calls
        
        assertNotNull("Mediation should be triggered at first time!", processId);
        
        FullCreativeUtil.waitForMediationComplete(api, 70 * 70 * 100);
        
        ProcessStatusWS status = api.getMediationProcessStatus();
        // todo: possible fail if incorrect mediation configuration or 'quick' machines
        // in common case 2d-4d calls to api should be parall to processing first call
        // assertTrue("In common case first process should be in running state. Try again on your machine or comment assert statement", isProcessing);
        assertNotNull("Status should be retrieved!", status);
        assertNotNull("Start date should be presented", status.getStart());

        ProcessStatusWS completedStatus = rootApi.getMediationProcessStatus();
        assertNotNull("Status should be retrieved", completedStatus);
        if (completedStatus.getMediationProcessId().equals(processId)) { // Inbound test
           // assertEquals("Inbound calls test has error records, status should be Finished", ProcessStatusWS.State.FINISHED, completedStatus2.getState());
            logger.debug("Mediation Status : ", completedStatus);
        }
        
        MediationProcess process = api.getMediationProcess(completedStatus.getMediationProcessId());
        assertNotNull("Mediation process should be retrieved", process);
        assertEquals("Mediation process should be filled", process.getId(), completedStatus.getMediationProcessId());
        assertEquals("Done and Billable records", new Integer(5), process.getDoneAndBillable());
        assertEquals("Error Detected records", new Integer(0), process.getErrors());
        assertEquals("Total CDR records process", new Integer(5), process.getRecordsProcessed());
        
        List<Integer> orderIds = Arrays.asList(process.getOrderIds());
        assertEquals("Total Order Created By Process ", new Integer(1), process.getOrderAffectedCount());
        assertNotNull("Order Created through mediation", orderIds.size());
        Integer orId1 = orderIds.get(0);
        OrderWS usageOrder = api.getOrder(orId1);
        assertTrue("Mediated Order Should have isMediated Flag checked: ", usageOrder.getIsMediated());
        quantityInOrderLines(usageOrder, "115.4000000000");
    }

    @Test
    public void test04getMediationRecordsByMediationProcessAndStatusWithNullProcessId() {
	try {
    	List<JbillingMediationRecord> mediationRecordListByProcess = Arrays.asList(api.getMediationRecordsByMediationProcessAndStatus(null,null));
    	fail("Exception expected");
	} catch(com.sapienter.jbilling.common.SessionInternalError  error) {
	    JBillingTestUtils.assertContainsError(error,  "Mediation Process ID Should Not Be NULL.");
	}
    }
    
    @Test
    public void test05getMediationRecordsByMediationProcessAndStatusWithRandomProcessId() throws Exception {
	UUID processId = UUID.randomUUID();
	List<JbillingMediationRecord> mediationRecordListByProcess = Arrays.asList(api.getMediationRecordsByMediationProcessAndStatus(processId.toString(),null));
        logger.debug("Total mediated records in this mediation process : {}", mediationRecordListByProcess.size());
        assertEquals("Total mediated records . ", 0, mediationRecordListByProcess.size());
    }
    
    @Test
    public void test06getErrorsByMediationProcessWithNullProcessId() throws Exception {
	try {
	List<JbillingMediationErrorRecord> errorRecordListByProcess = Arrays.asList(api.getErrorsByMediationProcess(null, 0, 0));
	fail("Exception expected");
	} catch(com.sapienter.jbilling.common.SessionInternalError  error) {
	    JBillingTestUtils.assertContainsError(error,  "Mediation Process ID Should Not Be NULL.");
	}
    }
    
    @Test
    public void test07getErrorsByMediationProcessWithRandomProcessId() throws Exception {
	UUID processId = UUID.randomUUID();
	List<JbillingMediationErrorRecord> errorRecordListByProcess = Arrays.asList(api.getErrorsByMediationProcess(processId.toString(),0,0));
        logger.debug("Total Error records in this mediation process : {}", errorRecordListByProcess.size());
        assertEquals("Total mediated records . ", 0, errorRecordListByProcess.size());
    }
    
    @Test
    public void test08InboundCallsMediationWithZeroEvaluatedQuantity() throws Exception {
		// Setup a BasicFilter which will be used to filter assets on Available status
		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(1);
		criteria.setOffset(0);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});

		// Get an available asset's id for plan subscription item (id = 603)
		AssetSearchResult assetsResult603 = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
		assertNotNull("No available asset found for product "+product8XXTollFreeId, assetsResult603);
		AssetWS[] available603Assets = assetsResult603.getObjects();
		assertTrue("No assets found for product 603.", null != available603Assets && available603Assets.length != 0);

		// Creating a new asset to avoid conflict as the next available asset is being used in subsequent test class & in CDR file 
		AssetWS createdAssetWS = createAsset("1000000001", product8XXTollFreeId, available603Assets[0].getAssetStatusId());
		Integer assetIdProduct603 = createdAssetWS.getId();
		logger.debug("Asset Available for product {} = {}", product8XXTollFreeId, assetIdProduct603);

		logger.debug("Creating user...");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2014);
		calendar.set(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		UserWS user = FullCreativeUtil.createUser(calendar.getTime());
		logger.debug("User created : {}", user.getId());

		logger.debug("Creating monthly order...");
		OrderWS order = new OrderWS();
		order.setUserId(user.getId());
                order.setActiveSince(new Date());
                order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
                order.setPeriod(new Integer(2));
		order.setCurrencyId(new Integer(1));

        OrderLineWS line1 = new OrderLineWS();
        line1.setItemId(Integer.valueOf(product8XXTollFreeId));
        line1.setDescription("Inbound");
        line1.setQuantity("1.0000000000");
        line1.setAmount("0.50");
        line1.setTypeId(Integer.valueOf(1));
        line1.setPrice("0.50");
        line1.setAssetIds(new Integer[]{
                Integer.valueOf(assetIdProduct603)
        });
		order.setOrderLines(new OrderLineWS[]{line1});

		//Creating order and setting up rating scheme with half round up
		OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		Integer orderId	= api.createOrder(order, orderChanges);
		logger.debug("Order {} created with asset", orderId);

		try {
			if(ratingSchemeId != -1) {
				api.deleteRatingScheme(ratingSchemeId);
	            if(api.getRatingSchemesForEntity().length == 0) {
					MediationRatingSchemeWS ratingSchemeWithHalfRoundUp =
							FullCreativeUtil.getRatingSchemeWithHalfRoundUp("Prancing Pony Half Round Up Rating Scheme");
					ratingSchemeId =  api.createRatingScheme(ratingSchemeWithHalfRoundUp);
				} else {
					ratingSchemeId = -1;
	            }
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Rating Scheme deletion & creation failed.");
		}

		//Constructing CDR
		List<String> quantities = Arrays.asList("29","31","20","5","0");
		List<String> inboundCdrs = buildInboundCDR(createdAssetWS.getIdentifier(), quantities, "09/29/2014");

		//Triggering mediation to process the CDR
		UUID mediationProcessId = api.processCDR(getMediationConfiguration(INBOUND_MEDIATION_LAUNCHER), inboundCdrs);
		logger.debug("mediationProcessId is:::::::::::: {}", mediationProcessId);
		FullCreativeUtil.waitForMediationComplete(api, 90 * 90 * 100);
		assertNotNull("Mediation should be triggered at first time!", mediationProcessId);

        MediationProcess process = api.getMediationProcess(mediationProcessId);
        assertNotNull("Mediation process should be retrieved", process);
        assertEquals("Done and Billable records", new Integer(1), process.getDoneAndBillable());
        assertEquals("Error Detected records", new Integer(0), process.getErrors());
        assertEquals("Total CDR records process", new Integer(5), process.getRecordsProcessed());
        assertEquals("Done and Not Billable records", new Integer(4), process.getDoneAndNotBillable());

        List<Integer> orderIds = Arrays.asList(process.getOrderIds());
        assertEquals("Total Order Created By Process ", new Integer(1), process.getOrderAffectedCount());
        assertNotNull("Order Created through mediation", orderIds);
        OrderWS usageOrder = api.getOrder(orderIds.get(0));
        assertTrue("Mediated Order Should have isMediated Flag checked: ", usageOrder.getIsMediated());
        quantityInOrderLines(usageOrder, "1.0000000000");
        JbillingMediationRecord[] viewEvents = api.getMediationEventsForOrder(orderIds.get(0));
        assertEquals("Total Order Events Created ", 5, viewEvents.length);
    }

	/**
	* Get Mediation Configuration Id using Mediation JobLauncher Name.
	* @param mediationJobLauncher
	* @return
	*/
    private Integer getMediationConfiguration(String mediationJobLauncher) {
   	
   	List<MediationConfigurationWS> allMediationConfiguration = Arrays.asList(api.getAllMediationConfigurations());
        for (MediationConfigurationWS mediationConfigurationWS: allMediationConfiguration) {
            if (null != mediationConfigurationWS.getMediationJobLauncher() &&
                    (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
       		 return mediationConfigurationWS.getId();
       	 }
        }
   	return null;
   }

    private AssetWS createAsset(String identifierValue, Integer itemId, Integer defaultStatusId) {
		logger.debug("Creating asset...");
		AssetWS asset = new AssetWS();
		asset.setIdentifier(identifierValue);
		asset.setItemId(itemId);
		asset.setEntityId(Integer.valueOf(1));
		asset.setEntities(Arrays.asList(Integer.valueOf(1)));
		asset.setAssetStatusId(defaultStatusId);
		asset.setDeleted(Integer.valueOf(0));

		Integer assetId = api.createAsset(asset);
		asset = api.getAsset(assetId);

		logger.debug("Asset created with id:{}", asset.getId());
		return asset;
	}

    private List<String> buildInboundCDR(String asset, List<String> quantities, String eventDate) {
		List<String> cdrs = new ArrayList<String>();
		quantities.forEach(quantity -> {
			cdrs.add("us-cs-telephony-voice-20170531.vdc-1225UTC-" + UUID.randomUUID().toString()+",6165042651,lisa.bravo,Inbound,"+ asset +","+eventDate+","+"12:00:16 AM,4,3,47,2,0,"+quantity+",47,0,null");
		});
		return cdrs;
	}

	@AfterClass
	public void cleanUp(){
		FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);
		try {
			if(ratingSchemeId!=-1) {
				logger.debug("Deleting persisted rating scheme {}", ratingSchemeId);
				Boolean deleted = api.deleteRatingScheme(ratingSchemeId);
				assertTrue(deleted);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Rating Scheme deletion failed.");
		}
	}
}
