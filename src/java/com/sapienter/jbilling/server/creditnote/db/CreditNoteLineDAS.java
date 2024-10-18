package com.sapienter.jbilling.server.creditnote.db;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;


public class CreditNoteLineDAS extends AbstractDAS<CreditNoteLineDTO> {
	
    /**
     * Fetch Credit Note Lines in between last 2 subscription invoices
     * @param userId
     * @param from
     * @param until
     * @return
     */
	public List<CreditNoteLineDTO> findCreditNoteLinesBetweenLastAndCurrentInvoiceDates(Integer userId, Date from, Date until) {
		Criteria criteria = getSession().createCriteria(CreditNoteLineDTO.class)
				.createAlias("creditNoteDTO", "creditNote")
				.createAlias("creditNote.creationInvoice", "inv");
		criteria.add(Restrictions.eq("creditNote.deleted", 0));
		criteria.add(Restrictions.eq("inv.deleted", 0));
		criteria.add(Restrictions.eq("inv.baseUser.id", userId));
		if(from != null) {
			criteria.add(Restrictions.gt("creditNote.createDateTime", from));
        }
		criteria.add(Restrictions.le("creditNote.createDateTime", until));
		criteria.addOrder(Order.desc("creditNote.createDateTime"));
		return criteria.list();
	}

}
