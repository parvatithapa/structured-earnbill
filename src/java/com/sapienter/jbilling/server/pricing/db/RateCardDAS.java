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

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;

import java.io.File;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: 2/15/12
 * Time: 12:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class RateCardDAS extends AbstractDAS<RateCardDTO> {

    public ScrollableResults getRateTableRows(String tableName) {
    	//Prevent sql injection
        tableName = tableName.replaceAll("'", "''");
        Query query = getSession().createSQLQuery("select * from " + tableName);
        return query.scroll();
    }

    public List<RateCardDTO> getRateCardsByEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(RateCardDTO.class)
                .createAlias("company", "company")
                .add(Restrictions.eq("company.id", entityId))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

}
