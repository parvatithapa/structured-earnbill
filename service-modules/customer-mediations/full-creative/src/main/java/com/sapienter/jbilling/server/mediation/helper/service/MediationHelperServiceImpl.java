package com.sapienter.jbilling.server.mediation.helper.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class MediationHelperServiceImpl implements MediationHelperService {
	
	private JdbcTemplate jdbcTemplate = null;
	
	private static final String PRODUCT_VISIBLE_TO_PARENT_COMPANY_SQL =
            "select count(*) as count from item i " +
                    " left join item_entity_map ie on ie.item_id = i.id where " +
                    " i.id = ? and " +
                    " (ie.entity_id = ? or i.entity_id = ?) and" +
                    " i.deleted = 0";
	
	private static final String PRODUCT_VISIBLE_TO_CHILD_HIERARCHY_SQL =
            "select count(*) as count from item i "+
                "left outer join item_entity_map iem "+
                "on i.id = iem.item_id "+
                "where i.id = ? "+
                "and  i.deleted = 0 "+
                "and  (i.entity_id = ? or " +
                " iem.entity_id = ? or " +
                "((iem.entity_id = ? or iem.entity_id is null) and " +
                "i.global = true));";

	@Override
    public Map<String, Object> resolveUserByAssetField(Integer entityId, String assetField) {
		Map<String, Object> userMap = new HashMap<>();
		String sql = "select id, currency_id from base_user where id = (select user_id from purchase_order where id = "
				+ "(select order_id from order_line where id = (select order_line_id from asset where identifier = ? "
				+ " and deleted =0 ) and deleted =0) and deleted =0 ) and deleted =0 ";
		
        SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql,assetField);
        if (rs.next()) {
        	userMap.put(MediationStepResult.USER_ID, rs.getInt("id"));
            userMap.put(MediationStepResult.CURRENCY_ID, rs.getInt("currency_id"));
        }
        return userMap;
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
	    	mediationJobLauncher = rs.getString("mediation_job_launcher");
	    }
	    return mediationJobLauncher;
	}
	
	@Override
	public Integer getUserCompanyByUserId(Integer userId) {
        Integer company = null;
        String sql = "select entity_id " +
                	 "from base_user " +
                	 "where id = ?";
        
        SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, userId);
        if (rs.next()) {
            company = rs.getInt("entity_id");
        }
        return company;
    }
	
	@Override
    public Boolean doesAssetIdentifierExist(String assetIdentifier) {
    	boolean identifier = false;
    	String sql = "select identifier " +
                	 "from asset " +
                	 "where identifier=? and deleted=0";
    	
        SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, assetIdentifier);
        if (rs.next()) {
            if (StringUtils.isNotEmpty(rs.getString("identifier"))) {
            	identifier = true;
            }
        }
        return identifier;
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
			 ParentCompanyId = rs.getInt("parent_id");
		 }
		return ParentCompanyId;
	}

	@Override
	public boolean isProductVisibleToCompany(Integer itemId, Integer entityId,Integer parentId) {
		//this means that the entityId is root so the product must be defined for that company
		 if (null == parentId) {
			 String sql = PRODUCT_VISIBLE_TO_PARENT_COMPANY_SQL;
			 Object[] paraObjects= {itemId,entityId,entityId};
			 Number count = getJdbcTemplate().queryForObject(sql,paraObjects, Integer.class);
			 return null != count ? count.longValue() > 0 : false;
		 }
		//check if the product is visible to either the parent or the child company
		 else {
			 String sql = PRODUCT_VISIBLE_TO_CHILD_HIERARCHY_SQL;
			 Object[] paraObjects= {itemId,entityId,entityId,parentId};
			 Number count = getJdbcTemplate().queryForObject(sql,paraObjects, Integer.class);
			 return null != count ? count.longValue() > 0 : false;
		 }
	}

	@Override
	public boolean isMediationConfigurationGlobal(Integer configId) {
		boolean isGlobal = false;
		String sql = "select global from mediation_cfg where id = ?";
		SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, configId);
		if (rs.next()){
			isGlobal = rs.getBoolean("global");
		}
		return isGlobal;
	}

	@Override
	public List<Integer> getChildEntitiesIds(Integer parentId) {
		List<Integer> ids = new ArrayList<Integer>();
		String sql = "select id from entity where parent_id = ?";
		SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, parentId);
		while(rs.next()){
			ids.add(rs.getInt("id"));
		}
		return ids;
	}

	public Map<String, String> getCompanyLevelMetaFieldValueByEntity(Integer entityId) {
		String sql = "select field.name, this.boolean_value, this.date_value, this.decimal_value, this.integer_value, this.string_value from meta_field_value this " + 
				" inner join meta_field_name field on this.meta_field_name_id = field.id  and field.entity_type ='COMPANY' and field.entity_id = ? ";
		
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sql,entityId);
		Map<String, String> metaFieldValueMapByEntity = new HashMap<String, String>();
		for(Map<String, Object> row: result) {
			List<Entry<String, Object>> entries = new ArrayList<Map.Entry<String,Object>>(row.entrySet());
				String fieldName = entries.remove(0).getValue().toString();
				for(Entry<String, Object> entry: entries) {
					if(entry.getValue()!=null) {
						metaFieldValueMapByEntity.put(fieldName, entry.getValue().toString());
						break;
					} 
				}
		}
		
		return metaFieldValueMapByEntity;
	}

	@Override
	public boolean isTablePresent(String tableName) {
        try (Connection connection = getJdbcTemplate().getDataSource().getConnection()) {
            try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
                return rs.next();
            }
        } catch (SQLException sqlException) {
            throw new SessionInternalError(sqlException);
        }
    }
	
	private Function<List<Map<String, Object>>, Map<String, Object>> getResult = (value) -> {
		if(!value.isEmpty())
		return value.get(0);
		return null;
	};
	
	@Override
	public Map<String, Object> getCityStateCountryByNPANXX(String[] NPANXX, String tableName) {
		String sqlWithNXX = "select city, state, country from "+tableName + " where npa = ? and nxx = ?";
		List<Map<String, Object>> result = jdbcTemplate.queryForList(sqlWithNXX, NPANXX[0], NPANXX[1]);
		Map<String, Object> row = getResult.apply(result);
		if(null!=row) {
			return row;
		}else {
			 String sqlWithOutNXX = "select state, country from "+tableName + " where npa = ? limit 1";
			 result = jdbcTemplate.queryForList(sqlWithOutNXX, NPANXX[0]);
			 if(null!= getResult.apply(result)) {
				 return getResult.apply(result);
			 }
		 }
		return Collections.emptyMap();
	}

}
