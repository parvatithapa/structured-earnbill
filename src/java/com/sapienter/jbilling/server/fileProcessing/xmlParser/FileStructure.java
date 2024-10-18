package com.sapienter.jbilling.server.fileProcessing.xmlParser;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by aman on 24/8/15.
 */
public class FileStructure {
    List<Record> records;
    List<RecordStructure> recordStructures;

    public List<RecordStructure> getRecordStructures() {
        return recordStructures;
    }

    public void setRecordStructures(List<RecordStructure> recordStructures) {
        this.recordStructures = recordStructures;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    public Record findRecordByName(String recordName) {
        for(Record record:records){
            if(record.getRecId().getDefaultValue().equals(recordName)){
                return record;
            }
        }
        return null;
    }

    public List<String> recordNames() {
        List<String> names = new LinkedList<String>();
        for(Record record:records){
            names.add(record.getRecId().getDefaultValue());
        }
        return names;
    }

    public Record getRecord(final String recordName) {
        Optional<Record> record = records.stream().filter(p-> p.getRecId().getDefaultValue().equals(recordName)).findFirst();
        return record.isPresent()?record.get():null;
    }

}
