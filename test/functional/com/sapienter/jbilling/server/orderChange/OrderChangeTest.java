package com.sapienter.jbilling.server.orderChange;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.ApiTestCase;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


import static org.testng.AssertJUnit.*;

/**
 * @author Bojan Dikovski
 * @since 03-SEP-2015
 */
@Test(groups = { "web-services" }, testName = "OrderChangeTest")
public class OrderChangeTest extends ApiTestCase {

    private static final Logger logger = LoggerFactory.getLogger(OrderChangeTest.class);
    private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static final String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
    private static final String CC_MF_NUMBER = "cc.number";
    private static final String CC_MF_EXPIRY_DATE = "cc.expiry.date";
    private static final String CC_MF_TYPE = "cc.type";
    private static final int CC_PM_ID = 1;

    private static Integer PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = null;
    private static Integer PRANCING_PONY_ACCOUNT_TYPE_ID = Integer.valueOf(1);
    private static Integer PRANCING_PONY_USER_ID;
    private static Integer PRANCING_PONY_CATEGORY_ID;
    private static Integer ORDER_PERIOD_MONTHLY;

    @Override
    public void prepareTestInstance() throws Exception {
        super.prepareTestInstance();

        // Prancing Pony entities
        PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(api);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        PRANCING_PONY_CATEGORY_ID = createItemCategory(api);
        PRANCING_PONY_USER_ID = createUser(true, null, Constants.PRIMARY_CURRENCY_ID, true, api, PRANCING_PONY_ACCOUNT_TYPE_ID).getId();
    }

    @Override
    protected void afterTestClass() throws Exception {

        if (null != PRANCING_PONY_CATEGORY_ID) {
            try {
                api.deleteItemCategory(PRANCING_PONY_CATEGORY_ID);
            } catch (Exception e) {
                fail(String.format("Error deleting category %d.\n %s", PRANCING_PONY_CATEGORY_ID, e.getMessage()));
            } finally {
                PRANCING_PONY_CATEGORY_ID = null;
            }
        }

        if (null != PRANCING_PONY_USER_ID) {
            try {
                api.deleteUser(PRANCING_PONY_USER_ID);
            } catch (Exception e) {
                fail(String.format("Error deleting user %d.\n %s", PRANCING_PONY_USER_ID, e.getMessage()));
            } finally {
                PRANCING_PONY_USER_ID = null;
            }
        }
    }

