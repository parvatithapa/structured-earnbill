package com.sapienter.jbilling.saml.integration.remote.vo;

import com.sapienter.jbilling.saml.integration.remote.type.PricingDuration;
import com.sapienter.jbilling.saml.integration.remote.type.PricingUnit;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "order")
public class OrderInfo implements Serializable {
    private static final long serialVersionUID = -6727466858109325508L;

    private String editionCode;
    private String addonOfferingCode;
    private PricingDuration pricingDuration;
    private List<OrderItemInfo> items = new ArrayList<>();

    @XmlElement(name = "editionCode")
    public String getEditionCode() {
        return editionCode;
    }

    public void setEditionCode(String editionCode) {
        this.editionCode = editionCode;
    }

    @XmlElement(name = "addonOfferingCode")
    public String getAddonOfferingCode() {
        return addonOfferingCode;
    }

    public void setAddonOfferingCode(String addonOfferingCode) {
        this.addonOfferingCode = addonOfferingCode;
    }

    @XmlElement(name = "pricingDuration")
    public PricingDuration getPricingDuration() {
        return pricingDuration;
    }

    public void setPricingDuration(PricingDuration pricingDuration) {
        this.pricingDuration = pricingDuration;
    }

    @XmlElement(name = "item")
    public List<OrderItemInfo> getItems() {
        return items;
    }

    public void setItems(List<OrderItemInfo> items) {
        this.items = items;
    }

    @XmlTransient
    public Integer getMaxUsers() {
        Integer maxUsers = null;
        for (OrderItemInfo item : items) {
            if (PricingUnit.USER.equals(item.getUnit())) {
                maxUsers = Integer.valueOf(item.getQuantity());
            }
        }
        return maxUsers;
    }
}
