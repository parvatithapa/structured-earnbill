package com.sapienter.jbilling.server.fileProcessing.fileGenerator;

import com.sapienter.jbilling.server.ediTransaction.EDIFileRecordWS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.Record;

import java.util.List;
import java.util.Map;

/**
 * Created by aman on 25/8/15.
 */
public interface IFileGenerator {
    public EDIFileDTO validateAndSaveInput();
    public List<EDIFileRecordWS> processInput();
    public EDIFileRecordWS generateEDIRecord(Record record, Map<String, String> recordInput);
}
