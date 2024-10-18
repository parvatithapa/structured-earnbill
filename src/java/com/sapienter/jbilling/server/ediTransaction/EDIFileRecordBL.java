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
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileRecordDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDAS;
import com.sapienter.jbilling.server.ediTransaction.db.EDIFileStatusDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

/**
 * @author Emil
 */
public class EDIFileRecordBL {

    private EDIFileStatusDAS ediFileStatusDAS = null;
    private EDIFileRecordDAS ediFileRecordDAS = null;


    public EDIFileRecordBL(){
        init();
    }


    private  void init(){

        ediFileStatusDAS =new EDIFileStatusDAS();
        ediFileRecordDAS =new EDIFileRecordDAS();

    }

    public EDIFileStatusDTO getEDIFileStatusDTO(EDIFileStatusWS ws) throws SessionInternalError{

        EDIFileStatusDTO dto = new EDIFileStatusDTO();
        if(ws.getId() !=null && ws.getId()>0){
            dto.setId(ws.getId());
            dto.setVersionNum(ediFileStatusDAS.find(ws.getId()).getVersionNum());
        }
        dto.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        dto.setName(ws.getName());

        return dto;
    }

    public Integer create(EDIFileStatusWS fileStatusWS){
        EDIFileStatusDTO ediFileStatusDTO=getEDIFileStatusDTO(fileStatusWS);
        EDIFileStatusDTO fileStatusDTO= ediFileStatusDAS.save(ediFileStatusDTO);
        return fileStatusDTO.getId();
    }

}
