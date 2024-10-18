package com.sapienter.jbilling.server.spa;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

public class SpaHappyFoxBL {
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private SpaHappyFox spaHappyFox;
    private SpaCommonFields commonFields;
    private List<String> ratePlan = new ArrayList<>();
    private List<String> ctiaca = new ArrayList<>();
    private List<String> cyx = new ArrayList<>();
    private List<String> serviceConnectionDate = new ArrayList<>();
    private List<String> serviceStatus = new ArrayList<>();
    private List<String> servicePhoneNumber = new ArrayList<>();
    private List<String> pPPoEuserName = new ArrayList<>();
    private List<String> pPPoEPassword = new ArrayList<>();
    private List<String> cPEstatus = new ArrayList<>();
    private List<String> cPEmakeModel = new ArrayList<>();
    private List<String> cPEMACaddress = new ArrayList<>();
    private List<String> cPEserialNumber = new ArrayList<>();
    private List<String> displayName = new ArrayList<>();
    private List<String> sipPassword = new ArrayList<>();
    private List<String> cPEPurchaseDate = new ArrayList<>();
    private List<String> emailPortalUserID = new ArrayList<>();
    private List<String> emailPortalPassword = new ArrayList<>();
    private Integer entityId;
    private Integer languageId;

    private static final String DSL = "DSL";
    private static final String CABLE_INTERNET = "Cable Internet";
    private static final String  HOME_PHONE = "Home Phone";

    private static final String SPACE = " ";
    private static final String HYPHEN = "-";
    private static final String COMMA = ", ";
    private static final String ADDRESS_PATTERN = "%s%s%s%s%s";

    public SpaHappyFoxBL(SpaHappyFox spaHappyFox) {
        this.spaHappyFox =  spaHappyFox;
        this.commonFields = spaHappyFox.getCommonFields();
    }

    public SpaHappyFox getSpaHappyFox() {
        return spaHappyFox;
    }

    public void setSpaHappyFox(SpaHappyFox spaHappyFox) {
        this.spaHappyFox = spaHappyFox;
    }

    public void getCustomerFields(UserWS user) {

        AccountInformationTypeDTO contactInformationAIT = new AccountInformationTypeDAS().findByName(SpaConstants.CONTACT_INFORMATION_AIT, user.getEntityId(), user.getAccountTypeId());
        CustomerDTO customerDTO = new CustomerDAS().find(user.getCustomerId());

        Date effectiveDate = customerDTO.getCurrentEffectiveDateByGroupId(contactInformationAIT.getId());
        CustomerAccountInfoTypeMetaField accountInfoTypeMetaField = customerDTO.getCustomerAccountInfoTypeMetaField(SpaConstants.EMAIL_ADDRESS, contactInformationAIT.getId(),effectiveDate);
        if(null != accountInfoTypeMetaField && null != accountInfoTypeMetaField.getMetaFieldValue()){
            commonFields.setEmailAddress(String.valueOf(accountInfoTypeMetaField.getMetaFieldValue().getValue()));
        }
        accountInfoTypeMetaField = customerDTO.getCustomerAccountInfoTypeMetaField(SpaConstants.EMAIL_ADDRESS, contactInformationAIT.getId(),effectiveDate);
        if(null != accountInfoTypeMetaField && null != accountInfoTypeMetaField.getMetaFieldValue()){
            commonFields.setEmailAddress(String.valueOf(accountInfoTypeMetaField.getMetaFieldValue().getValue()));
        }
        commonFields.setAccountNumber(String.valueOf(user.getId()));
        commonFields.setLanguage(user.getLanguage());
        setDistributelAddress(user.getId());
    }

