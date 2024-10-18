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

import com.sapienter.jbilling.server.util.db.AbstractDAS;


import org.hibernate.Query;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.SQLQuery;

import java.util.List;

/**
 * 
 * @author khobab
 *
 */
public class PaymentMethodTypeDAS extends AbstractDAS<PaymentMethodTypeDTO> {
	
	public PaymentMethodTypeDTO getPaymentMethodTypeByTemplate(String templateName, Integer entity) {
		// I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
        		.createAlias("entity", "e")
                 	.add(Restrictions.eq("e.id", entity))
                .createAlias("paymentMethodTemplate", "pmt")
                    .add(Restrictions.eq("pmt.templateName", templateName));
        return findFirst(criteria);
	}
	
	@SuppressWarnings("unchecked")
    public PaymentMethodTypeDTO findFirst(Query query) {
        query.setFirstResult(0).setMaxResults(1);
        return (PaymentMethodTypeDTO) query.uniqueResult();
    }
	
	public Integer countInstrumentsAttached(Integer paymentMethodId) {
		List list = getSession().createCriteria(PaymentInformationDTO.class)
			.add(Restrictions.eq("paymentMethodType.id", paymentMethodId))
			.list();
		
		if(list != null) {
			return list.size();
		}
		return 0;
	}

    public List<PaymentMethodTypeDTO> findByMethodName(String methodName, Integer entityId) {
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
                .add(Restrictions.eq("methodName", methodName).ignoreCase())
                .createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (List<PaymentMethodTypeDTO>) criteria.list();
    }
    public List<PaymentMethodTypeDTO> findByAllAccountType(Integer entityId) {
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
                .add(Restrictions.eq("allAccountType", true))
                .createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (List<PaymentMethodTypeDTO>) criteria.list();
    }

    public List<PaymentMethodTypeDTO> findAllByEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
                .createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (List<PaymentMethodTypeDTO>) criteria.list();
    }

    public List<PaymentMethodTypeDTO> getAllPaymentMethodTypes(Integer entityId) {
        return findAllByEntity(entityId);
    }

    public PaymentMethodTypeDTO findByPaymentMethodTypeId(Integer entityId, Integer paymentMethodTypeId) {
        Criteria criteria = getSession().createCriteria(PaymentMethodTypeDTO.class)
                .add(Restrictions.eq("id", paymentMethodTypeId))
                .createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));
        return findFirst(criteria);
    }
}
