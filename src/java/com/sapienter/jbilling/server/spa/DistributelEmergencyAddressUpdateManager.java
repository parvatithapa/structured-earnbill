package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.EmergencyAddressUpdateEvent;
import com.sapienter.jbilling.server.user.event.EmergencyAddressUpdateEvent.RequestType;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.apache.commons.lang.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by taimoor on 4/10/17.
 */
public class DistributelEmergencyAddressUpdateManager {

    private IWebServicesSessionBean webServicesSession;

    public static final String STREET_ADDRESS_SEPARATOR = " SPLIT ";
    public static final String PHONE_NUMBER_IDENTIFIER_REGIX = "[0-9]+";
    public static final String DUMMY_PHONE_NUMBER_IDENTIFIER = "New Number";
    public static final String CALL_IN_TRANSITION_IDENTIFIER = "IN TRANSITION";
    public static final String ERROR_CODE_ACCOUNT_INACTIVE = "Customer Inactive";

    private static final String PHONE_NOT_FOUND_ERROR = "Phone Number not found";
    private static final String FUTURE_ADDRESS_EFFECTIVE_DATE = "Future Address Effective Date";

    private boolean isAddressUpdate = false;
    private String errorCodes = null;
    private String errorMessage = null;

    public DistributelEmergencyAddressUpdateManager(IWebServicesSessionBean webServicesSession) {
        if(webServicesSession != null) {
            this.webServicesSession = webServicesSession;
        }else {
            this.webServicesSession = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        }
    }

    public void addNewPhoneNumber(Integer userId, String phoneNumber){
        addEmergencyAddress(userId, phoneNumber);
    }

    public void addEmergencyAddress(Integer userId, String phoneNumber){

        if(isPhoneNumber(phoneNumber)) {
            // Get Event object
            EmergencyAddressUpdateEvent emergencyAddress = createUpdateEvent(userId, phoneNumber, RequestType.ADD);

            if(emergencyAddress != null) {
            EventManager.process(emergencyAddress);

            this.isAddressUpdate = emergencyAddress.isUpdated();
            this.errorCodes = emergencyAddress.getErrorResponse();

            }else {
                this.isAddressUpdate=false;
            }
            updateCustomerMetaFields(userId, this.isAddressUpdate, this.errorCodes);
        }
    }

    public boolean updateEmergencyAddress(Integer userId, UserWS updatedUserWS, List<AssetWS> userAssets) {

        String phoneNumber = getPhoneNumber(userAssets);

        return updateEmergencyAddress(userId, phoneNumber, updatedUserWS);
    }

    public boolean updateEmergencyAddress(Integer userId, String phoneNumber) {

        this.isAddressUpdate = false;
        this.errorCodes = null;

        if(isPhoneNumber(phoneNumber)) {
            // Get Event object
            EmergencyAddressUpdateEvent emergencyAddress = createUpdateEvent(userId, phoneNumber, RequestType.UPDATE);

            if (emergencyAddress != null) {
                EventManager.process(emergencyAddress);

                this.isAddressUpdate = emergencyAddress.isUpdated();
                this.errorCodes = emergencyAddress.getErrorResponse();
            } else {
                this.isAddressUpdate = false;
            }

            updateCustomerMetaFields(userId, this.isAddressUpdate, this.errorCodes);
        }

        this.errorCodes = (errorCodes != null)? errorCodes : PHONE_NOT_FOUND_ERROR;

        return this.isAddressUpdate;
    }

