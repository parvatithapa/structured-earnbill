package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.server.system.event.Event;

import java.util.List;

/**
 * Northern911FailureEvent
 * 
 * This event is triggered when try to update an emergency address but fails.
 * 
 * @author Leandro Bagur
 * @since 23/10/17.
 */
public class Northern911FailureEvent implements Event {
    
    private Integer userId;
    private Integer entityId;
    private List<String> errors;
    
    public Northern911FailureEvent(Integer userId, Integer entityId, List<String> errors) {
        this.userId = userId;
        this.entityId = entityId;
        this.errors = errors;
    }
    
    @Override
    public String getName() {
        return "Northern 911 Failure for user " + userId;
    }

    @Override
    public Integer getEntityId() {
        return this.entityId;
    }
    
    public Integer getUserId() {
        return this.userId;
    }
    
    public List<String> getErrors() {
        return this.errors;
    }
    
}
