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
package com.sapienter.jbilling.server.user.permisson.db;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;

import java.util.List;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.Criteria;

public class RoleDAS extends AbstractDAS<RoleDTO> {

	public RoleDTO findByRoleTypeIdAndCompanyId(Integer roleTypeId, Integer companyId) {
		
	    Criteria criteria =getSession().createCriteria(getPersistentClass())
                                       .add(Restrictions.eq("roleTypeId", roleTypeId));
        if (null != companyId) {
            criteria.add(Restrictions.eq("company.id", companyId));
        } else {
        	criteria.add(Restrictions.isNull("company"));
        }

        return findFirst(criteria);
	}

    public Integer findDefaultCompanyId() {
        Criteria criteria = getSession().createCriteria(CompanyDTO.class)
                                        .setProjection(Projections.min("id"));

        return (Integer)criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<RoleDTO> findAllRolesByEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(RoleDTO.class)
                                        .add(Restrictions.eq("company.id", entityId));

        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<RoleDTO> findChildRolesByParentId(Integer roleParentId) {
        Criteria criteria = getSession().createCriteria(RoleDTO.class)
                                        .add(Restrictions.eq("parentRole.id", roleParentId));

        return criteria.list();
    }

    public boolean hasChildRoles(Integer roleParentId) {
        Criteria criteria = getSession().createCriteria(RoleDTO.class)
                                        .add(Restrictions.eq("parentRole.id", roleParentId))
                                        .setProjection(Projections.count("id"));

        return ((Long) criteria.uniqueResult()) > 0;
    }

    @SuppressWarnings("unchecked")
    public List<InternationalDescriptionDTO> getDescriptions(Integer roleId) {
        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_ROLE);
        Criteria criteria = getSession().createCriteria(InternationalDescriptionDTO.class)
                                        .add(Restrictions.eq("id.tableId", table.getId()))
                                        .add(Restrictions.eq("id.foreignId", roleId));

        return criteria.list();
    }
}
