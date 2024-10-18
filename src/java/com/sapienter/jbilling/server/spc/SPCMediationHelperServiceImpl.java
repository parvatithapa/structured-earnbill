package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.QuantityResolutionContext;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.OptusMurJMREvent;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDAS;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Constants;

@Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
public class SPCMediationHelperServiceImpl  implements SPCMediationHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FIND_ASSET_SQL = "SELECT id FROM asset WHERE identifier = ?";

    public static final String FIND_USER_BY_IDENTIFIER =
            "SELECT id, currency_id FROM base_user WHERE id = "
                    + "(SELECT user_id FROM purchase_order WHERE id = "
                    + "(SELECT order_id FROM order_line WHERE id = "
                    + "(SELECT order_line_id FROM asset WHERE identifier = ? "
                    + " AND deleted = 0) "
                    + " AND deleted = 0) "
                    + " AND deleted = 0) "
                    + " AND deleted = 0 ";

    private static final String CHECK_ITEM_SQL = "SELECT id FROM item WHERE id = ? AND deleted = 0";
    /**
     * route_id is code_string which is coming from field set in mediation step
     */
    public static final String FIND_ROUTE_TABLE_BY_IDENTIFIER = "SELECT tariff_code,name FROM %s WHERE upper(route_id) = ?";

    public static final String GET_QUANTITY_RESOLUTION_CONTEXT_SQL = "SELECT initial_increment, subsequent_increment FROM %s WHERE upper(route_id) = ?";

    private static final String FIND_CARRIER_USER_SQL = "SELECT jbilling_user_id FROM %s WHERE trunk_group_id = ?";

    private static final String FIND_ITEM_ID_SQL = "SELECT item_id FROM %s WHERE account_type_id = ? AND cdr_type = ?";
    private static final String FIND_ITEM_ID = "SELECT id from item where internal_number = ? and deleted = 0";

    private static final String FIND_ACCOUNT_TYPE_SQL = "SELECT COUNT(*) FROM %s WHERE account_type_id = ?";

    private static final String FIND_CARRIER_NAME_SQL = "SELECT DISTINCT(carrier_name) FROM %s WHERE destination = ? AND carrier_name = ?";

    private static final String FIND_COUNTRY_NAME_SQL = "SELECT DISTINCT(destination) FROM %s WHERE destination = ? AND route_id = ?";

    private static final String USER_ID_REQUIRED = "Provide user id!";

    private static final String FIND_ORDER_BY_ASSET_NUMBER_SQL =
            "SELECT id "
                    + "FROM  purchase_order "
                    + "WHERE id = (SELECT order_id "
                    + "              FROM order_line "
                    + "             WHERE id = (SELECT  order_line_id "
                    + "                           FROM  asset "
                    + "                          WHERE identifier = ?))";

    private static final String FIND_RATING_UNIT_INCREMENT_QTY_SQL =
            "SELECT ru.increment_unit_quantity "
                    + "FROM item itm, item_rating_configuration_map ircm, rating_configuration rc, rating_unit ru "
                    + "WHERE itm.id = ircm.item_id AND ircm.rating_configuration_id = rc.id AND rc.rating_unit = ru.id AND itm.id = ?";

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

    private PlanDTO getPlanFromUserId(Integer userId, String assetIdentifier) {
        OrderDTO assetOrder = new OrderDAS().findOrderByUserAndAssetIdentifier(userId, assetIdentifier);
        if (assetOrder == null) {
            logger.debug("Order resolution is failed for asset number {}", assetIdentifier);
            throw new SessionInternalError("Asset resolution is failed");
        }
        Optional<OrderLineDTO> orderLine = assetOrder.getLines().stream()
                .filter(line -> line.getItem().isPlan())
                .findFirst();
        Integer itemId = null;
        if (orderLine.isPresent()) {
            itemId = orderLine.get().getItemId();
        } else if (assetOrder.getParentOrder() != null &&
                assetOrder.getParentOrder().getPeriodId() != Constants.ORDER_PERIOD_ONCE) {
            itemId = assetOrder.getParentOrder().getLines().stream()
                    .filter(line -> line.getItem().isPlan())
                    .findFirst().get().getItemId();
        }
        if (itemId == null) {
            logger.debug("Plan resolution is failed");
            throw new SessionInternalError("Plan resolution is failed");
        }
        return new PlanDAS().findPlanByItemId(itemId);
    }

    @Override
    public Map<String, String> getProductCodeFromRouteRateCard(Integer userId, String assetIdentifier, String codeString){
        PlanDTO planDto = getPlanFromUserId(userId, assetIdentifier);
        String routeTableName = (String) planDto.getMetaField(SPCConstants.PLAN_RATING).getValue();
        if (StringUtils.isBlank(routeTableName)) {
            logger.debug("Plan does not have route rate card configured");
            throw new SessionInternalError("Route rate card table name resolution is failed");
        }
        RouteRateCardDTO rateCardDTO = new RouteRateCardDAS().getRouteRateCardByNameAndEntity(routeTableName, new UserDAS().find(userId).getEntity().getId());
        String tableName = rateCardDTO.getTableName();
        String sqlQuery = String.format(FIND_ROUTE_TABLE_BY_IDENTIFIER, tableName);

        Map<String, String> tariffInfo = new HashMap<>();
        SqlRowSet rs = jdbcTemplate.queryForRowSet(sqlQuery, codeString.toUpperCase());
        if (rs.next()) {
            tariffInfo.put(SPCConstants.TARIFF_CODE, rs.getString("tariff_code"));
            tariffInfo.put(SPCConstants.PRODUCT_CODE, rs.getString("name"));
        }
        return tariffInfo;
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
    public Optional<BigDecimal> getRatingUnitIncrementQuantityByItemId(Integer itemId) {
        try {
            Assert.notNull(itemId, "Provide Item id");
            BigDecimal ratingUnitIncrementQty =
                    jdbcTemplate.queryForObject(FIND_RATING_UNIT_INCREMENT_QTY_SQL, BigDecimal.class, itemId);
            logger.debug("Rating unit increment quantity [{}] for product id [{}] ", ratingUnitIncrementQty, itemId);
            return Optional.of(ratingUnitIncrementQty);
        } catch(IncorrectResultSizeDataAccessException ex) {
            logger.error("Failed to find rating unit increment quantity", ex);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Integer> getItemIdByUserAndCdrType(String dataTableName, Integer userId, String cdrType) {
        try {
            Assert.hasLength(cdrType, "Provide cdr type!");
            Assert.notNull(userId, USER_ID_REQUIRED);
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
    public Optional<Integer> getItemIdByProductCode(String productCode) {
        try {
            Assert.notNull(productCode, "Provide Product Code!");
            Integer itemId = Integer.valueOf(jdbcTemplate.queryForObject(FIND_ITEM_ID,String.class, productCode));
            logger.debug("Item Id [{}] for prodcut code [{}] ", itemId, productCode);
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
            Assert.notNull(userId, USER_ID_REQUIRED);
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

    private Optional<String> getRouteRateCardForItem(String itemId, Date eventDate) {
        PriceModelDTO priceModelDTO = new ItemDAS().find(Integer.valueOf(itemId)).getPrice(null != eventDate ? eventDate : new Date());
        logger.debug("PriceModel : {}", priceModelDTO.toString());
        if (null != priceModelDTO && PriceModelStrategy.ROUTE_BASED_RATE_CARD.equals(priceModelDTO.getType())) {
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
    public String getCompanyLevelTimeZoneOffSet(Integer entityId) {
        return TimezoneHelper.getOffsetFromTimeZone(TimezoneHelper.getCompanyLevelTimeZone(entityId));
    }

    @Override
    public Date getCompanyCurrentDate(Integer entityId) {
        return TimezoneHelper.companyCurrentDate(entityId);
    }

    @Override
    public String getCompanyLevelTimeZone(Integer entityId) {
        return TimezoneHelper.getCompanyLevelTimeZone(entityId);
    }

    @Override
    public boolean isItemPresentForId(Integer itemId) {
        Assert.notNull(itemId, "Provide item id");
        try {
            return jdbcTemplate.queryForObject(CHECK_ITEM_SQL, Integer.class, itemId)!=null;
        } catch(IncorrectResultSizeDataAccessException ex) {
            logger.error("Failed to reterived asset!", ex);
            return false;
        }
    }

    @Override
    public void notifyUserForJMR(JbillingMediationRecord jmr) {
        EventManager.process(new OptusMurJMREvent(jmr));
    }

    @Override
    public Optional<Integer> findOrderByAssetNumber(String number) {
        Assert.notNull(number, "Provide Asset Identifier");
        try {
            return Optional.of(jdbcTemplate.queryForObject(FIND_ORDER_BY_ASSET_NUMBER_SQL, Integer.class, number));
        } catch(IncorrectResultSizeDataAccessException ex) {
            logger.error("Failed to reterived order id!", ex);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Integer> getUserIdByCustomerMetaField(String metaFieldValue,String metaFieldName, Integer entityId) {
        Map<String, Integer> userMap = new HashMap<>();

        UserBL userBl = new UserBL();
        List<CustomerDTO> customerList = userBl.getUserByCustomerMetaField(metaFieldValue, metaFieldName, entityId);
        CustomerDTO customer = null != customerList ?
                    customerList.stream().filter(customerDTO ->
                    (!customerList.isEmpty())).findFirst().orElse(null) : null;

        if (null != customer) {
            userMap.put(MediationStepResult.USER_ID, customer.getBaseUser().getId());
            userMap.put(MediationStepResult.CURRENCY_ID, customer.getBaseUser().getCurrencyId());
        }

        return userMap;
    }

    @Override
    public QuantityResolutionContext constructQuantityResolutionContextForCodeString(Integer userId, String assetIdentifier, String codeString) {
        Assert.notNull(userId, "Please provide user id");
        Assert.notNull(assetIdentifier, "Please provide asset identifier");
        Assert.notNull(codeString, "Please provide asset code string");
        PlanDTO planDto = getPlanFromUserId(userId, assetIdentifier);
        String routeName = (String) planDto.getMetaField(SPCConstants.PLAN_RATING).getValue();
        if (StringUtils.isBlank(routeName)) {
            logger.debug("Plan does not have route rate card configured");
            throw new SessionInternalError("Route rate card table name resolution failed!");
        }
        RouteRateCardDTO rateCardDTO = new RouteRateCardDAS().getRouteRateCardByNameAndEntity(routeName, new UserDAS().find(userId).getEntity().getId());
        if(null == rateCardDTO) {
            logger.error("No route rate card found for name {}", routeName);
            throw new SessionInternalError("Route Base Rate card not found for "+ routeName);
        }
        String tableName = rateCardDTO.getTableName();
        String sqlQuery = String.format(GET_QUANTITY_RESOLUTION_CONTEXT_SQL, tableName);
        SqlRowSet rs = jdbcTemplate.queryForRowSet(sqlQuery, codeString.toUpperCase());
        if (rs.next()) {
            BigDecimal initialIncrement = new BigDecimal(rs.getString("initial_increment"));
            BigDecimal subsequentIncrement = new BigDecimal(rs.getString("subsequent_increment"));
            return new QuantityResolutionContext(initialIncrement, subsequentIncrement, BigDecimal.ROUND_UP, BigDecimal.ROUND_UP);
        }
        throw new SessionInternalError("no quantity resolution context found for code string " + codeString + " in route rate card " + tableName);
    }

    @Override
    public Map<String, String> getInternetItemIdWithQuantityUnit(Integer userId, Integer entityId, String metaFieldName, Integer planId) {
        Assert.notNull(userId, "Please provide user id");
        Assert.notNull(userId, "Please provide meta field name");
        Map<String, String> itemResolutionMap = new HashMap<>();
        String usageItemId = new MetaFieldDAS().getComapanyLevelMetaFieldValue(metaFieldName, entityId);
        if (!StringUtils.isNumeric(usageItemId)) {
            logger.debug("Either company level meta-field '{}' is not defined OR configured item id {} is invalid", metaFieldName, usageItemId);
            throw new SessionInternalError("Either company level meta-field is not defined OR configured item id is invalid");
        }

        if(planId == null) {
            logger.debug("User {} is not subscribed to any plan having usage item id {}", userId, usageItemId);
            throw new SessionInternalError("User does not have subscrition plan");
        }

        PlanDTO planDto = new PlanDAS().findPlanById(planId);
        if(!planDto.getPlanItems().stream().anyMatch(pi -> pi.getItem().getId() == Integer.valueOf(usageItemId))) {
            logger.debug("User {} is not subscribed to any plan having usage item id {}", userId, usageItemId);
            throw new SessionInternalError("User does not have subscrition plan");
        }

        MetaFieldValue<String> internetTechMF = planDto.getMetaField(SPCConstants.PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE);
        if (internetTechMF == null || StringUtils.isBlank(internetTechMF.getValue())) {
            logger.debug("Internet Technology is not configured for Plan {}", planId);
            throw new SessionInternalError("Internet Technology is not configured for Plan");
        }
        MetaFieldValue<String> quantityUnitMF = planDto.getMetaField(SPCConstants.PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT);
        if (quantityUnitMF == null || StringUtils.isBlank(quantityUnitMF.getValue())) {
            logger.debug("Chargeable unit of internet quantity is not configured for Plan {}", planId);
            throw new SessionInternalError("Chargeable unit of internet quantity is not configured for Plan");
        }

        itemResolutionMap.put(SPCConstants.INTERNET_USAGE_ITEM_ID, usageItemId);
        itemResolutionMap.put(SPCConstants.INTERNET_TECHNOLOGY_TYPE, internetTechMF.getValue());
        itemResolutionMap.put(SPCConstants.INTERNET_USAGE_CHARGEABLE_UNIT, quantityUnitMF.getValue());
        return itemResolutionMap;
    }

    public Integer getPlanId(Integer userId, String assetIdentifier) {
        PlanDTO planDto = getPlanFromUserId(userId, assetIdentifier);
        return null != planDto ? planDto.getId(): null;
    }
}
