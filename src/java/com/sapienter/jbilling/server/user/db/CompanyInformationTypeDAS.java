package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.*;

import java.util.*;

public class CompanyInformationTypeDAS extends AbstractDAS<CompanyInformationTypeDTO> {


    public List<CompanyInformationTypeDTO> getAvailableCompanyInformationTypes(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(CompanyInformationTypeDTO.class);
        query.add(Restrictions.eq("entityId", entityId));

        return (List<CompanyInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
    }

    public List<CompanyInformationTypeDTO> getInformationTypesForCompany(Integer companyId) {
        DetachedCriteria query = DetachedCriteria.forClass(CompanyInformationTypeDTO.class);
        query.add(Restrictions.eq("company.id", companyId));
        query.addOrder(Order.asc("displayOrder"));

        return (List<CompanyInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
    }

    public CompanyInformationTypeDTO findByName(String name, Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(CompanyInformationTypeDTO.class);
        query.setFetchMode("metaFields", FetchMode.JOIN);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.eq("name", name));
        List<CompanyInformationTypeDTO> list = (List<CompanyInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
        return !list.isEmpty() ? list.get(0) : null;

    }

    public Integer getCompanyIdbyMetaFiedId(Integer metafieldValueId){

        Criteria criteria = getSession().createCriteria(CompanyInfoTypeMetaField.class);
        criteria.add(Restrictions.eq("metaFieldValue.id", metafieldValueId));
        criteria.setProjection(Projections.property("company"));
        CompanyDTO companyDTO =(CompanyDTO) criteria.uniqueResult();

        Integer companyId=companyDTO.getId();
        return  companyId;
    }

    public CompanyInformationTypeDTO getGroupByNameAndEntityId(Integer entityId, EntityType entityType, String name) {

        if(name==null||name.trim().length()==0){
            return null;
        }
        DetachedCriteria query = DetachedCriteria.forClass(CompanyInformationTypeDTO.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.eq("entityType", entityType));
        query.add(Restrictions.eq("name", name));
        List<CompanyInformationTypeDTO> fields = (List<CompanyInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }
    
    public List<CompanyInformationTypeDTO> getCompanyInformationTypesByNameAndEntityId(Integer entityId, EntityType entityType, String name) {

    	if(name==null||name.trim().length()==0){
    		return Collections.<CompanyInformationTypeDTO>emptyList();
    	}
    	DetachedCriteria query = DetachedCriteria.forClass(CompanyInformationTypeDTO.class);
    	query.add(Restrictions.eq("entityId", entityId));
    	query.add(Restrictions.eq("entityType", entityType));
    	query.add(Restrictions.eq("name", name));
    	List<CompanyInformationTypeDTO> fields = (List<CompanyInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
    	return fields!=null && !fields.isEmpty() ? fields: Collections.<CompanyInformationTypeDTO>emptyList();
    }

    public MetaFieldGroup getCompanyInformationTypeByName(Integer entityId, Integer companyId, String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }

        DetachedCriteria query = DetachedCriteria.forClass(CompanyInformationTypeDTO.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.eq("company.id", companyId));
        query.add(Restrictions.eq("entityType", EntityType.COMPANY_INFO));
        query.add(Restrictions.eq("name", name));

        List<MetaFieldGroup> fields = (List<MetaFieldGroup>) getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }
    
    public Map<Integer, String> getInformationTypeIdAndNameMapForCompany(Integer companyId) {
        Criteria criteria = getSession().createCriteria(CompanyInformationTypeDTO.class);
        criteria.add(Restrictions.eq("company.id", companyId));
        ProjectionList projections=Projections.projectionList();
        projections.add(Projections.id());
        projections.add(Projections.property("name"));
        criteria.setProjection(projections);
        Iterator iterator = criteria.list().iterator();
        Map<Integer, String> result = new HashMap<Integer, String>();
        while(iterator.hasNext()) {
            Object [] objects = (Object[]) iterator.next();
            result.put(Integer.valueOf(objects[0].toString()), objects[1].toString());
            
        }
        return result;
    }
}
