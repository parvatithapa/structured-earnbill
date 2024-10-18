package com.sapienter.jbilling.server.payment.db;

import com.sapienter.jbilling.server.payment.PaymentTransferWS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.Date;
import java.util.List;

/**
 * @author Javier Rivero
 * @since 13/01/16.
 */
public class PaymentTransferDAS extends AbstractDAS<PaymentTransferDTO> {
    public PaymentTransferDTO getLastPaymentTransferByPaymentId(Integer paymentId) {

        Criteria criteria = getSession().createCriteria(PaymentTransferDTO.class);
        criteria.add(Restrictions.eq("payment.id", paymentId));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.addOrder(Order.desc("createDatetime"));

        return (PaymentTransferDTO)criteria.list().get(0);
    }

    /**
     * Get all payment transfers of payment
     * @param entityId
     * @param fromDate
     * @param toDate
     * @return
     */
    public List<Integer> getAllPaymentTransfersByDateRange(Integer entityId, Date fromDate, Date toDate) {
        Criteria criteria = getSession().createCriteria(PaymentTransferDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("payment", "payment")
                .createAlias("payment.baseUser", "baseUser")
                .createAlias("payment.baseUser.company", "company")
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.ge("createDatetime", fromDate))
                .add(Restrictions.lt("createDatetime", toDate))
                .addOrder(Order.desc("id"));
        return criteria.list();
    }

    /**
     * Get all payment transfers of payment
     * @param userId
     *
     * @return payment transfer list
     */
    public List<PaymentTransferWS> getAllPaymentTransfersByUserId(Integer userId) {
        Criteria criteria = getSession().createCriteria(PaymentTransferDTO.class);
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.eq("fromUserId", userId));
        criteria.addOrder(Order.desc("createDatetime"));
        return criteria.list();
    }

}
