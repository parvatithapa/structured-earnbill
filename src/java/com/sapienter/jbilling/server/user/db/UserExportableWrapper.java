package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.adennet.dto.UserInfoRowMapper;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.audit.db.EventLogDAS;
import com.sapienter.jbilling.server.util.audit.db.EventLogDTO;
import com.sapienter.jbilling.server.util.csv.DynamicExport;
import com.sapienter.jbilling.server.util.csv.ExportableWrapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Krunal Bhavsar
 */
public class UserExportableWrapper implements ExportableWrapper<UserDTO> {

    private static final Map<Integer, Map<String, Map<String, String>>> META_FIELD_MAP = new ConcurrentHashMap<>();
    private static final Map<Integer, List<String>> CUSTOMER_META_FIELD_MAP = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<Integer, String>> ACCOUNT_INFORMATION_TYPE_ID_AND_NAME_MAP = new ConcurrentHashMap<>();
    private static final String[] FIELDS = new String[]{
            "userId",
            "ICCID",
            "subscriberNumber",
            "assetStatus",

            // customer
            "walletBalance",
            "planDescription",
            "planStatus",
            "customerType",
            "identificationType",
            "identificationNumber",
            "governorate",

            // contact
            "firstName",
            "lastName",
            "address1",
            "address2",
            "city",
            "stateProvince",
            "postalCode",
            "countryCode",
            "phoneNumber",
            "faxNumber",
            "email",
            "createdBy",
            "createdDate",
            "updatedBy",
            "updatedDate",
            "language",
            "currency",
            "accountType",
            "customerStatus",
            "lastStatusChange"
    };


    private Integer userId;
    private CustomerDTO customer;
    private Integer accountTypeId;
    private Integer entityId;
    private DynamicExport dynamicExport = DynamicExport.NO;
    private CustomerDAS customerDAS;
    private ResourceBundle bundle;
    private BigDecimal walletBalance = BigDecimal.ZERO;
    private String planDescription = "";
    private String planStatus = "";
    private String subscriberNumber = "";
    private UserInfoRowMapper userInfoRowMapper;

    private void init(Integer userId) {
        this.userId = userId;
        customerDAS = new CustomerDAS();
        accountTypeId = customerDAS.getCustomerAccountTypeId(customerDAS.getCustomerId(userId));
        entityId = new UserDAS().getEntityByUserId(userId);
    }

    public UserExportableWrapper(Integer userId) {
        init(userId);
    }

    public UserExportableWrapper(Integer userId, DynamicExport dynamicExport, UserInfoRowMapper userInfoRowMapper) {
        init(userId);
        setDynamicExport(dynamicExport);
        this.userInfoRowMapper = userInfoRowMapper;
    }

    private class CustomerAITMetaFieldInfo {
        private String aitAndmetaFieldName;
        private String metaFieldValue;
    }

    private List<CustomerAITMetaFieldInfo> aitMetaFields = null;

    private List<CustomerAITMetaFieldInfo> getCustomerAITMetaFieldInfo() {

        List<CustomerAITMetaFieldInfo> aitMetaFields = new ArrayList<>();
        Map<String, Map<String, String>> allAITMetaFields = getPopulatedAITMetaFieldsMap();
        for (Map<String, String> aitMetaFieldInfoMap : allAITMetaFields.values()) {
            for (Entry<String, String> aitMetaFiedlInfoEntry : aitMetaFieldInfoMap.entrySet()) {
                CustomerAITMetaFieldInfo customerAITMetaFieldInfo = new CustomerAITMetaFieldInfo();

                customerAITMetaFieldInfo.aitAndmetaFieldName = aitMetaFiedlInfoEntry.getKey();
                String metaFieldValue = aitMetaFiedlInfoEntry.getValue();
                if (null != metaFieldValue && !metaFieldValue.isEmpty()) {
                    customerAITMetaFieldInfo.metaFieldValue = metaFieldValue;
                }
                aitMetaFields.add(customerAITMetaFieldInfo);
            }
        }

        Collections.sort(aitMetaFields, (o1, o2) -> (o1.aitAndmetaFieldName).compareTo((o2.aitAndmetaFieldName)));
        return aitMetaFields;
    }

