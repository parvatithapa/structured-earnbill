package com.sapienter.jbilling.server.util;

import java.io.Serializable;
import java.util.Date;

public class JobExecutionLineWS implements Serializable {
    private String type;
    private String name;
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
