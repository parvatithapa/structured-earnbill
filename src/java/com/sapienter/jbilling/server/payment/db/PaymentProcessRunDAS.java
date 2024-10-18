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

package com.sapienter.jbilling.server.payment.db;

import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

/**
 * 
 * @author mazhar
 *
 */
public class PaymentProcessRunDAS extends AbstractDAS<PaymentProcessRunDTO>{

	public PaymentProcessRunDTO findByProcessId(Integer billingProcessId){
		DetachedCriteria criteria = DetachedCriteria.forClass(PaymentProcessRunDTO.class);
		criteria.add(Restrictions.eq("billingProcessId", billingProcessId));
		List<PaymentProcessRunDTO> dtos = (List<PaymentProcessRunDTO>) getHibernateTemplate().findByCriteria(criteria);
		if(dtos != null && dtos.size()>0){
			return dtos.get(0);
		}
		return null;
	}
}
