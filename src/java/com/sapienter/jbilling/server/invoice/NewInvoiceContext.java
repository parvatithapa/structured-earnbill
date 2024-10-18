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

/*
 * Created on 20-Apr-2003
 *
 */
package com.sapienter.jbilling.server.invoice;

import java.math.BigDecimal;
import java.util.*;

import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.order.TimePeriod;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;

public class NewInvoiceContext extends InvoiceDTO {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NewInvoiceContext.class));

    public class OrderContext {
        public final OrderDTO           order;
        public final List<PeriodOfTime> periods;
        public BigDecimal               totalContribution = BigDecimal.ZERO;

        //optional - future
        public Date                     orderPeriodStart;
        public Date                     orderPeriodEnd;

        public OrderContext (final OrderDTO order, final List<PeriodOfTime> periods) {
            this.order = order;
            this.periods = periods;
        }

        public OrderContext (final OrderDTO order, final Date start, final Date end, final List<PeriodOfTime> periods) {
            this.order = order;
            this.periods = periods;
            this.orderPeriodStart= start;
            this.orderPeriodEnd= end;
        }

    }

    /**
     * Utility class to fill data during invoice composition task
     *
     * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
     *
     */
    public static class OrderLineCtx extends OrderLineDTO {
        // order
        // quantity
        // price
        // amount
        // description
        private final Integer typeId;
        private final Integer itemId;
        private Date          nextBillableDate;

        public OrderLineCtx (OrderDTO order, Integer itemId, Integer typeId) {
            setPurchaseOrder(order);
            this.itemId = itemId;
            this.typeId = typeId;
        }

        @Override
        public Integer getItemId () {
            return this.itemId;
        }

        @Override
        public Integer getTypeId () {
            return this.typeId;
        }

        public Date getNextBillableDate () {
            return nextBillableDate;
        }

        public static List<OrderLineCtx> fromOrderLineDTO (OrderLineDTO etalon) {
            OrderLineCtx newOrderLineCtx = new OrderLineCtx(etalon.getPurchaseOrder(), etalon.getItemId(),
                    etalon.getTypeId());
            newOrderLineCtx.setId(etalon.getId());
            newOrderLineCtx.setParentLine(etalon.getParentLine());
            newOrderLineCtx.setDescription(etalon.getDescription());
            newOrderLineCtx.setPrice(etalon.getPrice());
            newOrderLineCtx.setQuantity(etalon.getQuantity());
            newOrderLineCtx.setAmount(etalon.getAmount());
            newOrderLineCtx.setPercentage(etalon.isPercentage());
            newOrderLineCtx.nextBillableDate = etalon.getPurchaseOrder().getNextBillableDay();

            return Collections.singletonList(newOrderLineCtx);
        }

        public static List<OrderLineCtx> fromOrderCharges (OrderLineDTO orderLine, Date billingDate) {
            List<OrderLineCtx> contexts = new ArrayList<OrderLineCtx>(1);
            List<OrderChangeCtx> changes = recalcOrderChanges (orderLine, billingDate);
            
            boolean firstTime = true;
            BigDecimal priceBeforeChange = BigDecimal.ZERO;
            for (OrderChangeCtx change : changes) {
                BigDecimal price = change.getPrice();
                BigDecimal deltaQuantity = change.getQuantity();

                // if quantity was changed create [old price, delta quantity] context
                if ((BigDecimal.ZERO.compareTo(deltaQuantity) != 0) && !firstTime) {

                    OrderLineCtx newOrderLineCtx = createOrderLineCtx(orderLine, change);

                    newOrderLineCtx.setPrice(priceBeforeChange);
                    newOrderLineCtx.setQuantity(deltaQuantity);
                    if (orderLine.isPercentage()) {
                        newOrderLineCtx.setAmount(orderLine.getAmount());
                        newOrderLineCtx.setQuantity(BigDecimal.ONE);
                    } else {
                        newOrderLineCtx.setAmount(deltaQuantity.multiply(priceBeforeChange));
                    }
                    newOrderLineCtx.setPercentage(change.isPercentage());
                    contexts.add(newOrderLineCtx);
                }
                Integer entityId = orderLine.getPurchaseOrder().getUser().getEntity().getId();

                // This condition is to validate if the order is using the Teaser Pricing Model
                if (!PriceModelBL.isAllowedToUpdateOrderChange(change.getItem(), entityId, change.getStartDate())) {
                    OrderLineCtx newOrderLineCtx = createOrderLineCtx(orderLine, change);
                    newOrderLineCtx.setPrice(price);
                    newOrderLineCtx.setQuantity(change.quantityAfterChange);
                    newOrderLineCtx.setAmount(price.multiply(change.quantityAfterChange));
                    contexts.add(newOrderLineCtx);
                } else {
                    // if price was changed create [delta price, new quantity] context
                    if (price.compareTo(priceBeforeChange) != 0 || firstTime) {
                        BigDecimal deltaPrice = price.subtract(priceBeforeChange);
                        BigDecimal quantity = change.quantityAfterChange;

                        OrderLineCtx newOrderLineCtx = createOrderLineCtx(orderLine, change);
                        newOrderLineCtx.setPrice(deltaPrice);
                        newOrderLineCtx.setQuantity(quantity);
                        if(orderLine.isPercentage()) {
                            newOrderLineCtx.setAmount(orderLine.getAmount());
                            newOrderLineCtx.setQuantity(BigDecimal.ONE);
                        } else {
                             newOrderLineCtx.setAmount(deltaPrice.multiply(quantity));
                        }

                        contexts.add(newOrderLineCtx);
                    }

                    priceBeforeChange = price;
                }
                firstTime = false;
            }
            return contexts;
        }

        /**
         * Build new OrderLineCtx from order changes.
         * 
         * @param orderLine
         * @param billingDate
         * @return
         */
        public static List<OrderLineCtx> buildFromOrderCharges (OrderLineDTO orderLine, Date billingDate) {
            List<OrderLineCtx> contexts = new ArrayList<>(1);
            List<OrderChangeCtx> changes = recalcOrderChanges (orderLine, billingDate);

            for (OrderChangeCtx change : changes) {
                BigDecimal price = change.getPrice();
                BigDecimal deltaQuantity = change.getQuantity();


                // if quantity was changed create [old price, delta quantity] context
                if (BigDecimal.ZERO.compareTo(deltaQuantity) != 0) {

                    OrderLineCtx newOrderLineCtx = createOrderLineCtx(orderLine, change);

                    newOrderLineCtx.setPrice(price);
                    newOrderLineCtx.setQuantity(deltaQuantity);
                    if (orderLine.isPercentage()) {
                        newOrderLineCtx.setAmount(orderLine.getAmount());
                        newOrderLineCtx.setQuantity(BigDecimal.ONE);
                    } else {
                        newOrderLineCtx.setAmount(deltaQuantity.multiply(price));
                    }
                    newOrderLineCtx.setPercentage(change.isPercentage());
                    contexts.add(newOrderLineCtx);
                }
                Integer entityId = orderLine.getPurchaseOrder().getUser().getEntity().getId();

                // This condition is to validate if the order is using the Teaser Pricing Model
                if (!PriceModelBL.isAllowedToUpdateOrderChange(change.getItem(), entityId, change.getStartDate())) {
                    OrderLineCtx newOrderLineCtx = createOrderLineCtx(orderLine, change);
                    newOrderLineCtx.setPrice(price);
                    newOrderLineCtx.setQuantity(change.quantityAfterChange);
                    newOrderLineCtx.setAmount(price.multiply(change.quantityAfterChange));
                    contexts.add(newOrderLineCtx);
                }
            }
            return contexts;
        }

        private static OrderLineCtx createOrderLineCtx (OrderLineDTO orderLine, OrderChangeDTO change) {
            OrderLineCtx newOrderLineCtx = new OrderLineCtx(orderLine.getPurchaseOrder(), orderLine.getItemId(),
                    orderLine.getTypeId());
            newOrderLineCtx.setDescription(orderLine.getDescription());
            newOrderLineCtx.setPercentage(orderLine.isPercentage());
            newOrderLineCtx.setStartDate(change.getStartDate());
            newOrderLineCtx.setEndDate(change.getEndDate());
            newOrderLineCtx.nextBillableDate = change.getNextBillableDate();
            newOrderLineCtx.setId(orderLine.getId());
            return newOrderLineCtx;
        }

        private static List<OrderChangeCtx> recalcOrderChanges (OrderLineDTO orderLine, Date billingDate) {
            Date billingUntil;
            OrderDTO order = orderLine.getPurchaseOrder();
            UserBL userBL = new UserBL(order.getUser());
            Date nextInvoiceDate = order.getUser().getCustomer().getNextInvoiceDate();

            if (order.getBillingTypeId().compareTo(Constants.ORDER_BILLING_POST_PAID) == 0 ) {
                billingUntil = nextInvoiceDate;
            } else {
                billingUntil = userBL.getBillingUntilDate(nextInvoiceDate, billingDate);
            }
            LOG.debug("Calculating billing until for user, [%d] is billingUntil [%s] for filter order changes.", order.getUserId(), billingUntil);

            List<OrderChangeDTO> sourceChanges = orderLine.getOrderChangesSortedByCreateDateTime();
            List<OrderChangeDTO> filteredChanges = new ArrayList<>();
            for (OrderChangeDTO changeDTO : sourceChanges) {
                if (!changeDTO.getStartDate().after(billingUntil) &&
                        changeDTO.getStatus().getApplyToOrder().equals(ApplyToOrder.YES)){
                    filteredChanges.add(changeDTO);
                }
            }
            List<OrderChangeCtx> result = new ArrayList<OrderChangeCtx>(filteredChanges.size());

            BigDecimal quantityBeforeChange = BigDecimal.ZERO;
            for (OrderChangeDTO change : filteredChanges) {
                OrderChangeCtx ctx = new OrderChangeCtx(change);
                quantityBeforeChange = ctx.quantityAfterChange = quantityBeforeChange.add(ctx.getQuantity());
                result.add(ctx);
            }
            Collections.sort(result, OrderLineChangeDTOStartDateComparator);
            LOG.debug("Total Number of Filtered OrderChanges [%d] ", filteredChanges.size());
            return result;
        }
    }

    private static class OrderChangeCtx extends OrderChangeDTO {
        BigDecimal quantityAfterChange;

        OrderChangeCtx (OrderChangeDTO etalon) {
            this.setStartDate(etalon.getStartDate());
            this.setEndDate(etalon.getEndDate());
            this.setNextBillableDate(etalon.getNextBillableDate());
            this.setCreateDatetime(etalon.getCreateDatetime());
            this.setPrice(etalon.getPrice() != null ? etalon.getPrice() : BigDecimal.ZERO);
            this.setQuantity(etalon.getQuantity() != null ? etalon.getQuantity() : BigDecimal.ZERO);
            this.setPercentage(etalon.isPercentage());
            this.setItem(etalon.getItem());
        }
    }

    private List<OrderContext>   ordersContexts = new ArrayList<OrderContext>();
    private Set<InvoiceDTO>      invoices       = new HashSet<InvoiceDTO>();
    private List<InvoiceLineDTO> resultLines    = new ArrayList<InvoiceLineDTO>();

    private Integer              entityId;
    private Date                 billingDate;
    private TimePeriod           dueDatePeriod;
    boolean                      dateIsRecurring;

    public void setDate (Date newDate) {
        billingDate = newDate;
    }

    /**
     * Use the earliest day, with priority to recurring orders Used only for the parameter invoice date = begining of
     * period invoiced
     * 
     * @param newDate
     * @param isRecurring
     */
    public void setDate (Date newDate, boolean isRecurring) {
        if (billingDate == null) {
            billingDate = newDate;
            dateIsRecurring = isRecurring;
        } else if (dateIsRecurring) {
            if (newDate.before(billingDate) && isRecurring) {
                billingDate = newDate;
            }
        } else {
            if (!isRecurring && billingDate.before(newDate)) {
            } else {
                billingDate = newDate;
                dateIsRecurring = isRecurring;
            }
        }
    }

    public List<OrderContext> getOrders () {
        return ordersContexts;
    }

    public void addOrder (OrderDTO order, Date start, Date end, List<PeriodOfTime> periods) {
        LOG.debug("Adding order %d to new invoice", order.getId());
        if (start != null && end != null && start.after(end)) {
            // how come it starts after it ends ???
            throw new SessionInternalError("Adding " + "order " + order.getId() + " with a period that"
                    + " starts after it ends:" + start + " " + end);
        }
        ordersContexts.add(new OrderContext(order, start, end, periods));
    }

    public void addInvoice (InvoiceDTO line) {
        invoices.add(line);
    }

    public Set<InvoiceDTO> getInvoices () {
        return invoices;
    }

    public List<InvoiceLineDTO> getResultLines () {
        return resultLines;
    }

    public void addResultLine (InvoiceLineDTO line) {
        resultLines.add(line);
    }

    /**
     *
     * @return If this object holds any order lines or invoice lines, therefore if it makes sense to apply invoice
     *         composition tasks to it.
     */
    public boolean isEmpty () {
        return ordersContexts.isEmpty() && invoices.isEmpty();
    }

    /**
     * @return If after the invoice composition tasks lines have been inserted in the resultLines vector.
     */
    public boolean areLinesGeneratedEmpty () {
        return resultLines.isEmpty();
    }

    public String validate () {
        String message = null;

        if (getDueDate() == null) {
            // due date is mandaroty
            message = "Due date is null";
        } else if (getDueDate().before(getBillingDate())) {
            // the due date has to be after the invoice's billing date
            message = "Due date has to be past the billing date";
        }

        return message;
    }

    /**
     * @return
     */
    public Date getBillingDate () {
        return billingDate;
    }

    /**
     * @param date
     */
    public void setBillingDate (Date date) {
        billingDate = date;
    }

    public void calculateTotal () {
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLineDTO line : resultLines) {
            total = total.add(line.getAmount());
        }
        setTotal(total);
    }

    /**
     * @return Returns the entityId.
     */
    public Integer getEntityId () {
        return entityId;
    }

    /**
     * @param entityId
     *            The entityId to set.
     */
    public void setEntityId (Integer entityId) {
        this.entityId = entityId;
    }

    public TimePeriod getDueDatePeriod () {
        return dueDatePeriod;
    }

    public void setDueDatePeriod (TimePeriod dueDatePeriod) {
        this.dueDatePeriod = dueDatePeriod;
    }
}
