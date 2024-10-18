package com.sapienter.jbilling.server.task;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;
import com.sapienter.jbilling.test.framework.TestEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.sapienter.jbilling.test.Asserts.*;
import static org.testng.AssertJUnit.*;

/**
 * Tests the notification task: {@CreditLimitationNotificationTask}
 * @author Maeis Gharibjanian
 * @since 03-09-2013
 */
@Test(groups = { "integration", "task" }, testName = "CreditLimitationNotificationTaskTest")
public class CreditLimitationNotificationTaskTest extends ApiTestCase {

	private static final Logger logger = LoggerFactory.getLogger(CreditLimitationNotificationTaskTest.class);
	// id of credit limitation plugin at table: pluggable_task_type
	private static final String CREDIT_LIMITATION_NOTIFICATION_PLUGIN_NAME = "com.sapienter.jbilling.server.user.tasks.CreditLimitationNotificationTask";

    private static Integer ORDER_CHANGE_STATUS_APPLY_ID;
	private static Integer CURRENCY_USD;
	private static Integer CURRENCY_GBP;
	private static Integer LANGUAGE_ID;
	private static Integer CUSTOMER_MAIN_ROLE;
	private static Integer PRANCING_PONY_ACCOUNT_TYPE;
	private static Integer pluginId;
    private static Integer DYNAMIC_BALANCE_MANAGER_PLUGIN_ID;

	protected void prepareTestInstance() throws Exception {
		super.prepareTestInstance();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
		CURRENCY_USD = Constants.PRIMARY_CURRENCY_ID;
		CURRENCY_GBP = Integer.valueOf(5);
		CUSTOMER_MAIN_ROLE = Integer.valueOf(5);
		LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
		enablePlugin();
        DYNAMIC_BALANCE_MANAGER_PLUGIN_ID = getOrCreatePluginWithoutParams(
                "com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask", 10001);
	}

	protected void afterTestClass() throws Exception {
		if (pluginId != null) {
			api.deletePlugin(pluginId);
			pluginId = null;
		}
        if(null != DYNAMIC_BALANCE_MANAGER_PLUGIN_ID) {
            api.deletePlugin(DYNAMIC_BALANCE_MANAGER_PLUGIN_ID);
        }
	}

	private void enablePlugin() {
		PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(CREDIT_LIMITATION_NOTIFICATION_PLUGIN_NAME);

		//check if the static number for the plugin is the one we need
		if(!type.getClassName().equals(CREDIT_LIMITATION_NOTIFICATION_PLUGIN_NAME)){
			fail("The plugin with id:" + type.getId() + ", is not with class name CreditLimitationNotificationTask");
		}

		PluggableTaskWS plugin = new PluggableTaskWS();
		plugin.setTypeId(type.getId());
		plugin.setProcessingOrder(200);

		Hashtable<String, String> parameters = new Hashtable<>();
		plugin.setParameters(parameters);

        pluginId = api.createPlugin(plugin);
	}

