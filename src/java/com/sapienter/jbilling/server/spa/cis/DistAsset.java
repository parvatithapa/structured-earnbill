package com.sapienter.jbilling.server.spa.cis;

/**
 * Created by Matías Cabezas on 29/09/17.
 */
public class DistAsset {

    private String name;
    private String type;
    private String orderId;
    private String dashboard;
    private String id;
    private String metaField;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDashboard() {
        return dashboard;
    }

    public void setDashboard(String dashboard) {
        this.dashboard = dashboard;
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

    public String getMetaField() {
        return metaField;
    }

    public void setMetaField(String metaField) {
        this.metaField = metaField;
    }

}
