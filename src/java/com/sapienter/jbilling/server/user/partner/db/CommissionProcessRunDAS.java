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
package com.sapienter.jbilling.server.user.partner.db;

import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class CommissionProcessRunDAS extends AbstractDAS<CommissionProcessRunDTO>{

    public CommissionProcessRunDTO findLatestByDate(CompanyDTO entity) {
        Criteria criteria = getSession().createCriteria(CommissionProcessRunDTO.class);
        criteria.add(Restrictions.eq("entity", entity));
        criteria.add(Restrictions.eq("errorCount", 0));
        criteria.addOrder(Order.desc("periodEnd"));
        return findFirst(criteria);
    }

    public List<CommissionProcessRunDTO> findAllByEntity(CompanyDTO entity){
        Criteria criteria = getSession().createCriteria(CommissionProcessRunDTO.class);
        criteria.add(Restrictions.eq("entity", entity));
        criteria.addOrder(Order.desc("id"));

        return criteria.list();
    }
}
