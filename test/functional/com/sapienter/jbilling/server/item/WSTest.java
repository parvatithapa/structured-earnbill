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

package com.sapienter.jbilling.server.item;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateMidnight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.JBillingTestUtils;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.api.SpringAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;

/**
 * @author Emil
 */
@Test(groups = { "web-services", "item" }, testName = "item.WSTest")
public class WSTest {

	private static final Logger logger = LoggerFactory.getLogger(LateGuidedUsageTest.class);
	private static final Integer PRANCING_PONY = Integer.valueOf(1);
	private static final Integer ENABLED = Integer.valueOf(1);
	private static final Integer DISABLED = Integer.valueOf(0);
	private static final Integer US_DOLLAR = Integer.valueOf(1);
	private static final Integer AU_DOLLAR = Integer.valueOf(11);
	private static final Integer ENGLISH_LANGUAGE = Integer.valueOf(1);
	private static final Integer FRENCH_LANGUAGE = Integer.valueOf(2);
	private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private static Integer TEST_USER_ID;
	private static Integer TEST_ITEM_TYPE_ID;
	private static Integer TEST_ASSET_ITEM_TYPE_ID;
	private static Integer STATUS_DEFAULT_ID;
	private static Integer STATUS_AVAILABLE_ID;
	private static Integer STATUS_ORDER_SAVED_ID;
	private static Integer STATUS_RESERVED_ID;
	private static Integer TEST_ITEM_ID_WITH_ASSET_MANAGEMENT;
	protected static final Integer CURRENCY_USD = Integer.valueOf(1);
	private String ASSET_STATUS_AVAILABLE = "AVAILABLE";
	private String ASSET_STATUS_ORDERED = "ORDERED";
	private String ASSET_STATUS_DEFAULT = "DEFAULT";
	private String ASSET_STATUS_INTERNAL = "INTERNAL";
	
	private static JbillingAPI api;
	private static JbillingAPI childApi;


	@BeforeClass
	public void initializeTests() throws IOException, JbillingAPIException {
		if(null == api){
			api = JbillingAPIFactory.getAPI();
		}
		if(null == childApi){
			childApi = new SpringAPI(RemoteContext.Name.API_CHILD_CLIENT);
		}

		// Create And Persist User
		UserWS customer = null;
		try {
			customer = com.sapienter.jbilling.server.user.WSTest.createUser(true, true, null, US_DOLLAR, true);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Error creating customer!!!");
		}

		TEST_USER_ID = customer.getUserId();

		// Create Item Type
		ItemTypeWS itemType = createItemType(false, false);
		// Persist
		TEST_ITEM_TYPE_ID = api.createItemCategory(itemType);

		// Create Asset Managed Item Type
		ItemTypeWS assetItemType = createItemType(true, false);
		TEST_ASSET_ITEM_TYPE_ID = api.createItemCategory(assetItemType);

		// Get Default Asset Status Id
		Integer[] statusesIds = getAssetStatusesIds(TEST_ASSET_ITEM_TYPE_ID);
		STATUS_DEFAULT_ID = statusesIds[0];
		STATUS_AVAILABLE_ID = statusesIds[1];
		STATUS_ORDER_SAVED_ID = statusesIds[2];
		STATUS_RESERVED_ID = statusesIds[3];
	}

	@AfterClass
	public void tearDown() throws Exception {

		if(null != TEST_ITEM_TYPE_ID){
			api.deleteItemCategory(TEST_ITEM_TYPE_ID);
			TEST_ITEM_TYPE_ID = null;
		}

		if(null != TEST_ASSET_ITEM_TYPE_ID){
			api.deleteItemCategory(TEST_ASSET_ITEM_TYPE_ID);
			TEST_ASSET_ITEM_TYPE_ID = null;
		}

		if(null != STATUS_DEFAULT_ID){
			STATUS_DEFAULT_ID = null;
		}
		if(null != STATUS_AVAILABLE_ID){
			STATUS_AVAILABLE_ID = null;
		}
		if(null != STATUS_ORDER_SAVED_ID){
			STATUS_ORDER_SAVED_ID = null;
		}
		if(null != STATUS_RESERVED_ID){
			STATUS_RESERVED_ID = null;
		}

		if(null != TEST_USER_ID){
			api.deleteUser(TEST_USER_ID);
			TEST_USER_ID = null;
		}

		if(null != api){
			api = null;
		}
		if(null != childApi){
			childApi = null;
		}
	}

	@BeforeMethod(groups = "asset")
	public void initializeAssetManagementTest() {

		// Create Asset Managed Item
		ItemDTOEx assetProduct = createItem(true, false, TEST_ASSET_ITEM_TYPE_ID);

		// Persist Item
		TEST_ITEM_ID_WITH_ASSET_MANAGEMENT = api.createItem(assetProduct);

	}

	@AfterMethod(groups = "asset")
	public void cleanupAssetManagementTest() {
		if(null != TEST_ITEM_ID_WITH_ASSET_MANAGEMENT){
			api.deleteItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
			TEST_ITEM_ID_WITH_ASSET_MANAGEMENT = null;
		}
	}

	@Test
	public void test001Create() {

		// Create new Item
		ItemDTOEx newItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		logger.debug("Creating item {}", newItem);
		// Persist
		Integer itemId = api.createItem(newItem);
		assertNotNull("The item was not created", itemId);
		newItem.setId(itemId);

		// Check and compare
		ItemDTOEx persistedItem = api.getItem(itemId, TEST_USER_ID, null);
		assertNotNull(String.format("No items persisted for with: %d", itemId), persistedItem);

		// Check for match between manually created and persisted Items
		matchItems(newItem, persistedItem);

		// Clean up
		api.deleteItem(itemId);
	}

	@Test
	public void test002CreateMultipleDescriptions() {

		// Create And Persist Item with multiple descriptions
		ItemDTOEx newItem = createItemWithMultipleDescriptions(TEST_ITEM_TYPE_ID);

		// Get the persisted Item
		ItemDTOEx persistedItem = api.getItem(newItem.getId(), null, null);

		// Check for match between manually created and persisted Items
		matchItems(newItem, persistedItem);

		// Clean up
		api.deleteItem(persistedItem.getId());

	}

	@Test
	public void test003ModifyMultipleDescriptions() {

        /* Create And Persist Item with multiple descriptions */
		ItemDTOEx newItem = createItemWithMultipleDescriptions(TEST_ITEM_TYPE_ID);

		// test remove one description (english) and update
		newItem = api.getItem(newItem.getId(), null, null);
		List<InternationalDescriptionWS> descriptions = newItem.getDescriptions();
		assertNotNull(String.format("Item %d should have descriptions!!!", newItem.getId()), descriptions);
		assertEquals(String.format("There should be two descriptions for the %d Item!!", newItem.getId()), Integer.valueOf(2), Integer.valueOf(descriptions.size()));
		InternationalDescriptionWS englishDescription = descriptions.get(0);
		assertNotNull(String.format("Item %d should have english description!!!", newItem.getId()), englishDescription);

		// set english description as deleted
		englishDescription.setDeleted(true);
		api.updateItem(newItem);

		// Get the persisted Item and check the descriptions count
		newItem = api.getItem(newItem.getId(), null, null);
		descriptions = newItem.getDescriptions();
		assertNotNull(String.format("Item %d should have descriptions!!!", newItem.getId()), descriptions);
		assertEquals(String.format("There should be one description for the %d Item!!", newItem.getId()), Integer.valueOf(1), Integer.valueOf(descriptions.size()));

		// test modify content
		descriptions.get(0).setContent("newItemDescription-fr");
		api.updateItem(newItem);
		newItem = api.getItem(newItem.getId(), null, null);
		String frDescription = getDescription(newItem.getDescriptions(), FRENCH_LANGUAGE);
		assertEquals("newItemDescription-fr", frDescription);

		// Clean up
		api.deleteItem(newItem.getId());
	}

	private ItemDTOEx createItemWithMultipleDescriptions(Integer... types) {

		ItemDTOEx newItem = createItem(false, false, types);

		List<InternationalDescriptionWS> descriptions = new java.util.ArrayList<InternationalDescriptionWS>();
		InternationalDescriptionWS enDesc = new InternationalDescriptionWS(ENGLISH_LANGUAGE, "itemDescription-en");
		InternationalDescriptionWS frDesc = new InternationalDescriptionWS(FRENCH_LANGUAGE, "itemDescription-fr");
		descriptions.add(enDesc);
		descriptions.add(frDesc);

		newItem.setDescriptions(descriptions);

		logger.debug("Creating item {}", newItem);
		Integer itemId = api.createItem(newItem);
		assertNotNull("The item was not created", itemId);
		logger.debug("Done!");
		newItem.setId(itemId);

		return newItem;
	}

	private String getDescription(List<InternationalDescriptionWS> descriptions, int langId) {
		for (InternationalDescriptionWS description : descriptions) {
			if (description.getLanguageId() == langId) {
				return description.getContent();
			}
		}
		return "";
	}

	@Test
	public void test006GetAllItems() {

		logger.debug("Getting all items");
		ItemDTOEx[] items =  api.getAllItems();
		assertNotNull("The items were not retrieved", items);
		int initialItemsCount = items.length;

		// First Item
		ItemDTOEx firstItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer firstItemId = api.createItem(firstItem);
		firstItem.setId(firstItemId);

		// Second Item
		ItemDTOEx secondItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer secondItemId = api.createItem(secondItem);
		secondItem.setId(secondItemId);

		// Get the current number of persisted Item entities
		items = api.getAllItems();
		assertNotNull("The items were not retrieved", items);
		int currentItemsCount = items.length;

		// Check of the initial number of persisted entities increased by two
		assertEquals("The number of persisted Item entities is not increasing!!!", Integer.valueOf(initialItemsCount + 2), Integer.valueOf(currentItemsCount));

		// Now check if the newest persisted entities are in the 'allItems array'
		int firstPersistedItemIndex = -1;
		int secondPersistedItemIndex = -1;
		ItemDTOEx[] allItems = api.getAllItems();
		for (int i = 0; i < allItems.length; i++){
			if(allItems[i].getId().equals(firstItemId)){
				firstPersistedItemIndex = i;
			}
			else if(allItems[i].getId().equals(secondItemId)){
				secondPersistedItemIndex = i;
			}
		}
		if(firstPersistedItemIndex > 0 && secondPersistedItemIndex > 0){
			matchItems(firstItem, allItems[firstPersistedItemIndex]);
			matchItems(secondItem, allItems[secondPersistedItemIndex]);
		}
		else {
			fail(String.format("The index of first persisted item is %d, the index of second persisted item is %d. " +
					"\nThey should be positive numbers if found in the persisted array!!", firstPersistedItemIndex, secondPersistedItemIndex));
		}

		// Clean up
		api.deleteItem(firstItemId);
		api.deleteItem(secondItemId);

	}

