/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

/**
 * FreeUsagePoolTest
 * Free Usage Pool Definition (CRUD) related test cases.
 * @author Amol Gadre
 * @since 15-Dec-2013
 */

@Test(groups = { "usagePools" }, testName = "FreeUsagePoolTest")
public class FreeUsagePoolTest {

    private static final Logger logger = LoggerFactory.getLogger(FreeUsagePoolTest.class);
    private static final Integer DISABLED = Integer.valueOf(0);
    private static final Integer PRANCING_PONY = Integer.valueOf(1);
    private static final Integer LANGUAGE_FRENCH_ID = Integer.valueOf(2);
	private static final String QUANTITY = "100";

    private Integer itemTypeId;
    private Integer itemId;
	private Integer usagePoolId = null;
	long today = new Date().getTime();

    private JbillingAPI api;

    @BeforeTest
    public void initializeTests(){
        if(null == api)
		try {
			api = JbillingAPIFactory.getAPI();
		} catch (Exception e) {
			logger.error("Error while getting API", e);
		}

        // Create and persist Test Item Category
        itemTypeId = createItemType("FUPTestItemType");

        // Create and persist Test item
        itemId = createItem("TestItem", "TestItem", new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10.00"), Constants.PRIMARY_CURRENCY_ID), itemTypeId);

	}

    @AfterTest
    public void tearDown(){
        if(null != itemId){
            try{
                api.deleteItem(itemId);
                itemId = null;
            } catch (SessionInternalError sie){
                fail(String.format("Error deleting item %d", itemId));
            }
        }
        if(null != itemTypeId){
            try {
                api.deleteItemCategory(itemTypeId);
                itemTypeId = null;
            } catch (SessionInternalError sie){
                fail(String.format("Error deleting item type %d", itemTypeId));
            }
        }
        if(null != api){
            api = null;
        }
    }
	
	@Test
	public void test001CreateFreeUsagePool() throws Exception {
		
		Date today = new Date();
		
		try {
			UsagePoolWS usagePool1 = null;
			api.createUsagePool(usagePool1);
			fail("There should be an exception");
		} 
		catch (Exception e) {
			assertTrue("Exception is not of type SessionInternalError." + e, e instanceof SessionInternalError);
		}
		
		try {
			UsagePoolWS usagePool2 = new UsagePoolWS();
			usagePool2.setName("100 Local Calls Mins " + today);
			api.createUsagePool(usagePool2);
			fail("There should be an exception");
		} 
		catch (Exception e) {
			assertTrue("Exception is not of type SessionInternalError." + e, e instanceof SessionInternalError);
		}
		
		try {
			UsagePoolWS usagePool3 = new UsagePoolWS();
	        
			usagePool3.setName("100 Local Calls Mins " + today);
			usagePool3.setQuantity(QUANTITY);
			api.createUsagePool(usagePool3);
			fail("There should be an exception");
		} 
		catch (Exception e) {
			assertTrue("Exception is not of type SessionInternalError." + e, e instanceof SessionInternalError);
		}
		
		logger.debug("#testCreate");

		UsagePoolWS usagePool = populateFreeUsagePoolObject("100 National Calls", BigDecimal.ONE.toString(), Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Zero");

        logger.debug("Creating usagePool ...{}", usagePool);
        usagePoolId = api.createUsagePool(usagePool);
        assertNotNull("The item was not created", usagePoolId);
        api.deleteUsagePool(usagePoolId);

	}
	
	@Test
    public void test002UpdateUsagePool() {
        usagePoolId = createFreeUsagePool("200 Smart Phones free", "100", Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Zero");

        logger.debug("Getting usagePool");
        UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId);

        String name = usagePool.getNames().get(0).getContent();
        String quantity = usagePool.getQuantity();
        Integer precedence= usagePool.getPrecedence();
        String cyclePeriodUnit = usagePool.getCyclePeriodUnit();
        Integer cyclePeriodValue= usagePool.getCyclePeriodValue();
        Integer[] itemTypes = usagePool.getItemTypes();
        Integer[] items = usagePool.getItems();
        String resetValue=usagePool.getUsagePoolResetValue();

        logger.debug("Changing properties");
        usagePool.setName("200 National SMS"+today);
        usagePool.setQuantity(BigDecimal.ONE.toString());
        usagePool.setPrecedence(new Integer(1));
        usagePool.setCyclePeriodUnit("Months");
        usagePool.setCyclePeriodValue(new Integer(1));
        Integer itemTypes1[] = new Integer[1];
        itemTypes1[0] = itemTypeId;
        usagePool.setItemTypes(itemTypes1);
        Integer items1[] = new Integer[1];
        items1[0] = itemId;
        usagePool.setItems(items1);
        usagePool.setUsagePoolResetValue("Zero");

        logger.debug("Updating usagePools");
        api.updateUsagePool(usagePool);

        UsagePoolWS usagePoolsChanged = api.getUsagePoolWS(usagePoolId);
        assertEquals(usagePoolsChanged.getNames().get(0).getContent(), "200 National SMS"+today);
        logger.debug("quantity {}", usagePoolsChanged.getQuantity());
        assertEquals(usagePoolsChanged.getQuantity(), "1.0000000000");
        assertEquals(usagePoolsChanged.getPrecedence(), precedence);
        assertEquals(usagePoolsChanged.getCyclePeriodUnit(), cyclePeriodUnit);
        assertEquals(usagePoolsChanged.getCyclePeriodValue(), cyclePeriodValue);
        assertEquals(usagePoolsChanged.getItemTypes()[0], itemTypes[0]);
        assertEquals(usagePoolsChanged.getItems()[0], items[0]);
        assertEquals(usagePoolsChanged.getUsagePoolResetValue(), "Zero");
        logger.debug("Done!");

        logger.debug("Restoring initial usagePool state.");
        usagePool.setName(name);
        usagePool.setQuantity(BigDecimal.TEN.toString());
        api.updateUsagePool(usagePool);
        api.deleteUsagePool(usagePoolId);
	}
	
	@Test
    public void test003AllUsagePools() throws Exception {
        Integer usagePoolId1 = createFreeUsagePool("130 National Calls", "100",Constants.USAGE_POOL_CYCLE_PERIOD_DAYS, "Reset To Initial Value");
        Integer usagePoolId2 = createFreeUsagePool("220 National SMS", "20", Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Zero");
        Integer usagePoolId3 = createFreeUsagePool("Basic Internet", "100", Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Add The Initial Value");
        Integer usagePoolId4 = createFreeUsagePool("Local Calls", "200", Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS, "No Changes");
        Integer usagePoolId5 = createFreeUsagePool("Reserved VM 01 Linux", "0", Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Hours Per Calendar Month");

        UsagePoolWS usagePool = api.getUsagePoolWS(usagePoolId1);
        assertEquals("Name", "130 National Calls"+today, usagePool.getNames().get(0).getContent());
        assertEquals("quantity", "100.0000000000", usagePool.getQuantity());
        assertEquals("Precedence", new Integer(1), usagePool.getPrecedence());
        assertEquals("cycle Period Unit","Days", usagePool.getCyclePeriodUnit());
        assertEquals("Cycle Period Value", new Integer(1), usagePool.getCyclePeriodValue());
        assertEquals("Category 1", itemTypeId, usagePool.getItemTypes()[0]);
        assertEquals("Product 1", itemId, usagePool.getItems()[0]);
        assertEquals("Reset Value", "Reset To Initial Value", usagePool.getUsagePoolResetValue());
        
        usagePool = api.getUsagePoolWS(usagePoolId2);
        assertEquals("Name", "220 National SMS"+today, usagePool.getNames().get(0).getContent());
        assertEquals("quantity", "20.0000000000", usagePool.getQuantity());
        assertEquals("Precedence", new Integer(1), usagePool.getPrecedence());
        assertEquals("cycle Period Unit","Months", usagePool.getCyclePeriodUnit());
        assertEquals("Cycle Period Value", new Integer(1), usagePool.getCyclePeriodValue());
        assertEquals("Category 11", itemTypeId, usagePool.getItemTypes()[0]);
        assertEquals("Product 12", itemId, usagePool.getItems()[0]);
        assertEquals("Reset Value", "Zero", usagePool.getUsagePoolResetValue());

        usagePool = api.getUsagePoolWS(usagePoolId3);
        assertEquals("Name", "Basic Internet"+today, usagePool.getNames().get(0).getContent());
        assertEquals("quantity", "100.0000000000", usagePool.getQuantity());
        assertEquals("Precedence", new Integer(1), usagePool.getPrecedence());
        assertEquals("cyclePeriodUnit","Months", usagePool.getCyclePeriodUnit());
        assertEquals("CyclePeriodValue", new Integer(1), usagePool.getCyclePeriodValue());
        assertEquals("Category 21", itemTypeId, usagePool.getItemTypes()[0]);
        assertEquals("Product 22", itemId, usagePool.getItems()[0]);
        assertEquals("Reset Value", "Add The Initial Value", usagePool.getUsagePoolResetValue());

        usagePool = api.getUsagePoolWS(usagePoolId4);
        assertEquals("Name", "Local Calls"+today, usagePool.getNames().get(0).getContent());
        assertEquals("quantity", "200.0000000000", usagePool.getQuantity());
        assertEquals("Precedence", new Integer(1), usagePool.getPrecedence());
        assertEquals("cyclePeriodUnit","Billing Periods", usagePool.getCyclePeriodUnit());
        assertEquals("CyclePeriodValue", new Integer(1), usagePool.getCyclePeriodValue());
        assertEquals("Category 31", itemTypeId, usagePool.getItemTypes()[0]);
        assertEquals("Product 32", itemId, usagePool.getItems()[0]);
        assertEquals("Reset Value", "No Changes", usagePool.getUsagePoolResetValue());

        usagePool = api.getUsagePoolWS(usagePoolId5);
        assertEquals("Name", "Reserved VM 01 Linux"+today, usagePool.getNames().get(0).getContent());
        assertEquals("quantity",new BigDecimal("0"), new BigDecimal(usagePool.getQuantity()));
        assertEquals("Precedence", new Integer(1), usagePool.getPrecedence());
        assertEquals("cyclePeriodUnit","Months", usagePool.getCyclePeriodUnit());
        assertEquals("CyclePeriodValue", new Integer(1), usagePool.getCyclePeriodValue());
        assertEquals("Category 41", itemTypeId, usagePool.getItemTypes()[0]);
        assertEquals("Product 42", itemId, usagePool.getItems()[0]);
        assertEquals("Reset Value", "Hours Per Calendar Month", usagePool.getUsagePoolResetValue());

        api.deleteUsagePool(usagePoolId1);
        api.deleteUsagePool(usagePoolId2);
        api.deleteUsagePool(usagePoolId3);
        api.deleteUsagePool(usagePoolId4);
        api.deleteUsagePool(usagePoolId5);
    }
	
	@Test
    public void test004CreateUsagePoolWithMultipleNames() {
        UsagePoolWS usagePool= createUsagePoolWithMultipleNames();
        logger.debug("UsagePoolId created: {}", usagePool.getId());
        logger.debug("UsagePoolId created: {}", usagePool);

        usagePool = api.getUsagePoolWS(usagePool.getId());
        String enName = getName(usagePool.getNames(), Constants.LANGUAGE_ENGLISH_ID);
        String frName = getName(usagePool.getNames(), LANGUAGE_FRENCH_ID);
        logger.debug("names: {}, {}", enName, frName);
        assertEquals("104 National Calls"+today, enName);
        assertEquals("104 National calls Fr", frName);
        // delete the usagePool
        api.deleteUsagePool(usagePool.getId());

    }
	
	@Test
	public void test005ValidateUpdateFreeUsagePool() throws Exception {
		
		usagePoolId = createFreeUsagePool("165 National Calls", "100", Constants.USAGE_POOL_CYCLE_PERIOD_DAYS, "Reset To Initial Value");
		try {
			UsagePoolWS usagePool1 = api.getUsagePoolWS(usagePoolId);
			assertNotNull("Usage pool is null;",usagePool1);
			usagePool1.setName("");
			api.updateUsagePool(usagePool1);
			fail("There should be an exception");
		}
		catch (Exception e) {
			logger.error("##### exception caught: " + e);
			assertTrue("Exception is not of type SessionInternalError." + e, e instanceof SessionInternalError);
		}

        Integer newItemType = null;

		try {
			UsagePoolWS usagePool2 = api.getUsagePoolWS(usagePoolId);
			assertNotNull("Usage pool is null;",usagePool2);
			usagePool2.setQuantity("200");

            newItemType = createItemType("NewTestItemType");

			Integer itemTypes[] = new Integer[1];
			itemTypes[0] = newItemType;
			usagePool2.setItemTypes(itemTypes);
			api.updateUsagePool(usagePool2);
			UsagePoolWS usagePool3 = api.getUsagePoolWS(usagePoolId);
			for (Integer itemType : usagePool3.getItemTypes()) {
				assertEquals("Item type not updated." , newItemType, itemType);
			}
		}
		catch (Exception e) {
			fail("Message");
			assertTrue("Exception is not of type SessionInternalError." + e, e instanceof SessionInternalError);
		} finally {
            if(null != usagePoolId){
                api.deleteUsagePool(usagePoolId);
            }
            if(null != newItemType){
                api.deleteItemCategory(newItemType);
            }
        }
    }

	@Test(expectedExceptions = SessionInternalError.class)
	public void test006DeleteFreeUsagePool() {
		usagePoolId = createFreeUsagePool("500 National Calls", "500", Constants.USAGE_POOL_CYCLE_PERIOD_DAYS, "Reset To Initial Value");
		api.deleteUsagePool(usagePoolId);
		logger.debug("UsagePool Delete Successfully.");
		//because the pool does not exist this throws exception
		api.getUsagePoolWS(usagePoolId);
	}

    // create Item category
    private Integer createItemType(String description) {
        ItemTypeWS itemType = buildItemType(description);
        Integer itemTypeId = api.createItemCategory(itemType);
        assertNotNull(itemTypeId);
        ItemTypeWS[] types = api.getAllItemCategories();

        boolean addedFound = false;
        for (int i = 0; i < types.length; ++i) {
            if (itemType.getDescription().equals(types[i].getDescription())) {
                logger.debug("Test category was found. Creation was completed successfully.");
                addedFound = true;
                break;
            }
        }
        assertTrue(itemType.getDescription() + " not found.", addedFound);
        return itemTypeId;
    }

    private ItemTypeWS buildItemType(String desc){
        ItemTypeWS itemType = new ItemTypeWS();

        itemType.setDescription(desc);
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        itemType.setEntityId(PRANCING_PONY);
        List<Integer> entities = new ArrayList<Integer>(1);
        entities.add(PRANCING_PONY);
        itemType.setEntities(entities);
        itemType.setAllowAssetManagement(DISABLED);

        return itemType;
    }

    // create Product
    private Integer createItem(String number, String description, PriceModelWS priceModel, Integer... itemTypeId) {

        ItemDTOEx item = buildItem(number, description, itemTypeId);
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, priceModel);
        Integer itemId = api.createItem(item);
        assertNotNull("Item was not created", itemId);
        return itemId;

    }

    private ItemDTOEx buildItem(String number, String desc, Integer... itemTypesId) {
        ItemDTOEx item = new ItemDTOEx();
        Long entitySuffix = System.currentTimeMillis();
        item.setNumber(String.format("%s-%s", number, entitySuffix));
        item.setDescription(String.format("%s-%s", desc, entitySuffix));
        item.setTypes(itemTypesId);
        item.setEntityId(PRANCING_PONY);
        item.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        item.setPriceModelCompanyId(PRANCING_PONY);
        return item;
    }


	// create Free Usage Pool
	private Integer createFreeUsagePool(String usagePoolName, String quantity, String cyclePeriodUnit, String resetValue) {
		
		UsagePoolWS usagePool = populateFreeUsagePoolObject(usagePoolName, quantity, cyclePeriodUnit, resetValue);
		Integer poolId = api.createUsagePool(usagePool);
        logger.debug("usagePoolId :: {}", poolId);
        assertNotNull("Free usage pool should not be null ", poolId);
        return poolId;
	}
		
	private UsagePoolWS populateFreeUsagePoolObject(String usagePoolName, String quantity, String cyclePeriodUnit, String resetValue) {
		
        UsagePoolWS usagePool = new UsagePoolWS();
		usagePool.setName(usagePoolName + today);
		usagePool.setQuantity(quantity);
		usagePool.setPrecedence(new Integer(1));
		usagePool.setCyclePeriodUnit(cyclePeriodUnit);
		usagePool.setCyclePeriodValue(new Integer(1));
		usagePool.setItemTypes(new Integer[]{itemTypeId});
        usagePool.setItems(new Integer[]{itemId});
        usagePool.setEntityId(PRANCING_PONY);
        usagePool.setUsagePoolResetValue(resetValue);
        
        return usagePool;
	}
		
    private UsagePoolWS createUsagePoolWithMultipleNames() {
        List<InternationalDescriptionWS> names = new java.util.ArrayList<InternationalDescriptionWS>();
        InternationalDescriptionWS enName = new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "104 National Calls"+today);
        InternationalDescriptionWS frName = new InternationalDescriptionWS(LANGUAGE_FRENCH_ID, "104 National calls Fr");
        names.add(enName);
        names.add(frName);

        UsagePoolWS newUsagePool= populateFreeUsagePoolObject("", BigDecimal.TEN.toString(), Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, "Zero");
        newUsagePool.setNames(names);

        logger.debug("Creating Usage Pools ... {}", newUsagePool);
        Integer ret = api.createUsagePool(newUsagePool);
        assertNotNull("The usage pool was not created", ret);
        logger.debug("Done!");
        newUsagePool = api.getUsagePoolWS(ret);

        return newUsagePool;
    }
		
	private String getName(List<InternationalDescriptionWS> names,int langId) {
        for (InternationalDescriptionWS name : names) {
            if (name.getLanguageId() == langId) {
                return name.getContent();
            }
        }
        return "";
    }
}
