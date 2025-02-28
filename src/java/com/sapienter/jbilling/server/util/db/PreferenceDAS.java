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

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.Constants;

public class PreferenceDAS extends AbstractDAS<PreferenceDTO> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PreferenceDAS.class));
    private static final LockOptions PESSIMISTIC_WRITE = new LockOptions(LockMode.PESSIMISTIC_WRITE);


    private static final String FIND_BY_TYPE_SQL =
            "SELECT a " +
                    "  FROM PreferenceDTO a " +
                    " WHERE a.preferenceType.id = :typeId " +
                    "   AND a.foreignId = :foreignId " +
                    "   AND a.jbillingTable.name = :tableName ";

    public PreferenceDTO findByType_Row(Integer typeId,Integer foreignId,String tableName) {
        return (PreferenceDTO) createFindByTypeQuery(typeId, foreignId, tableName)
                .setCacheable(true)
                .uniqueResult();
    }

    public PreferenceDTO findByTypeWithLock(Integer typeId, Integer foreignId, String tableName) {
        return (PreferenceDTO) createFindByTypeQuery(typeId, foreignId, tableName)
                .setLockOptions(PESSIMISTIC_WRITE)
                .uniqueResult();
    }

    private Query createFindByTypeQuery(Integer typeId,Integer foreignId,String tableName)  {
        return getSession().createQuery(FIND_BY_TYPE_SQL)
                .setParameter("typeId", typeId)
                .setParameter("foreignId", foreignId)
                .setParameter("tableName", tableName);
    }


    /**
     * This method is used for caching preferences. The criteria query fetches
     * all preference types from preference_type table with an outer join on preference table.
     * DetachedCriteria is used to select from jbilling_table based on entity table name.
     * 3 fields are selected using Projections: preferenceType.id, preference.value, preferenceType.defaultValue
     * Method is called from PreferenceBL, and while caching into a map, preferenceType.id becomes the map key,
     * if preference.value is null, then preferenceType.defaultValue is used as map value.
     * @param entityId
     * @return an Object[] containing preference type id and preference values (both value set for entity and default)
     */
    @SuppressWarnings({ "unchecked" })
    public List<Object[]> getPreferencesByEntity(Integer entityId) {

        DetachedCriteria subCriteria = DetachedCriteria.forClass(JbillingTable.class, "jbillingTable");
        subCriteria.setProjection(Projections.property("jbillingTable.id"));
        subCriteria.add(Restrictions.eq("jbillingTable.name", Constants.TABLE_ENTITY));

        Criteria criteria = getSession().createCriteria(PreferenceTypeDTO.class, "preferenceTypeDto");

        criteria.createAlias("preferenceTypeDto.preferences",
                "preference",
                JoinType.LEFT_OUTER_JOIN,
                Restrictions.and(
                        Restrictions.eq("preference.foreignId", entityId),
                        Subqueries.propertyEq("preference.jbillingTable.id", subCriteria)
                        )
                );

        criteria.setProjection(Projections.projectionList()
                .add(Projections.property("preferenceTypeDto.id"))
                .add(Projections.property("preference.value"))
                .add(Projections.property("preferenceTypeDto.defaultValue")));

        return criteria.list();
    }

    /**
     * Single object that we will be using to hold lock against.
     */
    private static final Object LOCK = new Object();

    private static final String PREF_VALUE_HQL =
            "  FROM PreferenceDTO p" +
                    " WHERE p.preferenceType.id = :typeId " +
                    "   AND p.foreignId = :foreignId ";

    private static final String PREF_VALUE_UPDATE_HQL =
            "  UPDATE PreferenceDTO p" +
                    " SET p.value = :value " +
                    " WHERE p.preferenceType.id = :typeId " +
                    "   AND p.foreignId = :foreignId ";

    /**
     * This method return the current value of invoice number and increment
     * it to next invoice number. During read it gets lock over the LOCK
     * object until record updates. When record updated successfully
     * then release the lock so that can be read by other thread.
     */
    public Integer getPreferenceAndIncrement(Integer entityId, Integer typeId) {
        Integer value = null;

        synchronized (LOCK) {
            StatelessSession session = null;
            Transaction tx = null;
            try {
                session = getSessionFactory().openStatelessSession();
                tx = session.beginTransaction();

                Query query = session.createQuery(PREF_VALUE_HQL);
                query.setParameter("typeId", typeId);
                query.setParameter("foreignId", entityId);
                //			query.setLockMode("p", LockMode.UPGRADE);//no pessimistic locking for now
                PreferenceDTO preferenceDTO = (PreferenceDTO) query.uniqueResult();

                //If no record is set then next invoice will be start from 1.
                value = Integer.valueOf(1);
                if (preferenceDTO == null) {
                    //stop the generation of the invoice because an invoice number can not be generated
                    //here the code does not assume that the invoice numbers should start from 1.
                    //also there is a technical difficulty to do this insert in stateless session
                    throw new IllegalStateException("The preference for next invoice number must be set for all companies");

                } else if (preferenceDTO.getValue() != null) {
                    //the preference existed so just increment and update
                    value = preferenceDTO.getIntValue();
                    preferenceDTO.setValue(value + 1);

                    Query updateQuery = session.createQuery(PREF_VALUE_UPDATE_HQL);
                    updateQuery.setParameter("value", preferenceDTO.getValue());
                    updateQuery.setParameter("typeId", typeId);
                    updateQuery.setParameter("foreignId", entityId);
                    updateQuery.executeUpdate();
                }

                //if this explodes for some reason
                tx.commit();
                session.close();
            } catch (RuntimeException e) {
                LOG.debug("Generation of invoice number failed.", e);
                //no matter the exception, try doing the clean up and propagate the exception
                try {
                    if (null != tx) {if (tx.isActive() && !tx.wasRolledBack()) {
                        tx.rollback();
                    }}
                    if (null != session) {session.close();}
                } catch (Exception ex) {
                    //swallow the attempt to do a clean up
                }
                throw e;
            }
        }
        return value;
    }

}
