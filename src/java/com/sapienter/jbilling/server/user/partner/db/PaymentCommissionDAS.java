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

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class PaymentCommissionDAS extends AbstractDAS<PaymentCommissionDTO> {

    public List<PaymentCommissionDTO> findByInvoiceId(Integer invoiceId) {
        Criteria criteria = getSession().createCriteria(PaymentCommissionDTO.class)
                .add(Restrictions.eq("invoice.id", invoiceId))
                .addOrder(Order.asc("id"));

        return criteria.list();
    }

    public List<Integer> findInvoiceIdsByPartner(PartnerDTO partner) {
        Criteria criteria = getSession().createCriteria(PaymentCommissionDTO.class)
                .createAlias("invoice", "_invoice")
                .createAlias("_invoice.baseUser", "_baseUser")
                .createAlias("_baseUser.customer", "_customer")
                .createAlias("_customer.partners", "_partner")
                .add(Restrictions.eq("_partner.id", partner.getId()))
                .setProjection(Property.forName("_invoice.id"));
        return criteria.list();
    }

    public List<PaymentCommissionDTO> findByPartner(PartnerDTO partner) {
        Criteria criteria = getSession().createCriteria(PaymentCommissionDTO.class)
                .createAlias("invoice", "_invoice")
                .createAlias("_invoice.baseUser", "_baseUser")
                .createAlias("_baseUser.customer", "_customer")
                .createAlias("_customer.partners", "_partner")
                .add(Restrictions.eq("_partner.id", partner.getId()));
        return criteria.list();
    }

    public void deleteAllForPartner(Integer partnerId) {
        getHibernateTemplate().bulkUpdate("delete PaymentCommissionDTO where invoice.id in " +
                "(select i.id from InvoiceDTO i join i.baseUser.customer.partners as p where p.id = ?)", partnerId);
    }

}
