package com.sapienter.jbilling.server.usagePool.db;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.*;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

@Entity
@TableGenerator(
        name="swap_plan_history_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="swap_plan_history",
        allocationSize = 100
)
@Table(name = "swap_plan_history")
public class SwapPlanHistoryDTO {
	private Integer id;
	private Integer oldPlanId;
	private Integer newPlanID;
	private Integer orderId;
	private Date swapDate;
	private BigDecimal oldPlanOverageQuantity = BigDecimal.ZERO;
	private BigDecimal oldPlanOverageAmount = BigDecimal.ZERO;
	private BigDecimal oldPlanUsedfreeQuantity = BigDecimal.ZERO;
	
	
	public SwapPlanHistoryDTO() {
		swapDate = TimezoneHelper.serverCurrentDate();
	}
	
	public SwapPlanHistoryDTO(Integer oldPlanId, Integer newPlanID,  Integer orderId) {
		this.oldPlanId = oldPlanId;
		this.newPlanID = newPlanID;
		this.orderId = orderId;
		this.swapDate = TimezoneHelper.serverCurrentDate();
	}
	
	@Id
	@GeneratedValue(strategy= GenerationType.TABLE, generator="swap_plan_history_GEN")
	@Column(name="id", unique=true, nullable=false)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name="old_plan_id" , nullable =false)
	public Integer getOldPlanId() {
		return oldPlanId;
	}
	
	public void setOldPlanId(Integer oldPlanId) {
		this.oldPlanId = oldPlanId;
	}
	
	@Column(name="new_plan_id" , nullable =false)
	public Integer getNewPlanID() {
		return newPlanID;
	}
	
	public void setNewPlanID(Integer newPlanID) {
		this.newPlanID = newPlanID;
	}
	
	@Column(name="swap_date" , nullable =false)
	public Date getSwapDate() {
		return swapDate;
	}
	
	public void setSwapDate(Date swapDate) {
		this.swapDate = swapDate;
	}
	
	@Column(name="old_plan_overage_quantity", precision=17, scale=17)
	public BigDecimal getOldPlanOverageQuantity() {
		return oldPlanOverageQuantity;
	}
	
	public void setOldPlanOverageQuantity(BigDecimal oldPlanOverageQuantity) {
		this.oldPlanOverageQuantity = oldPlanOverageQuantity;
	}
	
	@Column(name="old_plan_overage_amount", precision=17, scale=17)
	public BigDecimal getOldPlanOverageAmount() {
		return oldPlanOverageAmount;
	}
	
	public void setOldPlanOverageAmount(BigDecimal oldPlanOverageAmount) {
		this.oldPlanOverageAmount = oldPlanOverageAmount;
	}
	
	@Column(name="old_plan_used_free_quantity", precision=17, scale=17)
	public BigDecimal getOldPlanUsedfreeQuantity() {
		return oldPlanUsedfreeQuantity;
	}
	
	public void setOldPlanUsedfreeQuantity(BigDecimal oldPlanUsedfreeQuantity) {
		this.oldPlanUsedfreeQuantity = oldPlanUsedfreeQuantity;
	}

	@Column(name="order_id", nullable = false)
	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	@Override
	public String toString() {
		return "SwapPlanHistoryDTO [oldPlanId=" + oldPlanId + ", newPlanID="
				+ newPlanID + ", orderId=" + orderId + ", swapDate=" + swapDate
				+ ", oldPlanOverageQuantity=" + oldPlanOverageQuantity
				+ ", oldPlanOverageAmount=" + oldPlanOverageAmount
				+ ", oldPlanUsedfreeQuantity=" + oldPlanUsedfreeQuantity + "]";
	}

}
