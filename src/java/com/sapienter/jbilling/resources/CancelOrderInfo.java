package com.sapienter.jbilling.resources;

import java.io.Serializable;
import java.util.Date;
/**
 * Just a wrapper class for the OrderResource - REST endpoints
 * It contains orderId and activeUntilDate
 * Instance of this class is passed in request body as JSON object 
 *
 */
@SuppressWarnings("serial")
public class CancelOrderInfo implements Serializable {

    private Integer orderId;
    private Date activeUntil;
    public Integer getOrderId() {
        return orderId;
    }
    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }
    public Date getActiveUntil() {
        return activeUntil;
    }
    public void setActiveUntil(Date activeUntil) {
        this.activeUntil = activeUntil;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CancelOrderInfo [orderId=");
        builder.append(orderId);
        builder.append(", activeUntil=");
        builder.append(activeUntil);
        builder.append("]");
        return builder.toString();
    }

}
