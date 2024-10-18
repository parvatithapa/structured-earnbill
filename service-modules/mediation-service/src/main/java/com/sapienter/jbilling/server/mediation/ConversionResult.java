package com.sapienter.jbilling.server.mediation;




import java.io.Serializable;

/**
 * Created by marcolin on 06/10/15.
 */
public class ConversionResult implements Serializable {

    private JbillingMediationRecord recordCreated;
    private ICallDataRecord recordProcessed;
    private JbillingMediationErrorRecord errorRecord;

    public ICallDataRecord getRecordProcessed() {
        return recordProcessed;
    }

    public void setRecordProcessed(ICallDataRecord recordProcessed) {
        this.recordProcessed = recordProcessed;
    }

    public JbillingMediationRecord getRecordCreated() {
        return recordCreated;
    }

    public void setRecordCreated(JbillingMediationRecord recordCreated) {
        this.recordCreated = recordCreated;
    }

    public void setErrorRecord(JbillingMediationErrorRecord errorRecord) {
        this.errorRecord = errorRecord;
    }

    public JbillingMediationErrorRecord getErrorRecord() {
        return errorRecord;
    }
}
