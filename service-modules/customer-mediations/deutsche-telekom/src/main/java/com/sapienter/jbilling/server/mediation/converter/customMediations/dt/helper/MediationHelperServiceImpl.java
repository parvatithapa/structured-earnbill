package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.helper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.annotation.PostConstruct;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MediationHelperServiceImpl implements MediationHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private JdbcTemplate jdbcTemplate = null;

	private Cache<String, Integer> itemCache;
	private Cache<String, Map<String, Object>> customerCache;
    private int cacheSize = 5000;
    private ConcurrentMap<String, String> productMap = new ConcurrentHashMap<>();

	private static final String USER_BY_METAFIELD = 
            "SELECT u.id, u.currency_id" +
            " FROM base_user u" +
            " JOIN customer c ON c.user_id=u.id" +
            " JOIN customer_meta_field_map cmm ON cmm.customer_id=c.id" +
            " JOIN meta_field_value mv ON mv.id=cmm.meta_field_value_id" +
            " JOIN meta_field_name n ON n.id=mv.meta_field_name_id" +
            " WHERE u.entity_id = ? AND n.name='externalAccountIdentifier' AND mv.string_value =  ?";

    private static final String ITEM_BY_IDENTIFIER = 
            "SELECT i.id FROM item i " +
            "left join item_entity_map iem on iem.item_id = i.id " +
            "WHERE i.entity_id = ? AND i.internal_number = ? AND i.deleted = 0 ";

    private static final String QUERY_ROUTE_TABLE_NAME = "SELECT table_name FROM route WHERE name = ?";

    @PostConstruct
    private void initCache() {
        itemCache =  CacheBuilder.newBuilder()
                .concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
                .maximumSize(cacheSize) // maximum records can be cached
                .expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
                .<String,Integer>build();

        customerCache =  CacheBuilder.newBuilder()
                .concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
                .maximumSize(cacheSize) // maximum records can be cached
                .expireAfterAccess(45, TimeUnit.MINUTES) // cache will expire after 45 minutes of access
                .<String,Map<String, Object>>build();
        logger.info("Item and Customer Cache initiated for MediationHelper service");
    }

    @Override
    public Map<String, Object> resolveCustomerByExternalAccountIdentifier(Integer entityId,String customerIdentifier) {
        String entityIdStr = entityId.toString();
        String customerIdentifierKey = entityIdStr + "_" + customerIdentifier;
        Map<String, Object> userMap = customerCache.getIfPresent(customerIdentifierKey);
        if(userMap != null) {
            logger.info("Customer returned from cache for identifier {}", customerIdentifierKey);
            return userMap;
        }
        userMap = new HashMap<>();

        SqlRowSet rs = jdbcTemplate.queryForRowSet(USER_BY_METAFIELD, entityId, customerIdentifier);
        if (rs.next()) {
        	userMap.put(MediationStepResult.USER_ID, rs.getInt("id"));
            userMap.put(MediationStepResult.CURRENCY_ID, rs.getInt("currency_id"));
            customerCache.put(customerIdentifierKey, userMap);
        }
        logger.info("Customer returned from DB for identifier {}", customerIdentifierKey);
        return userMap;
    }

    /*
     * Lookup sequence:
     * 1. Items mapped to another item (like OBS)
     * 2. Item identifier as found in CDR "ProductId" field
     * 3. Item identifier composed from "ProductId" and "ExtendParams"
     */
    @Override
    public Map.Entry<Integer, String> resolveItemById(Integer entityId, String itemIdentifier, String extendedParams) {

        String entityIdStr = entityId.toString();
        String itemIdentifierKey = entityIdStr + "_" + itemIdentifier;

        // mapped product ?
        Map.Entry<Integer, String> mappedItem = getMappedItem(entityId, itemIdentifier, itemIdentifierKey);
        if (mappedItem != null) {
            logger.info("Item Map returned for identifier key {}", itemIdentifierKey);
            return mappedItem;
        }

        // look through the cache
        Integer itemId = itemCache.getIfPresent(itemIdentifierKey);
        if (itemId != null) {
            return new IntegerStringPair(itemId, itemIdentifier);
        }

        String itemExtendedParams = itemIdentifier + "_" + extendedParams;
        String itemExtendedParamsKey = entityIdStr + "_" + itemIdentifier + "_" + extendedParams;

        itemId = itemCache.getIfPresent(itemExtendedParamsKey);
        if (itemId != null) {
            logger.info("Item Map returned for item extended params key {}", itemExtendedParamsKey);
            return new IntegerStringPair(itemId, itemExtendedParams);
        }

        // Not in cache, find
        List<Integer> itemIdList = jdbcTemplate.queryForList(ITEM_BY_IDENTIFIER,
                new Object[]{entityId, itemIdentifier}, Integer.class);

        if (!itemIdList.isEmpty()) {
            itemId = itemIdList.get(0);
            itemCache.put(itemIdentifierKey, itemId);
            return new IntegerStringPair(itemId, itemIdentifier);

        }

        itemIdList = jdbcTemplate.queryForList(ITEM_BY_IDENTIFIER,
                new Object[]{entityId, itemExtendedParams}, Integer.class);
        logger.info("Item Map returned rom DB for item extended params key {}", itemExtendedParamsKey);
        if(!itemIdList.isEmpty()) {
            itemId = itemIdList.get(0);
            itemCache.put(itemExtendedParamsKey, itemId);
            return new IntegerStringPair(itemId, itemExtendedParams);
        }

        return null;
    }

    private Map.Entry<Integer, String> getMappedItem(Integer entityId, String itemIdentifier,
                                                     String itemIdentifierKey) {

        String mappedCode = productMap.get(itemIdentifier);
        if (mappedCode != null) {
            Integer itemId = itemCache.getIfPresent(mappedCode);
            if (itemId != null) {
                return new IntegerStringPair(itemId, mappedCode);
            }

            List<Integer> itemIdList = jdbcTemplate.queryForList(ITEM_BY_IDENTIFIER,
                    new Object[]{ entityId, mappedCode }, Integer.class);

            if(!itemIdList.isEmpty()) {
                itemId = itemIdList.get(0);
                itemCache.put(itemIdentifierKey, itemId);
                return new IntegerStringPair(itemId, mappedCode);
            }
        }

        return null;
    }

    @Override
	public List<Map<String, Object>> getPreferencesByEntity(Integer entityId) {
    	String sql = "select pt.id as id, pref.value as value, pt.def_value as default_value "+
    				 "from preference_type pt left outer join preference pref on pt.id=pref.type_id "+
    				 "and ((pref.foreign_id=? and pref.table_id = (select jt.id from jbilling_table jt where jt.name='entity')))";
    	return getJdbcTemplate().queryForList(sql, entityId);
    }

	@Override
	public String getMediationJobLauncherByConfigId(Integer configId) {
		String mediationJobLauncher = null;
	    String sql = "select mediation_job_launcher " +
	            	 "from mediation_cfg " +
	            	 "where id = ?";

	    SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, configId);
	    if (rs.next()) {
	    	mediationJobLauncher = rs.getString(1);
	    }
	    return mediationJobLauncher;
	}

    @Override
    public String getMediationCdrFolderByConfigId(Integer configId) {
        String mediationCdrFolder = null;
        String sql = "select local_input_directory " +
                "from mediation_cfg " +
                "where id = ?";

        SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, configId);
        if (rs.next()) {
            mediationCdrFolder = rs.getString(1);
        }
        return mediationCdrFolder;
    }

	@Override
	public Integer getUserCompanyByUserId(Integer userId) {
        Integer company = null;
        String sql = "select entity_id " +
                	 "from base_user " +
                	 "where id = ?";
        
        SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, userId);
        if (rs.next()) {
            company = rs.getInt(1);
        }
        return company;
    }

	public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

	@Override
	public Integer getParentCompanyId(Integer entityId) {
		 Integer ParentCompanyId = null;
		 String sql = "select parent_id from entity where id =?";
		 SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, entityId);
		 if (rs.next()){
			 ParentCompanyId = rs.getInt(1);
		 }
		return ParentCompanyId;
	}

	@Override
	public boolean isMediationConfigurationGlobal(Integer configId) {
		boolean isGlobal = false;
		String sql = "select global from mediation_cfg where id = ?";
		SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, configId);
		if (rs.next()){
			isGlobal = rs.getBoolean(1);
		}
		return isGlobal;
	}

	@Override
	public List<Integer> getChildEntitiesIds(Integer parentId) {
		List<Integer> ids = new ArrayList<Integer>();
		String sql = "select id from entity where parent_id = ?";
		SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, parentId);
		while(rs.next()){
			ids.add(rs.getInt(1));
		}
		return ids;
	}

	private Function<List<Map<String, Object>>, Map<String, Object>> getResult = (value) -> {
		if(!value.isEmpty())
		return value.get(0);
		return null;
	};

    @Override
    public Set<String> productsToAggregate() {
        Set<String> productsToAggregate = new HashSet<>();

        String tableName = getRouteTableName("dt_product_aggregate");
        if(tableName != null) {
            SqlRowSet rs = jdbcTemplate.queryForRowSet("SELECT * FROM "+tableName);
            while (rs.next()) {
                productsToAggregate.add(rs.getString("product_code"));
            }
        }

        return productsToAggregate;
    }

    @Override
    public void loadProductMapCaches(Integer entityId) {
        productMap.clear();

        String tableName = getRouteTableName("dt_product_map");
        if(tableName != null) {
            SqlRowSet rs = jdbcTemplate.queryForRowSet("SELECT * FROM "+tableName);
            while (rs.next()) {
                productMap.put(rs.getString("from_product_code"), rs.getString("to_product_code"));
            }
        }
    }

    private String getRouteTableName(String name) {
        SqlRowSet rs = jdbcTemplate.queryForRowSet(QUERY_ROUTE_TABLE_NAME, name);
        String tableName = null;
        if (rs.next()) {
            tableName = rs.getString(1);
        }
        return tableName;
    }

    private static final class IntegerStringPair implements Map.Entry<Integer, String> {

        private Integer key;
        private String  value;

        private IntegerStringPair(Integer key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Integer getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String value) {
            return this.value = value;
        }
    }
}
