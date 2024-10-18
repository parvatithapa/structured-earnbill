package com.sapienter.jbilling.server.fileProcessing.fileParser;

import com.sapienter.jbilling.server.ediTransaction.EDIFileRecordWS;
import com.sapienter.jbilling.server.ediTransaction.EDIFileWS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;

import java.io.File;
import java.util.List;

/**
 * Created by aman on 25/8/15.
 */
public interface IFileParser {
    public List<EDIFileRecordWS> validateAndParseFile(List<String> data);
    public EDIFileDTO parseAndSaveFile();
}
