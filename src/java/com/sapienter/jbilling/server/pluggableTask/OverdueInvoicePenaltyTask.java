/**
 * 
 */
package com.sapienter.jbilling.server.pluggableTask;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.event.AboutToGenerateInvoices;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OverdueInvoicePenaltyTask is similar to the BasicPenaltyTask
 * The purpose is similar to create a Penalty order but in this case the 
 * it is not a one-time penalty order but a recurring Penalty Order that stays 
 * as long as that particular Invoice is overdue.
 * 
 * This task is powerful because it performs this action just before the Billing process collects orders. 
 * Thus it is different from BasicPenaltyTask, which is actually dependent on the Ageing Process to 
 * initiate action. Any overdue Invoice cannot be missed because this task performs its action just before the Billing 
 * Process collects orders.
 * 
 * It uses the same plugin parameters as the BasicPenaltyTask, the actual values of the params
 * may differ to acheive a different result or Penalty Description.
 * 
 * Once, the offending Invoice has been paid, the related Order should be set to have
 * an activeUntil date value of the date on which it has been fully paid.
 *
 * For this penalty to be pro-rated, a DynamicBalanceManagerTask must be configured.
 * 
 * @author Vikas Bodani
 * @since 05-Jun-2012
 *
 */
public class OverdueInvoicePenaltyTask extends PluggableTask implements IInternalEventsTask {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    public static final ParameterDescription PARAMETER_ITEM =
        	new ParameterDescription("penalty_item_id", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_CHARGE_ITEM =
            new ParameterDescription("penalty_charge_item_id", true, ParameterDescription.Type.STR);

        //initializer for pluggable params
        {
        	descriptions.add(PARAMETER_ITEM);
            descriptions.add(PARAMETER_CHARGE_ITEM);
    	}

        private Integer itemId;
        private Integer chargedItemId;

        @SuppressWarnings("unchecked")
        private static final Class<Event>[] events = new Class[] {
                AboutToGenerateInvoices.class
        };

        public Class<Event>[] getSubscribedEvents() { return events; }

        /**
         * Returns the configured penalty item id to be added to any overdue invoices.
         *
         * fixme: user configured penalty item id always comes through as a String
         *
         * @return item id
         * @throws PluggableTaskException if the parameter is not an integer
         */
        public Integer getPenaltyItemId() throws PluggableTaskException {
            if (itemId == null) {
                try {
                    itemId = Integer.parseInt((String) parameters.get(PARAMETER_ITEM.getName()));
                } catch (NumberFormatException e) {
                    throw new PluggableTaskException("Configured penalty item id must be an integer!", e);
                }
            }
            return itemId;
        }

    public Integer getChargedPenaltyItemId() throws PluggableTaskException {
        if (chargedItemId == null) {
            try {
                chargedItemId = Integer.parseInt((String) parameters.get(PARAMETER_CHARGE_ITEM.getName()));
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured penalty item id must be an integer!", e);
            }
        }
        return chargedItemId;
    }

        /**
         * @see IInternalEventsTask#process(com.sapienter.jbilling.server.system.event.Event)
         *
         * @param event event to process
         * @throws PluggableTaskException
         */
        public void process(Event event) throws PluggableTaskException {
            if (!(event instanceof AboutToGenerateInvoices))
                throw new PluggableTaskException("Cannot process event " + event);

            AboutToGenerateInvoices invEvent = (AboutToGenerateInvoices) event;
            
            UserBL userbl = new UserBL(invEvent.getUserId());
            OrderDTO activePenaltyOrder = getPenaltyOrder(userbl.getEntity());
        	
        	if (null != activePenaltyOrder) 
        		return;
            
        	logger.debug("Penalty Order not found, Processing event: user id: {}",
                     invEvent.getUserId());

            // find all unpaid, overdue invoices or invoices paid after due date, for this user and add the penalty item excluding
            // carried invoices as the remaining balance will already have been applied to the new invoice.
            InvoiceDAS invoiceDAS = new InvoiceDAS();
            List<InvoiceDTO> unpaidInvoices=invoiceDAS.findProccesableByUser(UserBL.getUserEntity(invEvent.getUserId())); 
            List<Integer> latePaid = new InvoiceDAS().findLatePaidInvoicesForUser(invEvent.getUserId());

            logger.debug("Found un-paid invoices {}", unpaidInvoices);
            logger.debug("Found invoice ids {}", latePaid);
            
            // quit if the user has no overdue invoices.
            if (unpaidInvoices.isEmpty() && latePaid.isEmpty()) {
            	logger.error("Cannot apply a penalty to a user that does not have an overdue invoice!");
                return;
            }

            for (Integer invoiceID: latePaid) {
            	unpaidInvoices.add(invoiceDAS.find(invoiceID));
            }
            
            ItemBL item;
            try {
                item = new ItemBL(getPenaltyItemId());
                logger.debug("Penalty item {}", getPenaltyItemId());
            } catch (SessionInternalError e) {
                throw new PluggableTaskException("Cannot find configured penalty item: " + getPenaltyItemId(), e);
            } catch (Exception e) {
                throw new PluggableTaskException(e);
            }
        	
            // sort and pick up the last unpaid invoice
        	Collections.sort(unpaidInvoices, new Comparator<InvoiceDTO>() {
        		@Override
        		public int compare(InvoiceDTO o1, InvoiceDTO o2) {
        			return o2.getId() - o1.getId();
        		}
			});
    		InvoiceDTO invoice = unpaidInvoices.get(0);
    		
            BigDecimal fee = calculatePenaltyFee(invoice, item);
            logger.debug("Calculated penalty item fee: {}", fee);

            if (fee.compareTo(BigDecimal.ZERO) <= 0)
                return;

            try {
                item = new ItemBL(getChargedPenaltyItemId());
                logger.debug(" Charged Penalty item {}", getChargedPenaltyItemId());
            } catch (SessionInternalError e) {
                throw new PluggableTaskException("Cannot find configured charged penalty item: " + getChargedPenaltyItemId(), e);
            } catch (Exception e) {
                throw new PluggableTaskException(e);
            }
            // create the order
            Integer orderId= createPenaltyOrder(invoice, item, fee, invEvent.getRunDate());
            logger.debug("Created penalty Order {}", orderId);
        }
        

    /** 
     * Create a task specific Order for the given Invoice having given Item
     * @param invoice Invoice for which to create Penalty order
     * @param item Penalty Item (Percentage or Flat)
     * @return orderId 
     */
    public Integer createPenaltyOrder(InvoiceDTO invoice, ItemBL item, BigDecimal fee, Date billingRunDate) throws PluggableTaskException {
        OrderDTO summary = new OrderDTO();
        summary.setOrderPeriod(new OrderPeriodDAS().find(Constants.ORDER_PERIOD_ONCE));

        OrderBillingTypeDTO type = new OrderBillingTypeDTO();
        type.setId(Constants.ORDER_BILLING_POST_PAID);
        summary.setOrderBillingType(type);
        //penalty applicable since the date Invoice got created.
        summary.setActiveSince(getLastDayOfBillingRunMonth(billingRunDate)); 
        logger.debug("Order active since {}", summary.getActiveSince());
        summary.setCurrency(invoice.getCurrency());

        //if invoice is paid after due date, this order must have an active until date
        // AC specific change: Since the order is not period order, no point in setting the active until date. 
        /*if (Constants.INVOICE_STATUS_PAID.equals(invoice.getInvoiceStatus().getId())) {
        	LOG.debug("Invoice " + invoice.getId() + " has been paid.");
        	summary.setActiveUntil(invoice.getPaymentMap().iterator().next().getCreateDatetime());
        }*/
        
        UserDTO user = new UserDTO();
        user.setId(invoice.getBaseUser().getId());
        summary.setBaseUserByUserId(user);

        // now add the item to the po
        Integer languageId = invoice.getBaseUser().getLanguageIdField();
        String description = item.getEntity().getDescription(languageId) + " as Overdue Penalty for Invoice Number " 
        + (getUseInvocieIdAsInvoiceNumberPreferenceValue(invoice.getBaseUser().getEntity().getId()) ? invoice.getId() : invoice.getPublicNumber());

        OrderLineDTO line = new OrderLineDTO();
        line.setAmount(fee);
        line.setPrice(fee);
        line.setDescription(description);
        line.setItemId(item.getEntity().getId());
        line.setTypeId(Constants.ORDER_LINE_TYPE_PENALTY);
        line.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        line.setDeleted(0);
        line.setUseItem(false);
        line.setQuantity(1);
        summary.getLines().add(line);

        // create the db record
        OrderBL order = new OrderBL();
        order.set(summary);
        return order.create(invoice.getBaseUser().getEntity().getId(), null, summary);
    }
    
    /**
     * This method goes through all the orders for user and 
     * checks if there is any existing penalty order and returns that.
     * @param user
     * @return OrderDTO
     */
    private OrderDTO getPenaltyOrder(UserDTO user) {
    	for (OrderDTO order : user.getOrders()) {
            if (order.getDeleted() == 0 && order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.INVOICE)) {
    			for (OrderLineDTO orderline : order.getLines()) {
    				if (orderline.getOrderLineType().getId() == Constants.ORDER_LINE_TYPE_PENALTY) {
    					return order;
    				}
    			}
            }
    	}
    	
    	return null;
    }

