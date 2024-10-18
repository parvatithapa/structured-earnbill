package com.sapienter.jbilling.server.invoiceTemplate.domain;

import com.sapienter.jbilling.server.invoiceTemplate.report.ColumnSettings;
import com.sapienter.jbilling.server.invoiceTemplate.report.FieldSetup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author elmot
 */
public class EventLines extends TableLines {

    @Override
    public void visit(Visitor visitor) {
        visitor.accept(this);
    }
}
