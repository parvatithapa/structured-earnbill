package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;

public class DtCDRConversionDataException extends RuntimeException {

    private JbillingMediationRecord record;
    private String errorCodes;

    public DtCDRConversionDataException(Throwable cause, JbillingMediationRecord record, String errorCodes) {
        super(cause);
        this.record = record;
        this.errorCodes = errorCodes;
    }

    public DtCDRConversionDataException(String message, Throwable cause, JbillingMediationRecord record, String errorCodes) {
        super(message, cause);
        this.record = record;
        this.errorCodes = errorCodes;
    }

    public JbillingMediationRecord getRecord() {
        return record;
    }

    public String getErrorCodes() {
        return errorCodes;
    }
}
