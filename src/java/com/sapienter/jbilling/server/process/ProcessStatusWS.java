/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * ProcessStatusWS
 *
 * @author Brian Cowdery
 * @since 08/09/11
 */
@ApiModel(value = "ProcessStatusWS", description = "Process status model")
public class ProcessStatusWS implements Serializable {

    public enum State { RUNNING, FINISHED, FAILED }

    private State state;
    private Integer processId;
    private UUID mediationProcessId;
    @ConvertToTimezone
    private Date start;
    @ConvertToTimezone
    private Date end;

    public ProcessStatusWS() {

    }

    public ProcessStatusWS(State state, Integer processId, Date start, Date end) {
        this.state = state;
        this.processId = processId;
        this.start = start;
        this.end = end;
    }

    public ProcessStatusWS(State state, MediationProcess mediationProcess) {
        this.state = state;
        this.mediationProcessId = mediationProcess.getId();
        this.start = mediationProcess.getStartDate();
        this.end = mediationProcess.getEndDate();
    }

    @ApiModelProperty(value = "State of the process status")
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @ApiModelProperty(value = "Process unique identifier")
    public Integer getProcessId() {
        return processId;
    }

    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    @ApiModelProperty(value = "Unique identifier of the mediation process ID")
    public UUID getMediationProcessId() {
        return mediationProcessId;
    }

    public void setMediationProcessId(UUID mediationProcessId) {
        this.mediationProcessId = mediationProcessId;
    }

    @ApiModelProperty(value = "Date when the process has started")
    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    @ApiModelProperty(value = "Date when the process has ended.")
    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "ProcessStatusWS{"
               + "state=" + state
               + ", processId=" + processId
               + ", start=" + start
               + ", end=" + end
               + '}';
    }
}