    /**
     * Returns a calculated penalty fee for the users current owing balance and
     * the configured penalty item.
     *
     * @param invoice overdue invoice
     * @param item penalty item
     * @return value of the penalty item (penalty fee)
     */
    private BigDecimal calculatePenaltyFee(InvoiceDTO invoice, ItemBL item) {
        // use the user's current balance as the base for our fee calculations
        BigDecimal base = UserBL.getBalance(invoice.getUserId());

        // if the item price is a percentage of the balance
        if (item.getEntity().getPrice(companyCurrentDate()).getType() == PriceModelStrategy.LINE_PERCENTAGE) {
            base = base.divide(new BigDecimal("100"), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
            base = base.multiply(item.getEntity().getPrice(companyCurrentDate()).getRate());
            return base;

        } else if (base.compareTo(BigDecimal.ZERO) > 0) {
            // price for a single penalty item.
            return item.getPrice(invoice.getBaseUser().getId(),
                                          invoice.getCurrency().getId(),
                                          BigDecimal.ONE,
                                          invoice.getBaseUser().getEntity().getId());
        } else {
            return BigDecimal.ZERO;
        }
    }

    private Date getLastDayOfBillingRunMonth(Date runDate) {
    	Calendar calendar = Calendar.getInstance();  
        calendar.setTime(runDate);
        calendar.set(Calendar.DAY_OF_MONTH, 1);  
        calendar.add(Calendar.DATE, -1);  
        return calendar.getTime(); 
    }
    
    /**
     * Invoice line will show Invoice ID if parameter is true.
     * else invoice line will show Invoice Number in description
     * @return boolean
     */
    protected boolean getUseInvocieIdAsInvoiceNumberPreferenceValue(Integer entityId){
        Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.PREFERENCE_USE_INVOICE_ID_AS_INVOICE_NUMBER_IN_INVOICE_LINE_DESCRIPTIONS);
        return (prefValue != null && prefValue == 1);
    }
}
