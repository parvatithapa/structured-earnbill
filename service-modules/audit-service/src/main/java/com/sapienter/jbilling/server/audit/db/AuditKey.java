package com.sapienter.jbilling.server.audit.db;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by marcomanzicore on 23/11/15.
 */
@Embeddable
public class AuditKey implements Serializable {

    @Id
    @Column(name = "audit_key", nullable = false)
    private String auditKey;
    @Id
    @Column(name = "timestamp", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;
    @Id
    @Column(name = "type")
    private String type;

    public String getAuditKey() {
        return auditKey;
    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
