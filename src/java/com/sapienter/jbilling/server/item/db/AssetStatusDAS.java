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

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * @author Gerhard
 * @since 15/04/13
 */
public class AssetStatusDAS extends AbstractDAS<AssetStatusDTO> {

    /**
     * Lists all non deleted statuses for the given category.
     *
     * @param categoryId
     * @param includeInternal  Must internal statuses be included in the results
     * @return
     */
    public List<AssetStatusDTO> getStatuses(int categoryId, boolean includeInternal ) {
        Query query = getSession().getNamedQuery(includeInternal ? "AssetStatusDTO.findForItemType" : "AssetStatusDTO.findForItemTypeNotInternal");
        query.setParameter("item_type_id", categoryId);
        return query.list();
    }

    /**
     * Find the default status for the ItemTypeDTO (which allows asset management) linked to the item.
     *
     * @param itemId    ItemDTO id
     * @return
     */
    public AssetStatusDTO findDefaultStatusForItem(int itemId) {
        Query query = getSession().getNamedQuery("AssetStatusDTO.findDefaultStatusForItem");
        query.setParameter("item_id", itemId);
        return (AssetStatusDTO) query.uniqueResult();
    }
    
    /**
     * Find the order_finished status for the ItemTypeDTO (which allows asset management) linked to the item.
     *
     * @param itemId    ItemDTO id
     * @return
     */
    public AssetStatusDTO findOrderFinishedStatusForItem(int itemId) {
        Query query = getSession().getNamedQuery("AssetStatusDTO.findOrderFinishedStatusForItem");
        query.setParameter("item_id", itemId);
        return (AssetStatusDTO) query.uniqueResult();
    }

    /**
     * Find the available status for the ItemTypeDTO (which allows asset management) linked to the item.
     * @param itemId    ItemDTO id
     * @return
     */
    public AssetStatusDTO findAvailableStatusForItem(int itemId) {
        Query query = getSession().getNamedQuery("AssetStatusDTO.findAvailableStatusForItem");
        query.setParameter("item_id", itemId);
        return (AssetStatusDTO) query.uniqueResult();
    }

    /**
     * Find the orderSaved status for the ItemTypeDTO (which allows asset management) linked to the item.
     * @param itemId    ItemDTO id
     * @return
     */
    public AssetStatusDTO findOrderSavedStatusForItem(int itemId) {
        Query query = getSession().getNamedQuery("AssetStatusDTO.findOrderSavedStatusForItem");
        query.setParameter("item_id", itemId);
        return (AssetStatusDTO) query.uniqueResult();
    }

    /**
     * Find the active status for the ItemTypeDTO (which allows asset management) linked to the item.
     * @param itemId    ItemDTO id
     * @return
     */
    public AssetStatusDTO findActiveStatusForItem(int itemId) {
        Query query = getSession().getNamedQuery("AssetStatusDTO.findActiveStatusForItem");
        query.setParameter("item_id", itemId);
        return (AssetStatusDTO) query.uniqueResult();
    }

    /**
     * Find the pending status for the ItemTypeDTO (which allows asset management) linked to the item.
     * @param itemId    ItemDTO id
     * @return
     */
    public AssetStatusDTO findPendingStatusForItem(int itemId) {
        Query query = getSession().getNamedQuery("AssetStatusDTO.findPendingStatusForItem");
        query.setParameter("item_id", itemId);
        return (AssetStatusDTO) query.uniqueResult();
    }

    public AssetStatusDTO findStatusByItemType(Integer itemTypeId, Integer isDefault, Integer isOrderSaved, Integer isAvailable, Integer isInternal, Integer isActive, Integer isPending) {
        Criteria criteria = getSession().createCriteria(AssetStatusDTO.class)
                .createAlias("itemType", "itemType")
                .add(Restrictions.eq("itemType.id", itemTypeId))
                .add(Restrictions.eq("isDefault", isDefault))
                .add(Restrictions.eq("isOrderSaved", isOrderSaved))
                .add(Restrictions.eq("isAvailable", isAvailable))
                .add(Restrictions.eq("isInternal", isInternal))
                .add(Restrictions.eq("isActive", isActive))
                .add(Restrictions.eq("isPending", isPending))
                .add(Restrictions.eq("deleted", 0))
                .setMaxResults(1);

        return findFirst(criteria);
    }
}
