package com.sapienter.jbilling.server.fileProcessing.fileGenerator;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.*;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.*;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by aman on 24/8/15.
 */
public class FlatFileGenerator implements IFileGenerator {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(FlatFileGenerator.class));
    public static final String fieldSeparator = "!";
    private FileFormat fileFormat;
    private EDIFileDTO ediFile;
    private List<Map<String, String>> input;
    private CompanyDTO company;
    private Integer ediFileId;
    private String fileName;
    private EDIFileBL fileBL;
    private static EDIFileStatusDTO PROCESSING;
    private static EDIFileStatusDTO PROCESSED;
    private static EDIFileStatusDTO ERROR_DETECTED;
    private int recordOrder = -1;

    public FlatFileGenerator(FileFormat fileFormat, int companyId, String fileName, Collection input) {
        this.fileFormat = fileFormat;
        this.company = new CompanyDAS().find(companyId);
        this.fileName = fileName;
        this.input = (List<Map<String, String>>)input;
        this.fileBL = new EDIFileBL();
        PROCESSING = new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_PROCESSING);
        PROCESSED = new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_PROCESSED);
        ERROR_DETECTED = new EDIFileStatusDAS().find(FileConstants.EDI_STATUS_ERROR_DETECTED);
    }

    public EDIFileDTO validateAndSaveInput() {

        validateData();
        //Create an object of EDI file
        EDIFileWS ediFileWS = fileBL.createEDIFileWS(fileName, fileFormat.getEdiTypeDTO(), company, TransactionType.OUTBOUND, PROCESSING);

        // Parse the data and convert it into WS objects
        List<EDIFileRecordWS> records = processInput();
//        records.stream().forEach((EDIFileRecordWS ediFileRecordWS) -> ediFileRecordWS.setEdiFile(ediFile));
        ediFileWS.setEDIFileRecordWSes(records.toArray(new EDIFileRecordWS[records.size()]));

        try {
            Integer ediFileId=new EDIFileBL().saveEDIFile(ediFileWS);
            // Save the data in to database as per file format
            ediFile=new EDIFileDAS().find(ediFileId);
            generateFile(records);
        } catch (Exception e) {
            LOG.error("Error occurred while generating EDI data to database");
            changeEDIFileDTO(ERROR_DETECTED);
            throw new SessionInternalError(e);
        }
        //Query to check if it have any error record.
        if (fileBL.checkErrorMessage(ediFileId)) {
            changeEDIFileDTO(ERROR_DETECTED);
        } else {
            changeEDIFileDTO(PROCESSED);
        }
        return ediFile;
    }

    public void validateData() {
        if (input == null || input.size() < 1) {
            throw new SessionInternalError("Data is invalid");
        }
    }

    private Map<Integer, String> getRecordIdFromInput() {
        Map<Integer, String> recordIds = new HashMap<Integer, String>();
        for (int i = 0; i < input.size(); i++) {
            String recId = input.get(i).get(FileConstants.TAG_NAME_REC_ID);
            if (recId == null) {
                throw new SessionInternalError("Data is invalid. No " + FileConstants.TAG_NAME_REC_ID + " found in input data");
            } else {
                recordIds.put(i, recId);
            }
        }
        return recordIds;
    }

    private void generateFile(List<EDIFileRecordWS> records) throws IOException {
        File folder = new File(FileConstants.getEDITypePath(fileFormat.getEdiTypeDTO().getEntity().getId(), fileFormat.getEdiTypeDTO().getPath(), FileConstants.OUTBOUND_PATH));
        LOG.debug("Folder get path  " + folder.getAbsolutePath());
        if(!folder.isDirectory()) {
            Files.createDirectories(folder.toPath());
        }
        List<String> fileContent = generateFileContent(records);
        File file = new File(folder.getAbsolutePath() + "/" + fileName);
        file.createNewFile();
        FileUtils.writeLines(file, fileContent);
    }

    private List<String> generateFileContent(List<EDIFileRecordWS> records) {
        List<String> fileContent = new ArrayList<String>();
        for (EDIFileRecordWS ediFileRecordWS : records) {
            StringBuffer recordLine = new StringBuffer("");
            recordLine.append(ediFileRecordWS.getHeader()).append(fieldSeparator);
            int prefixOrder = 0;
            for (EDIFileFieldWS ediFileFieldWS : ediFileRecordWS.getEdiFileFieldWSes()) {
                String seperatorrr = ediFileRecordWS.getTotalFileField()==ediFileFieldWS.getOrder()+1? "" :"!";
                int order = ediFileFieldWS.getOrder();
                if (order - prefixOrder > 1) {
                    recordLine.append(StringUtils.repeat(fieldSeparator, order - prefixOrder - 1));
                }
                if (ediFileFieldWS.getValue() != null) recordLine.append(ediFileFieldWS.getValue());
                recordLine.append(seperatorrr);
                prefixOrder = order;
            }
            recordLine.append(StringUtils.repeat(fieldSeparator, ediFileRecordWS.getTotalFileField() - 2 - prefixOrder));
            recordLine.append("\r"); // Add line separator for windows. It should work fine with all Operating System.
            fileContent.add(recordLine.toString());
        }
        return fileContent;
    }

    public void changeEDIFileDTO(EDIFileStatusDTO statusDTO) {
        ediFile = new EDIFileDAS().find(ediFile.getId());
        ediFile.setFileStatus(statusDTO);
        EDIFileBL fileBL = new EDIFileBL();
        fileBL.saveEDIFile(ediFile);
    }

    public List<EDIFileRecordWS> processInput() {
        List<EDIFileRecordWS> records = new LinkedList<EDIFileRecordWS>();
        FileStructure fileStructure = fileFormat.getFileStructure();
        LinkedList<RecordStructure> recordStructures = new LinkedList<RecordStructure>(fileStructure.getRecordStructures());
        Map<Integer, String> recordsWithIndex = getRecordIdFromInput();

        while (recordStructures.size() > 0) {
            processSingleRecord(records, recordStructures, recordsWithIndex);
        }
        return records;
    }

    private void processSingleRecord(List<EDIFileRecordWS> records, LinkedList<RecordStructure> recordStructures,
                                      Map<Integer, String> recordsWithIndex){

        RecordStructure recordStructure = recordStructures.pop();
        Record record = recordStructure.getRecord();
        Field recId = record.getRecId();

        // Check if this record has visibility for outbound file
        if (recId.getOutbound().compareTo(Visibility.X) == 0 || ((recordsWithIndex.size()<=recordOrder+1 || recordsWithIndex.get(recordOrder+1) == null) && (recId.getOutbound().compareTo(Visibility.O) == 0))) {
            return;
        }

        //Find out how many records should generate of that particular type.
        String nodeName = recId.getDefaultValue();
        String noOfRecords = recordStructure.getLoop();
        int recordCount = 0;
        if (noOfRecords.equals("n")) {
            recordCount = countValue(recordsWithIndex, nodeName);
        } else {
            recordCount = Integer.parseInt(noOfRecords);
        }

        // Can have multiple records
        for (int i = 0; i < recordCount; i++) {
            // Check if record is optional and more than one records exist for record in structure
            if((recordsWithIndex.size()<=recordOrder+1 || recordsWithIndex.get(recordOrder+1) == null) && (recId.getOutbound().compareTo(Visibility.O) == 0)){
                return;
            }
            recordOrder++;
            if ((recId.getOutbound().compareTo(Visibility.M) == 0) && (recordsWithIndex.get(recordOrder) == null || !recordsWithIndex.get(recordOrder).equals(nodeName))) {
                throw new SessionInternalError("Data is invalid. Input does not match the provided format");
            }
            //Generate EDIFileRecordWS object.
            EDIFileRecordWS ediFileRecordWS = generateEDIRecord(record, input.get(recordOrder));
            ediFileRecordWS.setHeader(nodeName);
            ediFileRecordWS.setRecordOrder(recordOrder);
            ediFileRecordWS.setEntityId(company.getId());
            ediFileRecordWS.setCreateDatetime(TimezoneHelper.serverCurrentDate());
            if (record.getFields() != null) {
                ediFileRecordWS.setTotalFileField(record.getFields().size() + 1);
            }
            records.add(ediFileRecordWS);

            List<RecordStructure> childRecordStructureList;
            if (recordStructure.getChildRecord() != null && recordStructure.getChildRecord().size() > 0) {
                childRecordStructureList = recordStructure.getChildRecord();
                for (int j = 0; j < childRecordStructureList.size(); j++) {
                    recordStructures.push(childRecordStructureList.get(j));
                    processSingleRecord(records, recordStructures, recordsWithIndex);
                }
            }
        }
    }

    public EDIFileRecordWS generateEDIRecord(Record record, Map<String, String> recordInput) {
        EDIFileRecordWS ediFileRecordWS = new EDIFileRecordWS();
        List<EDIFileFieldWS> ediFileFieldWSList = new LinkedList<EDIFileFieldWS>();
        // Bind Fields
        int fieldOrder = 0;
        for (Field field : record.getFields()) {
            fieldOrder++;
            // Check if this field has visibility for outbound file
            if (field.getOutbound().compareTo(Visibility.X) == 0) {
                continue;
            }

            String comment = null;
            String value = recordInput.get(field.getFieldName());

            //Check if Field has default value
            if ((field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) && (value == null || value.trim().isEmpty())) {
                value = field.getDefaultValue();
            }

            if (value == null || value.trim().isEmpty()) {
                if (field.getOutbound().compareTo(Visibility.M) == 0) {
                    comment = FileConstants.ERROR_MSG_FIELD_MANDATORY;
                }
            } else {
                //Check if field has value options
                Values values = field.getPossibleValues();
                if (values != null && !values.isValueExistsInOptions(value)) {
                    comment = FileConstants.ERROR_MSG_FIELD_OPTION_NOT_EXIST;
                }
                int maxSize = field.getMaxSize();

                if (maxSize > 0) {
                    if (maxSize < value.length()) {
                        comment = FileConstants.ERROR_MSG_FIELD_VALUE_MAX_SIZE_EXIST;
                    }
                }
            }

            ediFileFieldWSList.add(new EDIFileFieldWS(field.getFieldName(), value, comment, fieldOrder));
        }
        EDIFileFieldWS[] ediFileFieldWSArray = new EDIFileFieldWS[ediFileFieldWSList.size()];
        ediFileRecordWS.setEdiFileFieldWSes(ediFileFieldWSList.toArray(ediFileFieldWSArray));
        return ediFileRecordWS;
    }

    private static <T, E> int countValue(Map<T, E> data, String value) {
        int count = 0;
        for (Map.Entry<T, E> entry : data.entrySet()) {
            if (value.equals(entry.getValue())) {
                count++;
            }
        }
        return count;
    }


}
