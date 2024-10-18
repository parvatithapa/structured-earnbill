package com.sapienter.jbilling.server.invoice;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * @author Juan Vidal - 08Jan16
 */
public class InvoiceTemplateDAS extends AbstractDAS<InvoiceTemplateDTO> {

    @SuppressWarnings("unchecked")
    public List<InvoiceTemplateDTO> findAllInvoiceTemplateByEntity(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(getPersistentClass());
        query.createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (List<InvoiceTemplateDTO>) getHibernateTemplate().findByCriteria(query);
    }

    public boolean isDuplicateInvoiceTemplate(String invoiceTemplateName, Integer entityId) {
        Criteria criteria = getSession().createCriteria(InvoiceTemplateDTO.class)
                .createAlias("entity", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("name", invoiceTemplateName))
                .setProjection(Projections.rowCount());

        return (Long) criteria.uniqueResult() > 0 ? true : false;
    }

    public Integer getDefaultTemplateId(String defaultTemplateName) {
        return (Integer) getSession().createCriteria(InvoiceTemplateDTO.class)
                .setProjection(Projections.property("id"))
                .add(Restrictions.eq("name", defaultTemplateName))
                .uniqueResult();
    }
    
    public Integer getDefaultTemplateId(String defaultTemplateName, Integer entityId) {
        Criteria criteria = getSession().createCriteria(InvoiceTemplateDTO.class)
                .createAlias("entity", "entity")
                .setProjection(Projections.property("id"))
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("name", defaultTemplateName));

        return (Integer)criteria.uniqueResult();
    }

}
