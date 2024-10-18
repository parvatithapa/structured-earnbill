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
package com.sapienter.jbilling.server.pluggableTask.admin;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.springframework.util.CollectionUtils;
import org.springmodules.cache.CachingModel;
import org.springmodules.cache.FlushingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;


public class PluggableTaskDAS extends AbstractDAS<PluggableTaskDTO> {

    private CacheProviderFacade cache;
    private CachingModel cacheModel;
    private FlushingModel flushModel;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PluggableTaskDAS.class));


    // QUERIES
    private static final String findAllByEntitySQL =
        "SELECT b " +
        "  FROM PluggableTaskDTO b " + 
        " WHERE b.entityId = :entity";
    
    private static final String findByEntityTypeSQL =
        findAllByEntitySQL + 
        "   AND b.type.id = :type";

    private static final String findByEntityCategoryOrderSQL =
        findAllByEntitySQL + 
        "   AND b.type.category.id = :category" +
        "   AND b.processingOrder = :pr_order";

    private static final String findByEntityCategorySQL =
        "SELECT b " +
        "  FROM PluggableTaskDTO b " + 
        " WHERE b.entityId = :entity " +
        "   AND b.type.category.id = :category" +
        " ORDER BY b.processingOrder";

    private static final String findByEntityAndClassNameHQL =
            findAllByEntitySQL +
            " AND b.type.className = :className";

    private static final String findByEntityAndPluggableTaskType=
            findAllByEntitySQL +
            " AND b.type.pk = :typeId";


    // END OF QUERIES
   
    private PluggableTaskDAS() {
        super();
    }
    
    public List<PluggableTaskDTO> findAllByEntity(Integer entityId) {
        Query query = getSession().createQuery(findAllByEntitySQL);
        query.setParameter("entity", entityId);
        query.setCacheable(true);
        query.setComment("PluggableTaskDAS.findAllByEntity");
        return query.list();
    }
    
    public PluggableTaskDTO findByEntityType(Integer entityId, Integer typeId) {
        try {
            Query query = getSession().createQuery(findByEntityTypeSQL);
            query.setCacheable(true);
            query.setParameter("entity", entityId);
            query.setParameter("type", typeId);
            query.setComment("PluggableTaskDAS.findByEntityType");
            return (PluggableTaskDTO) query.uniqueResult();
        } catch(NonUniqueResultException e){
            PluggableTaskTypeDTO dto = new PluggableTaskTypeDAS().find(typeId);
            throw new SessionInternalError("Plugin duplicated with type " + typeId,
                    new String[] { "error.invoice.download.pdf,"+dto.getDescription(new CompanyDAS().find(entityId).getLanguageId(), "title")});
        }
    }
    
    public PluggableTaskDTO findByEntityCategoryOrder(Integer entityId, Integer categoryId, Integer processingOrder) {
        Query query = getSession().createQuery(findByEntityCategoryOrderSQL);
        query.setCacheable(true);
        query.setParameter("entity", entityId);
        query.setParameter("category", categoryId);
        query.setParameter("pr_order", processingOrder);
        query.setComment("PluggableTaskDAS.findByEntityTypeOrder");
        return (PluggableTaskDTO) query.uniqueResult();
    }

    public List<PluggableTaskDTO> findByEntityCategory(Integer entityId, Integer categoryId) {
        List<PluggableTaskDTO> ret = (List<PluggableTaskDTO>) cache.getFromCache("PluggableTaskDTO" +
                entityId + "+" + categoryId, cacheModel);
        if (ret == null) {
            Query query = getSession().createQuery(findByEntityCategorySQL);
            query.setCacheable(true);
            query.setParameter("entity", entityId);
            query.setParameter("category", categoryId);
            query.setComment("PluggableTaskDAS.findByEntityCategory");

            ret = query.list();
            cache.putInCache("PluggableTaskDTO" +
                        entityId + "+" + categoryId, cacheModel, ret);
        }
        return ret;
    }


    /**
     * Returns all plugins for
     * specific <code>className</code> for the
     * supplied <code>entityId</code>.
     *
     * @param entityId fetch for this entity.
     * @param className filter by this class name.
     * @return List of {@link PluggableTaskWS} objects representing the result set.
     */
    public List<PluggableTaskDTO> findByEntityAndClassName(Integer entityId, String className){
        Query query = getSessionFactory().getCurrentSession().createQuery(findByEntityAndClassNameHQL);
        query.setParameter("entity", entityId);
        query.setParameter("className", className);
        return query.list();
    }

    public PluggableTaskDTO findByEntityAndType(Integer entityId,Integer typeId){
        Query query = getSessionFactory().getCurrentSession().createQuery(findByEntityAndPluggableTaskType);
        query.setParameter("entity", entityId);
        query.setParameter("typeId", typeId);
        List<PluggableTaskDTO> ret=query.list();
        return CollectionUtils.isEmpty(ret)?null:ret.get(0);
    }

    public void setCache(CacheProviderFacade cache) {
        this.cache = cache;
    }

    public void setCacheModel(CachingModel model) {
        cacheModel = model;
    }

    public void setFlushModel(FlushingModel flushModel) {
        this.flushModel = flushModel;
    }

    public void invalidateCache() {
        cache.flushCache(flushModel);
    }

    public static PluggableTaskDAS getInstance() {
        return new PluggableTaskDAS();
    }

}
