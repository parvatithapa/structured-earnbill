package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.invoiceTemplate.domain.TableLines;

/**
 * @author Klim
 */
public class AddGroupCountProcessor implements FieldProcessor {

    @Override
    public String expression(FieldSetup fs, TableLines cl) {
        return "$V{" + cl.getAddGroupCriteria().getName() + "_COUNT}";
    }
}