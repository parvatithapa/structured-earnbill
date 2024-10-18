package com.sapienter.jbilling.subscribe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DtCancelValidationStatusResponse {

    private boolean cancellationAllowed;
    private String reason;

    public DtCancelValidationStatusResponse(boolean cancellationAllowed, String reason) {
        this.cancellationAllowed = cancellationAllowed;
        this.reason = reason;
    }

    public DtCancelValidationStatusResponse(){}

    public void setCancellationAllowed(boolean cancellationAllowed) {
        this.cancellationAllowed = cancellationAllowed;
    }

    public boolean getCancellationAllowed(){
        return this.cancellationAllowed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
