package com.sapienter.jbilling.einvoice.plugin;

import com.sapienter.jbilling.server.payment.event.PaymentLinkedToInvoiceEvent;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.process.event.InvoicesGeneratedEvent;

/**
 * Different e invoice suvidha provider implementation will be
 * implemented using IEInvoiceProvider
 * @author Krunal Bhavsar
 *
 */
public interface IEInvoiceProvider {
	/**
	 * Generate eInvoice on eInvoice suvidha provider
	 */
	void generateEInvoice(InvoicesGeneratedEvent invoicesGeneratedEvent) throws PluggableTaskException;
	/**
	 * cancel eInvoice on eInvoice suvidha provider
	 */
	void cancelEInvoice(InvoiceDeletedEvent invoiceDeletedEvent) throws PluggableTaskException;

	void generateEInvoiceOnSuccessfulPayment(PaymentLinkedToInvoiceEvent paymentLinkedToInvoiceEvent) throws PluggableTaskException;

	String sendInvoiceToEInvoicePortal(Integer invoiceId) throws PluggableTaskException;

    String getGSTIn() throws PluggableTaskException;
}
