package com.sapienter.jbilling.server.fileProcessing.fileParser;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDAS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.*;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Created by aman on 25/8/15.
 */
public class FlatFileParser implements IFileParser {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(FlatFileParser.class));

    private Integer recordOrder = 1;
    private FileFormat fileFormat;
    private File file;
    private CompanyDTO company;
    private List<String> recordData;

    private static EDIFileStatusDTO PROCESSING;
    private static EDIFileStatusDTO PROCESSED;
    private static EDIFileStatusDTO ERROR_DETECTED;

    public FlatFileParser(FileFormat fileFormat, File file, Integer entityId) {
        this.fileFormat = fileFormat;
        this.file = file;
        if(entityId != null) {
            company = new CompanyDAS().find(entityId);
        }

        PROCESSING = new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_PROCESSING);
        PROCESSED = new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_PROCESSED);
        ERROR_DETECTED = new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_ERROR_DETECTED);

    }

    public FlatFileParser() {}


    public List<EDIFileRecordWS> validateAndParseFile(List<String> data) {

        List<String> flatFileRecords = data;
        recordData = data;
        List<RecordStructure> recordStructures = fileFormat.getFileStructure().getRecordStructures();
        Iterator<String> flatFileRecordIterator = flatFileRecords.iterator();
        List<EDIFileRecordWS> ediFileRecordWSList = new ArrayList<EDIFileRecordWS>();
        for (RecordStructure recordStructure : recordStructures) {
            parseRecords(recordStructure, flatFileRecordIterator, ediFileRecordWSList, null);
        }

        return ediFileRecordWSList;

    }

    private boolean parseRecords(RecordStructure recordStructure, Iterator<String> flatFileRecordIterator, List<EDIFileRecordWS> ediFileRecordWSList, String currentLine) {
        Record record = recordStructure.getRecord();
        Field recId = record.getRecId();
        String noOfRecords = recordStructure.getLoop();
        Integer loop = 0;
        if (noOfRecords.equals("n")) {
            loop = countRecord(recId);
        } else {
            loop = Integer.parseInt(noOfRecords);
        }
        String recordLine = currentLine;
        boolean incomingLineParsed = true;

        for (int i = 0; i < loop; i++) {
            if (!recId.isNotUsed()) {
                String defaultValue = recId.getDefaultValue();
                if(recordLine == null || i > 0) {
                    recordLine = flatFileRecordIterator.hasNext() ? flatFileRecordIterator.next() : null;
                }
                String recordKey = recordLine != null ? recordLine.split("!")[0] : null;
                if(recId.getInbound().equals(Visibility.X)) {
                    incomingLineParsed = false;
                    break;
                }
                if (recordKey != null && defaultValue.equals(recordKey)) {
                    ediFileRecordWSList.add(parseFlatFileRecord(recordLine, record));
                    if (recordStructure.getChildRecord() != null && recordStructure.getChildRecord().size() > 0) {
                        boolean lineParsed = true;
                        for (RecordStructure childRecordStructure : recordStructure.getChildRecord()) {
                            if(lineParsed) {
                                recordLine = flatFileRecordIterator.hasNext() ? flatFileRecordIterator.next() : null;
                            }
                            lineParsed = parseRecords(childRecordStructure, flatFileRecordIterator, ediFileRecordWSList, recordLine);
                        }
                    }
                } else if (recId.getInbound().equals(Visibility.M)){
                    throw new SessionInternalError("Record " + defaultValue + " is mandatory but not present in file.");
                } else if (recId.getInbound().equals(Visibility.O)){
                    incomingLineParsed = false;
                    break;
                }
            }
        }

        return incomingLineParsed;
    }

    private List<String> getFlatFileLines(File file) {
        List<String> flatFiles = new LinkedList<String>();
        try ( InputStream in = new FileInputStream(file);
              BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (StringUtils.trimToNull(line) != null) {
                    flatFiles.add(line);
                }
            }

        } catch (IOException ioex) {
            LOG.debug("Caught exception while converting file into input stream " + ioex);
            throw new SessionInternalError(ioex);
        }
        recordData = flatFiles;
        return flatFiles;
    }

    private EDIFileRecordWS parseFlatFileRecord(String recordLine, Record record) {
        EDIFileRecordWS ediFileRecordWS = new EDIFileRecordWS();
        List<EDIFileFieldWS> ediFileFieldWSes = new ArrayList<EDIFileFieldWS>();

        List<String> flatFileRecords = new ArrayList<String>(Arrays.asList(recordLine.split("!", -1)));
        if (flatFileRecords.size() > 0) {
            flatFileRecords.remove(0);    // Remove first value. it's a recId.
        }
//        Integer index = 0;
        List<Field> recordFields = record.getFields();
        if (recordFields.size() != flatFileRecords.size()) {
            throw new SessionInternalError("Invalid Data. Input does not match the provided format so response file not send to LDC");
        }
        int flatFileRecordSize = flatFileRecords.size();
        String comment = null;
        String value = null;
        for (int index = 0; index < recordFields.size(); index++) {
            comment = "";
            Field field = recordFields.get(index);
            if (field.isNotUsed()) {
                continue;
            }

            if (index < flatFileRecordSize) {
                value = StringUtils.trimToNull(flatFileRecords.get(index));
            } else {
                value = null;
                comment = "Field exist in record but not in flat file.";
            }
            if (field.getInbound().equals(Visibility.M) && value == null) {
                comment = "field " + field.getFieldName() + " is mandatory but it does not have value.";
            }
            if(value != null && field.getMaxSize() > 0 && field.getMaxSize() < value.length()) {
                comment = "Field " + field.getFieldName() +  " from Record " + record.getRecId().getDefaultValue() +  " mus not exceed max size.";
            }

            EDIFileFieldWS ediFileRecordDataWS = new EDIFileFieldWS(field.getFieldName(), value, comment, index + 1);

            ediFileFieldWSes.add(ediFileRecordDataWS);
        }
        ediFileRecordWS.setEdiFileFieldWSes(ediFileFieldWSes.toArray(new EDIFileFieldWS[ediFileFieldWSes.size()]));
        ediFileRecordWS.setRecordOrder(recordOrder++);
        ediFileRecordWS.setHeader(record.getRecId().getDefaultValue());
        ediFileRecordWS.setEntityId(company.getId());
        if(record.getFields() != null) {
            ediFileRecordWS.setTotalFileField(record.getFields().size() + 1);
        }

        return ediFileRecordWS;
    }

    public EDIFileDTO parseAndSaveFile() {
        EDIFileBL fileBL = new EDIFileBL();
        EDIFileWS ediFile = fileBL.createEDIFileWS(file.getName(), fileFormat.getEdiTypeDTO(), company, TransactionType.INBOUND, PROCESSED);
        try {
            List data = getFlatFileLines(file);
            List<EDIFileRecordWS> records = validateAndParseFile(data);
            ediFile.setEDIFileRecordWSes(records.toArray(new EDIFileRecordWS[records.size()]));
            LOG.debug("Records is:  %s", records);
        } catch (Exception e) {
            LOG.error("Error occurred while saving EDI data to database.. " + e.getMessage(), e);
//            If there is some error in parsing file, return edi file DTO without any record and with error detected status.
            ediFile.setComment(e.getMessage());
            ediFile.setEdiFileStatusWS(new EDIFileStatusBL().getWS(ERROR_DETECTED));
            e.printStackTrace();
        }
        Integer ediFileId=fileBL.saveEDIFile(ediFile);
        return new EDIFileDAS().findNow(ediFileId);
    }

    private int countRecord(Field redId) {
        int count = 0;
        for(String record: recordData) {
            if(record.split("!")[0].equals(redId.getDefaultValue())) {
                count++;
            }
        }
        return count;
    }
}
