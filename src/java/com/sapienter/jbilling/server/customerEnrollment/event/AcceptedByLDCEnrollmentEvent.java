package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;

public class AcceptedByLDCEnrollmentEvent extends EnrollmentEvent {

    public AcceptedByLDCEnrollmentEvent(Integer entityId, CustomerEnrollmentWS customerEnrollment, String reason) {
        super(entityId, customerEnrollment, reason);
    }

    public String getName() {
        return "Enrollment Accepted by LDC";
    }
}