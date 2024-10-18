package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDependencyDTOEx;
import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeBL;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.PlanItemBL;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDependencyDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDAS;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.strategy.RouteBasedRateCardPricingStrategy;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.tools.JArrays;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by vivek on 15/11/14.
 */
public class PlanCopyTask extends AbstractCopyTask {
    PlanDAS planDAS = null;
    ItemDAS itemDAS = null;
    PlanItemDAS planItemDAS = null;
    OrderPeriodDAS orderPeriodDAS = null;
    CompanyDAS companyDAS = null;
    CategoryCopyTask categoryCopyTask = null;
    ProductCopyTask productCopyTask = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PlanCopyTask.class));

    private static final Class dependencies[] = new Class[]{
        OrderPeriodCopyTask.class,
        UsagePoolCopyTask.class
    };

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<ItemDTO> itemDTOs = itemDAS.findByEntityId(targetEntityId);
        return itemDTOs != null && !itemDTOs.isEmpty();
    }

    public PlanCopyTask() {
        init();
    }

    private void init() {
        planDAS = new PlanDAS();
        itemDAS = new ItemDAS();
        planItemDAS = new PlanItemDAS();
        orderPeriodDAS = new OrderPeriodDAS();
        companyDAS = new CompanyDAS();
        categoryCopyTask = new CategoryCopyTask();
        productCopyTask = new ProductCopyTask();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create PlanCopyTask ");
        List<PlanDTO> planDTOs = planDAS.findNonGlobalPlan(entityId);
        Map<Integer, Integer> oldNewItemMap = CopyCompanyUtils.oldNewItemMap;
        for (PlanDTO planDTO : planDTOs) {
            //create item
            planDAS.reattach(planDTO);

//            System.out.println(planDTO.getUsagePools());/*TODO: Usage pool still not copied in plans for new entity(Company)*/

            PlanWS planWS = PlanBL.getWS(planDTO);
            addUsagePool(planWS, planDTO);
            planWS.setId(0);
            //change the map key
            if (planWS.getMetaFieldsMap().containsKey(entityId)) {
                planWS.getMetaFieldsMap().put(targetEntityId, planWS.getMetaFieldsMap().remove(entityId));
            }

            for (ItemTypeDTO itemTypeDTO : planDTO.getItem().getItemTypes()) {
                if (!CopyCompanyUtils.oldNewCategoryMap.containsKey(itemTypeDTO.getId())) {
                    categoryCopyTask.createItemCategory(itemTypeDTO, targetEntityId);
                }
            }

            planWS.setItemId(productCopyTask.copyProduct(planDTO.getItem(), entityId, targetEntityId));

            List<PlanItemWS> planItemWSes = new ArrayList<PlanItemWS>();
            for (PlanItemDTO planItemDTO : planDTO.getPlanItems()) {
                planItemDAS.reattach(planItemDTO);
                PlanItemWS planItemWS = PlanItemBL.getWS(planItemDTO);
                planItemWS.setId(0);

                if (CopyCompanyUtils.oldNewItemMap.containsKey(planItemDTO.getItem().getId())) {
                    planItemWS.setItemId(CopyCompanyUtils.oldNewItemMap.get(planItemDTO.getItem().getId()));
                } else {
                    planItemWS.setItemId(productCopyTask.copyProduct(planItemDTO.getItem(), entityId, targetEntityId));
                }

                if (planItemWS.getModels() != null && !planItemWS.getModels().isEmpty()) {
                    for (Date date : planItemWS.getModels().keySet()) {
                        planItemWS.getModels().get(date).setId(0);
                        PriceModelWS priceModelWS = planItemWS.getModels().get(date);
                        if(priceModelWS.getAttributes()!=null && priceModelWS.getAttributes().size()!=0){
                            List<String> routeIds  = new LinkedList<String>();
                            priceModelWS.getAttributes().keySet()
                                                        .stream()
                                                        .filter(p->p.equals(RouteBasedRateCardPricingStrategy.PARAM_ROUTE_RATE_CARD_ID))
                                                        .map(p->p)
                                                        .forEach(p->routeIds.add(""+p));
                            for(String name : routeIds){
                                Integer id = CopyCompanyUtils.oldNewRouteRateCardMap.get(Integer.parseInt(priceModelWS.getAttributes().get(name)));
                                priceModelWS.getAttributes().put(name, id+"");
                            }
                        }
                    }
                }
                if (planItemWS.getBundle() != null) {
                    planItemWS.getBundle().setId(0);
                }

                planItemWSes.add(planItemWS);
            }
            planWS.setPlanItems(planItemWSes);
            OrderPeriodDTO tempOrderPeriodDTO = planDTO.getPeriod();
            orderPeriodDAS.reattach(tempOrderPeriodDTO);
            OrderPeriodDTO orderPeriodDTO = orderPeriodDAS.findOrderPeriod(targetEntityId, tempOrderPeriodDTO.getValue(), tempOrderPeriodDTO.getUnitId());
            planWS.setPeriodId(orderPeriodDTO.getId());
            for (int index : planWS.getMetaFieldsMap().keySet()) {
                for (int i = 0; i < planWS.getMetaFieldsMap().get(index).length; i++) {
                    planWS.getMetaFieldsMap().get(index)[i].setId(0);
                }
            }

            PlanDTO copyPlanDTO = PlanBL.getDTO(planWS);
            Integer planId = new PlanBL().create(copyPlanDTO);
            copyPlanDTO = planDAS.find(planId);

            oldNewItemMap.put(planDTO.getItemId(), copyPlanDTO.getItemId());
            CopyCompanyUtils.oldNewPlanItemMap.put(planDTO.getId(), copyPlanDTO.getId());
        }
        LOG.debug("Plan coping has been completed successfully.");
    }

    public Integer createItem(ItemDTOEx item, Integer targetEntityId) throws SessionInternalError {

        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
        // Get all descriptions to save-delete them afterwards.
        List<InternationalDescriptionWS> descriptions = item.getDescriptions();

        validateItem(item, targetEntityId);

        ItemBL itemBL = new ItemBL();

        // Set the creator entity id before creating the DTO object.
        item.setEntityId(targetEntityId);

        ItemDTO dto = itemBL.getDTO(item);

        // Set description to null
        dto.setDescription(null);

        // get the info from the caller
        Integer languageId = targetEntity.getLanguageId();

        validateAssetManagementForItem(dto, null);

        // call the creation
        Integer id = null;

        dto.setGlobal(item.isGlobal());

        id = itemBL.create(dto, languageId);

        dto = itemBL.getEntity();

        // save-delete descriptions
        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() != null && description.getContent() != null) {
                if (description.isDeleted()) {
                    dto.deleteDescription(description.getLanguageId());
                } else {
                    dto.setDescription(description.getContent(), description.getLanguageId());
                }
            }
        }
        return id;
    }

    /**
     * Validate an ItemDTOEx before saving
     *
     * @param item
     */
    private void validateItem(ItemDTOEx item, Integer targetEntityId) {

        //item may be shared - company hierarchies
        if (item.isGlobal()) {
            if (null == item.getEntityId()) {
                item.setEntityId(targetEntityId);
            }
            item.setEntities(Collections.<Integer>emptyList());
        } else {
            if (CollectionUtils.isEmpty(item.getEntities())) {
                List<Integer> list = new ArrayList<Integer>(1);
                list.add(targetEntityId);
                item.setEntities(list);
            }
        }

        Integer[] mandatoryItems = item.getMandatoryDependencyIdsOfType(ItemDependencyType.ITEM);
        validateItemMandatoryDependenciesCycle(item.getId(), JArrays.toArrayList(mandatoryItems));
        // check if all descriptions are to delete
        List<InternationalDescriptionWS> descriptions = item.getDescriptions();
        boolean noDescriptions = true;
        for (InternationalDescriptionWS description : descriptions) {
            if (!description.isDeleted()) {
                noDescriptions = false;
                break;
            }
        }
        if (noDescriptions) {
            throw new SessionInternalError("Must have a description", new String[]{
                    "ItemDTOEx,descriptions,validation.error.is.required"
            });
        }

        if (item.getOrderLineMetaFields() != null) {
            for (MetaFieldWS field : item.getOrderLineMetaFields()) {
                if (field.getDataType().equals(DataType.SCRIPT) &&
                        (null == field.getFilename() || field.getFilename().isEmpty())) {
                    throw new SessionInternalError("Script Meta Fields must define filename", new String[]{
                            "ItemDTOEx,orderLineMetaFields,product.validation.orderLineMetaFields.script.no.file," + field.getName()
                    });
                }
            }
        }

        //validate dependency quantities
        ItemDependencyDTOEx[] dependencies = item.getDependencies();
        if (dependencies != null) {
            for (ItemDependencyDTOEx dependency : dependencies) {
                if (dependency.getMaximum() != null) {
                    if (dependency.getMaximum() < dependency.getMinimum()) {
                        throw new SessionInternalError("Maximum quantity must be more than minimum", new String[]{
                                "ItemDTOEx,dependencies,product.validation.dependencies.max.lessthan.min"
                        });
                    }
                }
            }
        }
    }

        /*
     *   Asset Helper Methods
     */

    /**
     * Validations for asset manager type linked to the item.
     * Do the following checks
     * - if the item allows asset management, it must be linked to a category which allows asset management
     * - item may never be linked to more than 1 category allowing asset management
     * - if assets are already linked to this item, the type allowing asset management may not be removed or changed.
     *
     * @param newDto - with changes applied
     * @param oldDto - currrent persistent object
     */
    private void validateAssetManagementForItem(ItemDTO newDto, ItemDTO oldDto) {
        List<Integer> assetManagementTypes = extractAssetManagementTypes(newDto.getTypes());

        //if the item allows asset management, it must be linked to one category which allows asset management
        if (newDto.getAssetManagementEnabled() == 1) {
            if (assetManagementTypes.size() < 1) {
                throw new SessionInternalError("Product must belong to a category which allows asset management", new String[]{
                        "ItemDTOEx,types,product.validation.no.assetmanagement.type.error"
                });
            }
        }
        //only 1 asset management type allowed
        if (assetManagementTypes.size() > 1) {
            throw new SessionInternalError("Product belongs to more than one category which allows asset management", new String[]{
                    "ItemDTOEx,types,product.validation.multiple.assetmanagement.types.error"
            });
        }

        //checks only if this is an update
        if (oldDto != null) {
            //in the current persisted object, find the item type which allows asset management
            Integer currentAssetManagementType = null;
            for (ItemTypeDTO typeDTO : oldDto.getItemTypes()) {
                if (typeDTO.getAllowAssetManagement() == 1) {
                    currentAssetManagementType = typeDTO.getId();
                    break;
                }
            }

            if (currentAssetManagementType != null) {
                int assetCount = new AssetBL().countAssetsForItem(oldDto.getId());
                if (assetCount > 0) {
                    //asset management type may not be removed
                    if (assetManagementTypes.isEmpty())
                        throw new SessionInternalError("Asset management category may not be removed", new String[]{
                                "ItemDTOEx,types,product.validation.assetmanagement.removed.error"
                        });

                    //asset management type may not be changed
                    if (!currentAssetManagementType.equals(assetManagementTypes.get(0)))
                        throw new SessionInternalError("Asset management category may not be changed", new String[]{
                                "ItemDTOEx,types,product.validation.assetmanagement.changed.error"
                        });
                }
            }
        }
    }

    /**
     * Extract all ItemTypes which allows asset management from the list of provided ItemType ids.
     * This method loads all the ItemTypes for the provded ids and checks if they allow asset management.
     * The ones that do will be returned.
     *
     * @param types - ItemType ids
     * @return Ids of ItemTypes allowing asset management.
     */
    private List<Integer> extractAssetManagementTypes(Integer[] types) {
        List<Integer> typeIds = new ArrayList<Integer>(2);

        ItemTypeBL itemTypeBL = new ItemTypeBL();
        for (Integer typeId : types) {
            itemTypeBL.set(typeId);
            ItemTypeDTO itemTypeDTO = itemTypeBL.getEntity();
            if (itemTypeDTO.getAllowAssetManagement() == 1) {
                typeIds.add(typeId);
            }
        }
        return typeIds;
    }

    private void validateItemMandatoryDependenciesCycle(Integer rootItemId, Collection<Integer> dependencies) {
        if (dependencies == null || dependencies.isEmpty() || rootItemId == null) return;
        if (dependencies.contains(rootItemId)) {
            String errorCode = "ItemDTOEx,mandatoryItems,product.error.dependencies.cycle";
            throw new SessionInternalError("Cycle in product mandatory dependencies was found",
                    new String[]{errorCode});
        }
        for (Integer dependentItemId : dependencies) {
            ItemDTO item = itemDAS.find(dependentItemId);
            if (item != null && item.getDependencies() != null && !item.getDependencies().isEmpty()) {
                List<Integer> childDependencies = new LinkedList<Integer>();
                for (ItemDependencyDTO dependencyDTO : item.getDependencies()) {
                    if (dependencyDTO.getType().equals(ItemDependencyType.ITEM) && dependencyDTO.getMinimum() > 0) {
                        childDependencies.add(dependencyDTO.getDependentObjectId());
                    }
                }
                validateItemMandatoryDependenciesCycle(rootItemId, childDependencies);
            }
        }
    }

    private void addUsagePool(PlanWS planWS, PlanDTO planDTO) {

        Integer[] usagePools = new Integer[planDTO.getUsagePools().size()];
        int index = 0;
        for (UsagePoolDTO usagePoolDTO : planDTO.getUsagePools()) {
            usagePools[index++] = CopyCompanyUtils.oldNewUsagePoolMap.get(usagePoolDTO.getId());
        }
        planWS.setUsagePoolIds(usagePools);
    }
}
