package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.server.item.db.PlanDTO;


/**
 * Created by Fernando Sivila on 03/01/17.
 */
public interface QuoteTaxCalculationTask {

    PlanDTO calculateTax(PlanDTO plan, String province, String date, String languageId);

}
