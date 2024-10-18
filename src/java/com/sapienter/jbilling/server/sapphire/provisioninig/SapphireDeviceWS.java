package com.sapienter.jbilling.server.sapphire.provisioninig;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is used to bind Assets into it and send to the sapphire
 * provisioning rest requests
 *
 * @author jbilling
 *
 */
@SuppressWarnings("serial")
class SapphireDeviceWS implements Serializable {

    @JsonProperty("_id")
    private String id;
    private String itemType;
    private String itemCode;
    private String serialNo;
    private String provisioningSerialnumber;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getProvserialnumber() {
        return provisioningSerialnumber;
    }

    public void setProvserialnumber(String provserialnumber) {
        this.provisioningSerialnumber = provserialnumber;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DeviceWS [id=");
        builder.append(id);
        builder.append(", itemType=");
        builder.append(itemType);
        builder.append(", itemCode=");
        builder.append(itemCode);
        builder.append(", serialNo=");
        builder.append(serialNo);
        builder.append(", provisioningSerialnumber=");
        builder.append(provisioningSerialnumber);
        builder.append("]");
        return builder.toString();
    }
}
