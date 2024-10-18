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

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class CommissionDAS extends AbstractDAS<CommissionDTO> {
    
    public List<CommissionDTO> findAllByProcessRun(CommissionProcessRunDTO commissionProcessRun, Integer entityId){
        Criteria criteria = getSession().createCriteria(CommissionDTO.class)
                .add(Restrictions.eq("commissionProcessRun", commissionProcessRun))
                .createAlias("commissionProcessRun", "_commissionProcessRun")
                .createAlias("_commissionProcessRun.entity", "_entity")
                .add(Restrictions.eq("_entity.id", entityId))
                .addOrder(Order.desc("id"));

        return criteria.list();
    }
}
