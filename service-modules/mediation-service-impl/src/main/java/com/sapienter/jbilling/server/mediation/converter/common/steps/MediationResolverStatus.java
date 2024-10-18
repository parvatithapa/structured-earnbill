package com.sapienter.jbilling.server.mediation.converter.common.steps;


import com.sapienter.jbilling.server.mediation.converter.common.Constants;

/**
 * Status from the mediation CDR resolution
 *
 * @author Panche Isajeski
 * @since 02/17/13
 */
public enum MediationResolverStatus {

    DUPLICATE(-1), //Not saved on DB
    ERROR(Constants.MEDIATION_RECORD_STATUS_ERROR_DETECTED),
    NOT_BILLABLE(Constants.MEDIATION_RECORD_STATUS_DONE_AND_NOT_BILLABLE),
    SUCCESS(Constants.MEDIATION_RECORD_STATUS_DONE_AND_BILLABLE),
    ERROR_DECLARED(Constants.MEDIATION_RECORD_STATUS_ERROR_DECLARED);
    private final Integer intValue;

    MediationResolverStatus(Integer intValue) {
        this.intValue = intValue;
    }

    public Integer getIntValue() {
        return intValue;
    }
}
