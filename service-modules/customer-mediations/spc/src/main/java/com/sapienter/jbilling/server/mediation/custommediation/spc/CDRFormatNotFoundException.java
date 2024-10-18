package com.sapienter.jbilling.server.mediation.custommediation.spc;

/**
 * @author Neelabh
 * @since Dec 18, 2018
  */
public class CDRFormatNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CDRFormatNotFoundException(String message) {
        super(message);
    }

}