    @Override
    public String[] getFieldNames() {
        UserDTO user = getWrappedInstance();
        if (user != null) {
            this.customer = user.getCustomer();
            if (customer != null && dynamicExport.equals(DynamicExport.YES)) {
                return getCustomerAndMetaFieldsFieldNames();
            }
        }
        return FIELDS;
    }

    public String[] getCustomerAndMetaFieldsFieldNames() {
        //clearing cache.
        META_FIELD_MAP.clear();
        ACCOUNT_INFORMATION_TYPE_ID_AND_NAME_MAP.clear();
        CUSTOMER_META_FIELD_MAP.clear();

        List<String> fieldsList = new ArrayList<>();

        fieldsList.add("userId");
        fieldsList.add("ICCID");
        fieldsList.add("subscriberNumber");
        fieldsList.add("assetStatus");

        // customer
        fieldsList.add("WalletBalance");
        fieldsList.add("planDescription");
        fieldsList.add("planStatus");
        fieldsList.add("identificationType");
        fieldsList.add("identificationNumber");
        fieldsList.add("createdBy");
        fieldsList.add("createdDate");
        fieldsList.add("updatedBy");
        fieldsList.add("updatedDate");
        fieldsList.add("language");
        fieldsList.add("currency");
        fieldsList.add("accountType");
        fieldsList.add("customerStatus");
        fieldsList.add("lastStatusChange");

        if (null == this.aitMetaFields) {
            this.aitMetaFields = getCustomerAITMetaFieldInfo();
        }

        fieldsList.addAll(aitMetaFields.stream()
                .map(customerAITMetaFieldInfo -> customerAITMetaFieldInfo.aitAndmetaFieldName)
                .collect(Collectors.toList()));

        fieldsList.addAll(getCustomerMetaFieldNames());
        return fieldsList.toArray(new String[fieldsList.size()]);
    }

    @Override
    public Object[][] getFieldValues() {
        UserDTO user = getWrappedInstance();
        this.customer = user.getCustomer();
        if (dynamicExport.equals(DynamicExport.YES)) {
            return getCustomerAndMetaFieldsWithValues();
        }

        return getCustomerFieldValues();
    }

    public Object[][] getCustomerAndMetaFieldsWithValues() {
        if (null == this.aitMetaFields) {
            this.aitMetaFields = getCustomerAITMetaFieldInfo();
        }
        if(null != this.userInfoRowMapper){
            walletBalance = userInfoRowMapper.getWalletBalance();
            subscriberNumber = userInfoRowMapper.getSubscriberNumber();
            planDescription = userInfoRowMapper.getPlanDescription();
            planStatus = userInfoRowMapper.getPlanStatus();
        }
        UserDTO user = getWrappedInstance();
        List<Object[]> mfValues = new ArrayList<>();
        Collection<String> customerMetaFieldValues = this.getCustomerMetaFieldValues();

        // get eventLogs to find who had created/edited the customer
        List<EventLogDTO> eventLogDTOs = getEventLogDTOS(user.getId());
        // get created by
        String createdBy = getCreatedBy(eventLogDTOs);
        //get EventLogDTO to find who had edited the customer info.
        EventLogDTO eventLogDTO = getEventLogDTO(eventLogDTOs);

        Integer languageId = user.getLanguage().getId();
        Object[] objects = new Object[this.aitMetaFields.size() + customerMetaFieldValues.size() + 18];

        //custom fields
        objects[0] = user.getId();
        objects[1] = user.getUserName();
        objects[2] = subscriberNumber;
        objects[3] = getAssetStatus(user.getUserName());

        // customer
        objects[4] = customer != null ? walletBalance : null;
        objects[5] = customer != null ? planDescription : null;
        objects[6] = customer != null ? planStatus : null;
        objects[7] = customer != null ? customer.getIdentificationType() : null;
        objects[8] = customer != null ? customer.getIdentificationText() : null;
        objects[9] = customer != null ? createdBy : null;
        objects[10] = user.getCreateDatetime();
        objects[11] = eventLogDTO != null ? eventLogDTO.getBaseUser().getUserName() : null;
        objects[12] = eventLogDTO != null ? eventLogDTO.getCreateDatetime() : null;
        objects[13] = user.getLanguage() != null ? user.getLanguage().getDescription() : null;
        objects[14] = user.getCurrency() != null ? user.getCurrency().getDescription(languageId) : null;
        objects[15] = customer != null && customer.getAccountType() != null ? customer.getAccountType().getDescription(languageId) : null;
        objects[16] = getStatus(user);
        objects[17] = user.getLastStatusChange();

        // The value of initialTotalColumnCount is equal to total number of custom fields.
        int initialTotalColumnCount = 17;

        for (CustomerAITMetaFieldInfo customerAITMetaFieldInfo : this.aitMetaFields) {
            objects[++initialTotalColumnCount] = customerAITMetaFieldInfo.metaFieldValue;
        }

        for (Object object : customerMetaFieldValues) {
            objects[++initialTotalColumnCount] = object;
        }

        mfValues.add(objects);
        return mfValues.toArray(new Object[mfValues.size()][]);
    }

