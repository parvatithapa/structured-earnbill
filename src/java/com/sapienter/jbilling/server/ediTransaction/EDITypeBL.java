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
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDITypeDTO;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.fileProcessing.xmlParser.FileFormat;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Emil
 */
public class EDITypeBL {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EDITypeBL.class));
    private EDITypeDAS ediTypeDAS = null;
    public EDITypeBL(){
        init();
    }
    private  void init(){
        ediTypeDAS=new EDITypeDAS();
    }

    public EDITypeWS getWS(EDITypeDTO dto) {

        EDITypeWS ws = new EDITypeWS();
        ws.setId(dto.getId());
        ws.setCreateDatetime(dto.getCreateDatetime());
        ws.setEntityId(dto.getEntity().getId());
        if(dto.getId() > 0) {
            dto = new EDITypeDAS().findNow(dto.getId());
        }
        for(CompanyDTO companyDTO:dto.getEntities()){
            ws.getEntities().add(companyDTO.getId());
        }

        EDIFileStatusBL ediFileStatusBL=new EDIFileStatusBL();
        //this map used for binding the same status object as a child status object if any status have a child status.
        Map<String,EDIFileStatusWS> ediFileStatusWSMap = new HashMap<String,EDIFileStatusWS>();
        for(EDIFileStatusDTO ediFileStatusDTO:dto.getStatuses()){
            ws.getEdiStatuses().add(ediFileStatusBL.getWS(ediFileStatusDTO,ediFileStatusWSMap));
        }
        ws.setGlobal(dto.getGlobal());
        ws.setName(dto.getName());
        ws.setPath(dto.getPath());
        ws.setEdiSuffix(dto.getEdiSuffix());
        return ws;
    }

    public EDITypeDTO getDTO(EDITypeWS ws) throws SessionInternalError{
        EDITypeDTO dto = new EDITypeDTO();
        if(ws.getId() != null && ws.getId()>0){
            dto.setId(ws.getId());
            dto.setVersionNum(ediTypeDAS.find(ws.getId()).getVersionNum());
        }
        CompanyDAS companyDAS=new CompanyDAS();
        for(Integer entity:ws.getEntities()){
            dto.getEntities().add(companyDAS.find(entity));
        }
        EDIFileStatusBL ediFileStatusBL=new EDIFileStatusBL();
        //this map used for binding the same status object as a child status object if any status have a child status.
        Map<String,EDIFileStatusDTO> ediFileStatusDTOMap = new HashMap<String,EDIFileStatusDTO>();
        for(EDIFileStatusWS status:ws.getEdiStatuses()){
            EDIFileStatusDTO ediFileStatusDTO=ediFileStatusBL.getDTO(status,ediFileStatusDTOMap);
            dto.getStatuses().add(ediFileStatusDTO);
        }

        dto.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        dto.setName(ws.getName());
        dto.setPath(ws.getPath());
        dto.setGlobal(ws.getGlobal());
        dto.setEntity(companyDAS.find(ws.getEntityId()));
        dto.setEdiSuffix(ws.getEdiSuffix());
        return dto;
    }

    public Integer createEDIType(EDITypeWS ediTypeWS, File ediFormatFile) {
        EDITypeDTO ediTypeDTO = new EDITypeBL().getDTO(ediTypeWS);
//        Validate edi type first.
        validateEDIType(ediTypeWS, ediFormatFile);

//        Save first edi type. Use its id for creating directories.

        if (ediTypeWS.getId() == null || ediTypeWS.getId() == 0) {
            String uuid = UUID.randomUUID().toString();
//            Directory would follow this sequence. entityId/editypeId/inbound and entityId/editypeId/outbound
            File inboundDirectory = new File(FileConstants.getEDITypePath(ediTypeDTO.getEntity().getId(), uuid, FileConstants.INBOUND_PATH));

            if (!inboundDirectory.exists()) {
                inboundDirectory.mkdirs();
            }
            File outboundDirectory = new File(FileConstants.getEDITypePath(ediTypeDTO.getEntity().getId(), uuid, FileConstants.OUTBOUND_PATH));
            if (!outboundDirectory.exists()) {
                outboundDirectory.mkdirs();
            }

            ediTypeDTO.setPath(uuid);

        }

        if (ediFormatFile != null) {
            if (ediTypeWS.getId() != null && ediTypeWS.getId() != 0) {
                FileFormat.removeObject(ediTypeWS.getId());
            }
            String ediFormatFileName = getFormatFileName(ediTypeDTO.getPath(), ediTypeDTO.getEntity().getId());
            File existingFormatFile = new File(FileConstants.getFormatFilePath() + File.separator + ediFormatFileName);
            try {
                if (ediTypeWS.getId() == null || ediTypeWS.getId() == 0) {
                    FileUtils.copyFile(ediFormatFile, existingFormatFile);
                } else {
                    if (existingFormatFile.exists()) {
                        overwriteFile(ediFormatFile, existingFormatFile);
                    } else {
                        FileUtils.copyFile(ediFormatFile, existingFormatFile);
                    }
                }

            } catch (IOException ioex) {
                LOG.debug("Unable to save file properly. Please upload another file. " + ioex);
                ioex.printStackTrace();
                throw new SessionInternalError("Unable to save file properly. Please upload another file.");
            }
        }
        ediTypeDTO = ediTypeDAS.save(ediTypeDTO);
        return ediTypeDTO.getId();
    }

    private String getFormatFileName(String path, Integer companyId ){
        return path + FileConstants.HYPHEN_SEPARATOR + companyId + ".xml";
    }

    private void overwriteFile(File fromFile, File toFile) throws IOException {

        try( FileInputStream fis = new FileInputStream(fromFile);
            FileOutputStream fos = new FileOutputStream(toFile)
        ) {

            byte[] buffer = new byte[8000];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    private void validateEDIType(EDITypeWS ediTypeWS, File ediFormatFile) {
//        Validate edi type here.
        LOG.debug("Edi file in validate edi type");

        if ((ediTypeWS.getId() == null || ediTypeWS.getId() == 0) && ediTypeDAS.isEDITypeAlreadyExist(ediTypeWS.getEntityId(), ediTypeWS.getName())) {
            throw new SessionInternalError("Edi type with name " + ediTypeWS.getName() + " already exist.",
                    new String[]{"edit.type.already.exist," + ediTypeWS.getName()});
        }

        if (ediFormatFile != null && !FilenameUtils.getExtension(ediFormatFile.getName()).equals("xml")) {
//            Need to upload file with correct extension.
            throw new SessionInternalError("xml.error.found", new String[]{"xml.error.found"});
        }
    }

    public void deleteEDIType(Integer ediTypeId, Integer companyId) {
        // Delete edi all files of this type
        new EDIFileDAS().deleteAllFilesByEDIType(ediTypeId, companyId);

        EDITypeDTO ediTypeDTO = ediTypeDAS.findNow(ediTypeId);
        if (ediTypeDTO != null) {
            ediTypeDAS.delete(ediTypeDTO);
        } else {
            throw new SessionInternalError(
                    "EDIType not found",
                    new String[]{"EDIType,editype,not.found.editype.error"});
        }

        String directoryName = FileConstants.getEDITypePath(ediTypeDTO.getEntity().getId(), ediTypeDTO.getPath());
        File directory = new File(directoryName);
        try{
            if(directory.isDirectory() && directory.exists()) FileUtils.deleteDirectory(directory);
        }catch (IOException e){
            LOG.error("Exception occurred while deleting edi type file : " + e.getMessage());
            e.printStackTrace();
        }

        String ediFormatFileName = getFormatFileName(ediTypeDTO.getPath(), ediTypeDTO.getEntity().getId());
        File formatFile = new File(ediFormatFileName);
        if (formatFile.exists() && formatFile.isFile()) {
            if (!formatFile.delete()) {
                LOG.error("Exception occurred while deleting edi type file : " + formatFile.getPath());
            }
        }
    }
}
