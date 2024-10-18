package com.sapienter.jbilling.server.mediation;

/**
 * Created by marcolin on 7/4/14.
 */
public class OrderIdCompany {

    Integer orderId;
    Integer entityId;

    OrderIdCompany(Object orderId, Object entityId) {
        this.orderId = (Integer) orderId;
        this.entityId = (Integer) entityId;
    }
}
