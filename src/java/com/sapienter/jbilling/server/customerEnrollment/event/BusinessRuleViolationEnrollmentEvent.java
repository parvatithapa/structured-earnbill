package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;

public class BusinessRuleViolationEnrollmentEvent extends EnrollmentEvent {

    public BusinessRuleViolationEnrollmentEvent(Integer entityId, CustomerEnrollmentWS customerEnrollment, String reason) {
        super(entityId, customerEnrollment, reason);
    }

    public String getName() {
        return "Business Rule Violation";
    }
}