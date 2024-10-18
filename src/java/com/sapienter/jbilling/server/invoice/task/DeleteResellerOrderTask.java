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
package com.sapienter.jbilling.server.invoice.task;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.InvoiceDeletedEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;

public class DeleteResellerOrderTask extends PluggableTask
        implements IInternalEventsTask {

    private static final Logger logger =
            Logger.getLogger(ApplyNegativeInvoiceToPaymentTask.class);

    @SuppressWarnings("unchecked")
	private static final Class<Event> events[] = new Class[]{
            InvoiceDeletedEvent.class
    };
    
    public Class<Event>[] getSubscribedEvents () {
        return events;
    }

	@Override
	public void process(Event event) throws PluggableTaskException {
		logger.debug("Entering DeleteInvoiceOfReseller process - event: " + event);
		
		InvoiceDeletedEvent deletedEvent = (InvoiceDeletedEvent) event;

		Integer entityId = deletedEvent.getEntityId();
		Integer orderId = deletedEvent.getInvoice().getOrderProcesses().iterator().next().getPurchaseOrder().getId();
		
		CompanyDAS companyDAS = new CompanyDAS();
		CompanyDTO company = companyDAS.find(entityId);
		
		boolean resellerExists = company.getReseller() != null;
		
		if(!resellerExists) {
			logger.debug("No reseller customer for entity: " + entityId);
			return;
		}
		logger.debug("Reseller Exists for company");
		
		deleteResellerOrdersAndInvoices(company.getReseller().getId(), orderId);
	}
	
	/**
	 * finds orders and invoices of reseller for given order and deletes them
	 * 
	 * @param reseller	: Reseller User
	 * @param invoice	: child order for which this order was generated
	 */
	private void deleteResellerOrdersAndInvoices(Integer userId, Integer orderId) {
		
		OrderBL orderBL = new OrderBL();
		InvoiceBL invoiceBL = new InvoiceBL();
		
		OrderDAS orderDAS = new OrderDAS();
		InvoiceDAS invoiceDAS = new InvoiceDAS();
		
		for (OrderDTO order : orderDAS.findOrdersByUserAndResellerOrder(userId, orderId)) {
			for (InvoiceDTO invoice : invoiceDAS.findInvoicesByOrder(order.getId())) {
				invoiceBL.set(invoice);
				invoiceBL.delete(userId);
			}
			orderBL.set(order);
			orderBL.delete(userId);
		}
	}

    @Override
    public boolean isSingleton() {
        return true;
    }
}
