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

package com.sapienter.jbilling.server.customerEnrollment.db;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.account.AccountTypeBL;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentCommentWS;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;

import java.util.LinkedList;

/**
 * @author Emil
 */
public class CustomerEnrollmentCommentBL {

    private CustomerEnrollmentCommentDTO customerEnrollmentCommentDTO = null;
    private CustomerEnrollmentCommentDAS customerEnrollmentCommentDAS = null;


    public CustomerEnrollmentCommentBL(){
        init();
    }

    public CustomerEnrollmentCommentBL(Integer commentId){
        init();
        customerEnrollmentCommentDAS.find(commentId);
    }

    private  void init(){
        customerEnrollmentCommentDAS=new CustomerEnrollmentCommentDAS();
    }


    public CustomerEnrollmentCommentWS getWS(CustomerEnrollmentCommentDTO dto) {

        CustomerEnrollmentCommentWS ws = new CustomerEnrollmentCommentWS();
        ws.setId(dto.getId());
        ws.setComment(dto.getComment());
        if(dto.getUser() != null) {
            ws.setUserId(dto.getUser().getId());
            ws.setUserName(dto.getUser().getUserName());
        }
        ws.setDateCreated(dto.getCreationTime());
        return ws;
    }

    public CustomerEnrollmentCommentDTO getDTO(CustomerEnrollmentCommentWS ws) throws SessionInternalError{
        CustomerEnrollmentCommentDTO dto = new CustomerEnrollmentCommentDTO();
        dto.setComment(ws.getComment());
        dto.setId(ws.getId());
        if(ws.getUserId() != null && ws.getUserId() > 0) {
            dto.setUser(new UserDAS().find(ws.getUserId()));
        }
        dto.setCreationTime(ws.getDateCreated());
        return dto;
    }
}
