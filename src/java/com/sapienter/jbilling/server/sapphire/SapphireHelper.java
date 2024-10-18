package com.sapienter.jbilling.server.sapphire;

import static com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants.ASSET_PREFIX;
import static com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants.ENROLL_SUBSC_ORDER_STATUS_ID_META_FIELD_NAME;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

public class SapphireHelper {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SapphireHelper() {
        throw new IllegalStateException("Non instantiable class");
    }

    public static void setOrderStatusAsPending(OrderWS order, Integer entityId) {
        //Monthly subscription order status should be in "Pending" state when it's created.
        String orderStatusId = new MetaFieldDAS().getComapanyLevelMetaFieldValue(ENROLL_SUBSC_ORDER_STATUS_ID_META_FIELD_NAME, entityId);
        OrderStatusWS orderStatusWS = new OrderStatusWS();
        orderStatusWS.setId((null != orderStatusId) ? Integer.valueOf(orderStatusId) : null);
        order.setOrderStatusWS(orderStatusWS);
    }

    public static List<Integer> getInventoryAllocationProductIds(Map<String, String> params) {
        String itemIds = params.get(SapphireSignupConstants.PRODUCT_IDS_INVENTORY_ALLOCATION);
        if(StringUtils.isEmpty(itemIds)) {
            return Collections.emptyList();
        }
        return Arrays.stream(itemIds.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public static List<PlanItemDTO> getAssetEnabledPlanItemsFromSubscriptionItem(Integer subscriptionItemId) {
        List<PlanItemDTO> results = new ArrayList<>();
        PlanDTO plan = new PlanDAS().findPlanByItemId(subscriptionItemId);
        for(PlanItemDTO planItem : plan.getPlanItems()) {
            if(planItem.getItem().isAssetEnabledItem()) {
                results.add(planItem);
            }
        }
        return results;
    }

    public static List<Integer> collecteItemIdsFromPlanItems(List<PlanItemDTO> planItems) {
        if(CollectionUtils.isEmpty(planItems)) {
            return Collections.emptyList();
        }
        return planItems.stream()
                .map(planItem -> planItem.getItem().getId())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static List<AssetDTO> findAssetByItemWithLock(Integer entityId, Integer itemId, Integer maxResult) {
        SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
        Session session = sf.getCurrentSession();
        ItemTypeDTO category = new ItemDAS().find(itemId).findItemTypeWithAssetManagement();
        Integer assetStatusId = category.getAssetStatuses()
                .stream()
                .filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1)
                .collect(Collectors.toList())
                .get(0)
                .getId();

        return session.createCriteria(AssetDTO.class)
                .createAlias("item", "i")
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("i.id", itemId))
                .add(Restrictions.eq("assetStatus.id", assetStatusId))
                .add(Restrictions.eq("deleted", 0))
                .setLockMode(LockMode.PESSIMISTIC_WRITE) // used PESSIMISTIC lock for synchronization
                .setMaxResults(maxResult)
                .list();
    }

    /**
     * Creates Dummy asset for given {@link ItemDTO}
     * @param item
     * @return
     */
    public static Integer createAsset(Integer itemId) {
        IWebServicesSessionBean api = Context.getBean(Name.WEB_SERVICES_SESSION);
        ItemDTO item = new ItemDAS().find(itemId);
        ItemTypeDTO category = item.findItemTypeWithAssetManagement();
        Integer assetStatusId = category.getAssetStatuses()
                .stream()
                .filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1)
                .collect(Collectors.toList())
                .get(0)
                .getId();
        Integer entityId = api.getCallerCompanyId();
        AssetWS asset = new AssetWS();
        asset.setAssetStatusId(assetStatusId);
        asset.setItemId(item.getId());
        asset.setGlobal(item.isGlobal());
        Random random = new Random(System.nanoTime() % 100000);
        asset.setIdentifier(ASSET_PREFIX + "-"+ random.nextInt(1000000000));
        asset.setEntities(Arrays.asList(entityId));
        asset.setEntityId(entityId);
        asset.setId(api.createAsset(asset));
        logger.debug("Asset created {} with id {}", asset, asset.getId());
        return asset.getId();
    }
}
