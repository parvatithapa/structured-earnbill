package com.sapienter.jbilling.server.distributel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.server.item.AssetStatusDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.tasks.PaymentPaySafeTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.RouteWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

/**
 * Created by developer on 27/04/17.
 */
public class DistributelTestUtil {

    private static final Logger logger = LoggerFactory.getLogger(DistributelTestUtil.class);
    static AccountTypeWS accountType = null;
    static Integer contactInformationMetaFieldGroupId;
    static Integer emergencyAddressMetaFieldGroupId;
    static String UPDATE_DISTRIBUTEL_CUSTOMER = "com.sapienter.jbilling.server.customer.task.UpdateDistributelCustomerTask";
    private static Integer PRANCING_PONY_ENTITY_ID = 1;
    private static final String ASSET_STATE_IN_USE = "In Use";
    private static final String ASSET_STATE_AVAILABLE = "Available";
    private static final String ASSET_STATE_PENDING= "Pending";
    public static Integer ACCOUNT_TYPE_ID;

    static void buildAccountType(JbillingAPI api) throws IOException, JbillingAPIException {

        if (accountType == null) {
            List<MetaFieldWS> metaFieldList = buildSpaMetaField();

            logger.debug("buildAccountType");

            for (AccountTypeWS accountTypeWS : api.getAllAccountTypes()) {
                if (accountTypeWS.getDescription(api.getCallerLanguageId()).getContent().equals(SpaConstants.ACCOUNT_TYPE_RESIDENTIAL)) {
                    accountType = accountTypeWS;
                }
            }

            if (accountType == null) {
                accountType = new AccountTypeBuilder().addDescription(SpaConstants.ACCOUNT_TYPE_RESIDENTIAL, api.getCallerLanguageId()).build();
                accountType.setId(api.createAccountType(accountType));

            }
            ACCOUNT_TYPE_ID = accountType.getId();
            MetaFieldWS[] entityMetaFields = api.getMetaFieldsForEntity(EntityType.ACCOUNT_TYPE.name());

            logger.debug("entity meta fields = {}", entityMetaFields.length);

            for (MetaFieldWS metaFieldWS : metaFieldList) {
                boolean metaFieldFound = false;
                for (MetaFieldWS entitiyMetaFieldWS : entityMetaFields) {
                    if (metaFieldWS.getName().equals(entitiyMetaFieldWS.getName())) {
                        metaFieldWS.setId(entitiyMetaFieldWS.getId());
                        metaFieldFound = true;
                        break;
                    }
                }

                if (!metaFieldFound) {
                    metaFieldWS.setId(api.createMetaField(metaFieldWS));
                }
            }

            AccountInformationTypeWS[] accountInformationTypeWSes = api.getInformationTypesForAccountType(accountType.getId());

            for (AccountInformationTypeWS accountInformationTypeWS : accountInformationTypeWSes) {

                if (accountInformationTypeWS.getName().equals("Contact Information")) {
                    contactInformationMetaFieldGroupId = accountInformationTypeWS.getId();
                }

                if (accountInformationTypeWS.getName().equals("Emergency 911 Address")) {
                    emergencyAddressMetaFieldGroupId = accountInformationTypeWS.getId();
                }
            }

            List<MetaFieldWS> aitMetaFields = buildSpaMetaField().stream().filter(it -> it.getEntityType().equals(EntityType.ACCOUNT_TYPE)).collect(Collectors.toList());
            List<MetaFieldWS> groupMetaFields = metaFieldList.stream().filter(it -> it.getEntityType().equals(EntityType.ACCOUNT_TYPE)).collect(Collectors.toList());

            if (contactInformationMetaFieldGroupId == null) {
                MetaFieldGroupWS metaFieldGroup = new MetaFieldGroupWS();
                metaFieldGroup.setName("Contact Information");
                metaFieldGroup.setEntityType(EntityType.ACCOUNT_TYPE);
                metaFieldGroup.setMetaFields(groupMetaFields.toArray(new MetaFieldWS[groupMetaFields.size()]));
                metaFieldGroup.setId(api.createMetaFieldGroup(metaFieldGroup));

                AccountInformationTypeWS accountInformationType = new AccountInformationTypeBuilder(accountType).build();
                accountInformationType.setName("Contact Information");
                accountInformationType.setEntityId(PRANCING_PONY_ENTITY_ID);
                accountInformationType.setEntityType(EntityType.ACCOUNT_TYPE);

                accountInformationType.setMetaFields(aitMetaFields.toArray(new MetaFieldWS[aitMetaFields.size()]));
                contactInformationMetaFieldGroupId = api.createAccountInformationType(accountInformationType);

                logger.debug("Contact Information group created, id = {}", contactInformationMetaFieldGroupId);
            }

            if (emergencyAddressMetaFieldGroupId == null) {
                MetaFieldGroupWS metaFieldGroupEmergencyAddress = new MetaFieldGroupWS();
                metaFieldGroupEmergencyAddress.setName("Emergency 911 Address");
                metaFieldGroupEmergencyAddress.setEntityType(EntityType.ACCOUNT_TYPE);
                metaFieldGroupEmergencyAddress.setMetaFields(groupMetaFields.toArray(new MetaFieldWS[groupMetaFields.size()]));
                metaFieldGroupEmergencyAddress.setId(api.createMetaFieldGroup(metaFieldGroupEmergencyAddress));
                //emergencyAddressMetaFieldGroupId = metaFieldGroupEmergencyAddress.getId();

                AccountInformationTypeWS accountInformationTypeEmergencyAddress = new AccountInformationTypeBuilder(accountType).build();
                accountInformationTypeEmergencyAddress.setName("Emergency 911 Address");
                accountInformationTypeEmergencyAddress.setEntityId(PRANCING_PONY_ENTITY_ID);
                accountInformationTypeEmergencyAddress.setEntityType(EntityType.ACCOUNT_TYPE);
                //                accountInformationTypeEmergencyAddress.setMetaFields(metaFieldList.toArray(new MetaFieldWS[metaFieldList.size()]));

                accountInformationTypeEmergencyAddress.setMetaFields(aitMetaFields.toArray(new MetaFieldWS[aitMetaFields.size()]));
                emergencyAddressMetaFieldGroupId = api.createAccountInformationType(accountInformationTypeEmergencyAddress);
                //emergencyAddressMetaFieldGroupId = accountInformationTypeEmergencyAddress.getId();

                logger.debug("Emergency address 911 group created, id = {}", emergencyAddressMetaFieldGroupId);
            }
        }
    }

