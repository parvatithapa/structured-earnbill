package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.process.PeriodOfTime;

import java.math.BigDecimal;

public class OrderLineBasedCompositionTask extends InvoiceComposition {

    public void apply(NewInvoiceContext invoiceCtx, Integer userId) throws TaskException {
        // initialize resource bundle once, if not initialized
        if (!resourceBundleInitialized) {
            initializeResourceBundleProperties(userId);
        }

        /*
         * Process each order being included in this invoice
         */
        for (NewInvoiceContext.OrderContext orderCtx : invoiceCtx.getOrders()) {

            OrderDTO order = orderCtx.order;
            BigDecimal orderContribution = BigDecimal.ZERO;

            if (order.getNotesInInvoice() != null && order.getNotesInInvoice() == 1) {
                invoiceCtx.appendCustomerNote(order.getNotes());
            }

            // Add order lines - excluding taxes
            for (OrderLineDTO orderLine : order.getLines()) {
                if (orderLine.getDeleted() == 1) {
                    continue;
                }

                for (PeriodOfTime period : orderCtx.periods) {
                    orderContribution = orderContribution.add(composeInvoiceLine(invoiceCtx, userId, getAmountByType(orderLine, period),
                                                                                 orderLine, period, null, null, 0L, null, null, true));
                }
            }
        }

        /*
         * add delegated invoices
         */
        delegateInvoices(invoiceCtx);
    }
}
