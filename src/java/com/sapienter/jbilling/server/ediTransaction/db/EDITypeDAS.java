package com.sapienter.jbilling.server.ediTransaction.db;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.List;

public class EDITypeDAS extends AbstractDAS<EDITypeDTO> {

    public Long countByStatusId(Integer statusId){
            Criteria criteria = getSession().createCriteria(EDITypeDTO.class)
                    .createAlias("statuses", "st")
                    .add(Restrictions.eq("st.id", statusId));
            criteria.setProjection(Projections.rowCount());
            return (Long) criteria.uniqueResult();

    }

    public EDITypeDTO getEDITypeByEntity(Integer entityId, String statusName) {
        Criteria criteria = getSession().createCriteria(EDITypeDTO.class)
                .createAlias("entities", "com")
                .createAlias("statuses", "status")
                .add(Restrictions.eq("status.name", statusName))
                .add(Restrictions.eq("com.id", entityId));

        return (EDITypeDTO)criteria.uniqueResult();
    }


    public EDITypeDTO findByIdAndCompanyId(Integer ediTypeId, Integer entityId) {
        Criteria criteria = getSession().createCriteria(EDITypeDTO.class)
                .createAlias("entities", "com")
                .add(Restrictions.eq("id", ediTypeId))
                .add(Restrictions.eq("com.id", entityId));

        return (EDITypeDTO)criteria.uniqueResult();
    }

    public Long countByEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(EDITypeDTO.class)
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .setProjection(Projections.rowCount());
        return (Long) criteria.uniqueResult();
    }

    public List<EDITypeDTO> getEDITypesByEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(EDITypeDTO.class)
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId));
        return criteria.list();
    }

    public boolean isEDITypeAlreadyExist(Integer entityId, String name) {
        Criteria criteria = getSession().createCriteria(EDITypeDTO.class)
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("name", name))
                .setProjection(Projections.rowCount());
        Long ediTypeCount = (Long) criteria.uniqueResult();
        return ediTypeCount > 0;
    }
}
