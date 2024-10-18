package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aman on 21/12/15.
 */
public class ActivateOrderTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ActivateOrderTask.class));

    protected static final ParameterDescription DELAY_PERIOD_IN_DAYS =
            new ParameterDescription("delay period (In days)", true, ParameterDescription.Type.INT);

    {
        descriptions.add(DELAY_PERIOD_IN_DAYS);
    }

    public ActivateOrderTask() {
        setUseTransaction(true);
    }

    @Override
    public String getTaskName() {
        return "Activate Bill Ready orders, entity Id: " + this.getEntityId() + ", task Id:" + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        super._init(context);
        LOG.debug("Executing ActivateOrderTask for company : " + getEntityId());
        Integer delay = null;
        try {
            delay = getParameter(DELAY_PERIOD_IN_DAYS.getName(), 0);
        } catch (Exception e) {
            LOG.error("Error occured fetching parameters");
        }

        LOG.debug("delay period (In days) : " + delay);

        //Find Bill ready plans
        MetaFieldDAS das = new MetaFieldDAS();
        MetaField billingMF = das.getFieldByName(getEntityId(), new EntityType[]{EntityType.PLAN}, FileConstants.BILLING_MODEL);
        LOG.debug("MetaField Found for " + FileConstants.BILLING_MODEL + " : " + billingMF.getId());

        if (billingMF == null) {
            throw new SessionInternalError("Meta field not exist with name " + FileConstants.BILLING_MODEL + " for plan");
        }

        List<Integer> plans = new MetaFieldDAS().findPlansByMetaFieldValue(billingMF, FileConstants.BILLING_MODEL_BILL_READY, getEntityId());

        if (plans.size() == 0) {
            LOG.debug("No Bill ready plan found for entity " + getEntityId());
            return;
        }

        final List<String> planNames = new LinkedList<String>();
        PlanDAS planDAS = new PlanDAS();
            plans.stream().map(plan -> planDAS.findInternalNumberByPlan(plan)).forEach(planName -> planNames.add((String) planName));

        //Find Bill ready customers
        LOG.debug("Bill ready type plans : " + plans.size());
        List<Integer> billReadyCustomers = new UserDAS().findByMetaFieldNameAndValues(InvoiceBuildTask.InvoiceBuildConstants.PLAN.getValue(), planNames, getEntityId());
        LOG.debug(" Bill ready Customers :  " + billReadyCustomers.size());

        if (billReadyCustomers.size() == 0) {
            LOG.debug("No Bill ready customers found for entity " + getEntityId());
            return;
        }
        int suspendedStatus = new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.NOT_INVOICE, getEntityId());

        Calendar calendar = Calendar.getInstance(); // this would default to now
        calendar.setTime(TimezoneHelper.serverCurrentDate());
        calendar.add(Calendar.DAY_OF_MONTH, -(delay));
        Date expectedCreationDate = calendar.getTime();

        LOG.debug("Expected Creation Date " + expectedCreationDate);

        //Find Suspended orders
        ScrollableResults orders = null;

        try {
            orders = new OrderDAS().findByUsersAndStatus(billReadyCustomers.toArray(new Integer[billReadyCustomers.size()]), suspendedStatus, Constants.ORDER_PERIOD_ONCE, expectedCreationDate);
            LOG.debug("Fetched Bill ready Customer's orders");

            //Update orders
            IOrderSessionBean sesson = Context.getBean(Context.Name.ORDER_SESSION);
            while (orders.next()) {
                LOG.debug("updating order");
                OrderDTO order = (OrderDTO) orders.get(0);
                MetaFieldValue<Boolean> metaFieldValue=order.getMetaField(FileConstants.IS_REBILL_ORDER);
                if(metaFieldValue!=null && metaFieldValue.getValue()!=null && metaFieldValue.getValue()){
                    continue;
                }
                LOG.debug("Activating order : %s", order.getId());
                sesson.setStatus(order.getId(), new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, getEntityId()), null, Constants.LANGUAGE_ENGLISH_ID);
            }
        } finally {
            orders.close();
        }
    }
}
