package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.time.ZonedDateTime;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;

public interface SPCMediationHelperService {
    boolean isIdentifierPresent(String number);
    Map<String, Integer> getUserIdForAssetIdentifier(String identifier, Date eventDate);
    Map<String, String> getMetaFieldsForEntity(Integer entityId);
    Optional<Integer> getProductIdByIdentifier(String assetIdentifier);
    Optional<BigDecimal> getRatingUnitIncrementQuantityByItemId(Integer itemId);
    Optional<Integer> getItemIdByUserAndCdrType(String dataTableName, Integer userId, String cdrType);
    boolean isAccountTypeAvailable(String dataTableName, Integer userId);
    Locale getLocaleForEntity(Integer entityId);
    boolean isCountryPresentForItemAndCountry(String itemId, Date eventDate, String countryCode, String countryName);
    boolean isCarrierNamePresentForItemAndCountry(String itemId, Date eventDate, String carrierName, String countryName);
    Map<String, String> getProductCodeFromRouteRateCard(Integer userId, String assetIdentifier, String codeString, Date eventDate);
    Optional<Integer> getItemIdByProductCode(String productCode);
    boolean isItemPresentForId(Integer itemId);
    String getCompanyLevelTimeZoneOffSet(Integer entityId);
    Date getCompanyCurrentDate(Integer entityId);
    void notifyUserForJMRs(List<JbillingMediationRecord> jmrs);
    Optional<Integer> findSubscriptionOrderByUserAssetEventDate(Integer userId, String assetIdentifier, Date eventDate);
    String getCompanyLevelTimeZone(Integer entityId);
    Map<String, Integer> getUserIdByCustomerMetaField(String metaFieldValue, String metaFieldName, Integer entityId);
    QuantityResolutionContext constructQuantityResolutionContextForCodeString(Integer userId, String assetIdentifier, String codeString, Date eventDate);
    Map<String, String> getInternetItemIdWithQuantityUnit(Integer userId, Integer entityId, String metaFieldName, Integer planId);
    Integer getPlanId(Integer userId, String assetIdentifier, Date eventDate);
    Map<String, Date> getActiveSinceAndActiveUntilDates(Integer orderId);
    Date getTimeZonedEventDate(Date eventDate, Integer entityId);
    BigDecimal getAmountWithoutGST(Integer userId, Integer itemId, BigDecimal itemPrice);
    List <String> getCustomerCareContactNumbers(String dataTableName);
    Integer getRecordCountFromRouteRateCard(Integer userId, String assetIdentifier, String codeString, Date eventDate);
    String encryptSensitiveData(Integer hashingMethodId, String data);
    JbillingMediationRecord[] getUnBilledMediationEventsByServiceId(Integer entityId, String serviceId);
    JbillingMediationRecord[] getMediationEventsByServiceIdAndDateRange(Integer entityId, String serviceId, Integer offset, Integer limit, String startDate, String endDate);
}
