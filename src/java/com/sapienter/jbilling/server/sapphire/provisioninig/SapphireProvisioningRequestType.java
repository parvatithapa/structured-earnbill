package com.sapienter.jbilling.server.sapphire.provisioninig;

/**
 * Defined the constants that are used to call the sapphire provisioning rest services
 *
 * @author jbilling
 *
 */
enum SapphireProvisioningRequestType {

    ACTIVATION("ACTIVATION"),
    REACTIVATION("REACTIVATION"),
    RECONNECTION("RECONNECTION"),
    TERMINATION("TERMINATION"),
    CHANGE_CREDENTIALS("CHANGE-CREDENTIALS"),
    CHANGE_PLAN("CHANGE-PLAN"),
    DEVICE_SWAP("DEVICE-SWAP"),
    ADDON_ACTIVATION("ADDON-ACTIVATION"),
    ADDON_DISCONNECTION("ADDON-DISCONNECTION"),
    CHANGE_EX_DIRECTORY("CHANGE-ASSET-DETAILS"),
    NEW_PASSWORD_VOIP("NEW-PASSWORD-VOIP"),
    RENEWAL_BE("RENEWAL-BE"),
    RENEWAL_AE("RENEWAL-AE"),
    SUSPENSION("SUSPENSION"),
    DISCONNECTION("DISCONNECTION"),
    SERVICE_TRANSFER("SERVICE-TRANSFER");

    private String requestType;

    public String getRequestType() {
        return this.requestType;
    }

    private SapphireProvisioningRequestType(String requestType) {
        this.requestType = requestType;
    }
}
