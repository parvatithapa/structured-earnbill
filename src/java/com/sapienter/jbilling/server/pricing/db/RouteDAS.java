/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.pricing.ITableUpdater;
import com.sapienter.jbilling.server.pricing.RouteBeanFactory;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResultString;

import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.Query;
import org.hibernate.ScrollableResults;

/**
 * Class extending AbstractDAS which include generics methods to save,find and perform other queries
 */
public class RouteDAS extends AbstractDAS<RouteDTO>{

    private static Pattern ValidSqlIdentifierPattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final String COUNT_QUERY = "SELECT COUNT(id) FROM %s";
    private static final String MAX_QUERY = "SELECT MAX(CHAR_LENGTH(%s)) FROM %s";
    private static final String MIN_QUERY = "SELECT MIN(CHAR_LENGTH(%s)) FROM %s";
    private static final String SELECT_QUERY = "SELECT * FROM %s";

    private void validateSqlIdentifier (String sqlIdentifierName) {
        if (!ValidSqlIdentifierPattern.matcher(sqlIdentifierName).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier (table or column name, etc...): " + sqlIdentifierName);
        }
    }

    public ScrollableResults getRouteTableRows(String tableName) {
        validateSqlIdentifier(tableName);
        Query query = getSession().createSQLQuery(String.format(SELECT_QUERY, tableName));

        return query.scroll();
    }
    
    public RouteDTO getRoute(Integer routeId) {
		Query query = getSession().createQuery(
				" from RouteDTO where id=:routeId");
        return (RouteDTO) query.setParameter("routeId",routeId).uniqueResult();
    }

    public RouteDTO getRoute(Integer entityId, String name) {
        Query query = getSession().createQuery(
                " from RouteDTO where company.id= :entityId and name=:name");
        return (RouteDTO) query.setParameter("entityId", entityId).setParameter("name", name).uniqueResult();
    }

    public List<RouteDTO> getRoutes(Integer entityId) {
        Query query = getSession().createQuery(
                " from RouteDTO where company.id= :entityId");
        return query.setParameter("entityId", entityId).list();
    }

    public Integer getLongestValue(String tableName, String matchingField) {
        validateSqlIdentifier(tableName);
        validateSqlIdentifier(matchingField);
        Query query = getSession().createSQLQuery(String.format(MAX_QUERY, matchingField, tableName));

        Number longestVal = (Number) query.uniqueResult();
        return longestVal.intValue();
    }
    
    public Integer getSmallestValue(String tableName, String matchingField) {
        validateSqlIdentifier(tableName);
        validateSqlIdentifier(matchingField);
        Query query = getSession().createSQLQuery(String.format(MIN_QUERY, matchingField, tableName));
        Number smallestVal = (Number) query.uniqueResult();

        return smallestVal.intValue();
    }

    public RouteDTO getRootRoute(Integer companyId){
        Query query = getSession().createQuery(
                " from RouteDTO where " +
                        "     company.id= :companyId " +
                        " and rootTable = true ");
        query.setParameter("companyId", companyId);
        return (RouteDTO) query.uniqueResult();
    }

    public RouteDTO getRootRouteForMediation(Integer mediationCfg){
        Query query = getSession().createQuery(
                " from RouteDTO as r, MediationConfiguration as cfg where " +
                        " r.id = cfg.rootRoute.id " +
                        " and cfg.id = :mediationCfg " +
                        " and r.rootTable = true ");
        query.setParameter("mediationCfg", mediationCfg);
        return (RouteDTO) ((Object[])query.uniqueResult())[0];
    }

    public List<RouteDTO> getRootRoutes(Integer companyId){
        Query query = getSession().createQuery(
                " from RouteDTO where " +
                        "     company.id= :companyId " +
                        " and rootTable = true ");
        query.setParameter("companyId", companyId);
        return query.list();
    }

    public boolean isRouteNameUsed(Integer companyId, String name){
        Query query = getSession().createQuery(
                "select count(route) " +
                        " from RouteDTO route where " +
                        "     lower(route.name) = :name" +
                        " and route.company.id= :companyId ");

        query.setParameter("name", name.toLowerCase());
        query.setParameter("companyId", companyId);

        return (Long)query.uniqueResult() > 0;
    }

    public RouteDTO getRouteByName(Integer companyId, String name){
        Query query = getSession().createQuery(
                    " from RouteDTO route where " +
                    "     lower(route.name) = :name" +
                    " and route.company.id= :companyId ");

        query.setParameter("name", name.toLowerCase());
        query.setParameter("companyId", companyId);

        return (RouteDTO)query.uniqueResult();
    }

    public Integer getCountRows(String tableName) {
        validateSqlIdentifier(tableName);
        Query query = getSession().createSQLQuery(String.format(COUNT_QUERY, tableName));
        Number countVal = (Number) query.uniqueResult();

        return countVal.intValue();
    }


    public SearchResultString searchDataTable(SearchCriteria criteria,RouteDTO dto) {
        RouteBeanFactory factory = new RouteBeanFactory(dto);
        ITableUpdater updater = factory.getRouteUpdaterInstance();

        return updater.search(criteria);
    }
}
