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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Context;

public abstract class AbstractDAS<T> extends HibernateDaoSupport implements IDAS<T> {

    private Class<T> persistentClass;

    // if querys will be run cached or not
    private boolean queriesCached = false;

    @SuppressWarnings("unchecked")
    public AbstractDAS() {
        this.persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        setSessionFactory(Context.getBean(Context.Name.HIBERNATE_SESSION));
    }


    /**
     * Merges the entity, creating or updating as necessary
     *
     * @param newEntity entity to save/update
     * @return saved entity
     */
    @Override
    @SuppressWarnings("unchecked")
    public T save(T newEntity) {
        return (T) getSession().merge(newEntity);
    }

    @Override
    public void delete(T entity) {
        getHibernateTemplate().delete(entity);
    }

    public void refresh(T entity) {
        getHibernateTemplate().refresh(entity);
    }

    public Class<T> getPersistentClass() {
        return persistentClass;
    }

    /**
     * This will load a proxy. If the row does not exist, it still returns an
     * object (not null) and  it will NOT throw an
     * exception (until the other fields are accessed).
     * Use this by default, if the row is missing, it is an error.
     * @param id
     * @return
     */
    @Override
    public T find(Serializable id) {
        if (id == null) {
            return null;
        }
        return getHibernateTemplate().load(getPersistentClass(), id);
    }

    /**
     * This will hit the DB. If the row does not exist, it will NOT throw an
     * exception but it WILL return NULL
     * @param id
     * @return
     */
    @Override
    public T findNow(Serializable id) {
        if (id == null) {
            return null;
        }
        return getHibernateTemplate().get(getPersistentClass(), id);
    }

    /**
     * This will lock the row for the duration of this transaction. Or wait until the row is
     * unlocked if it is already locked. It genererates a select ... for update
     * @param id
     * @return
     */
    public T findForUpdate(Serializable id) {
        if (id == null) {
            return null;
        }
        return getHibernateTemplate().get(getPersistentClass(), id, LockMode.PESSIMISTIC_WRITE);
    }

    @Override
    public List<T> findAll() {
        return findByCriteria();
    }

    public List<T> findAllByIdInList(List<Integer> ids) {
        if(null!= ids && !ids.isEmpty()){
            return findByCriteria(Restrictions.in("id", ids));
        }else{
            return Collections.emptyList();
        }

    }

    @SuppressWarnings("unchecked")
    public List<T> findByExample(T exampleInstance, String... excludeProperty) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        Example example =  Example.create(exampleInstance);
        for (String exclude : excludeProperty) {
            example.excludeProperty(exclude);
        }
        crit.add(example);
        crit.setCacheable(queriesCached);
        return crit.list();
    }

    @SuppressWarnings("unchecked")
    public T findByExampleSingle(T exampleInstance, String... excludeProperty) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        Example example =  Example.create(exampleInstance);
        for (String exclude : excludeProperty) {
            example.excludeProperty(exclude);
        }
        crit.add(example);
        crit.setCacheable(queriesCached);
        return (T) crit.uniqueResult();
    }

    public T makePersistent(T entity) {
        getHibernateTemplate().saveOrUpdate(entity);
        return entity;
    }

    public void makeTransient(T entity) {
        getHibernateTemplate().delete(entity);
    }

    public void flush() {
        getHibernateTemplate().flush();
    }

    public void clear() {
        getHibernateTemplate().clear();
    }

    /**
     * Returns true if a persisted record exists for the given id.
     *
     * @param id primary key of entity
     * @return true if entity exists for id, false if entity does not exist
     */
    public boolean isIdPersisted(Serializable id) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.idEq(id))
                .setProjection(Projections.rowCount());

        return (criteria.uniqueResult() != null && ((Long) criteria.uniqueResult()) > 0);
    }

    /**
     * Use this inside subclasses as a convenience method.
     */
    @SuppressWarnings("unchecked")
    protected List<T> findByCriteria(Criterion... criterion) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        crit.setCacheable(queriesCached);
        return crit.list();
    }

    @SuppressWarnings("unchecked")
    protected T findByCriteriaSingle(Criterion... criterion) {
        Criteria crit = getSession().createCriteria(getPersistentClass());
        for (Criterion c : criterion) {
            crit.add(c);
        }
        crit.setCacheable(queriesCached);
        return (T) crit.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public T findFirst(Query query) {
        query.setFirstResult(0).setMaxResults(1);
        return (T) query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public T findFirst(Criteria criteria) {
        criteria.setFirstResult(0).setMaxResults(1);
        return (T) criteria.uniqueResult();

    }

    protected void useCache() {
        queriesCached = true;
    }

    /**
     * Makes this DTO now attached to the session and part of the persistent context.
     * This WILL trigger an update, which is usually fine since the reason to reattach
     * is to modify the object.
     * @param dto
     */
    public void reattach(T dto) {
        getSession().update(dto);
    }

    /**
     * Places the DTO in the session without updates or version checkes.
     * You have to make sure that the DTO has not been modified to use this
     * @param dto
     */
    public void reattachUnmodified(T dto) {
        getSession().buildLockRequest(LockOptions.NONE).lock(dto);
    }

    /**
     * Detaches the DTO from the session. Updates to the object will
     * no longer make it to the database.
     */
    public void detach(T dto) {
        getSession().flush(); // without this, get ready for the evil 'nonthreadsafe access to session'
        getSession().evict(dto);
    }

    protected void touch(List<T> list, String methodName) {
        try {
            Method toCall = persistentClass.getMethod(methodName);
            for(int f=0; f< list.size(); f++) {
                toCall.invoke(list.get(f));
            }
        } catch (Exception e) {
            throw new SessionInternalError("Error invoking method when touching proxy object",
                    AbstractDAS.class, e);

        }
    }

    protected Session getSession() {
        return currentSession();
    }
}
