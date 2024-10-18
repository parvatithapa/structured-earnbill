package com.sapienter.jbilling.server.distributel;

@SuppressWarnings("serial")
public class InvalidProductPriceUpdateRequestException extends RuntimeException {

    public InvalidProductPriceUpdateRequestException(String errorMessage) {
        super(errorMessage);
    }
}
