package com.sapienter.jbilling.server.invoiceTemplate.domain;

import com.sapienter.jbilling.server.invoiceTemplate.report.FieldSetup;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author elmot
 */
public abstract class CommonLines extends DocElement {

    private FieldSetup sortCriterion;

    public FieldSetup getSortCriterion() {
        return sortCriterion;
    }

    public void setSortCriterion(FieldSetup sortCriterion) {
        this.sortCriterion = sortCriterion;
    }

    @JsonIgnore
    public FieldSetup[] getSortCriteria() {
        return new FieldSetup[]{sortCriterion};
    }

    public abstract void visit(Visitor visitor);
}
