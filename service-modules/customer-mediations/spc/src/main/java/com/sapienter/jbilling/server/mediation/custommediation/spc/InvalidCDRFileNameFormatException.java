package com.sapienter.jbilling.server.mediation.custommediation.spc;

/**
 * @author Neelabh
 * @since Jan 04, 2019
  */
public class InvalidCDRFileNameFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCDRFileNameFormatException(String message) {
        super(message);
    }
    
    public InvalidCDRFileNameFormatException() {
        
    }
}