    public Object[][] getCustomerFieldValues() {

        Date today = Util.truncateDate(new Date());

        List<MetaFieldValue> metaFieldValues = customer.getAitTimelineMetaFieldsMap()
                                                       .values()
                                                       .stream()
                                                       .flatMap(ait -> ait.entrySet().stream())
                                                       .filter(aitMetaFields -> !aitMetaFields.getKey().after(today))
                                                       .flatMap(aitMetaFields -> aitMetaFields.getValue().stream())
                                                       .collect(Collectors.toList());
        UserDTO user = getWrappedInstance();
        Integer languageId = user.getLanguage().getId();
        List<String> customerMetaFieldValues = this.getCustomerMetaFieldValues();

        // get eventLogs to find who was created and edited
        List<EventLogDTO> eventLogDTOs = getEventLogDTOS(user.getId());
        // get created by
        String createdBy = getCreatedBy(eventLogDTOs);
        // get EventLogDTO to find who was edited the customer info
        EventLogDTO eventLogDTO = getEventLogDTO(eventLogDTOs);

        if(null != this.userInfoRowMapper){
            walletBalance = userInfoRowMapper.getWalletBalance();
            subscriberNumber = userInfoRowMapper.getSubscriberNumber();
            planDescription = userInfoRowMapper.getPlanDescription();
            planStatus = userInfoRowMapper.getPlanStatus();
        }
        return new Object[][]{
                {
                        user.getId(),
                        user.getUserName(),
                        subscriberNumber,
                        getAssetStatus(user.getUserName()),

                        // customer
                        customer != null ? walletBalance : null,
                        customer != null ? planDescription : null,
                        customer != null ? planStatus : null,
                        !customerMetaFieldValues.isEmpty() ? customerMetaFieldValues.get(0) : null,
                        customer != null ? customer.getIdentificationType() : null,
                        customer != null ? customer.getIdentificationText() : null,
                        !customerMetaFieldValues.isEmpty() ? customerMetaFieldValues.get(1) : null,

                        // contact
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.FIRST_NAME),
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.LAST_NAME),
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.ADDRESS1),
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.ADDRESS2),
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.CITY),
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.STATE_PROVINCE),
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.POSTAL_CODE),
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.COUNTRY_CODE),
                        concatMetaFields(getMetaFieldValueByType(metaFieldValues, MetaFieldType.PHONE_COUNTRY_CODE),
                                getMetaFieldValueByType(metaFieldValues, MetaFieldType.PHONE_AREA_CODE),
                                getMetaFieldValueByType(metaFieldValues, MetaFieldType.PHONE_NUMBER)),
                        concatMetaFields(getMetaFieldValueByType(metaFieldValues, MetaFieldType.FAX_COUNTRY_CODE),
                                getMetaFieldValueByType(metaFieldValues, MetaFieldType.FAX_AREA_CODE),
                                getMetaFieldValueByType(metaFieldValues, MetaFieldType.FAX_NUMBER)),
                        getMetaFieldValueByType(metaFieldValues, MetaFieldType.EMAIL),

                        createdBy,
                        user.getCreateDatetime(),
                        eventLogDTO != null ? eventLogDTO.getBaseUser().getUserName() : null,
                        eventLogDTO != null ? eventLogDTO.getCreateDatetime() : null,
                        user.getLanguage() != null ? user.getLanguage().getDescription() : null,
                        user.getCurrency() != null ? user.getCurrency().getDescription(languageId) : null,
                        customer != null && customer.getAccountType() != null ? customer.getAccountType().getDescription(languageId) : null,
                        getStatus(user),
                        user.getLastStatusChange(),
                }
        };
    }

    private String getAllUserAssets(Integer userId) {
        String assets = new AssetDAS().findAssetsIdentifierByUserId(userId);
        return (null != assets && !assets.isEmpty()) ? assets.substring(0, assets.length() - 1) : null;
    }

    private Map<Integer, String> getAccountInfoTypeIdAndNameByAccountId(Integer accountId) {
        Map<Integer, String> informationTypeIdAndNameMap = ACCOUNT_INFORMATION_TYPE_ID_AND_NAME_MAP.getOrDefault(accountId, new HashMap<>());
        if (informationTypeIdAndNameMap.isEmpty()) {
            informationTypeIdAndNameMap = new AccountInformationTypeDAS().getInformationTypeIdAndNameMapForAccountType(accountId);
            ACCOUNT_INFORMATION_TYPE_ID_AND_NAME_MAP.put(accountId, informationTypeIdAndNameMap);
        }

        return informationTypeIdAndNameMap;
    }

    private Map<String, Map<String, String>> getPopulatedAITMetaFieldsMap() {
        Map<String, Map<String, String>> allAITMetaFieldMap = getAllAITMetaFieldsMap();
        Map<String, Map<String, String>> metaFieldNameAndValueMapByGroupName = new HashMap<>();
        Map<Integer, String> informationTypeIdAndNameMap = getAccountInfoTypeIdAndNameByAccountId(accountTypeId);
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        for (Entry<Integer, String> entry : informationTypeIdAndNameMap.entrySet()) {
            metaFieldNameAndValueMapByGroupName.put(entry.getValue(), metaFieldDAS.getCustomerAITMetaFieldValue(customer.getId(), entry.getKey(), TimezoneHelper.companyCurrentDate(entityId)));
        }

        for (Entry<String, Map<String, String>> aitMetaField : allAITMetaFieldMap.entrySet()) {
            String aitName = aitMetaField.getKey();
            Map<String, String> metaFieldNameAndValueMapByGroupNameMap = metaFieldNameAndValueMapByGroupName.get(aitName);
            Map<String, String> resultMap = new HashMap<>();
            for (Entry<String, String> entry : aitMetaField.getValue().entrySet()) {
                String aitFieldName = entry.getKey().split(":")[1];
                resultMap.put(entry.getKey(), metaFieldNameAndValueMapByGroupNameMap.get(aitFieldName));
            }
            allAITMetaFieldMap.put(aitName, resultMap);

        }

        return allAITMetaFieldMap;
    }

    private Map<String, Map<String, String>> getAllAITMetaFieldsMap() {
        Map<String, Map<String, String>> allAITMetaFields = META_FIELD_MAP.getOrDefault(accountTypeId, new HashMap<>());
        if (allAITMetaFields.isEmpty()) {
            AccountTypeDTO dto = new AccountTypeDAS().find(accountTypeId, entityId);
            for (AccountInformationTypeDTO aitDTO : dto.getInformationTypes()) {
                String aitName = aitDTO.getName();
                Map<String, String> aitFieldNameAndValueMap = allAITMetaFields.getOrDefault(aitName, new HashMap<>());
                if (aitFieldNameAndValueMap.isEmpty()) {
                    aitFieldNameAndValueMap = new HashMap<>();
                }
                for (MetaField ac : aitDTO.getMetaFields()) {
                    aitFieldNameAndValueMap.put(aitName + ":" + ac.getName(), null);
                }
                allAITMetaFields.put(aitName, aitFieldNameAndValueMap);
            }
            META_FIELD_MAP.put(accountTypeId, allAITMetaFields);
        }

        return allAITMetaFields;
    }

    private List<String> getCustomerMetaFieldNames() {
        List<String> fieldNameList = CUSTOMER_META_FIELD_MAP.getOrDefault(entityId, new ArrayList<>());
        if (fieldNameList.isEmpty()) {
            fieldNameList.addAll(new MetaFieldDAS().getAvailableFields(entityId, new EntityType[]{EntityType.CUSTOMER}, true)
                    .stream()
                    .map(MetaField::getName)
                    .collect(Collectors.toList()));
            CUSTOMER_META_FIELD_MAP.put(entityId, fieldNameList);
        }
        return fieldNameList;
    }

    private List<String> getCustomerMetaFieldValues() {
        Map<String, String> customerMetaFieldNameAndValueMap = new MetaFieldDAS().getCustomerMetaFieldValue(customer.getId());

        return getCustomerMetaFieldNames()
                .stream()
                .map(fieldName -> customerMetaFieldNameAndValueMap.get(fieldName))
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getWrappedInstance() {
        return new UserDAS().find(userId);
    }

    @Override
    public void setDynamicExport(DynamicExport dynamicExport) {
        this.dynamicExport = dynamicExport;
    }

    private String getStatus(UserDTO user) {
        if (user.getDeleted() == 1) {
            bundle = ResourceBundle.getBundle("entityNotifications", user.getLanguage().asLocale());
            return bundle.getString("user.userstatus.deleted");
        } else if (user.getStatus() != null) {
            return user.getStatus().getDescription(user.getLanguage().getId());
        } else {
            return null;
        }
    }

    private Object getMetaFieldValueByType(List<MetaFieldValue> metaFieldValues, MetaFieldType metaFieldType) {
        return metaFieldValues.stream()
                .filter(mfv -> metaFieldType.equals(mfv.getField().getFieldUsage()))
                .map(MetaFieldValue::getValue)
                .findFirst()
                .orElse(null);
    }

    private String concatMetaFields(Object... objects) {
        return Arrays.stream(objects)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

    private List<EventLogDTO> getEventLogDTOS(Integer userId){
        return new EventLogDAS().getEventLogByAffectedUserId(userId);
    }

    private String getCreatedBy(List<EventLogDTO> eventLogDTOS) {
        return eventLogDTOS.stream()
            .filter(eventLogDto -> eventLogDto.getEventLogMessage().getId() == 25)
            .map(eventLogDto -> eventLogDto.getBaseUser().getUserName())
            .findFirst()
            .orElse(null);
    }

    private EventLogDTO getEventLogDTO(List<EventLogDTO> eventLogDTOS) {
        return eventLogDTOS.stream()
            .filter(eventLogDto -> eventLogDto.getEventLogMessage().getId() == 9)
            .findFirst()
            .orElse(null);
    }

    private String getAssetStatus(String userName) {
        AssetDTO assetByIdentifier = new AssetDAS().getAssetByIdentifier(userName);
        if (assetByIdentifier != null) {
            return assetByIdentifier.getAssetStatus().getId() == 300 ? "Released" : assetByIdentifier.isSuspended() ? "Suspended" : "Active";
        }
        return null;
    }
}
