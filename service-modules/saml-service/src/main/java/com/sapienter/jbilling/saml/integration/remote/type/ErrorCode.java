package com.sapienter.jbilling.saml.integration.remote.type;

public enum ErrorCode {
    USER_ALREADY_EXISTS,
    USER_NOT_FOUND,
    ACCOUNT_NOT_FOUND,
    MAX_USERS_REACHED,
    UNAUTHORIZED,
    OPERATION_CANCELLED,
    CONFIGURATION_ERROR,
    INVALID_RESPONSE,
    TRANSPORT_ERROR,
    UNKNOWN_ERROR;

    public static ErrorCode fromString(String string) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.toString().equals(string)) {
                return errorCode;
            }
        }
        return null;
    }
}
