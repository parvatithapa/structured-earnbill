package com.sapienter.jbilling.server.notification;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.notification.builder.NotificationBuilder;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.Thread.sleep;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Created by leandro on 01/06/17.
 */
@Test(groups = { "web-services", "notification" }, testName = "notification.WSTest")
public class WSTest {
    private static final String TEST_USER = "Test User";

    private static final Logger logger = LoggerFactory.getLogger(WSTest.class);

    private static final Integer TYPE_ID = MessageDTO.TYPE_PAYMENT_REFUND;
    private static final MessageSection MESSAGE_SECTION_1 = new MessageSection(1, "Subject");
    private static final MessageSection MESSAGE_SECTION_2 = new MessageSection(2, "Body");
    private static final MessageSection MESSAGE_SECTION_3 = new MessageSection(3, "Body (Html)");
    private static final MessageSection MESSAGE_SECTION_3_WRONG = new MessageSection(1, "so | IE]> </td></tr></table>");

    private static JbillingAPI api;
    private Integer orderChangeStatusApplyId;
    private Integer entityId =1;

    @BeforeTest
    public void initializeTests() throws Exception {
        api = JbillingAPIFactory.getAPI();
        orderChangeStatusApplyId = getOrCreateOrderChangeApplyStatus(api);
    }

    @AfterTest
    public void cleanUp(){
        api = null;
    }

    @Test
    public void test001CreateNotification() {
        NotificationBuilder notificationBuilder = new NotificationBuilder().withTypeId(TYPE_ID)
                                                                           .withContent(new MessageSection[]{
                                                                                   MESSAGE_SECTION_1,
                                                                                   MESSAGE_SECTION_2,
                                                                                   MESSAGE_SECTION_3
                                                                           });

        try {
            logger.debug("Creating Notification");
            api.createUpdateNotification(null, notificationBuilder.build());
        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            fail("The creation of the notification shouldn't fail");
        }
    }

    @Test
    public void test002CreateNotificationWithDesign() {
        NotificationBuilder notificationBuilder = new NotificationBuilder().withTypeId(TYPE_ID)
                                                                           .withIncludeAttachment(1)
                                                                           .withAttachmentDesign("invoice_design")
                                                                           .withContent(new MessageSection[]{
                                                                                   MESSAGE_SECTION_1,
                                                                                   MESSAGE_SECTION_2,
                                                                                   MESSAGE_SECTION_3
                                                                           });

        try {
            logger.debug("Creating Notification");
            api.createUpdateNotification(null, notificationBuilder.build());
        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            fail("The creation of the notification shouldn't fail");
        }
    }

