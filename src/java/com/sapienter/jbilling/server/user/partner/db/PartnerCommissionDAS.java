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

import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class PartnerCommissionDAS extends AbstractDAS<PartnerCommissionLineDTO> {

    public List<InvoiceCommissionDTO> findInvoiceCommissionsByPartnerAndProcessRun(PartnerDTO partner, CommissionProcessRunDTO commissionProcessRun){
        Criteria criteria = getSession().createCriteria(InvoiceCommissionDTO.class)
                .add(Restrictions.eq("partner", partner))
                .add(Restrictions.eq("commissionProcessRun", commissionProcessRun))
                .addOrder(Order.desc("id"));

        return criteria.list();
    }

    public List<PartnerCommissionLineDTO> findByPartnerAndProcessRun(PartnerDTO partner, CommissionProcessRunDTO commissionProcessRun){
        Criteria criteria = getSession().createCriteria(PartnerCommissionLineDTO.class)
                .add(Restrictions.eq("partner", partner))
                .add(Restrictions.eq("commissionProcessRun", commissionProcessRun))
                .addOrder(Order.desc("id"));

        return criteria.list();
    }

    public List<PartnerCommissionLineDTO> findByCommission(CommissionDTO commission){
        Criteria criteria = getSession().createCriteria(PartnerCommissionLineDTO.class)
                .add(Restrictions.eq("commission", commission))
                .addOrder(Order.desc("id"));

        return criteria.list();
    }

    public List<CustomerCommissionDTO> findCustomerCommission(UserDTO user) {
        return (List<CustomerCommissionDTO>) getHibernateTemplate().findByNamedQueryAndNamedParam("CustomerCommissionDTO.findForUser", "userId", user.getId());
    }

    public List<CustomerCommissionDTO> findCustomerCommission(UserDTO user, PartnerDTO partner) {
        return (List<CustomerCommissionDTO>) getHibernateTemplate().findByNamedQueryAndNamedParam("CustomerCommissionDTO.findForUserAndPartner",
                new String[] {"userId", "partnerId"},
                new Object[] {user.getId(), partner.getId()});
    }

    public List<InvoiceCommissionDTO> findInvoiceCommissionByUser(UserDTO user){
        Criteria criteria = getSession().createCriteria(InvoiceCommissionDTO.class)
                .createAlias("invoice", "_inv")
                .add(Restrictions.eq("_inv.baseUser", user));

        return criteria.list();
    }

    public List<InvoiceCommissionDTO> findInvoiceCommissionByInvoice(Integer invoiceId){
        Criteria criteria = getSession().createCriteria(InvoiceCommissionDTO.class)
                .createAlias("invoice", "_inv")
                .add(Restrictions.eq("_inv.id", invoiceId));

        return criteria.list();
    }

    public boolean hasInvoiceCommission(Integer invoiceId){
        Criteria criteria = getSession().createCriteria(InvoiceCommissionDTO.class)
                .createAlias("invoice", "_inv")
                .add(Restrictions.eq("_inv.id", invoiceId))
                .setProjection(Projections.rowCount());

        return (Long) criteria.uniqueResult() > 0 ? true : false;
    }

    public List<CustomerCommissionDTO> findCustomerCommissionByUser(UserDTO user){
        Criteria criteria = getSession().createCriteria(CustomerCommissionDTO.class)
                .add(Restrictions.eq("user", user));

        return criteria.list();
    }
}