	@Test
	public void test007UpdateItem() {

		// Get the initial number of persisted items
		int initialItemsCount = api.getAllItems().length;

		// Create new Item
		ItemDTOEx newItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		logger.debug("Creating item {}", newItem);

		// Persist Item
		Integer itemId = api.createItem(newItem);

		// Get the current number of persisted items
		int itemsCountAfterPersist = api.getAllItems().length;

		// Check if the initial count of items increased
		assertEquals("Initial count of items not increased!!", Integer.valueOf(initialItemsCount + 1), Integer.valueOf(itemsCountAfterPersist));

		// Get the persisted Item
		logger.debug("Getting item");
		ItemDTOEx item = api.getItem(itemId, TEST_USER_ID, null);
		logger.debug("After persist item: {}", item);
		// Update some of the properties
		logger.debug("Changing properties");
		item.setDescription("Another description");
		item.setNumber("NMR-01");
		item.setPrice(new BigDecimal("1.00"));

		// Persist changes
		logger.debug("Updating item");
		api.updateItem(item);

		// Get the current number of persisted items
		int itemsCountAfterUpdate = api.getAllItems().length;

		// Check if the current count of items remains the same as before update
		assertEquals("Initial count of items not increased!!", Integer.valueOf(itemsCountAfterPersist), Integer.valueOf(itemsCountAfterUpdate));

		// Get the updated item and compare it with the manually created one to verify changes
		ItemDTOEx itemChanged = api.getItem(itemId, TEST_USER_ID, null);
		logger.debug("After update item: {}", itemChanged);
		matchItems(item, itemChanged);

		// Clean up
		api.deleteItem(itemId);
	}

	@Test
	public void test008CurrencyConvert() {

		// Create And Persist User who uses USD currency
		UserWS customer = null;
		try {
			customer = com.sapienter.jbilling.server.user.WSTest.createUser(true, true, null, US_DOLLAR, true);
		} catch (Exception e) {
			fail("Error creating customer!!!");
		}

		// Create Item with AUD currency
		ItemDTOEx item = createItem(false, false, TEST_ITEM_TYPE_ID);
		item.setPrice(new BigDecimal("15.00"));
		item.addDefaultPrice(new Date(), new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("15.00"), AU_DOLLAR));

		// Persist Item
		Integer itemId = api.createItem(item);

		// Persisted item has price in AUD - fetch item using a USD customer
		item = api.getItem(itemId, customer.getId(), new PricingField[] {} );

		// price automatically converted to user currency when item is fetched
		assertEquals("Price in USD", 1, item.getCurrencyId().intValue());
		assertEquals("Converted price AUD->USD", BigDecimal.TEN, item.getPriceAsDecimal());
		logger.debug("Item default price: {}", item.getDefaultPrice());
		// verify that default item price is in AUD
		assertEquals("Default price in AUD", 11, item.getDefaultPrice().getCurrencyId().intValue());
		assertEquals("Default price in AUD", new BigDecimal("15.00"), item.getDefaultPrice().getRateAsDecimal());

