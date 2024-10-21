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
import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.AssertTrue;

import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.util.Constants;


/**
 * BillingProcessConfigurationWS
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
@ApiModel(value = "Billing process configuration data", description = "BillingProcessConfigurationWS model")
public class BillingProcessConfigurationWS implements Serializable, WSSecured {

    private int id;
    private Integer periodUnitId;
    private Integer entityId;
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
    @NotNull(message = "validation.error.is.required")
    private Date nextRunDate;
    private Integer generateReport;
    private Integer retries;
    private Integer daysForRetry;
    private Integer daysForReport;
    private int reviewStatus;
    private int dueDateUnitId;
    private int dueDateValue;
    private Integer dfFm;
    private Integer onlyRecurring;
    private Integer invoiceDateProcess;
    @Min(value = 1, message = "validation.error.min,1")
    private int maximumPeriods;
    private int autoPaymentApplication;
    private int autoCreditNoteApplication;
    private int applyCreditNotesBeforePayments;
    private boolean lastDayOfMonth = false;
    private String proratingType;
    private int autoPayment = 0;
    private Integer retryCount;
    private Integer skipEmails;
    private String skipEmailsDays;
    
    public BillingProcessConfigurationWS() {
    }

	public BillingProcessConfigurationWS(BillingProcessConfigurationWS ws) {
		this.id = ws.getId();
		this.periodUnitId = ws.getPeriodUnitId();
		this.entityId = ws.getEntityId();
		this.nextRunDate = ws.getNextRunDate();
		this.generateReport = ws.getGenerateReport();
		this.retries = ws.getRetries();
		this.daysForRetry = ws.getDaysForRetry();
		this.daysForReport = ws.getDaysForReport();
		this.reviewStatus = ws.getReviewStatus();
		this.dueDateUnitId = ws.getDueDateUnitId();
		this.dueDateValue = ws.getDueDateValue();
		this.dfFm = ws.getDfFm();
		this.onlyRecurring = ws.getOnlyRecurring();
		this.invoiceDateProcess = ws.getInvoiceDateProcess();
        this.maximumPeriods = ws.getMaximumPeriods();
		this.autoPaymentApplication = ws.getAutoPaymentApplication();
        this.autoCreditNoteApplication = ws.getAutoCreditNoteApplication();
        this.applyCreditNotesBeforePayments = ws.getApplyCreditNotesBeforePayments();
		this.lastDayOfMonth = ws.isLastDayOfMonth();
		this.proratingType = ws.getProratingType();
		this.autoPayment = ws.getAutoPayment();
		if(ws.getAutoPayment()==1){
			this.retryCount = ws.getRetryCount();
		 }
		this.skipEmails = ws.getSkipEmails();
		this.skipEmailsDays = ws.getSkipEmailsDays();
	}

    public BillingProcessConfigurationWS(BillingProcessConfigurationDTO dto) {
        this.id = dto.getId();
        this.periodUnitId = dto.getPeriodUnit() != null ? dto.getPeriodUnit().getId() : null ;
        this.entityId = dto.getEntity() != null ? dto.getEntity().getId() : null;
        this.nextRunDate = dto.getNextRunDate();
        this.generateReport = dto.getGenerateReport();
        this.retries = dto.getRetries();
        this.daysForRetry = dto.getDaysForRetry();
        this.daysForReport = dto.getDaysForReport();
        this.reviewStatus = dto.getReviewStatus();
        this.dueDateUnitId = dto.getDueDateUnitId();
        this.dueDateValue = dto.getDueDateValue();
        this.dfFm = dto.getDfFm();
        this.onlyRecurring = dto.getOnlyRecurring();
        this.invoiceDateProcess = dto.getInvoiceDateProcess();
        this.maximumPeriods = dto.getMaximumPeriods();
        this.autoPaymentApplication = dto.getAutoPaymentApplication();
        this.lastDayOfMonth = dto.getLastDayOfMonth();
        this.proratingType = null != dto.getProratingType() ? dto.getProratingType().getOptionText() : Constants.BLANK_STRING;
        this.autoPayment = dto.getAutoPayment();
        this.retryCount = dto.getRetryCount();
        this.skipEmails = dto.getSkipEmails();
        this.skipEmailsDays = dto.getSkipEmailsDays();
    }

	@ApiModelProperty(value = "Unique identifier of the billing process configuration")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Unique identifier of the period unit used in the billing process")
    public Integer getPeriodUnitId() {
        return periodUnitId;
    }

    public void setPeriodUnitId(Integer periodUnitId) {
        this.periodUnitId = periodUnitId;
    }

    @ApiModelProperty(value = "Unique identifier of the company for which the billing process configuration is defined")
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "This is the date when the billing process is scheduled to run")
    public Date getNextRunDate() {
        return nextRunDate;
    }

    public void setNextRunDate(Date nextRunDate) {
        this.nextRunDate = nextRunDate;
    }

    @ApiModelProperty(value = "Flag that if set will cause the result billing run to be a review run")
    public Integer getGenerateReport() {
        return generateReport;
    }

    public void setGenerateReport(Integer generateReport) {
        this.generateReport = generateReport;
    }

    @ApiModelProperty(value = "Number of retries of the billing process")
    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    @JsonIgnore
    public Integer getDaysForRetry() {
        return daysForRetry;
    }

    public void setDaysForRetry(Integer daysForRetry) {
        this.daysForRetry = daysForRetry;
    }

    @ApiModelProperty(value = "Number of days before the real billing process a review run can be created")
    public Integer getDaysForReport() {
        return daysForReport;
    }

    public void setDaysForReport(Integer daysForReport) {
        this.daysForReport = daysForReport;
    }

    @ApiModelProperty(value = "This field contains the status for the last billing run if it was a review run." +
            " It can have the values GENERATED (1), APPROVED (2) or DISAPPROVED (3)")
    public int getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(int reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    @ApiModelProperty(value = "Unique identifier of the period unit used when calculating the due date of the " +
            "invoices generated in the billing process")
    public int getDueDateUnitId() {
        return dueDateUnitId;
    }

    public void setDueDateUnitId(int dueDateUnitId) {
        this.dueDateUnitId = dueDateUnitId;
    }

    @ApiModelProperty(value = "Number of period units used when calculating the due date of the " +
            "invoices generated in the billing process")
    public int getDueDateValue() {
        return dueDateValue;
    }

    public void setDueDateValue(int dueDateValue) {
        this.dueDateValue = dueDateValue;
    }

    @ApiModelProperty(value = "Specific for Italian billing/accounting")
    public Integer getDfFm() {
        return dfFm;
    }

    public void setDfFm(Integer dfFm) {
        this.dfFm = dfFm;
    }

    @ApiModelProperty(value = "If this flag is set the process will require a customer to have at least one active" +
            " recurring order in the included period before an invoice is generated")
    public Integer getOnlyRecurring() {
        return onlyRecurring;
    }

    public void setOnlyRecurring(Integer onlyRecurring) {
        this.onlyRecurring = onlyRecurring;
    }

    @ApiModelProperty(value = "Flag used for choosing how the create date on generated invoices is set." +
            " 0 - the date is set to the date when the billing process is run;" +
            " 1 - the customer’s next invoice date is used")
    public Integer getInvoiceDateProcess() {
        return invoiceDateProcess;
    }

    public void setInvoiceDateProcess(Integer invoiceDateProcess) {
        this.invoiceDateProcess = invoiceDateProcess;
    }

    @ApiModelProperty(value = "Limit on how many order periods from a purchase order will be included in an invoice." +
            " It applies only to recurring purchase orders")
    public int getMaximumPeriods() {
        return maximumPeriods;
    }

    public void setMaximumPeriods(int maximumPeriods) {
        this.maximumPeriods = maximumPeriods;
    }

    @ApiModelProperty(value = "Flag used if the billing process should attempt to apply any payments with" +
             " positive balance to the newly generated invoices")
    public int getAutoPaymentApplication() {
        return autoPaymentApplication;
    }

    public void setAutoPaymentApplication(int autoPaymentApplication) {
        this.autoPaymentApplication = autoPaymentApplication;
    }

    public int getAutoCreditNoteApplication() {
		return autoCreditNoteApplication;
	}

	public void setAutoCreditNoteApplication(int autoCreditNoteApplication) {
		this.autoCreditNoteApplication = autoCreditNoteApplication;
	}

	public int getApplyCreditNotesBeforePayments() {
		return applyCreditNotesBeforePayments;
	}

	public void setApplyCreditNotesBeforePayments(int applyCreditNotesBeforePayments) {
		this.applyCreditNotesBeforePayments = applyCreditNotesBeforePayments;
	}

    @ApiModelProperty(value = "Flag set if the billing process runs at the end of the month")
    @JsonProperty(value = "lastDayOfMonth")
    public boolean isLastDayOfMonth() {
		return lastDayOfMonth;
	}

	public void setLastDayOfMonth(boolean lastDayOfMonth) {
		this.lastDayOfMonth = lastDayOfMonth;
	}

	@ApiModelProperty(value = "Type of prorating option the billing process will use")
	public String getProratingType() {
		return proratingType;
	}

	public void setProratingType(String proratingType) {
		this.proratingType = proratingType;
	}

	@Override
    public String toString() {
        return "BillingProcessConfigurationWS{"
               + "id=" + id
               + ", entityId=" + entityId
               + ", nextRunDate=" + nextRunDate
               + ", generateReport=" + generateReport
               + ", retries=" + retries
               + ", daysForRetry=" + daysForRetry
               + ", daysForReport=" + daysForReport
               + ", reviewStatus=" + reviewStatus
               + ", dueDateUnitId=" + dueDateUnitId
               + ", dueDateValue=" + dueDateValue
               + ", dfFm=" + dfFm
               + ", onlyRecurring=" + onlyRecurring
               + ", invoiceDateProcess=" + invoiceDateProcess
               + ", maximumPeriods=" + maximumPeriods
               + ", autoPaymentApplication=" + autoPaymentApplication
               + ", autoCreditNoteApplication=" + autoCreditNoteApplication
               + ", applyCreditNotesBeforePayments=" + applyCreditNotesBeforePayments
               + ", periodUnitId=" + periodUnitId
               + ", lastDayOfMonth=" + lastDayOfMonth
               + ", proratingType=" + proratingType
               + ", autoPayment=" + autoPayment
               + ", retryCount=" + retryCount
               + ", skipEmails=" + skipEmails
               + ", skipEmailsDays=" + skipEmailsDays
               + '}';
    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return this.entityId;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @ApiModelProperty(value = "Is it an auto payment")
    public int getAutoPayment() {
		return autoPayment;
	}

	public void setAutoPayment(int autoPayment) {
		this.autoPayment = autoPayment;
	}

    @ApiModelProperty(value = "Retry count")
	public Integer getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
	}
	
	@ApiModelProperty(value = "Skip emails sending in bill run")
    public Integer getSkipEmails() {
        return skipEmails;
    }

    public void setSkipEmails(Integer skipEmails) {
        this.skipEmails = skipEmails;
    }
    
    @ApiModelProperty(value = "Skip emails days values")
    public String getSkipEmailsDays() {
        return skipEmailsDays;
    }

    public void setSkipEmailsDays(String skipEmailsDays) {
        this.skipEmailsDays = skipEmailsDays;
    }
    
    @AssertTrue(message="Skip emails Days can not be empty")
    private boolean isValidateskipEmails() {
        return !(this.skipEmails.equals(1) && (this.skipEmailsDays == null || this.skipEmailsDays.equals("")));
    }
	
}
