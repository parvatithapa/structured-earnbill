package com.sapienter.jbilling.server.mediation.customMediations.movius;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public class CDRFormatNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    
    public CDRFormatNotFoundException(String message) {
        super(message);
    }
    
    public CDRFormatNotFoundException() {
        
    }
}
