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
package com.sapienter.jbilling.server.user.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.type.StandardBasicTypes;

public class CompanyDAS extends AbstractDAS<CompanyDTO> {
    
    @SuppressWarnings({"unchecked", "deprecation"})
	public List<CompanyDTO> findEntities() {
        return getSession().createCriteria(CompanyDTO.class).list();
    }
    
    @SuppressWarnings({"unchecked", "deprecation"})
	public List<CompanyDTO> findChildEntities(Integer parentId) {
    	return getSession().createCriteria(CompanyDTO.class)
                .add(Restrictions.eq("parent.id", parentId)).list();
    }

    public CompanyDTO findRootFromSource(Integer companyId) {
        CompanyDTO current= findNow(companyId);
        Integer parentId = null;
        if (null != current ) {
            if (null != current.getParent()) {
                parentId = current.getParent().getId();
            } else {
                parentId = current.getId();
            }
        }
        return null != parentId ? findNow(parentId) : null;
    }

    public List<CompanyDTO> getHierachyEntities(Integer entityId) {
        List<CompanyDTO> allEntities = new ArrayList<>();
        CompanyDTO current= findNow(entityId);
        if (null != current ) {

            Integer parentId= null;
            if ( null != current.getParent() ) {
                parentId= current.getParent().getId();
            } else {
                parentId= current.getId();
                allEntities.add(current);
            }

            allEntities.addAll(getChildEntitiesIds(parentId).stream().map(this::findNow).collect(Collectors.toList()));
        }
        return allEntities;
    }
    
    @SuppressWarnings({"unchecked", "deprecation"})
	public List<Integer> getChildEntitiesIds(Integer parentId) {
    	return getSession().createCriteria(CompanyDTO.class)
                .add(Restrictions.eq("parent.id", parentId))
                .setProjection(Projections.id())
                .list();
    }

    public Integer getParentCompanyId(Integer entityId) {
        SQLQuery query = getSession().createSQLQuery(
                "select parent_id from entity where id = :entityId");
        query.setParameter("entityId", entityId);
        return (Integer) query.uniqueResult();
    }
    
    public boolean isRoot(Integer entityId){
    	CompanyDTO entity = find(entityId);
    	
    	if(entity == null) {
    		return false;
    	}
    	
    	if(entity.getParent() == null){
    		return true;
    	}
    	
    	// if it has some child entities then its root
    	List<CompanyDTO> childs = findChildEntities(entityId);
    	if(childs != null && childs.size() > 0) {
    		return true;
    	}
    	// this entity is consistent to be a non root
    	return false;
    }

    private Criteria _allHierarchyEntities (CompanyDTO entity) {
        CompanyDTO parentEntity = entity.getParent();
        @SuppressWarnings("deprecation")
        Criteria searchCriteria = getSession().createCriteria(CompanyDTO.class);
        Criterion itself = Restrictions.eq("id", entity.getId());
        Criterion findByParent = Restrictions.eq("parent.id",
                (parentEntity == null) ? entity.getId() : parentEntity.getId());
        return searchCriteria
                .add(Restrictions.or(itself, findByParent));
    }

    @SuppressWarnings("unchecked")
    public List<CompanyDTO> findAllHierarchyEntities (Integer entityId) {
        CompanyDTO entity = find(entityId);
        if(entity == null) {
            return Collections.emptyList();
        }
        return _allHierarchyEntities(entity)
                .list();
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findAllHierarchyEntitiesIds (Integer entityId) {
        CompanyDTO entity = findNow(entityId);
        if(entity == null) {
            return Collections.emptyList();
        }
        return _allHierarchyEntities(entity)
                .setProjection(Projections.id())
                .list();
    }

    public List<Integer> findAllCurrentAndChildEntities( Integer entityId) {
        CompanyDTO entity = find(entityId);
        if(entity == null) {
            return Collections.emptyList();
        }
        List<Integer> retVal= getChildEntitiesIds(entityId);
        retVal.add(entityId);

        return retVal;
    }

    /**
     * Returns a list of companies id for the descendants of the given company.
     * @param parent: parent company
     * @return All given company descendants
     */
    public List<Integer> getDescendants(Integer parent){
        List<Integer> descendants = new ArrayList<Integer>();
        if(parent != null){
            for(Integer company: getChildEntitiesIds(parent)){
                //add it as descendant
                descendants.add(company);
                //call the same function in a recursive way to get all the descendants
                descendants.addAll(getDescendants(company));
            }
        }
        return descendants;
    }

    /**
     * Returns a list of companies id for the descendants of the company given and itself
     * @param company: parent company
     * @return All given company descendants and itself.
     */
    public List<Integer> getCurrentAndDescendants(Integer company){
        List<Integer> descendants = getDescendants(company);
        descendants.add(company);
        return descendants;
    }

    public CompanyDTO findEntityByName(String companyName) {
        if(StringUtils.trimToNull(companyName) == null) {
            return null;
        }
        Criteria searchCriteria = getSession().createCriteria(CompanyDTO.class);
        searchCriteria.add(Restrictions.eq("description", companyName));
        return (CompanyDTO) searchCriteria.uniqueResult();
    }

    public CompanyDTO findEntityByMetaFieldValue(String value) {
        if(StringUtils.trimToNull(value) == null) {
            return null;
        }

        Criteria criteria = getSession().createCriteria(CompanyDTO.class)
                .createAlias("metaFields", "metaFieldValue")
                .createAlias("metaFieldValue.field", "metaField")
                .add(Restrictions.sqlRestriction("string_value =  ?", value, StandardBasicTypes.STRING));

        return (CompanyDTO) criteria.uniqueResult();
    }

    public void copyLiquibaseChangeLogs(Integer companyFrom, Integer companyTo, String filename){
        StringBuilder sb = new StringBuilder();
        sb.append("insert into databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase) ");
        sb.append("select id, author, :destinationFile, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase ");
        sb.append("from databasechangelog where filename like :originalFile ");

        Query query = getSessionFactory().getCurrentSession().createSQLQuery(sb.toString())
                .setParameter("destinationFile", companyTo + "|" + filename)
                .setParameter("originalFile", companyFrom + "|" + filename);

        query.executeUpdate();
    }

    /**
     * This method used for finding the company name by id.
     *
     * @param entityId used for find the description.
     * @return string company name.
     */
    public String findCompanyNameByEntityId(Integer entityId) {
        if (entityId == null) return null;
        Criteria searchCriteria = getSession().createCriteria(CompanyDTO.class)
                .add(Restrictions.eq("id", entityId))
                .setProjection(Projections.property("description"));
        return (String) searchCriteria.uniqueResult();
    }
}
