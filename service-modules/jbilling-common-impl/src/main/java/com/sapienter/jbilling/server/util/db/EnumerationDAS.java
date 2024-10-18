/**
 * 
 */
package com.sapienter.jbilling.server.util.db;

import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * @author Vikas Bodani
 * @since 10-Aug-2011
 *
 */
public class EnumerationDAS extends AbstractDAS<EnumerationDTO> {

	@SuppressWarnings("unchecked")
    public boolean exists(Integer id, String name, Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(getPersistentClass());
        query.add(Restrictions.ne("id", id));
        query.add(Restrictions.eq("name", name).ignoreCase());
        query.add(Restrictions.eq("entityId", entityId));
        List<EnumerationDTO> enumerations = (List<EnumerationDTO>)getHibernateTemplate().findByCriteria(query);
        return !enumerations.isEmpty() ? true : false;
    }

    /**
     * Queries the data source for an {@link EnumerationDTO}
     * entity filtered by <code>id</code> and <code>entityId</code>
     *
     * @param id representing the unique Enumeration entity.
     * @param entityId representing the callers company.
     * @return {@link EnumerationDTO} entity, representing the result set.
     */
    public EnumerationDTO getEnumeration(Integer id, Integer entityId){
        Query query = getSessionFactory().getCurrentSession().getNamedQuery("EnumerationDTO.findByEntityAndId");
        query.setInteger("entityId", entityId).setInteger("enumerationId", id);
        List<EnumerationDTO> resultSet = query.list();
        return (null != resultSet && Integer.valueOf(0) < resultSet.size()) ? resultSet.get(0): null;
    }

    /**
     * Queries the data source for an {@link EnumerationDTO}
     * entity filtered by <code>name</code> and <code>entityId</code>
     *
     * @param name of Enumeration entity.
     * @param entityId representing the callers company.
     * @return {@link EnumerationDTO} entity, representing the result set.
     */
    public EnumerationDTO getEnumerationByName(String name, Integer entityId){
        Query query = getSessionFactory().getCurrentSession().getNamedQuery("EnumerationDTO.findByEntityAndName");
        query.setInteger("entityId", entityId).setString("name", name);
        List<EnumerationDTO> resultSet = query.list();
        return (null != resultSet && Integer.valueOf(0) < resultSet.size()) ? resultSet.get(0): null;
    }

    /**
     * Queries the data source for all {@link EnumerationDTO}
     * entities filtered by <code>entityId</code>, starting from <code>offset</code> row
     * and <code>max</code> number of rows.
     *
     * @param entityId representing the callers company.
     * @param max representing maximum number of rows (optional).
     * @param offset representing the offset (optional).
     * @return {@link EnumerationDTO} entity, representing the result set.
     */
    public List<EnumerationDTO> getAllEnumerations(Integer entityId, Integer max, Integer offset){
        Query query = getSessionFactory().getCurrentSession().getNamedQuery("EnumerationDTO.findAll");
        query.setInteger("entityId", entityId);
        if(null != max){
            query.setMaxResults(max);
        }
        if(null != offset){
            query.setFirstResult(offset);
        }
        return query.list();
    }

    /**
     * Queries the data source for a number
     * representing the count of all persisted entities.
     *
     * @param entityId representing the callers company.
     * @return number of persisted entities.
     */

    public Long getAllEnumerationsCount(Integer entityId){
        Query query = getSessionFactory().getCurrentSession().getNamedQuery("EnumerationDTO.getCountAll");
        query.setInteger("entityId", entityId);
        return (Long)query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<EnumerationDTO> findAllEnumerationByEntity(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(getPersistentClass());
        query.add(Restrictions.eq("entityId", entityId));
        List<EnumerationDTO> enumerations = (List<EnumerationDTO>) getHibernateTemplate().findByCriteria(query);
        return enumerations;
    }
}
