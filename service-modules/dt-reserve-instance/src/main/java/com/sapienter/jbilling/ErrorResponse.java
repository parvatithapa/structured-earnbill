package com.sapienter.jbilling;

public class ErrorResponse {

    private Integer code;
    private String reason;

    public ErrorResponse(Integer code, String reason){
        this.code = code;
        this.reason = reason;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
