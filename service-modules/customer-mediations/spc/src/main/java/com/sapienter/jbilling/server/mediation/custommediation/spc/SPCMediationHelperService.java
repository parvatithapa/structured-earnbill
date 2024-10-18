package com.sapienter.jbilling.server.mediation.custommediation.spc;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public interface SPCMediationHelperService {
    boolean isIdentifierPresent(String number);
    Optional<Integer> getUserIdForIncomingCall(String dataTableName, Integer trunkGroupId);
    Map<String, Integer> getUserIdForAssetIdentifier(String identifier);
    Map<String, String> getMetaFieldsForEntity(Integer entityId);
    Optional<Integer> getProductIdByIdentifier(String assetIdentifier);
    Optional<BigDecimal> getRatingUnitIncrementQuantityByItemId(Integer itemId);
    Optional<Integer> getItemIdByUserAndCdrType(String dataTableName, Integer userId, String cdrType);
    boolean isAccountTypeAvailable(String dataTableName, Integer userId);
    Locale getLocaleForEntity(Integer entityId);
    boolean isCountryPresentForItemAndCountry(String itemId, Date eventDate, String countryCode, String countryName);
    boolean isCarrierNamePresentForItemAndCountry(String itemId, Date eventDate, String carrierName, String countryName);
    Map<String, String> getProductCodeFromRouteRateCard(Integer userId, String assetIdentifier, String codeString);
    Optional<Integer> getItemIdByProductCode(String productCode);
    boolean isItemPresentForId(Integer itemId);
    String getCompanyLevelTimeZoneOffSet(Integer entityId);
    Date getCompanyCurrentDate(Integer entityId);
    void notifyUserForJMR(JbillingMediationRecord jmr);
    Optional<Integer> findOrderByAssetNumber(String number);
    String getCompanyLevelTimeZone(Integer entityId);
    Map<String, Integer> getUserIdByCustomerMetaField(String metaFieldValue, String metaFieldName, Integer entityId);
    QuantityResolutionContext constructQuantityResolutionContextForCodeString(Integer userId, String assetIdentifier, String codeString);
    Map<String, String> getInternetItemIdWithQuantityUnit(Integer userId, Integer entityId, String metaFieldName, Integer planId);
    Integer getPlanId(Integer userId, String assetIdentifier);
}
