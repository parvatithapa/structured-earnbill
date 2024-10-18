package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by vivek on 8/9/15.
 */
public class EnrollmentCompletionEvent implements Event {

    private Integer entityId;
    private Integer enrollmentId;

    public EnrollmentCompletionEvent(Integer entityId, Integer enrollmentId) {
        this.entityId = entityId;
        this.enrollmentId = enrollmentId;
    }

    public String getName() {
        return "Enrollment Completed";
    }

    public Integer getEntityId() {
        return entityId;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }
}
