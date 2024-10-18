/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.db.*;
import com.sapienter.jbilling.server.ediTransaction.task.NewEDIFileEvent;
import com.sapienter.jbilling.server.ediTransaction.task.UpdateEDIFileEvent;
import com.sapienter.jbilling.server.ediTransaction.task.UpdateEDIFileStatusEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Emil
 */
public class EDIFileBL {
    private EDIFileDTO dto = null;
    private EDIFileDAS ediFileDAS = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EDIFileBL.class));

    public EDIFileBL() {
        init();
    }

    public EDIFileBL(Integer ediFileId) {
        init();
        dto = ediFileDAS.find(ediFileId);
    }

    public EDIFileBL(EDIFileDTO fileDTO) {
        this.dto = fileDTO;
    }

    public EDIFileDTO getEntity() {
        return dto;
    }

    private void init() {
        ediFileDAS = new EDIFileDAS();
    }

    public EDIFileWS getWS() {
        EDIFileWS ws = new EDIFileWS();
        ws.setId(dto.getId());
        ws.setCreateDatetime(dto.getCreateDatetime());
        ws.setName(dto.getName());
        ws.setType(dto.getType());
        ws.setEdiTypeWS(new EDITypeBL().getWS(dto.getEdiType()));
        ws.setEntityId(dto.getEntity().getId());
        if(dto.getUser()!=null){
            ws.setUserId(dto.getUser().getId());
        }
        if(dto.getUtilityAccountNumber()!=null){
            ws.setUtilityAccountNumber(dto.getUtilityAccountNumber());
        }
        if(dto.getStartDate()!=null){
            ws.setStartDate(dto.getStartDate());
        }
        if(dto.getEndDate()!=null){
            ws.setEndDate(dto.getEndDate());
        }
        ws.setVersionNum(dto.getVersionNum());
        EDIFileStatusDTO statusDTO = dto.getFileStatus();
        if(dto.getFileStatus().getId() > 0) {
            statusDTO = new EDIFileStatusDAS().find(dto.getFileStatus().getId());
        }
        ws.setEdiFileStatusWS(new EDIFileStatusBL().getWS(statusDTO));
        List<EDIFileRecordWS> recordWSList = new ArrayList<EDIFileRecordWS>();
        for(EDIFileRecordDTO recordDTO: dto.getEdiFileRecords()) {
            recordWSList.add(getEDIFileRecordWS(recordDTO));
        }
        ws.setEDIFileRecordWSes(recordWSList.toArray(new EDIFileRecordWS[recordWSList.size()]));
        ws.setComment(dto.getComment());
        if(dto.getExceptionCode()!=null)ws.setExceptionCode(dto.getExceptionCode().getExceptionCode());
        return ws;
    }

    public EDIFileDTO getDTO(EDIFileWS ws) throws SessionInternalError {

        EDIFileDTO dto;
        if (ws.getId() != null && ws.getId() > 0) {
            dto = ediFileDAS.find(ws.getId());
            dto.setEdiFileRecords(new LinkedList<EDIFileRecordDTO>());
        }else{
            dto = new EDIFileDTO();
        }
//        dto.setVersionNum(ws.getVersionNum());
        dto.setEntity(new CompanyDAS().findNow(ws.getEntityId()));
        dto.setName(ws.getName());
        dto.setCreateDatetime(ws.getCreateDatetime());
        dto.setEdiType(new EDITypeDAS().findNow(ws.getEdiTypeWS().getId()));
        dto.setType(ws.getType());
        if(ws.getUserId()!=null){
            dto.setUser(new UserDAS().find(ws.getUserId()));
        }
        if(ws.getUtilityAccountNumber()!=null){
            dto.setUtilityAccountNumber(ws.getUtilityAccountNumber());
        }
        if(ws.getStartDate()!=null){
            dto.setStartDate(ws.getStartDate());
        }
        if(ws.getEndDate()!=null){
            dto.setEndDate(ws.getEndDate());
        }
        EDIFileStatusDTO ediFileStatusDTO = null;
        if(ws.getEdiFileStatusWS() != null && ws.getEdiFileStatusWS().getId()>0){
            ediFileStatusDTO = new EDIFileStatusDAS().find(ws.getEdiFileStatusWS().getId());
        }else {
            throw new SessionInternalError("Status not found for "+dto.getName()+" file");
        }
        dto.setFileStatus(ediFileStatusDTO);

        if(ws.getExceptionCode()!=null){
            for(EDIFileExceptionCodeDTO ediFileExceptionCodeDTO:ediFileStatusDTO.getExceptionCodes()){
                if(ediFileExceptionCodeDTO.getExceptionCode().equals(ws.getExceptionCode())){
                    dto.setExceptionCode(ediFileExceptionCodeDTO);
                    break;
                }
            }
        }else{
            dto.setExceptionCode(null);
        }

        if(ws.getEDIFileRecordWSes()!=null ){
            for(EDIFileRecordWS ediFileRecordWS:ws.getEDIFileRecordWSes()){
                EDIFileRecordDTO recordDTO=getEDIFileRecordDTO(ediFileRecordWS);
                recordDTO.setEdiFile(dto);
                dto.setEDIFileRecord(recordDTO);
            }
        }

        dto.setComment(ws.getComment());
        return dto;
    }

    public EDIFileRecordWS getEDIFileRecordWS(EDIFileRecordDTO ediFileRecordDTO) {
        EDIFileRecordWS ediFileRecordWS = new EDIFileRecordWS();
        ediFileRecordWS.setEntityId(ediFileRecordDTO.getId());
        ediFileRecordWS.setId(ediFileRecordDTO.getId());
        ediFileRecordWS.setCreateDatetime(ediFileRecordDTO.getCreationTime());
        if (ediFileRecordDTO.getEdiFile() != null) {
            ediFileRecordWS.setEdiFileId(ediFileRecordDTO.getEdiFile().getId());
        }
        if (ediFileRecordDTO.getFileFields().size() > 0) {
            List<EDIFileFieldWS> ediFileFieldsIds = new ArrayList<EDIFileFieldWS>();
            for (EDIFileFieldDTO ediFileFieldDTO : ediFileRecordDTO.getFileFields()) {
                ediFileFieldsIds.add(getEDIFieldWS(ediFileFieldDTO));
            }
            ediFileRecordWS.setEdiFileFieldWSes(ediFileFieldsIds.toArray(new EDIFileFieldWS[ediFileFieldsIds.size()]));
        }
        ediFileRecordWS.setHeader(ediFileRecordDTO.getEdiFileRecordHeader());
        ediFileRecordWS.setComment(ediFileRecordDTO.getComment());
        ediFileRecordWS.setRecordOrder(ediFileRecordDTO.getRecordOrder());
        ediFileRecordWS.setTotalFileField(ediFileRecordDTO.getTotalFileField());
        return ediFileRecordWS;
    }


    public EDIFileRecordDTO getEDIFileRecordDTO(EDIFileRecordWS ediFileRecordWS) {
        EDIFileRecordDTO ediFileRecordDTO = new EDIFileRecordDTO();
        if (ediFileRecordWS.getId() != null && ediFileRecordWS.getId() > 0) {
            ediFileRecordDTO.setId(ediFileRecordWS.getId());
            ediFileRecordDTO.setVersionNum(new EDIFileRecordDAS().find(ediFileRecordWS.getId()).getVersionNum());
        }
        ediFileRecordDTO.setRecordOrder(ediFileRecordWS.getRecordOrder());
        ediFileRecordDTO.setEdiFileRecordHeader(ediFileRecordWS.getHeader());
        ediFileRecordDTO.setCreationTime(TimezoneHelper.serverCurrentDate());
        ediFileRecordDTO.setTotalFileField(ediFileRecordWS.getTotalFileField());
        if(ediFileRecordWS.getComment()!=null){
            ediFileRecordDTO.setComment(ediFileRecordWS.getComment());
        }


        if (ediFileRecordWS.getEdiFileFieldWSes().length > 0) {
            for (EDIFileFieldWS ediFileFieldWS : ediFileRecordWS.getEdiFileFieldWSes()) {
                EDIFileFieldDTO ediFileFieldDTO = getEDIFileFieldDTO(ediFileFieldWS);
                ediFileRecordDTO.setFileField(ediFileFieldDTO);
                ediFileFieldDTO.setEdiFileRecord(ediFileRecordDTO);
            }
        }
        return ediFileRecordDTO;
    }

    public EDIFileFieldWS getEDIFieldWS(EDIFileFieldDTO ediFileFieldDTO) {
        EDIFileFieldWS ediFileFieldWS = new EDIFileFieldWS();
        ediFileFieldWS.setId(ediFileFieldDTO.getId());
        ediFileFieldWS.setComment(ediFileFieldDTO.getComment());
        ediFileFieldWS.setKey(ediFileFieldDTO.getEdiFileFieldKey());
        ediFileFieldWS.setValue(ediFileFieldDTO.getEdiFileFieldValue());
        ediFileFieldWS.setOrder(ediFileFieldDTO.getEdiFileFieldOrder());
        return ediFileFieldWS;
    }

    public EDIFileFieldDTO getEDIFileFieldDTO(EDIFileFieldWS ediFileFieldWS) {
        EDIFileFieldDTO ediFileFieldDTO = new EDIFileFieldDTO();
        if (ediFileFieldWS.getId() != null && ediFileFieldWS.getId() > 0) {
            ediFileFieldDTO.setId(ediFileFieldWS.getId());
        }
        ediFileFieldDTO.setEdiFileFieldValue(ediFileFieldWS.getValue());
        ediFileFieldDTO.setEdiFileFieldKey(ediFileFieldWS.getKey());
        ediFileFieldDTO.setComment(ediFileFieldWS.getComment());
        ediFileFieldDTO.setEdiFileFieldOrder(ediFileFieldWS.getOrder());
        return ediFileFieldDTO;
    }

    public int saveEDIFile(EDIFileWS ediFileWS) {
        EDIFileDTO fileDTO = getDTO(ediFileWS);
        Integer ediFileId= saveEDIFile(fileDTO);
        fileDTO=new EDIFileDAS().find(ediFileId);
        if(ediFileWS.getId()==null || ediFileWS.getId()==0){
            NewEDIFileEvent event=new NewEDIFileEvent(fileDTO.getEntity().getId(), fileDTO);
            EventManager.process(event);
        }
        return ediFileId;
    }


    public Integer saveEDIFile(EDIFileDTO ediFileDTO) throws SessionInternalError {
        return ediFileDAS.save(ediFileDTO).getId();
    }

    public EDIFileWS createEDIFileWS(String fileName, EDITypeDTO ediTypeDTO, CompanyDTO company, TransactionType transactionType, EDIFileStatusDTO fileStatusDTO) {
        EDIFileWS ediFileWS = new EDIFileWS();
        ediFileWS.setName(fileName);
        ediFileWS.setEdiTypeWS(new EDITypeBL().getWS(ediTypeDTO));
        ediFileWS.setEntityId(company.getId());
        ediFileWS.setType(transactionType);
        ediFileWS.setEdiFileStatusWS(new EDIFileStatusBL().getWS(fileStatusDTO));
        ediFileWS.setCreateDatetime(TimezoneHelper.serverCurrentDate());

        return ediFileWS;
    }

    public EDIFileDTO saveEDIRecords(List<EDIFileRecordWS> records, EDIFileDTO ediFile) {
        if (records != null && records.size() > 0) {
            for (EDIFileRecordWS ediFileRecordWS : records) {
                EDIFileRecordDTO ediFileRecordDTO = getEDIFileRecordDTO(ediFileRecordWS);
                ediFileRecordDTO.setEdiFile(ediFile);
                ediFile.setEDIFileRecord(ediFileRecordDTO);
            }
        }
        ediFile=ediFileDAS.find(saveEDIFile(ediFile));
        return ediFile;
    }

    public boolean checkErrorMessage(Integer ediFileId){
        return ediFileDAS.isErrorFieldExist(ediFileId)==1?true:false;
    }



    public void updateEDIFileStatus(Integer fileId, String statusName, String comment){
        EDIFileStatusDTO ediFileStatusDTO = null;
        EDIFileDAS das=new EDIFileDAS();
        EDIFileDTO ediFileDTO = das.find(fileId);
        if (ediFileDTO != null) {
            LOG.debug("In EnrollmentResponseParserTask before } edit status  " + statusName);
            EDITypeDTO ediTypeDTO = new EDITypeDAS().find(ediFileDTO.getEdiType().getId());
            LOG.debug("In EnrollmentResponseParserTask after fetching edit status");
            for (EDIFileStatusDTO fileStatusDTO : ediTypeDTO.getStatuses()) {
                if (fileStatusDTO.getName().equals(statusName)) {
                    ediFileStatusDTO = fileStatusDTO;
                    ediFileDTO.setFileStatus(ediFileStatusDTO);
                    ediFileDTO.setComment(comment);
                    das.save(ediFileDTO);
                    return;
                }
            }
            throw new SessionInternalError("EDI Status of name "+statusName+" did not found ");
        }else{
            throw new SessionInternalError("EDI File not found for id "+fileId);
        }
    }

    public static String getEDIFileTransactionId(EDIFileDTO ediFile){
        for(EDIFileRecordDTO ediFileRecord: ediFile.getEdiFileRecords()){
            if(ediFileRecord.getEdiFileRecordHeader().equals("HDR")){
                for(EDIFileFieldDTO ediField : ediFileRecord.getFileFields()){
                    if(ediField.getEdiFileFieldKey().equals("TRANS_REF_NR")){
                        return ediField.getEdiFileFieldValue();
                    }
                }
            }
        }
        return null;
    }

    // Note:

    /**
     * This method is uesd to reprocess an existing EDI files.
     * If escapeValidation is true then it will bypass the non mandatory validation for processing the edi file
     * else if its value is false then all validation should be performed .
     *
     * @param ediFileWS file which want to reprocess
     * @param statusWS
     * @param escapeValidation
     * @return edi file id
     */
    public int updateStatus(EDIFileWS ediFileWS, EDIFileStatusWS statusWS,Boolean escapeValidation) {
        EDIFileDTO fileDTO = ediFileDAS.find(ediFileWS.getId());
        EDIFileStatusDTO ediFileStatusDTO=new EDIFileStatusDAS().find(statusWS.getId());
        fileDTO.setFileStatus(ediFileStatusDTO);
        EventManager.process(new UpdateEDIFileEvent(fileDTO.getEntity().getId(), fileDTO));
        UpdateEDIFileStatusEvent event=new UpdateEDIFileStatusEvent(fileDTO.getEntity().getId(), fileDTO, escapeValidation?ediFileWS.getEdiFileStatusWS().getName() : null);
        EventManager.process(event);
        return saveEDIFile(fileDTO);
    }
}
