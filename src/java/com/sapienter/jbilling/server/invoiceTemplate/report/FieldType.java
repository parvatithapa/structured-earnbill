package com.sapienter.jbilling.server.invoiceTemplate.report;

/**
 * Created by Klim on 20.01.14.
 */
public enum FieldType {

    Field('F'), Variable('V');

    public final char placeholder;

    FieldType(char placeholder) {
        this.placeholder = placeholder;
    }
}
