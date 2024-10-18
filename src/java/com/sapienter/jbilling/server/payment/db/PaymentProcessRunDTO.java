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
package com.sapienter.jbilling.server.payment.db;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.sapienter.jbilling.server.util.Constants;

@Entity
@TableGenerator(
        name = "payment_process_run_GEN", 
        table = "jbilling_seqs", 
        pkColumnName = "name", 
        valueColumnName = "next_id", 
        pkColumnValue = "payment_process_run", 
        allocationSize = 10)
@Table(name = "payment_process_run")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class PaymentProcessRunDTO implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
    private Integer billingProcessId;
    private Date billingDate;
    private int runCount;

    public PaymentProcessRunDTO() {
    }

    public PaymentProcessRunDTO(Integer id) {
        this.id = id;
    }

    public PaymentProcessRunDTO(Integer billingProcessId, Date billingDate, int runCount) {
        this.billingProcessId = billingProcessId;
        this.billingDate = billingDate;
        this.runCount = runCount;
    }
    @Transient
    protected String getTable() {
        return Constants.TABLE_PAYMENT_PROCESS_RUN;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "payment_process_run_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    @Column(name = "billing_process_id", unique = true, nullable = false)
	public Integer getBillingProcessId() {
		return billingProcessId;
	}

	public void setBillingProcessId(Integer billingProcessId) {
		this.billingProcessId = billingProcessId;
	}
	@Column(name = "billing_date")
	public Date getBillingDate() {
		return billingDate;
	}

	public void setBillingDate(Date billingDate) {
		this.billingDate = billingDate;
	}

	@Column(name = "run_count")
	public int getRunCount() {
		return runCount;
	}

	public void setRunCount(int retryCount) {
		this.runCount = retryCount;
	}

}
