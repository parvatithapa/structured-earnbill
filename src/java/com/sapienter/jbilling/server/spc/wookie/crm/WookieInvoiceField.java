package com.sapienter.jbilling.server.spc.wookie.crm;

import java.util.Arrays;

import com.sapienter.jbilling.server.metafields.MetaFieldType;

enum WookieInvoiceField {

    // optinal billing address fields
    BILL_CITY(MetaFieldType.CITY), BILL_CODE(MetaFieldType.POSTAL_CODE), BILL_COUNTRY(MetaFieldType.COUNTRY_NAME),
    BILL_STATE((MetaFieldType.STATE_PROVINCE)), BILL_POBOX((MetaFieldType.POST_BOX)),

    // optional shipping address fields
    SHIP_CITY(MetaFieldType.CITY), SHIP_CODE((MetaFieldType.POSTAL_CODE)), SHIP_COUNTRY(MetaFieldType.COUNTRY_NAME),
    SHIP_STATE(MetaFieldType.STATE_PROVINCE), SHIP_POBOX((MetaFieldType.POST_BOX));

    private MetaFieldType metaFieldType;

    WookieInvoiceField(MetaFieldType metaFieldType) {
        this.metaFieldType = metaFieldType;
    }

    static String[] getFieldInLowerCase() {
        return Arrays.stream(values()).map(field -> field.name().toLowerCase()).toArray(String[]::new);
    }

    MetaFieldType getMetaFieldType() {
        return metaFieldType;
    }

    static WookieInvoiceField[] getBillingAITFields() {
        return new WookieInvoiceField[] {BILL_CITY, BILL_CODE, BILL_COUNTRY, BILL_POBOX, BILL_STATE};
    }

    static WookieInvoiceField[] getShippingAITFields() {
        return new WookieInvoiceField[] {SHIP_CITY, SHIP_CODE, SHIP_COUNTRY, SHIP_STATE, SHIP_POBOX};
    }
}
