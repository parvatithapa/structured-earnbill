package com.sapienter.jbilling.server.sapphire.cis;

import java.util.Date;

/**
 * Created by Matías Cabezas on 29/09/17.
 */
public class SappAsset {

    private String name;
    private String type;
    private String orderId;
    private String id;
    private String assetIdentifier;
    private String status;
    private Date startDate;

    public String getAssetIdentifier() {
        return assetIdentifier;
    }

    public void setAssetIdentifier(String assetIdentifier) {
        this.assetIdentifier = assetIdentifier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
