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
package com.sapienter.jbilling.server.item.db;

import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;
import org.hibernate.criterion.Subqueries;
import org.hibernate.type.StringType;

public class ItemDAS extends AbstractDAS<ItemDTO> {

    /**
     * Returns a list of all items for the given item type (category) id.
     * If no results are found an empty list will be returned.
     *
     * @param itemTypeId item type id
     * @return list of items, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<ItemDTO> findAllByItemType(Integer itemTypeId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("itemTypes", "type")
                .add(Restrictions.eq("type.id", itemTypeId))
                .add(Restrictions.eq("deleted", 0))
                .addOrder(Order.desc("id"));

        return criteria.list();
    }

    /**
     * Returns a list of all items with item type (category) who's
     * description matches the given prefix.
     *
     * @param prefix prefix to check
     * @return list of items, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<ItemDTO> findItemsByCategoryPrefix(String prefix) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("itemTypes", "type")
                .add(Restrictions.like("type.description", prefix + "%"));

        return criteria.list();
    }    

    public List<ItemDTO> findItemsByInternalNumber(String internalNumber) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("internalNumber", internalNumber));

        return criteria.list();
    }

    public ItemDTO findItemByInternalNumber(String internalNumber, Integer entityId) {

        Integer rootCompanyId = new CompanyDAS().getParentCompanyId(entityId);
        rootCompanyId = rootCompanyId!=null?rootCompanyId:entityId;
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("internalNumber", internalNumber).ignoreCase())
                .createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.disjunction()
                        .add(Restrictions.conjunction().add(Restrictions.eq("entity.id", rootCompanyId)).add(Restrictions.eq("global", true)))
                        .add(Restrictions.eq("entities.id", entityId)))
                .add(Restrictions.eq("deleted", 0));

        return (ItemDTO)criteria.uniqueResult();
    }

    private static final String CURRENCY_USAGE_FOR_ENTITY_SQL = new StringBuilder().append("    SELECT COUNT(*) ")
                                                                                   .append("      FROM item AS i ")
                                                                                   .append("INNER JOIN item_entity_map iem ON iem.item_id = i.id ")
                                                                                   .append("INNER JOIN entity_item_price_map eipm ON eipm.item_id = i.id ")
                                                                                   .append("INNER JOIN item_price_timeline ipt ON ipt.model_map_id = eipm.id ")
                                                                                   .append("INNER JOIN price_model pm ON pm.id = ipt.price_model_id ")
                                                                                   .append("WHERE pm.currency_id = :currencyId ")
                                                                                   .append("AND iem.entity_id = :entityId ")
                                                                                   .append("AND i.deleted = 0")
                                                                                   .toString();

    public Long findProductCountByCurrencyAndEntity(Integer currencyId, Integer entityId ) {
        Query sqlQuery = getSession().createSQLQuery(CURRENCY_USAGE_FOR_ENTITY_SQL);
        sqlQuery.setParameter("currencyId", currencyId);
        sqlQuery.setParameter("entityId", entityId);

        Number count = (Number) sqlQuery.uniqueResult();

        return null == count ? 0L : count.longValue();
    }

    public Long findProductCountByInternalNumber(String internalNumber, Integer entityId, boolean isNew, Integer id) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
        		.createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.eq("internalNumber", internalNumber))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("entities.id", entityId));

        if(!isNew)
            criteria.add(Restrictions.ne("id", id));

        return (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
    }
    
    public List<ItemDTO> findByEntityId(Integer entityId) {
    	Criteria criteria = getSession().createCriteria(ItemDTO.class)
        		.createAlias("entities","entities", CriteriaSpecification.LEFT_JOIN)
        		.add(Restrictions.eq("entities.id", entityId))
        		.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return criteria.list();
    }

    /**
     * Get all items for the given company its childs and global categories
     */
    public List<ItemDTO> findItems(Integer entity, List<Integer> entities, boolean isRoot) {
        Criteria criteria = getSession().createCriteria(ItemDTO.class)
                .createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN);

