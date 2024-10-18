/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.process.event;

import java.util.Date;

import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.system.event.Event;

public class AboutToGenerateInvoices implements Event {

    private Integer entityId;
    private Integer userId;
    private Date runDate;
    private BillingProcessDTO process;

    public AboutToGenerateInvoices(Integer entityId, Integer userId, Date runDate, BillingProcessDTO process) {
        this.entityId = entityId;
        this.userId = userId;
        this.runDate = runDate;
        this.process = process;
    }

    public AboutToGenerateInvoices(Integer entityId, Integer userId, Date runDate) {
        this(entityId, userId, runDate, null);
    }

    @Override
    public String getName() {
        return "ABOUT TO GENERATE INVOICE";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Date getRunDate() {
        return runDate;
    }

    public BillingProcessDTO getProcess() {
        return process;
    }

}
