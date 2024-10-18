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
import java.util.List;

import com.sapienter.jbilling.server.pluggableTask.ActivePeriodChargingTask;
import com.sapienter.jbilling.server.system.event.Event;

/**
 * This event is triggered when the BP try to generate the invoice lines
 *
 * @author Leandro Zoi
 * @since 01/51/18
 *
 */
public class ApplySuspendedPeriods implements Event {

    private List<ActivePeriodChargingTask.SuspendedCycle> cycles;
    private Integer entityId;
    private Integer userId;
    private Date startDate;
    private Date endDate;

    public ApplySuspendedPeriods(List<ActivePeriodChargingTask.SuspendedCycle> cycles, Integer entityId, Integer userId,
            Date startDate, Date endDate) {
        this.cycles = cycles;
        this.entityId = entityId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public List<ActivePeriodChargingTask.SuspendedCycle> getCycles() {
        return cycles;
    }

    public Integer getUserId() {
        return userId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public String getName() {
        return "Apply Suspended Event";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public String toString() {
        return getName() + " - entity " + entityId;
    }
}