    public void getOtherFields(UserWS user) {
        entityId = user.getEntityId();
        languageId = user.getLanguageId();

        if("Email".equals(commonFields.getServiceType())){
            MetaFieldValueWS customerFieldValue = getMetaField(user.getMetaFields(), SpaConstants.EMAIL_PORTAL_USERID);
            if (null != customerFieldValue) {
                emailPortalUserID.add(customerFieldValue.getStringValue());
            }
            customerFieldValue = getMetaField(user.getMetaFields(), SpaConstants.EMAIL_PORTAL_PASSWORD);
            if (null != customerFieldValue) {
                emailPortalPassword.add(customerFieldValue.getStringValue());
            }
        }
        List<OrderDTO> orders= new OrderDAS().findRecurringOrdersByUserId(user.getId());
        if(null != orders){
            for (OrderDTO order : orders) {
                List<OrderLineDTO> orderLines = order.getLines();
                if(DSL.equals(commonFields.getServiceType())
                        || CABLE_INTERNET.equals(commonFields.getServiceType())
                        || HOME_PHONE.equals(commonFields.getServiceType())){
                    setRatePlan(order);
                }
                if(null != order.getChildOrders() && !order.getChildOrders().isEmpty() && !getAsset(order).isEmpty()){
                    getAsset(order).stream()
                    .forEach(this::addPrivateFields);
                }else{
                    for (OrderLineDTO orderLine : orderLines) {
                        if(isLineInclude(orderLine) && null != orderLine.getAssets() && !orderLine.getAssets().isEmpty()){
                            orderLine.getAssets().stream()
                              .forEach(this::addPrivateFields);
                        }
                    }
                }
            }
        }
    }

