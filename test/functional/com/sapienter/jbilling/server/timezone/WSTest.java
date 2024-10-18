package com.sapienter.jbilling.server.timezone;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;


/**
 * Created by pablo_galera on 12/10/16.
 */

@Test(groups = { "web-services", "timezone" }, testName = "timezon.WSTest")
public class WSTest {

    private static final Logger logger = LoggerFactory.getLogger(WSTest.class);
	private static Integer PRANCING_PONY_BASIC_ACCOUNT_TYPE = Integer.valueOf(1);

	private static JbillingAPI api = null;
	private static int ORDER_CHANGE_STATUS_APPLY_ID;
	private static int ORDER_PERIOD_MONTHLY_ID;
	private static int ORDER_PERIOD_ONE_TIME_ID = 1;

    private static String GMT_PLUS_14_TIMEZONE = "Etc/GMT-14";
    private static String GMT_MINUS_12_TIMEZONE = "Etc/GMT+12";
    private static String GMT_PLUS_8_TIMEZONE = "Etc/GMT-8";
    private static String GMT_MINUS_8_TIMEZONE = "Etc/GMT+8";

	@BeforeClass
	public void setupClass() throws Exception {
		api = JbillingAPIFactory.getAPI();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api).intValue();
		ORDER_PERIOD_MONTHLY_ID = getOrCreateMonthlyOrderPeriod(api).intValue();
	}

    @Test
    public void test001RunBillingProcessForTimezone() {
        String companyTimezone = GMT_PLUS_14_TIMEZONE;
        if (LocalDateTime.now().toLocalTime().isBefore(LocalTime.NOON.minusHours(1))) {
            companyTimezone = GMT_MINUS_12_TIMEZONE;
        }

        CompanyWS company = api.getCompany();
        company.setTimezone(companyTimezone);
        api.updateCompany(company);

	    ItemTypeWS itemType = buildItemType();
	    itemType.setId(api.createItemCategory(itemType));

	    ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
	    item.setId(api.createItem(item));

	    UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        final Integer userId = api.createUser(user);

        //Update Next invoice date and billing cycle period.
        user = api.getUserWS(userId);
	    user.setPassword(null);
        Calendar now = Calendar.getInstance();
        now.setTime(TimezoneHelper.currentDateForTimezone(companyTimezone));
        int day = now.get(Calendar.DAY_OF_MONTH);
        user.setMainSubscription(createUserMainSubscription(day));
        user.setPassword(null);
        
        api.updateUser(user);
        user = api.getUserWS(userId);
	    user.setPassword(null);
        user.setNextInvoiceDate(TimezoneHelper.currentDateForTimezone(companyTimezone));
        user.setPassword(null);
        api.updateUser(user);

        // setup order
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(TimezoneHelper.currentDateForTimezone(companyTimezone));
        order.setProrateFlag(false);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(1);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[] { line });

        // create the order
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        orderChanges[0].setStartDate(TimezoneHelper.currentDateForTimezone(companyTimezone));
        Integer orderId1 = api.createOrder(order, orderChanges);

        OrderWS orderWS = api.getOrder(orderId1);
        assertEquals("Order creation date according to timezone", Util.truncateDate(TimezoneHelper.currentDateForTimezone(companyTimezone)),
                Util.truncateDate(orderWS.getCreateDate()));

        // create invoice
        Integer[] invoices = api.createInvoice(userId, false);
        assertEquals("Number of invoices returned", 1, invoices.length);
        InvoiceWS invoiceWS = api.getInvoiceWS(invoices[0]);

        assertEquals("Invoice creation date according to timezone", Util.truncateDate(TimezoneHelper.currentDateForTimezone(companyTimezone)),
                Util.truncateDate(invoiceWS.getCreateTimeStamp()));

        // clean up
        api.deleteInvoice(invoices[0]);
        api.deleteOrder(orderId1);
	    api.deleteItem(item.getId());
	    api.deleteItemCategory(itemType.getId());
	    api.deleteUser(userId);
        company.setTimezone("UTC");
        api.updateCompany(company);
    }


    @Test
    public void test002ProcessAgeingForTimezone() {
        String companyTimezone = GMT_PLUS_14_TIMEZONE;
        if (LocalDateTime.now().toLocalTime().isBefore(LocalTime.NOON.minusHours(1))) {
            companyTimezone = GMT_MINUS_12_TIMEZONE;
        }

        CompanyWS company = api.getCompany();
        company.setTimezone(companyTimezone);
        api.updateCompany(company);

        final Integer ACTIVE = Integer.valueOf(1);
        final Integer OVERDUE = Integer.valueOf(2);

        Date companyDatePlusMonth = TimezoneHelper.convertToTimezone(DateConvertUtils.asUtilDate(LocalDateTime.now().plusMonths(1)), companyTimezone);
        logger.debug("companyDatePlusMonth {}", companyDatePlusMonth);
        Date companyDatePlusMonthAndDay = TimezoneHelper.convertToTimezone(DateConvertUtils.asUtilDate(LocalDateTime.now().plusMonths(1).plusDays(1)), companyTimezone);
        logger.debug("companyDatePlusMonthAndDay {}", companyDatePlusMonthAndDay);

        {
            UserWS user1 = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
            user1.setId(api.createUser(user1));

            user1 = api.getUserWS(user1.getId());


            //setting user status as ACTIVE
            user1.setStatusId(ACTIVE);
            user1.setPassword(null);
            api.updateUser(user1);
            logger.debug("user initial status : {}", api.getUserWS(user1.getId()).getStatus());

            OrderWS order = setUpOrder(user1.getId(), new BigDecimal("21.00"), TimezoneHelper.currentDateForTimezone(companyTimezone));
            OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
            orderChanges[0].setStartDate(TimezoneHelper.currentDateForTimezone(companyTimezone));
            Integer orderId1 = api.createOrder(order, orderChanges);

            assertNotNull("Order 1 created", orderId1);
            logger.debug("Order 1 created {}", +orderId1);
            Integer invoiceId1 = api.createInvoiceFromOrder(orderId1, null);
            assertNotNull("Invoice created", invoiceId1);
            logger.debug("Invoice created{} ", invoiceId1);
            api.triggerAgeing(companyDatePlusMonth);
            logger.debug("user status : {}", api.getUserWS(user1.getId()).getStatus());
            //checking if user status is ACTIVE
            assertEquals("Expected ACTIVE user", ACTIVE, api.getUserWS(user1.getId()).getStatusId());

            //cleanup
            api.deleteInvoice(invoiceId1);
            api.deleteOrder(orderId1);
            updateCustomerStatusToActive(user1.getId(), api);
            api.deleteUser(user1.getId());
        }

        {
            UserWS user2 = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
            user2.setId(api.createUser(user2));

            user2 = api.getUserWS(user2.getId());

            logger.debug("user2 {}", user2.getId());
            //setting user status as ACTIVE
            user2.setStatusId(ACTIVE);
            api.updateUser(user2);
            logger.debug("user status again changed to : {}", api.getUserWS(user2.getId()).getStatus());

            //creating order having balance more than min balalance to ignore ageing i.e, 0.00
            OrderWS order2 = setUpOrder(user2.getId(), new BigDecimal("21.00"), TimezoneHelper.currentDateForTimezone(companyTimezone));
            OrderChangeWS[] orderChanges2 = OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID);
            orderChanges2[0].setStartDate(TimezoneHelper.currentDateForTimezone(companyTimezone));
            Integer orderId2 = api.createOrder(order2, orderChanges2);
            assertNotNull("Order 2 created", orderId2);
            logger.debug("Order 2 created {}", orderId2);
            Integer invoiceId2 = api.createInvoiceFromOrder(orderId2, null);
            assertNotNull("Invoice created", invoiceId2);
            logger.debug("Invoice created {}", invoiceId2);
            api.triggerAgeing(companyDatePlusMonthAndDay);
            logger.debug("user status : {}", api.getUserWS(user2.getId()).getStatus());
            //checking if user status is OVERDUE
            assertEquals("Expected OVERDUE user", OVERDUE, api.getUserWS(user2.getId()).getStatusId());

            //cleanup
            api.deleteInvoice(invoiceId2);
            api.deleteOrder(orderId2);
            updateCustomerStatusToActive(user2.getId(), api);
            api.deleteUser(user2.getId());
        }

        //cleanup company
        company.setTimezone("UTC");
        api.updateCompany(company);
    }

    @Test
    public void test002CreationDateTimeAccordingToTimezone() {
        CompanyWS company = api.getCompany();

        LocalDateTime processStartDateTime3, processStartDateTime2, processStartDateTime1 = LocalDateTime.now();
        processStartDateTime3 = processStartDateTime2 = processStartDateTime1;
        UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        user.setId(api.createUser(user));
        user = api.getUserWS(user.getId());

        OrderWS order = setUpOrder(user.getId(), new BigDecimal("21.00"), TimezoneHelper.serverCurrentDate());
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        Integer orderId = api.createOrder(order, orderChanges);
        logger.debug("Order created {}", orderId);

        Integer invoiceId = api.createInvoiceFromOrder(orderId, null);
        logger.debug("Invoice created {}", invoiceId);

        order = api.getOrder(orderId);
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        logger.debug("Validating creation time for UTC timezone, processStartDateTime1 = {}", processStartDateTime1);
        logger.debug("user.getCreateDatetime() {}", user.getCreateDatetime());
        logger.debug("order.getCreateDate() {}", order.getCreateDate());
        logger.debug("invoice.getCreateTimeStamp() {}", invoice.getCreateTimeStamp());
        assertTrue("User creation date in UTC", isDateBetween(user.getCreateDatetime(), processStartDateTime1, processStartDateTime1.plusMinutes(1)));
        assertTrue("Order creation date in UTC", isDateBetween(order.getCreateDate(), processStartDateTime1, processStartDateTime1.plusMinutes(1)));
        assertTrue("Invoice creation date in UTC", isDateBetween(invoice.getCreateTimeStamp(), processStartDateTime1, processStartDateTime1.plusMinutes(1)));

        company.setTimezone(GMT_PLUS_8_TIMEZONE);
        api.updateCompany(company);
        user = api.getUserWS(user.getId());
        order = api.getOrder(orderId);
        invoice = api.getInvoiceWS(invoiceId);
        processStartDateTime2 = TimezoneHelper.convertToTimezone(processStartDateTime2, GMT_PLUS_8_TIMEZONE);
        logger.debug("Validating creation time for UTC+0800 timezone, processStartDateTime = {}", processStartDateTime2);
        logger.debug("user.getCreateDatetime() {}", user.getCreateDatetime());
        logger.debug("order.getCreateDate() {}", order.getCreateDate());
        logger.debug("invoice.getCreateTimeStamp() {}", invoice.getCreateTimeStamp());
        assertTrue("User creation date in UTC+0800", isDateBetween(user.getCreateDatetime(), processStartDateTime2, processStartDateTime2.plusMinutes(1)));
        assertTrue("Order creation date in UTC+0800", isDateBetween(order.getCreateDate(), processStartDateTime2, processStartDateTime2.plusMinutes(1)));
        assertTrue("Invoice creation date in UTC+0800", isDateBetween(invoice.getCreateTimeStamp(), processStartDateTime2, processStartDateTime2.plusMinutes(1)));

        company.setTimezone(GMT_MINUS_8_TIMEZONE);
        api.updateCompany(company);
        user = api.getUserWS(user.getId());
        order = api.getOrder(orderId);
        invoice = api.getInvoiceWS(invoiceId);
        processStartDateTime3 = TimezoneHelper.convertToTimezone(processStartDateTime3, GMT_MINUS_8_TIMEZONE);
        logger.debug("Validating creation time for UTC-0800 timezone, processStartDateTime = {}", processStartDateTime3);
        logger.debug("user.getCreateDatetime() {}", user.getCreateDatetime());
        logger.debug("order.getCreateDate() {}", order.getCreateDate());
        logger.debug("invoice.getCreateTimeStamp() {}", invoice.getCreateTimeStamp());
        assertTrue("User creation date in UTC-0800", isDateBetween(user.getCreateDatetime(), processStartDateTime3, processStartDateTime3.plusMinutes(1)));
        assertTrue("Order creation date in UTC-0800", isDateBetween(order.getCreateDate(), processStartDateTime3, processStartDateTime3.plusMinutes(1)));
        assertTrue("Invoice creation date in UTC-0800", isDateBetween(invoice.getCreateTimeStamp(), processStartDateTime3, processStartDateTime3.plusMinutes(1)));

        //cleanup

        company.setTimezone("UTC");
        api.updateCompany(company);
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId);
        api.deleteUser(user.getId());
    }

    private boolean isDateBetween(Date date, LocalDateTime processStartDateTime, LocalDateTime processEndDateTime) {
        LocalDateTime localDateTime = DateConvertUtils.asLocalDateTime(date);
        return !localDateTime.isBefore(processStartDateTime) && !localDateTime.isAfter(processEndDateTime);
    }

    private OrderWS setUpOrder(Integer userId, BigDecimal price, Date date){
        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(date);

        //setup orderLines
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(1);
        line.setQuantity(1);
        line.setPrice(price);
        line.setAmount(price);

        order.setOrderLines(new OrderLineWS[] { line });

        return order;
    }

    private static UserWS buildUser(Integer accountTypeId) {
		UserWS newUser = new UserWS();
		newUser.setUserId(0);
		newUser.setUserName("testInvoiceUser-" + System.currentTimeMillis());
		newUser.setPassword("Admin123@");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(accountTypeId);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(Integer.valueOf(1));
		
		newUser.setInvoiceChild(false);

		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("contact.email");
		metaField3.setValue(newUser.getUserName() + "@shire.com");
		metaField3.setGroupId(accountTypeId);

		MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("contact.first.name");
		metaField4.setValue("Frodo");
		metaField4.setGroupId(accountTypeId);

		MetaFieldValueWS metaField5 = new MetaFieldValueWS();
		metaField5.setFieldName("contact.last.name");
		metaField5.setValue("Baggins");
		metaField5.setGroupId(accountTypeId);

		newUser.setMetaFields(new MetaFieldValueWS[] { metaField3, metaField4, metaField5 });
		return newUser;
	}

	private ItemTypeWS buildItemType() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Invoice, Item Type:" + System.currentTimeMillis());
		type.setOrderLineTypeId(1);//items
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}

	private ItemDTOEx buildItem(Integer itemTypeId, Integer priceModelCompanyId){
		ItemDTOEx item = new ItemDTOEx();
		long millis = System.currentTimeMillis();
		String name = String.valueOf(millis) + new Random().nextInt(10000);
		item.setDescription("Invoice, Product:" + name);
		item.setPriceModelCompanyId(priceModelCompanyId);
		item.setPrice(new BigDecimal("10"));
		item.setNumber("INV-PRD-"+name);
		item.setAssetManagementEnabled(0);
		Integer typeIds[] = new Integer[] {itemTypeId};
		item.setTypes(typeIds);
		return item;
	}

	private static MainSubscriptionWS createUserMainSubscription(int day) {
    	MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
    	mainSubscription.setPeriodId(ORDER_PERIOD_MONTHLY_ID); //monthly
    	mainSubscription.setNextInvoiceDayOfPeriod(day); // 1st of the month
    	return mainSubscription;
    }

	private static Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
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
		status1.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
		return api.createOrderChangeStatus(apply);
	}

	private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
		OrderPeriodWS[] periods = api.getOrderPeriods();
		for(OrderPeriodWS period : periods){
			if(1 == period.getValue().intValue() &&
					PeriodUnitDTO.MONTH == period.getPeriodUnitId().intValue()){
				return period.getId();
			}
		}
		//there is no monthly order period so create one
		OrderPeriodWS monthly = new OrderPeriodWS();
		monthly.setEntityId(api.getCallerCompanyId());
		monthly.setPeriodUnitId(1);//monthly
		monthly.setValue(1);
		monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "INV:MONTHLY")));
		return api.createOrderPeriod(monthly);
	}

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
    }
}