    public boolean updateEmergencyAddress(Integer userId, String phoneNumber, UserWS updatedUserWS) {
        
        if (updatedUserWS == null) {
            return updateEmergencyAddress(userId, phoneNumber);
        }
        
        String incomingErrorCodes = getEmergencyAddressErrorCodes(updatedUserWS);
        boolean isAddressUpToDate = isEmergencyAddressUpToDateWithServer(updatedUserWS);
        this.isAddressUpdate = isAddressUpToDate;
        this.errorCodes = (incomingErrorCodes !=null) ? incomingErrorCodes: "";
        
        boolean isFutureEffectiveDate = false;
        Date northern911EffectiveDate = updatedUserWS.getEffectiveDateMap().get(getEmergencyAddressGroupAITId(updatedUserWS));
        if (DateConvertUtils.asLocalDate(northern911EffectiveDate).isAfter(LocalDate.now())) {
            isFutureEffectiveDate = true;
        }
        
        if (!isFutureEffectiveDate) {
            if (isPhoneNumber(phoneNumber)) {
                // Get Event object
                UserDTO userDTO = new UserBL(userId).getEntity();
                Map<String, String> persistedMetaFields = getEmergencyAddress(userDTO);
                Map<String, String> updatedMetaFields = getEmergencyAddress(updatedUserWS);

                if (persistedMetaFields != null && updatedMetaFields != null) {

                    boolean isAddressUpdated = isEmergencyAddressUpdated(persistedMetaFields, updatedMetaFields);

                    if (isAddressUpdated || !(errorCodes.contains(CALL_IN_TRANSITION_IDENTIFIER) || isAddressUpToDate)) {

                        if (!updatedUserWS.isAccountExpired()) {
                            EmergencyAddressUpdateEvent emergencyAddress = createUpdateEvent(userId, phoneNumber, RequestType.UPDATE, updatedMetaFields);

                            EventManager.process(emergencyAddress);

                            this.isAddressUpdate = emergencyAddress.isUpdated();
                            this.errorCodes = emergencyAddress.getErrorResponse();
                        } else {
                            this.isAddressUpdate = false;
                            this.errorCodes = ERROR_CODE_ACCOUNT_INACTIVE;
                        }
                    }
                } else {
                    this.isAddressUpdate = false;
                }
            } else {
                this.errorCodes = (errorCodes != null) ? errorCodes : PHONE_NOT_FOUND_ERROR;
            }
        } else {
            this.errorCodes = (errorCodes != null) ? errorCodes : FUTURE_ADDRESS_EFFECTIVE_DATE;
        }

        return this.isAddressUpdate;
    }

    public void deleteEmergencyAddress(Integer userId, String phoneNumber){

        if(isPhoneNumber(phoneNumber)) {

            ContactDTO contactDTO = new ContactDTO();

            contactDTO.setPhoneNumber(extractPhoneNumber(phoneNumber));

            // Get Event object
            EmergencyAddressUpdateEvent emergencyAddress = createUpdateEvent(userId, phoneNumber, RequestType.DELETE);
            if(emergencyAddress!= null) {

            EventManager.process(emergencyAddress);

            this.isAddressUpdate = emergencyAddress.isUpdated();
            this.errorCodes = emergencyAddress.getErrorResponse();

            }else{
                this.isAddressUpdate =false;
            }
            updateCustomerMetaFields(userId, this.isAddressUpdate, this.errorCodes);
        }
    }

    public void updateEmergencyAddressOnOrderUpdate(Integer userId, OrderDTO orderDTO) {

        OrderWS persistedOrder = webServicesSession.getOrder(orderDTO.getId());

        Date companyCurrentDate = Util.truncateDate(TimezoneHelper.companyCurrentDate(persistedOrder.getOwningEntityId()));

        List<String> phoneNumbers = getPhoneNumbers(persistedOrder);

        for (String phoneNumber : phoneNumbers) {
            if (persistedOrder.getActiveUntil() != null && persistedOrder.getActiveUntil().compareTo(companyCurrentDate) == 0) {
                this.deleteEmergencyAddress(userId, phoneNumber);
            }else{
                this.updateEmergencyAddress(userId, phoneNumber);
            }
        }
    }

    public void addOrUpdatePhoneNumber(Integer entityId, Integer userId, String newPhoneNumber, String oldPhoneNumber){

        if(oldPhoneNumber.equals(newPhoneNumber)) {
            if (isPhoneNumber(oldPhoneNumber)) {

                ContactDTO contactDTO = new ContactDTO();

                contactDTO.setPhoneNumber(extractPhoneNumber(oldPhoneNumber));

                // Get Event object
                EmergencyAddressUpdateEvent emergencyAddress = new EmergencyAddressUpdateEvent(contactDTO, RequestType.QUERY, userId, entityId);

                EventManager.process(emergencyAddress);

                if(!emergencyAddress.isUpdated()){
                     addEmergencyAddress(userId, oldPhoneNumber);
                }
            }
        }
        else if(!oldPhoneNumber.equals(newPhoneNumber)) {

            if (isPhoneNumber(oldPhoneNumber)) {
                deleteEmergencyAddress(userId, oldPhoneNumber);
            }

            if (isPhoneNumber(newPhoneNumber)){
                addEmergencyAddress(userId, newPhoneNumber);
            }
        }
    }