    private void addPrivateFields(AssetDTO asset) {

        if(isHardwareAssetOrLine(asset,null)){
            cPEmakeModel.add(String.valueOf(asset.getItem().getInternalNumber()));
            cPEPurchaseDate.add(getCPEPurchaseDate(asset));
        }

        MetaFieldValue fieldValue = null;
        switch (commonFields.getServiceType()) {
        case DSL:
            if(!isHardwareAssetOrLine(asset,null)){
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.DOMAIN_ID);
                if (null != fieldValue) {
                    ctiaca.add(String.valueOf(fieldValue.getValue()));
                }
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_PPPOE_USER);
                if (null != fieldValue) {
                    pPPoEuserName.add(String.valueOf(fieldValue.getValue()));
                }
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_PPPOE_PASSWORD);
                if (null != fieldValue) {
                    pPPoEPassword.add(String.valueOf(fieldValue.getValue()));
                }
            }else {
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_MAC_ADRESS);
                if (null != fieldValue) {
                    cPEMACaddress.add(String.valueOf(fieldValue.getValue()));
                }
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_SERIAL_NUMBER);
                if (null != fieldValue) {
                    cPEserialNumber.add(String.valueOf(fieldValue.getValue()));
                }
                cPEstatus.add(asset.getAssetStatus().getDescription(languageId));
            }
            break;
        case HOME_PHONE:
            if(!isHardwareAssetOrLine(asset,null)){
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.PHONE_NUMBER);
                if (null != fieldValue ) {
                    servicePhoneNumber.add(String.valueOf(fieldValue.getValue()));
                }
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_CLIDNAME);
                if (null != fieldValue) {
                    displayName.add(String.valueOf(fieldValue.getValue()));
                }
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.SIPPASSWORD);
                if (null != fieldValue) {
                    sipPassword.add(String.valueOf(fieldValue.getValue()));
                }
            }else{
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_MAC_ADRESS);
                if (null != fieldValue) {
                    cPEMACaddress.add(String.valueOf(fieldValue.getValue()));
                }
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_SERIAL_NUMBER);
                if (null != fieldValue) {
                    cPEserialNumber.add(String.valueOf(fieldValue.getValue()));
                }
                cPEstatus.add(asset.getAssetStatus().getDescription(languageId));
            }

            break;
        case CABLE_INTERNET:
            if(!isHardwareAssetOrLine(asset,null)){
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.DOMAIN_ID);
                if (null != fieldValue) {
                    cyx.add(String.valueOf(fieldValue.getValue()));
                }
            }else {
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_MAC_ADRESS);
                if (null != fieldValue) {
                    cPEMACaddress.add(String.valueOf(fieldValue.getValue()));
                }
                fieldValue = MetaFieldHelper.getMetaField(asset,SpaConstants.MF_SERIAL_NUMBER);
                if (null != fieldValue) {
                    cPEserialNumber.add(String.valueOf(fieldValue.getValue()));
                }
                cPEstatus.add(asset.getAssetStatus().getDescription(languageId));
            }
            break;
        default:
            break;
        }
    }

    private Set<AssetDTO> getAsset(OrderDTO order) {
        Set<AssetDTO> asset  = new HashSet<>();
        for (OrderDTO childOrder : order.getChildOrders()) {
            if(childOrder.getDeleted() == 0){
                for (OrderLineDTO orderLine : childOrder.getLines()) {
                    if(isLineInclude(orderLine) && null != orderLine.getAssets() && !orderLine.getAssets().isEmpty()){
                        asset = orderLine.getAssets();
                    }
                }
            }
        }
        return asset;
    }

    private void setRatePlan(OrderDTO order) {
        for (OrderLineDTO orderLineDTO : order.getLines()) {
            if(null != orderLineDTO.getItem() && orderLineDTO.getItem().isPlan() && isLineInclude(orderLineDTO)
                    && !isHardwareAssetOrLine(null, orderLineDTO)){
                PlanDTO plan = orderLineDTO.getItem().getPlan();
                List <PlanItemDTO> planItems = plan.getPlanItems().stream()
                    .filter(pi -> pi.getBundle().getQuantity().compareTo(BigDecimal.ZERO) > 0
                            && pi.getBundle().getPeriod().equals(plan.getPeriod())).collect(Collectors.toList());
                if(null != getLineDescriptionForRatePlan(planItems,order)){
                    ratePlan.add(getLineDescriptionForRatePlan(planItems,order));
                }
                serviceConnectionDate.add(formatter.format(order.getActiveSince()));
                serviceStatus.add(new OrderStatusDAS().find(order.getStatusId()).getDescription(order.getUser().getLanguage().getId()));
                return;
            }
        }
        if (!setRatePlanForMigratedProduct(order)){
            List<OrderLineDTO> orderLines = order.getLines()
                    .stream()
                    .filter(line -> (line.getTypeId() != Constants.ORDER_LINE_TYPE_DISCOUNT
                                        && line.getTypeId() != Constants.ORDER_LINE_TYPE_PENALTY
                                        && line.getTypeId() != Constants.ORDER_LINE_TYPE_ADJUSTMENT
                                        && line.getTypeId() != Constants.ORDER_LINE_TYPE_TAX))
                   .filter(line -> (0 >= BigDecimal.ZERO.compareTo(line.getAmount())))
                   .filter(this::isLineInclude)
                   .collect(Collectors.toList());

               if(1 == orderLines.size() && !isHardwareAssetOrLine(null, orderLines.get(0))){
                   ratePlan.add(orderLines.get(0).getDescription());
                   serviceConnectionDate.add(formatter.format(order.getActiveSince()));
                   serviceStatus.add(new OrderStatusDAS().find(order.getStatusId()).getDescription(order.getUser().getLanguage().getId()));
               }else{
                   if(CollectionUtils.isNotEmpty(orderLines)){
                       for (OrderLineDTO orderLineDTO : orderLines) {
                           if(!CollectionUtils.isEmpty(orderLineDTO.getAssets()) && !isHardwareAssetOrLine(null, orderLineDTO)){
                               ratePlan.add(orderLineDTO.getDescription());
                               serviceConnectionDate.add(formatter.format(order.getActiveSince()));
                               serviceStatus.add(new OrderStatusDAS().find(order.getStatusId()).getDescription(order.getUser().getLanguage().getId()));
                               return;
                           } 
                       }
                       for (OrderLineDTO orderLineDTO : orderLines) {
	                        if(!isHardwareAssetOrLine(null, orderLineDTO)){
	                           ratePlan.add(orderLineDTO.getDescription());
	                           serviceConnectionDate.add(formatter.format(order.getActiveSince()));
	                           serviceStatus.add(new OrderStatusDAS().find(order.getStatusId()).getDescription(order.getUser().getLanguage().getId()));
	                           return;
	                       }
                       }
                   }
               }
        }
    }

    private String getLineDescriptionForRatePlan(List<PlanItemDTO> planItems, OrderDTO order) {
        List<OrderLineDTO> lines = new ArrayList<>();
        for (PlanItemDTO planItemDTO : planItems) {
            for (OrderLineDTO line : order.getLines()) {
                if(planItemDTO.getItem().equals(line.getItem())){
                    lines.add(line);
                }
            }
        }
        lines = lines.stream()
                     .filter(line -> (line.getTypeId() != Constants.ORDER_LINE_TYPE_DISCOUNT
                          && line.getTypeId() != Constants.ORDER_LINE_TYPE_PENALTY
                          && line.getTypeId() != Constants.ORDER_LINE_TYPE_ADJUSTMENT
                          && line.getTypeId() != Constants.ORDER_LINE_TYPE_TAX))
                     .filter(line -> (0 >= BigDecimal.ZERO.compareTo(line.getAmount())))
                     .collect(Collectors.toList());
        if(!lines.isEmpty()){
            return lines.get(0).getDescription();
        }
        return null;
    }

    private boolean setRatePlanForMigratedProduct(OrderDTO order) {
        for (OrderLineDTO orderLineDTO : order.getLines()) {
            if(isLineInclude(orderLineDTO) && null != orderLineDTO.getItem() 
                    && isMigratedProduct(orderLineDTO.getItem()) && CollectionUtils.isEmpty(orderLineDTO.getAssets())
                    && 0 >= BigDecimal.ZERO.compareTo(orderLineDTO.getAmount())){
                ratePlan.add(orderLineDTO.getDescription());
                serviceConnectionDate.add(formatter.format(order.getActiveSince()));
                serviceStatus.add(new OrderStatusDAS().find(order.getStatusId()).getDescription(order.getUser().getLanguage().getId()));
                return true;
            }
        }
        return false;
    }

    public void setAllFields(){
        SpaPrivateNotes spaPrivateNotes = new SpaPrivateNotes();
        spaPrivateNotes.setEmailPortalPassword(!emailPortalPassword.isEmpty() ? emailPortalPassword.toArray(new String[0]) : null);
        spaPrivateNotes.setEmailPortalUserID(!emailPortalUserID.isEmpty() ? emailPortalUserID.toArray(new String[0]) : null);
        spaPrivateNotes.setServiceConnectionDate(!serviceConnectionDate.isEmpty() ? serviceConnectionDate.toArray(new String[0]) : null);
        spaPrivateNotes.setServiceStatus(!serviceStatus.isEmpty() ? serviceStatus.toArray(new String[0]) : null);
        spaPrivateNotes.setRatePlan(!ratePlan.isEmpty() ? ratePlan.toArray(new String[0]) : null);
        spaPrivateNotes.setcTIACA(!ctiaca.isEmpty() ? ctiaca.toArray(new String[0]) : null);
        spaPrivateNotes.setpPPoEuserName(!pPPoEuserName.isEmpty() ? pPPoEuserName.toArray(new String[0]) : null);
        spaPrivateNotes.setpPPoEPassword(!pPPoEPassword.isEmpty() ? pPPoEPassword.toArray(new String[0]) : null);
        spaPrivateNotes.setServicePhoneNumber(!servicePhoneNumber.isEmpty() ? servicePhoneNumber.toArray(new String[0]) : null);
        spaPrivateNotes.setcPEmakeModel(!cPEmakeModel.isEmpty() ? cPEmakeModel.toArray(new String[0]) : null);
        spaPrivateNotes.setcPEMACaddress(!cPEMACaddress.isEmpty() ? cPEMACaddress.toArray(new String[0]) : null);
        spaPrivateNotes.setcPEserialNumber(!cPEserialNumber.isEmpty() ? cPEserialNumber.toArray(new String[0]) : null);
        spaPrivateNotes.setDisplayName(!displayName.isEmpty() ? displayName.toArray(new String[0]) : null);
        spaPrivateNotes.setSipPassword(!sipPassword.isEmpty() ? sipPassword.toArray(new String[0]) : null);
        spaPrivateNotes.setcPEstatus(!cPEstatus.isEmpty() ? cPEstatus.toArray(new String[0]) : null);
        spaPrivateNotes.setcYX(!cyx.isEmpty() ? cyx.toArray(new String[0]) : null);
        spaPrivateNotes.setcPEPurchaseDate(!cPEPurchaseDate.isEmpty() ? cPEPurchaseDate.toArray(new String[0]) : null);
        spaHappyFox.setCommonFields(commonFields);
        spaHappyFox.setPrivateNotes(spaPrivateNotes);
    }

    public List<Integer> findUser() {
        return new UserDAS().getUserIdByCustomerDetails(commonFields.getFullCustomerName(),
                commonFields.getPhoneNumber().replaceAll("\\s", ""), commonFields.getServicePostalCode());
    }

    public boolean validate() {
        return(StringUtils.isBlank(commonFields.getFullCustomerName())
                || StringUtils.isBlank(commonFields.getPhoneNumber())
                || StringUtils.isBlank(commonFields.getServicePostalCode())
                || StringUtils.isBlank(commonFields.getServiceType()));
    }

    private MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields, String fieldName) {
        for (MetaFieldValueWS ws : metaFields) {
            if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
                return ws;
            }
        }
        return null;
    }

    private String validateAndGetMetaField(CustomerAccountInfoTypeMetaField metaField, String prefix , String sufix ) {
        if(metaField != null &&
               StringUtils.isNotEmpty(((StringMetaFieldValue) metaField.getMetaFieldValue()).getValue())){
            return new StringBuilder ()
                .append(prefix)
                .append(metaField.getMetaFieldValue().getValue())
                .append(sufix).toString() ;
        }
        return StringUtils.EMPTY;
    }

    private void setDistributelAddress(Integer userId) {
        UserDTO user = new UserBL(userId).getEntity();
        AccountInformationTypeDTO serviceAddressGroupAIT = new AccountInformationTypeDAS().findByName(SpaConstants.SERVICE_ADDRESS_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId());
        Integer groupId = null != serviceAddressGroupAIT ? serviceAddressGroupAIT.getId() : 0;
        CustomerAccountInfoTypeMetaField sameAsCustomerInformationCAITMF = user.getCustomer().getCurrentCustomerAccountInfoTypeMetaField(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, groupId);
        if ( groupId == 0 || (sameAsCustomerInformationCAITMF != null && (Boolean) sameAsCustomerInformationCAITMF.getMetaFieldValue().getValue())) {
            groupId = new AccountInformationTypeDAS().findByName(SpaConstants.CONTACT_INFORMATION_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId()).getId();
        }

        Date currentDate = user.getCustomer().getCurrentEffectiveDateByGroupId(groupId);
        CustomerAccountInfoTypeMetaField streetNumberCAITMF = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_NUMBER, groupId, currentDate);
        CustomerAccountInfoTypeMetaField streetNameCAITMF = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_NAME, groupId, currentDate);
        CustomerAccountInfoTypeMetaField cityCAITMF = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.CITY, groupId, currentDate);
        CustomerAccountInfoTypeMetaField provinceCAITMF = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.PROVINCE, groupId, currentDate);
        CustomerAccountInfoTypeMetaField postalCodeCAITMF = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.POSTAL_CODE, groupId, currentDate);
        CustomerAccountInfoTypeMetaField streetTypeCAITMF = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_TYPE, groupId, currentDate);
        CustomerAccountInfoTypeMetaField streetDirectionCAITMF = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_DIRECTION, groupId, currentDate);
        CustomerAccountInfoTypeMetaField streetAptSuiteCAITMF = user.getCustomer().getCustomerAccountInfoTypeMetaField(SpaConstants.STREET_APT_SUITE, groupId, currentDate);

        String serviceAddress = null;
        if (!StringUtils.isEmpty(validateAndGetMetaField(provinceCAITMF, StringUtils.EMPTY, StringUtils.EMPTY)) && !"QC".equals(provinceCAITMF.getMetaFieldValue().getValue())) {
            serviceAddress = String.format(ADDRESS_PATTERN,
                    validateAndGetMetaField(streetAptSuiteCAITMF,StringUtils.EMPTY, HYPHEN),
                    validateAndGetMetaField(streetNumberCAITMF,StringUtils.EMPTY,StringUtils.EMPTY),
                    validateAndGetMetaField(streetNameCAITMF,SPACE,StringUtils.EMPTY),
                    validateAndGetMetaField(streetTypeCAITMF,SPACE,StringUtils.EMPTY),
                    validateAndGetMetaField(streetDirectionCAITMF, SPACE, StringUtils.EMPTY));
        }else {
            serviceAddress = String.format(ADDRESS_PATTERN,
                    validateAndGetMetaField(streetAptSuiteCAITMF, StringUtils.EMPTY, HYPHEN),
                    validateAndGetMetaField(streetNumberCAITMF,StringUtils.EMPTY, StringUtils.EMPTY),
                    validateAndGetMetaField(streetTypeCAITMF,SPACE, StringUtils.EMPTY),
                    validateAndGetMetaField(streetNameCAITMF, SPACE, StringUtils.EMPTY ),
                    validateAndGetMetaField(streetDirectionCAITMF,SPACE, StringUtils.EMPTY));
        }
        commonFields.setServiceAddress(serviceAddress);
        commonFields.setCity(StringUtils.isEmpty(validateAndGetMetaField(cityCAITMF, StringUtils.EMPTY, StringUtils.EMPTY)) ? null
                : validateAndGetMetaField(cityCAITMF, StringUtils.EMPTY, StringUtils.EMPTY) );
        commonFields.setProvince(StringUtils.isEmpty(validateAndGetMetaField(provinceCAITMF, StringUtils.EMPTY, StringUtils.EMPTY)) ? null
                : validateAndGetMetaField(provinceCAITMF, StringUtils.EMPTY, StringUtils.EMPTY));
    }

    private boolean isLineInclude(OrderLineDTO line){
        ItemTypeDTO itemType = null;
        switch (commonFields.getServiceType()) {
        case DSL:
            itemType = new ItemTypeDAS().findByDescription(entityId, SpaConstants.DSL_ITEM_TYPE_DESCRIPTION);
            break;
        case CABLE_INTERNET:
            itemType = new ItemTypeDAS().findByDescription(entityId, SpaConstants.CABLE_ITEM_TYPE_DESCRIPTION);
            break;
        case HOME_PHONE:
            itemType = new ItemTypeDAS().findByDescription(entityId, SpaConstants.HOME_ITEM_TYPE_DESCRIPTION);
            break;
        default:
            break;
        }
        if(null != itemType && line.getDeleted() == 0){
            return null != line.getItem() ? line.getItem().getItemTypes().contains(itemType) : Boolean.FALSE;
        }
        return false;
    }

    private String getCPEPurchaseDate(AssetDTO asset) {
        AssetAssignmentDTO assetAssignment =  asset.getAssignments().stream()
        .filter(as -> null == as.getEndDatetime())
        .findFirst()
        .orElse(null);
        return null != assetAssignment ? formatter.format(assetAssignment.getStartDatetime()) : null;
    }

    private boolean isHardwareAssetOrLine(AssetDTO asset,OrderLineDTO line){
        ItemTypeDTO itemType = null;
        switch (commonFields.getServiceType()) {
        case DSL:
            itemType = new ItemTypeDAS().findByDescription(entityId, SpaConstants.HARDWARE_DSL_ITEM_TYPE_DESCRIPTION);
            break;
        case CABLE_INTERNET:
            itemType = new ItemTypeDAS().findByDescription(entityId, SpaConstants.HARDWARE_CABLE_ITEM_TYPE_DESCRIPTION);
            break;
        case HOME_PHONE:
            itemType = new ItemTypeDAS().findByDescription(entityId, SpaConstants.HARDWARE_HOME_ITEM_TYPE_DESCRIPTION);
            break;
        default:
            break;
        }
        if(null != asset && null != itemType && null != asset.getItem()
                && null != asset.getItem().getItemTypes()){
            return asset.getItem().getItemTypes().contains(itemType);
        }else if (null != line && null != itemType && line.getDeleted() == 0
                && null != line.getItem() && null != line.getItem().getItemTypes()) {
            return  line.getItem().getItemTypes().contains(itemType);
        }else{
            return false;
        }
    }

    private boolean isMigratedProduct(ItemDTO item) {
        Predicate<ItemTypeDTO> p1 = it -> it.getId() == SpaConstants.MIGRATION_PRODUCT_CATEGORY;
        return item.getItemTypes().stream().anyMatch(p1);
    }
}
