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
package com.sapienter.jbilling.server.notification.db;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.sapienter.jbilling.server.process.db.BillingProcessDTO;

@SuppressWarnings("serial")
@Entity
@TableGenerator(
        name = "invoice_email_process_info_GEN", 
        table = "jbilling_seqs", 
        pkColumnName = "name", 
        valueColumnName = "next_id", 
        pkColumnValue = "invoice_email_process_info", 
        allocationSize = 100)
@Table(name = "invoice_email_process_info")
public class InvoiceEmailProcessInfoDTO implements Serializable {
        
    private int id;
    private BillingProcessDTO billingProcess;
    private Integer jobExecutionId;
    private Integer emailsEstimated;
    private Integer emailsSent;
    private Integer emailsFailed;
    private Date startDatetime;
    private Date endDatetime;
    private String source;
    
    public InvoiceEmailProcessInfoDTO() {
    }

    public InvoiceEmailProcessInfoDTO(int id) {
        this.id = id;        
    }

    public InvoiceEmailProcessInfoDTO(int id, BillingProcessDTO billingProcess, Integer jobExecutionId,            
            Integer emailsEstimated, Integer emailsSent, Integer emailsFailed, Date startDatetime, Date endDatetime, String source) {
        this.id = id;
        this.billingProcess = billingProcess;
        this.jobExecutionId = jobExecutionId;
        this.emailsEstimated = emailsEstimated;
        this.emailsSent = emailsSent;
        this.emailsFailed = emailsFailed;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.source = source;
    }
    
    public InvoiceEmailProcessInfoDTO(BillingProcessDTO billingProcess, Integer jobExecutionId,            
            Integer emailsEstimated, Integer emailsSent, Integer emailsFailed, Date startDatetime, Date endDatetime, String source) {
        this.billingProcess = billingProcess;
        this.jobExecutionId = jobExecutionId;
        this.emailsEstimated = emailsEstimated;
        this.emailsSent = emailsSent;
        this.emailsFailed = emailsFailed;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.source = source;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "invoice_email_process_info_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_process_id")
    public BillingProcessDTO getBillingProcess() {
        return this.billingProcess;
    }

    public void setBillingProcess(BillingProcessDTO billingProcessDTO) {
        this.billingProcess = billingProcessDTO;
    }
    
    @Column(name = "job_execution_id")
    public Integer getJobExecutionId() {
        return this.jobExecutionId;
    }

    public void setJobExecutionId(Integer jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }
    
    @Column(name = "emails_estimated")
    public Integer getEmailsEstimated() {
        return this.emailsEstimated;
    }

    public void setEmailsEstimated(Integer emailsEstimated) {
        this.emailsEstimated = emailsEstimated;
    }
        
    @Column(name = "emails_sent")
    public Integer getEmailsSent() {
        return this.emailsSent;
    }

    public void setEmailsSent(Integer emailsSent) {
        this.emailsSent = emailsSent;
    }
    
    @Column(name = "emails_failed")
    public Integer getEmailsFailed() {
        return this.emailsFailed;
    }

    public void setEmailsFailed(Integer emailsFailed) {
        this.emailsFailed = emailsFailed;
    }
    
    @Column(name = "start_datetime")
    public Date getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(Date startDatetime) {
        this.startDatetime = startDatetime;
    }
    
    @Column(name = "end_datetime")
    public Date getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(Date endDatetime) {
        this.endDatetime = endDatetime;
    }
    
    @Column(name = "source")
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getBillingProcess().getId())
                .append("-")
                .append(id);

        return key.toString();
    }
}
