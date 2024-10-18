package com.sapienter.jbilling.server.integration.common.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sapienter.jbilling.server.integration.common.service.vo.CompanyInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.CustomerInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderLineInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.OrderLineTierInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ProductInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ProductRatingConfigInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.ReservedPlanInfo;
import com.sapienter.jbilling.server.integration.common.service.vo.UsagePoolInfo;
import com.sapienter.jbilling.server.integration.common.utility.MetafieldRowMapper;
import com.sapienter.jbilling.server.integration.common.utility.TierRowMapper;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;

@Component
public class JdbcTemplateHelperDataAccessService implements HelperDataAccessService {

	@Getter
	@Setter
	private JdbcTemplate jdbcTemplate;

	private Cache<Integer, CompanyInfo> companyInfoCache;

	private Cache<String, CustomerInfo> customerInfoCache;

	private Cache<String, ProductInfo> productInfoCache;

	private Cache<String, List<UsagePoolInfo>> usagePoolsInfoCache;

	private Cache<String, List<Integer>> productsInPlanInfoCache;

	private Cache<String, MetaFieldValueWS> planMetafieldValueCache;

	private Cache<String, MetaField> metafieldCache;

	private Cache<String, Integer> customerUsagePoolToPlanCache;

	private Cache<String, ReservedPlanInfo> reservedPlanInfoPool;

	private Cache<Integer, Integer> itemIdOfPlanCache;

	private static final String LANGUAGE_ID_COLUMN_NAME = "language_id";

	private static final String CURRENCY_ID_COLUMN_NAME = "currency_id";

	private int cacheSize = 5000;
	private int metafieldCacheSize = 500;

