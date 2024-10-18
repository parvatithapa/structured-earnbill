package com.sapienter.jbilling.server.payment.tasks.paysafe;

import java.util.Arrays;

/**
 * Created by Fernando Sivila on 21/09/17.
 */
public enum PaySafeStatus {
    ACTIVE ("ACTIVE"), DISABLED("DISABLED"), CANCELLED("CANCELLED - DO NOT REACTIVATE");

    PaySafeStatus (String name) {
        this.name = name;
    }    
    private String name;

    public String getName() {
        return name;
    }

    public static PaySafeStatus getByName(String name) {
        return Arrays.stream(values()).filter(status -> status.getName().equals(name)).findFirst().orElse(null);
    }
}
