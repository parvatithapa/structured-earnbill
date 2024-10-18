package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.util.time.PeriodUnit;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.INT;

/**
 * Created by javierrivero on 5/3/18.
 */
public class RenewOrderNotificationTask extends AbstractCronTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription NOTIFICATION_ID =
            new ParameterDescription("notification_id", true, INT);
    {
        descriptions.add(NOTIFICATION_ID);
    }

    @Override
    public String getTaskName() {
        return "renew order notification task , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        _init(jobExecutionContext);
        Integer notificationMessageTypeId = Integer.parseInt(parameters.get(NOTIFICATION_ID.getName()));
        INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        List<OrderDTO> orderDTOS = new OrderDAS().getOrdersByRenewNotification();
        Date today = Util.truncateDate(new Date());

        for (OrderDTO order : orderDTOS) {
            Date sendNotificationDate = DateConvertUtils.asUtilDate(PeriodUnit.MONTHLY.addTo(DateConvertUtils.asLocalDate(order.getActiveUntil()),
                                                                                             -order.getRenewNotification()));
            if (sendNotificationDate.equals(today)) {
                MessageDTO message = notificationSession.getDTO(notificationMessageTypeId, order.getBaseUserByUserId().getLanguage().getId(), getEntityId());
                notificationSession.asyncNotify(order.getUserId(), message);
            }
        }
    }
}