    public void deleteEmergencyAddressOnOrderDelete(OrderWS orderWS){

        List<String> phoneNumbers = getPhoneNumbers(orderWS);

        for (String phoneNumber : phoneNumbers) {
            this.deleteEmergencyAddress(orderWS.getUserId(), phoneNumber);
        }
    }

    private Integer getEmergencyAddressGroupAITId(UserDTO user){

        Integer groupAITId = null;

        AccountInformationTypeDTO emergencyAddressGroupAIT = new AccountInformationTypeDAS().findByName(SpaConstants.EMERGENCY_ADDRESS_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId());

        if(emergencyAddressGroupAIT != null) {
            groupAITId = emergencyAddressGroupAIT.getId();
        }

        if(groupAITId != null){

            CustomerAccountInfoTypeMetaField customerMetaField = user.getCustomer().getCurrentCustomerAccountInfoTypeMetaField(SpaConstants.MF_PROVIDED, groupAITId);
            if(customerMetaField !=null && customerMetaField.getMetaFieldValue() != null && customerMetaField.getMetaFieldValue().getValue() != null) {
                if (!(Boolean)customerMetaField.getMetaFieldValue().getValue()) {
                    groupAITId = new AccountInformationTypeDAS().findByName(SpaConstants.CONTACT_INFORMATION_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId()).getId();
                }
            }
        }

        return groupAITId;
    }

    private Integer getEmergencyAddressGroupAITId(UserWS userWS) {

        AccountInformationTypeDTO emergencyAddressGroupAIT = new AccountInformationTypeDAS().findByName(SpaConstants.EMERGENCY_ADDRESS_AIT, userWS.getEntityId(), userWS.getAccountTypeId());
        AccountInformationTypeDTO contactInformationGroupAIT = new AccountInformationTypeDAS().findByName(SpaConstants.CONTACT_INFORMATION_AIT, userWS.getEntityId(), userWS.getAccountTypeId());

        Integer emergencyAddressGroupAITId = emergencyAddressGroupAIT.getId();
        Integer contactInformationGroupAITId = contactInformationGroupAIT.getId();
        Integer groupAITId = (emergencyAddressGroupAITId!=null)? emergencyAddressGroupAITId : null ;

        for(MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()){
            if(metaFieldValueWS.getGroupId() !=null && metaFieldValueWS.getGroupId().equals(emergencyAddressGroupAITId)) {
                if (metaFieldValueWS.getFieldName().equals(SpaConstants.MF_PROVIDED)) {
                    if ((Boolean) metaFieldValueWS.getValue()) {
                        groupAITId = emergencyAddressGroupAITId;
                    } else {
                        groupAITId = contactInformationGroupAITId;
                    }

                    break;
                }
            }
        }

        return groupAITId;
    }