    @Test
    public void test001OrderChangesReferenceDifferentOrderLinesForSameItemType() {

        //Create an item
        ItemDTOEx item = createItem("test001OrderChange1", new BigDecimal("10.5"), "OC-001");
        logger.debug("Creating item: {}", item);
        Integer itemId = api.createItem(item);
        assertNotNull("The item was not created: ", itemId);
        item.setId(itemId);

        //Build one time post-paid order
        OrderWS order = buildOrder(PRANCING_PONY_USER_ID,
                Constants.ORDER_BILLING_POST_PAID,
                Constants.ORDER_PERIOD_ONCE);

        //Build order change
        OrderChangeWS orderChange1 = buildFromItem(item, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange1.setStartDate(order.getActiveSince());
        orderChange1.getMetaFields()[0].setValue("str-val-1");
        orderChange1.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        //Build second order change
        OrderChangeWS orderChange2 = buildFromItem(item, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange2.setStartDate(order.getActiveSince());
        orderChange2.getMetaFields()[0].setValue("str-val-2");
        orderChange2.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        //Create order
        logger.debug("Creating order: {}", order);
        Integer orderId = api.createOrder(order, new OrderChangeWS[]{orderChange1, orderChange2});
        OrderChangeWS orderChanges[] = api.getOrderChanges(orderId);

        //Test that the two order changes reference different order lines
        assertEquals("Two order changes expected", 2, orderChanges.length);
        assertTrue("OrderLine Ids should be different orderLineId1:" + orderChanges[0].getOrderLineId()
                        + " orderLineId2:" + orderChanges[1].getOrderLineId(),
                !orderChanges[0].getOrderLineId().equals(orderChanges[1].getOrderLineId()));

        order = api.getOrder(orderId);

        //Build third order change
        OrderChangeWS orderChange3 = buildFromItem(item, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange3.setStartDate(order.getActiveSince());
        orderChange3.getMetaFields()[0].setValue("str-val-3");
        orderChange3.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        //Build fourth order change
        OrderChangeWS orderChange4 = buildFromItem(item, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange4.setStartDate(order.getActiveSince());
        orderChange4.getMetaFields()[0].setValue("str-val-4");
        orderChange4.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        //Update order
        logger.debug("Updating order: {}", order);
        api.updateOrder(order, new OrderChangeWS[]{orderChange3, orderChange4});

        //Test that there are four order changes for the order
        //and each order change references a different order line
        orderChanges = api.getOrderChanges(orderId);
        assertEquals("Four order changes expected", 4, orderChanges.length);
        Set<Integer> orderLineIds = new HashSet<>();
        for (OrderChangeWS orderChange : orderChanges) {
            orderLineIds.add(orderChange.getOrderLineId());
        }
        assertEquals("Every order change references a unique order line", 4, orderLineIds.size());

        //Cleanup
        api.deleteOrder(orderId);
        api.deleteItem(itemId);
    }

    @Test
    public void test002OrderChangesForSameItemTypeCreateValidInvoice() {

        //Create an item
        ItemDTOEx item = createItem("test002OrderChange2", new BigDecimal("10.5"), "OC-002");
        logger.debug("Creating item: {}", item);
        Integer itemId = api.createItem(item);
        assertNotNull("The item was not created: ", itemId);
        item.setId(itemId);

        //Build one time post-paid order
        OrderWS order = buildOrder(PRANCING_PONY_USER_ID, Constants.
                ORDER_BILLING_POST_PAID,
                Constants.ORDER_PERIOD_ONCE);

        //Build order change
        OrderChangeWS orderChange1 = buildFromItem(item, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange1.setStartDate(order.getActiveSince());
        orderChange1.getMetaFields()[0].setValue("str-val-1");
        orderChange1.setUseItem(Integer.valueOf(0));
        orderChange1.setDescription("ONE");
        orderChange1.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        //Build second order change
        item.setPrice(new BigDecimal("20.5"));
        OrderChangeWS orderChange2 = buildFromItem(item, order, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);
        orderChange2.setStartDate(order.getActiveSince());
        orderChange2.getMetaFields()[0].setValue("str-val-2");
        orderChange2.setUseItem(Integer.valueOf(0));
        orderChange2.setDescription("TWO");
        orderChange2.setUserAssignedStatusId(PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID);

        //Create order
        logger.debug("Creating order: {}", order);
        Integer orderId = api.createOrder(order, new OrderChangeWS[]{orderChange1, orderChange2});

        //Create invoice
        logger.debug("Creating invoice");
        Integer invoiceId = api.createInvoiceFromOrder(orderId, null);

        //Test that there are two order changes and they reference different order lines
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        Integer[] orders = invoice.getOrders();
        assertEquals("Number of orders should be one", 1, orders.length);
        OrderWS invoicedOrder = api.getOrder(orders[0]);
        OrderChangeWS[] orderChanges = api.getOrderChanges(invoicedOrder.getId());
        assertEquals("Two order changes expected", 2, orderChanges.length);
        assertTrue("OrderLine Ids should be different orderLineId1:" + orderChanges[0].getOrderLineId()
                        + " orderLineId2:" + orderChanges[1].getOrderLineId(),
                !orderChanges[0].getOrderLineId().equals(orderChanges[1].getOrderLineId()));

        //Test that are two order line in the invoice
        //and that the invoice total is the expected amount
        InvoiceLineDTO[] invoiceLines = invoice.getInvoiceLines();
        assertEquals("Two order lines expected", 2, invoiceLines.length);
        assertEquals("Total should be $31.0",
                new BigDecimal("31.00"),
                invoice.getTotalAsDecimal().setScale(2, RoundingMode.CEILING));

        //Cleanup
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId);
        api.deleteItem(itemId);
    }

    private static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api) {
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        Integer statusId = null;
        for (OrderChangeStatusWS orderChangeStatus : list) {
            if (orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
                statusId = orderChangeStatus.getId();
                break;
            }
        }
        if (statusId != null) {
            return statusId;
        } else {
            OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
            newStatus.setApplyToOrder(ApplyToOrder.YES);
            newStatus.setDeleted(0);
            newStatus.setOrder(1);
            newStatus.addDescription(new InternationalDescriptionWS(
                    com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status1"));
            return api.createOrderChangeStatus(newStatus);
        }
    }

    private Integer createItemCategory(JbillingAPI api) {
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("category" + Short.toString((short) System.currentTimeMillis()));
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        return api.createItemCategory(itemType);
    }

    private ItemDTOEx createItem(String desc, BigDecimal price, String number) {
        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setDescription(desc);
        newItem.setPrice(price);
        newItem.setNumber(number);
        newItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});

        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setDataType(DataType.STRING);
        metaField.setDisabled(false);
        metaField.setDisplayOrder(1);
        metaField.setEntityId(PRANCING_PONY_ENTITY_ID);
        metaField.setEntityType(EntityType.ORDER_LINE);
        metaField.setMandatory(false);
        metaField.setPrimary(false);
        metaField.setName("Item " + number + " orderLinesMetaField_1");
        newItem.setOrderLineMetaFields(new MetaFieldWS[]{metaField});

        return newItem;
    }

    private static OrderWS buildOrder(Integer userId, Integer billingTypeId, Integer periodId) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingTypeId);
        order.setPeriod(periodId);
        order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);
        order.setActiveSince(cal.getTime());
        order.setNotes("Order Notes");

        return order;
    }

