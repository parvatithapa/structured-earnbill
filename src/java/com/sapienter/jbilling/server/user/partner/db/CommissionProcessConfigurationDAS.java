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
import org.hibernate.criterion.Restrictions;

public class CommissionProcessConfigurationDAS extends AbstractDAS<CommissionProcessConfigurationDTO> {

    public CommissionProcessConfigurationDTO findByEntity(CompanyDTO entity) {
        Criteria criteria = getSession().createCriteria(CommissionProcessConfigurationDTO.class);
        criteria.add(Restrictions.eq("entity", entity));
        return (CommissionProcessConfigurationDTO) criteria.uniqueResult();
    }

}
