package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.server.invoiceTemplate.domain.CommonLines;
import com.sapienter.jbilling.server.invoiceTemplate.domain.TableLines;

/**
 * @author Klim
 */
public interface FieldProcessor {

    String expression(FieldSetup fs, TableLines cl);
}
