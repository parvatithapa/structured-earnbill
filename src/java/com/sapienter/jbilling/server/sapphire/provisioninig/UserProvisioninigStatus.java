package com.sapienter.jbilling.server.sapphire.provisioninig;

public enum UserProvisioninigStatus {

    PENDING_ACTIVATION("Pending Activation"),
    ACTIVATED("Activated"),
    PENDING_SUSPENSION("Pending Suspension"),
    SUSPENDED("Suspended"),
    PENDING_REACTIVATION("Pending Reactivation"),
    REACTIVATED("Reactivated"),
    PENDING_DISCONNECTION("Pending Disconnection"),
    DISCONNECTED("Disconnected"),
    PENDING_TERMINATION("Pending Termination"),
    TERMINATED("Terminated"),
    PENDING_RECONNECTION("Pending Reconnection"),
    RECONNECTED("Reconnected"),
    PENDING_CHANGE_OF_CREDENTIALS("Pending Change of Credentials"),
    CREDENTIALS_UPDATED("Credentials Updated"),
    PENDING_FOR_SERVICE_TRANSFER("Pending For Service Transfer"),
    SERVICE_TRANSFERRED("Service Transferred");

    private String status;
    private UserProvisioninigStatus(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }
}
