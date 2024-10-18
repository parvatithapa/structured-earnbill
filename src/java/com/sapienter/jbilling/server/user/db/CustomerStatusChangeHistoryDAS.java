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

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.Date;
import java.util.List;

/**
 *
 * @author Leandro Zoi
 * @since 01/51/18
 *
 */

public class CustomerStatusChangeHistoryDAS extends AbstractDAS<CustomerStatusChangeHistoryDTO> {

    public void createCustomerHistory(UserDTO user,String userName, String stepStatus, CustomerStatusType currentStatus, Date effectiveDate) {
        CustomerStatusChangeHistoryDTO history = new CustomerStatusChangeHistoryDTO();
        history.setBaseUser(user);
        history.setCollectionsStepStatus(stepStatus);
        history.setCurrentStatus(currentStatus);
        history.setModifiedAt(effectiveDate != null ? effectiveDate : DateConvertUtils.getNow());
        history.setModifiedBy(userName);
        save(history);
    }

    @SuppressWarnings("unchecked")
    public List<CustomerStatusChangeHistoryDTO> getHistoryByUserAndType(Integer userId, CustomerStatusType type, Date startDate, Date endDate) {
        Criteria criteria = getSession().createCriteria(CustomerStatusChangeHistoryDTO.class);
        criteria.add(Restrictions.eq("baseUser.id", userId));
        criteria.add(Restrictions.eq("currentStatus", type));
        criteria.add(Restrictions.ge("modifiedAt", startDate));
        criteria.add(Restrictions.le("modifiedAt", endDate));
        criteria.addOrder(Order.asc("modifiedAt"));

        return (List<CustomerStatusChangeHistoryDTO>) criteria.list();
    }

    public boolean hasSuspendenHistoryOpen(Integer userId) {
        Criteria countActivatedRows = getSession().createCriteria(CustomerStatusChangeHistoryDTO.class);
        countActivatedRows.add(Restrictions.eq("baseUser.id", userId));
        countActivatedRows.add(Restrictions.eq("currentStatus", CustomerStatusType.ACTIVE));
        countActivatedRows.setProjection(Projections.rowCount());

        Criteria countSuspendedRows = getSession().createCriteria(CustomerStatusChangeHistoryDTO.class);
        countSuspendedRows.add(Restrictions.eq("baseUser.id", userId));
        countSuspendedRows.add(Restrictions.eq("currentStatus", CustomerStatusType.SUSPENDED));
        countSuspendedRows.setProjection(Projections.rowCount());

        return ((long) countSuspendedRows.uniqueResult()) > ((long) countActivatedRows.uniqueResult());
    }

    public CustomerStatusChangeHistoryDTO getLastSuspendedPeriod(Integer userId){
        Criteria criteria = getSession().createCriteria(CustomerStatusChangeHistoryDTO.class);
        criteria.add(Restrictions.eq("baseUser.id", userId));
        criteria.add(Restrictions.eq("currentStatus", CustomerStatusType.SUSPENDED));
        criteria.addOrder(Order.desc("modifiedAt"));
        criteria.setMaxResults(1);

        return (CustomerStatusChangeHistoryDTO) criteria.uniqueResult();
    }

    public CustomerStatusChangeHistoryDTO getSuspendedPeriodBetweenPeriods(Integer userId, Date startDate, Date enDate){
        Criteria criteria = getSession().createCriteria(CustomerStatusChangeHistoryDTO.class);
        criteria.add(Restrictions.eq("baseUser.id", userId));
        criteria.add(Restrictions.eq("currentStatus", CustomerStatusType.SUSPENDED));
        criteria.add(Restrictions.ge("modifiedAt", startDate));
        criteria.add(Restrictions.le("modifiedAt", enDate));
        criteria.addOrder(Order.desc("modifiedAt"));
        criteria.setMaxResults(1);

        return (CustomerStatusChangeHistoryDTO) criteria.uniqueResult();
    }
}


