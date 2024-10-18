package com.sapienter.jbilling.server.payment.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * PaymentProcessorUnavailableDAS class
 * 
 * @author Leandro Bagur 
 * @since 24/11/17
 */
public class PaymentProcessorUnavailableDAS extends AbstractDAS<PaymentProcessorUnavailableDTO> { 

    @SuppressWarnings("unchecked")
    public List<PaymentProcessorUnavailableDTO> findByEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(PaymentProcessorUnavailableDTO.class)
            .add(Restrictions.eq("entityId", entityId));
        return criteria.list();
    }
    
}
