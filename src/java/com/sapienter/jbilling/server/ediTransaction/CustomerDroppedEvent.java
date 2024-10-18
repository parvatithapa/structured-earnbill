package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

import java.util.Date;

/**
 * Event gets fired when an NGES customer gets dropped.
 */
public class CustomerDroppedEvent implements Event {
    private int entityId;
    private int customerId;
    private Date dateDropped;

    public CustomerDroppedEvent(int entityId, int customer, Date dateDropped) {
        this.entityId = entityId;
        this.customerId = customer;
        this.dateDropped = dateDropped;
    }

    public int getCustomer() {
        return customerId;
    }

    public Date getDateDropped() {
        return dateDropped;
    }

    @Override
    public String getName() {
        return "Customer Dropped";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }
}
