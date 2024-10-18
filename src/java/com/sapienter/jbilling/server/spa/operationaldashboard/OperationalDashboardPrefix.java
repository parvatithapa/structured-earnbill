package com.sapienter.jbilling.server.spa.operationaldashboard;

/**
 * Created by Matías Cabezas on 05/10/17.
 */
public enum OperationalDashboardPrefix {
    NEW("NEW_"), OLD("OLD_"), NONE("");

    private OperationalDashboardPrefix(String value) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }
}
