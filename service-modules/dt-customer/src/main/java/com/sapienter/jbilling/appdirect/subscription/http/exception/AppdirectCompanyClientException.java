package com.sapienter.jbilling.appdirect.subscription.http.exception;

public final class AppdirectCompanyClientException extends RuntimeException {

    public AppdirectCompanyClientException(String message) {
        super(message);
    }

    public AppdirectCompanyClientException(Throwable cause) {
        super(cause);
    }

    public AppdirectCompanyClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
