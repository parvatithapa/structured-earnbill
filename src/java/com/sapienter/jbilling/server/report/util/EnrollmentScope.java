package com.sapienter.jbilling.server.report.util;

/**
 * EnrollmentScope enum.
 * 
 * This class represents the two types of enrollment: new customers or new services. It is used as filter within the reports.
 * 
 * @author Leandro Bagur
 * @since 10/01/18.
 */
public enum EnrollmentScope {
    NEW_CUSTOMERS("New customers"),
    ALL_NEW_ORDERS("All new orders");

    private String name;

    EnrollmentScope(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
