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

package com.sapienter.jbilling.server.pluggableTask.admin;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class PluggableTaskTypeCategoryDAS extends AbstractDAS<PluggableTaskTypeCategoryDTO> {

	public PluggableTaskTypeCategoryDTO findByInterfaceName(String interfaceName){
		Criteria criteria = getSession().createCriteria(PluggableTaskTypeCategoryDTO.class)
				.add(Restrictions.eq("interfaceName", interfaceName));
		return (PluggableTaskTypeCategoryDTO)criteria.uniqueResult();
	}
}
