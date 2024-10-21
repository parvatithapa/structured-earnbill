package com.sapienter.jbilling.server.spc;

import com.sapienter.jbilling.server.customer.event.CustomerBillingCycleChangeEvent;
import com.sapienter.jbilling.server.customer.event.NewCustomerEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class SPCUpdateCustomerNextInvoiceDateTask extends PluggableTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
            CustomerBillingCycleChangeEvent.class,
            NewCustomerEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        MainSubscriptionDTO mainSubscription = null;
        CustomerDTO customer = null;
        if (event instanceof CustomerBillingCycleChangeEvent) {
            customer = ((CustomerBillingCycleChangeEvent) event).getUser().getCustomer();
            mainSubscription = customer.getMainSubscription();
        }
        if (event instanceof NewCustomerEvent) {
            customer = ((NewCustomerEvent) event).getUser().getCustomer();
            mainSubscription = null != customer ? customer.getMainSubscription() : null;
        }
        if (null != mainSubscription && mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId() == PeriodUnitDTO.MONTH) {
            setCustomerNextInvoiceDate(customer);
        }

    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /**
     * If the customer's next invoice day of period value changed then calculate the customer's next invoice date based on
     * billing delay days and customer type parameters of the SPCUserFilterTask plugin.
     * @param user
     * @param entityId
     */
    public void setCustomerNextInvoiceDate(CustomerDTO customer){

        // return customer next invoice date in case billing cycle change for postpaid and prepaid type customer
        LocalDate calNextInvoiceDate = calculateNextInvoiceDateForDelayDays(customer);
        if (null != calNextInvoiceDate) {
            customer.setNextInvoiceDate(asUtilDate(calNextInvoiceDate));
        }
    }

    /**
     * return customer next invoice date in case billing cycle change for
     * 1. postpaid type customer and delay days greater than 0
     * 2. prepaid type customer and delay days value is 0
     * @param nextInvoiceDate
     * @param userDto
     * @param daysToDelay
     * @param nextInvoiceDayOfPeriod
     * @return
     */

    private LocalDate calculateNextInvoiceDateForDelayDays(CustomerDTO customer){
        SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);
        Integer daysToDelay = spcHelperService.getNumberOfDaysDelayForCustomer(customer, getEntityId());
        LocalDate nextInvoiceDate = customer.getNextInvoiceDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Date newNextInvoiceDate = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date lastBillingProcessDate = new BillingProcessDAS().getLastBillingProcessDate(getEntityId());
        Integer nextInvoiceDayOfPeriod = customer.getMainSubscription().getNextInvoiceDayOfPeriod();
        int noOfMonths = 0;
        while (newNextInvoiceDate.after(lastBillingProcessDate)) {
            nextInvoiceDate = nextInvoiceDate.minusMonths(1);
            newNextInvoiceDate = Date.from(nextInvoiceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            noOfMonths++;
        }
        //Handled FEB month scenarios when customer billing cycle is 29th, 30th, 31st monthly. Adjust next invoice date as per main subscription day of period.
        if (nextInvoiceDate.getDayOfMonth() != nextInvoiceDayOfPeriod) {
            Integer lastDayOfMonth = getLastDayOfMonth(nextInvoiceDate);
            if (nextInvoiceDayOfPeriod <= lastDayOfMonth) {
                nextInvoiceDate = nextInvoiceDate.withDayOfMonth(nextInvoiceDayOfPeriod); //Update next invoice date with the next invoice day of period
            } else {
                nextInvoiceDate = nextInvoiceDate.withDayOfMonth(lastDayOfMonth); //Update next invoice date with last day of month
            }
        }

        LocalDate nextInvoiceDateWithDelayDays = nextInvoiceDate.plusDays(daysToDelay);
        newNextInvoiceDate = Date.from(nextInvoiceDateWithDelayDays.atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (newNextInvoiceDate.after(lastBillingProcessDate)) {
            return nextInvoiceDate;
        } else if (noOfMonths > 1) {
            nextInvoiceDate = nextInvoiceDate.plusMonths(1);
            Integer lastDayOfMonth = getLastDayOfMonth(nextInvoiceDate);
            if (nextInvoiceDayOfPeriod <= lastDayOfMonth) {
                nextInvoiceDate = nextInvoiceDate.withDayOfMonth(nextInvoiceDayOfPeriod); //Update next invoice date with the next invoice day of period
            } else {
                nextInvoiceDate = nextInvoiceDate.withDayOfMonth(lastDayOfMonth); //Update next invoice date with last day of month
            }
            return nextInvoiceDate;
        }

        return null;
    }

    public Date asUtilDate (LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public Integer getLastDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth()).getDayOfMonth();
    }
}
