package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.db.OrderChangeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by pablo_galera on 16/01/17.
 */
public class SpaValidator {
    
    private static final String PHONE_NUMBER_REGEX = "[0-9]+";

    private static final FormatLogger log = new FormatLogger(
            Logger.getLogger(SpaValidator.class));

    private SpaValidator() {

    }

    public static boolean validateMandatory(SpaImportWS spaImportWS) {
        boolean provinceFound = false;
        if (spaImportWS.getProductsOrdered() == null || spaImportWS.getProductsOrdered().isEmpty()) {
            log.error("ProductsOrdered list is empty");
            return false;
        }
        if (!spaImportWS.isUpdateCustomer()) {
            for (SpaAddressWS address : spaImportWS.getAddresses()) {
                if (AddressType.BILLING.equals(AddressType.getByName(address.getAddressType()))) {
                    if (!StringUtils.isEmpty(address.getProvince())) {
                        provinceFound = true;
                    }
                }
            }
            if (!provinceFound) {
                log.error("Province attribute is empty");
                return false;
            }
        }
        return true;
    }

    public static String validateNonMandatoryAndMissingInformation(SpaImportWS spaImportWS) {

        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isEmpty(spaImportWS.getCustomerName())) {
            stringBuilder.append("\nCustomer Name is missing.");
            spaImportWS.setCustomerName("customerName" + System.currentTimeMillis());
        }
        if (StringUtils.isEmpty(spaImportWS.getPhoneNumber1())) {
            stringBuilder.append("\nPhone Number 1 is missing.");
        }
        if (StringUtils.isEmpty(spaImportWS.getEmailAddress())) {
            stringBuilder.append("\nEmail Address is missing.");
            spaImportWS.setEmailAddress("missingemailaddress@enrollment.com");
        }
        if (StringUtils.isEmpty(spaImportWS.getLanguage())) {
            stringBuilder.append("\nLanguage is missing.");
        }
        if (StringUtils.isEmpty(spaImportWS.getStaffIdentifier())) {
            stringBuilder.append("\nStaff Identifier is not applicable.");
        }
        if (StringUtils.isEmpty(spaImportWS.getCustomerCompany())) {
            stringBuilder.append("\nCustomer Company is not applicable.");
        }
        if (StringUtils.isEmpty(spaImportWS.getPhoneNumber2())) {
            stringBuilder.append("\nPhone Number 2 is not applicable.");
        }
        if (spaImportWS.getEmailVerified() == null) {
            stringBuilder.append("\nEmail Verified is not applicable.");
        }
        if (StringUtils.isEmpty(spaImportWS.getTaxExempt())) {
            stringBuilder.append("\nTax Exempt is not applicable.");
        }
        if (StringUtils.isEmpty(spaImportWS.getComments())) {
            stringBuilder.append("\nComments is not applicable.");
        }
        if (StringUtils.isEmpty(spaImportWS.getConfirmationNumber())) {
            stringBuilder.append("\nConfirmation Number is not applicable.");
        }
        if (spaImportWS.getPaymentCredential() == null) {
            stringBuilder.append("\nPayment Credential is not applicable.");
        }
        if (spaImportWS.getPaymentResult() == null) {
            stringBuilder.append("\nPayment Result is not applicable.");
        }

