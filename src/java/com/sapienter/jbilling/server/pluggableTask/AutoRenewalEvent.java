package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

public class AutoRenewalEvent implements Event {

    private Integer entityId;
    private CustomerDTO customer;
    private boolean renewalReached;
    private Integer daysBeforeNotification;

    public AutoRenewalEvent(Integer entityId, CustomerDTO customer, boolean renewalReached, Integer daysBeforeNotification) {
        this.entityId = entityId;
        this.customer = customer;
        this.renewalReached =renewalReached;
        this.daysBeforeNotification = daysBeforeNotification;
    }

    @Override
    public String getName() {
        return "Auto Renewal Notification";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public CustomerDTO getCustomer() {
        return customer;
    }

    public boolean isRenewalReached() {
        return renewalReached;
    }

    public Integer getDaysBeforeNotification() {
        return daysBeforeNotification;
    }
}