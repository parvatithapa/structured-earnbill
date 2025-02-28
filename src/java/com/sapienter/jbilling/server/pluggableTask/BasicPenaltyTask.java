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

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceBL;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Emil
 * This task will create a new purchase order with the item specified by the
 * task parameter and mark the invoice as processed with invoice.overdue_step = 0.
 *
 * Situations considered
 * payable not overdue  -> nothing
 * payable overdue (pure) -> penalty
 * payable overdue partialy paid -> penalty on balance
 * not payable, not paid, delegated -> penalty on total
 * not payable, partialy paid, delegated -> penalty on previous balance
 * Since the task is running on the day after the due date ... :
 * not payable, not paid, delegated and paid after due date -> penalty on total
 * not payable, not paid, delegated and paid partialy after due date -> penalty on total
 * not payable, not paid, delegated and paid before due date -> nothing
 * not payable, not paid, delegated and paid partialy before due date -> penalty on balance
 *
 * The one situation NOT considered is if many invoices get delegated to
 * a single one. This shouldn't happend becasue when an invoice is generated it will
 * inherit the previous one automaticaly
 */
public class BasicPenaltyTask extends PluggableTask implements IInternalEventsTask {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ParameterDescription PARAMETER_ITEM =
    	new ParameterDescription("item", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_AGEING_STEP =
    	new ParameterDescription("ageing_step", true, ParameterDescription.Type.STR);

    //initializer for pluggable params
    {
    	descriptions.add(PARAMETER_ITEM);
        descriptions.add(PARAMETER_AGEING_STEP);
	}

    private Integer itemId;
    private Integer ageingStep;

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
            NewUserStatusEvent.class
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

    /**
     * Returns the configured ageing step that the penalty should be applied to.
     *
     * fixme: user configured ageing step always comes through as a String
     *
     * @return ageing step
     * @throws PluggableTaskException if the parameter is not an integer
     */
    public Integer getAgeingStep() throws PluggableTaskException {
        if (ageingStep == null) {
            try {
                ageingStep = Integer.valueOf((String) parameters.get(PARAMETER_AGEING_STEP.getName()));
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured ageing_step must be an integer!", e);
            }
        }
        return ageingStep;
    }

    /**
     * Invoice line will show Invoice ID if parameter is true.
     * else invoice line will show Invoice Number in invoice line description
     * @return boolean
     */
    protected boolean getUseInvocieIdAsInvoiceNumberPreferenceValue(Integer entityId){
        Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, CommonConstants.PREFERENCE_USE_INVOICE_ID_AS_INVOICE_NUMBER_IN_INVOICE_LINE_DESCRIPTIONS);
        return (prefValue != null && prefValue == 1);
    }

