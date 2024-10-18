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
package com.sapienter.jbilling.server.util.db;

import org.hibernate.Query;

public class PreferenceTypeDAS extends AbstractDAS<PreferenceTypeDTO> {

	private static final String QUERY = "SELECT pType " +
            "FROM PreferenceTypeDTO pType, InternationalDescriptionDTO a " +
            "WHERE pType.id = a.id.foriegnId " +
            "AND a.id.tableId = :tableId "+
            "AND a.content = :content "+
            "AND a.id.languageId = :languageId";
	public PreferenceTypeDTO getPreferenceByContent(String content, int tableId, int languageId){
		
		Query query = getSession().createQuery(QUERY);
		query.setParameter("content", content);
		query.setParameter("tableId", tableId);
		query.setParameter("languageId", languageId);
		
		return (PreferenceTypeDTO) query.uniqueResult();
	}

}
