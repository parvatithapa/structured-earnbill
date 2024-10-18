package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.customer.CustomerBL;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDAS;
import com.sapienter.jbilling.server.ediTransaction.CustomerDroppedEvent;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.OrderUpdatedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.db.*;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * The task will negate all commission for a partner if the customer has terminated before the minimum period.
 */
public class CommissionReversalTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CommissionReversalTask.class));

    private static final Class<Event> events[] = new Class[]{
            CustomerDroppedEvent.class, OrderUpdatedEvent.class
    };

    private CompanyDAS companyDAS = new CompanyDAS();
    private CommissionProcessRunDAS commissionProcessRunDAS = new CommissionProcessRunDAS();
    private PartnerCommissionDAS partnerCommissionDAS = new PartnerCommissionDAS();
    private PartnerBL partnerBL = new PartnerBL();

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        BigDecimal minPeriod = loadEntityCancellationPeriod(event.getEntityId());
        //if there is no min period defined then do not continue
        if(minPeriod.compareTo(BigDecimal.ZERO) <= 0) {
            LOG.debug("Min days for commission is not > 0: %s", minPeriod);
            return;
        }
        CustomerDTO customer = null;
        Date dateDropped = null;
        if(event instanceof  CustomerDroppedEvent) {
            CustomerDroppedEvent customerDroppedEvent = (CustomerDroppedEvent)event;
            customer = new CustomerBL(customerDroppedEvent.getCustomer()).getEntity();
            dateDropped = customerDroppedEvent.getDateDropped();
        } else if(event instanceof OrderUpdatedEvent) {
            OrderUpdatedEvent orderUpdatedEvent = (OrderUpdatedEvent)event;
            OrderDTO order = new OrderBL(orderUpdatedEvent.getOrderId()).getEntity();
            if(order.getOrderPeriod().getId() == Constants.ORDER_PERIOD_ONCE) {
                //do not care about one time orders
                return;
            }
            customer = order.getBaseUserByUserId().getCustomer();
            List<OrderLineDTO> linesRemoved = orderUpdatedEvent.findLinesRemoved();
            LOG.debug("Lines removed from Order %s", linesRemoved);
            boolean planRemoved = false;
            for(OrderLineDTO lineRemoved : linesRemoved) {
                if(lineRemoved.getItem().isPlan()) {
                    dateDropped = lineRemoved.getEndDate();
                    if(dateDropped == null) {
                        List<OrderChangeDTO> orderChanges = lineRemoved.getOrderChangesSortedByStartDate();
                        if(!orderChanges.isEmpty()) {
                            dateDropped = orderChanges.get(orderChanges.size()-1).getStartDate();
                        }
                    }
                    if(dateDropped == null) {
                        dateDropped = order.getActiveUntil() != null ? order.getActiveUntil() : companyCurrentDate();
                    }
                    planRemoved = true;
                    break;
                }
            }

            if(!planRemoved) {
                LOG.debug("No plan was removed from order %s", order);
                return;
            }
        }


        Date lastEnrollmentDate = new CustomerEnrollmentDAS().findCustomerEnrollmentDate(event.getEntityId(),
                customer.getCustomerAccountInfoTypeMetaField(FileConstants.UTILITY_CUST_ACCT_NR).getMetaFieldValue().getValue().toString());
        if(lastEnrollmentDate == null) {
            LOG.warn("No last enrollment date for customer %s", customer);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(lastEnrollmentDate);
        calendar.add(Calendar.MONTH, minPeriod.intValue());
        Date reversalThreshold = calendar.getTime();

        if(dateDropped.after(reversalThreshold)) {
            LOG.debug("Customer was active until %s. Min date is %s", dateDropped, reversalThreshold);
            return;
        }

        PartnerCommissionDAS partnerCommissionDAS = new PartnerCommissionDAS();

        //find commissions not reversed
        List<PartnerCommissionLineDTO> commissionToReverse = new ArrayList<>();
        List<InvoiceCommissionDTO> invoiceCommissionLines = partnerCommissionDAS.findInvoiceCommissionByUser(customer.getBaseUser());
        for(InvoiceCommissionDTO invoiceCommissionDTO : invoiceCommissionLines) {
            if(invoiceCommissionDTO.getReversal() == null && invoiceCommissionDTO.getOriginalCommissionLine() == null) {
                commissionToReverse.add(invoiceCommissionDTO);
            }
        }

        List<CustomerCommissionDTO> customerCommissionLines = partnerCommissionDAS.findCustomerCommission(customer.getBaseUser());
        for(CustomerCommissionDTO customerCommissionDTO : customerCommissionLines) {
            if(customerCommissionDTO.getReversal() == null && customerCommissionDTO.getOriginalCommissionLine() == null) {
                commissionToReverse.add(customerCommissionDTO);
            }
        }

        partnerBL.reverseCommissions(commissionToReverse, getEntityId());
    }


    private BigDecimal loadEntityCancellationPeriod(int entityId) {
        CompanyDTO company = new CompanyDAS().find(entityId);
        MetaFieldValue<BigDecimal> metaFieldValue = company.getMetaField(FileConstants.COMMISSION_MIN_DAYS_META_FIELD_NAME);
        return metaFieldValue != null && metaFieldValue.getValue() != null ? metaFieldValue.getValue() : BigDecimal.ZERO;
    }
}
