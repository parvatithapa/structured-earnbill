package com.sapienter.jbilling.client.suretax.response;

import com.sapienter.jbilling.client.suretax.IResponse;

import java.util.List;

/**
 * Common interface for SurTax v1 and v2 responses
 */
public interface IResponseHeader extends IResponse {
    void setSuccessful(String successful);

    void setResponseCode(String responseCode);

    void setHeaderMessage(String headerMessage);

    void setClientTracking(String clientTracking);

    void setTotalTax(String totalTax);

    void setTransId(String transId);

    String getSuccessful();

    String getResponseCode();

    String getHeaderMessage();

    String getClientTracking();

    String getTotalTax();

    String getTransId();

    String getJsonString();

    List<ItemMessage> getItemMessages();
}
