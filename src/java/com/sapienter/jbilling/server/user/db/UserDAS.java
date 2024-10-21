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
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.DateType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.StringType;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class UserDAS extends AbstractDAS<UserDTO> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UserDAS.class));

     private static final String findInStatusSQL =
         "SELECT a " +
         "  FROM UserDTO a " +
         " WHERE a.userStatus.id = :status " +
         "   AND a.company.id = :entity " +
         "   AND a.deleted = 0" ;

     private static final String findNotInStatusSQL =
         "SELECT a " +
         "  FROM UserDTO a " +
         " WHERE a.userStatus.id <> :status " +
         "   AND a.company.id = :entity " +
         "   AND a.deleted = 0";

     private static final String findAgeingSQL =
         "SELECT a " +
         "  FROM UserDTO a " +
         " WHERE a.userStatus.id > " + UserDTOEx.STATUS_ACTIVE +
         "   AND a.customer.excludeAging = 0 " +
         "   AND a.company.id = :entity " +
         "   AND a.deleted = 0";

     private static final String CURRENCY_USAGE_FOR_ENTITY_SQL =
             "SELECT count(*) " +
             "  FROM UserDTO a " +
             " WHERE a.currency.id = :currency " +
             "	  AND a.company.id = :entity "+
             "   AND a.deleted = 0";
     
     private static final String FIND_CHILD_LIST_SQL =
    	        "SELECT u.id " +
    	        "FROM UserDTO u " +
    	        "WHERE u.deleted=0 and u.customer.parent.baseUser.id = :parentId";

    private static final String FIND_CHILD_LIST_DTO_SQL =
            "SELECT u " +
                    "FROM UserDTO u " +
                    "WHERE u.deleted=0 and u.customer.parent.baseUser.id = :parentId";

    private static final String FIND_USER_BY_METAFIELD_VALUE =
            "SELECT user_id " +
            "FROM customer " +
            "WHERE id = (" +
            "SELECT customer_id " +
            "FROM customer_meta_field_map cmf, " +
                 "meta_field_value mfv, " +
                 "meta_field_name mfn " +
            "WHERE cmf.meta_field_value_id = mfv.id " +
            "AND mfv.meta_field_name_id = mfn.id " +
            "AND mfn.name = :metaFieldName " +
            "AND mfv.string_value = :metaFieldValue " +
            "AND mfn.entity_id = :entityId)";

     private static final String FIND_USER_IDS_BY_NEXT_PAYMENT_DATE_SQL =
             "SELECT u.id " +
             "  FROM base_user u " +
             "  JOIN payment_information pi ON pi.user_id = u.id" +
             "  JOIN payment_information_meta_fields_map pimm1 " +
             "    ON (pimm1.payment_information_id = pi.id " +
                     "AND pimm1.meta_field_value_id IN (" +
                     "SELECT mfv.id" +
                     "  FROM meta_field_value mfv" +
                     "  JOIN meta_field_name mfn ON mfn.id = mfv.meta_field_name_id" +
                     " WHERE mfn.name = :mfn_name" +
                     "   AND mfv.date_value = :mfv_date_value))" +
             " WHERE u.entity_id = :entityId" +
             "   AND u.deleted = 0 " +
             "   AND u.status_id = 1";

    private static final String FIND_USER_IDS_BY_ACTION_DATE_SQL =
            "SELECT id" +
            "  FROM base_user" +
            " WHERE id IN (SELECT user_id" +
            "                FROM customer AS cust" +
            "                JOIN customer_meta_field_map cmm ON cust.id = cmm.customer_id" +
            "                JOIN meta_field_value mfv ON cmm.meta_field_value_id = mfv.id" +
            "               WHERE mfv.meta_field_name_id = :meta_field_name_id" +
            "                 AND mfv.date_value = :action_date)" +
            "  AND base_user.entity_id = :entity_id" +
            "  AND base_user.deleted = 0";

    private static final String FIND_USER_ID_BY_CONTACT_DETAILS =
              "    SELECT c.user_id "
            + "      FROM meta_field_value mfv "
            + "INNER JOIN meta_field_name mfn ON mfn.id = mfv.meta_field_name_id AND mfv.string_value = :postalCode AND mfn.name = 'Postal Code' "
            + "INNER JOIN customer_account_info_type_timeline cai ON cai.meta_field_value_id = mfv.id "
            + "INNER JOIN customer c ON c.id = cai.customer_id "
            + "INNER JOIN meta_field_group mfg ON cai.account_info_type_id = mfg.id "
            + "     WHERE mfg.account_type_id = c.account_type_id AND mfg.name = 'Contact Information' "
            + "       AND cai.effective_date =(SELECT MAX(effective_date) "
            + "                                  FROM customer_account_info_type_timeline "
            + "                                 WHERE customer_id = (SELECT id "
            + "                                                        FROM customer "
            + "                                                       WHERE user_id = c.user_id) "
            + "                                   AND account_info_type_id = mfg.id) "
            + "       AND c.user_id IN (SELECT c.user_id "
            + "                           FROM meta_field_value mfv "
            + "                     INNER JOIN meta_field_name mfn ON mfn.id = mfv.meta_field_name_id "
            + "                                                   AND mfv.string_value = :customerName "
            + "                                                   AND mfn.name = 'Customer Name' "
            + "                     INNER JOIN customer_account_info_type_timeline cai ON cai.meta_field_value_id = mfv.id "
            + "                     INNER JOIN customer c ON c.id = cai.customer_id "
            + "                     INNER JOIN meta_field_group mfg ON cai.account_info_type_id = mfg.id "
            + "                          WHERE mfg.account_type_id = c.account_type_id AND mfg.name = 'Contact Information' "
            + "                            AND cai.effective_date =(SELECT MAX(effective_date) "
            + "                                                       FROM customer_account_info_type_timeline "
            + "                                                      WHERE customer_id = (SELECT id "
            + "                                                                             FROM customer "
            + "                                                                            WHERE user_id = c.user_id) "
            + "                                                        AND account_info_type_id = mfg.id) "
            + "                            AND c.user_id in (SELECT distinct(c.user_id) "
            + "                                                FROM meta_field_value mfv "
            + "                                          INNER JOIN meta_field_name mfn ON mfn.id = mfv.meta_field_name_id "
            + "                                                                        AND REPLACE(mfv.string_value, ' ', '') = :phoneNumber "
            + "                                                                        AND mfn.name in ('Phone Number 1', 'Phone Number 2') "
            + "                                          INNER JOIN customer_account_info_type_timeline cai ON cai.meta_field_value_id = mfv.id "
            + "                                          INNER JOIN customer c ON c.id = cai.customer_id "
            + "                                          INNER JOIN meta_field_group mfg ON cai.account_info_type_id = mfg.id "
            + "                                               WHERE mfg.account_type_id = c.account_type_id AND mfg.name = 'Contact Information' "
            + "                                                 AND cai.effective_date =(SELECT MAX(effective_date) "
            + "                                                                            FROM customer_account_info_type_timeline "
            + "                                                                           WHERE customer_id = (SELECT id "
            + "                                                                                                  FROM customer "
            + "                                                                                                 WHERE user_id = c.user_id) "
            + "                                                                             AND account_info_type_id = mfg.id)) "
            + "                       ORDER BY mfn.name) "
            + "  ORDER BY mfn.name";

    private static final String UPDATE_USER_NAME_AND_STATUS_BY_ID = "UPDATE base_user bu " +
                                                                    "SET user_name = :newUserName, status_id = :statusId " +
                                                                    "WHERE bu.id = :userId " +
                                                                    "AND bu.user_name = :userName";

    private static final String DELETE_USER_PERMISSION = " DELETE FROM permission_user pmu " +
                                                         " WHERE pmu.user_id = :userId " ;

    private static final String FIND_NAME_BY_ID = "SELECT user_name FROM base_user WHERE id=:userId";
    private static final String FIND_CURRENCY_BY_ID = "SELECT currency_id FROM base_user WHERE id=:userId";

	public List<Integer> findChildList(Integer userId) {
		Query query = getSession().createQuery(FIND_CHILD_LIST_SQL);
		query.setParameter("parentId", userId);
		
		return query.list();
	}

    public List<UserDTO> findChildDTOList(Integer userId) {
        Query query = getSession().createQuery(FIND_CHILD_LIST_DTO_SQL);
        query.setParameter("parentId", userId);

        return query.list();
    }

     public Long findUserCountByCurrencyAndEntity(Integer currencyId, Integer entityId){
         Query query = getSession().createQuery(CURRENCY_USAGE_FOR_ENTITY_SQL);
         query.setParameter("currency", currencyId);
         query.setParameter("entity", entityId);

         return (Long) query.uniqueResult();
     }

    private static final String findCurrencySQL =
          "SELECT count(*) " +
          "  FROM UserDTO a " +
          " WHERE a.currency.id = :currency "+
          "   AND a.deleted = 0";

    public UserDTO findRoot(String username) {
        if (username == null || username.length() == 0) {
            LOG.error("can not find an empty root: " + username);
            return null;
        }
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
            .add(Restrictions.eq("userName", username))
            .add(Restrictions.eq("deleted", 0))
            .createAlias("roles", "r")
                .add(Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_ROOT));

        criteria.setCacheable(true); // it will be called over an over again

        return (UserDTO) criteria.uniqueResult();
    }

    public UserDTO findWebServicesRoot(String username) {
        if (username == null || username.length() == 0) {
            LOG.error("can not find an empty root: " + username);
            return null;
        }
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
            .add(Restrictions.eq("userName", username))
            .add(Restrictions.eq("deleted", 0))
            .createAlias("roles", "r")
                .add(Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_ROOT))
            .createAlias("permissions", "p")
                .add(Restrictions.eq("p.permission.id", 120));

        criteria.setCacheable(true); // it will be called over an over again

        return (UserDTO) criteria.uniqueResult();
    }

    public UserDTO findByUserId(Integer userId, Integer entityId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("id", userId))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("company", "e")
                .add(Restrictions.eq("e.id", entityId))
                .add(Restrictions.eq("e.deleted", 0));

        return (UserDTO) criteria.uniqueResult();
    }

    public UserDTO findByUserName(String username, Integer entityId) {
        return findByUserName(username, entityId, false);
    }

    public UserDTO findByUserName(String username) {
        return findByUserName(username, false);
    }

    public UserDTO findByUserName(String username, Integer entityId, boolean findDeleted) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("userName", username).ignoreCase())
                .createAlias("company", "e")
                    .add(Restrictions.eq("e.id", entityId))
                    .add(Restrictions.eq("e.deleted", 0));

        if(!findDeleted) {
            criteria.add(Restrictions.eq("deleted", 0));
        }

        return (UserDTO) criteria.uniqueResult();
    }

    public UserDTO findByUserName(String username, boolean findDeleted) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("userName", username).ignoreCase())
                .createAlias("company", "e")
                    .add(Restrictions.eq("e.deleted", 0));

        if(!findDeleted) {
            criteria.add(Restrictions.eq("deleted", 0));
        }

        return (UserDTO) criteria.uniqueResult();
    }

    public List<UserDTO> findByEmail(String email, Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("deleted", 0)) 
                .createAlias("company", "e")
                .add(Restrictions.eq("e.id", entityId))
                .createAlias("contact", "c")
                .add(Restrictions.eq("c.email", email).ignoreCase());

        return criteria.list();
    }

    public List<UserDTO> findInStatus(Integer entityId, Integer statusId) {
        Query query = getSession().createQuery(findInStatusSQL);
        query.setParameter("entity", entityId);
        query.setParameter("status", statusId);
        return query.list();
    }

    public List<UserDTO> findNotInStatus(Integer entityId, Integer statusId) {
        Query query = getSession().createQuery(findNotInStatusSQL);
        query.setParameter("entity", entityId);
        query.setParameter("status", statusId);
        return query.list();
    }

    public List<UserDTO> findAgeing(Integer entityId) {
        Query query = getSession().createQuery(findAgeingSQL);
        query.setParameter("entity", entityId);
        return query.list();
    }

    public boolean exists(Integer userId, Integer entityId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.idEq(userId))
                .createAlias("company", "company")
                .add(Restrictions.eq("company.id", entityId))
                .setProjection(Projections.rowCount());

        return (criteria.uniqueResult() != null && ((Long) criteria.uniqueResult()) > 0);
    }

    public Long findUserCountByCurrency(Integer currencyId){
        Query query = getSession().createQuery(findCurrencySQL);
        query.setParameter("currency", currencyId);
        return (Long) query.uniqueResult();
    }

    public List<UserDTO> findAdminUsers(Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.isNotNull("password"))
                .createAlias("roles", "r")
                .add(Restrictions.or(
                        Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_ROOT),
                        Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_SYSTEM_ADMIN)));

        return criteria.list();
    }

    public List<UserDTO> findAllCustomers(Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("roles", "r")
                .add(Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_CUSTOMER));

        return criteria.list();
    }

    public List<Integer> findAdminUserIds(Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("roles", "r")
                .add(Restrictions.or(
                        Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_ROOT),
                        Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_SYSTEM_ADMIN)));
        criteria.setProjection(Projections.id());

        return criteria.list();
    }

    public List<UserDTO> findRootUser(Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("roles", "r")
                .add(Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_ROOT));
        return criteria.list();
    }

    public List<Integer> findClerkUserIds(Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("roles", "r")
                .add(Restrictions.eq("r.roleTypeId", CommonConstants.TYPE_CLERK));
        criteria.setProjection(Projections.id());

        return (List<Integer>) criteria.list();
    }


    @SuppressWarnings("unchecked")
    public ScrollableResults findUserIdsWithUnpaidInvoicesForAgeing(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(UserDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("customer", "customer", CriteriaSpecification.INNER_JOIN)
                .add(Restrictions.eq("customer.excludeAging", 0))
                .createAlias("invoices", "invoice", CriteriaSpecification.INNER_JOIN)  // only with invoices
                .add(Restrictions.eq("invoice.isReview", 0))
                .add(Restrictions.eq("invoice.deleted", 0))
                .createAlias("invoice.invoiceStatus", "status", CriteriaSpecification.INNER_JOIN)
                .add(Restrictions.ne("status.id", Constants.INVOICE_STATUS_PAID))
                .setProjection(Projections.distinct(Projections.property("id")));
        if (entityId != null) {
            query.add(Restrictions.eq("company.id", entityId));
        }
        // added order to get all ids in ascending order
        query.addOrder(Order.asc("id"));

        Criteria criteria = query.getExecutableCriteria(getSession());
        return criteria.scroll();
    }

    public List<UserDTO> findByMetaFieldValueIds(Integer entityId, List<Integer> valueIds){
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.metaFields", "values");
        criteria.add(Restrictions.in("values.id", valueIds));
        return criteria.list();
    }
    
    public List<UserDTO> findByAitMetaFieldValueIds(Integer entityId, List<Integer> valueIds){
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.customerAccountInfoTypeMetaFields", "cmfs");
        criteria.createAlias("cmfs.metaFieldValue", "value");
        criteria.add(Restrictions.in("value.id", valueIds));
        return criteria.list();
    }

    public UserDTO findByMetaFieldNameAndValue(Integer entityId, String metaFieldName, String metaFieldValue){
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.customerAccountInfoTypeMetaFields", "cmfs");
        criteria.createAlias("cmfs.metaFieldValue", "value");
        criteria.createAlias("value.field", "metaField");
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.sqlRestriction("string_value =  ?", metaFieldValue, StringType.INSTANCE));
        criteria.add(Restrictions.eq("metaField.name", metaFieldName));
        return (UserDTO) criteria.uniqueResult();
    }

    /**
     * Returns the entity ID for the user. Executes really
     * fast and does not use any joins.
     */
    public Integer getUserCompanyId(Integer userId){
        SQLQuery query = getSession().createSQLQuery("select entity_id from base_user where id = :userId");
        query.setParameter("userId", userId);
        return (Integer) query.uniqueResult();
    }
    
    public boolean hasSubscriptionProduct(Integer userId) {
    	DetachedCriteria dc = DetachedCriteria.forClass(CustomerDTO.class).
    							createAlias("parent", "parent").
    							createAlias("parent.baseUser", "parentUser").
    			 				add(Restrictions.eq("parentUser.id", userId)).
    			 				createAlias("baseUser", "baseUser").
    			 				setProjection(Projections.property("baseUser.id"));
    	
 		Criteria c = getSession().createCriteria(OrderDTO.class).
 				     			add(Restrictions.eq("deleted", 0)).
 				     			createAlias("baseUserByUserId","user").
 				     			add(Property.forName("user.id").in(dc)).
 				     			
 				 				createAlias("lines","lines").
 				 				createAlias("lines.item", "item").
 				 				createAlias("item.itemTypes", "types").
 				 				add(Restrictions.eq("types.orderLineTypeId", Constants.ORDER_LINE_TYPE_SUBSCRIPTION)).
 				 				setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
 		
 		return c.list().size() > 0;
    }
    
    public boolean isSubscriptionAccount(Integer userId) {
        Criteria c = getSession().createCriteria(OrderDTO.class).
                add(Restrictions.eq("deleted", 0)).
                createAlias("baseUserByUserId", "user").
                add(Restrictions.eq("user.id", userId)).

                createAlias("lines", "lines").
                createAlias("lines.item", "item").
                createAlias("item.itemTypes", "types").
                add(Restrictions.eq("types.orderLineTypeId", Constants.ORDER_LINE_TYPE_SUBSCRIPTION)).
                setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        return c.list().size() > 0;
    }
    
    public void saveUserWithNewPasswordScheme(Integer userId, String newPassword, Integer newScheme, Integer entityId){
    	String hql = "Update UserDTO u set password = :password, encryptionScheme = :newScheme where id = :id and company.id = :entityId";
    	Query query = getSession().createQuery(hql).setString("password", newPassword).setInteger("newScheme", newScheme)
    			.setInteger("id", userId).setInteger("entityId", entityId);
    	query.executeUpdate();
    }

    public List<Integer> findUsersInActiveSince(Date activityThresholdDate, Integer entityId) {
        if (null == activityThresholdDate) {
            LOG.error("can not find users on empty date %s for entity id %s",activityThresholdDate, entityId );
            return null;
        }
        // Get a list of users that have not logged in since before the provided date
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.or(Restrictions.and(Restrictions.isNotNull("lastLogin"), Restrictions.le("lastLogin", activityThresholdDate)), Restrictions.and(Restrictions.isNull("lastLogin"), Restrictions.le("createDatetime", activityThresholdDate))))
                .add(Restrictions.eq("entity_id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.isNull("accountDisabledDate"))
                .setProjection(Projections.id());

        return criteria.list();
    }

    public List<UserDTO> getUsersNotInvoiced(Integer entityId, Date startDate, Date endDate, Integer max, Integer offset) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("	distinct u.* ");
        sb.append("from ");
        sb.append("	base_user u ");
        sb.append("	join entity e on u.entity_id = e.id ");
        sb.append("	join entity parent_entity  on e.parent_id = parent_entity.id ");
        sb.append("	join customer c on c.user_id = u.id ");
        sb.append("	left join customer_meta_field_map termination_cmfm on c.id = termination_cmfm.customer_id ");
        sb.append("	left join meta_field_value termination_mfv on termination_cmfm.meta_field_value_id = termination_mfv.id ");
        sb.append("	left join ( ");
        sb.append("		select mfv.string_value as value, c.id as customer_id from customer c ");
        sb.append("			join customer_meta_field_map cmfm on c.id = cmfm.customer_id ");
        sb.append("			join meta_field_value mfv on cmfm.meta_field_value_id = mfv.id ");
        sb.append("			join meta_field_name mfn on mfv.meta_field_name_id = mfn.id ");
        sb.append("		where mfn.name = 'Termination' ");
        sb.append("	) termination_mf on termination_mf.customer_id = c.id ");
        sb.append("where true ");
        sb.append("	and (u.entity_id = :entityId or parent_entity.id = :entityId) ");
        sb.append("	and e.parent_id is not null ");
        sb.append("	and u.deleted = 0 ");
        sb.append("	and u.id not in( ");
        sb.append("		select ");
        // Changes start - The order is considered invoiced if the order status is FINISHED
        // For the order of month of January , if getting invoiced in march , will be considered as invoice for the month of January rather March.
        // This is because the order was for the month of January.
        sb.append("			distinct po.user_id from purchase_order po where true and po.status_id in( ");
        sb.append("		select os.id from order_status os where true and os.order_status_flag = "+ OrderStatusFlag.FINISHED.ordinal() +" and os.entity_id = :entityId) ");
        sb.append("			and  ((po.active_until between :startDate and :endDate ) OR  (po.active_since between :startDate and :endDate)) ");
        sb.append("		    and po.deleted = 0 ");
        sb.append("	) ");
        sb.append("	and (lower(termination_mf.value) <> 'dropped' or termination_mf.value is null) ");

        // Exclude those customers which are subscribed to Day head rate change plans
        sb.append("	and u.id not in( ");
        sb.append("     select bu.id from base_user bu join customer c on bu.id = c.user_id ");
        sb.append("     join customer_account_info_type_timeline caitt on c.id = caitt.customer_id ");
        sb.append("     join meta_field_value mfv on caitt.meta_field_value_id = mfv.id ");
        sb.append("     join meta_field_name mfn on mfv.meta_field_name_id = mfn.id ");
        sb.append("     where mfn.name = 'PLAN' AND mfv.string_value in (select internal_number from item i ");
        sb.append("         join plan p on i.id = p.item_id ");
        sb.append("         join plan_meta_field_map pmfm on p.id = pmfm.plan_id ");
        sb.append("         join meta_field_value mfv on pmfm.meta_field_value_id = mfv.id ");
        sb.append("         join meta_field_name mfn on mfv.meta_field_name_id = mfn.id ");
        sb.append("         where mfn.name = 'Send rate change daily' AND mfv.boolean_value = TRUE AND mfn.entity_id = :entityId)");
        sb.append("	) ");

        sb.append("order by u.user_name ");
        sb.append("limit :max ");
        sb.append("offset :offset ");

        Query query = getSessionFactory().getCurrentSession().createSQLQuery(sb.toString())
                .addEntity(UserDTO.class)
                .setParameter("entityId", entityId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("max", max)
                .setParameter("offset", offset);

        return query.list();
    }

    public Long getUsersNotInvoicedCount(Integer entityId, Date startDate, Date endDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("	count(*) as total ");
        sb.append("from ");
        sb.append("	base_user u ");
        sb.append("	join entity e on u.entity_id = e.id ");
        sb.append("	join entity parent_entity  on e.parent_id = parent_entity.id ");
        sb.append("	join customer c on c.user_id = u.id ");
        sb.append("	left join customer_meta_field_map termination_cmfm on c.id = termination_cmfm.customer_id ");
        sb.append("	left join meta_field_value termination_mfv on termination_cmfm.meta_field_value_id = termination_mfv.id ");
        sb.append("	left join ( ");
        sb.append("		select mfv.string_value as value, c.id as customer_id from customer c ");
        sb.append("			join customer_meta_field_map cmfm on c.id = cmfm.customer_id ");
        sb.append("			join meta_field_value mfv on cmfm.meta_field_value_id = mfv.id ");
        sb.append("			join meta_field_name mfn on mfv.meta_field_name_id = mfn.id ");
        sb.append("		where mfn.name = 'Termination' ");
        sb.append("	) termination_mf on termination_mf.customer_id = c.id ");
        sb.append("where true ");
        sb.append("	and (u.entity_id = :entityId or parent_entity.id = :entityId) ");
        sb.append("	and e.parent_id is not null ");
        sb.append("	and u.deleted = 0 ");
        sb.append("	and u.id not in( ");
        sb.append("		select ");
        sb.append("			distinct i.user_id ");
        sb.append("		from ");
        sb.append("			invoice i ");
        sb.append("		where true ");
        sb.append("			and i.deleted = 0 ");
        sb.append("         and i.is_review = 0 ");
        sb.append("			and i.create_datetime between :startDate and :endDate ");
        sb.append("	) ");
        sb.append("	and (lower(termination_mf.value) <> 'dropped' or termination_mf.value is null) ");

        // Exclude those customers which are subscribed to day head rate change plans
        sb.append("	and u.id not in( ");
        sb.append("     select bu.id from base_user bu join customer c on bu.id = c.user_id ");
        sb.append("     join customer_account_info_type_timeline caitt on c.id = caitt.customer_id ");
        sb.append("     join meta_field_value mfv on caitt.meta_field_value_id = mfv.id ");
        sb.append("     join meta_field_name mfn on mfv.meta_field_name_id = mfn.id ");
        sb.append("     where mfn.name = 'PLAN' AND mfv.string_value in (select internal_number from item i ");
        sb.append("         join plan p on i.id = p.item_id ");
        sb.append("         join plan_meta_field_map pmfm on p.id = pmfm.plan_id ");
        sb.append("         join meta_field_value mfv on pmfm.meta_field_value_id = mfv.id ");
        sb.append("         join meta_field_name mfn on mfv.meta_field_name_id = mfn.id ");
        sb.append("         where mfn.name = 'Send rate change daily' AND mfv.boolean_value = TRUE AND mfn.entity_id = :entityId)");
        sb.append("	) ");

        Query query = getSessionFactory().getCurrentSession().createSQLQuery(sb.toString())
                .addScalar("total", StandardBasicTypes.LONG)
                .setParameter("entityId", entityId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate);

        return (Long) query.uniqueResult();
    }

    public List<UserDTO> findByMetaFieldNameAndValue(String metaFieldName, Date date, Integer entityId){
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.metaFields", "value");
        criteria.createAlias("value.field", "metaField");
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.sqlRestriction("date_value =  ?", date, DateType.INSTANCE));
        criteria.add(Restrictions.eq("metaField.name", metaFieldName));
        return criteria.list();
    }

    public List<UserDTO> findUsers(Conjunction conjunction, Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.metaFields", "value");
        criteria.createAlias("value.field", "metaField");
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(conjunction);
        return criteria.list();
    }

    public UserDTO findSingleByMetaFieldNameAndValue(String metaFieldName, String metaFieldValue, Integer companyId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.metaFields", "value");
        criteria.createAlias("value.field", "metaField");
        criteria.createAlias("company", "company");
        criteria.add(Restrictions.eq("company.id", companyId));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.sqlRestriction("string_value =  ?", metaFieldValue, StringType.INSTANCE));
        criteria.add(Restrictions.eq("metaField.name", metaFieldName));
        return (UserDTO)criteria.uniqueResult();
    }

    public List<Integer> findByMetaFieldNameAndValues(String metaFieldName, List<String> values, Integer entityId){
        String valuesConCat = "";
        for(String value : values){
            valuesConCat = valuesConCat.toString().isEmpty()?valuesConCat+"'"+value+"'":valuesConCat+","+"'"+value+"'";
        }
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.customerAccountInfoTypeMetaFields", "cmfs");
        criteria.createAlias("cmfs.metaFieldValue", "value");
        criteria.createAlias("value.field", "metaField");
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.sqlRestriction("string_value in (" + valuesConCat + ")"));
        criteria.add(Restrictions.eq("metaField.name", metaFieldName));
        criteria.setProjection(Projections.distinct(Projections.property("id")));
        return criteria.list();
    }

    /*
    * NGES : Find drop customers for the account number.
    * @params enityId
    * @params metaFieldName user account metafield name
    * @params metaFieldValue user account metafield value
    *
    * @return
    * */
    public List<Integer> findDropCustomers(Integer entityId, String metaFieldName, String metaFieldValue){
        //this query returning the all drop customer customer id for a utility account number
        StringBuilder sb = new StringBuilder();
        sb.append("select bu.id from base_user bu ");
        sb.append("inner join customer customer on bu.id=customer.user_id ");
        sb.append("inner join customer_account_info_type_timeline caitt on customer.id=caitt.customer_id ");
        sb.append("left outer join meta_field_group mfg on caitt.account_info_type_id=mfg.id ");
        sb.append("inner join meta_field_value aitValue on caitt.meta_field_value_id=aitValue.id ");
        sb.append("inner join meta_field_name aitMetaFieldName on aitValue.meta_field_name_id=aitMetaFieldName.id ");
        sb.append("inner join customer_meta_field_map cmfm on customer.id=cmfm.customer_id ");
        sb.append("inner join meta_field_value customerMetaFieldValue on cmfm.meta_field_value_id=customerMetaFieldValue.id ");
        sb.append("inner join meta_field_name customerMetaFieldName on customerMetaFieldValue.meta_field_name_id=customerMetaFieldName.id ");
        sb.append("where ");
        sb.append("bu.entity_id= :entityId ");
        sb.append("and bu.deleted=0 ");
        sb.append("and ( ");
        sb.append("      ( ");
        sb.append("      aitValue.string_value =  :metaFieldValue ");
        sb.append("      and aitMetaFieldName.name=:metaFieldName ");
        sb.append("      ) ");
        sb.append("      and ( ");
        sb.append("      customerMetaFieldValue.string_value in  ( ");
        sb.append("       'Dropped','Esco Rejected' ");
        sb.append("       ) ");
        sb.append("       and customerMetaFieldName.name='Termination' ");
        sb.append("       ) ");
        sb.append("    ) order by bu.create_datetime desc ");
        Query query = getSessionFactory().getCurrentSession().createSQLQuery(sb.toString())
                .setParameter("entityId", entityId)
                .setParameter("metaFieldName", metaFieldName)
                .setParameter("metaFieldValue", metaFieldValue);

        return query.list();
    }

    public UserDTO findUserByAccountNumber(Integer entityId, String metaFieldName, String metaFieldValue){
        return findUserByAccountNumber( entityId,  metaFieldName,  metaFieldValue, null);
    }
    // NGES : This method is NGES specific and it is returning non drop User
    public UserDTO findUserByAccountNumber(Integer entityId, String metaFieldName, String metaFieldValue, String commodityValue){
        List<Integer> dropUsers=findDropCustomers(entityId,metaFieldName,metaFieldValue);
        Criteria criteria = getSession().createCriteria(UserDTO.class, "user");
        criteria.add(Restrictions.eq("company.id", entityId));
        criteria.createAlias("user.customer", "customer");
        criteria.createAlias("customer.customerAccountInfoTypeMetaFields", "cmfs");
        criteria.createAlias("cmfs.metaFieldValue", "value");
        criteria.createAlias("value.field", "metaField");
        if(dropUsers.size()>0){
            criteria.add(Restrictions.not(
                    Restrictions.in("user.id", dropUsers)
            ));
        }
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.sqlRestriction("string_value =  ?", metaFieldValue, StandardBasicTypes.STRING));
        criteria.add(Restrictions.eq("metaField.name", metaFieldName));
        List<UserDTO> userDTOs = criteria.list();
        List<UserDTO> filteredUsers=new ArrayList<>();
        if (StringUtils.isNotEmpty(commodityValue)) {
            for (UserDTO user : userDTOs) {
                if (user.getCustomer().getCustomerAccountInfoTypeMetaFields().stream().filter(customerAccountInfoTypeMetaField -> customerAccountInfoTypeMetaField.getMetaFieldValue().getField().getName().equals(FileConstants.COMMODITY) && customerAccountInfoTypeMetaField.getMetaFieldValue().getValue().equals(commodityValue)).findAny().isPresent()) {
                    filteredUsers.add(user);
                }
            }
        }else{
            filteredUsers=userDTOs;
        }
        if (filteredUsers.size() > 1) {
            LOG.info("More than one customer found for " + metaFieldName + ":" + metaFieldValue + (commodityValue!=null?(" and commodity:" + commodityValue):""));
            throw new SessionInternalError("More than one customer found for " + metaFieldName + ":" + metaFieldValue + (commodityValue!=null?(" and commodity:" + commodityValue):""));
        }

        if(filteredUsers.size()==1){
            return filteredUsers.get(0);
        }
        else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findUserIdsWithEntityId(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(UserDTO.class)
                .setLockMode(LockMode.NONE)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("customer", "customer")
                .setProjection(Projections.distinct(Projections.property("id")));
        if (entityId != null) {
            query.add(Restrictions.eq("company.id", entityId));
        } else {
            return null;
        }
        // added order to get all ids in ascending order
        query.addOrder(Order.asc("id"));

        Criteria criteria = query.getExecutableCriteria(getSession());
        ScrollableResults scrollableResults = criteria.scroll();
        List<Integer> userIds = new ArrayList<Integer>();
        if (scrollableResults != null) {
            try {
                while (scrollableResults.next()) {
                    userIds.add(scrollableResults.getInteger(0));
                }
            } finally {
                scrollableResults.close();
            }
        }
        Collections.sort(userIds);
        return userIds;
    }

    private static final String findEntityIdByUser ="select entity_id from base_user where id=:userId";
    
    public Integer getEntityByUserId(Integer userId) {
    	SQLQuery query  = getSession().createSQLQuery(findEntityIdByUser);
    	query.setParameter("userId", userId);
    	return (Integer) query.uniqueResult();
    	
    }

    public UserDTO findUserByMetaFieldValue(String value, Integer entityId) {
        if(StringUtils.trimToNull(value) == null) {
            return null;
        }

        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .createAlias("metaFields", "metaFieldValue")
                .createAlias("metaFieldValue.field", "metaField")
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.sqlRestriction("string_value =  ?", value, StandardBasicTypes.STRING));

        return (UserDTO) criteria.uniqueResult();
    }

    public UserDTO findByEmailAndUserName(String email,String userName, Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("userName", userName).ignoreCase())
                .createAlias("company", "e")
                .add(Restrictions.eq("e.id", entityId))
                .createAlias("contact", "c")
                .add(Restrictions.eq("c.email", email).ignoreCase());

        return (UserDTO) criteria.uniqueResult();
    }

    public void refresh(UserDTO user) {
        getSession().refresh(user);
    }

    @Deprecated
    public List<Integer> getUserIdsByNextPaymentDate(String metaFieldName, Date nextPaymentDate, Integer entityId) {
        SQLQuery sqlQuery = getSession().createSQLQuery(FIND_USER_IDS_BY_NEXT_PAYMENT_DATE_SQL);
        sqlQuery.setParameter("mfn_name", metaFieldName);
        sqlQuery.setParameter("mfv_date_value", nextPaymentDate, DateType.INSTANCE);
        sqlQuery.setParameter("entityId", entityId);
        return (List<Integer>) sqlQuery.list();
    }

    public Integer findUserByMetaFieldNameAndValue(String name, String value, Integer entityId) {
        SQLQuery sqlQuery = getSession().createSQLQuery(FIND_USER_BY_METAFIELD_VALUE);
        sqlQuery.setParameter("metaFieldName", name);
        sqlQuery.setParameter("metaFieldValue", value);
        sqlQuery.setParameter("entityId", entityId);

        return (Integer) sqlQuery.uniqueResult();
    }

    public ScrollableResults findUserIdsByNextPaymentDate(String metaFieldName, Date nextPaymentDate, Integer entityId) {
        SQLQuery sqlQuery = getSession().createSQLQuery(FIND_USER_IDS_BY_NEXT_PAYMENT_DATE_SQL);
        sqlQuery.setParameter("mfn_name", metaFieldName);
        sqlQuery.setParameter("mfv_date_value", nextPaymentDate, DateType.INSTANCE);
        sqlQuery.setParameter("entityId", entityId);
        return sqlQuery.scroll(ScrollMode.FORWARD_ONLY);
    }

    @Deprecated
    public List<Integer> getUserIdsByMetaFieldNameAndValue(Integer metaFieldId, Date actionDate, Integer entityId){
        SQLQuery sqlQuery= getSession().createSQLQuery(FIND_USER_IDS_BY_ACTION_DATE_SQL);
        sqlQuery.setParameter("meta_field_name_id", metaFieldId);
        sqlQuery.setParameter("action_date", actionDate, DateType.INSTANCE);
        sqlQuery.setParameter("entity_id", entityId);
        return (List<Integer>) sqlQuery.list();
    }

    public ScrollableResults findUserIdsByMetaFieldNameAndValue(Integer metaFieldId, Date actionDate, Integer entityId) {
        SQLQuery sqlQuery= getSession().createSQLQuery(FIND_USER_IDS_BY_ACTION_DATE_SQL);
        sqlQuery.setParameter("meta_field_name_id", metaFieldId);
        sqlQuery.setParameter("action_date", actionDate, DateType.INSTANCE);
        sqlQuery.setParameter("entity_id", entityId);
        return sqlQuery.scroll(ScrollMode.FORWARD_ONLY);
    }

    public Integer getUserIdByCustomerDetails(String customerName, String customerPhonNumber, String customerPostalCode ) {
        SQLQuery query = getSession().createSQLQuery(FIND_USER_ID_BY_CONTACT_DETAILS);
        query.setParameter("postalCode", customerPostalCode);
        query.setParameter("customerName", customerName);
        query.setParameter("phoneNumber", customerPhonNumber);
        return (Integer) query.uniqueResult();
    }

    public List<UserDTO> getAllUsers(Integer entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
            .add(Restrictions.eq("company.id", entityId))
            .createAlias("roles", "r")
            .add(Restrictions.ne("r.roleTypeId", CommonConstants.TYPE_CUSTOMER))
            .addOrder(Order.asc("userName"));
        return criteria.list();
    }

    public void updateUserNameAndStatusById(Integer userId, String userName, Integer statusId){
        Query query = getSession().createSQLQuery(UPDATE_USER_NAME_AND_STATUS_BY_ID);
        query.setParameter("newUserName", String.format("Refunded(%s) %s", userId, userName));
        query.setParameter("statusId", statusId);
        query.setParameter("userId", userId);
        query.setParameter("userName", userName);
        query.executeUpdate();
    }

    public void removeUserPermission(Integer userId){
            Query query = getSession().createSQLQuery(DELETE_USER_PERMISSION);
            query.setParameter("userId", userId);
            query.executeUpdate();
    }

    public String getUserNameById(Integer userId) {
        SQLQuery query  = getSession().createSQLQuery(FIND_NAME_BY_ID);
        query.setParameter("userId", userId);
        return (String) query.uniqueResult();
    }

    public Integer getCurrencyByUserId(Integer userId) {
        SQLQuery query  = getSession().createSQLQuery(FIND_CURRENCY_BY_ID);
        query.setParameter("userId", userId);
        return (Integer) query.uniqueResult();
    }
}
