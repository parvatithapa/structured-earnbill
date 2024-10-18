package com.sapienter.jbilling.server.mediation.customMediations.movius;

public enum MoviusMetaFieldName {

    CUSTOMER_ORG_ID("Org Id") ,

    ANVEO_OUTGOING_CALL_COUNTRY_CODES("Anveo Country Code List"),

    ANVEO_CALL_ITEM_ID("Set Item Id For Anveo Calls") ,

    TATA_CALL_ITEM_ID("Set Item Id For Tata Calls") ,

    SMS_ITEM_ID("Set Item Id For SMS");

    private final String fieldName;
    
    private MoviusMetaFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
