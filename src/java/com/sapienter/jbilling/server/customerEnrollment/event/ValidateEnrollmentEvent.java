package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by Neeraj Bhatt on 23/02/2016.
 */
public class ValidateEnrollmentEvent implements Event {

    private Integer entityId;
    private CustomerEnrollmentDTO enrollmentDTO;

    public ValidateEnrollmentEvent(Integer entityId, CustomerEnrollmentDTO enrollmentDTO) {
        this.entityId = entityId;
        this.enrollmentDTO = enrollmentDTO;
    }

    public String getName() {
        return "Validate Enrollment";
    }

    public Integer getEntityId() {
        return entityId;
    }

    public CustomerEnrollmentDTO getEnrollmentDTO() {
        return enrollmentDTO;
    }
}
