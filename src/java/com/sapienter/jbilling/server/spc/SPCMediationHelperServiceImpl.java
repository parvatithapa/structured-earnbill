package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
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
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.custommediation.spc.NoCache;
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
import com.sapienter.jbilling.server.security.JBCrypto;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.common.CommonConstants;

import org.apache.http.HttpStatus;

@Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
public class SPCMediationHelperServiceImpl  implements SPCMediationHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String FIND_ASSET_SQL = "SELECT id FROM asset WHERE identifier = ?";

    private static final String FIND_USER_BY_IDENTIFIER_AND_EFFECTIVE_DATE =
            "SELECT bu.id, bu.currency_id  "
                    + "FROM purchase_order po "
                    + "INNER JOIN order_line ol ON ol.order_id = po.id "
                    + "INNER JOIN base_user bu ON bu.id = po.user_id "
                    + "INNER JOIN asset_assignment aa ON aa.order_line_id = ol.id "
                    + "INNER JOIN asset a ON a.id = aa.asset_id "
                    + "WHERE a.identifier = ? "
                    + "AND CASE WHEN aa.end_datetime IS NULL THEN (SELECT MAX(start_datetime) FROM asset_assignment WHERE end_datetime IS NULL AND asset_id = a.id) <= ? "
                    + "ELSE (? BETWEEN aa.start_datetime AND aa.end_datetime) END";
    

    private static final String CHECK_ITEM_SQL = "SELECT id FROM item WHERE id = ? AND deleted = 0";
    /**
     * route_id is code_string which is coming from field set in mediation step
     */
    public static final String FIND_ROUTE_TABLE_BY_IDENTIFIER = "SELECT tariff_code,name FROM %s WHERE upper(route_id) = ?";

    public static final String GET_QUANTITY_RESOLUTION_CONTEXT_SQL = "SELECT initial_increment, subsequent_increment FROM %s WHERE upper(route_id) = ?";

    private static final String FIND_ITEM_ID_SQL = "SELECT item_id FROM %s WHERE account_type_id = ? AND cdr_type = ?";
    private static final String FIND_ITEM_ID = "SELECT id from item where internal_number = ? and deleted = 0";

    private static final String FIND_ACCOUNT_TYPE_SQL = "SELECT COUNT(*) FROM %s WHERE account_type_id = ?";

    private static final String FIND_CARRIER_NAME_SQL = "SELECT DISTINCT(carrier_name) FROM %s WHERE destination = ? AND carrier_name = ?";

    private static final String FIND_COUNTRY_NAME_SQL = "SELECT DISTINCT(destination) FROM %s WHERE destination = ? AND route_id = ?";

    private static final String FIND_SPC_CC_SQL = "SELECT  DISTINCT(calltozero)  FROM %s";

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
    private static final String FIND_TAX_RATE = "SELECT tax_rate FROM %s WHERE description = ?" ;

    @Resource(name = "jBillingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private JMErrorRepository errorRepository;
    @Autowired
    private JMRRepository jmrRepository;
    @Autowired
    MediationService mediationService;

    @Override
    public boolean isIdentifierPresent(String identifier) {
        try {
            logger.debug("billableNumber identifier::: {}", identifier);
            Integer assetId = jdbcTemplate.queryForObject(FIND_ASSET_SQL, Integer.class, identifier);
            logger.debug("billableNumber assetId::: {}", assetId);
            return (null != assetId);
        } catch(IncorrectResultSizeDataAccessException ex) {
            logger.error("Failed to reterived asset!", ex);
            return false;
        }
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
    public Map<String, Integer> getUserIdForAssetIdentifier(String identifier, Date eventDate) {
        Assert.hasLength(identifier, "Please Provide Identifier!");
        Map<String, Integer> userMap = new HashMap<>();
        SqlRowSet rs = jdbcTemplate.queryForRowSet(FIND_USER_BY_IDENTIFIER_AND_EFFECTIVE_DATE, identifier, eventDate, eventDate);
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

    private PlanDTO getPlanFromUserId(Integer userId, String assetIdentifier, Date eventDate) {
    	 logger.debug("getPlanFromUserId userId {}", userId);
    	 logger.debug("getPlanFromUserId assetIdentifier {}", assetIdentifier);
    	 logger.debug("getPlanFromUserId eventDate {}", eventDate);
        OrderDTO planOrder = new OrderDAS().findOrderByUserAssetIdentifierEffectiveDate(userId, assetIdentifier, eventDate);
        if (planOrder == null) {
            logger.debug("Order resolution is failed for asset number {}", assetIdentifier);
            throw new SessionInternalError("Asset resolution is failed");
        }
        
        PlanDTO chargingPlan = planOrder.getPlanFromOrder();
        
        if (chargingPlan == null && 
        		planOrder.getParentOrder() != null &&
                planOrder.getParentOrder().getPeriodId() != Constants.ORDER_PERIOD_ONCE) {
        	chargingPlan = planOrder.getParentOrder().getPlanFromOrder();
        }
        
        if (chargingPlan == null) {
            logger.debug("Plan resolution is failed");
            throw new SessionInternalError("Plan resolution is failed");
        }
        
        return chargingPlan;
    }

    @Override
    public Map<String, String> getProductCodeFromRouteRateCard(Integer userId, String assetIdentifier, String codeString, Date eventDate){
        SqlRowSet rs = getRecordFromRouteRateCard(userId, assetIdentifier, codeString, eventDate);
        Map<String, String> tariffInfo = new HashMap<>();
        if (rs.next()) {
            tariffInfo.put(SPCConstants.TARIFF_CODE, rs.getString("tariff_code"));
            tariffInfo.put(SPCConstants.PRODUCT_CODE, rs.getString("name"));
        }
        return tariffInfo;
    }

    @Override
    public Integer getRecordCountFromRouteRateCard(Integer userId, String assetIdentifier, String codeString, Date eventDate){
        SqlRowSet rs = getRecordFromRouteRateCard(userId, assetIdentifier, codeString, eventDate);
        Integer count = 0;
        while (rs.next()) {
            count++;
        }
        return count;
    }

    private SqlRowSet getRecordFromRouteRateCard(Integer userId, String assetIdentifier,
            String codeString, Date eventDate) {
        PlanDTO planDto = getPlanFromUserId(userId, assetIdentifier, eventDate);
        String routeTableName = (String) planDto.getMetaField(SPCConstants.PLAN_RATING).getValue();
        if (StringUtils.isBlank(routeTableName)) {
            logger.debug("Plan does not have route rate card configured");
            throw new SessionInternalError("Route rate card table name resolution is failed");
        }
        RouteRateCardDTO rateCardDTO = new RouteRateCardDAS().getRouteRateCardByNameAndEntity(routeTableName, new UserDAS().find(userId).getEntity().getId());
        String tableName = rateCardDTO.getTableName();
        String sqlQuery = String.format(FIND_ROUTE_TABLE_BY_IDENTIFIER, tableName);
        SqlRowSet rs = jdbcTemplate.queryForRowSet(sqlQuery, codeString.toUpperCase());
        return rs;
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
    @NoCache
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
    @NoCache
    public void notifyUserForJMRs(List<JbillingMediationRecord> jmrs) {
        if(CollectionUtils.isNotEmpty(jmrs)) {
            EventManager.process(new OptusMurJMREvent(jmrs, jmrs.get(0).getjBillingCompanyId()));
        }
    }

    @Override
    public Optional<Integer> findSubscriptionOrderByUserAssetEventDate(Integer userId, String assetIdentifier, Date eventDate) {
        Assert.notNull(userId, "Provide userId");
        Assert.notNull(assetIdentifier, "Provide Asset Identifier");
        Assert.notNull(eventDate, "Provide eventDate");
        try {
        	
        	OrderDTO planOrder = new OrderDAS().findOrderByUserAssetIdentifierEffectiveDate(userId, assetIdentifier, eventDate);
            if (planOrder == null) {
                logger.debug("Order resolution is failed for asset number {}", assetIdentifier);
                throw new SessionInternalError("Asset resolution is failed");
            }
        	
            return Optional.of(planOrder.getId());
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
    public QuantityResolutionContext constructQuantityResolutionContextForCodeString(Integer userId, String assetIdentifier, String codeString, Date eventDate) {
        Assert.notNull(userId, "Please provide user id");
        Assert.notNull(assetIdentifier, "Please provide asset identifier");
        Assert.notNull(codeString, "Please provide asset code string");
        PlanDTO planDto = getPlanFromUserId(userId, assetIdentifier, eventDate);
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

    @Override
    public Integer getPlanId(Integer userId, String assetIdentifier, Date eventDate) {
        PlanDTO planDto = getPlanFromUserId(userId, assetIdentifier, eventDate);
        return null != planDto ? planDto.getId(): null;
    }

    @Override
    public Map<String, Date> getActiveSinceAndActiveUntilDates(Integer orderId) {
        OrderDTO order = new OrderDAS().find(orderId);
        Map<String, Date> map = new HashMap<>();
        map.put(SPCConstants.ACTIVE_SINCE_DATE, order.getActiveSince());
        map.put(SPCConstants.ACTIVE_UNTIL_DATE, order.getActiveUntil());
        return map;
    }

    @Override
    @NoCache
    public Date getTimeZonedEventDate(Date eventDate, Integer entityId) {
        // get company level time zone
        String companyTimeZone = TimezoneHelper.getCompanyLevelTimeZone(entityId);
        Date newDate = Date.from(Instant.ofEpochMilli(eventDate.getTime()).atZone(ZoneId.of(companyTimeZone)).toLocalDateTime()
                .atZone(ZoneId.systemDefault()).toInstant());
        logger.debug(
                "SPCOrderServiceImpl - company id : {}, time zone : {}, event date : {}, converted event date : {}",
                entityId, companyTimeZone, eventDate, newDate);
        return newDate;
    }

    @Override
    public BigDecimal getAmountWithoutGST(Integer userId,Integer itemId, BigDecimal itemPrice) {
        if(itemId.equals(BigDecimal.ZERO)){
            return itemPrice;
        }
        BigDecimal taxRate = null;
        MetaFieldValue tableNameMF = new UserBL(userId).getEntity().getEntity().getMetaField(Constants.TAX_TABLE_NAME);
        MetaFieldValue taxSchemeMF = new ItemBL(itemId).getEntity().getMetaField(Constants.TAX_SCHEME);
        if(null != tableNameMF && null != taxSchemeMF){
            String findSQL = String.format(FIND_TAX_RATE, tableNameMF.getValue());
            SqlRowSet rs = jdbcTemplate.queryForRowSet(findSQL, taxSchemeMF.getValue());
            if (rs.next()) {
                taxRate = new BigDecimal(rs.getString("tax_rate"));
            }
        }
        if(null != itemPrice && null != taxRate){
            taxRate = BigDecimal.ONE.add(((taxRate).divide(new BigDecimal(100))));
            itemPrice = itemPrice.divide(taxRate, 10, BigDecimal.ROUND_HALF_UP);
        }
        return itemPrice;
    }

    @Override
    public List<String> getCustomerCareContactNumbers(String dataTableName) {
        String findSQL = String.format(FIND_SPC_CC_SQL, dataTableName);
        List<String> ccNumbers = (jdbcTemplate.queryForList(findSQL, String.class) != null ? jdbcTemplate.queryForList(
                findSQL, String.class) : new ArrayList<String>());
        return ccNumbers;
    }

    @Override
    public String encryptSensitiveData(Integer hashingMethodId, String data) {
        return JBCrypto.encodePassword(hashingMethodId, data);
    }

    @Override
    public JbillingMediationRecord[] getUnBilledMediationEventsByServiceId(Integer entityId, String serviceId) {
        logger.info("getUnBilledMediationEventsByServiceId against entityId : {} , service Id : {}", entityId, serviceId);
        List<AssetDTO> assets = new AssetDAS().findAssetByMetaFieldValue(entityId, CommonConstants.SERVICE_ID, serviceId);
        validateAssets(serviceId, assets);
        AssetDTO asset = assets.get(0);
        return mediationService.getUnBilledMediationEventsByCallIdentifier(asset.getIdentifier()).toArray(new JbillingMediationRecord[0]);
    }

    @Override
    @Transactional(readOnly = true)
    public JbillingMediationRecord[] getMediationEventsByServiceIdAndDateRange(Integer entityId,String serviceId,Integer offset,Integer limit, String fromDate, String toDate) {
        logger.info("getUnBilledMediationEventsByServiceAndDateRange against entityId : {} , service Id : {}, Offset : {}, Limit : {}, start date : {}, end date : {}", entityId, serviceId, offset, limit, fromDate, toDate);
        SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT_YYYY_MM_DD);
        try{
            formatter.setLenient(false);
            Date startDate = formatter.parse(fromDate);
            Date endDate = null;
            if(StringUtils.isNotEmpty(toDate)) {
               endDate = formatter.parse(toDate);
            }
            if(null == startDate || null == endDate) {
               throw new SessionInternalError("Invalid date value!",
                       new String[] { "Dates should not be null" }, HttpStatus.SC_BAD_REQUEST);
            }
            if(null!= endDate && endDate.before(startDate)) {
               throw new SessionInternalError("To date should not be before From date!",
                       new String[] { "End date should not be before Start date" }, HttpStatus.SC_BAD_REQUEST);
            }
            if(offset<0) {
               throw new SessionInternalError("Invalid offset value!",
                       new String[] { "The offset value should be greater than 0" }, HttpStatus.SC_BAD_REQUEST);
            }

            if(limit<=0 || limit>10000) {
               throw new SessionInternalError("Invalid limit value!",
                       new String[] { "The limit should be between 1 and 10000" }, HttpStatus.SC_BAD_REQUEST);
           }
           if(null!= endDate) {
              endDate = DateConvertUtils.asUtilDate(DateConvertUtils.asLocalDateTime(endDate)
                        .plusDays(1L)
                        .minusSeconds(1L));
           }
           List<AssetDTO> assets = new AssetDAS().findAssetByMetaFieldValue(entityId, CommonConstants.SERVICE_ID, serviceId);
           validateAssets(serviceId, assets);
           AssetDTO asset = assets.get(0);
           String identifier = asset.getIdentifier();
           logger.info("identifier against service Id : {}", identifier);
           return mediationService.getMediationRecordsByCallIdentifierDateRange(identifier, offset, limit, startDate , endDate).toArray(new JbillingMediationRecord[0]);
        } catch (ParseException e) {
        	throw new SessionInternalError("Invalid Date provided",
        			new String[] { "Invalid Date provided, Date format should be yyyy-mm-dd " }, HttpStatus.SC_BAD_REQUEST);
        }
    }

    private void validateAssets(String serviceId, List<AssetDTO> assets) {
        if (assets.isEmpty()) {
            throw new SessionInternalError(
                CommonConstants.VALIDATION_FAILED,
                new String[] { "Asset not found for supplied service Id {}",serviceId }, HttpStatus.SC_NOT_FOUND);
        }
        if (assets.size() > 1) {
            throw new SessionInternalError(
                CommonConstants.VALIDATION_FAILED,
                new String[] { "More than one row for Service Id : {}",serviceId }, HttpStatus.SC_NOT_FOUND);
        }
    }
}
