package com.sapienter.jbilling.server.fileProcessing.xmlParser;

import java.util.List;

/**
 * Created by aman on 24/8/15.
 */
public class RecordStructure {
    Record record;
    String loop;
    List<RecordStructure> childRecord;

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public String getLoop() {
        return loop;
    }

    public void setLoop(String loop) {
        this.loop = loop;
    }

    public List<RecordStructure> getChildRecord() {
        return childRecord;
    }

    public void setChildRecord(List<RecordStructure> childRecord) {
        this.childRecord = childRecord;
    }

    @Override
    public String toString() {
        return "RecordStructure{" +
                "record=" + record +
                ", loop='" + loop + '\'' +
                ", childRecord=" + childRecord +
                '}';
    }
}
