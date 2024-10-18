/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool;

import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.usagePool.util.Util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;

import com.sapienter.jbilling.server.security.WSSecured;

/**
 * CustomerUsagePoolWS
 * The WS object for Customer Usage Pool association.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class CustomerUsagePoolWS implements WSSecured, Serializable {

	private Integer id;
    private Integer customerId = null;
    private Integer userId = null;
    private Integer usagePoolId = null;
    private Integer planId = null;
    private String quantity;
    private String initialQuantity;
    @ConvertToTimezone
	private Date cycleEndDate;
	private Integer versionNum;
	private UsagePoolWS usagePool;
	private Integer orderId = null;
    @ConvertToTimezone
	private Date cycleStartDate;
    private String lastRemainingQuantity;
	
	public CustomerUsagePoolWS() {
	}
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getCustomerId() {
		return customerId;
	}
	public void setCustomerId(Integer customerId) {
		this.customerId = customerId;
	}
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public Integer getUsagePoolId() {
		return usagePoolId;
	}
	public void setUsagePoolId(Integer usagePoolId) {
		this.usagePoolId = usagePoolId;
	}
	public Integer getPlanId() {
		return planId;
	}
	public void setPlanId(Integer planId) {
		this.planId = planId;
	}
	public String getQuantity() {
		return quantity;
	}
	public BigDecimal getQuantityAsDecimal() {
        return Util.string2decimal(quantity);
    }
	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}
	public void setQuantity(BigDecimal quantity) {
        this.quantity = (quantity != null ? quantity.toString() : null);
    }
	public BigDecimal getInitialQuantityAsDecimal() {
        return Util.string2decimal(initialQuantity);
    }
	public String getInitialQuantity() {
		return initialQuantity;
	}
	public void setInitialQuantity(String initialQuantity) {
		this.initialQuantity = initialQuantity;
	}
	public void setInitialQuantity(BigDecimal initialQuantity) {
		 this.initialQuantity = (initialQuantity != null ? initialQuantity.toString() : null);
	}
	public Date getCycleEndDate() {
		return cycleEndDate;
	}
	public void setCycleEndDate(Date cycleEndDate) {
		this.cycleEndDate = cycleEndDate;
	}
	public Integer getVersionNum() {
		return versionNum;
	}
	public void setVersionNum(Integer versionNum) {
		this.versionNum = versionNum;
	}
	public Integer getOrderId() {
		return orderId;
	}
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}
	public Date getCycleStartDate() {
		return cycleStartDate;
	}
	public void setCycleStartDate(Date cycleStartDate) {
		this.cycleStartDate = cycleStartDate;
	}

	public String getLastRemainingQuantity() {
        return lastRemainingQuantity;
    }
	
    public BigDecimal getLastRemainingQuantityAsDecimal() {
        return Util.string2decimal(lastRemainingQuantity);
    }

    public void setLastRemainingQuantity(String lastRemainingQuantity) {
        this.lastRemainingQuantity = lastRemainingQuantity;
    }

    @Override
	public String toString() {
		return "CustomerUsagePoolWS [id=" + id + ", customerId=" + customerId
				+ ", usagePoolId=" + usagePoolId + ", planId=" + planId + ", quantity=" + quantity +", initialQuantity"+initialQuantity
				+ ", cycleEndDate=" + cycleEndDate + ", cycleStartDate=" + cycleStartDate + ", versionNum=" + versionNum 
				+ ", orderId=" + orderId + " ]";
	}

	@Override
	public Integer getOwningEntityId() {
		return null;
	}

	@Override
	public Integer getOwningUserId() {
		return userId;
	}

    public UsagePoolWS getUsagePool() {
        return usagePool;
    }

    public void setUsagePool(UsagePoolWS usagePool) {
        this.usagePool = usagePool;
    }

    /**
     * A comparator that is used to sort customer usage pools based on precedence provided at system level usage pools.
     * If precedence at usage pool level is same, then created date for system level usage pools is considered.
     */
    public static final Comparator<CustomerUsagePoolWS> CustomerUsagePoolsByPrecedenceOrCreatedDateComparator = new Comparator<CustomerUsagePoolWS> () {
        public int compare(CustomerUsagePoolWS customerUsagePool1, CustomerUsagePoolWS customerUsagePool2) {

            Integer precedence1 = customerUsagePool1.getUsagePool().getPrecedence();
            Integer precedence2 =  customerUsagePool2.getUsagePool().getPrecedence();
            if(precedence1.intValue() == precedence2.intValue()) {

                Date createDate1 = customerUsagePool1.getUsagePool().getCreatedDate();
                Date createDate2 =  customerUsagePool2.getUsagePool().getCreatedDate();

                return createDate1.compareTo(createDate2);
            }
            return precedence1.compareTo(precedence2);
        }
    };
    
}