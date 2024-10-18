package com.sapienter.jbilling.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.appdirect.subscription.PayloadWS;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.AssetStatusDTOEx;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeTypeWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.payment.PaymentInformationRestWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.PreferenceTypeWS;
import com.sapienter.jbilling.server.util.PreferenceWS;

import org.joda.time.format.DateTimeFormat;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Vojislav Stanojevikj
 * @since 17-Oct-2016.
 */
final class RestEntitiesHelper {

    private RestEntitiesHelper() {}

    private static final String META_FIELD_NAME = "test.email";
    private static final String META_FIELD_DEFAULT_VALUE = "testRest@test.com";
    private static final String VALIDATION_RULE_ERROR_MESSAGE = "InvalidEmail!";
    private static final String RULE_ATTRIBUTE_KEY = "validationScript";
    private static final String RULE_ATTRIBUTE_VALUE = "_this==~/[_A-Za-z0-9-]+(.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})/";
    private static final String RULE_TYPE = "SCRIPT";

    private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
    private final static String CC_MF_NUMBER = "cc.number";
    private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
    private final static String CC_MF_TYPE = "cc.type";

    public static final Date DUMMY_TEST_DATE = Date.from(LocalDateTime.of(2010, 4, 5, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    public static final Date DUMMY_TEST_DATE_2 = Date.from(LocalDateTime.of(2010, 4, 7, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

    public static final Integer TEST_ENTITY_ID = Integer.valueOf(1);
    public static final Integer TEST_LANGUAGE_ID = Integer.valueOf(1);
    public static final Integer TEST_CURRENCY_ID = Integer.valueOf(1);

    private static final Integer ENABLED = Integer.valueOf(1);
    private static final Integer DISABLED = Integer.valueOf(0);

    public static AccountTypeWS buildAccountTypeMock(int id, String name){

        AccountTypeWS accountType = new AccountTypeWS(id, "");
        accountType.setEntityId(TEST_ENTITY_ID);
        accountType.setInvoiceTemplateId(Integer.valueOf(1));
        accountType.setDateCreated(DUMMY_TEST_DATE);
        accountType.setInvoiceDeliveryMethodId(Integer.valueOf(1));
        accountType.setCurrencyId(TEST_CURRENCY_ID);
        accountType.setLanguageId(TEST_LANGUAGE_ID);
        accountType.addDescription(new InternationalDescriptionWS("description", Integer.valueOf(1), name));
        accountType.setMainSubscription(new MainSubscriptionWS(Integer.valueOf(2), Integer.valueOf(1)));
        accountType.setCreditLimit(BigDecimal.ZERO);
        accountType.setCreditNotificationLimit1(BigDecimal.ZERO);
        accountType.setCreditNotificationLimit2(BigDecimal.ZERO);

        return accountType;
    }

    public static AgeingWS[] buildAgeingSteps() {
        AgeingWS[] ageingSteps = new AgeingWS[4];
        ageingSteps[0] = buildAgeingStep("Payment Due", Integer.valueOf(0), false, false, false);
        ageingSteps[1] = buildAgeingStep("Grace Period", Integer.valueOf(2), false, true, false);
        ageingSteps[2] = buildAgeingStep("First Retry", Integer.valueOf(3), true, false, false);
        ageingSteps[3] = buildAgeingStep("Suspended", Integer.valueOf(7), false, false, true);
        return ageingSteps;
    }

    public static AgeingWS buildAgeingStep(String statusStep,Integer days,
                                     boolean payment , boolean sendNotification, boolean suspended){

        AgeingWS ageingWS = new AgeingWS();
        ageingWS.setEntityId(TEST_ENTITY_ID);
        ageingWS.setStatusStr(statusStep);
        ageingWS.setDays(days);
        ageingWS.setPaymentRetry(Boolean.valueOf(payment));
        ageingWS.setSendNotification(Boolean.valueOf(sendNotification));
        ageingWS.setSuspended(Boolean.valueOf(suspended));
        ageingWS.setStopActivationOnPayment(false);
        ageingWS.setCollectionType(CollectionType.REGULAR);
        return  ageingWS;
    }

    public static AccountInformationTypeWS buildAccountInformationTypeMock(Integer accountTypeId, String name){

        AccountInformationTypeWS accountInformationType = new AccountInformationTypeWS();
        accountInformationType.setAccountTypeId(accountTypeId);
        accountInformationType.setName(name);
        accountInformationType.setDateCreated(DUMMY_TEST_DATE);
        accountInformationType.setDateUpdated(DUMMY_TEST_DATE_2);
        accountInformationType.setEntityId(TEST_ENTITY_ID);
        accountInformationType.setDisplayOrder(Integer.valueOf(1));
        accountInformationType.setMetaFields(new MetaFieldWS[]{buildEmailMetaField(0, EntityType.ACCOUNT_TYPE)});

         return accountInformationType;
    }

    public static MetaFieldWS buildEmailMetaField(Integer id, EntityType entityType){
        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setId(id);
        metaField.setEntityId(TEST_ENTITY_ID);
        metaField.setName(META_FIELD_NAME);
        metaField.setEntityType(entityType);
        metaField.setDataType(DataType.STRING);
        metaField.setFieldUsage(MetaFieldType.EMAIL);
        metaField.setDefaultValue(new MetaFieldValueWS(META_FIELD_NAME, null, DataType.STRING, false, META_FIELD_DEFAULT_VALUE));
        ValidationRuleWS validationRule = new ValidationRuleWS();
        validationRule.addErrorMessage(TEST_LANGUAGE_ID, VALIDATION_RULE_ERROR_MESSAGE);
        validationRule.addRuleAttribute(RULE_ATTRIBUTE_KEY, RULE_ATTRIBUTE_VALUE);
        validationRule.setRuleType(RULE_TYPE);
        metaField.setValidationRule(validationRule);

        return metaField;
    }

    public static MetaFieldWS buildMetaField(Integer id, EntityType entityType,
                                             DataType dataType, String filename){
        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setId(id);
        metaField.setEntityId(TEST_ENTITY_ID);
        metaField.setName(META_FIELD_NAME);
        metaField.setEntityType(entityType);
        metaField.setDataType(dataType);
        metaField.setFilename(filename);

        return metaField;
    }

    public static MetaFieldWS buildMetaField(Integer id, EntityType entityType,
                                             DataType dataType, String filename, String name){
        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setId(id);
        metaField.setEntityId(TEST_ENTITY_ID);
        metaField.setName(name);
        metaField.setEntityType(entityType);
        metaField.setDataType(dataType);
        metaField.setFilename(filename);

        return metaField;
    }

    public static MetaFieldValueWS buildMetaFieldValue(String metaFieldName, String value, Integer groupId){
        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS(metaFieldName, groupId, DataType.STRING, false, value);
        metaFieldValueWS.setDefaultValue(META_FIELD_DEFAULT_VALUE);
        metaFieldValueWS.setDisplayOrder(Integer.valueOf(1));
        return metaFieldValueWS;
    }

    public static UserWS buildUserMock(String userName, Integer accountTypeId){
        return buildUserMock(userName, accountTypeId, false);
    }

    public static UserWS buildUserMock(String userName, Integer accountTypeId, boolean useTimeStampInUserName){
        UserWS newUser = new UserWS();
        newUser.setUserId(0);
        if(useTimeStampInUserName) {
            userName = userName + System.currentTimeMillis();
        }
        newUser.setUserName(userName);
        newUser.setPassword("P@ssword1");
        newUser.setLanguageId(TEST_LANGUAGE_ID); // US
        newUser.setMainRoleId(Integer.valueOf(5)); // Role Customer
        newUser.setAccountTypeId(accountTypeId);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(TEST_CURRENCY_ID); // USD
        newUser.setEntityId(TEST_ENTITY_ID); // Prancing pony
        newUser.setInvoiceTemplateId(Integer.valueOf(1));
        newUser.setCreditLimit(BigDecimal.ZERO);
        newUser.setMainSubscription(new MainSubscriptionWS(Integer.valueOf(2), Integer.valueOf(1)));

        return newUser;
    }

	public static UserWS buildUserWithCustomerMetafieldMock(String username, Integer accountTypeId,
			String metaFieldName, String value, boolean shouldUseTimeStampInUserName){

		UserWS newUser = buildUserMock(username, accountTypeId, shouldUseTimeStampInUserName);
		MetaFieldValueWS metaFieldValueWS = buildMetaFieldValue(metaFieldName, value, null);
		newUser.setMetaFields(new MetaFieldValueWS[]{metaFieldValueWS});
		return newUser;
    }

    public static OrderPeriodWS buildOrderPeriodMock(Integer periodUnitId, Integer periodValue, String name){

        OrderPeriodWS orderPeriod = new OrderPeriodWS(null, TEST_ENTITY_ID, periodUnitId, periodValue);
        orderPeriod.setDescriptions(Arrays.asList(new InternationalDescriptionWS("description", TEST_LANGUAGE_ID, name)));
        return orderPeriod;
    }

    public static ItemTypeWS buildItemTypeMock(String name, boolean global, boolean allowAssetManagement){

        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription(name);
        itemType.setEntityId(TEST_ENTITY_ID);
        if(global) {
            itemType.setGlobal(true);
        }
        itemType.setEntities(Arrays.asList(TEST_ENTITY_ID));
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        if(allowAssetManagement) {
            itemType.setAllowAssetManagement(ENABLED);
            itemType.setAssetIdentifierLabel("Test Asset Label");
            addAssetStatuses(itemType);
            Set<MetaFieldWS> metaFields = new HashSet<>();
            metaFields.add(buildMetaField(Integer.valueOf(0), EntityType.ASSET, DataType.STRING, "", "asset.test"));
            itemType.setAssetMetaFields(metaFields);
        }
        return itemType;
    }

    private static void addAssetStatuses(ItemTypeWS itemType){

        // Default
        AssetStatusDTOEx status = new AssetStatusDTOEx();
        status.setDescription("One");
        status.setIsDefault(ENABLED);
        itemType.getAssetStatuses().add(status);

        // Available
        status = new AssetStatusDTOEx();
        status.setDescription("Two");
        status.setIsAvailable(ENABLED);
        itemType.getAssetStatuses().add(status);

        // Order Saved and Active
        status = new AssetStatusDTOEx();
        status.setDescription("Three");
        status.setIsOrderSaved(ENABLED);
        status.setIsActive(ENABLED);
        itemType.getAssetStatuses().add(status);

        // Reserved
        status = new AssetStatusDTOEx();
        status.setDescription("Four");
        itemType.getAssetStatuses().add(status);

        // Pending
        status = new AssetStatusDTOEx();
        status.setDescription("Five");
        status.setIsOrderSaved(ENABLED);
        status.setIsPending(ENABLED);
        itemType.getAssetStatuses().add(status);

    }

    public static ItemDTOEx buildItemMock(String name, boolean allowAssetManagement,
                                          boolean global, Integer... types){
        ItemDTOEx item = new ItemDTOEx();
        item.setDescription(name);
        item.setHasDecimals(DISABLED);
        item.setDeleted(DISABLED);
        item.setEntityId(TEST_ENTITY_ID);
        item.setCurrencyId(TEST_CURRENCY_ID);
        item.setEntities(Arrays.asList(TEST_ENTITY_ID));
        PriceModelWS defaultPrice = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, TEST_CURRENCY_ID);
        item.setDefaultPrice(defaultPrice);
        item.addDefaultPrice(new Date(), defaultPrice);
        //item.setPriceModelCompanyId(TEST_CURRENCY_ID);
        item.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        if(allowAssetManagement){
            item.setAssetManagementEnabled(ENABLED);
        }
        item.setNumber(name);
        item.setTypes(types);
        if(global) {
            item.setGlobal(true);
        }
        item.setExcludedTypes(new Integer[0]);
        return item;
    }

    public static AssetWS buildAssetMock(String identifierValue, Integer itemId, Integer defaultStatusId) {

        AssetWS asset = new AssetWS();
        asset.setIdentifier(identifierValue);
        asset.setItemId(itemId);
        asset.setEntityId(TEST_ENTITY_ID);
        asset.setEntities(Arrays.asList(TEST_ENTITY_ID));
        asset.setAssetStatusId(defaultStatusId);
        asset.setDeleted(DISABLED);
        return asset;
    }

    public static PlanWS buildPlanMock(Integer subscriptionItemId, String name, Integer periodId,
                                       RestPlanItem restPlanItem, RestPlanItem... restPlanItems){
        PlanWS planWS = new PlanWS();
        planWS.setItemId(subscriptionItemId);
        planWS.setDescription(name);
        planWS.setPeriodId(periodId);

        planWS.addPlanItem(buildPlanItemMock(restPlanItem));
        for (RestPlanItem pi : restPlanItems){
            planWS.addPlanItem(buildPlanItemMock(pi));
        }
        return planWS;
    }

    public static PlanItemWS buildPlanItemMock(RestPlanItem restPlanItem){

        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setPeriodId(restPlanItem.getPeriodId());
        bundle.setQuantity(restPlanItem.getQuantity());
        PlanItemWS planItem = new PlanItemWS();
        planItem.setItemId(restPlanItem.getItemId());
        planItem.setPrecedence(-1);
        SortedMap<Date, PriceModelWS> prices = new TreeMap<>();
        prices.put(Util.truncateDate(new Date()), new PriceModelWS(PriceModelStrategy.FLAT.name(), restPlanItem.getPrice(), TEST_CURRENCY_ID));
        planItem.setModels(prices);
        planItem.setBundle(bundle);

        return planItem;
    }

    public static PaymentMethodTypeWS buildPaymentMethodTypeMock(PaymentMethodTemplateWS paymentMethodTemplateWS){
        PaymentMethodTypeWS paymentMethod = new PaymentMethodTypeWS();
        paymentMethod.setAllAccountType(Boolean.TRUE);
        Set<MetaFieldWS> templateMetaFields = paymentMethodTemplateWS.getMetaFields();
        MetaFieldWS[] metaFields;
        if (templateMetaFields != null && templateMetaFields.size() > 0) {
            metaFields = new MetaFieldWS[templateMetaFields.size()];
            Integer i = 0;
            for (MetaFieldWS metaField : templateMetaFields) {
                MetaFieldWS mf = copyMetaField(metaField);
                mf.setEntityId(TEST_ENTITY_ID);
                mf.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
                metaFields[i] = mf;

                i++;
            }
        } else {
            metaFields = new MetaFieldWS[0];
        }
        paymentMethod.setMetaFields(metaFields);
        paymentMethod.setTemplateId(paymentMethodTemplateWS.getId());
        paymentMethod.setMethodName("CC-" + System.currentTimeMillis());
        paymentMethod.setIsRecurring(false);
        return paymentMethod;
    }

    private static MetaFieldWS copyMetaField(MetaFieldWS metaField) {
        MetaFieldWS mf = new MetaFieldWS();
        mf.setDataType(metaField.getDataType());
        mf.setDefaultValue(metaField.getDefaultValue());
        mf.setDisabled(metaField.isDisabled());
        mf.setDisplayOrder(metaField.getDisplayOrder());
        mf.setFieldUsage(metaField.getFieldUsage());
        mf.setFilename(metaField.getFilename());
        mf.setMandatory(metaField.isMandatory());
        mf.setName(metaField.getName());
        mf.setValidationRule(metaField.getValidationRule());
        mf.setPrimary(metaField.isPrimary());

        // set rule id to 0 so a new rule will be created
        if (mf.getValidationRule() != null) {
            mf.getValidationRule().setId(0);
        }

        return mf;
    }

    public static PaymentWS buildPaymentMock(Integer userId, Date date, Integer methodTypeId){

        Date truncatedDate = Util.truncateDate(date);
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal("15.00"));
        payment.setIsRefund(Integer.valueOf(0));
        payment.setMethodId(CommonConstants.PAYMENT_METHOD_VISA);
        payment.setPaymentDate(truncatedDate);
        payment.setCreateDatetime(date);
        payment.setResultId(Constants.RESULT_ENTERED);
        payment.setCurrencyId(TEST_CURRENCY_ID);
        payment.setUserId(userId);
        payment.setPaymentPeriod(Integer.valueOf(1));

        LocalDateTime local = truncatedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().plusYears(1);
        Date expiry = new Date(local.atZone(ZoneId.systemDefault()).toEpochSecond());
        PaymentInformationWS cheque = buildCCPaymentInstrumentMock(methodTypeId, "TestHolder", "4111111111111112", expiry);
        payment.getPaymentInstruments().add(cheque);

        return payment;
    }

    public static PaymentInformationWS buildCCPaymentInstrumentMock(Integer methodTypeId, String cardHolderName,
                                                                    String cardNumber, Date date){

        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(methodTypeId);
        cc.setProcessingOrder(Integer.valueOf(1));
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true,
                DataType.STRING, 1, cardHolderName);
        addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.STRING, 2,
                cardNumber);
        addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
                DataType.STRING, 3, DateTimeFormat.forPattern(
                        Constants.CC_DATE_FORMAT).print(date.getTime()));
        // have to pass meta field card type for it to be set
        addMetaField(metaFields, CC_MF_TYPE, true, false,
                DataType.STRING, 4, String.valueOf("Visa") );
        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }

    private static void addMetaField(List<MetaFieldValueWS> metaFields,
                                     String fieldName, boolean disabled, boolean mandatory,
                                     DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.setDisabled(disabled);
        ws.setMandatory(mandatory);
        ws.setDataType(dataType);
        ws.setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws);
    }

    public static Integer findDefaultAssetStatusId(ItemTypeWS itemType){
        if (null != itemType && itemType.getAllowAssetManagement().equals(Integer.valueOf(1))){
            for (AssetStatusDTOEx status : itemType.getAssetStatuses()){
                if (status.getIsDefault() == 1){
                    return status.getId();
                }
            }
        }
        return null;
    }

    public static Integer findAssetStatusIdByName(ItemTypeWS itemType, String name){
        if (null != itemType && itemType.getAllowAssetManagement().equals(Integer.valueOf(1))){
            for (AssetStatusDTOEx status : itemType.getAssetStatuses()){
                if (status.getDescription().equalsIgnoreCase(name)){
                    return status.getId();
                }
            }
        }
        return null;
    }

    @SafeVarargs
    public static <T> String addQueryParamsToUrl(String url, RestQueryParameter<T>... queryParameters){

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        for (RestQueryParameter<T> queryParameter : queryParameters){
            if (null != queryParameter)
            builder.queryParam(queryParameter.getParameterName(), queryParameter.getParameterValue());
        }
        return builder.build().encode().toUriString();
    }

    public static OrderStatusWS buildOrderStatus(Integer id, Integer entityId,
                                                 OrderStatusFlag flag, String statusName) {

        OrderStatusWS orderStatus = new OrderStatusWS(id, new CompanyWS(entityId), flag, statusName);
        orderStatus.setDescription(statusName);
        List descList = new ArrayList<InternationalDescriptionWS>();
        descList.add(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, statusName));
        orderStatus.setDescriptions(descList);
        return orderStatus;
    }

    public static OrderChangeStatusWS buildOrderChangeStatus(Integer id, Integer entityId, ApplyToOrder applyToOrder,
                                                             Integer order, String description) {

        OrderChangeStatusWS orderChangeStatus = new OrderChangeStatusWS();
        orderChangeStatus.setId(id);
        orderChangeStatus.setEntityId(entityId);
        orderChangeStatus.setOrder(order);
        orderChangeStatus.setApplyToOrder(applyToOrder);
        orderChangeStatus.setDeleted(Integer.valueOf(0));
        orderChangeStatus.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, description));
        return orderChangeStatus;
    }

    public static OrderChangeTypeWS buildOrderChangeType(Integer id, Integer entityId, String name) {

        OrderChangeTypeWS orderChangeType = new OrderChangeTypeWS();
        orderChangeType.setId(id);
        orderChangeType.setEntityId(entityId);
        orderChangeType.setName(name);
        orderChangeType.setDefaultType(Boolean.FALSE);
        orderChangeType.setAllowOrderStatusChange(Boolean.TRUE);
        return orderChangeType;
    }

    public static PluggableTaskTypeCategoryWS buildPluginTypeCategory(Integer id, String interfaceName) {

        PluggableTaskTypeCategoryWS pluginTypeCat = new PluggableTaskTypeCategoryWS();
        pluginTypeCat.setId(id);
        pluginTypeCat.setInterfaceName(interfaceName);
        return pluginTypeCat;
    }

    public static PluggableTaskTypeWS buildPluginType(Integer id, Integer categoryId, String className, Integer minParam) {

        PluggableTaskTypeWS pluginType = new PluggableTaskTypeWS();
        pluginType.setId(id);
        pluginType.setCategoryId(categoryId);
        pluginType.setClassName(className);
        pluginType.setMinParameters(minParam);
        return pluginType;
    }

    public static PluggableTaskWS buildPlugin(Integer id, Integer owningId, Integer typeId, String notes,
                                              Integer processingOrder, Hashtable<String, String> parameters) {

        PluggableTaskWS plugin = new PluggableTaskWS();
        plugin.setId(id);
        plugin.setProcessingOrder(processingOrder);
        plugin.setNotes(notes);
        plugin.setTypeId(typeId);
        plugin.setParameters(parameters);
        plugin.setOwningEntityId(owningId);
        return plugin;
    }

    public static PreferenceWS buildPreference(Integer id, Integer tableId, Integer foreignId,
                                               PreferenceTypeWS type, String value) {

        PreferenceWS preference = new PreferenceWS();
        preference.setId(id);
        preference.setTableId(tableId);
        preference.setForeignId(foreignId);
        preference.setPreferenceType(type);
        preference.setValue(value);
        return preference;
    }

    public static PreferenceTypeWS buildPreferenceType(Integer id, String description,
                                                       String defaultValue, ValidationRuleWS validationRule) {

        PreferenceTypeWS preferenceType = new PreferenceTypeWS();
        preferenceType.setId(id);
        preferenceType.setDescription(description);
        preferenceType.setDefaultValue(defaultValue);
        preferenceType.setValidationRule(validationRule);
        return preferenceType;
    }

    public static PayloadWS buildPayloadMockFromJsonString(String childExtAccntId, String subscriptionId) {

        String payloadStr = "{\"uuid\": \"81c7378f-4e1d-49dd-b0aa-0f38194342b8\",\"timestamp\": 1520929068925," +
                "\"resource\": {\"type\": \"SUBSCRIPTION\",\"uuid\": \"" + subscriptionId + "\",\"url\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/billing/v1/subscriptions/5548c36d-80a8-42c1-be81-0f3179691e15\"," +
                "\"content\": {\"id\": \"5548c36d-80a8-42c1-be81-0f3179691e15\",\"parentSubscriptionId\": null,\"creationDate\": 1520929068000,\"endDate\": null,\"externalAccountId\": \"" + childExtAccntId + "\",\"status\": \"ACTIVE\",\n" +
                "\"maxUsers\": null, \"assignedUsers\": 1, \"order\": {\"startDate\": 1520913600000, \"endDate\": 1523592000000, \"serviceStartDate\": \"2018-03-13\", \"nextBillingDate\": 1523592000000,\"endOfDiscountDate\": null, \"status\": \"ACTIVE\"," +
                "\"frequency\": \"MONTHLY\",\"currency\": \"EUR\",\"type\": \"NEW\",\"totalPrice\": 0,\"user\": {\"id\": \"bda34090-7327-4be3-8f81-03c18a3d6bac\",\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/account/v1/users/bda34090-7327-4be3-8f81-03c18a3d6bac\"" +
                "},\"salesSupportUser\": null,\"salesSupportCompany\": null,\"company\": {\"id\": \"external-account-id-for-parent-uuid\",\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/account/v1/companies/6d07f0e5-13c5-4ac8-b481-47ebf9c0b07e\" " +
                "},\"referenceCode\": null,\"paymentPlan\": {\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/marketplace/v1/products/3851/editions/9130/paymentPlans/10527\",\"id\": 10527," +
                "\"uuid\": \"b4cc8b1c-40e7-49e6-890b-e4545f20523d\",\"frequency\": \"MONTHLY\",\"contract\": {\"minimumServiceLength\": 1,\"cancellationPeriodLimit\": null,\"endOfContractGracePeriod\": 30, " +
                "\"blockSwitchToShorterContract\": true,\"blockContractDowngrades\": true,\"blockContractUpgrades\": false,\"alignWithParentCycleStartDate\": false,\"gracePeriod\": null,\"terminationFee\": null," +
                "\"autoExtensionPricingId\": 10527},\"allowCustomUsage\": true,\"keepBillDateOnUsageChange\": true,\"separatePrepaid\": false,\"isPrimaryPrice\": false," +
                "\"costs\": [],\"discount\": null,\"primaryPrice\": false},\"contract\": {\"minimumServiceLength\": 1,\"endOfContractDate\": 1523592000000,\"gracePeriodEndDate\": null," +
                "\"cancellationPeriodLimit\": null,\"endOfContractGracePeriod\": 30,\"terminationFee\": null,\"renewal\": {\"order\": null,\"paymentPlan\": {\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/marketplace/v1/products/3851/editions/9130/paymentPlans/10527\"," +
                "\"id\": 10527,\"uuid\": \"b4cc8b1c-40e7-49e6-890b-e4545f20523d\",\"frequency\": \"MONTHLY\",\"contract\": {\"minimumServiceLength\": 1,\"cancellationPeriodLimit\": null," +
                "\"endOfContractGracePeriod\": 30,\"blockSwitchToShorterContract\": true,\"blockContractDowngrades\": true,\"blockContractUpgrades\": false,\"alignWithParentCycleStartDate\": false," +
                "\"gracePeriod\": null,\"terminationFee\": null,\"autoExtensionPricingId\": 10527},\"allowCustomUsage\": true,\"keepBillDateOnUsageChange\": true,\"separatePrepaid\": false,\"isPrimaryPrice\": false," +
                "\"costs\": [],\"discount\": null,\"primaryPrice\": false}}},\"previousOrder\": null,\"nextOrder\": null,\"discount\": null,\"paymentPlanId\": 10527,\"discountId\": null,\"activated\": false," +
                "\"oneTimeOrders\": [{\"startDate\": 1520913600000,\"endDate\": 1523592000000,\"serviceStartDate\": \"2018-03-13\",\"nextBillingDate\": 1523592000000,\"endOfDiscountDate\": null,\"status\": \"ONE_TIME\",\"frequency\": \"MONTHLY\"," +
                "\"currency\": \"EUR\",\"type\": \"METERED_USAGE\",\"totalPrice\": 0,\"user\": null,\"salesSupportUser\": null,\"salesSupportCompany\": null,\"company\": null,\"referenceCode\": null,\"paymentPlan\": {\"id\": \"10527\",\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/marketplace/v1/products/3851/editions/9130/paymentPlans/10527\"}," +
                "\"links\": [],\"id\": 4394}],\"orderLines\": [{\"id\": 9869,\"editionPricingItemId\": 18935,\"type\": \"ITEM\",\"unit\": \"NOT_APPLICABLE\",\"quantity\": 1,\"price\": 0,\"listingPrice\": 0,\"totalPrice\": 0,\"applicationName\": \"Open Telekom Cloud\"," +
                "\"editionName\": \"Nutzungsabhängige Abrechnung (Pay as you go)\",\"description\": \"Open Telekom Cloud - Nutzungsabhängige Abrechnung (Pay as you go) - Monthly Fee\"},{\"id\": 9874,\"type\": \"TAX\",\"quantity\": 1," +
                "\"percentage\": 19,\"totalPrice\": 0,\"applicationName\": \"Open Telekom Cloud\",\"description\": \"Sales Tax\"}],\"parameters\": [],\"customAttributes\": [{\"name\": \"user_status\",\"attributeType\": \"TEXT\",\"value\": \"visited\"" +
                "}],\"links\": [{\"rel\": \"subscription\",\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/billing/v1/subscriptions/5548c36d-80a8-42c1-be81-0f3179691e15\"}],\"id\": 4395}," +
                "\"upcomingOrder\": null,\"user\": {\"id\": \"bda34090-7327-4be3-8f81-03c18a3d6bac\",\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/account/v1/users/bda34090-7327-4be3-8f81-03c18a3d6bac\"},\"company\": {\"id\": \"6d07f0e5-13c5-4ac8-b481-47ebf9c0b07e\"," +
                "\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/account/v1/companies/6d07f0e5-13c5-4ac8-b481-47ebf9c0b07e\"\n" +
                "},\"product\": {\"id\": \"3851\",\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/marketplace/v1/products/3851\"},\"edition\": {\"id\": \"9130\",\"href\": \"https://od-f39er4umatelekom.od2.appdirectondemand.com/api/marketplace/v1/products/3851/editions/9130\"},\"redirectUrl\": null," +
                "\"internalId\": \"5548c36d-80a8-42c1-be81-0f3179691e15\"}},\"resourceAction\": \"ADDED\"}";

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(payloadStr, PayloadWS.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    public static MediationConfigurationWS buildMediationConfiguration() {
        MediationConfigurationWS conf = new MediationConfigurationWS();
        conf.setEntityId(TEST_ENTITY_ID);
        conf.setGlobal(Boolean.FALSE);
        conf.setName("RestTest");
        conf.setOrderValue("123");
        conf.setMediationJobLauncher("sampleMediationJob");
        conf.setLocalInputDirectory("doesnotexist");
        conf.setCreateDatetime(new Date());
        return conf;
    }

    public static AgeingWS[] buildAgeingStepsWithNoDays() {
        AgeingWS[] ageingSteps = new AgeingWS[4];
        ageingSteps[0] = buildAgeingStep("Payment Due", null, false, true, false);
        ageingSteps[1] = buildAgeingStep("Grace Period", null, false, true, false);
        ageingSteps[2] = buildAgeingStep("First Retry", null, true, false, false);
        ageingSteps[3] = buildAgeingStep("Suspended", null, false, false, true);
        return ageingSteps;
    }

    public static AgeingWS[] buildAgeingStepsWithNoStepName() {
        AgeingWS[] ageingSteps = new AgeingWS[4];
        ageingSteps[0] = buildAgeingStep(null, Integer.valueOf(0), false, false, false);
        ageingSteps[1] = buildAgeingStep("Grace Period", Integer.valueOf(2), false, true, false);
        ageingSteps[2] = buildAgeingStep("First Retry", Integer.valueOf(3), true, false, false);
        ageingSteps[3] = buildAgeingStep("Suspended", Integer.valueOf(7), false, false, true);
        return ageingSteps;
    }

    public static CurrencyWS buildCurrencyMock(String description, String symbol, String code, String countryCode,
            boolean inUse, String rate, String sysrate) {
        CurrencyWS newCurrency = new CurrencyWS();
        newCurrency.setDescription(description);
        newCurrency.setSymbol(symbol);
        newCurrency.setCode(code);
        newCurrency.setCountryCode(countryCode);
        newCurrency.setInUse(inUse);
        newCurrency.setRate(rate);
        newCurrency.setSysRate(sysrate);
        newCurrency.setFromDate(null);
        newCurrency.setDefaultCurrency(false);
        newCurrency.setRateAsDecimal(BigDecimal.valueOf(1.5));
        newCurrency.setSysRateAsDecimal(BigDecimal.valueOf(1.288));
        return newCurrency;
    }
    
    public static void setContactDetails(Integer aitContactId, UserWS userWS, String dummyEmailAddress){
    	setAITMetaFields(aitContactId, userWS, new Date(), new String[]{"contact.email"
			, "contact.address1"
			, "contact.address2"
			, "contact.city"
			, "contact.state.province"
			, "contact.postal.code"
			, "contact.country.code"
			, "contact.first.name"
			, "contact.last.name" }
    	, new String[]{dummyEmailAddress
			, "777"
			, "Brockton Avenue"
			, "Abington"
			, "MA"
			, "2351"
			, "US"
			, "Stripe first"
			, "Stripe last" 
    	});
    }
    
    private static void setAITMetaFields(Integer aitId, UserWS user, Date date, String metaFieldName[], String metaFieldValue[]){
        Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> aitMetaFields = new HashMap<>();

        HashMap<Date, ArrayList<MetaFieldValueWS>> timeLineMetaFields = new HashMap<>();
        ArrayList<MetaFieldValueWS> metaFieldValues = new ArrayList<>();

        for(int i=0; i < metaFieldValue.length; i++){
        	MetaFieldValueWS metaFieldValueWS = RestEntitiesHelper.buildMetaFieldValue(metaFieldName[i], metaFieldValue[i], aitId);
            metaFieldValues.add(metaFieldValueWS);
        }

        user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[]{}));
        timeLineMetaFields.put(date, metaFieldValues);
        aitMetaFields.put(aitId, timeLineMetaFields);
        user.setAccountInfoTypeFieldsMap(aitMetaFields);
        ArrayList<Date> dates = new ArrayList<>();
        dates.add(date);
        Map<Integer, ArrayList<Date>> datesMap = new HashMap<>();
        datesMap.put(aitId, dates);
        user.setTimelineDatesMap(datesMap);
        Map<Integer, Date> effectiveDateMap = new HashMap<>();
        effectiveDateMap.put(aitId, date);
        user.setEffectiveDateMap(effectiveDateMap);
    }
    
    public static PaymentInformationRestWS buildPaymentInformationWSMock(Integer userId, Integer paymentMethodTypeId, String ccNumber, String expiryDate, String intendId){
    	PaymentInformationRestWS instrument =  new PaymentInformationRestWS();
    	
        instrument.setUserId(userId);
        instrument.setProcessingOrder(1);
        instrument.setPaymentMethodId(2);
        instrument.setPaymentMethodTypeId(paymentMethodTypeId);
        
        Map<String, Object> metaFields = new HashMap<String, Object>();
        
        if(intendId==null){
	        metaFields.put("cc.cardholder.name", "test payment intrument");
	        metaFields.put("cc.number", ccNumber);
	        metaFields.put("cc.expiry.date", expiryDate);
        }
        if(intendId!=null){
        	metaFields.put("cc.stripe.intent.id", intendId);
        }
    	
        instrument.setMetaFields(metaFields);
    	return instrument;
    }
}
