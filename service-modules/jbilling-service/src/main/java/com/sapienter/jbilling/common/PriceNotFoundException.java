package com.sapienter.jbilling.common;

/**
 * @author Neelabh
 * @since April 10, 2020
 */
public class PriceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PriceNotFoundException(String message) {
        super(message);
    }

}
