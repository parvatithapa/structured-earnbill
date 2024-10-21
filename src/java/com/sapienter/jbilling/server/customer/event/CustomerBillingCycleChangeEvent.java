package com.sapienter.jbilling.server.customer.event;

import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.db.UserDTO;
import java.util.HashMap;
import java.util.Map;

public class CustomerBillingCycleChangeEvent implements Event {

    private Integer entityId;
    private UserDTO user;
    private Map<String, String> parameters = new HashMap<>();

    public CustomerBillingCycleChangeEvent(Integer entityId, UserDTO user) {
        this.entityId = entityId;
        this.user = user;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getName() {
        return "Customer Billing Cycle Change Event";
    }

    public UserDTO getUser() {
        return user;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("CustomerBillingCycleChangeEvent: entityId = ").append(entityId);
        strBuilder.append(" user = ").append(user);
        strBuilder.append(" parameters = ").append(parameters);
        return strBuilder.toString();
    }

}
