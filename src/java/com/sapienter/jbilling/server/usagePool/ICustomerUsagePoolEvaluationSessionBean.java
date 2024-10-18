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
package com.sapienter.jbilling.server.usagePool;

import java.util.Date;

/**
 * ICustomerUsagePoolEvaluationSessionBean
 * A simple interface with a trigger method.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public interface ICustomerUsagePoolEvaluationSessionBean {

	public void trigger(Integer entityId, Date runDate);
	
}
