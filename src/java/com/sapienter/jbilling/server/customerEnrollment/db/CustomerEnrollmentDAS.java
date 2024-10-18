package com.sapienter.jbilling.server.customerEnrollment.db;

import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.*;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.StringType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CustomerEnrollmentDAS extends AbstractDAS<CustomerEnrollmentDTO> {

    private static final String FIND_CUSTOMER_ENROLLMENT_DATE =
            "SELECT max(enrollment_mf.date_value) FROM " +
                    " customer_enrollment ce " +
                    " LEFT JOIN ( " +
                    "   SELECT cemfm.customer_enrollment_id, mfv.date_value FROM customer_enrollment ce " +
                    "   INNER JOIN customer_enrollment_meta_field_map cemfm ON cemfm.customer_enrollment_id = ce.id " +
                    "   INNER JOIN meta_field_value mfv ON mfv.id = cemfm.meta_field_value_id " +
                    "   INNER JOIN meta_field_name mfn ON mfn.id = mfv.meta_field_name_id " +
                    "   WHERE mfn.name = '"+ FileConstants.ACTUAL_START_DATE+"') enrollment_mf ON enrollment_mf.customer_enrollment_id = ce.id " +
                    " LEFT JOIN ( " +
                    "   SELECT cemfm.customer_enrollment_id, mfv.string_value FROM customer_enrollment ce " +
                    "   INNER JOIN customer_enrollment_meta_field_map cemfm ON cemfm.customer_enrollment_id = ce.id " +
                    "   INNER JOIN meta_field_value mfv ON mfv.id = cemfm.meta_field_value_id " +
                    "   INNER JOIN meta_field_name mfn ON mfn.id = mfv.meta_field_name_id " +
                    "   WHERE mfn.name = '"+FileConstants.UTILITY_CUST_ACCT_NR+"') account_number_mf ON account_number_mf.customer_enrollment_id = ce.id " +
                    " WHERE ce.deleted = 0 " +
                    " AND ce.entity_id = :entityId " +
                    " AND account_number_mf.string_value = :accountNumber " +
                    " AND ce.enrollment_status IN (:notRejectedStatuses)";

    public Long countByAccountType(Integer accountType){
        Criteria criteria = getSession().createCriteria(CustomerEnrollmentDTO.class)
                .createAlias("accountType", "at")
                .add(Restrictions.eq("at.id", accountType));
        criteria.setProjection(Projections.rowCount());
        return (Long) criteria.uniqueResult();
    }

    public Long countChildCompanies(Integer enrollmentId) {
        Criteria criteria = getSession().createCriteria(CustomerEnrollmentDTO.class)
                .createAlias("parentEnrollment", "pe")
                .add(Restrictions.eq("pe.id", enrollmentId))
                .add(Restrictions.isNull("user"));
        criteria.setProjection(Projections.rowCount());
        return (Long) criteria.uniqueResult();
    }

    /* This method is return the enrollment which in the validated status*/
    public Long countEnrollmentByAccountNumber(Integer entityId, String metaFieldName, String accountNumber, CustomerEnrollmentStatus status){
        Criteria criteria = getSession().createCriteria(CustomerEnrollmentDTO.class);
        criteria.createAlias("company", "company");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("metaFields", "metaFieldValue");
        criteria.createAlias("metaFieldValue.field", "metaField");
        criteria.add(Restrictions.sqlRestriction("string_value =  ?", accountNumber, StandardBasicTypes.STRING));
        // find those enrollment which is not enrolled
        criteria.add(Restrictions.isNull("user"));
        criteria.add(Restrictions.eq("metaField.name", metaFieldName));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.eq("status", status));
        criteria.setProjection(Projections.rowCount());
        return  (Long)criteria.uniqueResult();
    }


    public Date findCustomerEnrollmentDate(Integer entityId, String accountNumber) {
        Query sqlQuery = getSession().createSQLQuery(FIND_CUSTOMER_ENROLLMENT_DATE);
        sqlQuery.setParameter("entityId", entityId);
        sqlQuery.setParameter("accountNumber", accountNumber);
        sqlQuery.setParameterList("notRejectedStatuses", new String[] {CustomerEnrollmentStatus.PENDING.name(), CustomerEnrollmentStatus.VALIDATED.name(), CustomerEnrollmentStatus.ENROLLED.name()});

        return (Date) sqlQuery.uniqueResult();
    }

    public List<UserDTO> findEnrolledCustomers(Integer entityId) {
        List<UserDTO> userList = new ArrayList<>();
        Criteria criteria = getSession().createCriteria(CustomerEnrollmentDTO.class);
        criteria.createAlias("company", "company");
        criteria.add(Restrictions.in("company.id", new CompanyDAS().findAllCurrentAndChildEntities(entityId)));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.eq("status", CustomerEnrollmentStatus.ENROLLED));
        List<CustomerEnrollmentDTO> enrollmentList = criteria.list();
        for (CustomerEnrollmentDTO enrollmentDTO : enrollmentList) {
            userList.add(enrollmentDTO.getUser());
        }

        return  userList;
    }

    public CustomerEnrollmentDTO findById(Integer customerEnrollmentId) {
        Criteria criteria = getSession().createCriteria(CustomerEnrollmentDTO.class);
        criteria.add(Restrictions.eq("id", customerEnrollmentId));
        criteria.add(Restrictions.eq("deleted", 0));
        return  (CustomerEnrollmentDTO)criteria.uniqueResult();
    }

    /**
     * This method used for finding the enrollment id's which are not enrolled ByUsing ScrollableResult.
     *
     * @param entityId .
     * @return List<Integer> enrollment id's.
     */
    public List<Integer> findIdsByEntity(Integer entityId) {
        if (entityId == null) return null;
        Criteria criteria = getSession().createCriteria(CustomerEnrollmentDTO.class)
                .setLockMode(LockMode.NONE)
                .createAlias("company", "company")
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.or(Restrictions.eq("status", CustomerEnrollmentStatus.VALIDATED), Restrictions.eq("status", CustomerEnrollmentStatus.REJECTED)))
                .setProjection(Projections.id())
                .addOrder(Order.asc("id"))
                .setComment("findIdsByEntity " + entityId);

        ScrollableResults scrollableResults = criteria.scroll();
        List<Integer> enrollmentIds = new ArrayList<Integer>();
        if (scrollableResults != null) {
            try {
                while (scrollableResults.next()) {
                    enrollmentIds.add(scrollableResults.getInteger(0));
                }
            } finally {
                scrollableResults.close();
            }
        }
        Collections.sort(enrollmentIds);
        return enrollmentIds;
    }

}
