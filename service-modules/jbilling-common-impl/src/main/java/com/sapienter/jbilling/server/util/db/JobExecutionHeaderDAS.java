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

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;


public class JobExecutionHeaderDAS extends AbstractDAS<JobExecutionHeaderDTO> {

    public static final String FIND_DISTINCT_JOB_TYPES ="SELECT DISTINCT(p.job_type) from job_execution_header p";

    public List<JobExecutionHeaderDTO> findByTypeAndDates(Integer entityId, String type, Date startDate, Date endDate, int offset, int limit, String sort, String order) {
        DetachedCriteria query = DetachedCriteria.forClass(getPersistentClass());
        query.add(Restrictions.eq("entityId", entityId));
        if(!StringUtils.isEmpty(type)) {
            query.add(Restrictions.eq("jobType", type));
        }
        if(startDate != null) {
            query.add(Restrictions.ge("startDate", startDate));
        }
        if(endDate != null) {
            query.add(Restrictions.le("startDate", endDate));
        }
        query.addOrder("desc".equals(order) ? Order.desc(sort) : Order.asc(sort));
        return (List<JobExecutionHeaderDTO>)getHibernateTemplate().findByCriteria(query, offset, limit);
    }

    public JobExecutionHeaderDTO findByExecutionId(long executionId) {
        Query query = getSessionFactory().getCurrentSession().getNamedQuery("JobExecutionHeaderDTO.findByExecutionId");
        query.setCacheable(true);
        query.setLong("executionId", executionId);
        return (JobExecutionHeaderDTO) query.uniqueResult();
    }

    public List<String> findDistinctJobTypes(){

        SQLQuery query = getSession().createSQLQuery(FIND_DISTINCT_JOB_TYPES);

        return query.list();
    }

}
