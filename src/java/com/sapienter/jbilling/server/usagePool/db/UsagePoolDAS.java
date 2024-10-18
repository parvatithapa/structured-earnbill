/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool.db;

import java.util.Collection;
import java.util.List;

import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * UsagePoolDAS
 * DAS for interacting with usage pool table. 
 * Functions that check for duplicate usage pool names. 
 * Also a function that finds all usage pools for the given entity.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class UsagePoolDAS extends AbstractDAS<UsagePoolDTO> {
	
	/**
	 * This method finds all usage pools for the given company from db
	 * and returns it as a list.
	 * @param entityId
	 * @return List<UsagePoolDTO>
	 */
	public List<UsagePoolDTO> findByEntityId(Integer entityId)
	{
		Criteria criteria = getSession().createCriteria(UsagePoolDTO.class)
        		.add(Restrictions.eq("entity.id", entityId))
        		.addOrder(Order.asc("id"));
        return criteria.list();
	}
	
	/**
	 * This function is added to check duplicate usage pool name while adding new usage pool.
	 * Since international_description table does not have entity_id, its required that a join is done with usage_pool table.
	 * @param table
	 * @param column
	 * @param content
	 * @param language
	 * @param entityId
	 * @return
	 */
	public Collection<InternationalDescriptionDTO> nameExists(String table, String column, String content, 
			Integer language, Integer entityId) {

		JbillingTableDAS jtDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        
        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a, UsagePoolDTO usagePool " +
                "WHERE a.id.foreignId = usagePool.id " +
                "AND usagePool.entity.id = :entityId " +
                "AND a.id.tableId = :tableId " +
                "AND a.id.psudoColumn = :psudoColumn " +
                "AND a.id.languageId = :languageId " +
                "AND UPPER(a.content) = UPPER(:content)";

            Query query = getSession().createQuery(QUERY);
            query.setParameter("entityId", entityId);
            query.setParameter("tableId", jtDAS.findByName(table).getId());
            query.setParameter("psudoColumn", column);
            query.setParameter("languageId", language);
            query.setParameter("content", content);
            return query.list();
    }
	
	/**
	 * This function is added to check duplicate usage pool name while updating an existing usage pool.
	 * Its an overloaded variant of the above nameExists method.
	 * It checks for all names excluding the usage pool which is being updated.
	 * @param table
	 * @param column
	 * @param content
	 * @param language
	 * @param entityId
	 * @param usagePoolId
	 * @return
	 */
	@Transactional(readOnly = true)
	public Collection<InternationalDescriptionDTO> nameExists(String table, String column, 
			String content, Integer language, Integer entityId, Integer usagePoolId) {

		JbillingTableDAS jtDAS = (JbillingTableDAS) Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        
        final String QUERY = "SELECT a " +
                "FROM InternationalDescriptionDTO a, UsagePoolDTO usagePool " +
                "WHERE a.id.foreignId = usagePool.id " +
                "AND usagePool.entity.id = :entityId " +
                "AND usagePool.id <> :usagePoolId " +
                "AND a.id.tableId = :tableId " +
                "AND a.id.psudoColumn = :psudoColumn " +
                "AND a.id.languageId = :languageId " +
                "AND UPPER(a.content) = UPPER(:content)";

            Query query = getSession().createQuery(QUERY);
            query.setParameter("entityId", entityId);
            query.setParameter("usagePoolId", usagePoolId);
            query.setParameter("tableId", jtDAS.findByName(table).getId());
            query.setParameter("psudoColumn", column);
            query.setParameter("languageId", language);
            query.setParameter("content", content);
            return query.list();
    }
	
}
