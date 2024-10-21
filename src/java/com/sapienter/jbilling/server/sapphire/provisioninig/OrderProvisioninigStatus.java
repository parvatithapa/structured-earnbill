package com.sapienter.jbilling.server.sapphire.provisioninig;

enum OrderProvisioninigStatus {

    PENDING_DEVICE_SWAP("Pending Device Swap"),
    DEVICE_SWAPPED("Device Swapped"),
    PENDING_UPDATE_OF_ASSET("Pending Update of Asset"),
    ASSET_DETAILS_UPDATED("Asset Details Updated"),
    PENDING_CHANGE_OF_PLAN("Pending Change of Plan"),
    PLAN_CHANGED("Plan Changed");

    private String status;

    private OrderProvisioninigStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
