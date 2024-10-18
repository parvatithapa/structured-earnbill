package com.sapienter.jbilling.client.suretax.responsev1;

import java.util.List;

import com.sapienter.jbilling.client.suretax.response.IResponseHeader;
import com.sapienter.jbilling.client.suretax.response.ItemMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SuretaxResponseV1 implements IResponseHeader {
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

    public void setSuccessful(String successful) {
        this.successful = successful;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public void setHeaderMessage(String headerMessage) {
        this.headerMessage = headerMessage;
    }

    public void setItemMessages(List<ItemMessage> itemMessages) {
        this.itemMessages = itemMessages;
    }

    public void setClientTracking(String clientTracking) {
        this.clientTracking = clientTracking;
    }

    public void setTotalTax(String totalTax) {
        this.totalTax = totalTax;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public void setGroupList(List<Group> groupList) {
        this.groupList = groupList;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
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
    public List<ItemMessage> getItemMessages() {
        return itemMessages;
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

    public List<Group> getGroupList() {
        return groupList;
    }

    @Override
    public String getJsonString() {
        return jsonString;
    }


    @Override
    public String toString() {
        return "SuretaxResponseV1 [successful=" + successful + ", responseCode="
                + responseCode + ", headerMessage=" + headerMessage
                + ", itemMessages=" + itemMessages + ", clientTracking="
                + clientTracking + ", totalTax=" + totalTax + ", transId="
                + transId + ", groupList=" + groupList + "]";
    }

}
