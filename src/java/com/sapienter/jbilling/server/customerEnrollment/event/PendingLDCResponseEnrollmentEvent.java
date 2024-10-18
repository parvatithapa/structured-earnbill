package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;

public class PendingLDCResponseEnrollmentEvent extends EnrollmentEvent {

    public PendingLDCResponseEnrollmentEvent(Integer entityId, CustomerEnrollmentWS customerEnrollment, String reason) {
        super(entityId, customerEnrollment, reason);
    }

    public String getName() {
        return "Pending LDC Response";
    }
}