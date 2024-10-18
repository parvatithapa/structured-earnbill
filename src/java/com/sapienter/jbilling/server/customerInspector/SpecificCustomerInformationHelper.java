package com.sapienter.jbilling.server.customerInspector;

import com.jayway.jsonpath.internal.function.numeric.Min;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.sapphire.SapphireConstants;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserHelperDisplayerFactory;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDTO;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by pablo123 on 01/03/2017.
 */
public class SpecificCustomerInformationHelper {

    private static final String DISTRIBUTEL_ADDRESS = "Distributel Address";
    private static final String DISTRIBUTEL_HISTORY = "Distributel History";
    private static final String DISTRIBUTEL_USERNAME = "Distributel UserName";
    private static final String DISTRIBUTEL_BANFF_ACCOUNT_ID = "Distributel BanffAccountId";
    private static final String CUSTOMER_STATUS = "Customer Status";
    private static final String CUSTOMER_EDIT = "Customer Edit";
    private static final String CUSTOMER_VIEW = "Customer View";
    private static final String INITIATE_SPA = "Initiate SPA";
    private static final String INVOICE_FULL_LIST = "Invoice Full List";
    private static final String PAYMENT_FULL_LIST = "Payment Full List";
    private static final String space = " ";
    private static final String hyphen = "-";
    private static final String comma = ", ";
    private static final String ADDRESS_PATTERN = "%s%s%s%s%s%s%s%s";
    private static final String SAPPHIRE_ADDRESS_PATTERN = "%s%s%s%s%s";
    private static final String SAPPHIRE_ADDRESS = "Sapphire Address";
    private static final String SAPPHIRE_USERNAME = "Sapphire Username";
    private static final String DATE_OF_BIRTH = "Date of Birth";
    private static final String EMAIL = "EMAIL";
    private static final String PHONE_NUMBER = "Phone Number 1";
    private static final String SAPP_EMAIL = "SAPP_EMAIL";
    private String name;
    private Integer userId;

