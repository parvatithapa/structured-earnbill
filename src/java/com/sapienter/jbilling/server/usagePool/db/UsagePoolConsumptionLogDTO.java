/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

/**
 * UsagePoolConsumptionLogDTO
 * This is a domain object representing the Usage Pool Consumption Log entity.
 * @author Amol Gadre
 * @since 10-Feb-2014
 */

package com.sapienter.jbilling.server.usagePool.db;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import com.sapienter.jbilling.server.user.db.CustomerDTO;

import javax.persistence.*;

import java.math.BigDecimal;
import java.util.*;

@Entity
@TableGenerator(
        name = "usage_pool_consumption_log_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "usage_pool_consumption_log",
        allocationSize = 100
)
@Table(name = "usage_pool_consumption_log")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class UsagePoolConsumptionLogDTO {

	private int id;
	private CustomerUsagePoolDTO customerUsagePool;
	private BigDecimal oldQuantity;
	private BigDecimal newQuantity;
	private BigDecimal percentageConsumption;
	private Date consumptionDate;
	private String actionExecuted;
	private int versionNum;
	
	public UsagePoolConsumptionLogDTO() {
		super();
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "usage_pool_consumption_log_GEN")
	@Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public boolean hasId() {
		return getId() > 0;
	}

	@ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="customer_usage_pool_id", nullable=true)
	public CustomerUsagePoolDTO getCustomerUsagePool() {
		return customerUsagePool;
	}

	public void setCustomerUsagePool(CustomerUsagePoolDTO customerUsagePool) {
		this.customerUsagePool = customerUsagePool;
	}

	@Column(name="old_quantity", precision=17, scale=17)
	public BigDecimal getOldQuantity() {
		return oldQuantity;
	}

	public void setOldQuantity(BigDecimal oldQuantity) {
		this.oldQuantity = oldQuantity;
	}

	@Column(name="new_quantity", precision=17, scale=17)
	public BigDecimal getNewQuantity() {
		return newQuantity;
	}

	public void setNewQuantity(BigDecimal newQuantity) {
		this.newQuantity = newQuantity;
	}

	@Column(name="percentage_consumption", precision=17, scale=17)
	public BigDecimal getPercentageConsumption() {
		return percentageConsumption;
	}

	public void setPercentageConsumption(BigDecimal percentageConsumption) {
		this.percentageConsumption = percentageConsumption;
	}

	@Column(name="consumption_date")
	public Date getConsumptionDate() {
		return consumptionDate;
	}

	public void setConsumptionDate(Date consumptionDate) {
		this.consumptionDate = consumptionDate;
	}

	@Column(name="action_executed", length=255)
	public String getActionExecuted() {
		return actionExecuted;
	}

	public void setActionExecuted(String actionExecuted) {
		this.actionExecuted = actionExecuted;
	}

	@Version
    @Column(name = "OPTLOCK")
	public int getVersionNum() {
		return versionNum;
	}

	public void setVersionNum(int versionNum) {
		this.versionNum = versionNum;
	}

	@Override
	public String toString() {
		return "UsagePoolConsumptionLogDTO [id=" + id + ", customerUsagePool="
				+ customerUsagePool + ", oldQuantity=" + oldQuantity
				+ ", newQuantity=" + newQuantity + ", percentageConsumption="
				+ percentageConsumption + ", consumptionDate="
				+ consumptionDate + ", actionExecuted=" + actionExecuted
				+ ", versionNum=" + versionNum + "]";
	}

}