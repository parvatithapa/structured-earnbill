package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.item.AssetStatusBL;
import com.sapienter.jbilling.server.item.ItemTypeBL;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.db.AssetStatusDAS;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by vivek on 6/11/14.
 */
public class CategoryCopyTask extends AbstractCopyTask {

    ItemDAS itemDAS = null;
    CompanyDAS companyDAS = null;
    AssetStatusDAS assetStatusDAS = null;
    ItemTypeDAS itemTypeDAS = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CategoryCopyTask.class));

    private static final Class dependencies[] = new Class[]{
            MetaFieldsCopyTask.class
    };

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<ItemTypeDTO> itemTypeDTOs = itemTypeDAS.findByEntityId(targetEntityId);
        return itemTypeDTOs != null && !itemTypeDTOs.isEmpty();
    }

    public CategoryCopyTask() {
        init();
    }

    private void init() {
        itemDAS = new ItemDAS();
        companyDAS = new CompanyDAS();
        assetStatusDAS = new AssetStatusDAS();
        itemTypeDAS = new ItemTypeDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create CategoryCopyTask");
        copyCategories(entityId, targetEntityId);

    }

    public void copyCategories(Integer entityId, Integer targetEntityId) {
        List<ItemTypeDTO> itemTypeDTOList = itemTypeDAS.findByEntityId(entityId);
        for (ItemTypeDTO itemTypeDTO : itemTypeDTOList) {
            if (!itemTypeDTO.isGlobal()) {
                createItemCategory(itemTypeDTO, targetEntityId);
            }
        }
        LOG.debug("Category Task has been completed.");
    }

    public ItemTypeDTO createItemCategory(ItemTypeDTO itemTypeDTO, Integer targetEntityId) throws SessionInternalError {
        itemTypeDAS.reattach(itemTypeDTO);
        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
        ItemTypeWS itemType = ItemTypeBL.toWS(itemTypeDTO);

        itemType.setEntityId(targetEntityId);
        itemType.setId(0);

        for (AssetStatusDTO statusDTO : itemTypeDTO.getAssetStatuses()) {
            statusDTO.setId(0);
        }

        if (itemType.getAssetMetaFields() != null) {
            for (MetaFieldWS field : itemType.getAssetMetaFields()) {
                field.setId(0);
                field.setEntityId(targetEntityId);

                if (field.getDataType().equals(DataType.SCRIPT) &&
                        (null == field.getFilename() || field.getFilename().isEmpty())) {
                    throw new SessionInternalError("Script Meta Fields must define filename", new String[]{
                            "ItemTypeWS,assetMetaFields,metafield.validation.filename.required"
                    });
                }
            }
        }

        Integer entityId = targetEntityId;
        if (itemType.getEntities().contains(entityId)) {
            itemType.getEntities().remove(entityId);
        }

        if (!itemType.isGlobal() && CollectionUtils.isEmpty(itemType.getEntities())) {
            ArrayList ents = new ArrayList();
            ents.add(entityId);
            itemType.setEntities(ents);
        }

        AssetStatusBL assetStatusBL = new AssetStatusBL();

        ItemTypeDTO dto = new ItemTypeDTO();
        dto.setDescription(itemType.getDescription());
        dto.setOrderLineTypeId(itemType.getOrderLineTypeId());
        dto.setParent(itemTypeDAS.find(itemType.getParentItemTypeId()));
        dto.setGlobal(itemType.isGlobal());
        dto.setOnePerOrder(itemType.isOnePerOrder());
        dto.setOnePerCustomer(itemType.isOnePerCustomer());
        dto.setInternal(itemType.isInternal());
        dto.setEntity(targetEntity);

        List<Integer> entities = new ArrayList<Integer>(0);

        if (!itemType.isGlobal()) {
            entities.addAll(itemType.getEntities());
        }

        dto.setEntities(convertToCompanyDTO(itemType.getEntities()));
        dto.setAllowAssetManagement(itemType.getAllowAssetManagement());
        dto.setAssetIdentifierLabel(itemType.getAssetIdentifierLabel());
        dto.setAssetStatuses(assetStatusBL.convertAssetStatusDTOExes(itemType.getAssetStatuses()));
        // Assign asset meta fields to the company that created the category.
        dto.setAssetMetaFields(MetaFieldBL.convertMetaFieldsToDTO(itemType.getAssetMetaFields(), targetEntityId));

        validateAssetMetaFields(new HashSet<>(0), dto.getAssetMetaFields());
        validateItemCategoryStatuses(dto);

        entities = new ArrayList<>(0);
        entities.add(targetEntityId);
        entities.addAll(companyDAS.getChildEntitiesIds(targetEntityId));

        ItemTypeBL itemTypeBL = new ItemTypeBL();
        itemTypeBL.setCallerCompanyId(targetEntityId);
        // a subscription product must allow asset management
        if (dto.getOrderLineTypeId() == Constants.ORDER_LINE_TYPE_SUBSCRIPTION && dto.getAllowAssetManagement() != 1) {
            throw new SessionInternalError("Subscription product category must allow asset management",
                    new String[]{"ItemTypeWS,allowAssetManagement,validation.error.subscription.category.asset.management"});
        }

        itemTypeBL.create(dto);
        dto = itemTypeBL.getEntity();

        //we need ids to create descriptions. Can only do it after flush
        for (AssetStatusDTO statusDTO : itemTypeBL.getEntity().getAssetStatuses()) {
            statusDTO.setDescription(statusDTO.getDescription(), Constants.LANGUAGE_ENGLISH_ID);
            AssetStatusDTO oldAssetStatusDTO = assetStatusDAS.findStatusByItemType(itemTypeDTO.getId(), statusDTO.getIsDefault(), statusDTO.getIsOrderSaved(), statusDTO.getIsAvailable(), statusDTO.getIsInternal(), statusDTO.getIsActive(), statusDTO.getIsPending());
            AssetStatusDTO copyAssetStatusDTO = assetStatusDAS.findStatusByItemType(dto.getId(), statusDTO.getIsDefault(), statusDTO.getIsOrderSaved(), statusDTO.getIsAvailable(), statusDTO.getIsInternal(), statusDTO.getIsActive(), statusDTO.getIsPending());
            CopyCompanyUtils.oldNewAssetStatusMap.put(oldAssetStatusDTO.getId(), copyAssetStatusDTO.getId());
        }

        CopyCompanyUtils.oldNewCategoryMap.put(itemTypeDTO.getId(), dto.getId());

        return itemTypeBL.getEntity();
    }

    private Set<CompanyDTO> convertToCompanyDTO(List<Integer> entities) {
        Set<CompanyDTO> childEntities = new HashSet<CompanyDTO>(0);

        for (Integer entity : entities) {
            childEntities.add(companyDAS.find(entity));
        }

        return childEntities;
    }

    private void validateAssetMetaFields(Collection<MetaField> currentMetaFields, Collection<MetaField> newMetaFields) throws SessionInternalError {
        MetaFieldBL metaFieldBL = new MetaFieldBL();
        Map currentMetaFieldMap = new HashMap(currentMetaFields.size() * 2);
        Set names = new HashSet(currentMetaFields.size() * 2);

        //collect the current meta fields
        for (MetaField dto : currentMetaFields) {
            currentMetaFieldMap.put(dto.getId(), dto);
        }

        //loop through the new metaFields
        for (MetaField metaField : newMetaFields) {
            if (names.contains(metaField.getName())) {
                throw new SessionInternalError("Meta field names must be unique [" + metaField.getName() + "]", new String[]
                        {"MetaFieldWS,name,metaField.validation.name.unique," + metaField.getName()});
            }
            names.add(metaField.getName());

            //if it is already in the DB validate the changes
            if (metaField.getId() > 0) {
                MetaField currentMetaField = (MetaField) currentMetaFieldMap.get(metaField.getId());

                //if the type change we have to make sure it is not already used
                boolean checkUsage = currentMetaField != null && !currentMetaField.getDataType().equals(metaField.getDataType());
                if (checkUsage && MetaFieldBL.isMetaFieldUsed(EntityType.ASSET, metaField.getId())) {
                    throw new SessionInternalError("Data Type may not be changes is meta field is used [" + metaField.getName() + "]", new String[]
                            {"MetaFieldWS,dataType,metaField.validation.type.change.not.allowed," + metaField.getName()});
                }
            }
        }
    }

    /**
     * Check that the type has only one status which is 'default' and one which has 'order saved' checked.
     * Check that status names are unique
     */
    private void validateItemCategoryStatuses(ItemTypeDTO dto) throws SessionInternalError {
        //no need to do further checking if the type doesn't allow asset management
        if (dto.getAllowAssetManagement() == 0) {
            return;
        }

        //status names must be unique
        Set<String> statusNames = new HashSet<String>(dto.getAssetStatuses().size() * 2);
        //list of errors found
        List<String> errors = new ArrayList<String>(2);

        //keep count of the number of 'default' and 'order create' statuses
        int defaultCount = 0;
        int createOrderCount = 0;
        int activeCount = 0;
        int pendingCount = 0;

        for (AssetStatusDTO statusDTO : dto.getAssetStatuses()) {
            if (statusDTO.getDeleted() == 0) {
                if (statusDTO.getIsDefault() == 1) {
                    defaultCount++;
                }
                if (statusDTO.getIsOrderSaved() == 1) {
                    createOrderCount++;
                }
                if(statusDTO.getIsPending() == 1){
                    pendingCount++;
                }
                if(statusDTO.getIsActive() == 1){
                    activeCount++;
                }
                if (statusDTO.getIsAvailable() == 1
                        && statusDTO.getIsPending() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.available.pending");
                }
                if (statusDTO.getIsActive() == 1
                        && statusDTO.getIsOrderSaved() == 0) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.active.not.ordersaved");
                }
                if (statusDTO.getIsPending() == 1
                        && statusDTO.getIsOrderSaved() == 0) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.pending.not.ordersaved");
                }
                if (statusDTO.getIsAvailable() == 1
                        && statusDTO.getIsActive() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.available.active");
                }
                if (statusDTO.getIsDefault() == 1
                        && statusDTO.getIsActive() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.default.active");
                }
                if (statusDTO.getIsDefault() == 1
                        && statusDTO.getIsPending() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.default.pending");
                }
                if (statusDTO.getIsActive() == 1
                        && statusDTO.getIsPending() == 1) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.both.active.pending");
                }
                if (statusDTO.getIsOrderSaved() == 1
                        && statusDTO.getIsPending() == 0 && statusDTO.getIsActive() == 0){
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.ordersaved.pending.active");
                }
                if (statusNames.contains(statusDTO.getDescription())) {
                    errors.add("ItemTypeWS,statuses,validation.error.category.status.description.unique," + statusDTO.getDescription());
                } else {
                    statusNames.add(statusDTO.getDescription());
                }
            }
        }

        if (defaultCount != 1) {
            errors.add("ItemTypeWS,statuses,validation.error.category.status.default.one");
        }

        if (0 == createOrderCount) {
            errors.add("ItemTypeWS,statuses,validation.error.category.status.order.saved.one");
        }

        if(1 != pendingCount){
            errors.add("ItemTypeWS,statuses,validation.error.category.status.pending.one");
        }

        if(1 != activeCount){
            errors.add("ItemTypeWS,statuses,validation.error.category.status.active.one");
        }

        if (errors.size() > 0) {
            throw new SessionInternalError("Category Status validation failed.", errors.toArray(new String[errors.size()]));
        }
    }
}