        SpaAddressWS address = spaImportWS.getAddress(AddressType.BILLING);
        if (StringUtils.isEmpty(address.getPostalCode())) {
            stringBuilder.append("\nBilling Address Postal Code is missing.");
        }
        if (address.getStreetNumber() == null) {
            stringBuilder.append("\nBilling Address Street Number is missing.");
        }
        if (StringUtils.isEmpty(address.getStreetName())) {
            stringBuilder.append("\nBilling Address Street Name is missing.");
        }
        if (StringUtils.isEmpty(address.getStreetType())) {
            stringBuilder.append("\nBilling Address Street Type is missing.");
        }
        if (StringUtils.isEmpty(address.getCity())) {
            stringBuilder.append("\nBilling Address City is missing.");
        }
        if (StringUtils.isEmpty(address.getStreetNumberSufix())) {
            stringBuilder.append("\nBilling Address Street Number Suffix is not applicable.");
        }
        if (StringUtils.isEmpty(address.getStreetAptSuite())) {
            stringBuilder.append("\nBilling Address Apt/Suite is not applicable.");
        }
        if (StringUtils.isEmpty(address.getStreetDirecton())) {
            stringBuilder.append("\nBilling Address Street Direction is not applicable.");
        }
        return stringBuilder.toString();
    }

    public static boolean hasToRecordPayment(SpaImportWS spaImportWS) {
        SpaPaymentCredentialWS spaPaymentCredentialWS=spaImportWS.getPaymentCredential();
        SpaPaymentResultWS spaPaymentResultWS =spaImportWS.getPaymentResult();
        if (spaPaymentCredentialWS == null || spaPaymentResultWS == null) {
            return false;
        }
        if(spaPaymentCredentialWS.hasInvalidFields() || spaPaymentResultWS.hasInvalidFields() ){
            return false;
        }
        return true;
    }

    public static boolean hasToSendNotification(SpaImportWS spaImportWS) {
        return !StringUtils.isEmpty(spaImportWS.getEmailAddress());
    }

    public static boolean validateVOIPPhoneNumber(SpaImportWS spaImportWS, Integer entityId) {
        return validateVOIPPhoneNumber(spaImportWS, entityId, null);
    }

    public static boolean validateVOIPPhoneNumber(SpaImportWS spaImportWS, Integer entityId, Integer evaluatedAssetId) {

        for (SpaProductsOrderedWS spaProductsOrderedWS : spaImportWS.getProductsOrdered()) {
            String phoneNumber = spaProductsOrderedWS.getPhoneNumber();
            if (!validateVOIPPhoneNumber(phoneNumber, entityId, spaProductsOrderedWS.getStartDate(), evaluatedAssetId)) {
                return false;
            }
        }
        return true;
    }

    public static boolean validateVOIPPhoneNumber(String phoneNumber, Integer entityId, Date startDate, Integer evaluatedAssetId) {
        if (!StringUtils.isEmpty(phoneNumber) && phoneNumber.matches(PHONE_NUMBER_REGEX)) {
            AssetDAS assetDAS = new AssetDAS();
            List<AssetWS> assets = assetDAS.findAssetByMetaFieldValue(entityId, SpaConstants.MF_PHONE_NUMBER, phoneNumber).stream()
                    .map(AssetBL::getWS)
                    .collect(Collectors.toList());

            for (AssetWS asset : assets) {
                Integer assetId = asset.getId();
                if (!assetId.equals(evaluatedAssetId)) {
                    OrderChangeDAS orderChangeDAS = new OrderChangeDAS();
                    List<OrderDTO> orderList = new ArrayList<>();

                    OrderChangeDTO orderChange = orderChangeDAS.findByOrderChangeByAssetIdInPlanItems(assetId);
                    if (orderChange != null) {
                        orderList.add(orderChange.getOrder());
                    } else {
                        orderChangeDAS.findOrderChangeIdsByAssetId(assetId).forEach(orderChangeId -> {
                            orderList.add(orderChangeDAS.find(orderChangeId).getOrder());
                        });
                    }

                    if (orderList.stream()
                            .anyMatch(order -> order.getActiveUntil() == null || order.getActiveUntil().after(startDate))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void validateDistributelAsset(AssetWS asset) {
        String mfVoIPPhoneNumber = "";
        if (asset.getMetaFields() != null) {
            for (MetaFieldValueWS mfValue : asset.getMetaFields()) {
                if (SpaConstants.MF_PHONE_NUMBER.equals(mfValue.getMetaField().getName())) {
                    mfVoIPPhoneNumber = mfValue.getStringValue();
                }
            }
        }

        if (!StringUtils.isEmpty(mfVoIPPhoneNumber) && !SpaValidator.validateVOIPPhoneNumber(mfVoIPPhoneNumber, asset.getEntityId(), 
                TimezoneHelper.companyCurrentDate(asset.getEntityId()), asset.getId())) {
            throw new SessionInternalError("The VoIP Phone Number for this asset already exists for another active asset",
                    new String[]{"AssetWS,phoneNumber,validation.asset.phonenumber.exists"});
        }
    }

    public static boolean validateServiceAssetIdentifier(SpaImportWS spaImportWS) {
        Set<String> withoutDuplicate = new HashSet<>();
        List<String> withDuplicate = new ArrayList<>();
        for (SpaProductsOrderedWS product : spaImportWS.getProductsOrdered()) {
            if(StringUtils.isNotBlank(product.getServiceAssetIdentifier())){
                withDuplicate.add(product.getServiceAssetIdentifier());
                withoutDuplicate.add(product.getServiceAssetIdentifier());
            }
        }
        return (withDuplicate.size() == withoutDuplicate.size() && isServiceAssetIdentifierExist(spaImportWS.getProductsOrdered()));
    }

    public static boolean validateModemAssetIdentifier(SpaImportWS spaImportWS) {
        Set<String> withoutDuplicate = new HashSet<>();
        List<String> withDuplicate = new ArrayList<>();
        for (SpaProductsOrderedWS product : spaImportWS.getProductsOrdered()) {
            if(StringUtils.isNotBlank(product.getModemAssetIdentifier())){
                withDuplicate.add(product.getModemAssetIdentifier());
                withoutDuplicate.add(product.getModemAssetIdentifier());
            }
        }
        return (withDuplicate.size() == withoutDuplicate.size() && isModemAssetIdentifierExist(spaImportWS.getProductsOrdered()));
    }

    private static boolean isServiceAssetIdentifierExist(List<SpaProductsOrderedWS> productsOrderedWSs){
        for (SpaProductsOrderedWS spaProductsOrderedWS : productsOrderedWSs) {
            if(StringUtils.isNotBlank(spaProductsOrderedWS.getServiceAssetIdentifier()) && new AssetBL().isAssetIdentifierExist(spaProductsOrderedWS.getServiceAssetIdentifier())){
                return false;
            }
        }
        return true;
    }

    private static boolean isModemAssetIdentifierExist(List<SpaProductsOrderedWS> productsOrderedWSs){
        for (SpaProductsOrderedWS spaProductsOrderedWS : productsOrderedWSs) {
            if(StringUtils.isNotBlank(spaProductsOrderedWS.getModemAssetIdentifier()) && new AssetBL().isAssetIdentifierExist(spaProductsOrderedWS.getModemAssetIdentifier())){
                return false;
            }
        }
        return true;
    }
}