    /**
     * @see IInternalEventsTask#process(com.sapienter.jbilling.server.system.event.Event)
     *
     * @param event event to process
     * @throws PluggableTaskException
     */
    public void process(Event event) throws PluggableTaskException {
        if (!(event instanceof NewUserStatusEvent))
            throw new PluggableTaskException("Cannot process event " + event);

        NewUserStatusEvent statusEvent = (NewUserStatusEvent) event;

        logger.debug("Processing event: new status id {} user id: {}",
                 statusEvent.getNewStatusId(),
                 statusEvent.getUserId());

        // user status id must match the configured ageing step.
        if (!statusEvent.getNewStatusId().equals(getAgeingStep()))
            return;

        // find all unpaid, overdue invoices for this user and add the penalty item excluding
        // carried invoices as the remaining balance will already have been applied to the new invoice.
        Date today = Calendar.getInstance().getTime();
        today = com.sapienter.jbilling.common.Util.truncateDate(today);
        List<Integer> overdueIds = new InvoiceDAS().findIdsOverdueForUser(statusEvent.getUserId(), today);

        // quit if the user has no overdue invoices.
        if (overdueIds.isEmpty()) {
            logger.error("Cannot apply a penalty to a user that does not have an overdue invoice!");
            return;
        }

        Integer invoiceId = overdueIds.get(0);

        InvoiceBL invoiceBL;
        try {
            invoiceBL = new InvoiceBL(invoiceId);
        } catch (Exception e2) {
            throw new PluggableTaskException(e2);
        }

        InvoiceDTO invoice = invoiceBL.getEntity();

        logger.debug("Processing overdue invoice {}. Adding penalty item {}",invoiceId, getPenaltyItemId());
        ItemBL item;
        try {
            item = new ItemBL(getPenaltyItemId());
        } catch (SessionInternalError e) {
            throw new PluggableTaskException("Cannot find configured penalty item: " + getPenaltyItemId(), e);
        } catch (Exception e) {
            throw new PluggableTaskException(e);
        }

        // Calculate the penalty fee. If the fee is zero (check the item cost) then
        // no penalty should be applied to this invoice.
        BigDecimal fee = calculatePenaltyFee(invoice, item);
        logger.debug("Calculated penalty item fee: {}", fee);

        if (fee.compareTo(BigDecimal.ZERO) <= 0)
            return;

        // create the order
        OrderDTO summary = new OrderDTO();
        OrderPeriodDTO period = new OrderPeriodDTO();
        period.setId(Constants.ORDER_PERIOD_ONCE);
        summary.setOrderPeriod(period);

        OrderBillingTypeDTO type = new OrderBillingTypeDTO();
        type.setId(Constants.ORDER_BILLING_PRE_PAID);
        summary.setOrderBillingType(type);
        summary.setCreateDate(Calendar.getInstance().getTime());
        summary.setCurrency(invoice.getCurrency());

        UserDTO user = new UserDTO();
        user.setId(invoice.getBaseUser().getId());
        summary.setBaseUserByUserId(user);

        // now add the item to the po
        Integer languageId = invoice.getBaseUser().getLanguageIdField();
        String description = item.getEntity().getDescription(languageId) + getInvoiceDelegatedDescription(invoice);

        OrderLineDTO line = new OrderLineDTO();
        line.setAmount(fee);
        line.setPrice(fee);
        line.setDescription(description);
        line.setItemId(getPenaltyItemId());
        line.setTypeId(Constants.ORDER_LINE_TYPE_PENALTY);
        line.setQuantity(1);
        summary.getLines().add(line);

        // create the db record
        OrderBL order = new OrderBL();
        order.set(summary);
        order.create(invoice.getBaseUser().getEntity().getId(), null, summary);
    }

    /**
     * Returns a calculated penalty fee for the users current owing balance and
     * the configured penalty item.
     *
     * @param invoice overdue invoice
     * @param item penalty item
     * @return value of the penalty item (penalty fee)
     */
    public BigDecimal calculatePenaltyFee(InvoiceDTO invoice, ItemBL item) {
        // use the user's current balance as the base for our fee calculations
        BigDecimal base = UserBL.getBalance(invoice.getUserId());

        // if the item price is a percentage of the balance
        if (item.getEntity().getPercentage() != null) {
            base = base.divide(new BigDecimal("100"), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
            base = base.multiply(item.getEntity().getPercentage());
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


    /**
     * Returns a string description for an "invoice delegated" line item.
     *
     * @param invoice invoice to compose description for
     * @return description
     * @throws PluggableTaskException thrown if locale could not be determined
     */
    public String getInvoiceDelegatedDescription(InvoiceDTO invoice) throws PluggableTaskException {
        Locale locale;
        try {
            UserBL userBl = new UserBL(invoice.getBaseUser());
            locale = userBl.getLocale();
        } catch (Exception e) {
            throw new PluggableTaskException("Exception finding locale to add delegated invoice line", e);
        }

        ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);
        
        DateTimeFormatter df = DateTimeFormat.forPattern(bundle.getString("format.date"));

        StringBuilder buff = new StringBuilder();
        buff.append(" - ")
                .append(bundle.getString("invoice.line.delegated"))
                .append(' ')
                .append(getUseInvocieIdAsInvoiceNumberPreferenceValue(invoice.getBaseUser().getEntity().getId()) ? invoice.getId() : invoice.getPublicNumber())
                .append(' ')
                .append(bundle.getString("invoice.line.delegated.due"))
                .append(' ')
                .append(df.print(invoice.getDueDate().getTime()));

        return buff.toString();
    }
}
