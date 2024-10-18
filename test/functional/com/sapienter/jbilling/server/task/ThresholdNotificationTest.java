/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

/**
 *
 */
package com.sapienter.jbilling.server.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.List;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.test.ApiTestCase;

import com.sapienter.jbilling.test.framework.TestEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import static org.testng.AssertJUnit.*;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

/**
 * @author Shweta Gupta
 * @since 07-Jan-2013
 *
 */
@Test(groups = { "integration", "task", "threshold-notification" }, testName = "ThresholdNotificationTest", priority = 10)
public class ThresholdNotificationTest extends ApiTestCase {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdNotificationTest.class);
    private static final Integer THRESHOLD_NOTIFICATION_PLUGIN_ID = 120;

    private static Integer ORDER_CHANGE_STATUS_APPLY_ID;
	private static Integer CURRENCY_GBP;
	private static Integer LANGUAGE_ID;
	private static Integer PRANCING_PONY_ACCOUNT_TYPE;
    private static Integer DYNAMIC_BALANCE_MANAGER_PLUGIN_ID;
    private Integer pluginId;

	@Override
	protected void prepareTestInstance() throws Exception {
		super.prepareTestInstance();

		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
		CURRENCY_GBP = Integer.valueOf(5);
		LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
		enablePlugin();

        DYNAMIC_BALANCE_MANAGER_PLUGIN_ID = getOrCreatePluginWithoutParams(
                "com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask", 10005);

	}

	private void enablePlugin() {
		PluggableTaskWS plugin = new PluggableTaskWS();
		plugin.setTypeId(THRESHOLD_NOTIFICATION_PLUGIN_ID);
		plugin.setProcessingOrder(44);

		pluginId = api.createPlugin(plugin);
	}

	@AfterClass
	@AfterMethod
	private void disableTaxPlugin() {
		if (pluginId != null) {
			api.deletePlugin(pluginId);
			pluginId = null;
		}
        if(null != DYNAMIC_BALANCE_MANAGER_PLUGIN_ID) {
            api.deletePlugin(DYNAMIC_BALANCE_MANAGER_PLUGIN_ID);
            DYNAMIC_BALANCE_MANAGER_PLUGIN_ID = null;
        }
	}

    @Test
    public void testThresholdNotification() {
        //This test doesn't work in a multi node environment
        if(TestEnvironment.isMultiNode()) {
            return;
        }

        BigDecimal orglDynamicBal= new BigDecimal("10.00");

        //test if emails_sent.txt does not exist
        String directory = Util.getSysProp("base_dir");

        logger.debug("Base Directory: {}", directory);

        try {
            File f= new File(directory + "emails_sent.txt");
            PrintWriter writer = new PrintWriter(f);
            writer.print("");
            writer.close();
        } catch (Exception e) {
            logger.error("File Error:\n", e);
            e.printStackTrace();
        }

        UserWS newUser = new UserWS();
        newUser.setUserName("thresholdtst" + new java.util.Date().getTime());
        newUser.setPassword("Admin123@");
        newUser.setLanguageId(LANGUAGE_ID);
        newUser.setCurrencyId(CURRENCY_GBP); //GBP
        newUser.setMainRoleId(Constants.TYPE_CUSTOMER);
        newUser.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        newUser.setIsParent(Boolean.FALSE);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

        //balance type and dynamic balance to 10.
        newUser.setDynamicBalance(orglDynamicBal);
	    newUser.setCreditLimit("0");

        //email contact meta field
        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.email");
        metaField2.setValue(newUser.getUserName() + "@gmail.com");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField2
        });

        // do the creation
        Integer newUserId = api.createUser(newUser);
        logger.debug("User created : {}", newUserId);
        assertNotNull("User created", newUserId);

        // get user
        UserWS createdUser = api.getUserWS(newUserId);
        assertEquals("Language id", LANGUAGE_ID.intValue(),
                createdUser.getLanguageId().intValue());

        ItemTypeWS itemCategory = buildItemCategory();
        itemCategory.setId(api.createItemCategory(itemCategory));
        ItemDTOEx item = buildItem(itemCategory.getId());
		item.setId(api.createItem(item));

        //create Order for $5,
        OrderWS order = new OrderWS();
        order.setUserId(newUserId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);//pre-paid
        order.setPeriod(Constants.ORDER_PERIOD_ONCE);//one time
        order.setCurrencyId(CURRENCY_GBP);
        order.setActiveSince(new java.util.Date());

        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line = new OrderLineWS();
        line.setPrice(new BigDecimal("5.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);//item
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("5.00"));
        line.setDescription("Example Item");
        line.setItemId(item.getId());
        line.setUseItem(false);
        lines[0] = line;

        order.setOrderLines(lines);
        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        logger.debug("Order created : {}", orderId);
        assertNotNull("Order created", orderId);

        //dynamic balance should reduce
        createdUser = api.getUserWS(newUserId);
        logger.debug("Original Dynamic bal : {}", orglDynamicBal);
        logger.debug("Current Dynamic bal : {}", createdUser.getDynamicBalanceAsDecimal());
        assertTrue(createdUser.getDynamicBalanceAsDecimal().compareTo(orglDynamicBal) < 0 );

        //test if emails_sent.txt was created
        try {
            logger.debug("Base Directory: {}", directory);
            File f= new File(directory + "emails_sent.txt");
            logger.debug("File Name: {}", f.getName());

            //assertTrue(f.isFile());
            FileReader fr= new FileReader(f);
            BufferedReader reader = new BufferedReader(fr);
            String strLine= reader.readLine();

            while (strLine != null ) {
                if (strLine.startsWith("To: ")) {
                    assertTrue(strLine.indexOf(createdUser.getContact().getEmail()) > 0);
                }
                if (strLine.startsWith("Subject")) {
                    assertTrue(strLine.indexOf("Your pre-paid balance is below Water mark.") > 0);
                }
                strLine= reader.readLine();
            }

        } catch (Exception e) {
            logger.error("Error reading the file", e);
	        fail("Test failed with exception: " + e.getMessage());
        }

        // clean up
	    api.deleteOrder(orderId);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemCategory.getId());
        api.deleteUser(newUserId);
    }

	private ItemDTOEx buildItem(Integer categoryId) {
		ItemDTOEx item = new ItemDTOEx();
		item.setCurrencyId(CURRENCY_GBP);
		item.setPrice(new BigDecimal("15"));
		item.setDescription("ITEM-NOTIFY");
		item.setEntityId(TEST_ENTITY_ID);
		item.setNumber("ITEM-NOTIFY");
		item.setTypes(new Integer[]{categoryId});
		return item;
	}

	private ItemTypeWS buildItemCategory() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Threshold Notify, Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
		OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
		for (OrderChangeStatusWS status : statuses) {
			if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
				return status.getId();
			}
		}
		//there is no APPLY status in db so create one
		OrderChangeStatusWS apply = new OrderChangeStatusWS();
		String status1Name = "APPLY: " + System.currentTimeMillis();
		OrderChangeStatusWS status1 = new OrderChangeStatusWS();
		status1.setApplyToOrder(ApplyToOrder.YES);
		status1.setDeleted(0);
		status1.setOrder(1);
		status1.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}

    private Integer getOrCreatePluginWithoutParams(String className, int processingOrder) {
        PluggableTaskWS[] taskWSs = api.getPluginsWS(api.getCallerCompanyId(), className);
        if(taskWSs.length != 0){
            return taskWSs[0].getId();
        }
        PluggableTaskWS pluggableTaskWS = new PluggableTaskWS();
        pluggableTaskWS.setTypeId(api.getPluginTypeWSByClassName(className).getId());
        pluggableTaskWS.setProcessingOrder(processingOrder);
        pluggableTaskWS.setOwningEntityId(api.getCallerCompanyId());
        return api.createPlugin(pluggableTaskWS);
    }
}
