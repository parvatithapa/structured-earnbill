package com.sapienter.jbilling.server.mediation.mrim;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.sapienter.jbilling.common.FormatLogger;

/**
 * @author neelabh
 * @since  03/08/2016
 */
public class RatingSchemeDASImpl implements RatingSchemeDAS {
	
	private JdbcTemplate jdbcTemplate = null;
	
	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RatingSchemeDASImpl.class));

	@Override
	public Integer getRatingSchemeByMediationAndEntity(Integer mediationCfgId, Integer entityId) {
    	Integer ratingIncrement = null;
    	String sql = "SELECT r.rating_scheme " +
    				 "FROM rating_scheme_association r " +
                     "WHERE r.mediation = ? " +
                     "AND r.entity = ?";
    	
    	SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, mediationCfgId, entityId);
    	if (rs.next()) {
    		ratingIncrement = rs.getInt("rating_scheme");
        }
    	return ratingIncrement;
    }
	
	@Override
	public Integer getGlobalRatingSchemeByEntity(Integer entityId) {
    	Integer rootEntityId = getRootEntityId(entityId);
    	Integer globalRatingSchemeId = null;
    	String sql = "SELECT rs.id " +
                	 "FROM rating_scheme rs " +
                	 "WHERE rs.entity_id = ? " +
                	 "AND rs.global = true";
    	
    	SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, rootEntityId);
    	if (rs.next()) {
    		globalRatingSchemeId = rs.getInt("id");
        }
        return globalRatingSchemeId;
    }
	
	@Override
	public Map<String, Integer> getMediationRatingSchemeById(Integer ratingSchemeId) {
    	Map<String, Integer> ratingSchemeMap = null;
    	String sql = "SELECT rs.initial_increment, rs.initial_rounding_mode, rs.main_increment, rs.main_rounding_mode " +
    				 "FROM  rating_scheme rs " +
                     "WHERE rs.id = ?";
    	
    	SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, ratingSchemeId);
    	if (rs.next()) {
    		ratingSchemeMap = new HashMap<String, Integer>();
    		ratingSchemeMap.put("initial_increment", rs.getInt("initial_increment"));
    		ratingSchemeMap.put("initial_rounding_mode", rs.getInt("initial_rounding_mode"));
    		ratingSchemeMap.put("main_increment", rs.getInt("main_increment"));
    		ratingSchemeMap.put("main_rounding_mode", rs.getInt("main_rounding_mode"));
        }
    	return ratingSchemeMap;
    }
	
	private Integer getRootEntityId(Integer entityId) {
    	Integer rootEntityId = entityId;
    	String sql = "SELECT e.parent_id " +
                     "FROM entity e " +
                     "WHERE e.id = ?";
    	
    	SqlRowSet rs = getJdbcTemplate().queryForRowSet(sql, entityId);
    	if(rs != null && rs.next()) {
    		int parentId = rs.getInt("parent_id");
    		if(!rs.wasNull()) {
            	rootEntityId = getRootEntityId(parentId);
            }
        }
    	return rootEntityId;
    }
	
	public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
