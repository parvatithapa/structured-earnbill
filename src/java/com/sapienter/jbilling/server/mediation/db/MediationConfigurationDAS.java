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

package com.sapienter.jbilling.server.mediation.db;

import java.util.List;

import org.hibernate.Query;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class MediationConfigurationDAS extends AbstractDAS<MediationConfiguration> {

    // QUERIES
    private static final String findAllByEntitySQL =
        "SELECT b " +
        "  FROM MediationConfiguration b " + 
        " WHERE b.entityId = :entity " +
        " ORDER BY orderValue";

    private static final String findAllByEntityIncludeGlobalSQL =
        "SELECT b " +
                "  FROM MediationConfiguration b " +
                " WHERE b.entityId = :childEntity OR " +
                "   ( b.entityId = :parentEntity AND b.global = true )" +
                " ORDER BY orderValue";

    private static final String findAllByPluggableTaskSQL =
            "SELECT b " +
            "  FROM MediationConfiguration b " + 
            " WHERE b.pluggableTask.id = :pluggableTask " +
            " ORDER BY orderValue";
    
    private static final String findAllByMediationProcessTaskSQL =
            "SELECT b " +
            "  FROM MediationConfiguration b " + 
            " WHERE b.processor.id = :processorTask " +
            " ORDER BY orderValue";

    private static final String FIND_ALL_BY_ENTITIES_SQL =
        "SELECT b " +
        "  FROM MediationConfiguration b " + 
        " WHERE b.entityId in (:entities) " +
        " ORDER BY orderValue";
    
    public List<MediationConfiguration> findAllByEntity(Integer entityId) {
        Query query = getSession().createQuery(findAllByEntitySQL);
        query.setParameter("entity", entityId);
        //return query.getResultList();
        return query.list();
    }
    
    @SuppressWarnings("unchecked")
    public List<MediationConfiguration> findAllByEntities(List<Integer> entities) {
        return getSession().createQuery(FIND_ALL_BY_ENTITIES_SQL)
                                  .setParameterList("entities", entities)
                                  .list();
    }

    public List<MediationConfiguration> findAllByEntityIncludeGlobal(Integer childCompanyId, Integer parentCompanyId){
        Query query = getSession().createQuery(findAllByEntityIncludeGlobalSQL);
        query.setParameter("childEntity", childCompanyId);
        query.setParameter("parentEntity", parentCompanyId);
        return query.list();
    }

    public List<MediationConfiguration> findAllByPluggableTask(Integer pluggableTaskId) {
    	Query query = getSession().createQuery(findAllByPluggableTaskSQL);
        query.setParameter("pluggableTask", pluggableTaskId);
        return query.list();
    }
    
    public List<MediationConfiguration> findAllByProcessorTask(Integer pluggableTaskId) {
    	Query query = getSession().createQuery(findAllByMediationProcessTaskSQL);
        query.setParameter("processorTask", pluggableTaskId);
        return query.list();
    }
}
