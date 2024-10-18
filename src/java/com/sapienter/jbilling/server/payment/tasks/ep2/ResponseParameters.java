package com.sapienter.jbilling.server.payment.tasks.ep2;

public enum ResponseParameters {
    STATUS_CODE ("code"),
    TRANSACTION_STATE ("transaction-state"),
    TRANSACTION_ID ("transaction-id"),
    TOKEN_ID ("token-id"),
    STATUS_DESCRIPTION("description"), 
    AUTHORIZATION_CODE("authorization-code");
    
    private final String name;

    private ResponseParameters(String name) {
        this.name = name;
    }

    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}
