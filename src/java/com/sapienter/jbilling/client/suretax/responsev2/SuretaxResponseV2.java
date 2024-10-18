package com.sapienter.jbilling.client.suretax.responsev2;

import com.sapienter.jbilling.client.suretax.response.IResponseHeader;
import com.sapienter.jbilling.client.suretax.response.ItemMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SuretaxResponseV2 implements IResponseHeader {
    @JsonProperty("Successful")
    public String successful;
    @JsonProperty("ResponseCode")
    public String responseCode;
    @JsonProperty("HeaderMessage")
    public String headerMessage;
    @JsonProperty("ItemMessages")
    public List<ItemMessage> itemMessages;
    @JsonProperty("ClientTracking")
    public String clientTracking;
    @JsonProperty("TotalTax")
    public String totalTax;
    @JsonProperty("TransId")
    public String transId;
    @JsonProperty("GroupList")
    public List<Group> groupList;
    public String jsonString;

    @Override
    public void setSuccessful(String successful) {
        this.successful = successful;
    }

    @Override
    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    @Override
    public void setHeaderMessage(String headerMessage) {
        this.headerMessage = headerMessage;
    }

    @Override
    public List<ItemMessage> getItemMessages() {
        return itemMessages;
    }

    public void setItemMessages(List<ItemMessage> itemMessages) {
        this.itemMessages = itemMessages;
    }

    @Override
    public void setClientTracking(String clientTracking) {
        this.clientTracking = clientTracking;
    }

    @Override
    public void setTotalTax(String totalTax) {
        this.totalTax = totalTax;
    }

    @Override
    public void setTransId(String transId) {
        this.transId = transId;
    }

    @Override
    public String getSuccessful() {
        return successful;
    }

    @Override
    public String getResponseCode() {
        return responseCode;
    }

    @Override
    public String getHeaderMessage() {
        return headerMessage;
    }

    @Override
    public String getClientTracking() {
        return clientTracking;
    }

    @Override
    public String getTotalTax() {
        return totalTax;
    }

    @Override
    public String getTransId() {
        return transId;
    }

    @Override
    public String getJsonString() {
        return jsonString;
    }

    public void setGroupList(List<Group> groupList) {
        this.groupList = groupList;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    @Override
    public String toString() {
        return "SuretaxResponseV2 [successful=" + successful + ", responseCode="
                + responseCode + ", headerMessage=" + headerMessage
                + ", itemMessages=" + itemMessages + ", clientTracking="
                + clientTracking + ", totalTax=" + totalTax + ", transId="
                + transId + ", groupList=" + groupList + "]";
    }

}
