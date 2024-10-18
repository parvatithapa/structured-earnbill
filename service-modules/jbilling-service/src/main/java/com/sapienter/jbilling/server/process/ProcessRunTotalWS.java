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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * ProcessRunTotalWS
 *
 * @author Brian Cowdery
 * @since 25-10-2010
 */
@ApiModel(value = "Process run total data", description = "ProcessRunTotalWS model")
public class ProcessRunTotalWS implements Serializable {

    private Integer id;
    private Integer processRunId;
    private Integer currencyId;
    private String totalInvoiced;
    private String totalPaid;
    private String totalNotPaid;

    public ProcessRunTotalWS() {
    }

    @ApiModelProperty(value = "Unique identifier of the process run total")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Unique identifier of the process run for which the total is defined")
    public Integer getProcessRunId() {
        return processRunId;
    }

    public void setProcessRunId(Integer processRunId) {
        this.processRunId = processRunId;
    }

    @ApiModelProperty(value = "Unique identifier of the currency in which the process run amounts are being expressed")
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @JsonIgnore
    public String getTotalInvoiced() {
        return totalInvoiced;
    }

    @ApiModelProperty(value = "The total invoiced amount of the process run", dataType = "BigDecimal")
    @JsonProperty(value = "totalInvoiced")
    public BigDecimal getTotalInvoicedAsDecimal() {
        return Util.string2decimal(totalInvoiced);
    }

    @JsonIgnore
    public void setTotalInvoiced(String totalInvoiced) {
        this.totalInvoiced = totalInvoiced;
    }

    @JsonProperty(value = "totalInvoiced")
    public void setTotalInvoiced(BigDecimal totalInvoiced) {
        this.totalInvoiced = (totalInvoiced != null ? totalInvoiced.toString() : null);
    }

    @JsonIgnore
    public String getTotalPaid() {
        return totalPaid;
    }

    @ApiModelProperty(value = "The total paid amount of the process run", dataType = "BigDecimal")
    @JsonProperty(value = "totalPaid")
    public BigDecimal getTotalPaidAsDecimal() {
        return Util.string2decimal(totalPaid);
    }

    @JsonIgnore
    public void setTotalPaid(String totalPaid) {
        this.totalPaid = totalPaid;
    }

    @JsonProperty(value = "totalPaid")
    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = (totalPaid != null ? totalPaid.toString() : null);
    }

    @JsonIgnore
    public String getTotalNotPaid() {
        return totalNotPaid;
    }

    @ApiModelProperty(value = "The total not paid amount of the process run", dataType = "BigDecimal")
    @JsonProperty(value = "totalNotPaid")
    public BigDecimal getTotalNotPaidAsDecimal() {
        return Util.string2decimal(totalNotPaid);
    }

    @JsonIgnore
    public void setTotalNotPaid(String totalNotPaid) {
        this.totalNotPaid = totalNotPaid;
    }

    @JsonProperty(value = "totalNotPaid")
    public void setTotalNotPaid(BigDecimal totalNotPaid) {
        this.totalNotPaid = (totalNotPaid != null ? totalNotPaid.toString() : null);
    }
    
    @Override
    public String toString() {
        return "ProcessRunTotalWS{"
               + "id=" + id
               + ", processRunId=" + processRunId
               + ", currencyId=" + currencyId
               + ", totalInvoiced=" + totalInvoiced
               + ", totalPaid=" + totalPaid
               + ", totalNotPaid=" + totalNotPaid
               + '}';
    }
}