    private static UserWS createUser(
            boolean goodCC, Integer parentId, Integer currencyId,
            boolean doCreate, JbillingAPI api, Integer accountTypeId) {

        // Create - This passes the password validation routine.
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("P@ssword1");
        newUser.setAccountTypeId(accountTypeId);
        newUser.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        newUser.setMainRoleId(Constants.TYPE_CUSTOMER);
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(Boolean.FALSE);
        newUser.setMainSubscription(new MainSubscriptionWS(ORDER_PERIOD_MONTHLY, 1));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue("test" + System.currentTimeMillis() + "@test.com");
        metaField1.setGroupId(accountTypeId);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Pricing Test");
        metaField2.setGroupId(accountTypeId);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue(newUser.getUserName());
        metaField3.setGroupId(accountTypeId);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("ccf.payment_processor");
        metaField4.setValue("FAKE_2"); // the plug-in parameter of the processor

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4
        });

        logger.debug("Creating credit card");
        String ccName = "Frodo Baggins";
        String ccNumber = "4111111111111152";
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        PaymentInformationWS newCC = createCreditCard(ccName, ccNumber, expiry.getTime());
        newUser.getPaymentInstruments().add(newCC);

        if (doCreate) {
            logger.debug("Creating user: {}", newUser);
            newUser = api.getUserWS(api.createUser(newUser));
        }

        return newUser;
    }

    private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api) {
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for (OrderPeriodWS period : periods) {
            if (1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()) {
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "ORD:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

    private static PaymentInformationWS createCreditCard(String cardHolderName, String cardNumber, Date date) {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(CC_PM_ID);
        cc.setProcessingOrder(new Integer(1));
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_GATEWAY_KEY);

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
        addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
        addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
                DataType.CHAR, 3, (DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).print(date.getTime()).toCharArray()));
        // have to pass meta field card type for it to be set
        addMetaField(metaFields, CC_MF_TYPE, true, false,
                DataType.STRING, 4, CreditCardType.VISA);
        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }

    private static void addMetaField(List<MetaFieldValueWS> metaFields,
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

    private static OrderChangeWS buildFromItem(ItemDTOEx item, OrderWS order, Integer statusId) {
        OrderChangeWS ws = new OrderChangeWS();
        ws.setOptLock(1);
        ws.setOrderChangeTypeId(com.sapienter.jbilling.common.Constants.ORDER_CHANGE_TYPE_DEFAULT);
        ws.setUserAssignedStatusId(statusId);
        ws.setStartDate(Util.truncateDate(new Date()));
        ws.setOrderWS(order);

        ws.setUseItem(1);

        ws.setDescription(item.getDescription());
        ws.setItemId(item.getId());
        ws.setPrice(item.getPriceAsDecimal());
        ws.setQuantity("1");

        MetaFieldWS[] metaFields = item.getOrderLineMetaFields();
        List<MetaFieldValueWS> values = new ArrayList<MetaFieldValueWS>();
        if (metaFields != null) for (MetaFieldWS mf : metaFields) {
            MetaFieldValueWS value = MetaFieldBL.createValue(mf, "");
            value.setFieldName(mf.getName());
            values.add(value);
        }
        ws.setMetaFields(values.toArray(new MetaFieldValueWS[values.size()]));
        return ws;
    }

}
