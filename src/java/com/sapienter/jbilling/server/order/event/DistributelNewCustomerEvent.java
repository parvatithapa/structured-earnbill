package com.sapienter.jbilling.server.order.event;

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.db.UserDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pablo_galera on 17/01/2017.
 */
public class DistributelNewCustomerEvent implements Event {
    private Integer entityId;
    private UserDTO user;
    private Map<String, String> parameters = new HashMap<>();

    public DistributelNewCustomerEvent(Integer entityId, UserDTO user) {
        this.entityId = entityId;
        this.user = user;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Distributel New Customer event";
    }

    public UserDTO getUser() {
        return user;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("DistributelNewCustomerEvent: entityId = ").append(entityId);
        strBuilder.append(" user = ").append(user);
        strBuilder.append(" parameters = ").append(parameters);
        return strBuilder.toString();
    }

}
