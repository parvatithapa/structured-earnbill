package com.sapienter.jbilling.server.util.audit.logConstants;

/**
 * Created by Martin on 10/10/2016.
 */
public enum LogConstants {
    STATUS_SUCCESS("Successful"), STATUS_NOT_SUCCESS("Unsuccessful"),STATUS_FATAL("Fatal"),
    ACTION_CREATE("Create"), ACTION_UPDATE("Update"), ACTION_DELETE("Delete"), ACTION_GET("Get"),
    ACTION_EVENT("Event"),ACTION_APPLY("Apply"),ACTION_PROCESS("Process"),
    MODULE_PAYMENT("Payment"),MODULE_PAYMENT_LINK("PaymentLink"),MODULE_CUSTOMER("Customer"),MODULE_USER("User"),
    MODULE_PERMISSIONS("Permissions"), MODULE_LOGIN("Login");

    private String name;

    LogConstants(String name){
        this.name=name;
    }

    @Override
    public String toString() {return name;}
}
