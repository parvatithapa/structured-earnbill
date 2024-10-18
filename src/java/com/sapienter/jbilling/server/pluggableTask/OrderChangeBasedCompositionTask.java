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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext.OrderLineCtx;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Holder;

/**
 * This task will copy all the lines on the orders and invoices to the new invoice, considering the periods involved for
 * each order, but not the fractions of perios. It will not copy the lines that are taxes. The quantity and total of
 * each line will be multiplied by the amount of periods.
 *
 * @author Emil Created on 27-Apr-2003
 */
public class OrderChangeBasedCompositionTask extends InvoiceComposition {

    private static final Comparator<OrderLineDTO> ORDER_LINE_DTO_COMPARATOR = (OrderLineDTO o1, OrderLineDTO o2) -> {
                                                                                    if(o1.getParentLine() != null && o2.getParentLine() == null){
                                                                                        return 1;
                                                                                    } else if(o1.getParentLine() == null && o2.getParentLine() != null){
                                                                                        return -1;
                                                                                    }

                                                                                    return 0;
                                                                               };

    private static final ParameterDescription INCLUDE_ZERO_INVOICE_LINE_PRODUCT = new ParameterDescription("Include Zero Invoice Line Product", false, ParameterDescription.Type.BOOLEAN);
    private static final ParameterDescription INCLUDE_ZERO_INVOICE_LINE_PLAN = new ParameterDescription("Include Zero Invoice Line Plan", false, ParameterDescription.Type.BOOLEAN);

    {
        descriptions.add(INCLUDE_ZERO_INVOICE_LINE_PRODUCT);
        descriptions.add(INCLUDE_ZERO_INVOICE_LINE_PLAN);
    }

    /**
     * Adds order note on Invoice
     * @param invoiceContext
     * @param orderContext
     */
    protected void addNoteOnInvoice(NewInvoiceContext invoiceContext, OrderDTO order) {
        if(Integer.valueOf(1).equals(order.getNotesInInvoice())) {
            invoiceContext.appendCustomerNote(order.getNotes());
        }
    }

    @Override
    public void apply (NewInvoiceContext invoiceCtx, Integer userId) throws TaskException {
        // initialize resource bundle once, if not initialized
        if (!resourceBundleInitialized) {
            initializeResourceBundleProperties(userId);
        }

        /*
         * Process each order being included in this invoice
         */
        for (NewInvoiceContext.OrderContext orderCtx : invoiceCtx.getOrders()) {

            OrderDTO order = orderCtx.order;

            addNoteOnInvoice(invoiceCtx, order);
            //Sort the lines. We want to process discounts last so that we can link them to the line receiving the discount.
            List<OrderLineDTO> orderLines = new ArrayList<>(order.getLines());
            orderLines.sort(ORDER_LINE_DTO_COMPARATOR);

            Holder<InvoiceLineDTO> newInvoiceLine = new Holder<>();
            Map<OrderLineDTO, InvoiceLineDTO> orderLineInvoiceLineMap = new HashMap<>(orderLines.size()*2);

            // Add order lines - excluding tax quotes
            for (OrderLineDTO orderLine : orderLines) {

                if (orderLine.getTypeId().equals(Constants.ORDER_LINE_TYPE_TAX_QUOTE) ||
                        skipDeletedLinePeriod(orderLine)) {
                    continue;
                }

                String assetIdentifier = getAssetIdentifiers(orderLine);

                String callIdentifier = null;
                StringMetaFieldValue dialedPhoneNumber = (StringMetaFieldValue) orderLine.getMetaField(Constants.PHONE_META_FIELD);
                if (dialedPhoneNumber != null){
                	callIdentifier = dialedPhoneNumber.getValue();
                }

                List<NewInvoiceContext.OrderLineCtx> contexts = calcOrderLineChanges(orderLine, invoiceCtx.getBillingDate());

                for (PeriodOfTime period : orderCtx.periods) {
                    for (NewInvoiceContext.OrderLineCtx orderLineCtx : contexts) {
                        PeriodOfTime adjustedPeriod = getAdjustedPeriod(orderLineCtx, period);
                        logger.debug("{} adjusted period for {} is {}", orderLineCtx.toString(), period, adjustedPeriod);
                        if (adjustedPeriod.getDaysInPeriod() != 0 || adjustedPeriod == PeriodOfTime.OneTimeOrderPeriodOfTime) {
                            newInvoiceLine.setTarget(null);
                            BigDecimal periodAmount = composeInvoiceLine(invoiceCtx, userId, null, orderLineCtx,
                                                                         adjustedPeriod, newInvoiceLine, callIdentifier,
                                                                         orderLine.getCallCounter(), assetIdentifier,
                                                                         null, allowLineWithZero(orderLineCtx));

                            logger.debug("Total Contribution {}, before adding Period Amount {}", orderCtx.totalContribution, periodAmount);
                            orderCtx.totalContribution = orderCtx.totalContribution.add(periodAmount);

                            //if the order line is a discount we want to link the line on the invoice to line receiving the discount
                            orderLineInvoiceLineMap.put(orderLine, newInvoiceLine.getTarget());
                            if(orderLine.getTypeId() == Constants.ORDER_LINE_TYPE_DISCOUNT && orderLine.getParentLine() != null) {
                                newInvoiceLine.getTarget().setParentLine(orderLineInvoiceLineMap.get(orderLine));
                            }
                        }
                    }
                }
            }
        }

        /*
         * add delegated invoices
         */
        delegateInvoices(invoiceCtx);
    }

