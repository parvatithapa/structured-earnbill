package com.sapienter.jbilling.server.customerEnrollment.csv;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;

public class CustomerEnrollmentEntryParserException extends RuntimeException {

    private CustomerEnrollmentWS customerEnrollment;
    private String description;
    private int line;

    public CustomerEnrollmentEntryParserException(CustomerEnrollmentWS customerEnrollment, String description, Exception e) {
        super(e);
        this.customerEnrollment = customerEnrollment;
        this.description = description;
    }

    public CustomerEnrollmentEntryParserException(CustomerEnrollmentWS customerEnrollment, String description) {
        this.customerEnrollment = customerEnrollment;
        this.description = description;
    }

    public CustomerEnrollmentWS getCustomerEnrollment() {
        return customerEnrollment;
    }

    @Override
    public String getMessage() {
        return description + String.format(" (line: %d).", line);
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }
}