    @Test
    public void testCreditLimitationNotification() throws IOException {
	    assertNotNull("Plugin not configured", pluginId);
	    logger.debug("CreditLimitationNotificationTaskTest, plugin id: {}", pluginId);

        //This test doesn't work in a multi node environment
        if(TestEnvironment.isMultiNode()) {
            return;
        }

	    PluggableTaskWS plugin = api.getPluginWS(pluginId);

	    BigDecimal orglDynamicBal = new BigDecimal("10.00");

        String directory = Util.getSysProp("base_dir");
//      This test become hard in cloud environment because any batch server can send this and the location will be different. For now it is disabled
//	    //test if emails_sent.txt does not exist
//
//	    System.out.println("Base Directory: " + directory);
//	    try {
//		    File f = new File(directory + "emails_sent.txt");
//		    f.delete();
//		    assertFalse("The emails_sent file exists", f.exists());
//	    } catch (Exception e) {
//		    System.out.println(e.getMessage());
//		    fail("Exception. Can not remove the emails_sent.txt file." + e.getMessage());
//	    }

	    UserWS newUser = new UserWS();
	    newUser.setUserName("creditLimit-test" + System.currentTimeMillis());
	    newUser.setPassword("Admin123@");
	    newUser.setLanguageId(LANGUAGE_ID);
	    newUser.setCurrencyId(CURRENCY_GBP);
	    newUser.setMainRoleId(CUSTOMER_MAIN_ROLE);
	    newUser.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
	    newUser.setIsParent(Boolean.FALSE);
	    newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

	    //balance type and dynamic balance to 10.
	    newUser.setDynamicBalance(orglDynamicBal);

	    //we have balance 10 and we make purchase of 15 which drops under 0
	    newUser.setCreditLimitNotification1(BigDecimal.ZERO);

	    String email = newUser.getUserName() + "@gmail.com";
	    //email contact meta field
	    MetaFieldValueWS metaField2 = new MetaFieldValueWS();
	    metaField2.setFieldName("contact.email");
	    metaField2.setValue(email);
	    metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

	    newUser.setMetaFields(new MetaFieldValueWS[]{
			    metaField2
	    });

	    // do the creation
	    Integer newUserId = api.createUser(newUser);
	    logger.debug("User created : {}", newUserId);
	    assertNotNull("User created", newUserId);

	    // verify that the dynamic balance of the saved users is correctly preserved
	    UserWS createdUser = api.getUserWS(newUserId);
	    assertEquals("Dynamic Balance Not Save Correctly", orglDynamicBal, createdUser.getDynamicBalanceAsDecimal());

	    //create Order for $15,
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
	    line.setQuantity(new Integer(3));
	    line.setAmount(new BigDecimal("5.00"));
	    line.setDescription("Example Item");
	    line.setItemId(new Integer(3));
	    line.setUseItem(false);
	    lines[0] = line;

	    order.setOrderLines(lines);
	    Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	    logger.debug("Order created : {}", orderId);
	    assertNotNull("Order created", orderId);

	    //verify that the order total is indeed 15$
	    order = api.getOrder(orderId);
	    assertEquals("Order total is not correct", new BigDecimal("15.00"), order.getTotalAsDecimal());

	    //dynamic balance should reduce and drop from $10 to -5$
	    createdUser = api.getUserWS(newUserId);
	    logger.debug("Original Dynamic bal : {}", orglDynamicBal);
	    logger.debug("Current Dynamic bal : {}", createdUser.getDynamicBalanceAsDecimal());

	    assertEquals("The dynamic balance should be -5.00$", new BigDecimal("-5.00"), createdUser.getDynamicBalanceAsDecimal());
	    assertTrue("The dynamic balance is not less than before", createdUser.getDynamicBalanceAsDecimal().compareTo(orglDynamicBal) < 0);

	    logger.debug("Base Directory: {}", directory);
	    File f = new File(directory + "emails_sent.txt");
		pause(2);//wait 2 second for sending notification mail.
	    assertTrue("File does not exists. File:" + f.getName(), f.exists());

	    logger.debug("File Name: {}", f.getName());

	    FileReader fr = new FileReader(f);

	    BufferedReader reader = new BufferedReader(fr);

	    String strLine = reader.readLine();

		//added the email check when checking the subject of the mail
		boolean subjectCheckStatus = false;
		while (strLine != null) {
			if (strLine.startsWith("To") && strLine.contains(email)) subjectCheckStatus = true;
			if (subjectCheckStatus && strLine.startsWith("Subject")) {
				assertTrue(strLine.indexOf("Your pre-paid balance is below Water mark.") > 0);
		    }
		    strLine = reader.readLine();
	    }
		//finally check the status
		if(!subjectCheckStatus){
			fail("Can not sent the mail to : "+email);
		}

	    //cleanup
	    api.deleteOrder(orderId);
	    api.deleteUser(newUserId);
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

	private void pause(long second) {
		logger.debug("pausing for {} seconds", second);
		try {
			TimeUnit.SECONDS.sleep(second);
		} catch (InterruptedException e) {
			logger.error("InterruptedException  occurs", e);
			// Restore the interrupted status
			Thread.currentThread().interrupt();
		}
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