    public static Integer enablePlugin(String pluginName, JbillingAPI api) {
        PluggableTaskWS pluggableTask = new PluggableTaskWS();
        PluggableTaskTypeWS pluggableTaskTypeWS = api.getPluginTypeWSByClassName(pluginName);
        pluggableTask.setTypeId(pluggableTaskTypeWS.getId());
        pluggableTask.setProcessingOrder(84);

        Integer pluggableTaskId = api.createPlugin(pluggableTask);

        logger.debug(pluginName + " Id = " + pluggableTaskId);

        return pluggableTaskId;
    }

    public static void disablePlugin(Integer pluggableTaskId, JbillingAPI api) {
        api.deletePlugin(pluggableTaskId);
        logger.debug("Deleted plugin whose Id = " + pluggableTaskId);
    }

    private static List<MetaFieldWS> buildSpaMetaField(){
        String[] metaFieldsAccountType = new String[]{
                SpaConstants.CUSTOMER_NAME,
                SpaConstants.CUSTOMER_COMPANY,
                SpaConstants.POSTAL_CODE,
                SpaConstants.STREET_NUMBER,
                SpaConstants.STREET_NUMBER_SUFFIX,
                SpaConstants.STREET_NAME,
                SpaConstants.STREET_TYPE,
                SpaConstants.STREET_APT_SUITE,
                SpaConstants.STREET_DIRECTION,
                SpaConstants.CITY,
                SpaConstants.PROVINCE,
                SpaConstants.PHONE_NUMBER_1,
                SpaConstants.PHONE_NUMBER_2,
                SpaConstants.EMAIL_ADDRESS,
                SpaConstants.EMAIL_VERIFIED,
                SpaConstants.SAME_AS_CUSTOMER_INFORMATION,
                SpaConstants.CONFIRMATION_NUMBER,
                SpaConstants.MF_PROVIDED,
                SpaConstants.MF_REQUIRED

        };

        String[] metaFieldsCustomer = new String[]{
                SpaConstants.SPA_ENROLLMENT_INCOMPLETE,
                SpaConstants.SPA_ENROLLMENT_NOTES,
                SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE,
                SpaConstants.NORTHERN_911_ERROR_CODE
        };
        List<MetaFieldWS> metaFieldList  = new ArrayList();
        MetaFieldWS metaField = null;
        for(String name : metaFieldsAccountType){
            metaField = new MetaFieldBuilder().build();
            metaField.setName(name);

            if(SpaConstants.EMAIL_VERIFIED.equals(name)){
                metaField.setDataType(DataType.DATE);
            }else if(SpaConstants.SAME_AS_CUSTOMER_INFORMATION.equals(name) ||
                    SpaConstants.MF_PROVIDED.equals(name) || SpaConstants.MF_REQUIRED.equals(name)){
                metaField.setDataType(DataType.BOOLEAN);
                //            } else if(SpaConstants.STREET_NUMBER.equals(name)){
                //                metaField.setDataType(DataType.INTEGER);
            } else{
                metaField.setDataType(DataType.STRING);
            }
            metaField.setEntityType(EntityType.ACCOUNT_TYPE);
            metaField.setPrimary(true);
            metaFieldList.add(metaField);
        }
        for(String name : metaFieldsCustomer){
            metaField = new MetaFieldBuilder().build();
            metaField.setName(name);
            if(SpaConstants.SPA_ENROLLMENT_INCOMPLETE.equals(name) || SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE.equals(name) ||
                    SpaConstants.MF_PROVIDED.equals(name) || SpaConstants.MF_REQUIRED.equals(name)){
                metaField.setDataType(DataType.BOOLEAN);
            }else{
                metaField.setDataType(DataType.STRING);
            }
            metaField.setEntityType(EntityType.CUSTOMER);
            metaField.setPrimary(true);
            metaFieldList.add(metaField);
            logger.debug("meta field name = {} | type = {}", metaField.getName(), metaField.getDataType());
        }
        return metaFieldList;
    }

