package com.sapienter.jbilling.resources;

import java.io.Serializable;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderWS;

/**
 * Just a wrapper class for the OrderResource - REST endpoints
 * It contains order info needed to create or update order
 * Instances of this class are passed in request body as JSON objects
 */
@SuppressWarnings("serial")
public class OrderInfo implements Serializable {

    private OrderWS order;

    private OrderChangeWS[] orderChanges;

    private AssetWS[] assets;

    public OrderInfo() {}

    public OrderInfo(OrderWS order, OrderChangeWS[] orderChanges) {
        this.order = order;
        this.orderChanges = orderChanges;
    }

    public OrderInfo(OrderWS order, OrderChangeWS[] orderChanges,
            AssetWS[] assets) {
        super();
        this.order = order;
        this.orderChanges = orderChanges;
        this.assets = assets;
    }

    public OrderWS getOrder() {
        return order;
    }

    public void setOrder(OrderWS order) {
        this.order = order;
    }

    public OrderChangeWS[] getOrderChanges() {
        return orderChanges;
    }

    public void setOrderChanges(OrderChangeWS[] orderChanges) {
        this.orderChanges = orderChanges;
    }

    public AssetWS[] getAssets() {
        return assets;
    }

    public void setAssets(AssetWS[] assets) {
        this.assets = assets;
    }

}