    private PeriodOfTime getAdjustedPeriod (NewInvoiceContext.OrderLineCtx orderLineCtx, PeriodOfTime period) {
        if (period == PeriodOfTime.OneTimeOrderPeriodOfTime) {
            return period;
        }

        Date start = period.getStart();
        if ((orderLineCtx.getStartDate() != null) && orderLineCtx.getStartDate().after(start)) {
            start = orderLineCtx.getStartDate();
        }

        if ((orderLineCtx.getNextBillableDate() != null) && orderLineCtx.getNextBillableDate().after(start)) {
            start = orderLineCtx.getNextBillableDate();
        }

        Date end = period.getEnd();
        if (orderLineCtx.getEndDate() != null && orderLineCtx.getEndDate().before(end)) {
            end = orderLineCtx.getEndDate();
        }

        PeriodOfTime result = new PeriodOfTime(start, end, period.getDaysInCycle());
        if (! orderLineCtx.getPurchaseOrder().getProrateFlag() && result.getDaysInPeriod() != 0) {
            // reset to full period for non prorated orders to get correct description lines.
            result = period;
        }

        return result;
    }

    private boolean allowLineWithZero(NewInvoiceContext.OrderLineCtx orderLine) {
        Integer itemId = orderLine.getItemId();
        if (itemId == null) {
            return false;
        }

        ItemDTO itemDTO = new ItemDAS().find(itemId);
        boolean includeZeroInvoiceLinePlan = Boolean.valueOf(parameters.get(INCLUDE_ZERO_INVOICE_LINE_PLAN.getName()));
        boolean includeZeroInvoiceLineProduct = Boolean.valueOf(parameters.get(INCLUDE_ZERO_INVOICE_LINE_PRODUCT.getName()));

        return ((itemDTO.isPlan() && includeZeroInvoiceLinePlan) || (!itemDTO.isPlan() && includeZeroInvoiceLineProduct));
    }

    public List<OrderLineCtx> calcOrderLineChanges (OrderLineDTO orderLine, Date billingDate) {
        if (orderLine.getOrderChanges().isEmpty()) {
            return OrderLineCtx.fromOrderLineDTO(orderLine);
        } else {
            return OrderLineCtx.fromOrderCharges(orderLine, billingDate);
        }
    }

    /**
     * Method allows inclusion/ exclusion of deleted order line on invoice.
     * Default core plugin only charges non deleted lines.
     * @param line
     * @return
     */
    protected boolean skipDeletedLinePeriod(OrderLineDTO line) {
        return line.getDeleted() == 1; //only charge non deleted lines
    }
}
