package com.sapienter.jbilling.server.fileProcessing.xmlParser;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by aman on 24/8/15.
 */
public class Record {
    Field recId;
    List<Field> fields = new LinkedList<Field>();

    public Field getRecId() {
        return recId;

    }

    public void setRecId(Field recId) {
        this.recId = recId;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "Record{" +
                "recId=" + recId +
                ", fields=" + fields +
                '}';
    }
}
