package com.sapienter.jbilling.server.exception;

import com.sapienter.jbilling.common.SessionInternalError;


public class QuantityRatingException extends SessionInternalError {

    public QuantityRatingException(String s) {
        super(s);
    }

    public QuantityRatingException(String s, Throwable e) {
        super(s, e);
    }

    public QuantityRatingException(Exception e, String[] errors) {
        super(e, errors);
    }

    public QuantityRatingException(String msg, String[] errors) {
        super(msg, errors);
    }

    public QuantityRatingException(String msg, Throwable e, String[] errors) {
        super(msg, e, errors);
    }
}
