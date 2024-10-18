package com.sapienter.jbilling.server.customerEnrollment.event;

public class RejectedByLDCEnrollmentEvent extends EnrollmentEvent {

    public RejectedByLDCEnrollmentEvent(Integer entityId, Integer customerEnrollmentId, String reason) {
        super(entityId, customerEnrollmentId, reason);
    }

    public String getName() {
        return "Enrollment Rejected by LDC";
    }
}