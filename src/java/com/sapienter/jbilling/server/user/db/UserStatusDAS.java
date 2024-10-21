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
package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.adennet.AdennetConstants;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.io.Serializable;
import java.util.List;

public class UserStatusDAS extends AbstractDAS<UserStatusDTO> {


    public UserStatusDTO findByValueIfSingle(Integer statusValue) {
        if (statusValue == null) {
            return null;
        }
        List<UserStatusDTO> statuses = findByCriteria(Restrictions.eq("statusValue", statusValue));
        if (statuses.size() == 1) {
            return statuses.get(0);
        } else {
            // search by ID is needed for unique
            return null;
        }
    }

    public List<UserStatusDTO> findByEntityId(Integer entityId) {
        Criteria crit = getSession().createCriteria(UserStatusDTO.class)
                .createAlias("ageingEntityStep", "aes", CriteriaSpecification.LEFT_JOIN);

        Disjunction disjunction = Restrictions.disjunction();
        disjunction.add(Restrictions.idEq(UserDTOEx.STATUS_ACTIVE));
        disjunction.add(Restrictions.idEq(AdennetConstants.USER_STATUS_DEACTIVATED));
        disjunction.add(Restrictions.eq("aes.company.id", entityId));

        crit.add(disjunction);
        crit.addOrder(Order.asc("id"));

        return crit.list();
    }

    @Override
    public UserStatusDTO findNow(Serializable statusId) {
        return find(statusId);
    }
    
    public UserStatusDTO findByDescription(String description, Integer languageId) {
    	SQLQuery query = getSession().createSQLQuery("select * from user_status where id = "
    			+ "( select foreign_id from international_description where table_id = 9 and content = :description and language_id = :languageId) ");
    	query.addEntity(UserStatusDTO.class);
    	query.setParameter("description", description);
    	query.setParameter("languageId", languageId);
    	return UserStatusDTO.class.cast(query.uniqueResult());
    }
}
