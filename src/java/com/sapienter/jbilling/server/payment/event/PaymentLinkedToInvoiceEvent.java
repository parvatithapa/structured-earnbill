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

package com.sapienter.jbilling.server.payment.event;

import java.math.BigDecimal;

import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;

public class PaymentLinkedToInvoiceEvent extends AbstractPaymentEvent {
	private InvoiceDTO invoice;
	private BigDecimal totalPaid;

	public PaymentLinkedToInvoiceEvent (Integer entityId, PaymentDTOEx payment, InvoiceDTO invoice, BigDecimal totalPaid) {
		super(entityId, payment);
		this.invoice = invoice;
		this.totalPaid = totalPaid;
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	public InvoiceDTO getInvoice () {
		return invoice;
	}

	public BigDecimal getTotalPaid () {
		return totalPaid;
	}

	@Override
	public String toString() {
		Integer paymentId = (getPayment() != null) ? getPayment().getId() : null;
		Integer invoiceId = (getInvoice() != null) ? getInvoice().getId() : null;

		return "PaymentLinkedToInvoiceEvent{"
		+ "paymentId=" + paymentId
		+ ", amount=" + getPayment().getAmount()
		+ ", invoiceId=" + invoiceId
		+ "}";
	}

}
