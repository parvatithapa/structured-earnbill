package com.sapienter.jbilling.server.audit.mock;

import com.sapienter.jbilling.server.audit.Auditable;

import java.io.Serializable;

/**
 * Created by marcomanzicore on 23/11/15.
 */
public class MockAuditableEntity implements Serializable, Auditable {

    private int id;
    private String value;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getAuditKey(Serializable id) {
        return "mock-audit-key-" + id;
    }
}
