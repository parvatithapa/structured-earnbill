/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.nges.export.batch.processor;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.nges.export.row.ExportProductRow;
import com.sapienter.jbilling.server.nges.export.row.ExportRow;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Value;


/**
 * Created by hitesh on 28/9/16.
 */
public class NGESExportProductProcessor extends AbstractNGESExportProcessor {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportProductProcessor.class));

    private PlanDAS planDAS;
    private String companyName;

    @Value("#{jobParameters['companyName']}")
    public void setCompanyName(final String companyName) {
        this.companyName = companyName;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("*****BEFORE STEP*****");
        planDAS = new PlanDAS();
    }

    @Override
    public ExportRow process(Integer planId) throws Exception {
        LOG.debug("find plan internal number for id:" + planId);
        String productId = planDAS.findInternalNumberByPlan(planId);
        return prepare(productId);
    }

    private ExportRow prepare(String productId) {
        LOG.debug("prepare row for product");
        ExportProductRow productRow = new ExportProductRow();
        productRow.setCompanyName(validate(FieldName.COMPANY_NAME, companyName, true));
        productRow.setProductId(validate(FieldName.PRODUCT_ID, productId, true));
        productRow.getRow();
        return productRow;
    }
}
