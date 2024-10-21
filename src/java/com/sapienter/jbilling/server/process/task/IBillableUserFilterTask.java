package com.sapienter.jbilling.server.process.task;

import java.util.Date;

public interface IBillableUserFilterTask {

    /**
     * checks user is billable or not for given billingRunDate.
     * @param userId
     * @param billingRunDate
     * @return
     */
    boolean isNotBillable (Integer userId, Date billingRunDate);
}
