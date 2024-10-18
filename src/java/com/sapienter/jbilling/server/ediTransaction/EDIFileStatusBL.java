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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileExceptionCodeDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileExceptionCodeDTO;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Emil
 */
public class EDIFileStatusBL {

    private EDIFileStatusDAS ediFileStatusDAS = null;

    public EDIFileStatusBL(){
        init();
    }


    private  void init(){

        ediFileStatusDAS =new EDIFileStatusDAS();

    }

    /*
    * This method used for only EDIFile level.
    * Because EDITFile have a single status at a time.
    * */
    public EDIFileStatusWS getWS(EDIFileStatusDTO dto){

            EDIFileStatusWS ws = new EDIFileStatusWS();
            if(dto != null && dto.getId() > 0) {
                ws.setId(dto.getId());
            }

            ws.setCreateDatetime(dto.getCreateDatetime());
            ws.setName(dto.getName());
            ws.setError(dto.isError());
            if(dto.getAssociatedEDIStatuses().size()>0){
                for(EDIFileStatusDTO childStatusDto:dto.getAssociatedEDIStatuses()){
                    ws.getChildStatuesIds().add(childStatusDto.getId());
                }
            }
            if(dto.getAssociatedEDIStatuses().size()>0){
                for(EDIFileStatusDTO childStatusDto:dto.getAssociatedEDIStatuses()){
                    //recursive call for child's
                    ws.getAssociatedEDIStatuses().add(getWS(childStatusDto));
                }
            }
            for(EDIFileExceptionCodeDTO ediFileExceptionCodeDTO:dto.getExceptionCodes()){
                ws.getExceptionCodes().add(getExceptionCodeWS(ediFileExceptionCodeDTO,ws));
            }
            return ws;
    }

    /*
    * This method used for EDIType level but you can also used for EDIFile.
    * Because EDIType have a list of status as a parent child self relationship.
    * Then binding same status as a parent level and child level to another status, if another status have child status.
    * */
    public EDIFileStatusWS getWS(EDIFileStatusDTO dto,Map<String,EDIFileStatusWS> ediFileStatusWSMap) {
        // ediFileStatusWSMap used for proper parent-child mapping
        if(ediFileStatusWSMap.get(dto.getName())!=null){
            return ediFileStatusWSMap.get(dto.getName());
        }else{
            EDIFileStatusWS ws = new EDIFileStatusWS();
            if(dto != null && dto.getId() > 0) {
                ws.setId(dto.getId());
            }

            ws.setCreateDatetime(dto.getCreateDatetime());
            ws.setName(dto.getName());
            ws.setError(dto.isError());
            if(dto.getAssociatedEDIStatuses().size()>0){
                for(EDIFileStatusDTO childStatusDto:dto.getAssociatedEDIStatuses()){
                    ws.getChildStatuesIds().add(childStatusDto.getId());
                }
            }
            if(dto.getAssociatedEDIStatuses().size()>0){
                for(EDIFileStatusDTO childStatusDto:dto.getAssociatedEDIStatuses()){
                    //recursive call for child's
                    ws.getAssociatedEDIStatuses().add(getWS(childStatusDto,ediFileStatusWSMap));
                }
            }
            for(EDIFileExceptionCodeDTO ediFileExceptionCodeDTO:dto.getExceptionCodes()){
                ws.getExceptionCodes().add(getExceptionCodeWS(ediFileExceptionCodeDTO,ws));
            }
            ediFileStatusWSMap.put(ws.getName(),ws);
            return ws;
        }

    }

    public EDIFileStatusDTO getDTO(EDIFileStatusWS ws,Map<String,EDIFileStatusDTO> ediFileStatusDTOMap) throws SessionInternalError {
        //ediFileStatusDTOMap used for proper parent-child mapping
        if (ediFileStatusDTOMap.get(ws.getName()) != null) {
            return ediFileStatusDTOMap.get(ws.getName());
        } else {
            EDIFileStatusDTO dto = new EDIFileStatusDTO();

            if (ws.getId() != null && ws.getId() > 0) {
                dto = ediFileStatusDAS.find(ws.getId());
            }
            dto.setCreateDatetime(TimezoneHelper.serverCurrentDate());
            dto.setName(ws.getName());

            if (ws.getAssociatedEDIStatuses() != null && ws.getAssociatedEDIStatuses().size() > 0) {
                List<EDIFileStatusDTO> associatedEDIStatuses = new ArrayList<EDIFileStatusDTO>();
                for (EDIFileStatusWS ediFileStatusWS : ws.getAssociatedEDIStatuses()) {
                    //recursive call for child's
                    associatedEDIStatuses.add(getDTO(ediFileStatusWS,ediFileStatusDTOMap));
                }
                dto.setAssociatedEDIStatuses(associatedEDIStatuses);
            }

            if (ws.getExceptionCodes() != null && ws.getExceptionCodes().size() > 0) {
                List<EDIFileExceptionCodeDTO> exceptionCodes = new ArrayList<EDIFileExceptionCodeDTO>();
                for (EDIFileExceptionCodeWS ediFileExceptionCodeWS : ws.getExceptionCodes()) {
                    exceptionCodes.add(getExceptionCodeDTO(dto, ediFileExceptionCodeWS));
                }
                dto.setExceptionCodes(exceptionCodes);
            }
            dto.setError(ws.isError());
            ediFileStatusDTOMap.put(dto.getName(), dto);
            return dto;
        }

    }

    public EDIFileExceptionCodeDTO getExceptionCodeDTO(EDIFileStatusDTO dto, EDIFileExceptionCodeWS ediFileExceptionCodeWS) {
        EDIFileExceptionCodeDTO ediFileExceptionCodeDTO = new EDIFileExceptionCodeDTO();
        if (ediFileExceptionCodeWS.getId() != null) {
            ediFileExceptionCodeDTO = new EDIFileExceptionCodeDAS().find(ediFileExceptionCodeWS.getId());
        }
        ediFileExceptionCodeDTO.setExceptionCode(ediFileExceptionCodeWS.getCode());
        ediFileExceptionCodeDTO.setDescription(ediFileExceptionCodeWS.getDescription());
        ediFileExceptionCodeDTO.setStatus(dto);

        return ediFileExceptionCodeDTO;
    }

    public EDIFileExceptionCodeWS getExceptionCodeWS(EDIFileExceptionCodeDTO dto, EDIFileStatusWS ws) {
        EDIFileExceptionCodeWS ediFileExceptionCodeWS = new EDIFileExceptionCodeWS();
        if (dto != null && dto.getId() > 0) {
            ediFileExceptionCodeWS.setId(dto.getId());
        }
        ediFileExceptionCodeWS.setDescription(dto.getDescription());
        ediFileExceptionCodeWS.setCode(dto.getExceptionCode());
        ediFileExceptionCodeWS.setEdiFileStatusWS(ws);

        return ediFileExceptionCodeWS;
    }

    public Integer create(EDIFileStatusWS fileStatusWS){
        EDIFileStatusDTO ediFileStatusDTO=getDTO(fileStatusWS,new HashMap<String, EDIFileStatusDTO>());
        EDIFileStatusDTO fileStatusDTO= ediFileStatusDAS.save(ediFileStatusDTO);
        return fileStatusDTO.getId();
    }

    public EDIFileStatusDTO findById(Integer id){
        return ediFileStatusDAS.find(id);
    }


}
