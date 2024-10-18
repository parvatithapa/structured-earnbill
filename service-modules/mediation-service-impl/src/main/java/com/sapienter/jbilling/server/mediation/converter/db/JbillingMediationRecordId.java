package com.sapienter.jbilling.server.mediation.converter.db;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by marcolin on 08/10/15.
 */
public class JbillingMediationRecordId implements Serializable {
    String recordKey = null;
    Date eventDate = null;

    public JbillingMediationRecordId() {}

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }
}
