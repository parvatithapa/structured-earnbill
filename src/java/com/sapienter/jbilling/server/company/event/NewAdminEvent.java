package com.sapienter.jbilling.server.company.event;

import com.sapienter.jbilling.server.system.event.Event;

import java.util.Map;

/**
 * Created by vivek on 10/8/15.
 */
public class NewAdminEvent implements Event {

    private Integer entityId;   // Current logged in Company's id
    private Integer targetEntityId; // copied company's id
    private Map<String, String> loginCredentials;
    private String email;
    private String leadUsername;


    public NewAdminEvent(Integer entityId, Integer targetEntityId, Map<String, String> credentialList, String email, String leadUsername) {
        this.entityId = entityId;
        this.targetEntityId = targetEntityId;
        this.loginCredentials = credentialList;
        this.email = email;
        this.leadUsername = leadUsername;
    }

    public String getName() {
        return "New Admin Created";
    }

    public Map<String, String> getLoginCredentials() {
        return loginCredentials;
    }

    public String getEmail() {
        return email;
    }

    public Integer getTargetEntityId() {

        return targetEntityId;
    }

    public String getleadUsername() {
        return leadUsername;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        return "NewAdminEvent{" +
                "entityId=" + entityId +
                ", email='" + email + '\'' +
                ", leadUsername='" + leadUsername + '\'' +
                '}';
    }

}
