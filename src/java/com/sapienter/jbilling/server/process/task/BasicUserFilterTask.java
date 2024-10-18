package com.sapienter.jbilling.server.process.task;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.user.UserBL;

public class BasicUserFilterTask extends PluggableTask implements IBillableUserFilterTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public boolean isNotBillable(Integer userId, Date billingRunDate) {
        UserBL userBL = new UserBL(userId);
        boolean isNotBillable = userBL.isNotBillable(billingRunDate);
        if(isNotBillable) {
            logger.debug("user {} not billable for run date {}", userId, billingRunDate);
        }
        return isNotBillable;
    }

}
