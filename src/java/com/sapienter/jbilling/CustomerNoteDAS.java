package com.sapienter.jbilling;

import com.sapienter.jbilling.server.user.db.CustomerNoteDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class CustomerNoteDAS extends AbstractDAS<CustomerNoteDTO> {

	String FIND_BY_CUSTOMER = "SELECT c "
			+ "from CustomerNoteDTO c "
			+ "where c.company.id = :entityId "
			+ "AND c.customer.id = :customerId ";

    private static final String UPDATE_CUSTOMER_NOTES =
            "UPDATE customer_notes cn  " +
            "SET notes_in_invoice=:notesInInvoice " +
            "WHERE cn.customer_id =:customerId " +
            "AND cn.id =:customerNotesId ";

    public List<CustomerNoteDTO> findByCustomer(Integer customerId, Integer entityId) {
    	Query query = getSession().createQuery(FIND_BY_CUSTOMER);
    	query.setParameter("entityId", entityId);
    	query.setParameter("customerId", customerId);
    	return query.list();
    }

    public void excludeNotesInInvoice(Integer customerId, Integer customerNotesId,boolean notesInInvoice) {
        Query sqlQuery = getSession().createSQLQuery(UPDATE_CUSTOMER_NOTES);
        sqlQuery.setParameter("notesInInvoice", notesInInvoice);
        sqlQuery.setParameter("customerId", customerId);
        sqlQuery.setParameter("customerNotesId", customerNotesId);
        sqlQuery.executeUpdate();
    }
}
