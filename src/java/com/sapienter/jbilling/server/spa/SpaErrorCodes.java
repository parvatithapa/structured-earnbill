package com.sapienter.jbilling.server.spa;

/**
 * Created by developer on 20/07/17.
 */
public enum SpaErrorCodes {

    GENERAL_ERROR(-1),
    LOOKING_FOR_USER_ERROR(-2),
    CREATING_CUSTOMER_ERROR(-3),
    GENERATING_ORDERS_ERROR(-4),
    GENERATING_INVOICE_ERROR(-5),
    SAVING_PAYMENT_ERROR(-6),
    ADDING_NEW_SERVICE_ERROR(-7),
    PROVISIONING_ACTIVITIES_ERROR(-8),
    EMAIL_INVOICE_NOTIFICATION_ERROR(-9),
    REPEATED_VOIP_PHONE_NUMBER_ERROR(-10),
    REPEATED_SERVICE_IDENTIFIER_NUMBER_ERROR(-11),
    REPEATED_MODEM_IDENTIFIER_NUMBER_ERROR(-12);

    private Integer value;

    SpaErrorCodes(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return this.value;
    }
    
}
