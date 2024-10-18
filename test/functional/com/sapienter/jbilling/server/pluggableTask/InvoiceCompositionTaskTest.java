package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;

@Test(groups = {"web-services", "pluggable"}, testName = "InvoiceCompositionTaskTest")
public class InvoiceCompositionTaskTest {

    public static final int COMPOSITION_TASK_ID = 6092;
    public static final int ORDER_LINE_PLUGIN_TYPE_ID = 140;
    public static final int ORDER_CHANGE_PLUGIN_TYPE_ID = 4;
    private static Integer PRANCING_PONY_BASIC_ACCOUNT_TYPE = 1;

    private static JbillingAPI api = null;
    private static int ORDER_CHANGE_STATUS_APPLY_ID;
    private static int ORDER_PERIOD_ONE_TIME_ID = 1;

    /**
     * OrderChangeBasedCompositionTask extended for testing. The locale is settable and this will never attempt to look up the
     * entity preference for appending the order id to the invoice line.
     * <p>
     * This class is needed so that the invoice line description composition can be tested without the need for a live
     * container.
     */
    private class TestOrderChangeBasedCompositionTask extends OrderChangeBasedCompositionTask {

        private Locale locale = Locale.getDefault();

        @Override
        protected Locale getLocale(Integer userId) {
            return locale; // for testing, return whatever locale is set
        }

        @Override
        protected boolean needAppendOrderId(Integer entityId) {
            return false; // for testing, never append the order ID
        }

        @Override
        protected String composeDescription(OrderLineDTO orderLine, PeriodOfTime period) {
            return super.composeDescription(orderLine, period);
        }
    }

    // class under test
    private TestOrderChangeBasedCompositionTask task = new TestOrderChangeBasedCompositionTask();

    @BeforeClass
    public void setupClass() throws Exception {
        api = JbillingAPIFactory.getAPI();
        ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply(api);
    }