    public static Integer createRoute(String fileName, String tableName, String csvString) throws  JbillingAPIException, IOException {
        File temporalFile = File.createTempFile(fileName, ".csv");
        RouteWS routeWS = new RouteWS();
        routeWS.setName(tableName);
        routeWS.setRootTable(false);
        routeWS.setRouteTable(false);
        routeWS.setOutputFieldName("");
        routeWS.setDefaultRoute("");
        writeToFile(temporalFile, csvString);
        Integer id= null;
        id = JbillingAPIFactory.getAPI().createRoute(routeWS, temporalFile);
        temporalFile.delete();
        return id;
    }


    private static void writeToFile(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();
    }


    public static void addAssetStatus(ItemTypeWS itemTypeWS, String statusDescription) throws  JbillingAPIException, IOException{
        boolean existStatus = itemTypeWS.getAssetStatuses().stream().anyMatch(status -> status.getDescription().equals(statusDescription));
        if(!existStatus){
            AssetStatusDTOEx assetStatus = new AssetStatusDTOEx();
            assetStatus.setId(0);
            assetStatus.setDescription(statusDescription);
            assetStatus.setIsInternal(0);
            itemTypeWS.getAssetStatuses().add(assetStatus);
            JbillingAPIFactory.getAPI().updateItemCategory(itemTypeWS);
        }
    }

    public static Integer createMetaField(String name, EntityType type, DataType dataType, Integer displayOrder) throws  JbillingAPIException, IOException {
        MetaFieldWS[] metaFields = JbillingAPIFactory.getAPI().getMetaFieldsForEntity(type.name());
        for(MetaFieldWS mf : metaFields){
            if(mf.getName().equals(name)){
                return mf.getId();
            }
        }
        MetaFieldWS metaFieldWS = new MetaFieldWS();
        metaFieldWS.setName(name);
        metaFieldWS.setEntityType(type);
        metaFieldWS.setDataType(dataType);
        metaFieldWS.setDisabled(false);
        metaFieldWS.setDisplayOrder(displayOrder);
        metaFieldWS.setMandatory(false);
        metaFieldWS.setPrimary(true);
        metaFieldWS.setHelpContentURL("");
        metaFieldWS.setHelpDescription("");
        return JbillingAPIFactory.getAPI().createMetaField(metaFieldWS);
    }

