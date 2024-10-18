package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentBL;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDAS;
import com.sapienter.jbilling.server.system.event.Event;

public abstract class EnrollmentEvent implements Event {

    protected static final CustomerEnrollmentDAS customerEnrollmentDAS = new CustomerEnrollmentDAS();

    protected Integer entityId;
    protected CustomerEnrollmentWS customerEnrollment;
    protected String reason;

    public EnrollmentEvent(Integer entityId, Integer customerEnrollmentId) {
        this.entityId = entityId;
        this.customerEnrollment = new CustomerEnrollmentBL().getWS(customerEnrollmentDAS.findNow(customerEnrollmentId));
    }

    public EnrollmentEvent(Integer entityId, Integer customerEnrollmentId, String reason) {
        this(entityId, customerEnrollmentId);
        this.reason = reason;
    }

    public EnrollmentEvent(Integer entityId, CustomerEnrollmentWS customerEnrollment) {
        this.entityId = entityId;
        this.customerEnrollment = customerEnrollment;
    }

    public EnrollmentEvent(Integer entityId, CustomerEnrollmentWS customerEnrollment, String reason) {
        this(entityId, customerEnrollment);
        this.reason = reason;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public CustomerEnrollmentWS getCustomerEnrollment() {
        return customerEnrollment;
    }

    public String getReason() {
        return reason;
    }
}