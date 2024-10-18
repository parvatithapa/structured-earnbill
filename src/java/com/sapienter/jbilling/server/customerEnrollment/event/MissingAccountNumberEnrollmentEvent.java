package com.sapienter.jbilling.server.customerEnrollment.event;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;

public class MissingAccountNumberEnrollmentEvent extends EnrollmentEvent {

    public MissingAccountNumberEnrollmentEvent(Integer entityId, CustomerEnrollmentWS customerEnrollment) {
        super(entityId, customerEnrollment);
        this.reason = String.format("%s, %s, %s", customerEnrollment.getMetaFieldValue("NAME"), customerEnrollment.getMetaFieldValue("CITY"), customerEnrollment.getMetaFieldValue(FileConstants.STATE));
    }

    public String getName() {
        return "Missing Account Number";
    }
}