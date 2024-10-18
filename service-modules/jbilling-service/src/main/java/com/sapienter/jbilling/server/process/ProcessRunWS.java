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

package com.sapienter.jbilling.server.process;


import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ProcessRunWS
 *
 * @author Brian Cowdery
 * @since 25-10-2010
 */
@ApiModel(value = "Process run data", description = "ProcessRunWS model")
public class ProcessRunWS implements Serializable {

    private Integer id;
    private Integer billingProcessId;
    private Date runDate;
    @ConvertToTimezone
    private Date started;
    @ConvertToTimezone
    private Date finished;
    private Integer invoicesGenerated;
    @ConvertToTimezone
    private Date paymentFinished;
    private List<ProcessRunTotalWS> processRunTotals = new ArrayList<ProcessRunTotalWS>(0);
    private Integer statusId;
    private String statusStr;

    public ProcessRunWS() {
    }

    @ApiModelProperty(value = "Unique identifier of the process run")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Unique identifier of the billing process for which the process run is defined")
    public Integer getBillingProcessId() {
        return billingProcessId;
    }

    public void setBillingProcessId(Integer billingProcessId) {
        this.billingProcessId = billingProcessId;
    }

    @ApiModelProperty(value = "Run date for the process run")
    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    @ApiModelProperty(value = "Start date for the process run")
    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    @ApiModelProperty(value = "End date for the process run")
    public Date getFinished() {
        return finished;
    }

    public void setFinished(Date finished) {
        this.finished = finished;
    }

    @ApiModelProperty(value = "Number of generated invoices by the process run")
    public Integer getInvoicesGenerated() {
        return invoicesGenerated;
    }

    public void setInvoicesGenerated(Integer invoicesGenerated) {
        this.invoicesGenerated = invoicesGenerated;
    }

    @ApiModelProperty(value = "Date when the payment finished for the process run")
    public Date getPaymentFinished() {
        return paymentFinished;
    }

    public void setPaymentFinished(Date paymentFinished) {
        this.paymentFinished = paymentFinished;
    }

    @ApiModelProperty(value = "List of process run total objects")
    public List<ProcessRunTotalWS> getProcessRunTotals() {
        return processRunTotals;
    }

    public void setProcessRunTotals(List<ProcessRunTotalWS> processRunTotals) {
        this.processRunTotals = processRunTotals;
    }

    @ApiModelProperty(value = "Flag that indicates the status of the process run")
    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    @ApiModelProperty(value = "Name of the process run status as determined from the field statusId")
    public String getStatusStr() {
        return statusStr;
    }

    public void setStatusStr(String statusStr) {
        this.statusStr = statusStr;
    }

    @Override
    public String toString() {
        return "ProcessRunWS{"
               + "id=" + id
               + ", billingProcessId=" + billingProcessId
               + ", runDate=" + runDate
               + ", started=" + started
               + ", finished=" + finished
               + ", invoicesGenerated=" + invoicesGenerated
               + ", paymentFinished=" + paymentFinished
               + ", processRunTotals=" + (processRunTotals != null ? processRunTotals.size() : null)
               + ", statusId=" + statusId
               + ", statusStr='" + statusStr + '\''
               + '}';
    }
}
