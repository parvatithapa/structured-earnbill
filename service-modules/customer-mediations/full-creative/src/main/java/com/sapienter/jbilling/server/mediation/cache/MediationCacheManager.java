package com.sapienter.jbilling.server.mediation.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;
import com.sapienter.jbilling.server.util.Context;

/**
 *
 * @author krunal bhavsar
 *
 */
public class MediationCacheManager {
	private static FormatLogger LOG = new FormatLogger(Logger.getLogger(MediationCacheManager.class));
	private static String MEDIATION_HELPER_SERVICE_BEAN = "mediationHelperService";
	private static Cache<String, Object> mediationCache;
	private static MediationHelperService mhs;

	static {
		mhs = Context.getBean(MEDIATION_HELPER_SERVICE_BEAN);
		mediationCache =  CacheBuilder.newBuilder()
								 	  .concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
								 	  .maximumSize(Long.MAX_VALUE) // maximum records can be cached
								 	  .expireAfterAccess(10, TimeUnit.MINUTES) // cache will expire after 10 minutes of access
								 	  .<String,Object>build();
	}

	public static void clearCache() {
		mediationCache.invalidateAll();
	}

	public static String getMetaFieldValue(String metaFieldName, Integer entityId) {
		String fieldValue = populateMetaFieldsValueForEntity(entityId).get(metaFieldName);
		if(null == fieldValue || fieldValue.isEmpty()) {
			LOG.debug("throwing EmptyResultDataAccessException preferenceValue : |%s|", metaFieldName);
			throw new EmptyResultDataAccessException("Could not find Meta Field Value " + metaFieldName, 1);
		}
		return fieldValue;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> populateMetaFieldsValueForEntity(Integer entityId) {
		String key ="Company-Level-MetaFieldValue-EntityId-"+entityId;

		Map<String, String> cachedCompanyLevelMetaFieldValueMap = (Map<String, String>) mediationCache.getIfPresent(key);
		if(null!= cachedCompanyLevelMetaFieldValueMap) {
			return cachedCompanyLevelMetaFieldValueMap;
		}
		mediationCache.put(key, mhs.getCompanyLevelMetaFieldValueByEntity(entityId));
		return (Map<String, String>) mediationCache.getIfPresent(key);

	}

	public static Boolean isProductVisibleToCompany(Integer itemId, Integer entityId,Integer parentId) {
		String key = "item-"+itemId+"-"+entityId+"-"+parentId;

		Boolean isVisible = (Boolean) mediationCache.getIfPresent(key);
		if(null != isVisible) {
			return isVisible;
		}
		mediationCache.put(key, mhs.isProductVisibleToCompany(itemId, entityId, parentId));
		return (Boolean) mediationCache.getIfPresent(key);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> resolveUserByAssetField(Integer entityId, String assetField) {
		String key  = "assetIdentifier-"+entityId+"-"+assetField;

		Map<String,Object> userMap = (Map<String, Object>) mediationCache.getIfPresent(key);
		if(null != userMap) {
			return userMap;
		}
		mediationCache.put(key, mhs.resolveUserByAssetField(entityId, assetField));
		return (Map<String, Object>) mediationCache.getIfPresent(key);
	}

	public static boolean isTablePresent(Integer entityId, String tableName) throws Exception {
		String key = entityId.toString()+"-"+tableName;
		Boolean isPresent = (Boolean) mediationCache.getIfPresent(key);
		if(null!=isPresent) {
			return isPresent;
		}
		mediationCache.put(key, Boolean.valueOf(mhs.isTablePresent(tableName)));
		return (boolean) mediationCache.getIfPresent(key);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getCityStateCountryByNPANXX(String[] NPANXX, String tableName) {
		String key = String.join("-", NPANXX);
		Map<String,Object> result = (Map<String, Object>) mediationCache.getIfPresent(key);
		if(null != result) {
			return result;
		}
		mediationCache.put(key, mhs.getCityStateCountryByNPANXX(NPANXX, tableName));
		return (Map<String, Object>) mediationCache.getIfPresent(key);
	}

	@SuppressWarnings("unchecked")
	public static List<String> getAllCRDDirectionsForEntity(Integer entityId) {
	    String key = entityId + "-"+ "CDR-Direction";
        List<String> cachedValue = (List<String>) mediationCache.getIfPresent(key);
	    if(cachedValue!=null) {
	        return cachedValue;
	    }
	    Map<String, String> values = populateMetaFieldsValueForEntity(entityId);
	    List<String> directions = Arrays.stream(MetaFieldName.values())
	            .filter(field -> field.name().contains("_CALL_TYPE"))
	            .map(MetaFieldName::getMetaFieldName)
	            .collect(Collectors.toList());
	    List<String> directionValues = new ArrayList<>();
	    for(String direction : directions) {
	        directionValues.add(values.get(direction));
	    }
	    mediationCache.put(key, directionValues);
	    return (List<String>) mediationCache.getIfPresent(key);
	}
}
