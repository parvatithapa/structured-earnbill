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

package com.sapienter.jbilling.server.process.task;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

/**
 * RemovalOfAssetsFromSubscriptionTask
 *
 * @author Ashish Srivastava
 * @since 05/12/19
 */
public class RemovalOfAssetsFromSubscriptionTask extends AbstractCronTask {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    //private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RemovalOfAssetsFromSubscriptionTask.class));
    
    protected static final ParameterDescription NUMBER_OF_DAYS =
            new ParameterDescription("Number of Days", false, ParameterDescription.Type.STR);

    {
        descriptions.add(NUMBER_OF_DAYS);
    }
    public RemovalOfAssetsFromSubscriptionTask () {
        setUseTransaction(true);
    }

    public String getTaskName() {
        return "RemovalOfAssetsFromSubscriptionTask: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        _init(context);
        Date companyCurrentDate = companyCurrentDate();
        OrderDAS orderDas = new OrderDAS();
        List<Integer> orderIds = orderDas.findAllFinishedSubscriptions();
        String  numberOfDays = getParameter(NUMBER_OF_DAYS.getName(),StringUtils.EMPTY);
        Calendar cal = Calendar.getInstance();
        for(Integer orderId: orderIds) {
            OrderDTO order = orderDas.find(orderId);
            Date activeUntilDate = null != order.getActiveUntil() ? order.getActiveUntil() : order.getFinishedDate();
            if (null == activeUntilDate) {
                LOG.debug("Order don't have Active Until and Finished Date so skipping order", order.getId());
                continue;
            }
            //When number of days parameter value not present and Active Until Date before current date then remove all assets from order.
            if(null == numberOfDays && activeUntilDate.before(companyCurrentDate) ) {
                new AssetBL().unlinkAssets(orderId, null);
            } else {
                cal.setTime(activeUntilDate);
                cal.add(Calendar.DATE, Integer.parseInt(numberOfDays));
                if(cal.getTime().before(companyCurrentDate)) {
                    new AssetBL().unlinkAssets(orderId, null);
                }
            }
        }
        LOG.info("Ended RemovalOfAssetsFromSubscriptionTask at " + TimezoneHelper.serverCurrentDate() + " server timezone.");
    }
}
