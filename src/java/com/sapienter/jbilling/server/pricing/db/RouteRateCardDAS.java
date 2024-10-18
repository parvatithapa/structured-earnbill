package com.sapienter.jbilling.server.pricing.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;

public class RouteRateCardDAS extends AbstractDAS<RouteRateCardDTO> {
	
    private static Pattern ValidSqlIdentifierPattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final String MAX_QUERY = "SELECT MAX(CHAR_LENGTH(%s)) FROM %s";
    private static final String MIN_QUERY = "SELECT MIN(CHAR_LENGTH(%s)) FROM %s";
    private static final String SELECT_QUERY = "SELECT * FROM %s";

    private void validateSqlIdentifier (String sqlIdentifierName) {
        if (!ValidSqlIdentifierPattern.matcher(sqlIdentifierName).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier (table or column name, etc...): " + sqlIdentifierName);
        }
    }

    public ScrollableResults getRouteRateCardTableRows(String tableName) {
        validateSqlIdentifier(tableName);
        Query query = getSession().createSQLQuery(String.format(SELECT_QUERY, tableName));

		return query.scroll();
	}
    
    /**
     * Gets the longest value in the given rate card table for the given matching field.
     * @param tableName
     * @param matchingField
     * @return
     */
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

    @SuppressWarnings("unchecked")
    public List<RouteRateCardDTO> getRouteRateCardsByEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(RouteRateCardDTO.class)
                                        .createAlias("company", "company")
                                        .add(Restrictions.eq("company.id", entityId))
                                        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return (List<RouteRateCardDTO>) criteria.list();
    }

    @SuppressWarnings("unchecked")
    public RouteRateCardDTO getRouteRateCardByNameAndEntity(String name,Integer entityId) {
        Criteria criteria = getSession().createCriteria(RouteRateCardDTO.class)
                                        .add(Restrictions.eq("name", name))
                                        .createAlias("company", "company")
                                        .add(Restrictions.eq("company.id", entityId))
                                        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return (RouteRateCardDTO) criteria.uniqueResult();
    }
}