    private ContactDTO createContactDetails(Map<String, String> metaFieldsMap, String phoneNumber){

        ContactDTO contactDTO = new ContactDTO();

        contactDTO.setPhoneNumber(extractPhoneNumber(phoneNumber));

        String customerName = "";
        if(metaFieldsMap.containsKey(SpaConstants.CUSTOMER_NAME)){
            customerName = metaFieldsMap.get(SpaConstants.CUSTOMER_NAME);
        }

        String city = "";
        if(metaFieldsMap.containsKey(SpaConstants.CITY)){
            city = metaFieldsMap.get(SpaConstants.CITY);
        }

        String postalCode = "";
        if(metaFieldsMap.containsKey(SpaConstants.POSTAL_CODE)){
            postalCode = metaFieldsMap.get(SpaConstants.POSTAL_CODE);
        }

        String province = "";
        if(metaFieldsMap.containsKey(SpaConstants.PROVINCE)){
            province = metaFieldsMap.get(SpaConstants.PROVINCE);
        }

        String aptSuite = "";
        if(metaFieldsMap.containsKey(SpaConstants.STREET_APT_SUITE)){
            aptSuite = metaFieldsMap.get(SpaConstants.STREET_APT_SUITE);
        }

        String streetName = "";
        if(metaFieldsMap.containsKey(SpaConstants.STREET_NAME)){
            streetName = metaFieldsMap.get(SpaConstants.STREET_NAME);
        }

        String streetNumber = "";
        if(metaFieldsMap.containsKey(SpaConstants.STREET_NUMBER_SUFFIX)){
            streetNumber = metaFieldsMap.get(SpaConstants.STREET_NUMBER_SUFFIX) + " ";
        }

        if(metaFieldsMap.containsKey(SpaConstants.STREET_NUMBER)){
            streetNumber += metaFieldsMap.get(SpaConstants.STREET_NUMBER);
        }

        String streetDirection = "";
        if (metaFieldsMap.containsKey(SpaConstants.STREET_DIRECTION)) {
            streetDirection = metaFieldsMap.get(SpaConstants.STREET_DIRECTION);
        }

        String streetType = "";
        if (metaFieldsMap.containsKey(SpaConstants.STREET_TYPE)) {
            streetType = metaFieldsMap.get(SpaConstants.STREET_TYPE);
        }

        String[] customerNameArray = customerName.split(" ", 2);
        if(customerNameArray.length >= 1) {

            contactDTO.setFirstName(customerNameArray[0]);

            if(customerNameArray.length >= 2) {
                contactDTO.setLastName(customerNameArray[1]);
            }
        }
        contactDTO.setCity(city);
        contactDTO.setPostalCode(postalCode);
        contactDTO.setStateProvince(province);

        // Concatenate Street address
        contactDTO.setAddress1(streetNumber + STREET_ADDRESS_SEPARATOR + streetName + STREET_ADDRESS_SEPARATOR + streetType + STREET_ADDRESS_SEPARATOR + streetDirection + STREET_ADDRESS_SEPARATOR + aptSuite);

        return contactDTO;
    }

    private EmergencyAddressUpdateEvent createUpdateEvent(Integer userId, String phoneNumber, RequestType requestType){

        UserDTO user = new UserBL(userId).getEntity();

        // Get Emergency Address
        Map<String, String> metaFieldsMap = getEmergencyAddress(user);

        if(metaFieldsMap !=null) {
        // Create Contact details from Emergency address to be used in distributel event
        ContactDTO contactDTO = createContactDetails(metaFieldsMap, phoneNumber);

        // Create Event object
        return new EmergencyAddressUpdateEvent(contactDTO, requestType, userId, user.getEntity().getId());
        }
        return null;
    }

    private EmergencyAddressUpdateEvent createUpdateEvent(Integer userId, String phoneNumber, RequestType requestType, Map<String, String> updatedMetaFields){

        UserDTO userDTO = new UserBL(userId).getEntity();

        // Create Contact details from Emergency address to be used in distributel event
        ContactDTO contactDTO = createContactDetails(updatedMetaFields, phoneNumber);

        // Create Event object
        return new EmergencyAddressUpdateEvent(contactDTO, requestType, userId, userDTO.getEntity().getId());
    }

    /**
     * Return Phone Number if available otherwise NULL
     * @param userAssets
     * @return
     */
    private String getPhoneNumber(List<AssetWS> userAssets){
        String phoneNumber = null;

        for(AssetWS assetWS : userAssets){
            for (MetaFieldValueWS metaFieldValue : assetWS.getMetaFields()){
                if(metaFieldValue.getFieldName().equals("Phone Number")){
                    if (isPhoneNumber(String.valueOf(metaFieldValue.getValue()))){

                        phoneNumber = String.valueOf(metaFieldValue.getValue());
                        break;
                    }
                }
            }
        }
        return phoneNumber;
    }

