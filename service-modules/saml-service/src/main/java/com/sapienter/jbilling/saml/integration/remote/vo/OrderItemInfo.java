package com.sapienter.jbilling.saml.integration.remote.vo;

import com.sapienter.jbilling.saml.integration.remote.type.PricingUnit;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "orderItem")
public class OrderItemInfo implements Serializable {
    private static final long serialVersionUID = 1850642171706386994L;

    private PricingUnit unit;
    private int quantity;

    public PricingUnit getUnit() {
        return unit;
    }

    public void setUnit(PricingUnit unit) {
        this.unit = unit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
