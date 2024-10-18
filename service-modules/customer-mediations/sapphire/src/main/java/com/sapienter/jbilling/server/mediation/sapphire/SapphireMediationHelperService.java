package com.sapienter.jbilling.server.mediation.sapphire;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public interface SapphireMediationHelperService {
    boolean isIdentifierPresent(String number);
    Optional<Integer> getUserIdForIncomingCall(String dataTableName, Integer trunkGroupId);
    Map<String, Integer> getUserIdForAssetIdentifier(String identifier);
    Map<String, String> getMetaFieldsForEntity(Integer entityId);
    Optional<Integer> getProductIdByIdentifier(String assetIdentifier);
    Integer getCurrencyIdForUser(Integer userId);
    Optional<Integer> getItemIdByUserAndCdrType(String dataTableName, Integer userId, String cdrType);
    boolean isAccountTypeAvailable(String dataTableName, Integer userId);
    Locale getLocaleForEntity(Integer entityId);
    boolean isCountryPresentForItemAndCountry(String itemId, Date eventDate, String countryCode, String countryName);
    boolean isCarrierNamePresentForItemAndCountry(String itemId, Date eventDate, String carrierName, String countryName);
    Optional<String> getRouteRateCardForItem(String itemId, Date eventDate);
    String getCompanyTimeZone(Integer entityId);
}