    /**
     * Return a list of Phone Number if available otherwise Empty
     * @param orderDTO
     * @return
     */
    private List<String> getPhoneNumbers(OrderWS orderDTO){

        List<String> phoneNumbers = new ArrayList<String>();

        for(OrderLineWS lineDTO: orderDTO.getOrderLines()){

            for(Integer assetId: lineDTO.getAssetIds()) {
                AssetWS assetWS = webServicesSession.getAsset(assetId);
                if (assetWS!=null){
                    for (MetaFieldValueWS metaFieldValue : assetWS.getMetaFields()){
                        if(metaFieldValue.getFieldName().equals("Phone Number")){
                            if (isPhoneNumber(String.valueOf(metaFieldValue.getValue()))){
                                phoneNumbers.add(String.valueOf(metaFieldValue.getValue()));
                                break;
                            }
                        }
                    }
                }
            }
        }

        return phoneNumbers;
    }

    /**
     * Verifies if the Asset identifier is Phone Number
     * @param assetIdentifier
     * @return
     */
    private boolean isPhoneNumber(String assetIdentifier){
        return !StringUtils.isEmpty(assetIdentifier) && assetIdentifier.matches(PHONE_NUMBER_IDENTIFIER_REGIX);
    }

    public String getErrorCodes() {
        return errorCodes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private String extractPhoneNumber(String assetIdentifier){
        return assetIdentifier.trim();
    }

    private Map<String, String> getEmergencyAddress(UserWS userWS){

        Integer groupAITId = getEmergencyAddressGroupAITId(userWS);

        Map<String, String> metaFieldsMap = new HashMap<String, String>();

        for(MetaFieldValueWS metaFieldValueWS :userWS.getMetaFields()){
            if(metaFieldValueWS.getGroupId()!=null && metaFieldValueWS.getGroupId().equals(groupAITId)) {
                metaFieldsMap.put(metaFieldValueWS.getFieldName(), String.valueOf( metaFieldValueWS.getValue()));
            }
        }

        AccountInformationTypeDTO contactInformationGroupAIT = new AccountInformationTypeDAS().findByName(SpaConstants.CONTACT_INFORMATION_AIT, userWS.getEntityId(), userWS.getAccountTypeId());
        Integer contactInformationGroupAITId = contactInformationGroupAIT.getId();

        for(MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()){
            if(metaFieldValueWS.getGroupId()!=null && metaFieldValueWS.getGroupId().equals(contactInformationGroupAITId)
                    && metaFieldValueWS.getFieldName().equals(SpaConstants.CUSTOMER_NAME)) {
                metaFieldsMap.put(metaFieldValueWS.getFieldName(), (String) metaFieldValueWS.getValue());

                break;
            }
        }

        return metaFieldsMap;
    }

    private Map<String, String> getEmergencyAddress(UserDTO user){

        AccountInformationTypeDTO contactInformationGroupAIT = new AccountInformationTypeDAS().
                findByName(SpaConstants.CONTACT_INFORMATION_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId());

        Integer contactInformationGroupAITId = contactInformationGroupAIT.getId();
        Integer groupAITId = getEmergencyAddressGroupAITId(user);

        Map<String, String> metaFieldsMap = new HashMap<String, String>();

        if(groupAITId == null)
            return null;

        Date currentEffectiveDate = user.getCustomer().getCurrentEffectiveDateByGroupId(groupAITId);
        CustomerAccountInfoTypeMetaField accountInfoTypeMetaField;

        String customerName = "";
        accountInfoTypeMetaField = user.getCustomer().getCurrentCustomerAccountInfoTypeMetaField(SpaConstants.CUSTOMER_NAME, contactInformationGroupAITId);
        if(accountInfoTypeMetaField !=null){
            customerName = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
        }
        metaFieldsMap.put(SpaConstants.CUSTOMER_NAME, customerName);

        String city = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.CITY, groupAITId, currentEffectiveDate);
        if(accountInfoTypeMetaField !=null){
            city = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
        }
        metaFieldsMap.put(SpaConstants.CITY, city);

        String postalCode = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.POSTAL_CODE, groupAITId, currentEffectiveDate);
        if(accountInfoTypeMetaField !=null){
            postalCode = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
        }
        metaFieldsMap.put(SpaConstants.POSTAL_CODE, postalCode);

