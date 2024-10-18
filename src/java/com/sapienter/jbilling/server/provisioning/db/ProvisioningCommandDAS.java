/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.provisioning.db;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class ProvisioningCommandDAS
        extends AbstractDAS<ProvisioningCommandDTO> {

    public List<ProvisioningCommandDTO> findCommandsByEntityId(Integer entityId) {
        Criteria criteria = getSession().createCriteria(ProvisioningCommandDTO.class)
                .add(Restrictions.eq("entity.id", entityId));

        return criteria.list();
    }
}