	@PostConstruct
	private void initCache() {

		companyInfoCache = CacheBuilder.newBuilder()

			.concurrencyLevel(4)
			.maximumSize(100)
			.expireAfterAccess(20, TimeUnit.MINUTES)
			.<Integer, CompanyInfo>build();

		customerInfoCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(5000) // maximum records can be cached
			.expireAfterAccess(20, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<String, CustomerInfo>build();

		productInfoCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(1000) // maximum records can be cached
			.expireAfterAccess(20, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<String, ProductInfo>build();

		usagePoolsInfoCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(cacheSize) // maximum records can be cached
			.expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<String, List<UsagePoolInfo>>build();

		productsInPlanInfoCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(cacheSize) // maximum records can be cached
			.expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<String, List<Integer>>build();

		planMetafieldValueCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(cacheSize) // maximum records can be cached
			.expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<String, MetaFieldValueWS>build();

		metafieldCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(metafieldCacheSize) // maximum records can be cached
			.expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<String, MetaField>build();

		customerUsagePoolToPlanCache = CacheBuilder.newBuilder()

			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(cacheSize) // maximum records can be cached
			.expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<String, Integer>build();

		reservedPlanInfoPool = CacheBuilder.newBuilder()
			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(cacheSize) // maximum records can be cached
			.expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<String, ReservedPlanInfo>build();

		itemIdOfPlanCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
			.maximumSize(cacheSize) // maximum records can be cached
			.expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
			.<Integer, Integer>build();
	}

	private static final String USER_INFO_QUERY =
		"SELECT u.id, u.currency_id , u.language_id, mv.string_value as mfv_value" +
			" FROM entity e" +
			" JOIN base_user u ON u.entity_id = e.id" +
			" JOIN customer c on c.user_id=u.id" +
			" JOIN customer_meta_field_map cmm on cmm.customer_id=c.id" +
			" JOIN meta_field_value mv on mv.id=cmm.meta_field_value_id" +
			" JOIN meta_field_name n on n.id=mv.meta_field_name_id" +
			" WHERE e.id = ? AND u.id = ? AND n.name = ? AND mv.string_value IS NOT NULL";

	@Override
	public Optional<CustomerInfo> getCustomerInfo(int entityId, int userId, String metaFileName) {
		String key = getCustomerInfoCacheKey(entityId, userId, metaFileName);
		CustomerInfo customerInfo = customerInfoCache.getIfPresent(key);
		if (customerInfo != null) {
			return Optional.of(customerInfo);
		}

		SqlRowSet rs = jdbcTemplate.queryForRowSet(USER_INFO_QUERY, entityId, userId, metaFileName);
		if (rs.next()) {
			customerInfo = CustomerInfo.builder().externalAccountIdentifier(rs.getString("mfv_value"))
				.userId(userId)
				.externalAccountIdentifier(rs.getString("mfv_value"))
				.languageId(rs.getInt(LANGUAGE_ID_COLUMN_NAME))
				.currencyId(rs.getInt(CURRENCY_ID_COLUMN_NAME)).build();

			customerInfoCache.put(key, customerInfo);
			return Optional.of(customerInfo);
		}
		return Optional.empty();
	}

	private String getCustomerInfoCacheKey(Integer entityId, Integer userId, String metafieldName) {
		return String.format("%d:%d:%s", entityId, userId, metafieldName);
	}

	private static final String USER_WITH_MEDIATED_ORDERS_PARTITIONED = "SELECT DISTINCT u.id AS user_id" +
		" FROM entity e" +
		" JOIN base_user u ON u.entity_id = e.id" +
		" JOIN purchase_order po on po.user_id = u.id" +
		" JOIN order_status os on os.id = po.status_id" +
		" WHERE u.entity_id = e.id AND u.deleted = 0 AND po.is_mediated = true AND po.deleted = 0 AND po.create_datetime <= ? AND e.id = ? AND os.id = ? AND u.id % ? = ?";

	@Override
	public List<Integer> getUsersWithMediatedOrderAndPartition(Integer entityId, int orderStatusId, Date lastMediationRun, int partitions, int partition) {
		SqlRowSet rs = jdbcTemplate.queryForRowSet(USER_WITH_MEDIATED_ORDERS_PARTITIONED, lastMediationRun, entityId, orderStatusId,  partitions, partition - 1);
		List<Integer> userIds = new ArrayList<>();
		while (rs.next()) {
			userIds.add(rs.getInt("user_id"));
		}
		return userIds;
	}

	private static final String MEDIATED_ORDERS_BY_STATUS_AND_USER = "SELECT po.id, po.currency_id, u.language_id" +
		" FROM entity e" +
		" JOIN base_user u ON u.entity_id = e.id" +
		" JOIN purchase_order po on po.user_id = u.id" +
		" JOIN order_status os on os.id = po.status_id" +
		" WHERE u.entity_id = e.id AND u.deleted = 0 AND po.is_mediated = true AND po.deleted = 0 AND po.create_datetime <= ? AND e.id = ? AND u.id = ? AND os.id = ?";

	@Override
	public List<OrderInfo> getMediatedOrdersByStatusAndUser(Integer entityId, Integer userId, int orderStatusId, Date lastMediationRun) {
		SqlRowSet rs = jdbcTemplate.queryForRowSet(MEDIATED_ORDERS_BY_STATUS_AND_USER, lastMediationRun, entityId, userId, orderStatusId);
		List<OrderInfo> orders = new ArrayList<>();

		while (rs.next()) {
			orders.add(OrderInfo.builder().orderId(rs.getInt("id"))
				.currencyId(rs.getInt(CURRENCY_ID_COLUMN_NAME))
				.languageId(rs.getInt(LANGUAGE_ID_COLUMN_NAME)).build());
		}
		return orders;
	}

	private static final String USERS_WITH_RESERVED_PLANS = "SELECT DISTINCT u.id AS user_id" +
		" FROM entity e" +
		" JOIN base_user u ON u.entity_id = e.id" +
		" JOIN purchase_order po on po.user_id = u.id" +
		" JOIN order_status os on os.id = po.status_id" +
		" JOIN customer_usage_pool_map cupm ON po.id = cupm.order_id" +
		" JOIN plan_meta_field_map pmfp ON cupm.plan_id = pmfp.plan_id" +
		" JOIN meta_field_value mfv ON pmfp.meta_field_value_id = mfv.id" +
		" WHERE u.entity_id = e.id AND u.deleted = 0" +
		" AND po.deleted = 0 AND os.order_status_flag = 0 AND e.id = ?" +
		" AND mfv.string_value = ? AND mfv.meta_field_name_id = ?" +
		" AND u.id % ? = ?";

	public List<Integer> getUsersWithReservedMonthlyPlans(Integer entityId, int orderStatusId, int partitions, int partition) {

		List<Integer> userIds = new ArrayList<>();
		MetaField metafieldPaymentOption = getFieldByName(entityId, EntityType.PLAN, com.sapienter.jbilling.server.integration.Constants.PLAN_PAYMENT_OPTION_MF);
		if (metafieldPaymentOption == null) {
			return userIds;
		}
		SqlRowSet rs = jdbcTemplate.queryForRowSet(USERS_WITH_RESERVED_PLANS, entityId, com.sapienter.jbilling.server.integration.Constants.PLAN_PAYMENT_OPTION_MONTHLY, metafieldPaymentOption.getId(),
			partitions, partition - 1);

		while (rs.next()) {
			userIds.add(rs.getInt("user_id"));
		}
		return userIds;
	}

	private static final String RESERVED_PLAN_BY_USER = "SELECT po.id, po.active_since, po.active_until, cupm.plan_id, po.currency_id, u.language_id" +
		" FROM entity e" +
		" JOIN base_user u ON u.entity_id = e.id" +
		" JOIN purchase_order po on po.user_id = u.id" +
		" JOIN customer_usage_pool_map cupm ON po.id = cupm.order_id" +
		" JOIN plan_meta_field_map pmfp ON cupm.plan_id = pmfp.plan_id" +
		" JOIN meta_field_value mfv ON pmfp.meta_field_value_id = mfv.id" +
		" JOIN order_status os on os.id = po.status_id" +
		" WHERE u.entity_id = e.id AND u.deleted = 0" +
		" AND po.deleted = 0" +
		" AND os.order_status_flag = 0 AND e.id = ? AND u.id = ?" +
		" AND  mfv.string_value = ? AND mfv.meta_field_name_id = ?";


	public List<OrderInfo> getReservedPlanOrdersByCustomer(Integer entityId, Integer userId) {

		List<OrderInfo> ordersInfos = new ArrayList<>();
		MetaField metafieldPaymentOption = getFieldByName(entityId, EntityType.PLAN, com.sapienter.jbilling.server.integration.Constants.PLAN_PAYMENT_OPTION_MF);
		if (metafieldPaymentOption == null) {
			return ordersInfos;
		}

		SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(RESERVED_PLAN_BY_USER,
			entityId, userId, com.sapienter.jbilling.server.integration.Constants.PLAN_PAYMENT_OPTION_MONTHLY, metafieldPaymentOption.getId());
		while (sqlRowSet.next()) {
			ordersInfos.add(OrderInfo.builder()
				.orderId(sqlRowSet.getInt("id"))
				.planId(sqlRowSet.getInt("plan_id"))
				.languageId(sqlRowSet.getInt(LANGUAGE_ID_COLUMN_NAME))
				.currencyId(sqlRowSet.getInt(CURRENCY_ID_COLUMN_NAME))
				.activeSince(sqlRowSet.getDate("active_since"))
				.activeUntil(sqlRowSet.getDate("active_until")).build());
		}

		return ordersInfos;
	}

	private static final String PRODUCT_INFO_QUERY =
		" SELECT a.internal_number, b.content,b.language_id " +
			" FROM item a, international_description b, jbilling_table c," +
			" entity e" +
			" WHERE a.entity_id = e.id" +
			" AND e.id = ?" +
			" AND a.id = ?" +
			" AND a.deleted = 0" +
			" AND b.table_id = c.id" +
			" AND c.name = 'item'" +
			" AND b.foreign_id = a.id" +
			" AND b.psudo_column = 'description'";

	@Override
	public Optional<ProductInfo> getProductInfo(int entityId, int productId, int languageId) {
		String key = getProductInfoCacheKey(entityId, productId, languageId);
		ProductInfo productInfo = productInfoCache.getIfPresent(key);
		if (productInfo != null) {
			return Optional.of(productInfo);
		}

		SortedMap<Date, List<ProductRatingConfigInfo>> ratingConfig = getProductRatingConfigInfo(entityId, productId, languageId);

		SqlRowSet rs = jdbcTemplate.queryForRowSet(PRODUCT_INFO_QUERY, entityId, productId);
		List<ProductInfo> productInfos = new ArrayList<>();
		while (rs.next()) {
			String internalNumber = rs.getString("internal_number");
			String description = rs.getString("content");
			int descLanguageId = rs.getInt(LANGUAGE_ID_COLUMN_NAME);
			ProductInfo newProductInfo = ProductInfo.builder()
				.productId(productId)
				.productCode(internalNumber)
				.description(description)
				.languageId(descLanguageId)
				.ratingConfig(ratingConfig).build();
			productInfos.add(newProductInfo);
		}
		if (CollectionUtils.isEmpty(productInfos)) {
			return Optional.empty();
		}
		Optional<ProductInfo> newProdInfo = Optional.of(productInfos).get().stream()
			.filter(x -> languageId == x.getLanguageId())
			.findFirst();
		if (!newProdInfo.isPresent()) {
			newProdInfo = Optional.of(productInfos).get().stream()
				.filter(x -> Constants.LANGUAGE_ENGLISH_ID == x.getLanguageId())
				.findFirst();
		}
		if (!newProdInfo.isPresent()) {
			newProdInfo = Optional.of(productInfos).get().stream().findFirst();
			newProdInfo.get().setDescription(newProdInfo.get().getProductCode());
		}
		productInfoCache.put(key, newProdInfo.get());

		return newProdInfo;
	}

	private String getProductInfoCacheKey(Integer entityId, Integer productId, Integer languageId) {
		return String.format("%d:%d:%d", entityId, productId, languageId);
	}

	private static final String PRODUCT_RATING_CONFIG_QUERY =
		"SELECT itcm.start_date, ind.content as pricing_unit ,ind.language_id " +
			"FROM item_rating_configuration_map itcm,international_description ind " +
			"WHERE itcm.item_id = ? " +
			"AND itcm.rating_configuration_id=ind.foreign_id " +
			"AND ind.psudo_column=? " +
			"AND ind.table_id=? ";

	private SortedMap<Date, List<ProductRatingConfigInfo>> getProductRatingConfigInfo(int entityId, int productId, int languageId) {

		SortedMap<Date, List<ProductRatingConfigInfo>> ratingInfo = new TreeMap<>();
		JbillingTableDAS tableDas = Context
			.getBean(Context.Name.JBILLING_TABLE_DAS);
		JbillingTable table = tableDas.findByName(Constants.TABLE_RATING_CONFIGURATION);
		SqlRowSet rs = jdbcTemplate.queryForRowSet(PRODUCT_RATING_CONFIG_QUERY, productId, "pricing_unit", table.getId());
		while (rs.next()) {
			Date startDate = rs.getDate("start_date");
			String pricingUnit = rs.getString("pricing_unit");
			ProductRatingConfigInfo info = ProductRatingConfigInfo.builder()
				.pricingUnit(pricingUnit)
				.languageId(rs.getInt(LANGUAGE_ID_COLUMN_NAME))
				.build();

			ratingInfo.computeIfAbsent(startDate, k -> new ArrayList<ProductRatingConfigInfo>());

			List<ProductRatingConfigInfo> productRatingConfigInfo = ratingInfo.get(startDate);
			productRatingConfigInfo.add(info);
			ratingInfo.put(startDate, productRatingConfigInfo);
		}
		return ratingInfo;
	}

	private static final String COMPANY_INFO_QUERY =
			"SELECT e.language_id, e.currency_id, l.code lang_code, l.country_code " +
			"FROM 	entity e, language l " +
			"WHERE 	e.language_id = l.id " +
			"AND 	e.id = ? ";

	@Override
	public Optional<CompanyInfo> getCompanyInfo(int entityId) {

		CompanyInfo companyInfo = companyInfoCache.getIfPresent(entityId);
		if (companyInfo != null) {
			return Optional.of(companyInfo);
		}

		SqlRowSet rs = jdbcTemplate.queryForRowSet(COMPANY_INFO_QUERY, entityId);
		if (rs.next()) {
			String langCode = rs.getString("lang_code");
			String countryCode = rs.getString("country_code");
			Locale locale = new Locale(langCode, countryCode);

			companyInfo = CompanyInfo.builder()
				.companyId(entityId)
				.languageId(rs.getInt(LANGUAGE_ID_COLUMN_NAME))
				.currencyId(rs.getInt(CURRENCY_ID_COLUMN_NAME))
				.locale(locale)
				.build();
			return Optional.of(companyInfo);
		}
		return Optional.empty();
	}

	private static final String USAGE_POOL_INFO_PLAN_ID =
		"SELECT * from usage_pool up" +
			" JOIN plan_usage_pool_map upm" +
			" ON up.id=upm.usage_pool_id where upm.plan_id = ? ";

	@Override
	public List<UsagePoolInfo> getUsagePoolsByPlanId(Integer entityId, Integer planId, Integer languageId) {

		String key = getPlanKey(entityId, planId, languageId);

		List<UsagePoolInfo> pools = usagePoolsInfoCache.getIfPresent(key);
		if (null != pools) {
			return pools;
		}
		pools = new ArrayList<>();
		UsagePoolInfo usagePoolInfo;
		SqlRowSet rs = jdbcTemplate.queryForRowSet(USAGE_POOL_INFO_PLAN_ID, planId);
		if (rs.next()) {
			usagePoolInfo = UsagePoolInfo.builder()
				.id(rs.getInt("id"))
				.quantity(rs.getBigDecimal("quantity"))
				.resetValue(rs.getString("reset_value")).build();

			pools.add(usagePoolInfo);
		}
		usagePoolsInfoCache.put(key, pools);
		return pools;
	}

	private String getPlanKey(Integer entityId, Integer planId, Integer languageId) {
		return String.format("%d:%d:%d", entityId, planId, languageId);
	}

	private static final String PRODUCT_IDs_BY_PLAN_ID = "SELECT item_id FROM plan_item where plan_id = ?";

	@Override
	public List<Integer> getProductIdsByPlanId(Integer entityId, Integer planId, Integer languageId) {

		String key = getPlanKey(entityId, planId, languageId);

		List<Integer> productIds = productsInPlanInfoCache.getIfPresent(key);
		if (null != productIds) {
			return productIds;
		}
		productIds = jdbcTemplate.queryForList(PRODUCT_IDs_BY_PLAN_ID, new Object[]{planId}, Integer.class);
		productsInPlanInfoCache.put(key, productIds);

		return productIds;
	}

	private static final String META_FIELD_PLAN_QUERY = "SELECT * FROM meta_field_value WHERE meta_field_name_id= ?" +
		" AND id IN (SELECT meta_field_value_id FROM plan_meta_field_map WHERE plan_id= ? )";

	private static final String META_FIELD_ORDER_QUERY = "SELECT * FROM meta_field_value WHERE meta_field_name_id= ?" +
		" AND id IN (SELECT meta_field_value_id FROM order_meta_field_map WHERE order_id= ? )";

	@Override
	public MetaFieldValueWS getPlanMetafieldValue(Integer entityId, Integer planId, Integer metafieldNameId) {

		String key = getPlanMetafieldValueKey(entityId, planId, metafieldNameId);
		MetaFieldValueWS metafieldValue = planMetafieldValueCache.getIfPresent(key);
		if (null != metafieldValue) {
			return metafieldValue;
		}

		try {
			metafieldValue = (MetaFieldValueWS) jdbcTemplate.queryForObject(META_FIELD_PLAN_QUERY, new MetafieldRowMapper(), metafieldNameId, planId);
			planMetafieldValueCache.put(key, metafieldValue);
		} catch (EmptyResultDataAccessException empty) {
			metafieldValue = null;

		}
		return metafieldValue;
	}

	private String getPlanMetafieldValueKey(Integer entityId, Integer planId, Integer metafieldNameId) {
		return String.format("%d:%d:%d", entityId, planId, metafieldNameId);
	}

	public MetaFieldValueWS getOrderMetafieldValue(Integer entityId, Integer orderId, Integer metafieldNameId) {

		MetaFieldValueWS metafieldValue;
		try {
			metafieldValue = (MetaFieldValueWS) jdbcTemplate.queryForObject(META_FIELD_ORDER_QUERY, new MetafieldRowMapper(), metafieldNameId, orderId);

		} catch (EmptyResultDataAccessException empty) {
			metafieldValue = null;
		}
		return metafieldValue;
	}

	@Override
	public MetaField getFieldByName(Integer entityId, EntityType entityType, String name) {
		String key = getMetafieldKey(entityId, entityType, name);
		MetaField metafield = metafieldCache.getIfPresent(key);
		if (null != metafield) {
			return metafield;
		}

		metafield = MetaFieldBL.getFieldByName(entityId, new EntityType[]{entityType}, name);
		if (metafield != null) {
			metafieldCache.put(key, metafield);
		}

		return metafield;
	}

	private String getMetafieldKey(Integer entityId, EntityType entityType, String name) {

		return String.format("%d:%s:%s", entityId, entityType.name(), name);
	}

	private static final String CUSTOMER_USAGE_POOL_QUERY = "SELECT customer_usage_pool_id, quantity FROM order_line_usage_pool_map WHERE order_line_id = ?";

	private static final String PLAN_ID_FOR_CUSTOMER_POOL_QUERY = "SELECT plan_id FROM customer_usage_pool_map WHERE id = ?";

	public Map<Integer, BigDecimal> getCustomerPoolsWithUtilizedQty(Integer entityId, Integer orderId, Integer languageId) {

		Map<Integer, BigDecimal> customerPoolIdsAndQuantiy = new HashMap<>();
		SqlRowSet rs = jdbcTemplate.queryForRowSet(CUSTOMER_USAGE_POOL_QUERY, orderId);

		while (rs.next()) {
			customerPoolIdsAndQuantiy.put(rs.getInt("customer_usage_pool_id"), rs.getBigDecimal("quantity"));
		}

		return customerPoolIdsAndQuantiy;
	}

	public Integer getPlanAssociatedToCustomerPool(Integer entityId, Integer customerPoolId, Integer languageId) {

		String key = getCustomerPoolToPlanKey(entityId, customerPoolId, languageId);

		Integer planId = customerUsagePoolToPlanCache.getIfPresent(key);
		if (planId != null) {
			return planId;
		} else {
			planId = jdbcTemplate.queryForObject(PLAN_ID_FOR_CUSTOMER_POOL_QUERY, new Object[]{customerPoolId}, Integer.class);
			customerUsagePoolToPlanCache.put(key, planId);
			return planId;
		}
	}

	private String getCustomerPoolToPlanKey(Integer entityId, Integer customerPoolId, Integer languageId) {
		return String.format("%d:%d:%d", entityId, customerPoolId, languageId);
	}

	private static final String ORDERLINE_TIERS_QUERY = "SELECT * FROM order_line_tier WHERE order_line_id = ?";

	public List<OrderLineTierInfo> getOrderLineTiers(Integer orderLineId) {

		List<OrderLineTierInfo> tiers = new ArrayList<>();
		try {
			tiers = (List<OrderLineTierInfo>) jdbcTemplate.query(ORDERLINE_TIERS_QUERY, new TierRowMapper(), orderLineId);
		} catch (DataAccessException e) {
			return tiers;
		}
		return tiers;

	}

	private static final String ITEM_ID_FOR_PLAN_QUERY = "SELECT item_id FROM plan WHERE id = ?";

	public Integer getItemIdForPlan(Integer planId) {

		Integer key = getkeyItemIdOfPlanCache(planId);
		Integer itemId = itemIdOfPlanCache.getIfPresent(key);
		if (itemId != null) {
			return itemId;
		}

		return jdbcTemplate.queryForObject(ITEM_ID_FOR_PLAN_QUERY, new Object[]{planId}, Integer.class);
	}

	private Integer getkeyItemIdOfPlanCache(Integer planId) {

		return planId;
	}

	public ReservedPlanInfo getReservedPlanInfo(Integer entityId, int planId, BigDecimal price) {

		Optional<CompanyInfo> companyInfo = getCompanyInfo(entityId);
		Integer languageId = companyInfo.get().getLanguageId();

		String key = getReservedPlanInfoKey(entityId, planId, languageId);
		ReservedPlanInfo reservedPlanInfo = reservedPlanInfoPool.getIfPresent(key);

		if (reservedPlanInfo != null) {
			return reservedPlanInfo;
		}

		MetaField metafieldPaymentOption = getFieldByName(entityId, EntityType.PLAN, com.sapienter.jbilling.server.integration.Constants.PLAN_PAYMENT_OPTION_MF);
		MetaField metafieldDuration = getFieldByName(entityId, EntityType.PLAN, com.sapienter.jbilling.server.integration.Constants.PLAN_DURATION_MF);

		MetaFieldValueWS paymentOption = getPlanMetafieldValue(entityId, planId, metafieldPaymentOption.getId());
		MetaFieldValueWS duration = getPlanMetafieldValue(entityId, planId, metafieldDuration.getId());

		Integer itemId = getItemIdForPlan(planId);

		Optional<ProductInfo> planInfo = getProductInfo(entityId, itemId, languageId);
		String description = "";
		if (planInfo.isPresent()) {
			description = planInfo.get().getDescription();
		}

		return ReservedPlanInfo.builder()
			.paymentOption(paymentOption.getStringValue())
			.duration(duration.getStringValue())
			.description(description)
			.price(price)
			.planId(planId)
			.entityId(entityId).build();
	}

	private String getReservedPlanInfoKey(Integer entityId, Integer planId, Integer languageId) {

		return String.format("%d:%d:%d", entityId, planId, languageId);
	}

	private static final String ORDER_LINES_BY_ORDER_QUERY = "SELECT ol.id, ol.item_id, ol.quantity, ol.price, ol.amount, ol.description, ol.create_datetime" +
															" FROM order_line ol" +
															" WHERE ol.order_id=?";
	@Override
	public List<OrderLineInfo> getOrderLines(Integer entityId, int orderId) {
		SqlRowSet rs = jdbcTemplate.queryForRowSet(ORDER_LINES_BY_ORDER_QUERY, orderId);
		List<OrderLineInfo> orderLines = new ArrayList<>();
		while (rs.next()) {
			orderLines.add(OrderLineInfo.builder()
				.orderId(orderId)
				.orderLineId(rs.getInt("id"))
				.itemId(rs.getInt("item_id"))
				.quantity(rs.getBigDecimal("quantity"))
				.price(rs.getBigDecimal("price"))
				.amount(rs.getBigDecimal("amount"))
				.createDateTime(rs.getDate("create_datetime"))
				.description(rs.getString("description")).build());
		}
		return orderLines;
	}
}