        Disjunction dis = Restrictions.disjunction();
        dis.add(Restrictions.eq("global", true));
        dis.add(Restrictions.in("entities.id", entities));
        if (isRoot) {
            dis.add(Restrictions.eq("entities.parent.id", entity));
        }

        criteria.add(dis)
                .addOrder(Order.asc("id"))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return criteria.list();
	}

    private static final String PRODUCT_VISIBLE_TO_PARENT_COMPANY_SQL =
            "select count(*) from item i " +
                    " left join item_entity_map ie on ie.item_id = i.id where " +
                    " i.id = :itemId and " +
                    " (ie.entity_id = :entityId or i.entity_id = :entityId) and" +
                    " i.deleted = 0";
    
    private static final String PRODUCT_AVAILABLE_TO_PARENT_COMPANY_SQL =
            "select count(*) from item i " +
                    " left join item_entity_map ie on ie.item_id = i.id where " +
                    " i.id = :itemId and " +
                    " (ie.entity_id = :entityId) and" +
                    " i.deleted = 0";

    private static final String PRODUCT_VISIBLE_TO_CHILD_HIERARCHY_SQL =
            "select count(*) from item i "+
                "left outer join item_entity_map icem "+
                "on i.id = icem.item_id "+
                "where i.id = :itemId "+
                "and  i.deleted = 0 "+
                "and  (i.entity_id = :childCompanyId or " +
                " icem.entity_id = :childCompanyId or " +
                "((icem.entity_id = :parentCompanyId or icem.entity_id is null) and " +
                "i.global = true));";

    public boolean isProductVisibleToCompany(Integer itemId, Integer entityId, Integer parentId) {
        if (null == parentId) {
            //this means that the entityId is root so the
            //product must be defined for that company
            SQLQuery query = getSession().createSQLQuery(PRODUCT_VISIBLE_TO_PARENT_COMPANY_SQL);
            query.setParameter("itemId", itemId);
            query.setParameter("entityId", entityId);
            Number count = (Number) query.uniqueResult();
            return null != count ? count.longValue() > 0 : false;
        } else {
            //check if the product is visible to either the parent or the child company
            SQLQuery query = getSession().createSQLQuery(PRODUCT_VISIBLE_TO_CHILD_HIERARCHY_SQL);
            query.setParameter("itemId", itemId);
            query.setParameter("parentCompanyId", parentId);
            query.setParameter("childCompanyId", entityId);
            Number count = (Number) query.uniqueResult();
            return null != count ? count.longValue() > 0 : false;
        }
    }
    
    public boolean isProductAvailableToCompany(Integer itemId, Integer entityId) {
            //this means that the entityId is root so the
            //product must be defined for that company
            SQLQuery query = getSession().createSQLQuery(PRODUCT_AVAILABLE_TO_PARENT_COMPANY_SQL);
            query.setParameter("itemId", itemId);
            query.setParameter("entityId", entityId);
            Number count = (Number) query.uniqueResult();
            return null != count ? count.longValue() > 0 : false;
        
    }

    public ItemDTO findByMetaFieldNameAndValue(Integer entityId, String metaFieldName, String metaFieldValue){
        Integer rootCompanyId = new CompanyDAS().getParentCompanyId(entityId);
        rootCompanyId = rootCompanyId!=null?rootCompanyId:entityId;
        Criteria criteria = getSession().createCriteria(ItemDTO.class)
                .createAlias("entities", "entities", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.disjunction()
                        .add(Restrictions.conjunction().add(Restrictions.eq("entity.id", rootCompanyId)).add(Restrictions.eq("global", true)))
                        .add(Restrictions.eq("entities.id", entityId)))
                .createAlias("metaFields", "value")
                .createAlias("value.field", "mf")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("mf.name", metaFieldName))
                .add(Restrictions.sqlRestriction("string_value =  ?", metaFieldValue, StringType.INSTANCE));
        return (ItemDTO)criteria.uniqueResult();
    }

    /* Method return all non global product which belongs to non global category of having entity id's for copying Product */
    public List<ItemDTO> findNonGlobalItemsForCopyProduct(Integer entityId) {
        DetachedCriteria subquery = DetachedCriteria.forClass(PlanDTO.class, "plan")
                                                    .createAlias("plan.item", "item")
                                                    .add(Property.forName("item.id").eqProperty("itemDto.id"));

        Criteria criteria = getSession().createCriteria(ItemDTO.class, "itemDto")
                                        .createAlias("itemDto.entities", "entities", CriteriaSpecification.LEFT_JOIN)
                                        .createAlias("itemDto.itemTypes", "itemType")
                                        .add(Restrictions.eq("entities.id", entityId))
                                        .add(Restrictions.eq("itemType.global", false))
                                        .add(Subqueries.notExists(subquery.setProjection(Projections.property("plan.id"))))
                                        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return criteria.list();
    }
    
    private static final String PLAN_FOR_ITEM =
            "select count(*) from plan i " +
                    " where " +
                    " i.item_id = :itemId ";

    public boolean isPlan(int itemId) {
        SQLQuery query = getSession().createSQLQuery(PLAN_FOR_ITEM);
        query.setParameter("itemId", itemId);
        Number count = (Number) query.uniqueResult();
        return count.intValue() > 0;
    }

    public List<InternationalDescriptionDTO> getDescriptions(Integer itemId) {
        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_ITEM);
        Criteria criteria = getSession().createCriteria(InternationalDescriptionDTO.class)
                .add(Restrictions.eq("id.tableId", table.getId()))
                .add(Restrictions.eq("id.foreignId", itemId))
                .add(Restrictions.eq("id.psudoColumn", "description"));
        return criteria.list();
    }

   public BigDecimal getTaxRate(String taxScheme, String tableName, Date invoiceGenerationDate, String taxDateFormat) {
       BigDecimal[] taxRate = {BigDecimal.ZERO};

       @SuppressWarnings("unchecked")
       List<Object[]> rows = getSession().createSQLQuery("SELECT * FROM " + tableName).list();
       DateTimeFormatter formatter = DateTimeFormatter.ofPattern(StringUtils.isNotEmpty(taxDateFormat) ? taxDateFormat :
           "MM-dd-yyyy", Locale.ENGLISH);
       LocalDate dateToCompare = invoiceGenerationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
       rows.stream().forEach(row -> {
           String resTaxScheme = row[1].toString();
           LocalDate startDate = LocalDate.parse((row[3].toString()), formatter);
           LocalDate endDate = LocalDate.parse((row[4].toString()), formatter);

           if(resTaxScheme.equals(taxScheme) && (dateToCompare.equals(startDate) || dateToCompare.equals(endDate) ||
                   (dateToCompare.isAfter(startDate) && dateToCompare.isBefore(endDate)))) {
               taxRate[0] = new BigDecimal(row[5].toString());
           }
       });

       return taxRate[0];
   }

   private static final String ITEMS_CATEGORY_TYPE_ADJUSTMENT_SQL =
           "SELECT i.id " +
                   "FROM item i " +
                   "INNER JOIN item_type_map itm ON itm.item_id = i.id " +
                   "INNER JOIN item_type it ON it.id = itm.type_id " +
                   "INNER JOIN order_line_type olt ON olt.id = it.order_line_type_id " +
                   "INNER JOIN international_description int_des ON int_des.foreign_id = olt.id " +
                   "INNER JOIN jbilling_table jt ON jt.id = int_des.table_id " +
                   "WHERE int_des.content = 'Adjustment' " +
                   "AND jt.name = 'order_line_type' " +
                   "AND int_des.language_id = 1";

   /**
    * Returns all items belongs to category type Adjustment
    */
   public List<Integer> getAllItemsCategoryTypeAdjustment() {
       SQLQuery query = getSession().createSQLQuery(ITEMS_CATEGORY_TYPE_ADJUSTMENT_SQL);
       return query.list();
   }
}
