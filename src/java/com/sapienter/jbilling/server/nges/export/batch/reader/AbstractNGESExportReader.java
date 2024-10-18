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
import com.sapienter.jbilling.server.user.db.UserDAS;
import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Created by hitesh on 3/8/16.
 */
public abstract class AbstractNGESExportReader implements ItemReader<Integer>, StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(NGESExportInvoiceReader.class));

    public Integer entityId;
    public List<Integer> ids;

    @Value("#{jobParameters['entityId']}")
    public void setEntityId(final Integer entityId) {
        this.entityId = entityId;
    }

    /**
     * This method return the next id from ids list.
     *
     * @return Integer
     */
    public Integer nextId() {
        if (ids.size() > 0) {
            return ids.remove(0);
        } else {
            return null;
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        LOG.debug("*****START AFTER STEP*****");
        clear();
        LOG.debug("*****STOP AFTER STEP*****");
        return null;
    }

    /**
     * This method used for clear entity.
     */
    public void clear() {
        entityId = null;
        ids = null;
    }
}
