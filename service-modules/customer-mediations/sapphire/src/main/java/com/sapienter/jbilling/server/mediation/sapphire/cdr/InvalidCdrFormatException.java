package com.sapienter.jbilling.server.mediation.sapphire.cdr;


@SuppressWarnings("serial")
public class InvalidCdrFormatException extends RuntimeException {

    public InvalidCdrFormatException(String message, Exception ex) {
        super(message, ex);
    }

    public InvalidCdrFormatException(String message) {
        super(message);
    }

}
