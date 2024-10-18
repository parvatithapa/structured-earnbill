package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.DateType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.Map;

public class AccountInformationTypeDAS extends AbstractDAS<AccountInformationTypeDTO> {


    public List<AccountInformationTypeDTO> getAvailableAccountInformationTypes(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
        query.add(Restrictions.eq("entityId", entityId));

        return (List<AccountInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
    }

    public List<AccountInformationTypeDTO> getInformationTypesForAccountType(Integer accountTypeId) {
        DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
        query.add(Restrictions.eq("accountType.id", accountTypeId));
        query.addOrder(Order.asc("displayOrder"));

        return (List<AccountInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
    }

    public AccountInformationTypeDTO findByName(String name, Integer entityId, Integer accountTypeId) {
        DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
        query.setFetchMode("metaFields", FetchMode.JOIN);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.eq("name", name));
        query.add(Restrictions.eq("accountType.id", accountTypeId));
        List<AccountInformationTypeDTO> list = (List<AccountInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
        return !list.isEmpty() ? list.get(0) : null;

    }
    
     public List<CustomerAccountInfoTypeMetaField> findByCustomerAndEffectiveDate(Integer customerId, Integer accountInfoTypeId, Date effectiveDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(effectiveDate);
        cal.add(Calendar.DATE, 1);
    	 
    	DetachedCriteria query = DetachedCriteria.forClass(CustomerAccountInfoTypeMetaField.class);
        query.add(Restrictions.eq("customer.id", customerId));
        query.add(Restrictions.eq("accountInfoType.id", accountInfoTypeId));
        query.add(Restrictions.ge("effectiveDate", effectiveDate));
        query.add(Restrictions.lt("effectiveDate", cal.getTime()));
        List<CustomerAccountInfoTypeMetaField> list = (List<CustomerAccountInfoTypeMetaField>)getHibernateTemplate().findByCriteria(query);
        return !list.isEmpty() ? list : null;

    }

    public Integer getBaseUserIdbyMetaFiedId(Integer metafieldValueId){

        Criteria criteria = getSession().createCriteria(CustomerAccountInfoTypeMetaField.class);
        criteria.add(Restrictions.eq("metaFieldValue.id", metafieldValueId));
        criteria.setProjection(Projections.property("customer"));
        CustomerDTO customerDTO=(CustomerDTO) criteria.uniqueResult();

        Integer baseUserId=customerDTO.getBaseUser().getUserId();
        return  baseUserId;
    }

    public AccountInformationTypeDTO getGroupByNameAndEntityId(Integer entityId, EntityType entityType, String name, Integer accountTypeID) {

        if(name==null||name.trim().length()==0){
            return null;
        }
        DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.eq("entityType", entityType));
        query.add(Restrictions.eq("name", name));
        query.add(Restrictions.eq("accountType.id", accountTypeID));
        List<AccountInformationTypeDTO> fields = (List<AccountInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }
    
    public List<AccountInformationTypeDTO> getAccountInformationTypesByNameAndEntityId(Integer entityId, EntityType entityType, String name) {

    	if(name==null||name.trim().length()==0){
    		return Collections.<AccountInformationTypeDTO>emptyList();
    	}
    	DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
    	query.add(Restrictions.eq("entityId", entityId));
    	query.add(Restrictions.eq("entityType", entityType));
    	query.add(Restrictions.eq("name", name));
    	List<AccountInformationTypeDTO> fields = (List<AccountInformationTypeDTO>)getHibernateTemplate().findByCriteria(query);
    	return fields!=null && !fields.isEmpty() ? fields: Collections.<AccountInformationTypeDTO>emptyList();
    }

    public MetaFieldGroup getAccountInformationTypeByName(Integer entityId, Integer accountTypeId, String name) {
        if (name == null || name.trim().length() == 0) {
            return null;
        }

        DetachedCriteria query = DetachedCriteria.forClass(AccountInformationTypeDTO.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.eq("accountType.id", accountTypeId));
        query.add(Restrictions.eq("entityType", EntityType.ACCOUNT_TYPE));
        query.add(Restrictions.eq("name", name));

        List<MetaFieldGroup> fields = (List<MetaFieldGroup>) getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }

    public Map<Integer, String> getInformationTypeIdAndNameMapForAccountType(Integer accountTypeId) {
        Criteria criteria = getSession().createCriteria(AccountInformationTypeDTO.class);
        criteria.add(Restrictions.eq("accountType.id", accountTypeId));
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

    public AccountInformationTypeDTO findAccountTypeByMetafieldGroup(String groupName, String metafieldName, Object metafieldDefaultValue, Integer entityId, DataType dataType) {
        Criteria criteria = getSession().createCriteria(AccountInformationTypeDTO.class)
                .createAlias("metaFields", "mf")
                .createAlias("mf.defaultValue", "dv")
                .add(Restrictions.ilike("name", groupName))
                .add(Restrictions.eq("entityId", entityId))
                .add(Restrictions.ilike("mf.name", metafieldName));
        Criterion criterion = criteriaMetafieldValueType(dataType, metafieldDefaultValue);
        if( criterion != null ){
            criteria.add(criterion);
        }else{
            return null;
        }
        return (AccountInformationTypeDTO) criteria.uniqueResult();
    }

    private Criterion criteriaMetafieldValueType(DataType dateType, Object value) {
        if (dateType == DataType.STRING) {
            return Restrictions.sqlRestriction("string_value =  ?", value, StringType.INSTANCE);
        }
        if (dateType == DataType.BOOLEAN) {
            return Restrictions.sqlRestriction("boolean_value =  ?", value, BooleanType.INSTANCE);
        }
        if (dateType == DataType.DECIMAL) {
            return Restrictions.sqlRestriction("decimal_value =  ?", value, BigDecimalType.INSTANCE);
        }
        if (dateType == DataType.INTEGER) {
            return Restrictions.sqlRestriction("integer_value =  ?", value, IntegerType.INSTANCE);
        }
        if (dateType == DataType.DATE) {
            return Restrictions.sqlRestriction("date_value =  ?", value, DateType.INSTANCE);
        }
        return null;
    }
}
