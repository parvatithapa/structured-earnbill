package com.sapienter.jbilling.server.invoiceTemplate.domain;

import com.sapienter.jbilling.server.invoiceTemplate.report.ColumnSettings;
import com.sapienter.jbilling.server.invoiceTemplate.report.FieldSetup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author elmot
 */
public class InvoiceLines extends TableLines {

    private double minimalTotal = -Double.MAX_VALUE;

    public double getMinimalTotal() {
        return minimalTotal;
    }

    public void setMinimalTotal(double minimalTotal) {
        this.minimalTotal = minimalTotal;
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.accept(this);
    }
}
