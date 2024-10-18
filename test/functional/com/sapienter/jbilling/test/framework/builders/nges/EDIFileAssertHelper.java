package com.sapienter.jbilling.test.framework.builders.nges;

import com.sapienter.jbilling.server.ediTransaction.EDIFileFieldWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileRecordWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
/**
 * Created by aman on 7/4/16.
 */
public class EDIFileAssertHelper {
    private EDIFileWS file;
    private String recordName;
    // Zero based index
    private Integer orderOfRecord;
    private Map<String, String> fields = new HashMap<String, String>();

    private EDIFileAssertHelper(EDIFileWS file) {
        if (file == null) {
            throw new IllegalArgumentException("Provided EDI file is null");
        }
        this.file = file;
    }

    public static EDIFileAssertHelper forEDIFile(EDIFileWS file) {
        return new EDIFileAssertHelper(file);
    }

    public EDIFileAssertHelper withOrderOfRecord(Integer orderOfRecord) {
        this.orderOfRecord = orderOfRecord;
        return this;
    }

    public EDIFileAssertHelper withRecordName(String recordName) {
        this.recordName = recordName;
        return this;
    }

    public EDIFileAssertHelper withFieldS(Map<String, String> fields) {
        this.fields=fields;
        return this;
    }

    public void assertFileContent() {
        if(recordName==null) return;
        assertNotNull("File : "+file.getName() +" should have records", file.getEDIFileRecordWSes());
        assertTrue("File : "+file.getName() +" should have at least one record", file.getEDIFileRecordWSes().length>0);

        EDIFileRecordWS[] records = file.getEDIFileRecordWSes();
        Optional searchedRecord = Arrays.stream(records).filter(record -> record.getHeader().equals(recordName) && (orderOfRecord!=null?record.getRecordOrder()==orderOfRecord:true)).findFirst();

        assertTrue("File : "+file.getName() +" should have record : "+recordName+(orderOfRecord!=null?" with order of record : "+orderOfRecord:""), searchedRecord.isPresent());

        EDIFileRecordWS record = (EDIFileRecordWS)searchedRecord.get();

        if(fields.size()<1){
            return;
        }

        assertNotNull("File : "+file.getName() +" -> Record : "+record.getHeader() +" should have fields", record.getEdiFileFieldWSes());
        assertTrue("File : "+file.getName() +" -> Record : "+record.getHeader() +" should have at least one field", record.getEdiFileFieldWSes().length>0);

        fields.entrySet().stream().forEach(e -> {
            Optional searchedField = Arrays.stream(record.getEdiFileFieldWSes()).filter(field -> field.getKey().equals(e.getKey())).findFirst();
            assertTrue("File : " + file.getName() + " -> Record : " + record.getHeader() +". Field : "+ e.getKey() +"  not found", searchedField.isPresent());
            EDIFileFieldWS field = (EDIFileFieldWS)searchedField.get();
            assertEquals("File : " + file.getName() + " -> Record :  " + record.getHeader() +". Field : "+ e.getKey() +" value not matched", e.getValue(), field.getValue());
        });

    }
}
