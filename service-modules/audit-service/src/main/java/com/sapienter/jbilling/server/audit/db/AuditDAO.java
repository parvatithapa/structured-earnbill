package com.sapienter.jbilling.server.audit.db;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by marcomanzicore on 23/11/15.
 */
@Entity
@Table(name = "audit")
@IdClass(AuditKey.class)
public class AuditDAO {

    @Id
    private String auditKey;
    @Id
    private Date timestamp;
    @Id
    private String type;
    @Column(name = "entity")
    @Lob
    private byte[] entity;
    @Column(name = "event")
    private String event;

    public AuditDAO() {}

    public String getAuditKey() {
        return auditKey;
    }

    public void setAuditKey(String auditKey) {
        this.auditKey = auditKey;
    }

    public byte[] getEntity() {
        return entity;
    }

    public void setEntity(byte[] entity) {
        this.entity = entity;
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

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }
}