    public static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
            String fieldName) {
        for (MetaFieldValueWS ws : metaFields) {
            if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
                return ws;
            }
        }
        return null;
    }


    public static MetaFieldWS getMetafield(String name,DataType dataType,EntityType entityType) {
        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setName(name);
        metafieldWS.setDataType(dataType);
        metafieldWS.setMandatory(false);
        metafieldWS.setDisabled(false);
        metafieldWS.setDisplayOrder(5);
        metafieldWS.setEntityType(EntityType.ORDER);
        metafieldWS.setPrimary(true);
        return metafieldWS;
    }

    public static void addMetaField(List<MetaFieldValueWS> metaFields,
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

    public static MetaFieldValueWS createMetaFieldValue(String metafieldName, DataType dataType, String value, Integer entityId) {
        MetaFieldValueWS provinceMFValueWS = new MetaFieldValueWS();
        provinceMFValueWS.setFieldName(metafieldName);
        provinceMFValueWS.getMetaField().setDataType(dataType);
        provinceMFValueWS.setValue(value);
        provinceMFValueWS.getMetaField().setEntityId(entityId);
        return provinceMFValueWS;
    }

    public static Integer createPlanMetaField(String name, EntityType type, DataType dataType, Integer displayOrder) throws  JbillingAPIException, IOException{
        MetaFieldWS metaFieldWS = new MetaFieldWS();
        metaFieldWS.setName(name);
        metaFieldWS.setEntityType(type);
        metaFieldWS.setDataType(dataType);
        metaFieldWS.setDisabled(false);
        metaFieldWS.setDisplayOrder(displayOrder);
        metaFieldWS.setMandatory(false);
        metaFieldWS.setPrimary(true);
        metaFieldWS.setHelpContentURL("");
        metaFieldWS.setHelpDescription("");

        return JbillingAPIFactory.getAPI().createMetaField(metaFieldWS);
    }

    public static Integer createItemCategory(Integer orderLineTypeId) throws  JbillingAPIException, IOException{
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("category" + Short.toString((short) System.currentTimeMillis()));
        itemType.setOrderLineTypeId(orderLineTypeId);
        itemType.setAllowAssetManagement(1);
        itemType.setAssetStatuses(createAssetStatusForCategory());
        return JbillingAPIFactory.getAPI().createItemCategory(itemType);
    }

    private static Set<AssetStatusDTOEx> createAssetStatusForCategory() {
        Set<AssetStatusDTOEx> assetStatuses = new HashSet<AssetStatusDTOEx>();
        AssetStatusDTOEx addToOrderStatus = new AssetStatusDTOEx();
        addToOrderStatus.setDescription("AddToOrderStatus");
        addToOrderStatus.setIsAvailable(0);
        addToOrderStatus.setIsDefault(0);
        addToOrderStatus.setIsInternal(0);
        addToOrderStatus.setIsOrderSaved(1);
        addToOrderStatus.setIsActive(1);
        addToOrderStatus.setIsPending(0);
        assetStatuses.add(addToOrderStatus);

        AssetStatusDTOEx available = new AssetStatusDTOEx();
        available.setDescription("Available");
        available.setIsAvailable(1);
        available.setIsDefault(1);
        available.setIsInternal(0);
        available.setIsOrderSaved(0);
        available.setIsActive(0);
        available.setIsPending(0);
        assetStatuses.add(available);

        AssetStatusDTOEx notAvailable = new AssetStatusDTOEx();
        notAvailable.setDescription("NotAvailable");
        notAvailable.setIsAvailable(0);
        notAvailable.setIsDefault(0);
        notAvailable.setIsInternal(0);
        notAvailable.setIsOrderSaved(0);
        notAvailable.setIsActive(0);
        notAvailable.setIsPending(0);
        assetStatuses.add(notAvailable);

        AssetStatusDTOEx pending = new AssetStatusDTOEx();
        pending.setDescription("Pending");
        pending.setIsAvailable(0);
        pending.setIsDefault(0);
        pending.setIsInternal(0);
        pending.setIsOrderSaved(1);
        pending.setIsActive(0);
        pending.setIsPending(1);
        assetStatuses.add(pending);

        return assetStatuses;
    }

    public static PlanItemWS createPlanItem(Integer itemId,
            BigDecimal quantity, Integer periodId, PriceModelWS priceModel) {
        PlanItemWS planItemWS = new PlanItemWS();
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setPeriodId(periodId);
        bundle.setQuantity(quantity);
        planItemWS.setItemId(itemId);
        planItemWS.setBundle(bundle);
        planItemWS.addModel(CommonConstants.EPOCH_DATE,priceModel);
        return planItemWS;
    }

    public static Integer createItemCategory(String description, Integer orderLineTypeId, boolean allowAsset, JbillingAPI api) {
        ItemTypeWS[] itemTypeWS = api.getAllItemCategories();
        for(ItemTypeWS item : itemTypeWS){
            if(item.getDescription().equals(description)){
                return item.getId();
            }
        }
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription(description);
        itemType.setOrderLineTypeId(orderLineTypeId);
        itemType.setAllowAssetManagement(allowAsset?1:0);

        if(allowAsset){
            HashSet<AssetStatusDTOEx> assetStatus = new HashSet();
            assetStatus.add(new AssetStatusDTOEx(0,ASSET_STATE_IN_USE,0,0,1,0, 1, 0, 0));
            assetStatus.add(new AssetStatusDTOEx(0,ASSET_STATE_AVAILABLE,1,1,0,0, 0,0, 0));
            assetStatus.add(new AssetStatusDTOEx(0,ASSET_STATE_PENDING,0,0,1,0,0,1, 0));
            itemType.setAssetStatuses(assetStatus);

            HashSet<MetaFieldWS> mfs = new HashSet();

            MetaFieldWS mfMacAddress = new MetaFieldBuilder().build();
            mfMacAddress.setEntityId(PRANCING_PONY_ENTITY_ID);
            mfMacAddress.setDataType(DataType.STRING);
            mfMacAddress.setEntityType(EntityType.ASSET);
            mfMacAddress.setDisplayOrder(1);
            mfMacAddress.setName(SpaConstants.MF_MAC_ADRESS);
            mfs.add(mfMacAddress);

            MetaFieldWS mfModel = new MetaFieldBuilder().build();
            mfModel.setEntityId(PRANCING_PONY_ENTITY_ID);
            mfModel.setDataType(DataType.STRING);
            mfModel.setEntityType(EntityType.ASSET);
            mfModel.setDisplayOrder(1);
            mfModel.setName(SpaConstants.MF_MODEL);
            mfs.add(mfModel);

            MetaFieldWS mfBanffAccountId = new MetaFieldBuilder().build();
            mfBanffAccountId.setEntityId(PRANCING_PONY_ENTITY_ID);
            mfBanffAccountId.setDataType(DataType.STRING);
            mfBanffAccountId.setEntityType(EntityType.ASSET);
            mfBanffAccountId.setDisplayOrder(1);
            mfBanffAccountId.setName(SpaConstants.DOMAIN_ID);
            mfs.add(mfBanffAccountId);

            MetaFieldWS mfSerialNumber = new MetaFieldBuilder().build();
            mfSerialNumber.setEntityId(PRANCING_PONY_ENTITY_ID);
            mfSerialNumber.setDataType(DataType.STRING);
            mfSerialNumber.setEntityType(EntityType.ASSET);
            mfSerialNumber.setDisplayOrder(1);
            mfSerialNumber.setName(SpaConstants.MF_SERIAL_NUMBER);
            mfs.add(mfSerialNumber);

            MetaFieldWS mfTrackingNumber = new MetaFieldBuilder().build();
            mfTrackingNumber.setEntityId(PRANCING_PONY_ENTITY_ID);
            mfTrackingNumber.setDataType(DataType.STRING);
            mfTrackingNumber.setEntityType(EntityType.ASSET);
            mfTrackingNumber.setDisplayOrder(1);
            mfTrackingNumber.setName(SpaConstants.TRACKING_NUMBER);
            mfs.add(mfTrackingNumber);

            MetaFieldWS mfCarrier = new MetaFieldBuilder().build();
            mfCarrier.setEntityId(PRANCING_PONY_ENTITY_ID);
            mfCarrier.setDataType(DataType.STRING);
            mfCarrier.setEntityType(EntityType.ASSET);
            mfCarrier.setDisplayOrder(1);
            mfCarrier.setName(SpaConstants.COURIER);
            mfs.add(mfCarrier);

            MetaFieldWS mfPhoneNumber = new MetaFieldBuilder().build();
            mfPhoneNumber.setEntityId(PRANCING_PONY_ENTITY_ID);
            mfPhoneNumber.setDataType(DataType.STRING);
            mfPhoneNumber.setEntityType(EntityType.ASSET);
            mfPhoneNumber.setDisplayOrder(1);
            mfPhoneNumber.setName(SpaConstants.MF_PHONE_NUMBER);
            mfs.add(mfPhoneNumber);

            itemType.setAssetMetaFields(mfs);
        }
        return api.createItemCategory(itemType);
    }

    public static void initMetafieldCreation() throws Exception{

        createMetaField(SpaConstants.INSTALLATION_TIME, EntityType.ORDER, DataType.STRING, 1);
        createMetaField(SpaConstants.DETAIL_FILE_NAMES, EntityType.ORDER, DataType.STRING, 2);
        createMetaField(SpaConstants.MF_ENROLLMENT_TYPE, EntityType.ORDER, DataType.STRING, 3);
        createMetaField(SpaConstants.MF_STAFF_IDENTIFIER, EntityType.ORDER, DataType.STRING, 4);

        createMetaField(SpaConstants.MF_MAC_ADRESS, EntityType.ASSET, DataType.STRING, 2);
        createMetaField(SpaConstants.MF_MODEL, EntityType.ASSET, DataType.STRING, 3);
        createMetaField(SpaConstants.DOMAIN_ID, EntityType.ASSET, DataType.STRING, 4);
        createMetaField(SpaConstants.MF_SERIAL_NUMBER, EntityType.ASSET, DataType.STRING, 5);
        createMetaField(SpaConstants.COURIER, EntityType.ASSET, DataType.STRING, 6);
        createMetaField(SpaConstants.TRACKING_NUMBER, EntityType.ASSET, DataType.STRING, 7);
        createMetaField(SpaConstants.MF_PHONE_NUMBER, EntityType.ASSET, DataType.STRING, 8);
        createMetaField(SpaConstants.COURIER, EntityType.ASSET, DataType.STRING, 9);
    }

    public static void updatePaymentMethodTypeCreditCard(JbillingAPI api) {
        PaymentMethodTypeWS paymentMethodTypeCreditCard = api.getPaymentMethodType(1); // PMT Credit Card in Entity 1
        boolean paysafeMFfound = false;
        for (MetaFieldWS mf : paymentMethodTypeCreditCard.getMetaFields()) {
            if (PaymentPaySafeTask.PAYMENT_STATUS.equals(mf.getName())) {
                paysafeMFfound = true;
            }
        }
        if (!paysafeMFfound) {
            List<MetaFieldWS> paymentMetaFieldWSList = new ArrayList<>(Arrays.asList(paymentMethodTypeCreditCard.getMetaFields()));
            paymentMetaFieldWSList.add(createMetaFieldWS(DataType.STRING, 7, PaymentPaySafeTask.PAYMENT_STATUS, EntityType.PAYMENT_METHOD_TYPE, 1));
            paymentMetaFieldWSList.add(createMetaFieldWS(DataType.INTEGER, 8, PaymentPaySafeTask.PAYMENT_ATTEMPT_COUNT, EntityType.PAYMENT_METHOD_TYPE, 1));
            paymentMetaFieldWSList.add(createMetaFieldWS(DataType.STRING, 9, PaymentPaySafeTask.PAYSAFE_PROFILE_ID, EntityType.PAYMENT_METHOD_TYPE, 1));
            paymentMethodTypeCreditCard.setMetaFields(paymentMetaFieldWSList.toArray(new MetaFieldWS[paymentMetaFieldWSList.size()]));
            api.updatePaymentMethodType(paymentMethodTypeCreditCard);
        }
    }

    public static MetaFieldWS createMetaFieldWS(DataType dataType, Integer displayOrder, String name, EntityType entityType, Integer entityId) {
        MetaFieldWS mf = new MetaFieldWS();
        mf.setEntityId(entityId);
        mf.setDataType(dataType);
        mf.setDisplayOrder(displayOrder);
        mf.setName(name);
        mf.setEntityType(entityType);
        return mf;
    }

    public static Date getDate(int month, int day, int year) {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH,day);
        cal.set(Calendar.YEAR, year);

        return cal.getTime();
    }

}
