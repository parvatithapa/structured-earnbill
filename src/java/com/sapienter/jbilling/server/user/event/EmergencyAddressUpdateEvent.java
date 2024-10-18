package com.sapienter.jbilling.server.user.event;


import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;

/**
 * Created by taimoor on 4/9/17.
 */
public class EmergencyAddressUpdateEvent implements Event {

    public enum RequestType{
        ADD,
        UPDATE,
        DELETE,
        VERIFY,
        QUERY
    }

    private final RequestType requestType;
    private final Integer entityId;
    private final Integer userId;
    private String errorResponse;
    private boolean isUpdated;
    private final ContactDTO contactDto;

    public EmergencyAddressUpdateEvent(ContactDTO contactDto, RequestType requestType, Integer userId, Integer entityId) {
        this.contactDto = contactDto;
        this.requestType = requestType;
        this.entityId = entityId;
        this.userId = userId;
        this.errorResponse = "Initialized";
    }

    public String getName() {
        return "Emergency Address Update Event";
    }

    public Integer getEntityId() {
        return entityId;
    }

    public ContactDTO getContactDto() {
        return contactDto;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getErrorResponse() {
        return errorResponse;
    }

    public void setErrorResponse(String errorResponse) {
        this.errorResponse = errorResponse;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(boolean updated) {
        isUpdated = updated;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("EmergencyAddressUpdateEvent{");
        strBuilder.append("requestType=").append(requestType);
        strBuilder.append(", entityId=").append(entityId);
        strBuilder.append(", userId=").append(userId);
        strBuilder.append(", errorResponse='").append(errorResponse).append("'");
        strBuilder.append(", isUpdated=").append(isUpdated);
        strBuilder.append(", contactDto=").append(contactDto).append('}');
        return strBuilder.toString();
    }
}
