package com.sapienter.jbilling.server.sapphire.provisioninig;

import java.io.Serializable;
import java.util.Arrays;

/**
 * This class is used to bind the request data used to send the information
 * regarding the sapphire provisioning requests to the sapphire restful api's
 *
 * @author jbilling
 *
 */
@SuppressWarnings("serial")
class SapphireProvisioningRequestWS implements Serializable {

    private Long id;
    private String officeId;
    private String requestType;
    private Integer clientId;
    private String firstName;
    private String lastName;
    private String email;
    private Integer planId;
    private Integer orderId;
    private String hardwareId;
    private SapphireDeviceWS[] devices;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public SapphireDeviceWS[] getDevices() {
        return devices;
    }

    public void setDevices(SapphireDeviceWS[] devices) {
        this.devices = devices;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProvisioningRequestWS [id=");
        builder.append(id);
        builder.append(", officeId=");
        builder.append(officeId);
        builder.append(", requestType=");
        builder.append(requestType);
        builder.append(", clientId=");
        builder.append(clientId);
        builder.append(", firstName=");
        builder.append(firstName);
        builder.append(", lastName=");
        builder.append(lastName);
        builder.append(", email=");
        builder.append(email);
        builder.append(", planId=");
        builder.append(planId);
        builder.append(", orderId=");
        builder.append(orderId);
        builder.append(", hardwareId=");
        builder.append(hardwareId);
        builder.append(", devices=");
        builder.append(Arrays.toString(devices));
        builder.append("]");
        return builder.toString();
    }

}
