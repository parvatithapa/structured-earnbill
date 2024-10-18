/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.util.db;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class CurrencyDAS extends AbstractDAS<CurrencyDTO> {

    private static final String HQL_CURRENCY_BY_CODE;
    private static final String CODE_BY_ID_SQL =
            "SELECT code " +
            "FROM currency " +
            "WHERE id = :id";

    static {
        HQL_CURRENCY_BY_CODE = "SELECT a FROM CurrencyDTO a WHERE a.code = :code";
    }

    public boolean findAssociationExistsForCurrency(Integer currencyId, Class associationClass, String currencyFieldName) {
		
		Criteria criteria =getSession().createCriteria(associationClass)
                            .add(Restrictions.eq(currencyFieldName + ".id", currencyId));
        
        return findFirst(criteria) != null;
	}

    public List<CurrencyDTO> findCurrenciesSortByDescription(Integer entityId) {
        final String hql = "select a" +
                " from CurrencyDTO a, InternationalDescriptionDTO b, JbillingTable c, CompanyDTO e " +
                "where e.id = :entityId " +
                " and b.id.tableId = c.id " +
                "and c.name = 'currency'    and b.id.foreignId = a.id " +
                "and b.id.languageId = e.language.id    and b.id.psudoColumn = 'description' " +
                "order by b.content";
        Query query = getSession().createQuery(hql);
        query.setParameter("entityId", entityId);
        return (List<CurrencyDTO>) query.list();
	}
    
    public CurrencyDTO findCurrencyByCode(String code) {
    	Query query = getSession().createQuery(HQL_CURRENCY_BY_CODE);
        query.setParameter("code", code);
        return (CurrencyDTO) query.uniqueResult();
    }

    public String findCurrencyCodeById(Integer id) {
        SQLQuery sqlQuery = getSession().createSQLQuery(CODE_BY_ID_SQL);
        sqlQuery.setParameter("id", id);
        return (String) sqlQuery.uniqueResult();
    }
}
