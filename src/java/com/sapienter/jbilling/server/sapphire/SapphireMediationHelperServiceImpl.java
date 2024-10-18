package com.sapienter.jbilling.server.sapphire;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationHelperService;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

@Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
public class SapphireMediationHelperServiceImpl implements SapphireMediationHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FIND_ASSET_SQL = "SELECT id FROM asset WHERE identifier = ?";

    private static final String FIND_USER_BY_IDENTIFIER =
            "SELECT id, currency_id FROM base_user WHERE id = "
                    + "(SELECT user_id FROM purchase_order WHERE id = "
                    + "(SELECT order_id FROM order_line WHERE id = "
                    + "(SELECT order_line_id FROM asset WHERE identifier = ? "
                    + " AND deleted = 0) "
                    + " AND deleted = 0) "
                    + " AND deleted = 0) "
                    + " AND deleted = 0 ";

    private static final String FIND_CARRIER_USER_SQL = "SELECT jbilling_user_id FROM %s WHERE trunk_group_id = ?";

    private static final String FIND_ITEM_ID_SQL = "SELECT item_id FROM %s WHERE account_type_id = ? AND cdr_type = ?";

    private static final String FIND_ACCOUNT_TYPE_SQL = "SELECT COUNT(*) FROM %s WHERE account_type_id = ?";

    private static final String FIND_CARRIER_NAME_SQL = "SELECT DISTINCT(carrier_name) FROM %s WHERE destination = ? AND carrier_name = ?";

    private static final String FIND_COUNTRY_NAME_SQL = "SELECT DISTINCT(destination) FROM %s WHERE destination = ? AND route_id = ?";

    @Resource(name = "jBillingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public boolean isIdentifierPresent(String number) {
        Assert.hasLength(number, "Please Provide Number!");
        Optional<Integer> assetId = findAssetByIdentifier(number);
        if(assetId.isPresent()) {
            logger.debug("Asset {} found for Number {}", assetId, number);
        }
        return assetId.isPresent();
    }

    private Optional<Integer> findAssetByIdentifier(String identifier) {
        try {
            return Optional.of(jdbcTemplate.queryForObject(FIND_ASSET_SQL, Integer.class, identifier));
        } catch(IncorrectResultSizeDataAccessException ex) {
            logger.error("Failed to reterived asset!", ex);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> getUserIdForIncomingCall(String dataTableName, Integer trunkGroupId) {
        Assert.hasLength(dataTableName, "Please Provide Table Name!");
        Assert.notNull(trunkGroupId, "Please Provide Trunk Group Id!");
        String findSQL = String.format(FIND_CARRIER_USER_SQL, dataTableName);
        try {
            Optional<Integer> userId = Optional.of(Integer.valueOf(jdbcTemplate.queryForObject(findSQL, String.class, trunkGroupId.toString())));
            logger.debug("Usre Id {} for incoming call from table {} for trunk group id {}", userId, dataTableName, trunkGroupId);
            return userId;
        } catch(IncorrectResultSizeDataAccessException ex) {
            logger.error("Failed to getUserIdForIncomingCall!", ex);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Integer> getUserIdForAssetIdentifier(String identifier) {
        Assert.hasLength(identifier, "Please Provide Identifier!");
        Map<String, Integer> userMap = new HashMap<>();
        SqlRowSet rs = jdbcTemplate.queryForRowSet(FIND_USER_BY_IDENTIFIER, identifier);
        if (rs.next()) {
            userMap.put(MediationStepResult.USER_ID, rs.getInt("id"));
            userMap.put(MediationStepResult.CURRENCY_ID, rs.getInt("currency_id"));
        }
        return userMap;
    }

    @Override
    public Map<String, String> getMetaFieldsForEntity(Integer entityId) {
        Map<String, String> result = new HashMap<>();
        CompanyDTO company = new CompanyDAS().find(entityId);
        for(@SuppressWarnings("rawtypes") MetaFieldValue value : company.getMetaFields()) {
            Object metaFieldValue = value.getValue();
            result.put(value.getField().getName(), Objects.nonNull(metaFieldValue) ? metaFieldValue.toString() : "");
        }
        return result;
    }

    @Override
    public Optional<Integer> getProductIdByIdentifier(String assetIdentifier) {
        Optional<Integer> assetId = findAssetByIdentifier(assetIdentifier);
        if(!assetId.isPresent()) {
            return Optional.empty();
        }
        logger.debug("Asset Id {} for identifier {}", assetId.get(), assetIdentifier);
        AssetDTO asset = new AssetDAS().find(assetId.get());
        return Optional.of(asset.getItem().getId());
    }

    @Override
    public Integer getCurrencyIdForUser(Integer userId) {
        Assert.notNull(userId, "Provide user id!");
        UserDTO user = new UserDAS().find(userId);
        return user.getCurrencyId();
    }

    @Override
    public Optional<Integer> getItemIdByUserAndCdrType(String dataTableName, Integer userId, String cdrType) {
        try {
            Assert.hasLength(cdrType, "Provide cdr type!");
            Assert.notNull(userId, "Provide user id!");
            Integer accountId = new UserDAS().find(userId).getCustomer().getAccountType().getId();
            String findSQL = String.format(FIND_ITEM_ID_SQL, dataTableName);
            Integer itemId = Integer.valueOf(jdbcTemplate.queryForObject(findSQL, String.class, accountId.toString(), cdrType));
            logger.debug("Item Id [{}] for Cdr Tyep [{}] for account [{}] for user [{}]", itemId, cdrType, accountId, userId);
            return Optional.of(itemId);
        } catch(IncorrectResultSizeDataAccessException ex) {
            logger.error("Failed to find Item id", ex);
            return Optional.empty();
        }
    }

    @Override
    public boolean isAccountTypeAvailable(String dataTableName, Integer userId) {
        try {
            logger.debug("checking if the account type is present or not with userId {}", userId);
            Assert.notNull(userId, "Provide user id!");
            Integer accountId = new UserDAS().find(userId).getCustomer().getAccountType().getId();
            String findSQL = String.format(FIND_ACCOUNT_TYPE_SQL, dataTableName);
            Integer count = jdbcTemplate.queryForObject(findSQL, Integer.class, accountId.toString());
            return count != 0;
        } catch (IncorrectResultSizeDataAccessException e) {
            logger.error("Failed to find count of account type! ", e);
            return false;
        }
    }

    @Override
    public Locale getLocaleForEntity(Integer entityId) {
        Assert.notNull(entityId, "Please Provide entity id!");
        CompanyDTO entity = new CompanyDAS().find(entityId);
        Locale locale = entity.getLanguage().asLocale();
        logger.debug("Reterived local {} for entity {}", locale, entityId);
        return locale;
    }

    @Override
    public boolean isCountryPresentForItemAndCountry(String itemId, Date eventDate, String countryCode, String countryName) {
        try {
            Optional<String> routeRateCardTableName = getRouteRateCardForItem(itemId, eventDate);
            if (routeRateCardTableName.isPresent()) {
                String findSQL = String.format(FIND_COUNTRY_NAME_SQL, routeRateCardTableName.get());
                SqlRowSet rs = jdbcTemplate.queryForRowSet(findSQL, countryName, countryCode);
                if (rs.next()) {
                    logger.debug("found country name : {}", rs.getString("destination"));
                    return true;
                }
            }
        } catch (InvalidResultSetAccessException e) {
            logger.error(e.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public boolean isCarrierNamePresentForItemAndCountry(String itemId, Date eventDate, String carrierName, String countryName) {
        try {
            Optional<String> routeRateCardTableName = getRouteRateCardForItem(itemId, eventDate);
            if (routeRateCardTableName.isPresent()) {
                String findSQL = String.format(FIND_CARRIER_NAME_SQL, routeRateCardTableName.get());
                SqlRowSet rs = jdbcTemplate.queryForRowSet(findSQL, countryName, carrierName);
                if (rs.next()) {
                    logger.debug("found carrier name : {}", rs.getString("carrier_name"));
                    return true;
                }
            }
        } catch (InvalidResultSetAccessException e) {
            logger.error(e.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public Optional<String> getRouteRateCardForItem(String itemId, Date eventDate) {
        PriceModelDTO priceModelDTO = new ItemDAS().find(Integer.valueOf(itemId)).getPrice(null != eventDate ? eventDate : new Date());
        if (null != priceModelDTO && PriceModelStrategy.ROUTE_BASED_RATE_CARD.equals(priceModelDTO.getType())) {
            logger.debug("PriceModel : {}", priceModelDTO);
            Map<String, String> attributes = priceModelDTO.getAttributes();
            Optional<String> rateCardId = Optional.ofNullable(attributes.get("route_rate_card_id"));
            if (rateCardId.isPresent()) {
                logger.debug("found rate card id : {} for itemId : {}", rateCardId, itemId);
                RouteRateCardDTO routeRateCardDTO = new RouteRateCardDAS().findNow(Integer.valueOf(rateCardId.get()));
                return Optional.of(routeRateCardDTO.getTableName());
            }
        }
        logger.debug("No rate card found for given itemId : {}", itemId);
        return Optional.empty();
    }

    @Override
    public String getCompanyTimeZone(Integer entityId) {
        Assert.notNull(entityId, "Please Provide entity id!");
        CompanyDTO entity = new CompanyDAS().find(entityId);
        String timeZone = entity.getTimezone();
        if(StringUtils.isEmpty(timeZone)) {
            timeZone = TimeZone.getDefault().getID();
            logger.debug("using default time zone {}", timeZone);
        }
        logger.debug("Reterived timeZone {} for entity {}", timeZone, entityId);
        return timeZone;
    }
}
