package com.sapienter.jbilling.exception;

import org.springframework.aop.ThrowsAdvice;

public class DtReserveInstanceException extends Throwable {

    Integer errorCode;

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    String errorMessage;

    public DtReserveInstanceException(Integer errorCode, String errorMessage){
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }


}
