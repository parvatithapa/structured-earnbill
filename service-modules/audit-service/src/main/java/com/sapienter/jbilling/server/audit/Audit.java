package com.sapienter.jbilling.server.audit;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by marcomanzicore on 23/11/15.
 */
public class Audit implements Serializable{
    private String key;
    private byte[] object;
    private Long timestamp;
    private String event;
    private Map<String, String> columns = new TreeMap<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    //Actually the object is a byte[] because UserDTO is on the parent classloader of grails and can't be found. After services this will not be needed anymore
    public byte[] getObject() {
        return object;
    }

    public void setObject(byte[] object) {
        this.object = object;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long time) {
        this.timestamp = time;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    public void setColumns(Map<String, String> columns) {
        this.columns = columns;
    }
}