    @Test
    public void test003CreateNotificationWithErrorInTheBody() {
        NotificationBuilder notificationBuilder = new NotificationBuilder().withTypeId(TYPE_ID)
                                                                           .withContent(new MessageSection[]{
                                                                                   MESSAGE_SECTION_1,
                                                                                   MESSAGE_SECTION_2,
                                                                                   MESSAGE_SECTION_3_WRONG
                                                                           });

        try {
            logger.debug("Creating Notification");
            api.createUpdateNotification(null, notificationBuilder.build());
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("notification.parse.template.error"));
        }
    }

    @Test
    public void test004CreateNotificationWithoutDesign() {
        NotificationBuilder notificationBuilder = new NotificationBuilder().withTypeId(TYPE_ID)
                                                                           .withIncludeAttachment(1)
                                                                           .withContent(new MessageSection[]{
                                                                                   MESSAGE_SECTION_1,
                                                                                   MESSAGE_SECTION_2,
                                                                                   MESSAGE_SECTION_3
                                                                           });

        try {
            logger.debug("Creating Notification");
            api.createUpdateNotification(null, notificationBuilder.build());
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("notification.enter.design.not.exists"));
        }
    }

    @Test
    public void testNotifyUserWithEmail() {

        try {

            Integer accountTypeId = createAccountType("Test Account Type:" + System.currentTimeMillis());

            //Creating AIT
            List<MetaFieldWS> metaFieldWSList = new ArrayList<>();

            MetaFieldWS metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.email");
            metaFieldWS.setFieldUsage(MetaFieldType.EMAIL);
            metaFieldWSList.add(metaFieldWS);
            Integer emailMF = api.createMetaField(metaFieldWS);

            metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.first.name");
            metaFieldWS.setFieldUsage(MetaFieldType.FIRST_NAME);
            metaFieldWSList.add(metaFieldWS);
            Integer firstName = api.createMetaField(metaFieldWS);

            metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.last.name");
            metaFieldWS.setFieldUsage(MetaFieldType.LAST_NAME);
            metaFieldWSList.add(metaFieldWS);
            Integer lastName = api.createMetaField(metaFieldWS);

            metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.address1");
            metaFieldWS.setFieldUsage(MetaFieldType.ADDRESS1);
            metaFieldWSList.add(metaFieldWS);
            Integer address1 = api.createMetaField(metaFieldWS);

            metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.address2");
            metaFieldWS.setFieldUsage(MetaFieldType.ADDRESS2);
            metaFieldWSList.add(metaFieldWS);
            Integer address2 = api.createMetaField(metaFieldWS);

            metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.city");
            metaFieldWS.setFieldUsage(MetaFieldType.CITY);
            metaFieldWSList.add(metaFieldWS);
            Integer city = api.createMetaField(metaFieldWS);

            metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.postalCode");
            metaFieldWS.setFieldUsage(MetaFieldType.POSTAL_CODE);
            metaFieldWSList.add(metaFieldWS);
            Integer postalCode = api.createMetaField(metaFieldWS);

            metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.state");
            metaFieldWS.setFieldUsage(MetaFieldType.STATE_PROVINCE);
            metaFieldWSList.add(metaFieldWS);
            Integer state = api.createMetaField(metaFieldWS);

            metaFieldWS = new MetaFieldWS();
            metaFieldWS.setDataType(DataType.STRING);
            metaFieldWS.setEntityType(EntityType.ACCOUNT_TYPE);
            metaFieldWS.setName("contact.organization");
            metaFieldWS.setFieldUsage(MetaFieldType.ORGANIZATION);
            metaFieldWSList.add(metaFieldWS);
            Integer organization = api.createMetaField(metaFieldWS);

            AccountInformationTypeWS ait = new AccountInformationTypeWS();
            ait.setName("Contact Information");
            ait.setUseForNotifications(true);
            ait.setAccountTypeId(accountTypeId);

            ait.setDateCreated(new Date(1970, 1, 1));
            ait.setDateUpdated(null);
            ait.setEntityId(1);
            ait.setEntityType(EntityType.ACCOUNT_TYPE);
            ait.setDisplayOrder(Integer.valueOf(1));

            List<InternationalDescriptionWS> descriptions = new ArrayList<>();
            InternationalDescriptionWS internationalDescriptionWS = new InternationalDescriptionWS(
                    Constants.LANGUAGE_ENGLISH_ID, "Contact Information");
            descriptions.add(internationalDescriptionWS);

            ait.setDescriptions(descriptions);
            ait.setMetaFields(metaFieldWSList.toArray(new MetaFieldWS[metaFieldWSList.size()]));
            Integer aitId = api.createAccountInformationType(ait);

            Integer userId = createUser(accountTypeId, aitId);

            logger.debug("Creating category ...");
            Integer categoryId = createItemType(true);

            logger.debug("Creating items ...");
            Integer item1 = createItem(true, categoryId);
            Integer item2 = createItem(true, categoryId);

            logger.debug("Creating plans ...");
            Integer planItemId1 = createItemDTOEx(categoryId, "Old Plan");
            Integer plan1 = createPlans(item1, planItemId1, "Old Plan");
            Integer planItemId2 = createItemDTOEx(categoryId, "New Plan");
            Integer plan2 = createPlans(item2, planItemId2, "New Plan");

            Integer orderId = createOrder(userId, planItemId1);

            logger.debug("Order created with id " + orderId);

            OrderWS order = api.getOrder(orderId);

            OrderChangeWS[] orderChanges = api.calculateSwapPlanChanges(order, planItemId1
                    , planItemId2, SwapMethod.DIFF, Util.truncateDate(order.getActiveSince()));
            assertNotNull("Swap changes should be calculated", orderChanges);

            api.createUpdateOrder(order, orderChanges);

            logger.debug("Creating Notification");

            Integer notificationId = api.createMessageNotificationType(4, "Test Notification for testNotifyUserWithEmail test case" +
                    Calendar.getInstance().getTimeInMillis(), 1);

            logger.debug("Notification created with id " + notificationId);

            NotificationBuilder notificationBuilder = new NotificationBuilder().withTypeId(notificationId)
                    .withContent(new MessageSection[]{
                            new MessageSection(1, "Test Notification for testNotifyUserWithEmail test case"),
                            new MessageSection(2, "First Name:$first_name\n" +
                                    "Last Name:$last_name\n" +
                                    "Address 1:$address1\n" +
                                    "Address 2:$address2\n" +
                                    "City:$city\n" +
                                    "Postal Code:$postal_code\n" +
                                    "State:$state_province\n" +
                                    "Organization:$organization_name\n" +
                                    "Username:$username\n" +
                                    "Password:$password\n" +
                                    "User Id:$user_id\n" +
                                    "old_plan_description:$old_plan_description\n" +
                                    "new_plan_description:$new_plan_description"),
                            MESSAGE_SECTION_3
                    });

            api.getIdFromCreateUpdateNotification(null, notificationBuilder.build());

            boolean result = false;

            try{
                result = api.notifyUserByEmail(null, notificationId);
            }catch (Exception exception){
                assertEquals(exception.getMessage().contains("User id can not be null"), true);
            }

            try {
                result = api.notifyUserByEmail(userId, null);
            }catch (Exception exception){
                assertEquals(exception.getMessage().contains("Notification id can not be null"), true);
            }

            result = api.notifyUserByEmail(userId, notificationId);

            assertEquals(true, result);

            sleep(5000);

            byte[] encoded = Files.readAllBytes(Paths.get(Util.getSysProp("base_dir") + "/emails_sent.txt"));
            String content = new String(encoded, "ISO-8859-1");

            assertEquals(true, content.contains("To: test@test.com\n" +
                    "From: admin@prancingpony.me\n" +
                    "Subject: Test Notification for testNotifyUserWithEmail test case\n" +
                    "Body: First Name:Test User\n" +
                    "Last Name:Test User\n" +
                    "Address 1:Address1\n" +
                    "Address 2:Address2\n" +
                    "City:XYZ\n" +
                    "Postal Code:000000\n" +
                    "State:XYZ\n" +
                    "Organization:XYZ"));
            logger.debug("##content :: "+content);

            ItemDTOEx itemDTO1 = api.getItem(planItemId1, userId, null);
            ItemDTOEx itemDTO2 = api.getItem(planItemId2, userId, null);

            logger.debug("##itemDTO1.getDescription() : "+itemDTO1.getDescription());
            logger.debug("##itemDTO2.getDescription() : "+itemDTO2.getDescription());

            assertEquals(content.contains("User Id:"+userId+"\n" +
                    "old_plan_description:"+itemDTO1.getDescription()+"\n" +
                    "new_plan_description:"+itemDTO2.getDescription()),true);

            api.deleteOrder(orderId);
            api.deleteUser(userId);
            api.deleteAccountType(accountTypeId);
            api.deletePlan(plan2);
            api.deletePlan(plan1);
            api.deleteItem(item2);
            api.deleteItem(item1);
            api.deleteItemCategory(categoryId);
            api.deleteMetaField(firstName);
            api.deleteMetaField(lastName);
            api.deleteMetaField(city);
            api.deleteMetaField(state);
            api.deleteMetaField(organization);
            api.deleteMetaField(emailMF);
            api.deleteMetaField(postalCode);
            api.deleteMetaField(address1);
            api.deleteMetaField(address2);

        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            fail("Exception occurred");
        }
    }

    private Integer createUser(Integer accountTypeId, Integer aitId) {
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName(TEST_USER + "-"
                + Calendar.getInstance().getTimeInMillis());

        newUser.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        newUser.setAccountTypeId(accountTypeId);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        newUser.setInvoiceChild(false);
        newUser.setMainRoleId(com.sapienter.jbilling.client.util.Constants.ROLE_CUSTOMER);

        MetaFieldValueWS email = new MetaFieldValueWS();
        email.setFieldName("contact.email");
        email.setStringValue("test@test.com");
        email.setGroupId(aitId);

        MetaFieldValueWS firstNameMFV = new MetaFieldValueWS();
        firstNameMFV.setFieldName("contact.first.name");
        firstNameMFV.setStringValue(TEST_USER);
        firstNameMFV.setGroupId(aitId);

        MetaFieldValueWS lastNameMFV = new MetaFieldValueWS();
        lastNameMFV.setFieldName("contact.last.name");
        lastNameMFV.setStringValue(TEST_USER);
        lastNameMFV.setGroupId(aitId);

        MetaFieldValueWS address1MFV = new MetaFieldValueWS();
        address1MFV.setFieldName("contact.address1");
        address1MFV.setStringValue("Address1");
        address1MFV.setGroupId(aitId);

        MetaFieldValueWS address2MFV = new MetaFieldValueWS();
        address2MFV.setFieldName("contact.address2");
        address2MFV.setStringValue("Address2");
        address2MFV.setGroupId(aitId);

        MetaFieldValueWS cityMFV = new MetaFieldValueWS();
        cityMFV.setFieldName("contact.city");
        cityMFV.setStringValue("XYZ");
        cityMFV.setGroupId(aitId);

        MetaFieldValueWS postalCodeMFV = new MetaFieldValueWS();
        postalCodeMFV.setFieldName("contact.postalCode");
        postalCodeMFV.setStringValue("000000");
        postalCodeMFV.setGroupId(aitId);

        MetaFieldValueWS stateMFV = new MetaFieldValueWS();
        stateMFV.setFieldName("contact.state");
        stateMFV.setStringValue("XYZ");
        stateMFV.setGroupId(aitId);

        MetaFieldValueWS organizationMFV = new MetaFieldValueWS();
        organizationMFV.setFieldName("contact.organization");
        organizationMFV.setStringValue("XYZ");
        organizationMFV.setGroupId(aitId);

        newUser.setMetaFields(new MetaFieldValueWS[]{email,firstNameMFV,lastNameMFV, address1MFV,
                address2MFV, cityMFV, postalCodeMFV, stateMFV, organizationMFV});

        logger.debug("Creating user ...");
        Integer userId = api.createUser(newUser);
        logger.debug("User created with " + userId);

        return userId;
    }


    private Integer createItemType(boolean global) {
        ItemTypeWS itemType = new ItemTypeWS();
        String categoryName = "ignition-test-category:" + System.currentTimeMillis();

        itemType.setDescription(categoryName);
        itemType.setEntityId(entityId);
        if (global) {
            itemType.setGlobal(global);
        } else {
            itemType.setEntities(new ArrayList<Integer>(entityId));
        }
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);

        Integer categoryId = api.createItemCategory(itemType);

        logger.debug("Item category created with id " + categoryId + " name " + categoryName);

        return categoryId;
    }

    private Integer createItem(boolean global, Integer type) {
        ItemDTOEx item = new ItemDTOEx();
        item.setDescription("TestItem: " + System.currentTimeMillis());
        item.setNumber("TestWS-" + System.currentTimeMillis());
        item.setTypes(new Integer[]{type});
        item.setPrice(new BigDecimal(55));

        item.setExcludedTypes(new Integer[]{});
        if (global) {
            item.setGlobal(global);
        } else {
            item.setGlobal(false);
        }
        item.setEntityId(entityId);
        Integer itemId = api.createItem(item);

        logger.debug("Item created with id " + itemId);

        return itemId;
    }

    private Integer createOrder(Integer userId, Integer itemId) {
        // Create

        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(Constants.ORDER_PERIOD_ONCE);
        order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2018, 1, 1);
        order.setActiveSince(cal.getTime());

        // Add Lines
        OrderLineWS[] lines = new OrderLineWS[1];

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setDescription(String.format("Line for product %d", itemId));
        line.setItemId(itemId);

        line.setUseItem(Boolean.TRUE);
        lines[0] = line;

        order.setOrderLines(lines);

        logger.debug("Creating order ... {}", order);
        return api.createOrder(order, OrderChangeBL.buildFromOrder(order, orderChangeStatusApplyId));
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
            newStatus.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status1"));
            return api.createOrderChangeStatus(newStatus);
        }
    }

    private Integer createAccountType(String description){
        AccountTypeWS accountType = new AccountTypeWS();
        accountType.setEntityId(entityId);
        accountType.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        accountType.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        accountType.setCreditLimit(BigDecimal.ZERO);
        accountType.setInvoiceDeliveryMethodId(1);
        accountType.setMainSubscription(new MainSubscriptionWS(
                Constants.PERIOD_UNIT_MONTH, 1));

        List<InternationalDescriptionWS> descriptions = new ArrayList<>();
        InternationalDescriptionWS internationalDescriptionWS = new InternationalDescriptionWS(
                Constants.LANGUAGE_ENGLISH_ID, description);
        descriptions.add(internationalDescriptionWS);
        accountType.setDescriptions(descriptions);
        Integer accountTypeId = api.createAccountType(accountType);

        logger.debug("Account type created with id " + accountTypeId);

        return accountTypeId;
    }

    private Integer createItemDTOEx(Integer categoryId, String planNumber){
        ItemDTOEx itemDTOEx1 = new ItemDTOEx();
        itemDTOEx1.setDescription("Test Plan " + Calendar.getInstance().getTimeInMillis());
        itemDTOEx1.setEntityId(entityId);
        itemDTOEx1.setTypes(new Integer[]{categoryId});
        itemDTOEx1.setPrice("1");
        itemDTOEx1.setNumber(planNumber);
        itemDTOEx1.setActiveSince(Util.getDate(2010, 1, 1));
        return api.createItem(itemDTOEx1);
    }

    private Integer createPlans(Integer itemId, Integer planItemId, String description){

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, 1);
        SortedMap<Date, PriceModelWS> models = new TreeMap<>();
        models.put(Constants.EPOCH_DATE, priceModel);

        PlanItemBundleWS bundle1 = new PlanItemBundleWS();
        bundle1.setPeriodId(2);
        bundle1.setQuantity(BigDecimal.ONE);
        PlanItemWS pi1 = new PlanItemWS();
        pi1.setItemId(itemId);
        pi1.setPrecedence(-1);
        pi1.setModels(models);
        pi1.setBundle(bundle1);

        PlanWS plan1 = new PlanWS();
        plan1.setDescription(description);
        plan1.setItemId(planItemId);
        plan1.setPeriodId(2);
        plan1.addPlanItem(pi1);
        Integer planId1 = api.createPlan(plan1);

        logger.debug("Plan created with id " + planId1);

        return planId1;
    }

}
