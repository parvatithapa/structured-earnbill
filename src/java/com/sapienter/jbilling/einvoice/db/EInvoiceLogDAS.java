package com.sapienter.jbilling.einvoice.db;

import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class EInvoiceLogDAS extends AbstractDAS<EInvoiceLogDTO> {

	public EInvoiceLogDTO findByInvoiceId(Integer invoiceId) {
		return (EInvoiceLogDTO) getSession().createCriteria(EInvoiceLogDTO.class)
				.add(Restrictions.eq("invoiceId", invoiceId))
				.uniqueResult();
	}
}
