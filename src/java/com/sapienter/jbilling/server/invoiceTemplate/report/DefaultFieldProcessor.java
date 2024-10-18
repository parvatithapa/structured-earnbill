package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.invoiceTemplate.domain.TableLines;

/**
 * @author Klim
 */
public class DefaultFieldProcessor implements FieldProcessor {

    @Override
    public String expression(FieldSetup fs, TableLines cl) {
        return "$" + fs.getType().placeholder + '{' + fs.getName() + '}';
    }
}