    public SpecificCustomerInformationHelper(Integer userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Object getValue() {
        switch (name) {
            case DISTRIBUTEL_ADDRESS:
                return getDistributelAddress();
            case DISTRIBUTEL_HISTORY:
                return getDistributelHistory();
            case DISTRIBUTEL_USERNAME:
                return getDistributelUserName();
            case DISTRIBUTEL_BANFF_ACCOUNT_ID:
                return getDistributelBanffAccountId();
            case CUSTOMER_STATUS:
                return getCustomerStatus();
            case CUSTOMER_VIEW:
                return "/customer/list/" + this.userId;
            case CUSTOMER_EDIT:
                return "/customer/edit/" + this.userId;
            case INITIATE_SPA:
                return getInitiateSPA();
            case INVOICE_FULL_LIST:
                return "/invoice/user/" + this.userId;
            case PAYMENT_FULL_LIST:
                return "/payment/user/" + this.userId;
            case SAPPHIRE_ADDRESS:
                return getSapphireAddress();
            case SAPPHIRE_USERNAME:
                return getSapphireUserName();
            case DATE_OF_BIRTH:
                return getSapphireMetafield(SapphireConstants.DATE_OF_BIRTH);
            case EMAIL:
                return getDistributelMetafield(SpaConstants.EMAIL_ADDRESS);
            case SAPP_EMAIL:
                String email = String.valueOf(getSapphireMetafield(SapphireConstants.EMAIL));
                return null != email ? email : StringUtils.EMPTY;
            case PHONE_NUMBER:
                return getDistributelMetafield(SpaConstants.PHONE_NUMBER_1);
        }
        return null;
    }

    private Object getCustomerStatus() {
        return new UserBL(userId).getEntity().getStatus().getDescription();
    }

    private Object getInitiateSPA() {
        UserDTO user = new UserBL(userId).getEntity();
        // Add in SpaConstants a new one for the Initiate SPA redirect operation
        MetaFieldValue initiateSpaUrlMF = user.getCompany().getMetaField(SpaConstants.INITIATE_SPA_URL);
        if (initiateSpaUrlMF == null) {
            return null;
        }
        return initiateSpaUrlMF.getValue() + userId.toString();
    }

    private Object getDistributelHistory() {
        UserDTO user = new UserBL(userId).getEntity();
        MetaFieldValue legacySytemUrlMF = user.getCompany().getMetaField(SpaConstants.LEGACY_SYSTEM_URL);
        if (legacySytemUrlMF == null) {
            return null;
        }
        return legacySytemUrlMF.getValue() + "/" + userId;
    }

    private Object getDistributelAddress() {
        UserDTO user = new UserBL(userId).getEntity();
        AccountInformationTypeDTO serviceAddressGroupAIT = new AccountInformationTypeDAS().findByName(SpaConstants.SERVICE_ADDRESS_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId());
        Integer groupId = serviceAddressGroupAIT.getId();
        CustomerAccountInfoTypeMetaField sameAsCustomerInformationCAITMF = user.getCustomer().getCurrentCustomerAccountInfoTypeMetaField(SpaConstants.SAME_AS_CUSTOMER_INFORMATION, groupId);
        if (sameAsCustomerInformationCAITMF != null && (Boolean) sameAsCustomerInformationCAITMF.getMetaFieldValue().getValue()) {
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

        return (validMetaField(provinceCAITMF) && !"QC".equals(provinceCAITMF.getMetaFieldValue().getValue())) ? 
            String.format(ADDRESS_PATTERN,
                validMetaField(streetAptSuiteCAITMF) ? streetAptSuiteCAITMF.getMetaFieldValue().getValue() + hyphen : StringUtils.EMPTY,
                validMetaField(streetNumberCAITMF) ? streetNumberCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(streetNameCAITMF) ? space + streetNameCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(streetTypeCAITMF) ? space + streetTypeCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(streetDirectionCAITMF) ? space + streetDirectionCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(cityCAITMF) ? comma + cityCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(provinceCAITMF) ? comma + provinceCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(postalCodeCAITMF) ? space + postalCodeCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY) :
            String.format(ADDRESS_PATTERN,
                validMetaField(streetAptSuiteCAITMF) ? streetAptSuiteCAITMF.getMetaFieldValue().getValue() + hyphen : StringUtils.EMPTY,
                validMetaField(streetNumberCAITMF) ? streetNumberCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(streetTypeCAITMF) ? space + streetTypeCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(streetNameCAITMF) ? space + streetNameCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(streetDirectionCAITMF) ? space + streetDirectionCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(cityCAITMF) ? comma + cityCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(provinceCAITMF) ? comma + provinceCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY,
                validMetaField(postalCodeCAITMF) ? space + postalCodeCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY);
    }

    private Object getSapphireAddress() {
        String cityCAITMF = String.valueOf(getSapphireMetafield(SapphireConstants.CITY));
        String postalCodeCAITMF = String.valueOf(getSapphireMetafield(SapphireConstants.POSTAL_CODE));
        String addressNumberCAITMF = String.valueOf(getSapphireMetafield(SapphireConstants.ADDRESS_NUMBER));
        String streetNameCAITMF = String.valueOf(getSapphireMetafield(SapphireConstants.STREET_NAME));
        String countryCAITMF = String.valueOf(getSapphireMetafield(SapphireConstants.COUNTRY));

        return
            String.format(SAPPHIRE_ADDRESS_PATTERN,
                    null != addressNumberCAITMF ? addressNumberCAITMF : StringUtils.EMPTY,
                    null != streetNameCAITMF ? comma + streetNameCAITMF : StringUtils.EMPTY,
                    null != cityCAITMF ? comma + cityCAITMF : StringUtils.EMPTY,
                    null != countryCAITMF ? comma + countryCAITMF : StringUtils.EMPTY,
                    null != postalCodeCAITMF ? comma + postalCodeCAITMF : StringUtils.EMPTY);
    }

    private boolean validMetaField(CustomerAccountInfoTypeMetaField metaField) {
        return metaField != null && 
               StringUtils.isNotEmpty(((StringMetaFieldValue) metaField.getMetaFieldValue()).getValue());
    }

    private Object getDistributelUserName() {
        UserDTO user = new UserBL(userId).getEntity();
        return UserHelperDisplayerFactory.factoryUserHelperDisplayer(user.getCompany().getId()).getDisplayName(user);
    }

    private Set<String> getDistributelBanffAccountId() {
        List<AssetWS> assetWSSet = new AssetBL().getAllAssetsByUserId(userId);
        return assetWSSet.stream()
                        .flatMap(assetWS -> Arrays.stream(assetWS.getMetaFields()))
                        .filter(metaFieldValueWS -> SpaConstants.DOMAIN_ID.equals(metaFieldValueWS.getFieldName()))
                        .map(MetaFieldValueWS::getStringValue)
                        .collect(Collectors.toSet());
    }

    private Object getSapphireUserName() {
        UserDTO user = new UserBL(userId).getEntity();
        return user.getUserName();
    }

    private Object getDistributelMetafield(String metaField) {
        UserDTO user = new UserBL(userId).getEntity();
        Integer groupId = new AccountInformationTypeDAS().findByName(SpaConstants.CONTACT_INFORMATION_AIT, user.getEntity().getId(), user.getCustomer().getAccountType().getId()).getId();
        Date currentDate = user.getCustomer().getCurrentEffectiveDateByGroupId(groupId);
        CustomerAccountInfoTypeMetaField emailMetaField = user.getCustomer().getCustomerAccountInfoTypeMetaField(metaField, groupId, currentDate);
        return validMetaField(emailMetaField) ? emailMetaField.getMetaFieldValue().getValue() :StringUtils.EMPTY;
    }

    private Object getSapphireMetafield(String metaField) {
        UserDTO user = new UserBL(userId).getEntity();
        Integer groupId = new AccountInformationTypeDAS().getAvailableAccountInformationTypes(user.getEntity().getId())
                                                         .stream().filter(ait -> ait.getAccountType().getId() == user.getCustomer().getAccountType().getId())
                                                         .min(Comparator.comparing(AccountInformationTypeDTO :: getDisplayOrder))
                                                         .get().getId();
        Date currentDate = user.getCustomer().getCurrentEffectiveDateByGroupId(groupId);
        CustomerAccountInfoTypeMetaField value = user.getCustomer().getCustomerAccountInfoTypeMetaField(metaField, groupId, currentDate);
        return validMetaField(value) ? (value.getMetaFieldValue().getValue() instanceof char [] ? String.valueOf(((char [])value.getMetaFieldValue().getValue())) : value.getMetaFieldValue().getValue()) : StringUtils.EMPTY;
    }
}
