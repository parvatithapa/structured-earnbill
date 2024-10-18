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

package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ediTransaction.EnrollmentCqModelRequest;
import com.sapienter.jbilling.server.ediTransaction.EnrollmentCqRateRequest;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.OrderUpdatedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Send 814 EDI Model or Rate Change transactions to the LDC based on OrderUpdatedEvents.
 * Generate a model-change EDI transaction if the new order has a plan with a different “billing model” than the previous one
 * Generate a rate-change EDI transaction, on the following conditions:
 *  - If we switch from any billing model to Rate Ready
 *  - If we switch from Rate Ready to another Rate Ready plan with different rate.
 *
 * @author Gerhard Maree
 */
public class BillingModelModificationTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger( Logger.getLogger(BillingModelModificationTask.class) );

    private static final Class<Event> events[] = new Class[] {
            OrderUpdatedEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) throws PluggableTaskException {
        OrderUpdatedEvent orderUpdatedEvent = (OrderUpdatedEvent)event;
        ItemBL itemBL = new ItemBL();
        Date txDate = companyCurrentDate();

        PlanDTO oldPlan = findPlanInOrderLines(orderUpdatedEvent.getOldLines(), itemBL);
        PlanDTO newPlan = findPlanInOrderLines(orderUpdatedEvent.getNewLines(), itemBL);

        if(oldPlan == null || newPlan == null || oldPlan.getId().equals(newPlan.getId())) {
            LOG.debug("No plan change. Old Plan = [%s]. New Plan = [%s]", oldPlan, newPlan);
            return;
        }

        String oldBillingModel = BillingModelModificationTask.getBillingModel(oldPlan);
        String newBillingModel = BillingModelModificationTask.getBillingModel(newPlan);

        if(oldBillingModel == null) {
            LOG.error("Plan '"+oldPlan.getDescription()+"'("+oldPlan.getId()+") does not have a billing model");
            return;
        }
        if(newBillingModel == null) {
            LOG.error("Plan '"+newPlan.getDescription()+"'("+newPlan.getId()+") does not have a billing model");
        }
        LOG.debug("Old Billing Model: %s", oldBillingModel);
        LOG.debug("New Billing Model: %s", newBillingModel);

        OrderDTO order = new OrderBL(orderUpdatedEvent.getOrderId()).getEntity();
        CustomerDTO customer = order.getUser().getCustomer();

        if(!oldBillingModel.equals(newBillingModel)) {
            LOG.debug("Sending Model Change Request");
            createModelChangeRequest(customer, order, newBillingModel.equalsIgnoreCase(FileConstants.BILLING_MODEL_RATE_READY));
        }

        if(newBillingModel.equalsIgnoreCase(FileConstants.BILLING_MODEL_RATE_READY)) {
            boolean createRateChange = false;
            PriceModelDTO newPriceModel = getPriceModelOfItem(newPlan, txDate);
            if(!oldBillingModel.equalsIgnoreCase(FileConstants.BILLING_MODEL_RATE_READY)) {
                LOG.debug("Billing model changed to rate ready");
                createRateChange = true;
            } else {
                PriceModelDTO oldPriceModel = getPriceModelOfItem(oldPlan, txDate);
                createRateChange = !oldPriceModel.equalsModel(newPriceModel);
                LOG.debug("Price of Rate Ready Changed [%s]", createRateChange);
            }

            if(createRateChange) {
                createRateChangeRequest(customer, order, newPriceModel.getRate());
            }
        }
    }

    private PriceModelDTO getPriceModelOfItem(PlanDTO plan, Date date) {
        List<PlanItemDTO> items = plan.getPlanItems();
        if(items.isEmpty()) {
            return null;
        }
        return items.get(0).getPrice(date);
    }

    private void createModelChangeRequest(CustomerDTO customer, OrderDTO order, boolean isRateReady) {
        EnrollmentCqModelRequest request = new EnrollmentCqModelRequest(customer.getBaseUser().getCompany(), customer,
                isRateReady ? EnrollmentCqModelRequest.BillModel.RATE_READY : EnrollmentCqModelRequest.BillModel.BILL_READY,
                order.getActiveUntil(), TimezoneHelper.companyCurrentDate(customer.getBaseUser().getCompany()));
        request.generateFile();
    }

    private void createRateChangeRequest(CustomerDTO customer, OrderDTO order, BigDecimal rate) {
        EnrollmentCqRateRequest request = new EnrollmentCqRateRequest(customer.getBaseUser().getCompany(), customer, rate,
                order.getActiveUntil(), TimezoneHelper.companyCurrentDate(customer.getBaseUser().getCompany()));
        request.generateFile();
    }

    public static String getBillingModel(PlanDTO plan) {
        MetaFieldValue<String> billingModelMf = plan.getMetaField(FileConstants.BILLING_MODEL);
        if(billingModelMf != null) {
            return billingModelMf.getValue();
        }
        return null;
    }

    public static PlanDTO findPlanInOrderLines(List<OrderLineDTO> orderLines, ItemBL itemBL) {
        for(OrderLineDTO line : orderLines) {
            itemBL.set(line.getItem().getId());
            ItemDTO item = itemBL.getEntity();
            if(line.getDeleted() == 0 && item.isPlan()) {
                return itemBL.getEntity().getPlans().iterator().next();
            }
        }
        return null;
    }
}