    @Test
    public void test001InvoiceCompositionOrderChange() {
        enableOrderLinePlugin(api);
        UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        user.setUserId(api.createUser(user));
        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
        item.setId(api.createItem(item));

        // setup order
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());
        order.setProrateFlag(false);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(3);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[]{line});
        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        order = api.getOrder(orderId);
        //Assert that the order has only 1 line


        // At this point we know that there is only one order line in the order
        Integer orderLineId = order.getOrderLines()[0].getId();
        line = api.getOrderLine(orderLineId);

        // Remove 2 items and update the order
        line.setDeleted(1);
        line.setQuantity(2);
        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line, order, ORDER_CHANGE_STATUS_APPLY_ID);
        api.updateOrder(order, new OrderChangeWS[]{orderChange});

        InvoiceWS invoice = api.getInvoiceWS(api.createInvoiceFromOrder(orderId, null));

        //The invoice should has one line
        assertEquals(1, invoice.getInvoiceLines().length);

        disableOrderChangesPlugin(api);
    }

    @Test
    public void test002InvoiceCompositionOrderLines() {
        UserWS user = buildUser(PRANCING_PONY_BASIC_ACCOUNT_TYPE);
        user.setUserId(api.createUser(user));
        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
        item.setId(api.createItem(item));

        // setup order
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_ONE_TIME_ID);
        order.setCurrencyId(1);
        order.setActiveSince(new Date());
        order.setProrateFlag(false);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(item.getId());
        line.setQuantity(3);
        line.setPrice(new BigDecimal("10.00"));
        line.setAmount(new BigDecimal("10.00"));

        order.setOrderLines(new OrderLineWS[]{line});
        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        order = api.getOrder(orderId);
        //Assert that the order has only 1 line


        // At this point we know that there is only one order line in the order
        Integer orderLineId = order.getOrderLines()[0].getId();
        line = api.getOrderLine(orderLineId);

        // Remove 2 items and update the order
        line.setDeleted(1);
        line.setQuantity(2);
        OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line, order, ORDER_CHANGE_STATUS_APPLY_ID);
        api.updateOrder(order, new OrderChangeWS[]{orderChange});

        InvoiceWS invoice = api.getInvoiceWS(api.createInvoiceFromOrder(orderId, null));

        //The invoice should has two lines
        assertEquals(2, invoice.getInvoiceLines().length);
    }

    public void testComposeDescription() {
        // period being processed
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2011, Calendar.SEPTEMBER, 1);
        Date start = calendar.getTime();

        calendar.set(2011, Calendar.OCTOBER, 1);
        Date end = calendar.getTime();

        PeriodOfTime period = new PeriodOfTime(start, end, 0);

        // verify description
        String description = task.composeDescription(getMockOrderLine(getMockOrder()), period);
        assertEquals("Line description Period from 09/01/2011 to 09/30/2011", description);
    }

    public void testComposeDescriptionTZ() {
        // try composing in a different time zone.
        TimeZone EDT = TimeZone.getTimeZone("EDT");
        TimeZone.setDefault(EDT);
        DateTimeZone.setDefault(DateTimeZone.forTimeZone(EDT));

        // period being processed
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.clear();

        calendar.set(2011, Calendar.SEPTEMBER, 1);
        Date start = calendar.getTime();

        calendar.set(2011, Calendar.OCTOBER, 1);
        Date end = calendar.getTime();

        PeriodOfTime period = new PeriodOfTime(start, end, 0);

        // verify description, different timezone shouldn't have affected the dates
        String description = task.composeDescription(getMockOrderLine(getMockOrder()), period);
        assertEquals("Line description Period from 09/01/2011 to 09/30/2011", description);
    }

    private OrderDTO getMockOrder() {
        UserDTO user = new UserDTO(1);
        user.setCompany(new CompanyDTO(1));

        OrderDTO order = new OrderDTO();
        order.setBaseUserByUserId(user);
        order.setOrderPeriod(new OrderPeriodDTO(2)); // not a one time period

        return order;
    }

    private OrderLineDTO getMockOrderLine(OrderDTO order) {
        OrderLineDTO orderLine = new OrderLineDTO();
        orderLine.setPurchaseOrder(order);
        orderLine.setDescription("Line description");
        return orderLine;
    }

    public static UserWS buildUser(Integer accountTypeId) {
        UserWS newUser = new UserWS();
        newUser.setUserId(0);
        newUser.setUserName("testInvoiceUser-" + System.currentTimeMillis());
        newUser.setPassword("Admin123@");
        newUser.setLanguageId(1);
        newUser.setMainRoleId(5);
        newUser.setAccountTypeId(accountTypeId);
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(1);

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

        newUser.setMetaFields(new MetaFieldValueWS[]{metaField3, metaField4, metaField5});
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

    private ItemDTOEx buildItem(Integer itemTypeId, Integer priceModelCompanyId) {
        ItemDTOEx item = new ItemDTOEx();
        long millis = System.currentTimeMillis();
        String name = String.valueOf(millis) + new Random().nextInt(10000);
        item.setDescription("Invoice, Product:" + name);
        item.setPriceModelCompanyId(priceModelCompanyId);
        item.setPrice(new BigDecimal("10"));
        item.setNumber("INV-PRD-" + name);
        item.setAssetManagementEnabled(0);
        Integer typeIds[] = new Integer[]{itemTypeId};
        item.setTypes(typeIds);
        return item;
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

    //Enable/disable the PricingModelPricingTask plug-in.
    private void enableOrderLinePlugin(JbillingAPI api) {
        PluggableTaskWS plugin = api.getPluginWS(COMPOSITION_TASK_ID);
        plugin.setTypeId(ORDER_LINE_PLUGIN_TYPE_ID);
        api.updatePlugin(plugin);
    }

    private void disableOrderChangesPlugin(JbillingAPI api) {
        PluggableTaskWS plugin = api.getPluginWS(COMPOSITION_TASK_ID);
        plugin.setTypeId(ORDER_CHANGE_PLUGIN_TYPE_ID);
        api.updatePlugin(plugin);
    }
}

