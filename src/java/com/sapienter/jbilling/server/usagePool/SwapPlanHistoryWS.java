package com.sapienter.jbilling.server.usagePool;

import java.io.Serializable;
import java.util.Date;

public class SwapPlanHistoryWS implements Serializable {
	
    private static final long serialVersionUID = 20130704L;

	private Integer id;
	private Integer oldPlanId;
	private Integer newPlanID;
	private Integer orderId;
	private Date swapDate;
	private String oldPlanOverageQuantity;
	private String oldPlanOverageAmount;
	private String oldPlanUsedfreeQuantity;
	
	public SwapPlanHistoryWS() {
		
	}
	
	public SwapPlanHistoryWS(Integer id, Integer oldPlanId, Integer newPlanID, Integer orderId,
			Date swapDate, String oldPlanOverageQuantity,
			String oldPlanOverageAmount, String oldPlanUsedfreeQuantity) {
		this.id = id;
		this.oldPlanId = oldPlanId;
		this.newPlanID = newPlanID;
		this.orderId = orderId;
		this.swapDate = swapDate;
		this.oldPlanOverageQuantity = oldPlanOverageQuantity;
		this.oldPlanOverageAmount = oldPlanOverageAmount;
		this.oldPlanUsedfreeQuantity = oldPlanUsedfreeQuantity;
	}

	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public Integer getOldPlanId() {
		return oldPlanId;
	}
	
	public void setOldPlanId(Integer oldPlanId) {
		this.oldPlanId = oldPlanId;
	}
	
	public Integer getNewPlanID() {
		return newPlanID;
	}
	
	public void setNewPlanID(Integer newPlanID) {
		this.newPlanID = newPlanID;
	}
	
	public Date getSwapDate() {
		return swapDate;
	}
	
	public void setSwapDate(Date swapDate) {
		this.swapDate = swapDate;
	}
	
	public String getOldPlanOverageQuantity() {
		return oldPlanOverageQuantity;
	}
	
	public void setOldPlanOverageQuantity(String oldPlanOverageQuantity) {
		this.oldPlanOverageQuantity = oldPlanOverageQuantity;
	}
	
	public String getOldPlanOverageAmount() {
		return oldPlanOverageAmount;
	}
	
	public void setOldPlanOverageAmount(String oldPlanOverageAmount) {
		this.oldPlanOverageAmount = oldPlanOverageAmount;
	}
	
	public String getOldPlanUsedfreeQuantity() {
		return oldPlanUsedfreeQuantity;
	}
	
	public void setOldPlanUsedfreeQuantity(String oldPlanUsedfreeQuantity) {
		this.oldPlanUsedfreeQuantity = oldPlanUsedfreeQuantity;
	}

	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	@Override
	public String toString() {
		return "SwapPlanHistoryWS [oldPlanId=" + oldPlanId + ", newPlanID="
				+ newPlanID + ", swapDate=" + swapDate
				+ ", oldPlanOverageQuantity=" + oldPlanOverageQuantity
				+ ", oldPlanOverageAmount=" + oldPlanOverageAmount
				+ ", oldPlanUsedfreeQuantity=" + oldPlanUsedfreeQuantity + "]";
	}
	
}
