package com.sapienter.jbilling.server.spa;

/**
 * DistributelException class
 * 
 * This exception is thrown when there is some issue processing the spa import
 * 
 * @author Leandro Bagur
 * @since 05/12/17.
 */
public class DistributelException extends Exception {

    private SpaErrorCodes errorCode;
    
    public DistributelException(String message, SpaErrorCodes errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public DistributelException(Throwable cause, SpaErrorCodes errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public DistributelException(String message, Throwable cause, SpaErrorCodes errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public SpaErrorCodes getErrorCode() {
        return errorCode;
    }
    
}
