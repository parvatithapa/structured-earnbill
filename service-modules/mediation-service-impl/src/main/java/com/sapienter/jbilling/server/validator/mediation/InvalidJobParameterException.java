package com.sapienter.jbilling.server.validator.mediation;

@SuppressWarnings("serial")
public class InvalidJobParameterException extends RuntimeException {

    public InvalidJobParameterException(String message, Exception ex) {
        super(message, ex);
    }
}
