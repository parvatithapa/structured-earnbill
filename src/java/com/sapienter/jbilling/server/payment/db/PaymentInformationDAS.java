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

import org.hibernate.Criteria;
import org.hibernate.Query;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * 
 * @author khobab
 *
 */
public class PaymentInformationDAS extends AbstractDAS<PaymentInformationDTO> {
	
	private final static String IS_CREDIT_CARD_SQL = 
		"select p.id from payment_information p " +
			" inner join payment_information_meta_fields_map pimf on p.id = pimf.payment_information_id " +
			" inner join meta_field_value mf on pimf.meta_field_value_id = mf.id " +
			" inner join meta_field_name mfn  on (mf.meta_field_name_id = mfn.id and mfn.field_usage = 'PAYMENT_CARD_NUMBER') " +
			" where p.id = :instrument";
	
	/**
	 * creates payment instrument without meta fields
	 * 
	 * @param dto	PaymentInformationDTO
	 * @return	PaymentInformationDTO
	 */
	public PaymentInformationDTO create(PaymentInformationDTO dto, Integer entityId){
		PaymentInformationDTO saved = new PaymentInformationDTO(dto.getProcessingOrder(), dto.getUser(), dto.getPaymentMethodType(), dto.getPaymentMethodId());
		saved.setCreateDateTime(dto.getCreateDateTime());
		saved.updatePaymentMethodMetaFieldsWithValidation(entityId, dto);
		return save(saved);
	}
	
	public boolean isCreditCard(Integer instrument) {
        Query sqlQuery = getSession().createSQLQuery(IS_CREDIT_CARD_SQL);
        sqlQuery.setParameter("instrument", instrument);
        sqlQuery.setMaxResults(1);
        Number count = (Number) sqlQuery.uniqueResult();
        return Integer.valueOf(null == count ? 0 : count.intValue()) > 0;
	}

	public Long findByAccountTypeAndPaymentMethodType(Integer accountTypeId, Integer paymentMethodTypeId) {
		Criteria criteria = getSession().createCriteria(PaymentInformationDTO.class)
				.createAlias("user", "user")
				.createAlias("user.customer", "customer")
				.createAlias("customer.accountType", "accountType")
				.add(Restrictions.eq("accountType.id", accountTypeId))
				.add(Restrictions.eq("paymentMethodType.id", paymentMethodTypeId))
				.setProjection(Projections.rowCount());
		return (Long)criteria.uniqueResult();
	}

    public boolean exists(Integer userId,Integer paymentMethodTypeId) {
         Criteria criteria = getSession().createCriteria(PaymentInformationDTO.class)
                .createAlias("user", "user")
                .add(Restrictions.eq("user.id", userId))
                .add(Restrictions.eq("paymentMethodType.id", paymentMethodTypeId))
                .setProjection(Projections.rowCount());
        return (criteria.uniqueResult() != null && ((Long) criteria.uniqueResult()) > 0);
    }
    
    String UPDATE_PAYMENTS_WITH_CREDIT_CARD_ID_NULL = "update payment set credit_card_id = null where credit_card_id =:creditCardId";
    public void updatePayment(Integer creditCardId ) {
    	 Query sqlQuery = getSession().createSQLQuery(UPDATE_PAYMENTS_WITH_CREDIT_CARD_ID_NULL);
    	 sqlQuery.setParameter("creditCardId", creditCardId);
    	 sqlQuery.executeUpdate();
	}
}
