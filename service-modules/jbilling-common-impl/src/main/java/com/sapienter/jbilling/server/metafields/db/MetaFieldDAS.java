/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.metafields.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.*;
import java.math.BigDecimal;
import java.util.*;

import org.hibernate.ScrollableResults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Brian Cowdery
 * @since 03-Oct-2011
 */
public class MetaFieldDAS extends AbstractDAS<MetaField> {

    public static final FormatLogger LOG = new FormatLogger(Logger.getLogger(MetaFieldDAS.class));

    private static final String findCountByDTypeName =
            "SELECT count(*) " +
                    "  FROM MetaField a " +
                    " WHERE a.dataType = :dataType " +
                    " AND a.name = :name";

    private static final String findAllIdsByDataTypeNameSQL =
            "SELECT id " +
                    "  FROM MetaField a " +
                    " WHERE a.dataType = :dataType " +
                    " AND a.name = :name";

    @SuppressWarnings("unchecked")
    public List<MetaField> getAvailableFields(Integer entityId, EntityType[] entityType, Boolean primary) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.in("entityType", entityType));
        if (null != primary) {
            query.add(Restrictions.eq("primary", primary.booleanValue()));
        }
        query.addOrder(Order.asc("displayOrder"));
        List<MetaField> result = null;
        try {

            result = (List<MetaField>) getHibernateTemplate().findByCriteria(query);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            LOG.error(e);
        }
        return result;
    }

    public boolean isDependencyExist(Integer metaFieldId) {
        String sql = "SELECT count(*)  FROM meta_field_dependency_map where dependent_meta_field_id = ?;";
        Session session = getSession();
        Number dependencies = (Number) session.createSQLQuery(sql)
                .setInteger(0, metaFieldId)
                .uniqueResult();
        return dependencies.longValue() != 0;
    }

    @SuppressWarnings("unchecked")
    public MetaField getFieldByName(Integer entityId, EntityType[] entityType, String name) {
        return getFieldByName(entityId, entityType, name, null);
    }

    @SuppressWarnings("unchecked")
    public MetaField getFieldByName(Integer entityId, EntityType[] entityType, String name, Boolean primary) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.in("entityType", entityType));
        query.add(Restrictions.eq("name", name));

        if (null != primary) {
            query.add(Restrictions.eq("primary", primary.booleanValue()));
        }

        List<MetaField> fields = (List<MetaField>) getHibernateTemplate().findByCriteria(query);
        return (null!= fields && !fields.isEmpty()) ? fields.get(0) : null;
    }

    public MetaField getFieldByNameTypeAndGroup(Integer entityId, EntityType[] entityType, String name, Integer groupId) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.in("entityType", entityType));
        query.add(Restrictions.eq("name", name));
        query.createAlias("metaFieldGroups", "groups", CriteriaSpecification.LEFT_JOIN);
        query.add(Restrictions.eq("groups.id", groupId));
        query.add(Restrictions.eq("groups.entityType", EntityType.ACCOUNT_TYPE));
        query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        List<MetaField> fields = (List<MetaField>) getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }

    public MetaField getFieldByNameTypeAndGroupForCompany(Integer entityId, EntityType[] entityType, String name, Integer groupId) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.in("entityType", entityType));
        query.add(Restrictions.eq("name", name));
        query.createAlias("metaFieldGroups", "groups", CriteriaSpecification.LEFT_JOIN);
        query.add(Restrictions.eq("groups.id", groupId));
        query.add(Restrictions.eq("groups.entityType", EntityType.COMPANY_INFO));
        query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        List<MetaField> fields = (List<MetaField>) getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }

    public MetaField getFieldByNameAndGroup(Integer entityId, String name, Integer groupId) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.eq("name", name));
        query.createAlias("metaFieldGroups", "groups", CriteriaSpecification.LEFT_JOIN);
        query.add(Restrictions.eq("groups.id", groupId));
        query.add(Restrictions.eq("groups.entityType", EntityType.ACCOUNT_TYPE));
        query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        List<MetaField> fields = (List<MetaField>) getHibernateTemplate().findByCriteria(query);
        return !fields.isEmpty() ? fields.get(0) : null;
    }


    public void deleteMetaFieldValuesForEntity(EntityType entityType, int metaFieldId) {
        Session session = getSession();
        List<String> deleteEntitiesList = new ArrayList<String>();

        switch (entityType) {
            case INVOICE:
                deleteEntitiesList.add(" invoice_meta_field_map ");
                break;
            case CUSTOMER:
                deleteEntitiesList.add(" customer_meta_field_map ");
                break;
            case AGENT:
                deleteEntitiesList.add(" partner_meta_field_map ");
                break;
            case ACCOUNT_TYPE:
                deleteEntitiesList.add(" customer_meta_field_map ");
                break;
            case PRODUCT:
                deleteEntitiesList.add(" item_meta_field_map ");
                break;
            case ORDER:
                deleteEntitiesList.add(" order_meta_field_map ");
                break;
            case PAYMENT:
                deleteEntitiesList.add(" payment_meta_field_map ");
                break;
            case USER:
                deleteEntitiesList.add(" user_meta_field_map ");
                break;
            case PLAN:
                deleteEntitiesList.add(" plan_meta_field_map ");
                break;
            case ASSET:
                deleteEntitiesList.add(" asset_meta_field_map ");
                break;
            case ORDER_LINE:
                deleteEntitiesList.add(" order_line_meta_field_map ");
                deleteEntitiesList.add(" order_change_meta_field_map ");
                break;
            case PAYMENT_METHOD_TEMPLATE:
            case PAYMENT_METHOD_TYPE:
            	deleteEntitiesList.add(" payment_information_meta_fields_map ");
            	String deleteValuesHql = "delete from payment_method_template_meta_fields_map where meta_field_id = "+metaFieldId ;
            	session.createSQLQuery(deleteValuesHql).executeUpdate();
            	break;
        }
        String deleteFromSql = "delete from ";
        String deleteWhereSql = " where meta_field_value_id in " +
                "(select val.id from meta_field_value val where meta_field_name_id = :metaFieldId  )";
        for (String deleteSingleEntity : deleteEntitiesList) {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append(deleteFromSql).append(deleteSingleEntity).append(deleteWhereSql);
            session.createSQLQuery(sqlBuilder.toString())
                    .setParameter("metaFieldId", metaFieldId)
                    .executeUpdate();
        }
        String deleteValuesHql = "delete from " + MetaFieldValue.class.getSimpleName() + " where field.id = ?";
        getHibernateTemplate().bulkUpdate(deleteValuesHql, metaFieldId);
    }

    /**
     * Useful to delete meta field values for a given {@link com.sapienter.jbilling.server.metafields.EntityType} entityType and ID id
     *
     * @param id
     * @param entityType
     * @param values
     */
    /*TODO: This method is no longer use in any methods. We may delete it.*/
    public void deleteMetaFieldValues(Integer id, EntityType entityType, List<MetaFieldValue> values) {
        Session session = getSession();
        List<String> deleteEntitiesList = new ArrayList<String>();

        String metaFieldValuesToDelete = "delete from meta_field_value where id in (";

        StringBuffer csvID = new StringBuffer();
        for (MetaFieldValue value : values) {
            csvID.append(value.getId()).append(',');
        }
        metaFieldValuesToDelete += csvID.substring(0, csvID.length() - 1) + ")";

        switch (entityType) {
            case INVOICE:
                deleteEntitiesList.add(" invoice_meta_field_map where invoice_id = " + id);
                break;
            case CUSTOMER:
                deleteEntitiesList.add(" customer_meta_field_map where customer_id = " + id);
                break;
            case AGENT:
                deleteEntitiesList.add(" partner_meta_field_map where partner_id = " + id);
                break;
            case ACCOUNT_TYPE:
                deleteEntitiesList.add(" customer_meta_field_map where customer_id = " + id);
                break;
            case PRODUCT:
                deleteEntitiesList.add(" item_meta_field_map where item_id =" + id);
                break;
            case ORDER:
                deleteEntitiesList.add(" order_meta_field_map where order_id = " + id);
                break;
            case PAYMENT:
                deleteEntitiesList.add(" payment_meta_field_map where payment_id = " + id);
                break;
            case PLAN:
                deleteEntitiesList.add(" plan_meta_field_map where plan_id = " + id);
                break;
            case ASSET:
                deleteEntitiesList.add(" asset_meta_field_map where asset_id = " + id);
                break;
            case ORDER_LINE:
                deleteEntitiesList.add(" order_line_meta_field_map where asset_id = " + id);
                break;
            case USER:
                deleteEntitiesList.add(" user_meta_field_map where user_id = " + id);
                break;
        }

        String deleteFromSql = "delete from ";
        for (String deleteSingleEntity : deleteEntitiesList) {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append(deleteFromSql).append(deleteSingleEntity);
            session.createSQLQuery(sqlBuilder.toString()).executeUpdate();
        }
        session.createSQLQuery(metaFieldValuesToDelete).executeUpdate();
    }

    public Long getFieldCountByDataTypeAndName(DataType dataType, String name, Integer entityId) {
        Query query;
        if (entityId != null) {
            query = getSession().createQuery(findCountByDTypeName + " AND a.entityId = :entityId");
            query.setParameter("entityId", entityId);
        } else {
            query = getSession().createQuery(findCountByDTypeName);
        }
        query.setParameter("dataType", dataType);
        query.setParameter("name", name);
        return (Long) query.uniqueResult();
    }

    /**
     * Method to search entities (Customer, Order, Product, Invoice etc) with matching Meta Field values
     *
     * @param metaField
     * @param value     - currently supported to search a string value, can be extended for others.
     * @return
     */
    /*TODO: This method is no longer use in application. We may delete this method.*/
    public final List<Integer> findEntitiesByMetaFieldValue(MetaField metaField,
                                                            String value) {
        List<Integer> customizedEntityList = null;
        Session session = getSession();
        try {
            String temp = "select val.id from meta_field_value val where meta_field_name_id="
                    + metaField.getId();
            switch (metaField.getDataType()) {
                case STRING:
                    temp += " and string_value= :value";
                    break;
            }
            List<Integer> values = session.createSQLQuery(temp).setString("value", value).list();

            List<String> queries = new ArrayList<String>();
            if (!values.isEmpty()) {
                for (Integer id : values) {
                    switch (metaField.getEntityType()) {
                        case INVOICE:
                            queries.add("select map.invoice_id from invoice_meta_field_map map, invoice i where map.meta_field_value_id = "
                                    + id + " and map.invoice_id = i.id and i.deleted = 0");
                            break;
                        case CUSTOMER:
                            queries.add("select customer_id from customer_meta_field_map where meta_field_value_id = "
                                    + id + " and customer_id not in (select c.id from customer c, base_user bu where c.user_id = bu.id and bu.deleted > 0)");
                            // queries.add("select partner_id from partner_meta_field_map where meta_field_value_id="
                            // + id);
                            break;
                        case AGENT:
                            queries.add("select partner_id from partner_meta_field_map where meta_field_value_id = "
                                    + id + " and partner_id not in (select p.id from partner p, base_user bu where p.user_id = bu.id and bu.deleted > 0)");
                            break;
                        case PRODUCT:
                            queries.add("select map.item_id from item_meta_field_map map, item i where map.meta_field_value_id="
                                    + id + " i.id = map.item_id and i.deleted = 0");
                            break;
                        case ORDER:
                            queries.add("select map.order_id from order_meta_field_map map, purchase_order po where meta_field_value_id="
                                    + id + " po.id = map.order_id and po.deleted = 0");
                            break;
                        case PAYMENT:
                            queries.add("select map.payment_id from payment_meta_field_map map, payment p where meta_field_value_id="
                                    + id + " p.id = map.payment_id and p.deleted = 0");
                            break;
                        case USER:
                            queries.add("select map.user_id from user_meta_field_map map, base_user u where meta_field_value_id="
                                    + id + " u.id = map.user_id and p.deleted = 0");
                            break;
                        case PLAN:
                            queries.add("select map.plan_id from plan_meta_field_map map, plan p where meta_field_value_id="
                                    + id + " p.id = map.plan_id and p.deleted = 0");
                            break;
                    }
                }
                customizedEntityList = new ArrayList<Integer>();
                for (String query : queries) {
                    customizedEntityList.addAll(session.createSQLQuery(query)
                            .list());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // do something esle?
        }

        return customizedEntityList;
    }

    public Long countMetaFieldValuesForEntity(EntityType entityType, int metaFieldId) {
        Session session = getSession();
        Set<String> entityTypes = new HashSet<String>();

        switch (entityType) {
            case INVOICE:
                entityTypes.add(" invoice_meta_field_map ");
                break;
            case CUSTOMER:
                entityTypes.add(" customer_meta_field_map ");
                break;
            case AGENT:
                entityTypes.add(" partner_meta_field_map ");
                break;
            case ACCOUNT_TYPE:
                entityTypes.add(" customer_meta_field_map ");
                entityTypes.add(" item_type_meta_field_map ");
                break;
            case PRODUCT:
                entityTypes.add(" item_meta_field_map ");
                break;
            case ORDER:
                entityTypes.add(" order_meta_field_map ");
                break;
            case PAYMENT:
                entityTypes.add(" payment_meta_field_map ");
                break;
            case PLAN:
                entityTypes.add(" plan_meta_field_map ");
                break;
            case ASSET:
                entityTypes.add(" asset_meta_field_map ");
                break;
            case ORDER_LINE:
                entityTypes.add(" order_line_meta_field_map ");
                break;
            case USER:
                entityTypes.add(" user_meta_field_map ");
                break;
            case PAYMENT_METHOD_TYPE:
            case PAYMENT_METHOD_TEMPLATE:
                entityTypes.add(" payment_information_meta_fields_map ");
                break;
        }

        Long count = 0L;
        String sql;
        String countSql = "select count(*) from ";
        String countWhereSql =
                " where meta_field_value_id in " +
                        "(select val.id " +
                        "   from meta_field_value val " +
                        "       where meta_field_name_id = :metaFieldId " +
                        "           and (boolean_value is not null " +
                        "               or date_value is not null " +
                        "               or decimal_value is not null " +
                        "               or integer_value is not null " +
                        "               or (string_value is not null and string_value <> ''))) " +
                        "or meta_field_value_id in " +
                        "(select distinct val.id " +
                        "   from meta_field_value val join list_meta_field_values lmv on lmv.meta_field_value_id = val.id " +
                        "   where val.meta_field_name_id = :metaFieldId " +
                        ")";

        for (String entity : entityTypes) {
            sql = countSql + entity + countWhereSql;
            Number temp = (Number) session.createSQLQuery(sql)
                    .setParameter("metaFieldId", metaFieldId)
                    .uniqueResult();
            count = count + (temp == null ? 0L : temp.longValue());
        }

        return count;
    }

    public Long getTotalFieldCount(int metaFieldId) {
        long totalCount = 0L;
        for (EntityType entityType : EntityType.values()) {
            totalCount = totalCount + countMetaFieldValuesForEntity(entityType, metaFieldId);
        }

        return totalCount;
    }

    private static final String findCustomerValuesSQL =
            "select this.id " +
                    " from meta_field_value this " +
                    " inner join meta_field_name field on this.meta_field_name_id=field.id " +
                    " inner join customer_meta_field_map cmap on cmap.meta_field_value_id = this.id" +
                    " where field.field_usage = :type " +
                    "   and cmap.customer_id = :customer " +
                    "   order by field.id asc";

    public List<Integer> getCustomerFieldValues(Integer customerId, MetaFieldType type) {
        if (null == customerId || null == type) {
            throw new IllegalArgumentException("can have null arguments for customer or type");
        }

        SQLQuery query = getSession().createSQLQuery(findCustomerValuesSQL);
        query.setParameter("type", type.toString());
        query.setParameter("customer", customerId);
        return query.list();
    }


    private static final String findCustomerValuesByGroupSQL =
            "(select this.id " +
                    " from meta_field_value this " +
                    " inner join meta_field_name field on this.meta_field_name_id=field.id " +
                    " inner join metafield_group_meta_field_map mgmfm on field.id = mgmfm.meta_field_value_id " +
                    " inner join customer_meta_field_map cmap on cmap.meta_field_value_id = this.id" +
                    " where field.field_usage = :type " +
                    "   and cmap.customer_id = :customer " +
                    "   and mgmfm.metafield_group_id = :groupId " +
                    "   order by field.id asc)" +
                    "UNION" +
                    "(select this.id " +
                    "from meta_field_value this " +
                    "inner join meta_field_name field on this.meta_field_name_id=field.id " +
                    "inner join metafield_group_meta_field_map mgmfm on field.id = mgmfm.meta_field_value_id " +
                    "inner join customer_account_info_type_timeline timeline on timeline.meta_field_value_id = this.id " +
                    "where field.field_usage = :type " +
                    "and timeline.customer_id = :customer " +
                    "and mgmfm.metafield_group_id = :groupId " +
                    "and effective_date = (select max(effective_date) from customer_account_info_type_timeline where customer_id = :customer and effective_date <= :startDate) " +
                    "order by field.id asc)";

    public List<Integer> getCustomerFieldValues(Integer customerId, MetaFieldType type, Integer groupId, Date effectiveDate) {
        if (null == customerId || null == type || null == groupId) {
            throw new IllegalArgumentException("can have null arguments for customer, type or group");
        }

        SQLQuery query = getSession().createSQLQuery(findCustomerValuesByGroupSQL);
        query.setParameter("type", type.toString());
        query.setParameter("customer", customerId);
        query.setParameter("groupId", groupId);
        query.setDate("startDate", effectiveDate);
        return query.list();
    }
    
    /**
     * Returns All IDs with matching criteria
     *
     * @param dataType
     * @param name
     * @return
     */
    public List<Integer> getAllIdsByDataTypeAndName(DataType dataType, String name) {
        Query query = getSession().createQuery(findAllIdsByDataTypeNameSQL);
        query.setParameter("dataType", dataType);
        query.setParameter("name", name);
        return query.list();
    }

    public MetaFieldValue getStringMetaFieldValue(Integer valueId) {
        Criteria criteria = getSession().createCriteria(StringMetaFieldValue.class);
        criteria.add(Restrictions.eq("id", valueId));
        return (MetaFieldValue) criteria.uniqueResult();
    }

    public MetaFieldValue getIntegerMetaFieldValue(Integer valueId) {
        Criteria criteria = getSession().createCriteria(IntegerMetaFieldValue.class);
        criteria.add(Restrictions.eq("id", valueId));
        return (MetaFieldValue) criteria.uniqueResult();
    }

    public static final String getByFieldTypes =
            "select mf.id " +
                    " from meta_field_name mf" +
                    " where mf.field_usage in (:types) " +
                    " and mf.entity_id = :entity ";

    @SuppressWarnings("unchecked")
    public List<Integer> getByFieldType(Integer entityId, MetaFieldType[] types) {
        if (null == entityId || null == types || types.length == 0) {
            throw new IllegalArgumentException("entity and types must be defined");
        }
        String strTypes[] = toStringArray(types);
        SQLQuery query = getSession().createSQLQuery(getByFieldTypes);
        query.setParameter("entity", entityId);
        query.setParameterList("types", strTypes);
        return query.list();
    }

    private String[] toStringArray(MetaFieldType[] types) {
        String result[] = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            result[i] = types[i].toString();
        }
        return result;
    }


    public List<Integer> findByValue(MetaField field, Object value, Boolean sensitive) {
        if (null == field || null == value) {
            throw new IllegalArgumentException("arguments field and/or value can not be null");
        }

        StringBuilder queryBuilder = getFindByValueQueryBuilder(field.getDataType(), value, sensitive);
        SQLQuery query = getSession().createSQLQuery(queryBuilder.toString());
        return query.list();
    }

    public List<Integer> findByValueAndField(DataType type, Object value, Boolean sensitive, List<Integer> fields) {
        if (null == type || null == value || null == fields) {
            throw new IllegalArgumentException("arguments type/value/fields can not be null");
        }

        StringBuilder queryBuilder = getFindByValueQueryBuilder(type, value, sensitive);
        queryBuilder.append(" and meta_field_name_id in (:fields)");

        SQLQuery query = getSession().createSQLQuery(queryBuilder.toString());
        query.setParameterList("fields", fields);
        return query.list();
    }


    private StringBuilder getFindByValueQueryBuilder(DataType type, Object value, Boolean sensitive) {
        StringBuilder queryBuilder = new StringBuilder(
                "select mfv.id from meta_field_value mfv where ");

        if (type.equals(DataType.STRING)) {
            if (null == sensitive || sensitive.booleanValue()) {
                queryBuilder.append("mfv.string_value = '" + (String) value + "' ");
            } else {
                queryBuilder.append("lower(mfv.string_value) = '" + ((String) value).toLowerCase() + "' ");
            }
        }

        return queryBuilder;
    }

    private static final String getValuesByCustomerFields =
            "(select mv.id " +
                    " from meta_field_value mv, customer_meta_field_map cmfm " +
                    " where cmfm.customer_id = :customer " +
                    "   and cmfm.meta_field_value_id = mv.id" +
                    "   and mv.meta_field_name_id in (:fields))" +
                    " UNION " +
                    "(select mv.id " +
                    " from meta_field_value mv, customer_account_info_type_timeline cmfm " +
                    " where cmfm.customer_id = :customer " +
                    "  and cmfm.meta_field_value_id = mv.id " +
                    "  and mv.meta_field_name_id in (:fields) " +
                    "  and cmfm.effective_date = (select max(effective_date) from customer_account_info_type_timeline where customer_id = :customer and effective_date <= :startDate))";

    public List<Integer> getValuesByCustomerAndFields(Integer customerId, List<Integer> fields, Date startDate) {
        if (null == customerId || null == fields || fields.size() == 0) {
            throw new IllegalArgumentException(" customer and fields can not be null");
        }

        SQLQuery query = getSession().createSQLQuery(getValuesByCustomerFields);
        query.setParameter("customer", customerId);
        query.setParameterList("fields", fields);

        query.setDate("startDate", startDate);


        return query.list();
    }

    @Transactional(readOnly = true)
    public List<MetaField> getMetaFieldsByEntity(Integer entityId) {
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entityId", entityId));
        List<MetaField> result = null;
        try {
            result = (List<MetaField>) getHibernateTemplate().findByCriteria(query);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            LOG.error(e);
        }
        return result;
    }

    public Long countMetaFieldsByEntity(Integer entityId) {
        Criteria crit = getSession().createCriteria(MetaField.class);
        crit.add(Restrictions.eq("entityId", entityId))
                .setProjection(Projections.rowCount());
        Long result = null;
        try {
            result = (Long) crit.uniqueResult();
        } catch (Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            LOG.error(e);
        }
        return result;
    }

    private static final String getIdByEmail =
            "select mv.id  from meta_field_value mv" +
                    " where mv.string_value =:email ";
    
    public Integer getIdByEmail(String email){

        SQLQuery query = getSession().createSQLQuery(getIdByEmail);
        query.setParameter("email", email);
        List<Integer> metafieldValuesIds= (List<Integer>) query.list();
        if (metafieldValuesIds.size()>1|| metafieldValuesIds.size()==0){
            return null;
        }else{
        return (Integer) query.list().get(0);
        }

    }


    public static final String getPlansByMetaFieldQuery =
            "select pmfm.plan_id from plan_meta_field_map pmfm " +
                    "inner join meta_field_value value on pmfm.meta_field_value_id=value.id " +
                    " inner join meta_field_name name on value.meta_field_name_id=name.id " +
                    "where name.id= :mfId AND value.string_value= :value AND name.entity_id= :entityId";

    public List<Integer> findPlansByMetaFieldValue(MetaField metaField, String value, Integer entityId) {
        SQLQuery query = getSession().createSQLQuery(getPlansByMetaFieldQuery);
        query.setParameter("mfId", metaField.getId());
        query.setParameter("value", value);
        query.setParameter("entityId", entityId);

        return query.list();
    }

    private static final String findUseIdByCustomerAccountNumberSQL =
            "SELECT c.user_id FROM meta_field_name mfn " +
                    "INNER JOIN meta_field_value mfv ON mfv.meta_field_name_id = mfn.id " +
                    "INNER JOIN metafield_group_meta_field_map mfg_mfv_map ON mfn.id = mfg_mfv_map.meta_field_value_id " +
                    "INNER JOIN meta_field_group mfg ON mfg.id = mfg_mfv_map.metafield_group_id " +
                    "INNER JOIN customer_account_info_type_timeline caitt ON mfg.id = caitt.account_info_type_id AND mfv.id = caitt.meta_field_value_id " +
                    "INNER JOIN customer c ON c.id = caitt.customer_id " +
                    "WHERE mfn.name = 'UTILITY_CUST_ACCT_NR' AND mfv.string_value = :customerAccountNumber";

    public Integer getUserIdByCustomerAccountNumber(String customerAccountNumber){
        Query query = getSession().createSQLQuery(findUseIdByCustomerAccountNumberSQL);
        query.setParameter("customerAccountNumber", customerAccountNumber);
        return (Integer) query.uniqueResult();
    }
    
    private static final String SELECT_CUSTOMER_AIT_META_FIELD_VALUE_BY_GROUP_SQL = 
    		"select field.name, this.boolean_value, this.date_value, this.decimal_value, this.integer_value, this.string_value " +  
    				"from meta_field_value this " +
    				"inner join meta_field_name field on this.meta_field_name_id=field.id " +
    				"inner join metafield_group_meta_field_map mgmfm on field.id = mgmfm.meta_field_value_id " +
    				"inner join customer_account_info_type_timeline timeline on timeline.meta_field_value_id = this.id " + 
    				"where timeline.customer_id = :customer " +
    				"and mgmfm.metafield_group_id = :groupId " +
    				"and effective_date = (select max(effective_date) from customer_account_info_type_timeline where customer_id = :customer and effective_date <= :startDate)";

    public Map<String, String> getCustomerAITMetaFieldValue(Integer customerId, Integer groupId, Date effectiveDate){
    	if(null == customerId || null == groupId){
    		throw new IllegalArgumentException("can not have null arguments for customer or group");
    	}
    	SQLQuery query = getSession().createSQLQuery(SELECT_CUSTOMER_AIT_META_FIELD_VALUE_BY_GROUP_SQL);
    	query.setParameter("customer", customerId);
    	query.setParameter("groupId", groupId);
    	query.setDate("startDate", effectiveDate);
    	return getValue(query.scroll());
    }
    
    private static final String findCustomerMetaFieldValueBySQL = "select field.name, this.boolean_value, this.date_value, this.decimal_value, this.integer_value, this.string_value  " +
    		" from meta_field_value this "
    		+" inner join meta_field_name field on this.meta_field_name_id=field.id " + 
    		"  inner join customer_meta_field_map cmap on cmap.meta_field_value_id = this.id "
    		+" where field.entity_type = 'CUSTOMER' and cmap.customer_id = :customerId ";

    public Map<String, String> getCustomerMetaFieldValue(Integer customerId) {
    	if(null == customerId){
    		throw new IllegalArgumentException("can not have null arguments for customer");
    	}
    	SQLQuery query = getSession().createSQLQuery(findCustomerMetaFieldValueBySQL);
    	query.setParameter("customerId", customerId);
    	return getValue(query.scroll());
    }
    
    private static final String findOrderLineMetaFieldValueBySQL = "select field.name, this.boolean_value, this.date_value, this.decimal_value, this.integer_value, this.string_value  " +
    		" from meta_field_value this "
    		+" inner join meta_field_name field on this.meta_field_name_id=field.id " + 
    		"  inner join order_line_meta_field_map olmap on olmap.meta_field_value_id = this.id "
    		+" where field.entity_type = 'ORDER_LINE' and olmap.order_line_id = :lineId ";

    public Map<String, String> getOrderLineMetaFieldValue(Integer lineId) {
    	if(null == lineId){
    		throw new IllegalArgumentException("can not have null arguments for OrderLineId");
    	}
    	SQLQuery query = getSession().createSQLQuery(findOrderLineMetaFieldValueBySQL);
    	query.setParameter("lineId", lineId);
    	return getValue(query.scroll());
    }

    public void removeMetaFieldDependency(List<Integer> ids) {
        String removeMetaFieldDependency = "DELETE FROM meta_field_dependency_map mfdm where mfdm.meta_field_id IN (:ids)";
        Query query = getSession().createSQLQuery(removeMetaFieldDependency);
        query.setParameterList("ids", ids);
        query.executeUpdate();
    }
    
    private static final String FIND_META_FIELD_BY_COMPANY = "SELECT {mfn.*} from meta_field_name mfn " +
            "INNER JOIN metafield_group_meta_field_map mfgmfm ON mfgmfm.meta_field_value_id = mfn.id " +
            "INNER JOIN meta_field_group mfg ON mfg.id = mfgmfm.metafield_group_id " +
            "WHERE mfg.entity_id = :entityId and mfg.company_id = :companyId and mfn.id = :metaFieldId";

    /**
     * search meta field by company for doing dependency in a same company meta field only.
     *
     * @param metaFieldId, entityId, companyId
     *
     * @return MetaField
     */
    public MetaField getMetaFieldByCompany(Integer metaFieldId, Integer entityId, Integer companyId) {
        SQLQuery query = getSession().createSQLQuery(FIND_META_FIELD_BY_COMPANY);
        query.setParameter("entityId", entityId);
        query.setParameter("companyId", companyId);
        query.setParameter("metaFieldId", metaFieldId);
        query.addEntity("mfn", MetaField.class);
        List<MetaField> fields = query.list();
        return !fields.isEmpty() ? fields.get(0) : null;
    }
    
    private static final String FIND_META_FIELD_BY_ACCOUNT_TYPE = "SELECT {mfn.*} from meta_field_name mfn " +
            "INNER JOIN metafield_group_meta_field_map mfgmfm ON mfgmfm.meta_field_value_id = mfn.id " +
            "INNER JOIN meta_field_group mfg ON mfg.id = mfgmfm.metafield_group_id " +
            "WHERE mfg.entity_id = :entityId and mfg.account_type_id = :accountTypeId and mfn.id = :metaFieldId";

    /**
     * search meta field by account type for doing dependency in a same account type meta field only.
     *
     * @param metaFieldId, entityId, accountTypeId
     *
     * @return MetaField
     */
    public MetaField getMetaFieldByAccountType(Integer metaFieldId, Integer entityId, Integer accountTypeId) {
        SQLQuery query = getSession().createSQLQuery(FIND_META_FIELD_BY_ACCOUNT_TYPE);
        query.setParameter("entityId", entityId);
        query.setParameter("accountTypeId", accountTypeId);
        query.setParameter("metaFieldId", metaFieldId);
        query.addEntity("mfn", MetaField.class);
        List<MetaField> fields = query.list();
        return !fields.isEmpty() ? fields.get(0) : null;
    }
    
    public String getComapanyLevelMetaFieldValue(String metaFieldName, Integer entityId) {
    	String sql = "select boolean_value, date_value, decimal_value, integer_value, string_value  from meta_field_value " 
    			+" where meta_field_name_id = (select id from meta_field_name where name = ? and entity_id = ? and entity_type = 'COMPANY')";

    	SqlRowSet result = ((JdbcTemplate) Context.getBean(Name.JDBC_TEMPLATE)).queryForRowSet(sql, metaFieldName, entityId);
    	String fieldValue = null;
    	if (result.next()) {
    		Boolean booleanValue = (Boolean) result.getObject(1);
    		Date dateValue = (Date) result.getObject(2);
    		BigDecimal decimalValue = (BigDecimal) result.getObject(3);
    		Integer intValue = (Integer) result.getObject(4);
    		String stringValue = (String) result.getObject(5);
    		if(booleanValue!=null) {
    			fieldValue = booleanValue.toString();
    		} else if(dateValue!=null) {
    			fieldValue = dateValue.toString();
    		} else if(decimalValue!=null) {
    			fieldValue = decimalValue.toString();
    		} else if(intValue!=null) {
    			fieldValue =intValue.toString();
    		} else if(stringValue!=null) {
    			fieldValue =stringValue;
    		}
    	}
    	return fieldValue;
    }
    
    private static final String getCustomerAITMetaFieldValueMapByMetaFieldType =
            "SELECT field.field_usage, this.boolean_value, this.date_value, this.decimal_value, this.integer_value, this.string_value " +
                    "FROM meta_field_value this " +
                    "INNER JOIN meta_field_name field ON this.meta_field_name_id=field.id " +
                    "INNER JOIN metafield_group_meta_field_map mgmfm ON field.id = mgmfm.meta_field_value_id " +
                    "INNER JOIN customer_account_info_type_timeline timeline ON timeline.meta_field_value_id = this.id " +
                    "WHERE timeline.customer_id = :customer " +
                    "AND mgmfm.metafield_group_id = :groupId " +
                    "AND effective_date = (SELECT MAX(effective_date) " +
                                            "FROM customer_account_info_type_timeline " +
                                            "WHERE customer_id = :customer and effective_date <= :startDate " +
                                            "AND account_info_type_id = :groupId )";

    public Map<String, String> getCustomerAITMetaFieldValueMapByMetaFieldType(Integer customerId, Integer groupId, Date effectiveDate){
    	if(null == customerId || null == groupId){
    		throw new IllegalArgumentException("can not have null arguments for customer or group");
    	}
    	SQLQuery query = getSession().createSQLQuery(getCustomerAITMetaFieldValueMapByMetaFieldType);
    	query.setParameter("customer", customerId);
    	query.setParameter("groupId", groupId);
    	query.setDate("startDate", effectiveDate);
    	return getValue(query.scroll());
    }
    
    /**
     * Returns Map which contains meta field name and it's associated value for 
     * given ScrollableResults
     * @param ScrollableResults
     * @return
     */
    private Map<String,String> getValue(ScrollableResults result) {
    	Map<String, String> resultMap = new HashMap<String, String>();
    	while(result.next()) {
    		String fieldName = (String) result.get(0);
    		String fieldValue = null;
    		Boolean booleanValue = (Boolean) result.get(1);
    		Date dateValue = (Date) result.get(2);
    		BigDecimal decimalValue = (BigDecimal) result.get(3);
    		Integer intValue = (Integer) result.get(4);
    		String stringValue = (String) result.get(5);
    		if(booleanValue!=null) {
    			fieldValue = booleanValue.toString();
    		} else if(dateValue!=null) {
    			fieldValue = dateValue.toString();
    		} else if(decimalValue!=null) {
    			fieldValue = decimalValue.toString();
    		} else if(intValue!=null) {
    			fieldValue =intValue.toString();
    		} else if(stringValue!=null) {
    			fieldValue =stringValue;
    		}
    		resultMap.put(fieldName, fieldValue);

    	}
        result.close();
    	return resultMap;
    }
    
    
    @SuppressWarnings("unchecked")
    public List<MetaField> getAvailableMetaFields(Integer entityId, EntityType entityType, String metaFieldName, Boolean primary) {
    	
        DetachedCriteria query = DetachedCriteria.forClass(MetaField.class);
        query.add(Restrictions.eq("entityId", entityId));
        query.add(Restrictions.eq("entityType", entityType));
        query.add(Restrictions.eq("name", metaFieldName));
        if (null != primary) {
            query.add(Restrictions.eq("primary", primary.booleanValue()));
        }
        List<MetaField> result = null;
        try {
            result = (List<MetaField>) getHibernateTemplate().findByCriteria(query);
        } catch (Exception e) {
            LOG.error("Exception occurred while getting available metafield against the entity type ",e);
        }
        return result;
    }
}