		// Clean up
		api.deleteItem(itemId);
		api.deleteUser(customer.getId());
	}

	@Test
	public void test009GetAllItemCategories() {

		ItemTypeWS[] types = api.getAllItemCategories();
		assertNotNull("Some result should be received!!", types);

		// Get initial number of categories
		int initialCategoriesCount = types.length;

		// Create two new categories
		// First category
		// Create
		ItemTypeWS firstCategory = createItemType(false, false);
		// Persist
		Integer firstItemCategoryId = api.createItemCategory(firstCategory);
		firstCategory.setId(firstItemCategoryId);

		// Second category
		// Create
		ItemTypeWS secondCategory = createItemType(false, false);
		// Persist
		Integer secondItemCategoryId = api.createItemCategory(secondCategory);
		secondCategory.setId(secondItemCategoryId);

		// Check if the initial number of persisted item types increased by two
		types = api.getAllItemCategories();
		int categoriesCountAfterPersist = types.length;

		assertEquals("The initial number of persisted item types is not increasing!!", Integer.valueOf(initialCategoriesCount + 2), Integer.valueOf(categoriesCountAfterPersist));

		// Check if the returned array of item types contains previously persisted item types.
		int firstItemTypeIndex = -1;
		int secondItemTypeIndex = -1;

		for (int i = 0; i < categoriesCountAfterPersist; i++){
			if(types[i].getId().equals(firstItemCategoryId)){
				firstItemTypeIndex = i;
			}
			else if(types[i].getId().equals(secondItemCategoryId)){
				secondItemTypeIndex = i;
			}
		}

		if(firstItemTypeIndex > 0 && secondItemTypeIndex > 0){
			matchItemTypes(firstCategory, types[firstItemTypeIndex], false);
			matchItemTypes(secondCategory, types[secondItemTypeIndex], false);
		}
		else {
			fail(String.format("The index of first persisted category is %d, the index of second persisted category is %d. " +
					"\nThey should be positive numbers if found in the persisted array!!", firstItemTypeIndex, secondItemTypeIndex));
		}

		// Clean up
		api.deleteItemCategory(firstItemCategoryId);
		api.deleteItemCategory(secondItemCategoryId);

	}

	@Test
	public void test010CreateItemCategory() {

		String description = "Ice creams (WS test)";

		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription(description);
		itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);

		logger.debug("Creating item category '{}'...", description);
		Integer itemCategoryId = api.createItemCategory(itemType);
		assertNotNull(itemCategoryId);
		logger.debug("Done.");

		logger.debug("Getting all item categories...");
		ItemTypeWS[] types = api.getAllItemCategories();

		boolean addedFound = false;
		for (int i = 0; i < types.length; ++i) {
			if (description.equals(types[i].getDescription())) {
				logger.debug("Test category was found. Creation was completed successfully.");
				addedFound = true;
				break;
			}
		}
		assertTrue("Ice cream not found.", addedFound);

		//Test the creation of a category with the same description as another one.
		logger.debug("Going to create a category with the same description.");

		try {
			itemCategoryId = api.createItemCategory(itemType);
			fail("It should have thrown a SessionInternalError exception.");
		} catch (SessionInternalError sessionInternalError) {
			logger.error("Exception caught. The category was not created because another one already existed with the same description.", sessionInternalError);
		} finally {
			// Clean up
			api.deleteItemCategory(itemCategoryId);
		}

		//Test the creation of a category with the same (case insensitive) description as another one.
		logger.debug("Going to create a category with the same description but ignoring differences between uppercase and lowercase letters.");
		// Create and save the original itemType in uppercase letters.
		ItemTypeWS itemTypeOriginal = new ItemTypeWS();
		itemTypeOriginal.setDescription("FROZEN FOOD (TEST)");
		itemTypeOriginal.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		Integer itemCategory2Id = api.createItemCategory(itemTypeOriginal);
		assertNotNull(itemCategory2Id);
		// Create the duplicate with the same description but in lowercase letters.
		ItemTypeWS itemTypeDuplicate = new ItemTypeWS();
		itemTypeDuplicate.setDescription("frozen food (TEST)");
		itemTypeDuplicate.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		try {
			api.createItemCategory(itemTypeDuplicate);
			fail("It should have thrown a SessionInternalError exception.");
		} catch (SessionInternalError sessionInternalError) {
			logger.error("Exception caught. The category was not created because another one already existed with the same description, ignoring differences between lowercase and upercase letters.).", sessionInternalError);
		} finally {
			// Clean up
			api.deleteItemCategory(itemCategory2Id);
		}
	}

	@Test
	public void test011UpdateItemCategory() {

		String originalDescription;
		String description = "Updated Description";

		// Create new category
		ItemTypeWS itemCategory = createItemType(false, false);
		// Persist
		Integer itemCategoryId = api.createItemCategory(itemCategory);

		logger.debug("Getting item category...");

		// Get recently persisted item type
		itemCategory = api.getItemCategoryById(itemCategoryId);

		logger.debug("Changing description...");
		originalDescription = itemCategory.getDescription();
		itemCategory.setDescription(description);
		api.updateItemCategory(itemCategory);

		logger.debug("Getting item category...");
		itemCategory = api.getItemCategoryById(itemCategoryId);
		logger.debug("Verifying description has changed...");
		assertEquals(description, itemCategory.getDescription());
		logger.debug("Restoring description...");
		itemCategory.setDescription(originalDescription);
		api.updateItemCategory(itemCategory);

		// Create second category
		ItemTypeWS secondCategory = createItemType(false, false);
		// Persist
		Integer itemCategory2Id = api.createItemCategory(secondCategory);

		//Test the update of a category description to match one from another description.
		logger.debug("Getting item category...");

		// Get recently persisted item type
		secondCategory = api.getItemCategoryById(itemCategory2Id);
		// use used description
		secondCategory.setDescription(originalDescription);

		try {
			api.updateItemCategory(secondCategory);
			fail("It should have thrown a SessionInternalError exception.");
		} catch (SessionInternalError sessionInternalError) {
			logger.error("Exception caught. The category was not updated because another one already existed with the same description.", sessionInternalError);
		} finally {
			// Clean up
			api.deleteItemCategory(itemCategoryId);
			api.deleteItemCategory(itemCategory2Id);
		}

		//Test the update of a category description to match one from another description but changing a lowercase by an uppercase letter.
		logger.debug("Going to create a category with the same description but ignoring differences between uppercase and lowercase letters.");

		// Create and save the original itemType with uppercase letters.
		ItemTypeWS itemTypeOriginal = new ItemTypeWS();
		itemTypeOriginal.setDescription("FRUITS AND VEGETABLES (TEST)");
		itemTypeOriginal.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		Integer itemCategory3Id = api.createItemCategory(itemTypeOriginal);
		// Create a second itemType.
		ItemTypeWS itemTypeOther= new ItemTypeWS();
		itemTypeOther.setDescription("vegetables and fruits(TEST)");
		itemTypeOther.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		Integer itemCategory4Id = api.createItemCategory(itemTypeOther);
		// Change the description by the same as the original itemType but in lowercase letters.
		itemTypeOther.setDescription("fruits and vegetables (TEST)");
		try {
			api.updateItemCategory(itemTypeOther);
			fail("It should have thrown a SessionInternalError exception.");
		} catch (SessionInternalError sessionInternalError) {
			logger.error("Exception caught. The category was not created because another one already existed with the same description, ignoring differences between lowercase and upercase letters.).", sessionInternalError);
		} finally {
			// Clean up
			api.deleteItemCategory(itemCategory3Id);
			api.deleteItemCategory(itemCategory4Id);
		}
	}

	@Test
	public void test012GetItemsByCategory() {

		// First Item
		ItemDTOEx firstItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer firstItemId = api.createItem(firstItem);
		firstItem.setId(firstItemId);

		// Second Item
		ItemDTOEx secondItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer secondItemId = api.createItem(secondItem);
		secondItem.setId(secondItemId);

		// Get the items for a category
		ItemDTOEx[] items = api.getItemByCategory(TEST_ITEM_TYPE_ID);
		assertNotNull("There should be items received!!", items);
		assertEquals(String.format("Number of items for %d category is not 2!!!", TEST_ITEM_TYPE_ID), 2, items.length);

		// Find and Match
		matchItems(secondItem, items[0]);
		matchItems(firstItem, items[1]);

		// Clean up
		api.deleteItem(firstItemId);
		api.deleteItem(secondItemId);
	}

	@Test( groups = { "web-services", "asset"})
	public void test013CreateAsset() throws Exception {

		// Create asset
		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);

		// Set Asset Identifier as empty and try to persist
		asset.setIdentifier("");
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsAnyError(error, Arrays.asList(new String[]
					{"AssetWS,identifier,validation.error.null.asset.identifier",
							"AssetWS,identifier,validation.error.size,1,200"}));
		}

		// Create Asset without status and try to persist
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, null);
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatusId,validation.error.null.asset.status");
		}

		// Create Asset without item and try to persist
		asset = getAssetWS(null, STATUS_DEFAULT_ID);
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,itemId,validation.error.null.item");
		}

		// Create Non asset managed item
		ItemDTOEx item = createItem(false, false, TEST_ITEM_TYPE_ID);
		Integer itemId = api.createItem(item);

		// Create asset of non asset managed product
		asset = getAssetWS(itemId, STATUS_DEFAULT_ID);
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,itemId,asset.validation.item.not.assetmanagement");
		}

		// Create asset with empty meta field value and try to persist
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		MetaFieldValueWS[] assetMetaFieldsValues = asset.getMetaFields();
		assertNotNull(String.format("There should be meta fields for %s!!!", asset.getIdentifier()), asset.getMetaFields());
		assertEquals("There should be one meta field value!!", Integer.valueOf(1), Integer.valueOf(assetMetaFieldsValues.length));

		// Set to empty
		asset.setMetaFields(new MetaFieldValueWS[0]);
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "MetaFieldValue,value,metafield.validation.value.unspecified,Regulatory Code");
		}

		// Create Valid Asset
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);

		// Persist
		Integer assetId = api.createAsset(asset);

		// Get the persisted Asset
		AssetWS savedAsset = api.getAsset(assetId);
		JBillingTestUtils.assertPropertiesEqual(asset, savedAsset, new String[] {"id", "createDatetime", "status", "orderLineId", "metaFields", "provisioningCommands"});
		assertEquals(1, asset.getMetaFields().length);
		assertEquals(asset.getMetaFields()[0].getFieldName(), "Regulatory Code");
		assertTrue(asset.getMetaFields()[0].getListValueAsList().contains("01"));
		assertTrue(asset.getMetaFields()[0].getListValueAsList().contains("02"));

		// Try to create duplicate and persist
		try {
			api.createAsset(getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID));
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,identifier,asset.validation.duplicate.identifier");
		}

		// Clean up
		api.deleteAsset(assetId);
		api.deleteItem(itemId);
	}

	@Test(groups = { "web-services", "asset"})
	public void test014UpdateAsset() throws Exception {

		// Create Asset
		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);
		// Persist Asset
		Integer assetId = api.createAsset(asset);

		// Get saved asset
		AssetWS savedAsset = api.getAsset(assetId);

		// Create Second Asset
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);
		asset.setIdentifier("ID2");

		// Persist Second Asset
		Integer asset2Id = api.createAsset(asset);

		// Get second saved asset
		AssetWS savedAsset2 = api.getAsset(asset2Id);

		// Set the identifier as duplicate
		savedAsset2.setIdentifier(savedAsset.getIdentifier());
		try {
			api.updateAsset(savedAsset2);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,identifier,asset.validation.duplicate.identifier");
		}

		savedAsset2 = api.getAsset(asset2Id);
		savedAsset2.setAssetStatusId(STATUS_ORDER_SAVED_ID);
		try {
			api.updateAsset(savedAsset2);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatus,asset.validation.status.change.toordersaved");
		}

		// Clean up
		api.deleteAsset(asset2Id);
		api.deleteAsset(assetId);
	}


	@Test(groups = { "web-services", "asset"})
	public void test015createCategoryWithStatuses() throws Exception {

		ItemTypeWS type = createItemType(true, false);

		AssetStatusDTOEx status = new AssetStatusDTOEx();
		status.setDescription("OneDuplicate");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(ENABLED);
		status.setIsOrderSaved(DISABLED);
		status.setIsPending(DISABLED);
		status.setIsActive(DISABLED);
		type.getAssetStatuses().add(status);

		try {
			api.createItemCategory(type);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemTypeWS,statuses,validation.error.category.status.default.one");
		}

		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(ENABLED);
		status.setIsActive(ENABLED);
		status.setIsPending(DISABLED);
		try {
			api.createItemCategory(type);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemTypeWS,statuses,validation.error.category.status.active.one");
		}
	}


	@Test(groups = { "web-services", "asset"})
	public void test016UpdateItemWithAsset() {

		// Create Second Asset Managed Category
		ItemTypeWS assetCategory2 = createItemType(true, false);
		// Persist
		Integer assetCategory2Id = api.createItemCategory(assetCategory2);

		// Create Asset Managed Items
		ItemDTOEx assetItem = createItem(true, false, TEST_ASSET_ITEM_TYPE_ID);
		// Persist
		Integer assetItemId = api.createItem(assetItem);

		// Get item an update item types
		ItemDTOEx item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {TEST_ASSET_ITEM_TYPE_ID, assetCategory2Id});

		try {
			api.updateItem(item);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemDTOEx,types,product.validation.multiple.assetmanagement.types.error");
		}

		logger.debug("#test016UpdateItemWithAsset. Type without asset management");
		item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {TEST_ITEM_TYPE_ID});

		try {
			api.updateItem(item);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemDTOEx,types,product.validation.no.assetmanagement.type.error");
		}

		logger.debug("#test016UpdateItemWithAsset. Change asset management type");
		item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {assetCategory2Id});
		api.updateItem(item);

		AssetWS asset = getAssetWS(assetItemId, STATUS_AVAILABLE_ID);
		Integer assetId = api.createAsset(asset);

		item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {TEST_ASSET_ITEM_TYPE_ID});
		try {
			api.updateItem(item);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemDTOEx,types,product.validation.assetmanagement.changed.error");
		}

		logger.debug("#test016UpdateItemWithAsset. No asset manager");
		item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[0]);
		try {
			api.updateItem(item);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemDTOEx,types,validation.error.missing.type");
		}

		// Clean up
		api.deleteAsset(assetId);
		api.deleteItem(assetItemId);
		api.deleteItemCategory(assetCategory2Id);
	}

	@Test(groups = { "web-services", "asset"})
	public void test017UpdateItemTypeAssetManagement() {

		ItemTypeWS assetCategory = null;
		ItemTypeWS[] typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == TEST_ASSET_ITEM_TYPE_ID) {
				assetCategory = itemTypeWS;
				break;
			}
		}

		if(null == assetCategory){
			fail(String.format("Can not find persisted test asset category %d", TEST_ASSET_ITEM_TYPE_ID));
		}

		assetCategory.setAllowAssetManagement(DISABLED);
		logger.debug("#test017UpdateItemTypeAssetManagement. Can not change type's asset man enabled as product has asset management enabled");
		try {
			api.updateItemCategory(assetCategory);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemTypeWS,allowAssetManagement,product.category.validation.product.assetmanagement.enabled");
		}

		// Create Category WO asset management
		ItemTypeWS categoryWOAssetManagement = createItemType(false, false);
		// Persist
		Integer categoryWOAssetManagementId = api.createItemCategory(categoryWOAssetManagement);

		// Create Asset Managed Items
		ItemDTOEx assetItem = createItem(true, false, TEST_ASSET_ITEM_TYPE_ID);
		// Persist
		Integer assetItemId = api.createItem(assetItem);

		// Create Asset
		AssetWS asset = getAssetWS(assetItemId, STATUS_AVAILABLE_ID);
		Integer assetId = api.createAsset(asset);

		ItemDTOEx item = api.getItem(assetItemId, null, null);
		item.setTypes(new Integer[] {TEST_ASSET_ITEM_TYPE_ID, categoryWOAssetManagementId});
		api.updateItem(item);

		typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == categoryWOAssetManagementId) {
				categoryWOAssetManagement = itemTypeWS;
				break;
			}
		}

		categoryWOAssetManagement.setAllowAssetManagement(ENABLED);
		addAssetStatuses(categoryWOAssetManagement);

		logger.debug("#test017UpdateItemTypeAssetManagement. Product will have 2 categories with asset management");
		try {
			api.updateItemCategory(categoryWOAssetManagement);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "ItemTypeWS,allowAssetManagement,product.category.validation.multiple.linked.assetmanagement.types.error");
		}

		// Clean up
		api.deleteAsset(assetId);
		api.deleteItem(assetItemId);
		api.deleteItemCategory(categoryWOAssetManagementId);
	}

	@Test(groups = { "web-services", "asset"})
	public void test018GetAssetsForCategory() {

		Integer[] ids = api.getAssetsForCategory(TEST_ASSET_ITEM_TYPE_ID);
		assertEquals("Ids: "+Arrays.asList(ids), 0, ids.length);

		ids = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		assertEquals("Ids: "+Arrays.asList(ids), 0, ids.length);

		// Create Asset
		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);
		Integer assetId = api.createAsset(asset);

		ids = api.getAssetsForCategory(TEST_ASSET_ITEM_TYPE_ID);
		assertEquals("Ids: "+Arrays.asList(ids), 1, ids.length);
		assertEquals("Ids: "+Arrays.asList(ids), assetId, ids[0]);

		ids = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		assertEquals("Ids: "+Arrays.asList(ids), 1, ids.length);
		assertEquals("Ids: "+Arrays.asList(ids), assetId, ids[0]);

		// Create second asset for asset product
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_AVAILABLE_ID);
		asset.setIdentifier("Modified");
		Integer secondAssetId = api.createAsset(asset);

		List idList = Arrays.asList(api.getAssetsForCategory(TEST_ASSET_ITEM_TYPE_ID));
		assertEquals("Ids: "+idList, 2, idList.size());
		assertTrue("Ids: " + idList, idList.contains(assetId));
		assertTrue("Ids: "+idList,idList.contains(secondAssetId));

		idList = Arrays.asList(api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT));
		assertEquals(2, idList.size());
		assertTrue("Ids: " + idList, idList.contains(assetId));
		assertTrue("Ids: " + idList, idList.contains(secondAssetId));

		// Clean up
		api.deleteAsset(assetId);
		api.deleteAsset(secondAssetId);

		ids = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		assertEquals("Ids: " + Arrays.asList(ids), 0, ids.length);

		ids = api.getAssetsForCategory(TEST_ASSET_ITEM_TYPE_ID);
		assertEquals("Ids: "+Arrays.asList(ids), 0, ids.length);
	}

	@Test(groups = { "web-services", "asset"})
	public void test019AssetTransition() {

		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		Integer assetId = api.createAsset(asset);

		AssetWS savedAsset = api.getAsset(assetId);
		AssetTransitionDTOEx[] transitions = api.getAssetTransitions(assetId);
		assertEquals(1, transitions.length);
		assertEquals(STATUS_DEFAULT_ID, transitions[0].getNewStatusId());
		assertNotNull(transitions[0].getCreateDatetime());
		assertNotNull(transitions[0].getUserId());
		assertNull(transitions[0].getPreviousStatusId());
		assertNull(transitions[0].getAssignedToId());

		savedAsset.setIdentifier("test019AssetTransition");
		api.updateAsset(savedAsset);
		savedAsset = api.getAsset(assetId);

		transitions = api.getAssetTransitions(assetId);
		assertEquals(1, transitions.length);
		assertEquals(STATUS_DEFAULT_ID, transitions[0].getNewStatusId());

		savedAsset.setAssetStatusId(STATUS_AVAILABLE_ID);
		api.updateAsset(savedAsset);
		transitions = api.getAssetTransitions(assetId);
		assertEquals(2, transitions.length);
		int cnt = 0;
		for(AssetTransitionDTOEx ex: transitions) {
			if(ex.getNewStatusId().equals(STATUS_DEFAULT_ID)) {
				cnt += 1;
			} else if(ex.getNewStatusId().equals(STATUS_AVAILABLE_ID)) {
				cnt += 10;
				assertEquals(STATUS_DEFAULT_ID, ex.getPreviousStatusId());
				assertNotNull(ex.getCreateDatetime());
				assertNotNull(ex.getUserId());
				assertNull(ex.getAssignedToId());
			}
		}
		assertEquals("Not all statuses found ["+cnt+"]", 11, cnt);

		// Clean up
		api.deleteAsset(assetId);
	}

	@Test(groups = { "web-services", "asset"}  )
	public void test020BatchUpload() throws Exception {

		File sourceFile = File.createTempFile("testAsset", ".csv");
		writeToFile(sourceFile,
				"identifier,notes,INT1,Regulatory Code\n" +
						"Id1,Note1,1,01\",\"02\n" +
						"Id2,Note2,,01\n" +
						"Id3,Note3,,05\n"
		);
		File errorFile = File.createTempFile("testAssetError", ".csv");

		logger.debug("Source file: {}", sourceFile);
		logger.debug("Error file: {}", errorFile);
		api.startImportAssetJob(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, "identifier", "notes", "Global", "Entities", sourceFile.getAbsolutePath(), errorFile.getAbsolutePath());
		Thread.sleep(2000);

		String errors = FileUtils.readFileToString(errorFile);
		assertEquals("Errors was: "+errors, "", errors);

		Integer[] assetIds = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		int assetsFoundCount = 0;
		for(Integer assetId : assetIds) {
			AssetWS asset = api.getAsset(assetId);
			if("Id1".equals(asset.getIdentifier())) {
				assetsFoundCount += 1;
				assertEquals("Note1", asset.getNotes());
				int mfCnt = 0;
				for(MetaFieldValueWS fieldWS : asset.getMetaFields()) {
					if("INT1".equals(fieldWS.getFieldName())) {
						mfCnt += 1;
						assertEquals(1, (int)fieldWS.getIntegerValue());
					} else if("Regulatory Code".equals(fieldWS.getFieldName())) {
						mfCnt += 10;
						assertTrue(fieldWS.getListValueAsList().contains("01"));
						assertTrue(fieldWS.getListValueAsList().contains("02"));
					}
				}
				assertEquals("Not all metafields found ", 11, mfCnt);
			} else if("Id2".equals(asset.getIdentifier())) {
				assetsFoundCount += 10;
				assertEquals("Note2", asset.getNotes());
				int mfCnt = 0;
				for(MetaFieldValueWS fieldWS : asset.getMetaFields()) {
					if("INT1".equals(fieldWS.getFieldName())) {
						mfCnt += 1;
						assertEquals(5, (int)fieldWS.getIntegerValue());
					} else if("Regulatory Code".equals(fieldWS.getFieldName())) {
						mfCnt += 10;
						assertTrue(fieldWS.getListValueAsList().contains("01"));
					}
				}
				assertEquals("Not all metafields found ", 11, mfCnt);
			} else if("Id3".equals(asset.getIdentifier())) {
				assetsFoundCount += 100;
				assertEquals("Note3", asset.getNotes());
				int mfCnt = 0;
				for(MetaFieldValueWS fieldWS : asset.getMetaFields()) {
					if("INT1".equals(fieldWS.getFieldName())) {
						mfCnt += 1;
						assertEquals(5, (int)fieldWS.getIntegerValue());
					} else if("Regulatory Code".equals(fieldWS.getFieldName())) {
						mfCnt += 10;
						assertTrue(fieldWS.getListValueAsList().contains("05"));
					}
				}
				assertEquals("Not all metafields found ", 11, mfCnt);
			}
		}
		assertEquals("Assets found "+assetsFoundCount, 111, assetsFoundCount);

		writeToFile(sourceFile, "identifier,notes,Regulatory Code\n" +
				",Note,01\n" +
				"Id4,Note4,01\n" +
				"Id4,Note41,01\n"
		);

		api.startImportAssetJob(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, "identifier","notes", "Global", "Entities", sourceFile.getAbsolutePath(), errorFile.getAbsolutePath());
		Thread.sleep(2000);

		assetIds = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		assetsFoundCount = 0;
		for(Integer assetId : assetIds) {
			AssetWS asset = api.getAsset(assetId);
			if("Id4".equals(asset.getIdentifier())) {
				assetsFoundCount += 1;
				assertEquals("Note4", asset.getNotes());
			} else if("Id1".equals(asset.getIdentifier())) {
				assertEquals("Note1", asset.getNotes());
			}
			if(asset.getIdentifier().startsWith("Id") && asset.getIdentifier().length() == 3) {
				api.deleteAsset(assetId);
			}
		}
		assertEquals("Assets found "+assetsFoundCount, 1, assetsFoundCount);

		errors = FileUtils.readFileToString(errorFile);
		logger.debug("{}", errors);
		assertTrue(errors.contains("Id4,Note41,01,An asset with the identifier already exists"));
		assertTrue(errors.contains(",Note,01,The identifier must be between 1 and 200 characters long"));

		// Clean up
		FileUtils.deleteQuietly(errorFile);
		FileUtils.deleteQuietly(sourceFile);
	}

	@Test(groups = { "web-services", "asset"})
	public void test021UpdateItemTypeAssetManagementMetaFields() throws Exception {

		// Create Asset Managed Category
		ItemTypeWS assetCategory = createItemType(true, false);
		// Persist
		Integer assetCategoryId = api.createItemCategory(assetCategory);

		// Statuses
		Integer[] statusesIds = getAssetStatusesIds(assetCategoryId);
		Integer statusDefaultId = statusesIds[0];

		ItemTypeWS[] typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == assetCategoryId) {
				assetCategory = itemTypeWS;
				break;
			}
		}

		MetaFieldWS mf = new MetaFieldWS();
		BeanUtils.copyProperties(mf, assetCategory.getAssetMetaFields().iterator().next());
		mf.setName("Regulatory Code");
		mf.setId(0);

		assetCategory.getAssetMetaFields().add(mf);

		logger.debug("#test021UpdateItemTypeAssetManagementMetaFields. Two MetaFields with same name");
		try {
			api.updateItemCategory(assetCategory);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "MetaFieldWS,name,metaField.validation.name.unique,Regulatory Code");
		}

		// Create Asset Managed Item
		ItemDTOEx item = createItem(true, false, assetCategoryId);
		Integer itemId = api.createItem(item);

		AssetWS asset = getAssetWS(itemId, statusDefaultId);
		Integer assetId = api.createAsset(asset);

		AssetWS savedAsset = api.getAsset(assetId);

		typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == assetCategoryId) {
				assetCategory = itemTypeWS;
				break;
			}
		}

		String name = null;
		for(MetaFieldWS metaField: assetCategory.getAssetMetaFields()) {
			if(!metaField.getDataType().equals(DataType.STRING)) {
				metaField.setDataType(DataType.STRING);
				if(metaField.getDefaultValue() != null) {
					metaField.getDefaultValue().setValue("str");
				}
				name = metaField.getName();
				break;
			}
		}

		logger.debug("#test021UpdateItemTypeAssetManagementMetaFields. Can not change data type if meta field is in use");
		try {
			api.updateItemCategory(assetCategory);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "MetaFieldWS,dataType,metaField.validation.type.change.not.allowed,"+name);
		}


		typeWSs = api.getAllItemCategories();
		for(ItemTypeWS itemTypeWS : typeWSs) {
			if(itemTypeWS.getId().intValue() == assetCategoryId) {
				assetCategory = itemTypeWS;
				break;
			}
		}

		assertEquals(2, assetCategory.getAssetMetaFields().size());

		logger.debug("#test021UpdateItemTypeAssetManagementMetaFields. Remove meta fields");
		assetCategory.setAssetMetaFields(new HashSet(0));
		try {
			api.updateItemCategory(assetCategory);

		} catch (SessionInternalError error) {
			error.printStackTrace();
			fail(error.getMessage());
		}

		savedAsset = api.getAsset(assetId);

		assertEquals(0, savedAsset.getMetaFields().length);

		// Clean up
		api.deleteAsset(assetId);
		api.deleteItem(itemId);
		api.deleteItemCategory(assetCategoryId);
	}

	@Test
	public void test022CreateItemWithOrderLineMetaFields() {

		// Create Item
		ItemDTOEx item = createItem(false, false, TEST_ITEM_TYPE_ID);

		// Add MetaField Order Line
		MetaFieldWS metaField = new MetaFieldWS();
		metaField.setDataType(DataType.STRING);
		metaField.setDisabled(false);
		metaField.setDisplayOrder(1);
		metaField.setEntityId(PRANCING_PONY);
		metaField.setEntityType(EntityType.ORDER_LINE);
		metaField.setMandatory(false);
		metaField.setPrimary(false);
		metaField.setName("Item WS-022 orderLinesMetaField_1");
		item.setOrderLineMetaFields(new MetaFieldWS[]{metaField});

		logger.debug("Creating item {}", item);
		// Persist
		Integer itemId = api.createItem(item);
		assertNotNull("The item was not created", itemId);

		ItemDTOEx itemDtoEx = api.getItem(itemId, TEST_USER_ID, null);
		assertNotNull("Item orderLineMetaFields not fount (empty)", itemDtoEx.getOrderLineMetaFields());
		assertEquals("Item orderLineMetaFields size is incorrect", 1, itemDtoEx.getOrderLineMetaFields().length);
		assertEquals("Item orderLineMetaField is incorrect", metaField.getName(), itemDtoEx.getOrderLineMetaFields()[0].getName());

		itemDtoEx.getOrderLineMetaFields()[0].setName("Item WS-022 metaFieldChangedName");
		api.updateItem(itemDtoEx);

		itemDtoEx = api.getItem(itemId, TEST_USER_ID, null);
		assertNotNull("Item orderLineMetaFields not fount (empty)", itemDtoEx.getOrderLineMetaFields());
		assertEquals("Item orderLineMetaFields size is incorrect", 1, itemDtoEx.getOrderLineMetaFields().length);
		assertEquals("Item orderLineMetaField is incorrect", "Item WS-022 metaFieldChangedName", itemDtoEx.getOrderLineMetaFields()[0].getName());

		itemDtoEx.setOrderLineMetaFields(new MetaFieldWS[]{});

		api.updateItem(itemDtoEx);
		itemDtoEx = api.getItem(itemId, TEST_USER_ID, null);
		assertTrue("Item orderLineMetaFields should be empty", itemDtoEx.getOrderLineMetaFields() == null || itemDtoEx.getOrderLineMetaFields().length == 0);

		// Clean up
		api.deleteItem(itemId);
	}

	@Test(groups = {"web-services", "asset"})
	public void test023findItemsByMetaFields() {

		// Create Two Assets
		AssetWS firstAsset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		Integer firstAssetId = api.createAsset(firstAsset);

		AssetWS secondAsset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		secondAsset.setIdentifier("ASSET2");
		secondAsset.getMetaFields()[0].setListValue(new String[]{"03"});
		Integer secondAssetId = api.createAsset(secondAsset);

		logger.debug("id EQ");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("id", Filter.FilterConstraint.EQ, firstAssetId)});
		AssetSearchResult result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assertEquals(1, result.getObjects().length);

		logger.debug("Regulatory Code EQ");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("Regulatory Code", Filter.FilterConstraint.EQ, "01")});
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assertEquals(1, result.getObjects().length);
		assertEquals(firstAssetId, result.getObjects()[0].getId());

		logger.debug("Regulatory Code EQ");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("Regulatory Code", Filter.FilterConstraint.EQ, "03")});
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assertEquals(1, result.getObjects().length);
		assertEquals(secondAssetId, result.getObjects()[0].getId());

		logger.debug("identifier ILIKE");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("identifier", Filter.FilterConstraint.LIKE, "ASSET")});
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
        AssetWS[] assets = result.getObjects();
		assertEquals(2, assets.length);
		for(AssetWS asset: assets) {
			assertTrue(asset.getIdentifier().toLowerCase().indexOf("asset")>=0);
		}

		logger.debug("max");
		criteria.setMax(1);
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assets = result.getObjects();
		assertEquals(2, result.getTotal());
		assertEquals(1, assets.length);
		for(AssetWS asset: assets) {
			assertTrue(asset.getIdentifier().toLowerCase().indexOf("asset")>=0);
		}

		logger.debug("sort asc");
		criteria.setFilters(new BasicFilter[]{ new BasicFilter("identifier", Filter.FilterConstraint.LIKE, "ASSET")});
		criteria.setMax(0);
		criteria.setSort("identifier");
		criteria.setDirection(SearchCriteria.SortDirection.ASC);
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assets = result.getObjects();
		assertEquals("ASSET1", assets[0].getIdentifier());
		assertEquals("ASSET2", assets[1].getIdentifier());

		logger.debug("sort desc");
		criteria.setDirection(SearchCriteria.SortDirection.DESC);
		result = api.findAssets(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, criteria);
		assets = result.getObjects();
		assertEquals("ASSET2", assets[0].getIdentifier());
		assertEquals("ASSET1", assets[1].getIdentifier());

		// Clean up
		api.deleteAsset(firstAssetId);
		api.deleteAsset(secondAssetId);
	}

	@Test
	public void test024DeleteItemTypeWithStatuses() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("TmpAsstMgmgt4");
		type.setAssetIdentifierLabel("Lbl3");
		type.setOrderLineTypeId(1);
		type.setAllowAssetManagement(1);
		addAssetStatuses(type);
		int id = api.createItemCategory(type);
		api.deleteItemCategory(id);
	}


	@Test(groups = { "web-services", "asset"}  )
	public void test025AssetGroupBatchUpload() throws Exception {

		File sourceFile = File.createTempFile("testAsset", ".csv");
		writeToFile(sourceFile,
				"identifier,notes,INT1,Regulatory Code\n" +
						"Id11,Note1,1,01\",\"02\n" +
						"Id12,Note2,,01\n" +
						"Id13,Note3,,05\n"
		);
		File errorFile = File.createTempFile("testAssetError", ".csv");

		logger.debug("Source file: {}", sourceFile);
		logger.debug("Error file: {}", errorFile);
		api.startImportAssetJob(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, "identifier", "notes", "Global", "Entities", sourceFile.getAbsolutePath(), errorFile.getAbsolutePath());
		Thread.sleep(2000);

		String errors = FileUtils.readFileToString(errorFile);
		logger.debug("Errors: {}", errors);

		logger.debug("Initial assets uploaded");
		writeToFile(sourceFile,
				"identifier,Regulatory Code,Asset1,AssetProduct1,Asset3,AssetProduct3\n" +
						"Id14,01,Id11,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+",Id12,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+"\n" +
						"Id15,01,Id13,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+",Id12,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+"\n" +
						"Id16,01,Id13,"+TEST_ITEM_ID_WITH_ASSET_MANAGEMENT+",,\n"
		);

		api.startImportAssetJob(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, "identifier", "notes", "Global", "Entities", sourceFile.getAbsolutePath(), errorFile.getAbsolutePath());
		//Time interval should much enough for mysql when run test cases in group
		Thread.sleep(8000);

		errors = FileUtils.readFileToString(errorFile);
		logger.debug("Errors: {}", errors);
		assertTrue("Errors contained: "+errors, errors.contains("The asset [Id12] is already part of an asset group"));


		Integer[] assetIds = api.getAssetsForItem(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
		List assetsToDelete = Arrays.asList(new String[] {"Id14","Id16"});
		for(Integer assetId : assetIds) {
			AssetWS asset = api.getAsset(assetId);
			if(assetsToDelete.contains(asset.getIdentifier())) {
				api.deleteAsset(assetId);
			}
		}
		assetsToDelete = Arrays.asList(new String[] {"Id11","Id12","Id13"});
		for(Integer assetId : assetIds) {
			AssetWS asset = api.getAsset(assetId);
			if(assetsToDelete.contains(asset.getIdentifier())) {
				api.deleteAsset(assetId);
			}
		}
		// Clean up
		FileUtils.deleteQuietly(errorFile);
		FileUtils.deleteQuietly(sourceFile);
	}


	@Test(groups = { "web-services", "asset"} )
	public void test026AssetGroupCreate() {

		AssetWS asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("GID1");
		Integer assetId = api.createAsset(asset);
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("GID2");
		Integer assetId2 = api.createAsset(asset);
		asset = getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("GID3");
		Integer assetId3 = api.createAsset(asset);

		asset=getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("Group1");
		asset.setContainedAssetIds(new Integer[] {assetId, assetId2});
		Integer groupId = api.createAsset(asset);

		logger.debug("Create Asset Group");
		asset = api.getAsset(groupId);
		List assetIds = Arrays.asList(asset.getContainedAssetIds());
		assertTrue("Contained assets doesn not include id "+ assetId +" "+assetIds, assetIds.contains(assetId));
		assertTrue("Contained assets doesn not include id "+ assetId2 +" "+assetIds, assetIds.contains(assetId2));

		asset = api.getAsset(assetId);
		assertEquals(Constants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(assetId2);
		assertEquals(Constants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());

		logger.debug("Assign asset to group which already belongs to another group");
		asset=getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("Group2");
		asset.setContainedAssetIds(new Integer[] {assetId});
		try {
			api.createAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {          //The asset [ASSET1] is already part of an asset group.
			JBillingTestUtils.assertContainsError(error,  "AssetWS,containedAssets,asset.validation.group.linked,GID1");
		}

		logger.debug("Change status of asset belonging to a group");
		asset = api.getAsset(assetId);
		asset.setAssetStatusId(STATUS_AVAILABLE_ID);
		try {
			api.updateAsset(asset);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatus,asset.validation.status.change.internal");
		}

		logger.debug("Remove Asset from Group");
		asset = api.getAsset(groupId);
		asset.setContainedAssetIds(new Integer[] {assetId, assetId3});
		api.updateAsset(asset);

		asset = api.getAsset(assetId2);
		//it must have the default status
		assertEquals(STATUS_DEFAULT_ID, asset.getAssetStatusId());
		asset = api.getAsset(assetId3);
		assertEquals(Constants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());

		logger.debug("Add group to group");
		asset=getAssetWS(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT, STATUS_DEFAULT_ID);
		asset.setIdentifier("Group2");
		asset.setContainedAssetIds(new Integer[] {groupId});
		Integer groupId2 = api.createAsset(asset);

		asset = api.getAsset(assetId3);
		assertEquals(Constants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(assetId);
		assertEquals(Constants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(groupId);
		assertEquals(Constants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());

		logger.debug("Delete group");
		api.deleteAsset(groupId2);
		asset = api.getAsset(assetId3);
		assertEquals(Constants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(assetId);
		assertEquals(Constants.ASSET_STATUS_MEMBER_OF_GROUP, asset.getAssetStatusId());
		asset = api.getAsset(groupId);
		assertEquals(STATUS_DEFAULT_ID, asset.getAssetStatusId());

		logger.debug("Delete member of group");
		try {
			api.deleteAsset(assetId);
			fail("Exception expected");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "AssetWS,assetStatus,asset.validation.status.change.internal");
		}

		logger.debug("Delete group 2");
		api.deleteAsset(groupId);
		asset = api.getAsset(assetId3);
		assertEquals(STATUS_DEFAULT_ID, asset.getAssetStatusId());
		asset = api.getAsset(assetId);
		assertEquals(STATUS_DEFAULT_ID, asset.getAssetStatusId());

		api.deleteAsset(assetId3);
		api.deleteAsset(assetId2);
		api.deleteAsset(assetId);
	}

	@Test
	public void test023GetChildProductCategoryAndProduct() {

		// get original items
		Integer totalItemTypes = api.getAllItemCategoriesByEntityId(PRANCING_PONY).length;
		Integer totalItems = api.getAllItemsByEntityId(PRANCING_PONY).length;

		//Create an item type for child entity
		long rand = System.currentTimeMillis();
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("Root Category"+rand);
		itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		Integer itemTypeId = childApi.createItemCategory(itemType);

		//Create a item for child entity
		ItemDTOEx newItem = new ItemDTOEx();
		newItem.setDescription("A reseller item" + rand);
		newItem.setPriceModelCompanyId(new Integer(1));
		newItem.setPrice(new BigDecimal("30"));
		newItem.setNumber("RP-1");

		List<Integer> childEntities= new ArrayList<Integer>(1);
		childEntities.add(childApi.getCallerCompanyId());
		newItem.setEntities(childEntities);

		Integer types[] = new Integer[1];
		types[0] = itemTypeId;
		newItem.setTypes(types);
		Integer itemId = childApi.createItem(newItem);

		//verify item type
		List<ItemTypeWS> itemsReceived = Arrays.asList(api.getAllItemCategoriesByEntityId(PRANCING_PONY));
		assertEquals(totalItemTypes + 1, itemsReceived.size());

		boolean found = false;
		for(ItemTypeWS itemTypeRx: itemsReceived){
			if(itemTypeRx.getDescription().equals("Root Category"+rand)){
				assertEquals(Constants.ORDER_LINE_TYPE_ITEM, itemTypeRx.getOrderLineTypeId().intValue());
				found=true;
				break;
			}
		}
		assertEquals("Recently created item type not found",true,found);

		//verify item
		List<ItemDTOEx> receivedItems = Arrays.asList(api.getAllItemsByEntityId(PRANCING_PONY));
		assertEquals(totalItems + 1, receivedItems.size());

		found = false;
		for(ItemDTOEx item: receivedItems){
			if ( ("A reseller item" + rand).equals(item.getDescription()) ) {
				assertEquals("RP-1", item.getNumber());
				found = true;
				break;
			}
		}
		assertEquals("Recently created item not found",true,found);

		// tear down
		childApi.deleteItem(itemId);
		childApi.deleteItemCategory(itemTypeId);
	}

	@Test
	public void test024CreateItemDependenciesWithQuantities() {

		// Create Item
		ItemDTOEx item = createItem(false, false, TEST_ITEM_TYPE_ID);

		// Create Dependent item
		ItemDTOEx dependentItem = createItem(false, false, TEST_ITEM_TYPE_ID);
		// Persist
		Integer dependentItemId = api.createItem(dependentItem);

		ItemDependencyDTOEx dep1 = new ItemDependencyDTOEx();
		dep1.setDependentId(dependentItemId);   //Currency test item
		dep1.setMinimum(-1);
		dep1.setType(ItemDependencyType.ITEM);
		item.setDependencies(new ItemDependencyDTOEx[]{dep1});

		logger.debug("Creating item {}", item);
		try {
			api.createItem(item);
			fail("Exception expected");
		} catch (SessionInternalError e) {
			JBillingTestUtils.assertContainsAnyError(e, Arrays.asList("ItemDTOEx,dependencies.minimum,validation.error.min,0", "ItemDTOEx,dependencies.minimum,validation.error.notnull") );
		}

		dep1.setMinimum(3);
		dep1.setMaximum(2);
		try {
			api.createItem(item);
			fail("Exception expected");
		} catch (SessionInternalError e) {
			JBillingTestUtils.assertContainsError(e, "ItemDTOEx,dependencies,product.validation.dependencies.max.lessthan.min" );
		}

		dep1.setMinimum(2);
		dep1.setMaximum(3);

		// Persist
		Integer itemId = api.createItem(item);

        // tear down
        ItemDTOEx persistedItem = api.getItem(itemId, TEST_USER_ID, null);
        persistedItem.setDependencies(null);
        api.updateItem(persistedItem);
        api.deleteItem(itemId);
		api.deleteItem(dependentItemId);
	}

	@Test
	public void test025PlanValidityDates() {
		String count = Short.toString((short)System.currentTimeMillis());

//        I'm creating a (nested) Gold service plan which has the following products:
//                - SMS Service (bundled quantity=1, period = monthly)
//                - GPRS Service (bundled quantity=1, period = monthly)
//                - SMS to NA (bundled quantity=0, period = monthly)

        ItemDTOEx smsServiceItem = new ItemDTOEx();
        smsServiceItem.setDescription("SMS Service");
        smsServiceItem.setEntityId(PRANCING_PONY);
        smsServiceItem.setTypes(new Integer[]{TEST_ITEM_TYPE_ID});
        smsServiceItem.setPrice("1");
        smsServiceItem.setNumber("SMS-"+count);
        smsServiceItem.setActiveSince(Util.getDate(2010, 1, 1));
        smsServiceItem.setActiveUntil(Util.getDate(2010, 9, 1));
        Integer smsServiceItemId = api.createItem(smsServiceItem);
        smsServiceItem.setId(smsServiceItemId);

        ItemDTOEx gprsServiceItem = new ItemDTOEx();
        gprsServiceItem.setDescription("GPRS Service");
        gprsServiceItem.setEntityId(PRANCING_PONY);
        gprsServiceItem.setTypes(new Integer[]{TEST_ITEM_TYPE_ID});
        gprsServiceItem.setPrice("1");
        gprsServiceItem.setNumber("GPRS-"+count);
        Integer gprsServiceItemId = api.createItem(gprsServiceItem);
        gprsServiceItem.setId(gprsServiceItemId);

        ItemDTOEx goldServiceItem = new ItemDTOEx();
        goldServiceItem.setDescription("Gold Service Plan");
        goldServiceItem.setEntityId(PRANCING_PONY);
        goldServiceItem.setTypes(new Integer[]{TEST_ITEM_TYPE_ID});
        goldServiceItem.setPrice("1");
        goldServiceItem.setNumber("GSP-"+count);
        goldServiceItem.setActiveSince(Util.getDate(2010, 1, 1));
        goldServiceItem.setActiveUntil(Util.getDate(2010, 10, 1));

        Integer goldServiceItemId = api.createItem(goldServiceItem);

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, 1);
        SortedMap<Date, PriceModelWS> models = new TreeMap<Date, PriceModelWS>();
        models.put(Constants.EPOCH_DATE, priceModel);

        PlanItemBundleWS bundle1 = new PlanItemBundleWS();
        bundle1.setPeriodId(2);
        bundle1.setQuantity(BigDecimal.ONE);
        PlanItemWS pi1 = new PlanItemWS();
        pi1.setItemId(smsServiceItemId);
        pi1.setPrecedence(-1);
        pi1.setModels(models);
        pi1.setBundle(bundle1);

        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(2);
        bundle2.setQuantity(BigDecimal.ONE);
        PlanItemWS pi2 = new PlanItemWS();
        pi2.setItemId(gprsServiceItemId);
        pi2.setPrecedence(-1);
        pi2.setModels(models);
        pi2.setBundle(bundle2);


        Integer goldServicePlanId = 0;
        PlanWS goldServicePlan = new PlanWS();
        goldServicePlan.setItemId(goldServiceItemId);
        goldServicePlan.setDescription("Gold Service Plan");
        goldServicePlan.setPeriodId(2);
        goldServicePlan.addPlanItem(pi1);
        goldServicePlan.addPlanItem(pi2);

        //plan valid dates ends after bundled item
        logger.debug("plan valid dates ends after bundled item");
        try {
            api.createPlan(goldServicePlan);
            fail("Plan has invalid dates");
        } catch (SessionInternalError e) {
            JBillingTestUtils.assertContainsError(e, "PlanWS,planItems,validation.error.plan.planItem.expired,SMS-"+count);
        }


        //plan valid dates begin before bundled item
        logger.debug("plan valid dates begin before bundled item ");
        goldServiceItem = api.getItem(goldServiceItemId, null, null);
        goldServiceItem.setActiveUntil(null);
        goldServiceItem.setActiveSince(Util.getDate(2009, 1, 1));
        api.updateItem(goldServiceItem);

        try {
            api.createPlan(goldServicePlan);
            fail("Plan has invalid dates");
        } catch (SessionInternalError e) {
            JBillingTestUtils.assertContainsError(e, "PlanWS,planItems,validation.error.plan.planItem.expired,SMS-"+count);
        }

        //clear plan item dates
        goldServiceItem.setActiveUntil(null);
        goldServiceItem.setActiveSince(null);
        api.updateItem(goldServiceItem);

        //plan must be created and get start and end date of SMS service item
        logger.debug("//plan must be created and get start and end date of SMS service item ");
        goldServiceItem = api.getItem(goldServiceItemId, null, null);
        goldServiceItem.setActiveUntil(null);
        api.updateItem(goldServiceItem);
        goldServicePlanId = api.createPlan(goldServicePlan);
        goldServicePlan = api.getPlanWS(goldServicePlanId);
        goldServiceItem = api.getItem(goldServiceItemId, null, null);
        assertNotNull(goldServiceItem.getActiveUntil());
        assertNotNull(goldServiceItem.getActiveSince());
        assertEquals(smsServiceItem.getActiveUntil(), goldServiceItem.getActiveUntil());
        assertEquals(smsServiceItem.getActiveSince(), goldServiceItem.getActiveSince());

		api.deletePlan(goldServicePlanId);

		//set valid dates exactly at exactly right boundary
		logger.debug("//set valid dates exactly at exactly right boundary ");
		goldServiceItem.setActiveSince(Util.getDate(2010, 1, 1));
		goldServiceItem.setActiveUntil(Util.getDate(2010, 9, 1));
		api.updateItem(goldServiceItem);

		goldServicePlanId = api.createPlan(goldServicePlan);


		smsServiceItem.setActiveSince(Util.getDate(2010, 2, 1));
		try {
			api.updateItem(smsServiceItem);
			fail("Item has invalid dates");
		} catch (SessionInternalError e) {
			JBillingTestUtils.assertContainsError(e, "ItemDTOEx,activeSince,validation.error.item.activeSince.plan.inactive,GSP-"+count);
		}

		api.deletePlan(goldServicePlanId);
		api.deleteItem(smsServiceItemId);
		api.deleteItem(goldServiceItemId);
		api.deleteItem(gprsServiceItemId);
	}

	@Test
	public void test026FindCategoryById() throws Exception{
		// Get an API instance
		JbillingAPI api = JbillingAPIFactory.getAPI();
		// Find category
		final Integer DRINK_PASSES = Integer.valueOf(1);
		ItemTypeWS itemTypeWS = api.getItemCategoryById(DRINK_PASSES);

		// Free not null check
		assertNotNull(itemTypeWS);

		// Check the id
		Integer categoryId = itemTypeWS.getId();
		assertNotNull("There must be id for each entity!!!", categoryId);
		assertTrue("Entity Id must be positive number!!!", categoryId > Integer.valueOf(0));

		// Check the description
		String description = itemTypeWS.getDescription();
		assertNotNull("This category must have description!!!", description);
		assertEquals("Not a provided description!!!", description.toUpperCase(), "Drink passes".toUpperCase());

		// Check the order line type (ITEM = 1)
		Integer orderLineTypeId = itemTypeWS.getOrderLineTypeId();
		assertNotNull("This category must have order line type!!!", orderLineTypeId);
		assertEquals("Category not of type ITEM!!!", orderLineTypeId, Integer.valueOf(1));

		// Check the company Id (entity)
		Integer entityId = itemTypeWS.getEntityId();
		assertNotNull("This category must have entityId!!!", entityId);
		assertEquals("Category not in Prancing Pony!!!", orderLineTypeId, Integer.valueOf(1));
	}

	@Test
	public void test027AssetReservation() throws Exception {

        /* ASSET CREATION */
		Integer categoryID = this.createCategoryWithAssetManagement();
		Integer itemID = this.createItem(categoryID);
		Integer assetID = this.createAsset(itemID, categoryID, ASSET_STATUS_AVAILABLE);
		AssetWS assetAvailable = api.getAsset(assetID);
		assertEquals("The asset status is AVAILABLE.",assetAvailable.getAssetStatusId(),this.getStatus(categoryID,ASSET_STATUS_AVAILABLE));

        /* ASSET RESERVATION */
		api.reserveAsset(assetAvailable.getId(),2);
		UserWS user = CreateObjectUtil.createUser(true, null, null);
		OrderWS order = CreateObjectUtil.createOrderObject(
				user.getId(), CURRENCY_USD, Constants.ORDER_BILLING_PRE_PAID,
				Constants.ORDER_PERIOD_ONCE, new DateMidnight(2013, 01, 21).toDate());

		OrderLineWS[] lines = new OrderLineWS[1];
		OrderLineWS line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Fist line");
		line.setItemId(itemID);
		line.setAssetIds(new Integer[] {assetID});
		lines[0] = line;

		order.setOrderLines(lines);

		try {
			api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			fail("Already reserved for different customer");
		} catch (SessionInternalError error) {
			JBillingTestUtils.assertContainsError(error, "OrderLineWS,assetIds,validation.asset.status.reserved");
		}

        /* ASSET RELEASE */
		api.releaseAsset(assetAvailable.getId(),2);
		Integer orderId = null;
		try {
			orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			assertNotNull("Order is created", orderId);
		} finally {
			if(user!=null) api.deleteUser(user.getId());
			if(orderId!=null) api.deleteOrder(orderId);
		}
	}

	/**
	 * JBFC-583
	 * This test case is added to check the accessibility of root
	 * company's global category/product/asset from child company
	 */
	@Test
	public void test028AccessRootCompanyItemFromChildCompany() throws Exception {
		long currentTime = System.currentTimeMillis();

		// Creating global asset managed item category
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("Global Asset Managed Category " + currentTime);
		itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		itemType.setAllowAssetManagement(1);
		itemType.setGlobal(true);
		itemType.setAssetStatuses(this.createAssetStatuses());
		Integer categoryID = api.createItemCategory(itemType);
		assertNotNull(categoryID);

		// Create global asset managed Item
		ItemDTOEx item = new ItemDTOEx();
		item.setGlobal(true);
		item.setDescription("Global Asset Managed Item " + currentTime);
		item.setNumber("GAMI-028");
		item.setAssetManagementEnabled(1);
		item.setTypes(new Integer[]{categoryID});
		Integer itemID = api.createItem(item);
		assertNotNull(itemID);

		// Create global asset number
		AssetWS asset = new AssetWS();
		asset.setItemId(itemID);
		asset.setAssetStatusId(this.getStatus(categoryID, ASSET_STATUS_AVAILABLE));
		asset.setIdentifier("Global-Asset-" + currentTime);
		asset.setEntityId(1);
		asset.setDeleted(0);
		asset.setGlobal(true);
		Integer assetID = api.createAsset(asset);
		assertNotNull(assetID);

		// Accessing parent company's category/item/asset from child company
		try {
			ItemDTOEx[] categoryFromChild = childApi.getItemByCategory(categoryID);
			assertNotNull(categoryFromChild);
			ItemDTOEx itemFromChild = childApi.getItem(itemID, null, null);
			assertNotNull(itemFromChild);
			AssetWS assetFromChild = childApi.getAsset(assetID);
			assertNotNull(assetFromChild);
		} catch (SecurityException ex) {
			fail("Root company's category/item/asset are not accessible from child company. "+ex.getMessage());
		} finally {
			// Clean up
			api.deleteAsset(assetID);
			api.deleteItem(itemID);
			api.deleteItemCategory(categoryID);
		}
	}

	protected static AssetWS getAssetWS(Integer itemId, Integer statusId) {
		AssetWS asset = new AssetWS();
		asset.setEntityId(PRANCING_PONY);
		asset.setIdentifier("ASSET1");
		asset.setItemId(itemId);
		asset.setNotes("NOTE1");
		asset.setAssetStatusId(statusId);
		asset.setDeleted(DISABLED);
		MetaFieldValueWS mf = new MetaFieldValueWS();
		mf.setFieldName("Regulatory Code");
		mf.getMetaField().setDataType(DataType.LIST);
		mf.setListValue(new String[] {"01", "02"});
		asset.setMetaFields(new MetaFieldValueWS[]{mf});
		return asset;
	}

	private void writeToFile(File file, String content) throws IOException {
		FileWriter fw = new FileWriter(file);
		fw.write(content);
		fw.close();
	}

	protected static ItemTypeWS createItemType(boolean allowAssetManagement, boolean global){
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("TestCategory: " + System.currentTimeMillis());
		itemType.setEntityId(PRANCING_PONY);
		if(global) {
			itemType.setGlobal(global);
		} else {
			itemType.setEntities(new ArrayList<Integer>(PRANCING_PONY));
		}
		itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		if(allowAssetManagement) {
			itemType.setAllowAssetManagement(ENABLED);
			itemType.setAssetIdentifierLabel("Test Asset Label");
			addAssetStatuses(itemType);
			itemType.setAssetMetaFields(createAssetMetaField());
		}
		return itemType;
	}

	protected static Set<MetaFieldWS> createAssetMetaField() {
		Set<MetaFieldWS> metaFields = new HashSet<MetaFieldWS>();

		// First Meta Filed
		MetaFieldWS metaField = new MetaFieldWS();
		metaField.setDataType(DataType.LIST);
		metaField.setName("Regulatory Code");
		metaField.setDisabled(false);
		metaField.setDisplayOrder(1);
		metaField.setEntityId(PRANCING_PONY);
		metaField.setEntityType(EntityType.ASSET);
		metaField.setMandatory(true);
		metaField.setPrimary(false);

		metaFields.add(metaField);

		// Second Meta Field
		metaField = new MetaFieldWS();
		metaField.setDataType(DataType.INTEGER);
		metaField.setName("INT1");
		metaField.setDisabled(false);
		metaField.setDisplayOrder(2);
		metaField.setEntityId(1);
		metaField.setEntityType(EntityType.ASSET);
		metaField.setMandatory(false);
		metaField.setPrimary(false);

		MetaFieldValueWS valueWS = new MetaFieldValueWS();
		valueWS.setIntegerValue(5);
		valueWS.setFieldName("INT1");
		valueWS.getMetaField().setDataType(DataType.INTEGER);

		metaField.setDefaultValue(valueWS);

		metaFields.add(metaField);

		return metaFields;
	}

	protected static void addAssetStatuses(ItemTypeWS itemType){
		if(null == itemType) {
			fail("Can not add statuses on null object!!");
		}
		if (!ENABLED.equals(itemType.getAllowAssetManagement())){
			fail("Can not add statuses on category that is not asset managed!!!");
		}

		// Default
		AssetStatusDTOEx status = new AssetStatusDTOEx();
		status.setDescription("One");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(ENABLED);
		status.setIsOrderSaved(DISABLED);
		status.setIsActive(DISABLED);
		status.setIsPending(DISABLED);
		itemType.getAssetStatuses().add(status);

		// Available
		status = new AssetStatusDTOEx();
		status.setDescription("Two");
		status.setIsAvailable(ENABLED);
		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(DISABLED);
		status.setIsActive(DISABLED);
		status.setIsPending(DISABLED);
		itemType.getAssetStatuses().add(status);

		// Order Saved
		status = new AssetStatusDTOEx();
		status.setDescription("Three");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(ENABLED);
		status.setIsActive(ENABLED);
		status.setIsPending(DISABLED);
		itemType.getAssetStatuses().add(status);

		// Reserved
		status = new AssetStatusDTOEx();
		status.setDescription("Four");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(DISABLED);
		status.setIsActive(DISABLED);
		status.setIsPending(DISABLED);
		itemType.getAssetStatuses().add(status);

		// Pending
		status = new AssetStatusDTOEx();
		status.setDescription("Five");
		status.setIsAvailable(DISABLED);
		status.setIsDefault(DISABLED);
		status.setIsOrderSaved(ENABLED);
		status.setIsActive(DISABLED);
		status.setIsPending(ENABLED);
		itemType.getAssetStatuses().add(status);

	}

	protected static ItemDTOEx createItem(boolean allowAssetManagement, boolean global, Integer... types){
		ItemDTOEx item = new ItemDTOEx();
		item.setDescription("TestItem: " + System.currentTimeMillis());
		item.setNumber("TestWS-" + System.currentTimeMillis());
		item.setTypes(types);
		if(allowAssetManagement){
			item.setAssetManagementEnabled(ENABLED);
		}
		item.setExcludedTypes(new Integer[]{});
		item.setHasDecimals(DISABLED);
		if(global) {
			item.setGlobal(global);
		} else {
			item.setGlobal(false);
		}
		item.setDeleted(DISABLED);
		item.setEntityId(PRANCING_PONY);
		ArrayList<Integer> entities = new ArrayList<Integer>();
		entities.add(PRANCING_PONY);
		item.setEntities(entities);
		return item;
	}

	private void matchItems(ItemDTOEx expected, ItemDTOEx actual) {
		// ID
		assertEquals("The ID of two same items differs!!", expected.getId(), actual.getId());
		// Code
		assertEquals("The Number of two same items differs!!", expected.getNumber(), actual.getNumber());
		// Excluded Types
		if(null != expected.getExcludedTypes() && null != actual.getExcludedTypes()){
			if(expected.getExcludedTypes().length != actual.getExcludedTypes().length){
				fail("The number of excluded types differs!!!");
			}
			for (int i = 0; i < expected.getExcludedTypes().length; i++){
				assertEquals(String.format("Excluded Types at %d position Differs!!", i), expected.getExcludedTypes()[i], actual.getExcludedTypes()[i]);
			}
		}
		else if(null == expected.getExcludedTypes() ^ null == actual.getExcludedTypes()){
			fail(String.format("Expected Excluded Types are %s\nActual Excluded Types are %s",
					expected.getExcludedTypes(), actual.getExcludedTypes()));
		}
		// Has Decimals
		assertEquals("Decimals flag of two same items differs!!", expected.getHasDecimals(), actual.getHasDecimals());
		// Standard Availability
		assertEquals("Is Standard Availability flag of two same Items do not match!!", expected.isStandardAvailability(), actual.isStandardAvailability());
		// Global Flag
		assertEquals("Global flag of two same Items do not match!!", expected.isGlobal(), actual.isGlobal());
		// Deleted
		assertEquals("Deleted flag of new item can not be different than 0!!", Integer.valueOf(0), actual.getDeleted());
		// Asset Management Flag
		assertEquals("Asset Management flag of two same items differs!!", expected.getAssetManagementEnabled(), actual.getAssetManagementEnabled());
		// Entity ID
		assertEquals("Entity ID of two same items differs!!", expected.getEntityId(), actual.getEntityId());
		// Entities
		if(null != expected.getEntities() && null != actual.getEntities()){
			if(expected.getEntities().size() != actual.getEntities().size()){
				fail("The number of Entities differs!!!");
			}
			for (int i = 0; i < expected.getEntities().size(); i++){
				assertEquals(String.format("Entities at %d position differs!!", i), expected.getEntities().get(i), actual.getEntities().get(i));
			}
		}
		else if(null == expected.getEntities() ^ null == actual.getEntities()){
			fail(String.format("Expected Entities are %s\nActual Entities are %s",
					expected.getEntities(), actual.getEntities()));
		}
		// Description
		assertEquals("Description of two same items do not match!!", expected.getDescription(), actual.getDescription());
		// Types (ItemTypes)
		if(null != expected.getTypes() && null != actual.getTypes()){
			if(expected.getTypes().length != actual.getTypes().length){
				fail("The number of types differs!!!");
			}
			for (int i = 0; i < expected.getTypes().length; i++){
				assertEquals(String.format("Types at %d position Differs!!", i), expected.getTypes()[i], actual.getTypes()[i]);
			}
		}
		else if(null == expected.getTypes() ^ null == actual.getTypes()){
			fail(String.format("Expected Types are %s\nActual Types are %s",
					expected.getTypes(), actual.getTypes()));
		}
		// International Descriptions
		if(null != expected.getDescriptions() && null != actual.getDescriptions()){
			if(expected.getDescriptions().size() != actual.getDescriptions().size()){
				fail("The number of descriptions differs!!!");
			}
			for (int i = 0; i < expected.getDescriptions().size(); i++){
				matchInternationalDescriptions(expected.getDescriptions().get(i), actual.getDescriptions().get(i));
			}
		}
		else if(null == expected.getDescriptions() ^ null == actual.getDescriptions()){
			fail(String.format("Expected Descriptions are %s\nActual Descriptions are %s",
					expected.getDescriptions(), actual.getDescriptions()));
		}
	}

	private void matchInternationalDescriptions(InternationalDescriptionWS expected, InternationalDescriptionWS actual){
		assertEquals("Pseudo Column of two same international descriptions do not match!!", expected.getPsudoColumn(), actual.getPsudoColumn());
		assertEquals("Language ID of two same international descriptions do not match!!", expected.getLanguageId(), actual.getLanguageId());
		assertEquals("Content of two same international descriptions do not match!!", expected.getContent(), actual.getContent());
		assertEquals("Delete flag of two same international descriptions do not match!!", expected.isDeleted(), actual.isDeleted());
	}

	private void matchItemTypes(ItemTypeWS expected, ItemTypeWS actual, boolean assetManagedCategories){
		assertEquals("The ID of two same categories differs!!", expected.getId(), actual.getId());
		assertEquals("The description of two same categories differs!!", expected.getDescription(), actual.getDescription());
		assertEquals("The Order Line Type ID of two same categories differs!!", expected.getOrderLineTypeId(), actual.getOrderLineTypeId());
		assertEquals("The Parent Type ID of two same categories differs!!", expected.getParentItemTypeId(), actual.getParentItemTypeId());
		assertEquals("The global flag of two same categories differs!!", expected.isGlobal(), actual.isGlobal());
		assertEquals("The internal flag of two same categories differs!!", expected.isInternal(), actual.isInternal());
		assertEquals("The Entity ID of two same categories differs!!", expected.getEntityId(), actual.getEntityId());
		for (int i = 0; i < expected.getEntities().size(); i++){
			assertEquals("The Child Entity ID of two same categories differs!!", expected.getEntities().get(i), actual.getEntities().get(i));
		}
		if(assetManagedCategories){
			assertEquals("Asset management not enabled in two same categories!!", expected.getAllowAssetManagement(), actual.getAllowAssetManagement());
			assertEquals("Asset Identifier Label not the same for two same categories!!", expected.getAssetIdentifierLabel(), actual.getAssetIdentifierLabel());
			for (Iterator<AssetStatusDTOEx> i1 = expected.getAssetStatuses().iterator(), i2 = actual.getAssetStatuses().iterator();
			     i1.hasNext() && i2.hasNext();){
				matchAssetStatuses(i1.next(), i2.next());
			}
		}
		if(null != expected.getAssetMetaFields() && null != actual.getAssetMetaFields()){
			assertEquals("The number of meta fields differs!!", expected.getAssetMetaFields().size(), actual.getAssetMetaFields().size());
			for (Iterator<MetaFieldWS> i1 = expected.getAssetMetaFields().iterator(), i2 = actual.getAssetMetaFields().iterator();
			     i1.hasNext() && i2.hasNext();){
				matchMetaFields(i1.next(), i2.next());
			}
		}
		assertEquals("The One Per Customer flag of two same categories differs!!", expected.isOnePerCustomer(), actual.isOnePerCustomer());
		assertEquals("The One Per Order flag of two same categories differs!!", expected.isOnePerOrder(), actual.isOnePerOrder());
	}

	private void matchAssetStatuses(AssetStatusDTOEx expected, AssetStatusDTOEx actual){
		assertEquals("The ID of two same asset statuses differs!!", expected.getId(), actual.getId());
		assertEquals("The description of two same asset statuses differs!!", expected.getDescription(), actual.getDescription());
		assertEquals("Is default flag of two same asset statuses differs!!", expected.getIsDefault(), actual.getIsDefault());
		assertEquals("Is available flag of two same asset statuses differs!!", expected.getIsAvailable(), actual.getIsAvailable());
		assertEquals("Is orderSaved flag of two same asset statuses differs!!", expected.getIsOrderSaved(), actual.getIsOrderSaved());
		assertEquals("Is internal flag of two same asset statuses differs!!", expected.getIsInternal(), actual.getIsInternal());
	}

	private void matchMetaFields(MetaFieldWS expected, MetaFieldWS actual){
		assertEquals("Meta Field names differs!!!", expected.getName(), actual.getName());

		if (expected.getFieldUsage() != null && actual.getFieldUsage() != null) {
			assertEquals(expected.getFieldUsage(), actual.getFieldUsage());
		} else if (expected.getFieldUsage() == null ^ actual.getFieldUsage() == null) {
			fail("Field usage is: " + expected.getFieldUsage() + " and retrieved field usage is: " + actual.getFieldUsage());
		}

		if (expected.getValidationRule() != null && actual.getValidationRule() != null) {
			matchValidationRule(expected.getValidationRule(), actual.getValidationRule());
		} else if (expected.getValidationRule() == null ^ actual.getValidationRule() == null) {
			fail("Validation rule is: " + expected.getValidationRule() + " and retrieved validation rule is: " + actual.getValidationRule());
		}
		assertEquals(expected.getDataType(), actual.getDataType());
		assertEquals(expected.getDefaultValue(), actual.getDefaultValue());
		assertEquals(expected.getDisplayOrder(), actual.getDisplayOrder());
	}

	private void matchValidationRule(ValidationRuleWS expected, ValidationRuleWS actual) {
		assertTrue("Can not validate null objects!!", expected != null && actual != null);
		assertEquals("Validation rule types differs!!", expected.getRuleType(), actual.getRuleType());
		assertEquals("Error messages length differs!!", expected.getErrorMessages().size(), actual.getErrorMessages().size());
		assertEquals("Rule Attributes differs!!", expected.getRuleAttributes(), actual.getRuleAttributes());
	}

	private Integer[] getAssetStatusesIds(Integer itemTypeId){
		if(null == itemTypeId){
			fail("Can not search for a status in Item Type with null id!!");
		}

		ItemTypeWS assetCategory = null;
		// Find the category with the itemTypeId
		ItemTypeWS[] itemTypes = api.getAllItemCategoriesByEntityId(PRANCING_PONY);
		for (ItemTypeWS itemType : itemTypes){
			if(itemType.getId().equals(itemTypeId)){
				assetCategory = itemType;
			}
		}
		Integer[] statuses = new Integer[4];
		// If category not found return 0
		if(null != assetCategory){
			for (AssetStatusDTOEx status : assetCategory.getAssetStatuses()){
				if(1 == status.getIsDefault()){
					statuses[0] = status.getId();
				}
				else if(1 == status.getIsAvailable()){
					statuses[1] = status.getId();
				}
				else if(1 == status.getIsOrderSaved() && 1 == status.getIsActive()){
					statuses[2] = status.getId();
				}
				else if(1 == status.getIsOrderSaved() && 1 == status.getIsPending()){
					statuses[3] = status.getId();
				}
			}
		}
		assertEquals("Not all asset statuses found!!!", Integer.valueOf(4), Integer.valueOf(statuses.length));
		return statuses;
	}


    @Test
    public void test028GlobalValueUsingGetAllItemCategories() throws Exception{

        ItemTypeWS simpleItemType = createItemType(false, false);
        simpleItemType.setDescription("simpleCategory"+System.currentTimeMillis());
        api.createItemCategory(simpleItemType);

        ItemTypeWS assetItemType = createItemType(true, false);
        assetItemType.setDescription("assetItemType"+System.currentTimeMillis());
        api.createItemCategory(assetItemType);

        ItemTypeWS globalItemType = createItemType(false, true);
        globalItemType.setDescription("globalItemType"+System.currentTimeMillis());
        api.createItemCategory(globalItemType);

        ItemTypeWS globalAndAssetItemType = createItemType(true, true);
        globalAndAssetItemType.setDescription("globalAndAssetItemType"+System.currentTimeMillis());
        api.createItemCategory(globalAndAssetItemType);

        ItemTypeWS[] itemTypes = api.getAllItemCategories();
        for(ItemTypeWS item: itemTypes){
            if(item.getDescription().startsWith("simpleCategory")){
                assertFalse("SimpleCategory should be non global", item.isGlobal());
                assertEquals("AssetManagement should be Disable on SimpleCategory", item.getAllowAssetManagement(), DISABLED);
            }
            else if(item.getDescription().startsWith("assetItemType")){
                assertFalse("AssetItemType should be non global", item.isGlobal());
                assertEquals("AssetManagement should be Enable on AssetItemType", item.getAllowAssetManagement(), ENABLED);
            }
            else if(item.getDescription().startsWith("globalItemType")){
                assertTrue("GlobalItemType should be global", item.isGlobal());
                assertEquals("AssetManagement should be Disable on GlobalItemType", item.getAllowAssetManagement(), DISABLED);
            }
            else if(item.getDescription().startsWith("globalAndAssetItemType")){
                assertTrue("GlobalAndAssetItemType should be global", item.isGlobal());
                assertEquals("AssetManagement should be Enable on GlobalAndAssetItemType", item.getAllowAssetManagement(), ENABLED);
            }
        }
    }

    @Test(groups = { "web-services", "asset"})
    public void test029UpdateAssetMetafield() throws Exception {

        // Create Asset
        AssetWS asset = new AssetWS();
        asset.setEntityId(PRANCING_PONY);
        asset.setIdentifier("ASSET1");
        asset.setItemId(TEST_ITEM_ID_WITH_ASSET_MANAGEMENT);
        asset.setNotes("NOTE1");
        asset.setAssetStatusId(STATUS_DEFAULT_ID);
        asset.setDeleted(DISABLED);
        asset.setIdentifier("ID21234");

        MetaFieldValueWS mf = new MetaFieldValueWS();
        mf.setFieldName("Regulatory Code");
        mf.getMetaField().setDataType(DataType.LIST);
        mf.setListValue(new String[] {"01", "02"});

        MetaFieldValueWS mf2 = new MetaFieldValueWS();
        mf2.setFieldName("INT1");
        mf2.getMetaField().setDataType(DataType.INTEGER);
        mf2.setIntegerValue(4);

        asset.setMetaFields(new MetaFieldValueWS[]{mf,mf2});
        // Persist Asset
        Integer assetId = api.createAsset(asset);

        // Get saved asset
        AssetWS savedAsset = api.getAsset(assetId);
        Optional<MetaFieldValueWS> int1FieldValue = Arrays.stream(savedAsset.getMetaFields())
                .filter(field -> field.getFieldName().contains("INT1")).findFirst();
        int1FieldValue.get().setIntegerValue(null);
        try {
            api.updateAsset(savedAsset);
            int totalMf = api.getAsset(assetId).getMetaFields().length;
            assertEquals(1,totalMf);
            assertNotNull("Regulatory Code MF should be present",Arrays.stream(savedAsset.getMetaFields())
            .filter(field -> field.getFieldName().contains("Regulatory Code")).findFirst());
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "AssetWS,identifier,asset.validation.duplicate.identifier");
        }
        api.deleteAsset(assetId);
    }


	private Integer createAsset(Integer itemID, Integer categoryID, String statusDesc) throws IOException, JbillingAPIException {
		AssetWS asset = new AssetWS();
		asset.setItemId(itemID);
		asset.setAssetStatusId(this.getStatus(categoryID, statusDesc));
		asset.setIdentifier("asset-identifier-for-asset-reservation-" + Math.random() * 10000);
		asset.setEntityId(1);
		asset.setDeleted(0);
		return api.createAsset(asset);
	}
	
	private Integer getStatus(Integer categoryID, String statusDesc) throws IOException, JbillingAPIException {
		AssetStatusDTOEx statusDTOex = null;
		for(ItemTypeWS itemType : api.getAllItemCategories()) {
			if(categoryID.equals(itemType.getId())) {
				for(AssetStatusDTOEx status : itemType.getAssetStatuses()) {
					if(statusDesc.equals(ASSET_STATUS_AVAILABLE)  && status.getIsAvailable()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
					else if(statusDesc.equals(ASSET_STATUS_DEFAULT)  && status.getIsDefault()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
					else if(statusDesc.equals(ASSET_STATUS_ORDERED)  && status.getIsOrderSaved()==AssetStatusBL.ASSET_STATUS_TRUE && status.getIsActive()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
					else if(statusDesc.equals(ASSET_STATUS_ORDERED)  && status.getIsOrderSaved()==AssetStatusBL.ASSET_STATUS_TRUE && status.getIsPending()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
					else if(statusDesc.equals(ASSET_STATUS_INTERNAL)  && status.getIsInternal()==AssetStatusBL.ASSET_STATUS_TRUE) {
						statusDTOex = new AssetStatusDTOEx();
						statusDTOex.setId(status.getId());
						break;
					}
				}
			}
			if(statusDTOex!=null) break;
		}
		return statusDTOex.getId();
	}
	
	private Integer createItem(Integer categoryID) throws IOException, JbillingAPIException {
		ItemDTOEx item = new ItemDTOEx();
		item.setGlobal(true);
		item.setDescription("item-test-for-asset-reservation");
		item.setNumber("ITFAR-023");
		item.setReservationDuration(10);
		item.setAssetManagementEnabled(1);
		item.setTypes(new Integer[]{categoryID});
		return api.createItem(item);
	}

	private Integer createCategoryWithAssetManagement() throws IOException, JbillingAPIException {
		ItemTypeWS itemType = new ItemTypeWS();
		itemType.setDescription("Asset Category " + Math.random() * 10000);
		itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		itemType.setAllowAssetManagement(1);
		itemType.setGlobal(true);
		itemType.setAssetStatuses(this.createAssetStatuses());
		itemType.setDescription("category-test-for-asset-reservation-" + Math.random() * 10000);
		return api.createItemCategory(itemType);
	}

	private Set<AssetStatusDTOEx> createAssetStatuses() {
		Set<AssetStatusDTOEx> assetStatusList = new HashSet<AssetStatusDTOEx>();

		AssetStatusDTOEx status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsActive(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsPending(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setDescription("asset-reserved");
		assetStatusList.add(status);

		status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsActive(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsPending(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setDescription("asset-available-A");
		assetStatusList.add(status);

		status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsActive(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsPending(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setDescription("asset-default");
		assetStatusList.add(status);

		status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsActive(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setIsPending(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setDescription("asset-order-saved");
		assetStatusList.add(status);

		status = new AssetStatusDTOEx();
		status.setIsAvailable(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsDefault(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsOrderSaved(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setIsInternal(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsActive(AssetStatusBL.ASSET_STATUS_FALSE);
		status.setIsPending(AssetStatusBL.ASSET_STATUS_TRUE);
		status.setDescription("asset-pending");
		assetStatusList.add(status);

		return assetStatusList;
	}
	
}
