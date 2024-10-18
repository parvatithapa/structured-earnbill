package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;

public class IncompleteInvalidDataEnrollmentEvent extends EnrollmentEvent {

    public IncompleteInvalidDataEnrollmentEvent(Integer entityId, CustomerEnrollmentWS customerEnrollment, String reason) {
        super(entityId, customerEnrollment, reason);
    }

    public String getName() {
        return "Incomplete Invalid Data";
    }
}