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

package com.sapienter.jbilling.server.pluggableTask;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.invoice.NewInvoiceContext.OrderLineCtx;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

/**
 * Movius Specific Invoice composition task to support prorate and non-prorated orders invoice generation
 * on the based in order changes and order line respectively. 
 * 
 * @author Ashok Kale
 *
 */
public class MoviusInvoiceCompositionTask extends OrderChangeBasedCompositionTask {
    
    @Override
    public List<OrderLineCtx> calcOrderLineChanges (OrderLineDTO orderLine, Date billingDate) {
        if (orderLine.getOrderChanges().isEmpty() || !orderLine.getPurchaseOrder().getProrateFlagValue()) {
          return orderLine.getDeleted() == 0 ? OrderLineCtx.fromOrderLineDTO(orderLine): Collections.emptyList() ;
        } else {
            return OrderLineCtx.buildFromOrderCharges(orderLine, billingDate);
        }
    }

    @Override
    protected boolean skipDeletedLinePeriod(OrderLineDTO line) {
        return line.getDeleted() == 1 && (!line.getPurchaseOrder().getProrateFlagValue());
    }
}