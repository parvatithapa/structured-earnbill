/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.ItemTypeBL;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.value.ListMetaFieldValue;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.FilterHqlHelper;
import com.sapienter.jbilling.server.util.search.SearchCriteria;

/**
 * @author Gerhard
 * @since 15/04/13
 */
public class AssetDAS extends AbstractDAS<AssetDTO> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AssetDAS.class));

    /**
     * Count the number of non deleted assets linked to the item.
     *
     * @param itemId
     * @return
     */
    public int countAssetsForItem(int itemId) {
        Query query = getSession().getNamedQuery("AssetDTO.countForItem");
        query.setInteger("item_id", itemId);
        return ((Long) query.uniqueResult()).intValue();
    }

    /**
     * List the asset ids linked to category for the given asset identifier.
     *
     * @param itemTypeId
     * @param identifier
     * @return
     */
    public List<AssetDTO> getAssetForItemTypeAndIdentifier(int itemTypeId, String identifier) {
        Query query = getSession().getNamedQuery("AssetDTO.identifierForIdentifierAndCategory");
        query.setInteger("item_type_id", itemTypeId);
        query.setString("identifier", identifier);
        return query.list();
    }



    /**
     * Return the id for the identifier.
     *
     * @param identifier
     * @return
     */
    public Integer getAssetsForIdentifier(String identifier) {
        Query query = getSession().getNamedQuery("AssetDTO.getForIdentifier");
        query.setString("identifier", identifier);
        List<AssetDTO> list = query.list();
        if (list != null && list.size() > 0) {
            return list.get(0).getId();
        }
        return null;
    }

    /**
     * Return ids for all non deleted assets linked to the category.
     *
     * @param categoryId ItemTypeDTO id
     * @return
     */
    public List<Integer> getAssetsForCategory(Integer categoryId) {
        Query query = getSession().getNamedQuery("AssetDTO.idsForItemType");
        query.setInteger("item_type_id", categoryId);
        return query.list();
    }

    /**
     * Return ids for all non deleted assets linked to the item.
     *
     * @param itemId  ItemDTO id
     * @return
     */
    public List<Integer> getAssetsForItem(Integer itemId) {
        Query query = getSession().getNamedQuery("AssetDTO.idsForItem");
        query.setInteger("item_id", itemId);
        return query.list();
    }

    /**
     * Search for assets using search criteria.
     * Valid fields
     *  - 'id', 'identifier' - AssetDTO values
     *  - 'status' - String status description
     *  - anything else will search meta fields.
     *
     * You can order by any of the properties of AssetDTO.
     *
     * @param productId
     * @param criteria
     * @return
     */
    public AssetSearchResult findAssets(int productId, SearchCriteria criteria) {
        logger.debug(criteria);
        AssetSearchResult result = new AssetSearchResult();

        //HQL clauses
        StringBuilder hqlFrom = new StringBuilder("SELECT DISTINCT a FROM AssetDTO a LEFT JOIN a.metaFields mfs");
        StringBuilder hqlWhere = new StringBuilder(" WHERE a.item.id= :productId ");

        //parameters for HQL query
        Map<String, Object> parameters = new HashMap();
        Map<String, Object> metaFieldParameters = new HashMap<String, Object>();
        ItemTypeDTO assetMngmtType = null;
        Map<String, MetaField> metaFieldMap = null;

        int filterIdx = 1;

        String filterOperator = "AND";
        boolean hasAssetStatusFilter = false;

        for(BasicFilter filter : criteria.getFilters()) {
            //get HQL parameter name and operation
            String f = filter.getField();
            String hqlOp = FilterHqlHelper.getHqlOperator(filter.getConstraint());
            String parmName = (filter.getField() + filterIdx++).replaceAll("\\W", "");

            hqlWhere.append(" ").append(filterOperator).append(" ");
            parameters.put(parmName, FilterHqlHelper.likeMatchStart(filter.getConstraint(), filter.getValue()));

            if("id".equals(f) || "identifier".equals(f)) {
                hqlWhere.append("a.").append(f).append(hqlOp).append(":").append(parmName);

            } else if("status".equals(f)) {
                hasAssetStatusFilter = true;
                hqlFrom.append(", InternationalDescriptionDTO intd, JbillingTable jt");

                hqlWhere.append(" intd.id.foreignId=a.assetStatus.id AND intd.id.tableId=jt.id AND jt.name=:")
                .append(Constants.TABLE_ASSET_STATUS)
                .append(" AND intd.content").append(hqlOp).append(" :").append(parmName);

            } else { //it is a meta field
                //load the asset management type and meta fields
                if(assetMngmtType == null) {
                    assetMngmtType = new ItemTypeBL().findItemTypeWithAssetManagementForItem(productId);
                    Set<MetaField> metafields = assetMngmtType.getAssetMetaFields();
                    metaFieldMap = new HashMap<String, MetaField>(metafields.size()*2);
                    for (MetaField mf : metafields) {
                        metaFieldMap.put(mf.getName(), mf);
                    }
                }

                //build the sub query
                MetaField metaField = metaFieldMap.get(f);
                if(metaField != null) {
                    String valueClass = metaField.createValue().getClass().getName();

                    //convert the parameter value to the correct class (except lists) and update it in the map
                    DataType dataType = metaField.getDataType();
                    if(!dataType.equals(DataType.LIST)) {
                        Object parmValue = MetaFieldBL.createValueFromDataType(metaField, parameters.get(parmName),dataType).getValue();
                        parameters.put(parmName, parmValue);
                    }

                    StringBuilder subQuery = null;
                    String mfIdParameterName = metaField.getName().replaceAll(" ", "") + metaField.getId();
                    metaFieldParameters.put(mfIdParameterName, Integer.valueOf(metaField.getId()));

                    if(valueClass.equals(ListMetaFieldValue.class.getName())) {
                        subQuery = new StringBuilder("SELECT mv FROM ListMetaFieldValue mv JOIN mv.value vals WHERE mv.field.id=:").append(mfIdParameterName)
                                .append(" AND vals").append(hqlOp).append(":").append(parmName);
                    } else {
                        subQuery = new StringBuilder("SELECT mv FROM ").append(valueClass).append(" mv WHERE mv.field.id=:").append(mfIdParameterName)
                                .append(" AND mv.value").append(hqlOp).append(":").append(parmName);
                    }

                    hqlWhere.append(" mfs in ( ").append(subQuery.toString()).append(" )");
                }
            }
        }

        //construct the search query
        String baseHqlQuery = hqlFrom.append(hqlWhere.toString()).toString();
        String hqlQuery = baseHqlQuery;
        if(criteria.getSort() != null && criteria.getSort().length() > 0) {
            hqlQuery += " ORDER BY a." + criteria.getSort() +" "+FilterHqlHelper.getSortDirection(criteria.getDirection());
        }
        Query query = buildQueryWithParameters(hqlQuery, productId, parameters, metaFieldParameters, hasAssetStatusFilter);

        //set the offset and maximum results
        if(criteria.getMax() > 0) {
            query.setMaxResults(criteria.getMax());
        }
        if(criteria.getOffset() > 0) {
            query.setFirstResult(criteria.getOffset());
        }

        //do the query
        List objects = query.list();
        result.setObjects(AssetBL.getWS(objects));

        //get the total
        if(criteria.getTotal() < 0) {
            String hqlCountQuery = baseHqlQuery.replaceFirst("SELECT DISTINCT a FROM", "SELECT count(distinct a) FROM");

            Query countQuery = buildQueryWithParameters(hqlCountQuery, productId, parameters, metaFieldParameters, hasAssetStatusFilter);

            result.setTotal(((Long)countQuery.uniqueResult()).intValue());
        }
        return result;
    }

    private Query buildQueryWithParameters (String sqlString, int productId, Map<String, Object> parameters,
            Map<String, Object> metaFieldParameters, boolean hasAssetStatusFilter) {

        Query query = getSession().createQuery(sqlString);

        query.setParameter("productId", productId);

        for(Entry<String, Object> parmAndValue: parameters.entrySet()) {
            query.setParameter(parmAndValue.getKey(), parmAndValue.getValue());
        }
        for (Entry<String, Object> mfParmAndValue : metaFieldParameters.entrySet()) {
            query.setParameter(mfParmAndValue.getKey(), mfParmAndValue.getValue());
        }
        if (hasAssetStatusFilter) {
            query.setParameter(Constants.TABLE_ASSET_STATUS, Constants.TABLE_ASSET_STATUS);
        }
        return query;
    }

    /**
     * Find the asset for the given identifier belonging to the product.
     *
     * @param identifier
     * @param itemId
     * @return
     */
    public AssetDTO getForItemAndIdentifier(String identifier, int itemId) {
        Query query = getSession().getNamedQuery("AssetDTO.getForItemAndIdentifier");
        query.setInteger("itemId", itemId);
        query.setString("identifier", identifier);
        return (AssetDTO)query.uniqueResult();
    }

    public List<AssetDTO> getAssetsInScopeOfProduct(List<Integer> entities, boolean isGlobal) {
        Criteria criteria = getSession().createCriteria(AssetDTO.class)
                .setFetchMode("item", FetchMode.JOIN)
                .createAlias("item", "i")
                .add(Restrictions.disjunction()
                        .add(Restrictions.in("i.entity.id", entities))
                        .add(Restrictions.eq("i.global", isGlobal))
                        .add(Restrictions.in("i.entities.id", entities)));
        return criteria.list();
    }

    public List<AssetDTO> findAssetsByUser(Integer userId) {
        Criteria criteria = getSession().createCriteria(AssetDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("orderLine", "ol")
                .createAlias("ol.purchaseOrder", "od")
                .createAlias("od.baseUserByUserId", "bu")
                .add(Restrictions.eq("bu.id", userId))
                .addOrder(Order.desc("id"));
        return criteria.list();
    }


    public List<AssetDTO> findAssetByProductCode(String productCode, Integer companyId){

        Criteria criteria = getSession().createCriteria(AssetDTO.class)
                .createAlias("item", "i")
                .add(Restrictions.eq("entity.id", companyId))
                .add(Restrictions.eq("i.internalNumber", productCode))
                .add(Restrictions.eq("deleted", 0));
        return criteria.list();
    }

    public List<AssetDTO> findAssetByProductCodeAndAssetStatusId(String productCode, Integer assetStatusId, Integer companyId){

        Criteria criteria = getSession().createCriteria(AssetDTO.class)
                .createAlias("item", "i")
                .add(Restrictions.eq("entity.id", companyId))
                .add(Restrictions.eq("i.internalNumber", productCode))
                .add(Restrictions.eq("assetStatus.id", assetStatusId))
                .add(Restrictions.eq("deleted", 0));
        return criteria.list();
    }

    public AssetDTO findAssetByProductCodeAndIdentifier(String productCode, String identifier, Integer companyId){
        final String hql ="from AssetDTO a "
                + " where a.item.internalNumber= :productCode "
                + " and a.identifier = :identifier and a.entity.id= :companyId and a.deleted=0";
        Query query = getSession().createQuery(hql);
        query.setParameter("identifier", identifier);
        query.setParameter("productCode", productCode);
        query.setParameter("companyId", companyId);
        return (AssetDTO)query.uniqueResult();
    }

    public AssetDTO getAssetByIdentifier(String identifier) {
        Query query = getSession().getNamedQuery("AssetDTO.getForIdentifier");
        query.setString("identifier", identifier);
        return (AssetDTO)query.uniqueResult();
    }

    private static final String findAssetIdentifiersByUserId = "select identifier from asset where order_line_id in " +
            "(select id from order_line where order_id in " +
            "(select id  from purchase_order where user_id = :userId) and deleted = 0 )";

    public String findAssetsIdentifierByUserId(Integer userId) {
        if(null == userId) {
            throw new IllegalArgumentException("can not have null arguments for user");
        }
        SQLQuery query = getSession().createSQLQuery(findAssetIdentifiersByUserId);
        query.setParameter("userId", userId);

        ScrollableResults results = query.scroll();

        StringBuilder identifiers = new StringBuilder();
        while(results.next()) {
            String identifier = (String) results.get(0);
            identifiers.append(identifier+",");
        }
        return identifiers.toString();
    }

    private static final String findAssetIdentifiersByOrderId = "select identifier,order_line_id from asset where order_line_id in " +
            "(select id from order_line where order_id = :orderId )";

    public Map<Integer, Map<Integer, List<String>>> findAssetsIdentifierByOrderId(Integer orderId) {
        if(null == orderId) {
            throw new IllegalArgumentException("can not have null arguments for order");
        }
        SQLQuery query = getSession().createSQLQuery(findAssetIdentifiersByOrderId);
        query.setParameter("orderId",orderId);

        ScrollableResults results = query.scroll();
        Map<Integer, List<String>> orderLineAndAssetIdentifierMap = new HashMap<Integer, List<String>>();
        Map<Integer, Map<Integer, List<String>>> orderAndOrderLineAssetIdentifierMap = new HashMap<Integer, Map<Integer,List<String>>>();
        while(results.next()) {
            String identifier = (String) results.get(0);
            Integer orderLineId = (Integer) results.get(1);
            List<String> identifiers = orderLineAndAssetIdentifierMap.getOrDefault(orderLineId, new ArrayList<String>());
            identifiers.add(identifier);
            orderLineAndAssetIdentifierMap.put(orderLineId, identifiers);
        }
        orderAndOrderLineAssetIdentifierMap.put(orderId, orderLineAndAssetIdentifierMap);
        return  orderAndOrderLineAssetIdentifierMap;
    }

    public List<AssetDTO> getAssetsByProduct(Integer itemId, Integer companyId, Integer offset, Integer max){

        Criteria criteria = getSession().createCriteria(AssetDTO.class)
                .createAlias("item", "i")
                .add(Restrictions.eq("entity.id", companyId))
                .add(Restrictions.eq("i.id", itemId))
                .add(Restrictions.eq("deleted", 0))
                .addOrder(Order.asc("id"));
        if (null != max){
            criteria.setMaxResults(max);
        }
        if (null != offset){
            criteria.setFirstResult(offset);
        }
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<AssetDTO> getAssetsByCategory(Integer categoryId, Integer companyId, Integer offset, Integer max) {
        return buildAssetCategoryCriteria(categoryId, offset, max)
                .add(Restrictions.eq("entity.id", companyId))
                .list();
    }

    private static final String FIND_ASSETS_BY_CATEGORY_AND_STATUS_SQL =
            "SELECT id FROM asset WHERE item_id IN "
                    + "(SELECT item_id FROM item_type_map WHERE type_id = :categoryId) "
                    + "AND status_id = :statusId ORDER BY id DESC";

    @SuppressWarnings("unchecked")
    public List<Integer> getAssetsByCategoryAndStatus(Integer categoryId, Integer statusId, Integer offset, Integer max) {
        Assert.notNull(categoryId, "category id is null!");
        Assert.isTrue(null!= offset && offset >= 0, "please proivde positive offset value");
        Assert.isTrue(null!= max && max >= 0, "please proivde positive max value");
        SQLQuery sqlQuery = (SQLQuery) getSession().createSQLQuery(FIND_ASSETS_BY_CATEGORY_AND_STATUS_SQL)
                .setParameter("categoryId", categoryId)
                .setParameter("statusId", statusId)
                .setFirstResult(offset)
                .setMaxResults(max);
        return sqlQuery.list();
    }

    private Criteria buildAssetCategoryCriteria(Integer categoryId, Integer offset, Integer max) {
        Assert.notNull(categoryId, "category id is null!");
        Assert.isTrue(null!= offset && offset >= 0, "please proivde positive offset value");
        Assert.isTrue(null!= max && max >= 0, "please proivde positive max value");
        return getSession().createCriteria(AssetDTO.class)
                .createAlias("item", "i")
                .createAlias("i.itemTypes", "itemTypes")
                .add(Restrictions.eq("itemTypes.id", categoryId))
                .add(Restrictions.eq("deleted", 0))
                .addOrder(Order.asc("id"))
                .setFirstResult(offset)
                .setMaxResults(max);
    }

    public List<AssetDTO> findAssetByMetaFieldValue(Integer entityId, String name, String value){

        Criteria criteria = getSession().createCriteria(AssetDTO.class)
                .createAlias("metaFields", "metaFieldValue")
                .createAlias("metaFieldValue.field", "metaField")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("metaField.name", name))
                .add(Restrictions.sqlRestriction("string_value =  ?", value, StringType.INSTANCE));

        return criteria.list();
    }

    public List<AssetDTO> findAssetsForOrderChanges(List<Integer> assetIds){
        Criteria criteria = getSession().createCriteria(AssetDTO.class)
                .add(Restrictions.in("id", assetIds));
        return criteria.list();
    }

    /**
     * List the asset for the given asset identifier.
     *
     * @param identifier
     * @return
     */
    public List<AssetDTO> findAssetsByIdentifier(String identifier) {
        Query query = getSession().getNamedQuery("AssetDTO.getForIdentifier");
        query.setString("identifier", identifier);
        return query.list();
    }

}
