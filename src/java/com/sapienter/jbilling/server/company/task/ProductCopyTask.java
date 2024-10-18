package com.sapienter.jbilling.server.company.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemDependencyDTOEx;
import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.item.ItemTypeBL;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDAS;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.EntityItemPrice;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemDependencyDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.strategy.RouteBasedRateCardPricingStrategy;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.tools.JArrays;

/**
 * Created by vivek on 15/11/14.
 */
public class ProductCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ProductCopyTask.class));

    ItemTypeDAS itemTypeDAS = null;
    ItemDAS itemDAS = null;
    MetaFieldDAS metaFieldDAS = null;
    CompanyDAS companyDAS = null;
    AssetDAS assetDAS = null;
    AssetStatusDAS assetStatusDAS = null;

    private static final Class dependencies[] = new Class[]{
        EnumerationCopyTask.class,
        CategoryCopyTask.class
    };

    public Class[] getDependencies() {
        return dependencies;
    }

    public ProductCopyTask() {
        init();
    }

    void init() {
        itemTypeDAS = new ItemTypeDAS();
        itemDAS = new ItemDAS();
        metaFieldDAS = new MetaFieldDAS();
        companyDAS = new CompanyDAS();
        assetDAS = new AssetDAS();
        assetStatusDAS = new AssetStatusDAS();
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<ItemDTO> itemDTOs = itemDAS.findByEntityId(targetEntityId);
        return itemDTOs != null && !itemDTOs.isEmpty();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create ProductCopyTask ");
        List<ItemDTO> itemDTOs = itemDAS.findNonGlobalItemsForCopyProduct(entityId);

        for (ItemDTO itemDTO : itemDTOs) {
            copyProduct(itemDTO, entityId, targetEntityId);
        }

        LOG.debug("Product copy task has been completed.");
    }

    public void setAssetIdsForItem(Integer newProductId, ItemDTO oldProduct, Integer entityId, Integer targetEntityId) {
        List<AssetDTO> assetDTOs = assetDAS.findAssetByProductCode(oldProduct.getInternalNumber(), entityId);
        if (!assetDTOs.isEmpty()) {
            for (AssetDTO assetDTO : assetDTOs) {
                AssetWS assetWS = AssetBL.getWS(assetDTO);
                assetWS.setId(0);
                assetWS.setOrderLineId(null);
                assetWS.setProvisioningCommands(new ProvisioningCommandWS[0]);
                assetWS.setEntityId(targetEntityId);
                assetWS.setItemId(newProductId);


                if (assetWS.getAssetStatusId() != null) {
                    AssetStatusDTO assetStatusDTO = assetStatusDAS.find(CopyCompanyUtils.oldNewAssetStatusMap.get(assetWS.getAssetStatusId()));
                    if (assetStatusDTO.getIsOrderSaved() == 1) {
                        assetStatusDTO = assetStatusDAS.findDefaultStatusForItem(newProductId);
                    }
                    assetWS.setAssetStatusId(assetStatusDTO.getId());
                }

                for (MetaFieldValueWS metaFieldValueWS : assetWS.getMetaFields()) {
                    metaFieldValueWS.setId(0);
                }
                IWebServicesSessionBean local = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
                Integer userId = local.getCallerId();
                Integer newAssetId = createAsset(assetWS, userId);
                CopyCompanyUtils.oldNewAssetMap.put(assetDTO.getId(), newAssetId);
            }
        }
    }

    public ItemDTOEx setItemDTOEx(ItemDTO itemDTO, Integer entityId, Integer targetEntityId) {

        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
        ItemDTOEx itemDTOEx = new ItemBL().getWS(itemDTO);
        itemDTOEx.setId(0);
        itemDTOEx.setEntityId(targetEntityId);
        itemDTOEx.getEntities().remove(entityId);
        itemDTOEx.getEntities().add(targetEntityId);
        itemDTOEx.setCurrencyId(targetEntity.getCurrencyId());

        //change map data
        itemDTOEx.getMetaFieldsMap().put(targetEntityId, itemDTOEx.getMetaFieldsMap().remove(entityId));

        for (MetaFieldValueWS metaFieldValueWS : itemDTOEx.getMetaFields()) {
            metaFieldValueWS.setId(0);
        }

        if(itemDTO.isPlan()) {
            itemDTOEx.setIsPlan(true);
        }

        for (MetaFieldWS metaFieldWS : itemDTOEx.getOrderLineMetaFields()) {
            metaFieldWS.setEntityId(targetEntityId);
            metaFieldWS.setId(0);
            metaFieldWS.setEntityId(targetEntityId);
        }

        for (ItemDependencyDTOEx itemDependencyDTOEx : itemDTOEx.getDependencies()) {
            itemDependencyDTOEx.setId(0);
        }

        SortedMap<Integer, MetaFieldValueWS[]> metaFieldMap = itemDTOEx.getMetaFieldsMap();
        SortedMap<Integer, MetaFieldValueWS[]> copyMetaFieldMap = new TreeMap<Integer, MetaFieldValueWS[]>();
        Iterator<Integer> iterator = itemDTOEx.getMetaFieldsMap().keySet().iterator();

        while (iterator.hasNext()) {
            Integer metaFieldMapId = iterator.next();
            Integer copyMetaFieldMapId;
            MetaFieldValueWS[] metaFieldValueWSes;
            if (entityId.equals(metaFieldMapId)) {
                metaFieldValueWSes = metaFieldMap.get(metaFieldMapId);
                copyMetaFieldMapId = targetEntityId;
            } else {
                metaFieldValueWSes = metaFieldMap.get(metaFieldMapId);
                copyMetaFieldMapId = metaFieldMapId;
                copyMetaFieldMap.put(metaFieldMapId, metaFieldMap.get(metaFieldMapId));
            }
            copyMetaFieldMap.put(copyMetaFieldMapId, metaFieldValueWSes);

            for (MetaFieldValueWS metaFieldValueWS : metaFieldValueWSes) {
                metaFieldValueWS.setId(0);
            }
        }

        itemDTOEx.setMetaFieldsMap(copyMetaFieldMap);
        itemDTOEx.setMetaFieldsMap(metaFieldMap);

        if (itemDTOEx.getDefaultPrice() != null) {
            itemDTOEx.getDefaultPrice().setId(0);
            while (itemDTOEx.getDefaultPrice().getNext() != null) {
                itemDTOEx.getDefaultPrice().getNext().setId(0);
            }
        }

        if (itemDTOEx.getDefaultPrices().size() > 0) {
            for (Date date : itemDTOEx.getDefaultPrices().keySet()) {
                itemDTOEx.getDefaultPrices().get(date).setId(null);
                itemDTOEx.getDefaultPrices().get(date).setCurrencyId(targetEntity.getCurrencyId());

                if (itemDTOEx.getDefaultPrices().get(date).getNext() != null) {
                    itemDTOEx.getDefaultPrices().get(date).getNext().setId(0);
                }
            }
        } else {
            Set<EntityItemPrice> entityItemPrices = itemDTO.getDefaultPrices();
            SortedMap<Date, PriceModelDTO> priceModelDTOSortedMap = new TreeMap<Date, PriceModelDTO>();
            SortedMap<Date, PriceModelWS> datePriceModelWSSortedMap = new TreeMap<Date, PriceModelWS>();
            for (EntityItemPrice entityItemPrice : entityItemPrices) {
                priceModelDTOSortedMap.putAll(entityItemPrice.getPrices());
            }

            for (Date date : priceModelDTOSortedMap.keySet()) {
                PriceModelWS priceModelWS = PriceModelBL.getWS(priceModelDTOSortedMap.get(date));
                priceModelWS.setId(null);
                if (priceModelWS.getNext() != null) {
                    priceModelWS.getNext().setId(null);
                }
                if(priceModelWS.getAttributes()!=null && priceModelWS.getAttributes().size()!=0){
                    if(priceModelWS.getAttributes()!=null && priceModelWS.getAttributes().size()!=0){
                        List<String> routeIds  = new LinkedList<String>();
                        priceModelWS.getAttributes().keySet().stream()
                                                             .filter(p->p.equals(RouteBasedRateCardPricingStrategy.PARAM_ROUTE_RATE_CARD_ID))
                                                             .map(p->p)
                                                             .forEach(p->routeIds.add(""+p));
                        for(String name : routeIds){
                            Integer id = CopyCompanyUtils.oldNewRouteRateCardMap.get(Integer.parseInt(priceModelWS.getAttributes().get(name)));
                            priceModelWS.getAttributes().put(name, id+"");
                        }
                    }
                }
                datePriceModelWSSortedMap.put(date, priceModelWS);
            }
            itemDTOEx.setDefaultPrices(datePriceModelWSSortedMap);
        }

        Integer[] copyItemTypeIds = new Integer[itemDTOEx.getTypes().length];
        int idx = 0;
        for (Integer itemTypeId : itemDTOEx.getTypes()) {
            copyItemTypeIds[idx++] = CopyCompanyUtils.oldNewCategoryMap.get(itemTypeId);
        }

        itemDTOEx.setTypes(copyItemTypeIds);
        return itemDTOEx;
    }

    /*
      * ITEM
      */
    public Integer createItem(ItemDTOEx item, Integer targetEntityId) throws SessionInternalError {
        // Get all descriptions to save-delete them afterwards.
        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
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
                    if (assetManagementTypes.isEmpty()) {
                        throw new SessionInternalError("Asset management category may not be removed", new String[]{
                                "ItemDTOEx,types,product.validation.assetmanagement.removed.error"
                        });
                    }

                    //asset management type may not be changed
                    if (!currentAssetManagementType.equals(assetManagementTypes.get(0))) {
                        throw new SessionInternalError("Asset management category may not be changed", new String[]{
                                "ItemDTOEx,types,product.validation.assetmanagement.changed.error"
                        });
                    }
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

    /*
     *  Asset
     */
    public Integer createAsset(AssetWS asset, Integer userId) throws SessionInternalError {
        AssetBL assetBL = new AssetBL();
        AssetDTO dto = assetBL.getDTO(asset);

        //set default meta field values and validate
        MetaFieldHelper.updateMetaFieldDefaultValuesWithValidation(dto.getEntity().getLanguageId(),
                dto.getItem().findItemTypeWithAssetManagement().getAssetMetaFields(), dto);

        //do validation
        checkItemAllowsAssetManagement(dto);
        assetBL.checkForDuplicateIdentifier(dto);
        checkOrderAndStatus(dto);
        assetBL.checkContainedAssets(dto.getContainedAssets(), 0);

        return assetBL.create(dto, userId);
    }

    /**
     * Check that the item linked to the asset allows asset management.
     *
     * @param dto
     * @throws SessionInternalError
     */
    private void checkItemAllowsAssetManagement(AssetDTO dto) throws SessionInternalError {
        if (dto.getItem().getAssetManagementEnabled() == 0) {
            throw new SessionInternalError("The item does not allow asset management", new String[]{"AssetWS,itemId,asset.validation.item.not.assetmanagement"});
        }
    }

    /**
     * If the asset belongs to an order, it must have a status of unavailable
     *
     * @param dto
     * @throws SessionInternalError
     */
    private void checkOrderAndStatus(AssetDTO dto) throws SessionInternalError {
        if (dto.getOrderLine() != null && dto.getAssetStatus().getIsAvailable() == 1) {
            throw new SessionInternalError("An asset belonging to an order must have an unavailable status", new String[]{"AssetWS,assetStatus,asset.validation.status.not.unavailable"});
        }
    }

    public Integer copyProduct(ItemDTO itemDTO, Integer entityId, Integer targetEntityId) {
        itemDAS.reattach(itemDTO);

        ItemDTOEx itemDTOEx = setItemDTOEx(itemDTO, entityId, targetEntityId);
        Integer itemId = createItem(itemDTOEx, targetEntityId);
        setAssetIdsForItem(itemId, itemDTO, entityId, targetEntityId);

        CopyCompanyUtils.oldNewItemMap.put(itemDTO.getId(), itemId);

        return itemId;
    }
}
