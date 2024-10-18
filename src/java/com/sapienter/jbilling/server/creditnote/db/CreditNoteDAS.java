package com.sapienter.jbilling.server.creditnote.db;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.creditnote.CreditNoteInvoiceMapWS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class CreditNoteDAS extends AbstractDAS<CreditNoteDTO> {

    public static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    public List<CreditNoteDTO> findAllByEntityId(Integer entityId) {
        Criteria criteria = getSession().createCriteria(CreditNoteDTO.class);
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.eq("entityId", entityId));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<CreditNoteDTO> findCreditNotesWithBalanceOldestFirst(Integer userId) {
        Criteria criteria = getSession().createCriteria(CreditNoteDTO.class).createAlias("creationInvoice", "inv");
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.eq("inv.baseUser.id", userId));
        criteria.add(Restrictions.gt("balance", BigDecimal.ZERO));
        criteria.addOrder(Order.asc("createDateTime"));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<CreditNoteDTO> findAvailableCreditNotesBalanceByUser(Integer userId) {
        // user's credit notes with balance
        Criteria criteria = getSession().createCriteria(CreditNoteDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("creationInvoice", "inv")
                .createAlias("inv.baseUser", "u")
                .add(Restrictions.eq("u.id", userId));

        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findIdsByUserLatestFirst(Integer userId, int maxResults) {
        Criteria criteria = getSession().createCriteria(CreditNoteDTO.class).createAlias("creationInvoice", "inv")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("inv.baseUser.id", userId))
                .setProjection(Projections.id())
                .addOrder(Order.desc("createDateTime"))
                .addOrder(Order.desc("id"))
                .setMaxResults(maxResults);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<CreditNoteDTO> findCreditNotesByUser(Integer userId, Integer offset, Integer limit) {
        Criteria criteria = getSession().createCriteria(CreditNoteDTO.class).createAlias("creationInvoice", "inv")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("inv.baseUser.id", userId))
                .addOrder(Order.desc("createDateTime"))
                .addOrder(Order.desc("id"));
        if(null != limit) {
            criteria = criteria.setMaxResults(limit);
        }
        if(null!= offset) {
            criteria = criteria.setFirstResult(offset);
        }
        return criteria.list();
    }

    /**
     * Fetch Credit Notes in between last 2 subscription invoices
     *
     * @param userId
     * @param from
     * @param until
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<CreditNoteDTO> findCreditNotesBetweenLastAndCurrentInvoiceDates(Integer userId, Date from, Date until) {
        Criteria criteria = getSession().createCriteria(CreditNoteDTO.class).createAlias("creationInvoice", "inv");
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.eq("inv.baseUser.id", userId));
        if (from != null) {
            criteria.add(Restrictions.gt("createDateTime", from));
        }
        criteria.add(Restrictions.le("createDateTime", until));
        criteria.addOrder(Order.desc("createDateTime"));
        return criteria.list();
    }

    private static final String FIND_CREDIT_NOTE_INVOICE_MAP_SQL =
            "SELECT id AS id, credit_note_id AS creditNoteId, invoice_id AS invoiceId, "
                    + " amount AS amount, create_datetime AS createDatetime "
                    + " FROM credit_note_invoice_map WHERE invoice_id IN (SELECT id FROM invoice WHERE "
                    + " create_datetime >= :fromDate AND create_datetime<= :toDate)";

    @SuppressWarnings("unchecked")
    public List<CreditNoteInvoiceMapWS> findCreditNoteInvoiceMapsByDate(Date fromDate, Date toDate) {
        SQLQuery sqlQuery = getSession().createSQLQuery(FIND_CREDIT_NOTE_INVOICE_MAP_SQL);
        sqlQuery.setParameter("fromDate", fromDate);
        sqlQuery.setParameter("toDate", toDate);
        sqlQuery.addScalar("id");
        sqlQuery.addScalar("creditNoteId");
        sqlQuery.addScalar("invoiceId");
        sqlQuery.addScalar("amount");
        sqlQuery.addScalar("createDatetime");
        sqlQuery.setResultTransformer(Transformers.aliasToBean(CreditNoteInvoiceMapWS.class));
        return sqlQuery.list();
    }
}
