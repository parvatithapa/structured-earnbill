package com.sapienter.jbilling.server.customerEnrollment;

/**
 * Created by neeraj on 13/8/15.
 */
public enum CustomerEnrollmentStatus {
    PENDING("PENDING"), VALIDATED("VALIDATED"), REJECTED("REJECTED"), ENROLLED("ENROLLED");

    private String name;

    private CustomerEnrollmentStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
