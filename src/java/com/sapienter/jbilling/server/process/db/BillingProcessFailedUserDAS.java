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
package com.sapienter.jbilling.server.process.db;

import java.util.List;

import org.hibernate.Query;

import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class BillingProcessFailedUserDAS extends AbstractDAS<BillingProcessFailedUserDTO> {

	public BillingProcessFailedUserDTO create(BillingProcessInfoDTO batchProcessDTO, UserDTO userDTO) {
		BillingProcessFailedUserDTO dto = new BillingProcessFailedUserDTO();
		dto.setBatchProcess(batchProcessDTO);
		dto.setUser(userDTO);
        dto = save(dto);
        return dto;
    }
	
	 public List<BillingProcessFailedUserDTO> getEntitiesByBatchProcessId(Integer entityId) {
	        final String hql =
	            "select a " +
	            "  from BillingProcessFailedUserDTO a " +
	            " where a.batchProcess.id = :entity " +
	            " order by a.id desc ";
	       
	        Query query = getSession().createQuery(hql);
	        query.setParameter("entity", entityId);
	        return (List<BillingProcessFailedUserDTO>) query.list();
	}
	 
	public void removeFailedUsersForBatchProcess(Integer batchProcessId) {
	        String hql = "DELETE FROM " + BillingProcessFailedUserDTO.class.getSimpleName() +
	                " WHERE batchProcess.id = :batchProcessId";
	        Query query = getSession().createQuery(hql);
	        query.setParameter("batchProcessId", batchProcessId);
	        query.executeUpdate();
	}
	
	public List<BillingProcessFailedUserDTO> getEntitiesByExecutionId(Integer executionId) {
		   final String hql =
	            "select a " +
	            "  from BillingProcessFailedUserDTO a " +
	            " where a.batchProcess.jobExecutionId = :entity " +
	            " order by a.id desc ";
	       
	        Query query = getSession().createQuery(hql);
	        query.setParameter("entity", executionId);
	        return (List<BillingProcessFailedUserDTO>) query.list();
	}
}