        String province = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.PROVINCE, groupAITId, currentEffectiveDate);
        if(accountInfoTypeMetaField !=null){
            province = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
        }
        metaFieldsMap.put(SpaConstants.PROVINCE, province);

        String aptSuite = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_APT_SUITE, groupAITId, currentEffectiveDate);
        if(accountInfoTypeMetaField !=null){
            aptSuite = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
        }
        metaFieldsMap.put(SpaConstants.STREET_APT_SUITE, aptSuite);

        String streetName = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_NAME, groupAITId, currentEffectiveDate);
        if(accountInfoTypeMetaField !=null){
            streetName = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
        }
        metaFieldsMap.put(SpaConstants.STREET_NAME, streetName);

        String streetNumber = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_NUMBER, groupAITId, currentEffectiveDate);
        if(accountInfoTypeMetaField !=null){
            streetNumber = String.valueOf(accountInfoTypeMetaField.getMetaFieldValue().getValue());
        }
        metaFieldsMap.put(SpaConstants.STREET_NUMBER, streetNumber);

        String streetNumberSuffix = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_NUMBER_SUFFIX, groupAITId, currentEffectiveDate);
        if(accountInfoTypeMetaField !=null){
            streetNumberSuffix = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
        }
        metaFieldsMap.put(SpaConstants.STREET_NUMBER_SUFFIX, streetNumberSuffix);

        String streetType = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_TYPE, groupAITId, currentEffectiveDate);
        if (accountInfoTypeMetaField != null) {
            streetType = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
            metaFieldsMap.put(SpaConstants.STREET_TYPE, streetType);
        }


        String streetDirection = "";
        accountInfoTypeMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_DIRECTION, groupAITId, currentEffectiveDate);
        if (accountInfoTypeMetaField != null) {
            streetDirection = (String) accountInfoTypeMetaField.getMetaFieldValue().getValue();
            metaFieldsMap.put(SpaConstants.STREET_DIRECTION, streetDirection);
        }


        return metaFieldsMap;
    }

    private boolean isEmergencyAddressUpdated(Map<String, String> persistedMetaFields, Map<String, String> updatedMetaFields){
        boolean isUpdated = false;

        for(Map.Entry<String, String> entry :persistedMetaFields.entrySet()){
            String key = entry.getKey();
            if(updatedMetaFields.containsKey(key)){
                boolean isModifiedByNull = (entry.getValue() != null && updatedMetaFields.get(key) == null) || (entry.getValue() == null && updatedMetaFields.get(key) != null);                
                if(isModifiedByNull || !(entry.getValue()).equals(updatedMetaFields.get(key))){
                    isUpdated = true;
                    break;
                }
            }
        }

        return isUpdated;
    }

    private boolean isEmergencyAddressUpToDateWithServer(UserWS userWS){

        boolean isUpToDate = false;

        for(MetaFieldValueWS metaFieldValueWS :userWS.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals(SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE)) {
                isUpToDate = (boolean) metaFieldValueWS.getValue();
                break;
            }
        }

        return  isUpToDate;
    }

    private String getEmergencyAddressErrorCodes(UserWS userWS){

        String emergencyCodes = "";

        for(MetaFieldValueWS metaFieldValueWS :userWS.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals(SpaConstants.NORTHERN_911_ERROR_CODE)) {
                emergencyCodes = ((String) metaFieldValueWS.getValue());
                break;
            }
        }

        return emergencyCodes;
    }

    public void updateCustomerServerResponseMetaFields(UserWS userWS, UserDTOEx userDTOEx, boolean isUpdated) {

        if (userDTOEx.getCustomer() == null) return;

        boolean upToDateMetaFieldFound = false;
        boolean errorResponseMetaFieldFound = false;

        String errorCodes = getErrorCodes().replace(CALL_IN_TRANSITION_IDENTIFIER, "");

        MetaFieldValueWS[] metaFieldValueArray = userWS.getMetaFields();

        List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>(Arrays.asList(metaFieldValueArray));

        for (MetaFieldValueWS metaFieldValue : metaFieldValueArray) {

            if (metaFieldValue.getFieldName().equals(SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE)) {
                metaFieldValue.setValue(isUpdated);
                upToDateMetaFieldFound = true;
            }

            if (metaFieldValue.getFieldName().equals(SpaConstants.NORTHERN_911_ERROR_CODE)) {
                metaFieldValue.setValue(errorCodes);
                errorResponseMetaFieldFound = true;
            }
        }

        if (!upToDateMetaFieldFound) {
            MetaField metaField = new MetaFieldDAS().getFieldByName(userWS.getEntityId(), new EntityType[]{EntityType.CUSTOMER}, SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE);
            if (metaField != null) {
                MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                metaFieldValueWS.setFieldName(SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE);
                metaFieldValueWS.setBooleanValue(isUpdated);
                metaFieldValueList.add(metaFieldValueWS);
            }
        }

        if (!errorResponseMetaFieldFound) {
            MetaField metaField = new MetaFieldDAS().getFieldByName(userWS.getEntityId(), new EntityType[]{EntityType.CUSTOMER}, SpaConstants.NORTHERN_911_ERROR_CODE);
            if (metaField != null) {
                MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                metaFieldValueWS.setFieldName(SpaConstants.NORTHERN_911_ERROR_CODE);
                metaFieldValueWS.setStringValue(errorCodes);
                metaFieldValueList.add(metaFieldValueWS);
            }
        }

        MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
        metaFieldValueList.toArray(updatedMetaFieldValueWSArray);

        userWS.setMetaFields(updatedMetaFieldValueWSArray);

        // convert user WS to a DTO that includes customer data
        UserDTOEx dto = new UserDTOEx(userWS, userWS.getEntityId());

        userDTOEx.getCustomer().setMetaFields(dto.getCustomer().getMetaFields());

    }

    public void updateCustomerMetaFields(Integer userId, boolean isUpdated, String errorResponse){
        UserWS userWS = webServicesSession.getUserWS(userId);

        MetaFieldValueWS[] metaFieldValueArray = userWS.getMetaFields();

        List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>(Arrays.asList(metaFieldValueArray));

        boolean upToDateMetaFieldFound = false;
        boolean errorResponseMetaFieldFound = false;

        if(errorResponse != null) {
            if(errorResponse.contains(CALL_IN_TRANSITION_IDENTIFIER)) {
                errorResponse = errorResponse.replace(CALL_IN_TRANSITION_IDENTIFIER,"");
            }else{
                errorResponse = errorResponse.concat(CALL_IN_TRANSITION_IDENTIFIER);
            }
        }

        for (MetaFieldValueWS metaFieldValue : metaFieldValueArray) {

            if (metaFieldValue.getFieldName().equals(SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE)) {
                metaFieldValue.setValue(isUpdated);
                upToDateMetaFieldFound = true;
            }

            if (metaFieldValue.getFieldName().equals(SpaConstants.NORTHERN_911_ERROR_CODE)) {
                metaFieldValue.setValue(errorResponse);
                errorResponseMetaFieldFound = true;
            }
        }

        if (!upToDateMetaFieldFound){
            MetaField metaField = new MetaFieldDAS().getFieldByName(userWS.getEntityId(), new EntityType[]{EntityType.CUSTOMER}, SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE);
            if(metaField!=null) {
                MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                metaFieldValueWS.setFieldName(SpaConstants.EMERGENCY_ADDRESS_UPTO_DATE);
                metaFieldValueWS.setBooleanValue(isUpdated);
                metaFieldValueList.add(metaFieldValueWS);
            }
        }

        if (!errorResponseMetaFieldFound){
            MetaField metaField = new MetaFieldDAS().getFieldByName(userWS.getEntityId(), new EntityType[]{EntityType.CUSTOMER}, SpaConstants.NORTHERN_911_ERROR_CODE);
            if(metaField!=null) {
                MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                metaFieldValueWS.setFieldName(SpaConstants.NORTHERN_911_ERROR_CODE);
                metaFieldValueWS.setStringValue(errorResponse);
                metaFieldValueList.add(metaFieldValueWS);
            }
        }

        MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
        metaFieldValueList.toArray(updatedMetaFieldValueWSArray);

        userWS.setMetaFields(updatedMetaFieldValueWSArray);

        webServicesSession.updateUser(userWS);
    }
}
