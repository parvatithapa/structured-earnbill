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

package com.sapienter.jbilling.server.nges.export.batch.reader;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDAS;
import org.apache.log4j.Logger;
import org.springframework.batch.core.StepExecution;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h1>NGES Export Invoice Reader!</h1>
 * The NGESExportInvoiceReader program implements an application that
 * simply fetch invoiceLine id's by entityId.
 * <p>
 * If you call transactional classes from a Spring Batch job, Spring’s transaction propagation can
 * interfere with the Spring Batch transaction because of the propagation level.
 * The REQUIRES_NEW propagation level could typically cause problems because
 * the application code runs in its own transaction, independent of the Spring Batch transaction.
 * So using Propagation.NOT_SUPPORTED for making this reader non-transactional.
 *
 * @author Hitesh Yadav
 * @version 4.6
 * @since 2016-08-16
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class NGESExportInvoiceReader extends AbstractNGESExportReader {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportInvoiceReader.class));

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("*****START BEFORE STEP*****");
        ids = new InvoiceLineDAS().findIdsByEntity(entityId);
        LOG.debug("*****STOP BEFORE STEP*****");
    }

    @Override
    public Integer read() throws Exception {
        LOG.debug("*****START READ*****");
        Integer invoiceLineId = nextId();
        LOG.debug("invoiceLineId:" + invoiceLineId);
        LOG.debug("*****STOP READ*****");
        return invoiceLineId;
    }
}
