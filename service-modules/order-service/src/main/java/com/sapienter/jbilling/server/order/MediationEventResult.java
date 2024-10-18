package com.sapienter.jbilling.server.order;

import java.math.BigDecimal;

/**
 * Created by marcolin on 03/11/15.
 */
public class MediationEventResult {
    private Integer orderLinedId;
    private Integer costOrderLineId;
    private BigDecimal amountForChange;
    private BigDecimal costAmountForChange;
    private BigDecimal quantityEvaluated;
    private Integer currentOrderId;
    private String exceptionMessage;
    private String errorCodes;
    private boolean quantityResolutionSuccess = true;
    
    public String getExceptionMessage() {
		return exceptionMessage;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

    public Integer getCostOrderLineId() {
        return costOrderLineId;
    }

    public void setCostOrderLineId(Integer costOrderLineId) {
        this.costOrderLineId = costOrderLineId;
    }

    public Integer getOrderLinedId() {
        return orderLinedId;
    }

    public void setOrderLinedId(Integer orderLinedId) {
        this.orderLinedId = orderLinedId;
    }

    public BigDecimal getAmountForChange() {
        return amountForChange;
    }

    public void setAmountForChange(BigDecimal amountForChange) {
        this.amountForChange = amountForChange;
    }

    public BigDecimal getCostAmountForChange() {
        return costAmountForChange;
    }

    public void setCostAmountForChange(BigDecimal costAmountForChange) {
        this.costAmountForChange = costAmountForChange;
    }

    public BigDecimal getQuantityEvaluated() {
        return quantityEvaluated;
    }

    public void setQuantityEvaluated(BigDecimal quantityEvaluated) {
        this.quantityEvaluated = quantityEvaluated;
    }

    public Integer getCurrentOrderId() {
        return currentOrderId;
    }

    public void setCurrentOrderId(Integer currentOrderId) {
        this.currentOrderId = currentOrderId;
    }
    
    public boolean hasException() {
    	return (getExceptionMessage()!=null && !getExceptionMessage().isEmpty());
    }

    public boolean hasQuantityEvaluated() {
		return (getQuantityEvaluated() != null && getQuantityEvaluated().compareTo(BigDecimal.ZERO) > 0);
    }

    public String getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(String errorCodes) {
        this.errorCodes = errorCodes;
    }

    public boolean isQuantityResolutionSuccess() {
        return quantityResolutionSuccess;
    }

    public void setQuantityResolutionSuccess(boolean quantityResolutionSuccess) {
        this.quantityResolutionSuccess = quantityResolutionSuccess;
    }
}
