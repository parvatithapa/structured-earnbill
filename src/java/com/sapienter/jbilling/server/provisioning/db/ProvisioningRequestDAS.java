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

import com.sapienter.jbilling.server.provisioning.ProvisioningRequestStatus;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class ProvisioningRequestDAS
        extends AbstractDAS<ProvisioningRequestDTO> {

    public List<ProvisioningRequestDTO> findUnprocessedRequestsByCommandId(Integer commandId) {

        Criteria criteria = getSession().createCriteria(ProvisioningRequestDTO.class)
                .add(Restrictions.eq("requestStatus", ProvisioningRequestStatus.SUBMITTED))
                .add(Restrictions.eq("provisioningCommand.id", commandId));
        return criteria.list();

    }

    public List<ProvisioningRequestDTO> findRequestsByCommandId(Integer commandId) {
        Criteria criteria = getSession().createCriteria(ProvisioningRequestDTO.class)
                .add(Restrictions.eq("provisioningCommand.id", commandId));
        return criteria.list();
    }

    public ProvisioningRequestDTO findByIdentifier(String identifier) {
        Criteria criteria = getSession().createCriteria(ProvisioningRequestDTO.class)
                .add(Restrictions.eq("identifier", identifier))
                .setMaxResults(1);

        return findFirst(criteria);
    }

    public Integer getRequestsCountByCommandId(Integer commandId) {

        Criteria criteria = getSession().createCriteria(ProvisioningRequestDTO.class)
                .add(Restrictions.eq("provisioningCommand.id", commandId));

        return criteria.list().size();
    }

    public Integer getRequestByCommandIdAndStatus(Integer commandId, ProvisioningRequestStatus status) {

        Criteria criteria = getSession().createCriteria(ProvisioningRequestDTO.class)
                .add(Restrictions.eq("requestStatus", status))
                .add(Restrictions.eq("provisioningCommand.id", commandId));

        return criteria.list().size();
    }
}
