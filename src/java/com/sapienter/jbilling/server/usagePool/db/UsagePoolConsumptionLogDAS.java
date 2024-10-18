/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool.db;

import java.math.BigDecimal;
import java.util.List;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Projections;

/**
 * UsagePoolConsumptionLogDAS
 * This DAS has a finder method to fetch percentage consumption for a given customer usage pool id.
 * @author Amol Gadre
 * @since 10-Feb-2014
 */

public class UsagePoolConsumptionLogDAS extends AbstractDAS<UsagePoolConsumptionLogDTO> {

	public BigDecimal findPercentageConsumptionBycustomerUsagePoolId(Integer customerUsagePoolId, Integer actionId)
	{
		Criteria criteria = getSession().createCriteria(UsagePoolConsumptionLogDTO.class)
        		.add(Restrictions.eq("customerUsagePool.id", customerUsagePoolId))
                .add(Restrictions.eq("actionExecuted", "" + actionId))
        		.setProjection(Projections.max("percentageConsumption"));
		BigDecimal percentageConsumption = (BigDecimal)criteria.uniqueResult();
		
        return null != percentageConsumption ? percentageConsumption :null;
	}
	
}
