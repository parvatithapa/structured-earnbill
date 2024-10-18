package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.invoiceTemplate.domain.TableLines;

/**
 * @author Klim
 */
public class GroupCountProcessor implements FieldProcessor {

    @Override
    public String expression(FieldSetup fs, TableLines cl) {
        return "$V{" + cl.getGroupCriteria().getName() + "_COUNT}";
    }
}
