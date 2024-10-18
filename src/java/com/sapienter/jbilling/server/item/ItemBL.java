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

package com.sapienter.jbilling.server.item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.account.AccountTypeBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemDependencyDAS;
import com.sapienter.jbilling.server.item.db.ItemDependencyDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.item.db.RatingConfigurationDTO;
import com.sapienter.jbilling.server.item.event.ItemDeletedEvent;
import com.sapienter.jbilling.server.item.event.ItemUpdatedEvent;
import com.sapienter.jbilling.server.item.event.NewItemEvent;
import com.sapienter.jbilling.server.item.tasks.IPricing;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pricing.PriceContextDTO;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.tasks.PriceModelPricingTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;

public class ItemBL {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ItemBL.class));

    private ItemDAS itemDas = null;
    private ItemDTO item = null;
    private EventLogger eLogger = null;
    private String priceCurrencySymbol = null;
    private List<PricingField> pricingFields = null;

    public ItemBL(Integer itemId)
            throws SessionInternalError {
        try {
            init();
            set(itemId);
        } catch (Exception e) {
            throw new SessionInternalError("Setting item", ItemBL.class, e);
        }
    }

    public ItemBL() {
        init();
    }

    public ItemBL(ItemDTO item) {
        this.item = item;
        init();
    }

    public void set(Integer itemId) {
        item = itemDas.find(itemId);
    }

    private void init() {
        eLogger = EventLogger.getInstance();
        itemDas = new ItemDAS();
    }

    public ItemDTO getEntity() {
        return item;
    }

    public Integer create(ItemDTO dto, Integer languageId) {
        EntityBL entity = new EntityBL(dto.getEntityId());
        if (languageId == null) {
            languageId = entity.getEntity().getLanguageId();
        }

        if (dto.getHasDecimals() != null) {
            dto.setHasDecimals(dto.getHasDecimals());
        } else {
            dto.setHasDecimals(0);
        }

        if (dto.getAssetManagementEnabled() != null) {
            dto.setAssetManagementEnabled(dto.getAssetManagementEnabled());
        } else {
            dto.setAssetManagementEnabled(0);
        }

        // Backwards compatible with the old ItemDTOEx Web Service API, use the
        // transient price field as the rate for a default pricing model.
        if (dto.getPrice() != null) {
            dto.addDefaultPrice(CommonConstants.EPOCH_DATE, getDefaultPrice(dto.getPrice(), dto.isPercentage()), dto.getPriceModelCompanyId());
        }

        // default currency for new prices (if currency is not explicitly set)
        if (dto.getDefaultPrices() != null) {
            for (PriceModelDTO price : dto.getDefaultPrices().iterator().next().getPrices().values()) {
                if (price.getCurrency() == null) {
                    if (dto.isGlobal()) {
                        if(entity.getEntity() != null) {
                            price.setCurrency(entity.getEntity().getCurrency());
                        }
                    } else {
                        if(dto.getEntities().iterator().next().getParent()!=null){
                            price.setCurrency(dto.getEntities().iterator().next().getParent().getCurrency());
                        }else{
                            price.setCurrency(dto.getEntities().iterator().next().getCurrency());
                        }
                    }
                }
            }
        }

        // validate all pricing attributes

        if (dto.getDefaultPrices() != null) {
            // There can only be one ItemPrice in set at this stage.
            PriceModelBL.validateAttributes(dto.getDefaultPrices().iterator().next().getPrices().values());
        } else {
            LOG.debug("Percentage items cannot have a default price model.");
            dto.getDefaultPrices().clear();
        }

        validateUniqueProductCode(dto, dto.getChildEntitiesIds(), true);

        dto.setDeleted(0);

        // for parent entity
        if (!dto.getIsPlan()) {
            dto.updateMetaFieldsWithValidation(dto.getEntity().getLanguageId(), dto.getEntityId(), null, dto);
            //for child entities
            if (dto.isGlobal()) {
                for(CompanyDTO company : new CompanyDAS().findChildEntities(dto.getId())) {
                    dto.updateMetaFieldsWithValidation(company.getLanguageId(), company.getId(), null, dto);
                }
            } else {
                for(Integer id : dto.getChildEntitiesIds()) {
                    dto.updateMetaFieldsWithValidation(new CompanyDAS().find(id).getLanguageId(), id, null, dto);
                }
            }
        }
        //add the orderline meta fields
        if(dto.getOrderLineMetaFields().size() > 0) {
            validateProductOrderLinesMetaFields(dto.getOrderLineMetaFields());

            Set<MetaField> orderLineMetaFieldDtos = dto.getOrderLineMetaFields();
            dto.setOrderLineMetaFields(new HashSet<MetaField>());
            MetaFieldBL metaFieldBL = new MetaFieldBL();
            for(MetaField metaField : orderLineMetaFieldDtos) {
                dto.getOrderLineMetaFields().add(metaFieldBL.create(metaField));
            }
        }

        if(!dto.getIsPlan()){
            dto.updateMetaFieldsWithValidation(dto.getEntity().getLanguageId(), dto.getEntityId(),null, dto);
        }

        item = itemDas.save(dto);

        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription(), languageId);
        }

        updateTypes(dto);
        updateExcludedTypes(dto);
        updateAccountTypes(dto);

        //triggering processing of event for parent company
        NewItemEvent newItemEvent = new NewItemEvent(item);
        EventManager.process(newItemEvent);

        // triggering process for child companies
        for(Integer id : dto.getChildEntitiesIds()) {
            newItemEvent.setEntityId(id);
            EventManager.process(newItemEvent);
        }
        return item.getId();
    }

    public Integer create(ItemDTO dto, Integer languageId, boolean isPlan) {
        EntityBL entity = new EntityBL(dto.getEntityId());
        if (languageId == null) {
            languageId = entity.getEntity().getLanguageId();
        }

        if (dto.getHasDecimals() != null) {
            dto.setHasDecimals(dto.getHasDecimals());
        } else {
            dto.setHasDecimals(0);
        }

        // Backwards compatible with the old ItemDTOEx Web Service API, use the
        // transient price field as the rate for a default pricing model.
        if (dto.getPrice() != null) {
            dto.addDefaultPrice(CommonConstants.EPOCH_DATE, getDefaultPrice(dto.getPrice(),dto.isPercentage()), dto.getPriceModelCompanyId());
        }

        // default currency for new prices (if currency is not explicitly set)
        if (dto.getDefaultPrices() != null) {
            for (PriceModelDTO price : dto.getDefaultPrices().iterator().next().getPrices().values()) {
                if (price.getCurrency() == null) {
                    price.setCurrency(entity.getEntity().getCurrency());
                }
            }
        }

        // validate all pricing attributes
        if (dto.getDefaultPrices() != null) {
            LOG.debug("Validating Attributes....");
            PriceModelBL.validateAttributes(dto.getDefaultPrices().iterator().next().getPrices().values());
        }

        dto.setDeleted(0);
        if(!isPlan) {
            dto.updateMetaFieldsWithValidation(dto.getEntity().getLanguageId(), dto.getEntityId(),null, dto);
        }

        item = itemDas.save(dto);

        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription(), languageId);
        }
        updateTypes(dto);
        updateExcludedTypes(dto);

        // trigger internal event
        EventManager.process(new NewItemEvent(item));

        return item.getId();
    }

    public void update(Integer executorId, ItemDTO dto, Integer languageId)  {
        update(executorId, dto, languageId, false);
    }

    public void update(Integer executorId, ItemDTO dto, Integer languageId, boolean isPlan)  {
        eLogger.audit(executorId, null, Constants.TABLE_ITEM, item.getId(),
                EventLogger.MODULE_ITEM_MAINTENANCE,
                EventLogger.ROW_UPDATED, null, null, null);

        //validate unique product code
        validateUniqueProductCode(dto, dto.getChildEntitiesIds(), false);

        validateProductOrderLinesMetaFields(dto.getOrderLineMetaFields());

        item.setNumber(dto.getNumber());
        item.setGlCode(dto.getGlCode());

        if (dto.getDescription() != null) {
            item.setDescription(dto.getDescription(), languageId);
        }
        item.setPercentage(dto.getPercentage());
        item.setHasDecimals(dto.getHasDecimals());
        item.setAssetManagementEnabled(dto.getAssetManagementEnabled());
        item.setStandardAvailability(dto.isStandardAvailability());

        item.setStandardPartnerPercentage(dto.getStandardPartnerPercentage());
        item.setMasterPartnerPercentage(dto.getMasterPartnerPercentage());

        updateTypes(dto);
        updateExcludedTypes(dto);

        updateAccountTypes(dto);

        mergeDependencies(dto);

        item.setGlobal(dto.isGlobal());
        item.setEntities(dto.getEntities());
        if(dto.getEntity() != null) {
            if(!dto.getIsPlan()) {
                item.updateMetaFieldsWithValidation(dto.getEntity().getLanguageId(), dto.getEntityId(), null, dto);
            }
            item.setEntity(dto.getEntity());
        }
        //clear meta fields, in case we have different child entities than the ones assigned before we do not want there meta fields
        item.getMetaFields().clear();

        if(!dto.getIsPlan()) {
            if (dto.isGlobal()) {
                for (CompanyDTO company : new CompanyDAS().findChildEntities(dto.getEntityId())) {
                    item.updateMetaFieldsWithValidation(company.getLanguageId(), company.getId(), null, dto);
                }
            } else {
                for (Integer entityId : dto.getChildEntitiesIds()) {
                    item.updateMetaFieldsWithValidation(new CompanyDAS().find(entityId).getLanguageId(), entityId, null, dto);
                }
            }
        }

        Collection<Integer> unusedMetaFieldIds = updateProductOrderLineMetaFields(dto.getOrderLineMetaFields());

        if(!isPlan && !dto.getIsPlan()) {
            item.updateMetaFieldsWithValidation(dto.getEntity().getLanguageId(), dto.getEntityId(),null, dto);
        }

        updateDefaultPrice(dto);
        // validate all pricing attributes

        if (item.getDefaultPrices() != null && !item.getDefaultPrices().isEmpty()) {
            SortedMap<Date, PriceModelDTO> prices = dto.getPriceModelCompanyId() != null ? item.getDefaultPricesByCompany(dto.getPriceModelCompanyId()) : item.getGlobalDefaultPrices();
            if(prices != null) {
                PriceModelBL.validateAttributes(prices.values());
            }
        }

        updateRatingConfiguration(dto);

        item.setActiveSince(dto.getActiveSince());
        item.setActiveUntil(dto.getActiveUntil());
        item.setReservationDuration(dto.getReservationDuration());

        item=itemDas.save(item);

        deleteUnusedProductOrderLineMetaFields(unusedMetaFieldIds);

        LOG.debug("Processing ItemUpdatedEvent using entity id:" + item.getEntityId());
        ItemUpdatedEvent itemUpdatedEvent = new ItemUpdatedEvent(item);
        EventManager.process(itemUpdatedEvent);
        for (Integer id : dto.getChildEntitiesIds()) {
            LOG.debug("Processing ItemUpdatedEvent using entity id:" + id);
            itemUpdatedEvent.setEntityId(id);
            EventManager.process(itemUpdatedEvent);
        }
    }



    /**
     * Merge dependencies in dto into the entity. Adding and removing
     * dependencies as necessary.
     *
     * @param dto
     */
    private void mergeDependencies(ItemDTO dto) {
        Map<Integer, ItemDependencyDTO> dependencyIdObjMap = new HashMap<Integer, ItemDependencyDTO>(item.getDependencies().size() * 2);
        for(ItemDependencyDTO dependencyDTO : item.getDependencies()) {
            dependencyIdObjMap.put(dependencyDTO.getId(), dependencyDTO);
        }

        for(ItemDependencyDTO dependencyDTO : dto.getDependencies()) {
            //if it is a new dependency
            if(dependencyDTO.getId() <= 0) {
                item.addDependency(dependencyDTO);
            } else {
                //the objects are immutable. so we do not update values for ItemDependencyDTO
                dependencyIdObjMap.remove(dependencyDTO.getId());
            }
        }

        //remove dependencies which are in entity but not dto
        for(ItemDependencyDTO dependencyDTO : dependencyIdObjMap.values()) {
            item.getDependencies().remove(dependencyDTO);
        }
    }

    /**
     * Constructs a FLAT PriceModelDTO with the given rate to be used as
     * the default price for items. This type of price model matches the old
     * "$ per unit" style pricing for basic items.
     *
     * @param rate rate per unit
     * @return price model
     */
    private PriceModelDTO getDefaultPrice(BigDecimal rate , boolean isPercentage) {
        PriceModelDTO model = new PriceModelDTO();
        model.setRate(rate);
        model.setType(!isPercentage ? PriceModelStrategy.FLAT : PriceModelStrategy.LINE_PERCENTAGE);

        return model;
    }



    /**
     * Updates the price of this item to that of the given ItemDTO. This method
     * handles updates to the price using both the items default price model, and
     * the transient price attribute.
     *
     * If the given dto has a price through {@link ItemDTO#getPrice()}, then the
     * default price model rate will be set to the price. Otherwise the given dto's
     * price model is used to update.
     *
     * @param dto item holding the updates to apply to this item
     */
    private void updateDefaultPrice(ItemDTO dto) {
        //Find the price in item and dto with respect to company
        SortedMap<Date, PriceModelDTO> itemPrices = null;
        SortedMap<Date, PriceModelDTO> dtoPrices = null;

        if(dto.getPriceModelCompanyId() != null) {
            itemPrices = item.getDefaultPricesByCompany(dto.getPriceModelCompanyId());
            dtoPrices = dto.getDefaultPricesByCompany(dto.getPriceModelCompanyId());
        } else {
            itemPrices = item.getGlobalDefaultPrices();
            dtoPrices = dto.getGlobalDefaultPrices();
        }

        if (itemPrices == null || itemPrices.isEmpty()) {
            // new default price
            if (dtoPrices != null || !dtoPrices.isEmpty()) {
                //itemPrices.clear();
                itemPrices = new TreeMap<Date, PriceModelDTO>();
                itemPrices.putAll(dtoPrices);

            } else if (dtoPrices != null) {
                itemPrices.put(CommonConstants.EPOCH_DATE, getDefaultPrice(dto.getPrice(),dto.isPercentage()));
            }

        } else {
            // update existing default price
            if (dtoPrices != null || !dtoPrices.isEmpty()) {
                itemPrices.clear();

                itemPrices.putAll(dtoPrices);

            } else if (dtoPrices != null) {
                if (dtoPrices.size() == 1) {

                    itemPrices.get(0).setRate(dto.getPrice());

                } else {
                    // cannot use legacy price column, there is more than 1 price that can be updated
                    // we should be updating the individual price model instead.
                    throw new SessionInternalError("Item uses multiple dated prices, cannot use WS price.");
                }
            }
        }

        // default price currency should always be the entity currency
        if (itemPrices != null) {
            // TODOs
            for (PriceModelDTO price : itemPrices.values()) {
                if (price.getCurrency() == null) {
                    if(item.getEntity() != null) {
                        price.setCurrency(item.getEntity().getCurrency());
                    } else {
                        if(item.getEntities().iterator().next().getParent()!=null){
                            price.setCurrency(item.getEntities().iterator().next().getParent().getCurrency());
                        }else{
                            price.setCurrency(item.getEntities().iterator().next().getCurrency());
                        }
                    }
                }
            }
        }
        CompanyDTO company = dto.getPriceModelCompanyId() != null ? new CompanyDAS().find(dto.getPriceModelCompanyId()) : null;

        if(itemPrices == null || itemPrices.isEmpty()) {
            // default prices for given company were removed. Remove price entry.
            item.removeDefaultPricesByCompany(company);
        } else {
            // Now set the itemPrices in item
            item.setDefaultPricesByCompany(itemPrices, company);
        }
    }

    private void updateRatingConfiguration(ItemDTO dto){

        SortedMap<Date,RatingConfigurationDTO> itemRatingConfiguration=item.getRatingConfigurations();
        SortedMap<Date,RatingConfigurationDTO> dtoRatingConfiguration=dto.getRatingConfigurations();

        if(!MapUtils.isEmpty(dtoRatingConfiguration)){

            if(itemRatingConfiguration==null) {
                itemRatingConfiguration = new TreeMap<>(dtoRatingConfiguration);
            }
            else{
                itemRatingConfiguration.clear();
                itemRatingConfiguration.putAll(dtoRatingConfiguration);

            }

        } else{

            itemRatingConfiguration=new TreeMap<>();
        }

        if (itemRatingConfiguration != null) {
            item.setRatingConfigurations(itemRatingConfiguration);
        }

    }

    private void updateTypes(ItemDTO dto)
    {
        // update the types relationship
        Collection types = item.getItemTypes();
        types.clear();
        ItemTypeBL typeBl = new ItemTypeBL();
        // TODO verify that all the categories belong to the same
        // order_line_type_id
        for (int f=0; f < dto.getTypes().length; f++) {
            typeBl.set(dto.getTypes()[f]);
            types.add(typeBl.getEntity());
        }
    }

    private void updateExcludedTypes(ItemDTO dto) {
        item.getExcludedTypes().clear();

        ItemTypeBL itemType = new ItemTypeBL();
        for (Integer typeId : dto.getExcludedTypeIds()) {
            itemType.set(typeId);
            item.getExcludedTypes().add(itemType.getEntity());
        }
    }

    private void updateAccountTypes(ItemDTO dto) {
        item.getAccountTypeAvailability().clear();
        if (!dto.isStandardAvailability()) {
            AccountTypeBL accountTypeBL = new AccountTypeBL();
            for (Integer accountTypeId : dto.getAccountTypeIds()) {
                accountTypeBL.setAccountType(accountTypeId);
                item.getAccountTypeAvailability().add(accountTypeBL.getAccountType());
            }
        }
    }

    public void delete(Integer executorId) {
        //check if there are assets linked to the item
        int assets = new AssetBL().countAssetsForItem(item.getId());
        if(assets > 0) {
            throw new SessionInternalError("Unable to delete item. There are linked assets.",
                    new String[] {"validation.item.no.delete.assets.linked"});
        }
        int dependencyCount = new ItemDependencyDAS().countByDependentItem(item.getId());
        if (dependencyCount > 0) {
            throw new SessionInternalError("Unable to delete item. Its use in other product as dependency.",
                    new String[]{"validation.item.dependency.exist"});
        }
        item.setDeleted(1);

        item.setTypes(new Integer[0]);

        eLogger.audit(executorId, null, Constants.TABLE_ITEM, item.getId(),
                EventLogger.MODULE_ITEM_MAINTENANCE,
                EventLogger.ROW_DELETED, null, null, null);

        // trigger internal event
        ItemDeletedEvent itemDeletedEvent = new ItemDeletedEvent(item);
        EventManager.process(itemDeletedEvent);
        for (Integer entityId : item.getChildEntitiesIds()) {
            itemDeletedEvent.setEntityId(entityId);
            EventManager.process(itemDeletedEvent);
        }

        itemDas.flush();
        itemDas.clear();
    }

    public boolean validateDecimals( Integer hasDecimals ){
        if( hasDecimals == 0 ){
            if(new OrderLineDAS().findLinesWithDecimals(item.getId()) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the basic price for an item and currency, without including purchase quantity or
     * the users current usage in the pricing calculation.
     *
     * This method does not execute any pricing plug-ins and does not use quantity or usage
     * values for {@link PriceModelDTO#applyTo(com.sapienter.jbilling.server.order.db.OrderDTO, com.sapienter.jbilling.server.order.db.OrderLineDTO, java.math.BigDecimal, com.sapienter.jbilling.server.item.tasks.PricingResult, java.util.List, com.sapienter.jbilling.server.order.Usage, boolean, java.util.Date)}
     * price calculations.
     *
     * @param date
     * @param item item to price
     * @param currencyId currency id of requested price
     * @return The price in the requested currency
     */
    public BigDecimal getPriceByCurrency(Date date, ItemDTO item, Integer userId, Integer currencyId, OrderDTO order, OrderLineDTO orderLine)  {
        return getPriceByCurrency(date, item, userId, currencyId, null, order, orderLine);
    }

    // return the price calculated with/without quantity (it's depends of PriceModel type)
    public BigDecimal getPriceByCurrency(Date date, ItemDTO item, Integer userId, Integer currencyId, BigDecimal quantity, OrderDTO order, OrderLineDTO orderLine)  {
        if (item.getDefaultPrices() != null && !item.getDefaultPrices().isEmpty()) {
            // empty usage for default pricing
            Usage usage = new Usage();
            usage.setUserId(userId);
            usage.setItemId(item.getId());
            usage.setAmount(BigDecimal.ZERO);
            usage.setQuantity(BigDecimal.ZERO);

            // calculate default price from strategy
            PricingResult result = new PricingResult(item.getId(), userId, currencyId);
            List<PricingField> fields = pricingFields == null ? Collections.emptyList() : pricingFields;

            // price for today
            long startTime = System.currentTimeMillis();
            PriceModelDTO priceModel = item.getPrice(TimezoneHelper.companyCurrentDate(item.getPriceModelCompanyId()), item.getPriceModelCompanyId());
            LOG.debug("getPriceByCurrency took %s miliseconds for user %s", (System.currentTimeMillis() - startTime), userId);

            if (priceModel != null) {

                if(priceModel.getType().equals(PriceModelStrategy.LINE_PERCENTAGE)) {

                    item.setIsPercentage(true);
                    return priceModel.getRate();

                } else if(priceModel.getType().equals(PriceModelStrategy.GRADUATED) || priceModel.getType().equals(PriceModelStrategy.GRADUATED_RATE_CARD)) {

                    priceModel.applyTo(null, null, quantity, result, fields, usage, false, date);

                } else {
                    startTime = System.currentTimeMillis();
                    priceModel.applyTo(null, null ,BigDecimal.ONE, result, fields, usage, false, date);
                    LOG.debug("getPriceByCurrency's applyTo took %s for user %s", (System.currentTimeMillis() - startTime), userId);
                }
                return result.getPrice() == null ? BigDecimal.ZERO : result.getPrice();
            }
        }
        return BigDecimal.ZERO;
    }


    public BigDecimal getPrice(Integer userId, BigDecimal quantity, Integer entityId) throws SessionInternalError {
        UserBL user = new UserBL(userId);
        return getPrice(userId, user.getCurrencyId(), quantity, entityId, null, null, false, null);
    }

    public BigDecimal getPrice(Integer userId, Integer currencyId, BigDecimal quantity, Integer entityId) throws SessionInternalError {
        return getPrice(userId, currencyId, quantity, entityId, null, null, false, null);
    }

    public BigDecimal getPrice(Integer entityId, Integer userId ,Integer planId, Integer itemId, List<PricingField> fields, OrderDTO order) throws SessionInternalError {

        PluggableTaskManager taskManager = null;
        try {
            taskManager = new PluggableTaskManager(entityId, Constants.PLUGGABLE_TASK_ITEM_PRICING);
        } catch (PluggableTaskException e) {
        }

        PriceModelPricingTask task = null;
        try {
            if (taskManager != null) {
                task = (PriceModelPricingTask) taskManager.getNextClass();
            }
        } catch (PluggableTaskException e) {
            // eat it
        }
        // if one was not configured just use the basic task by default
        if (task == null) {
            LOG.debug("No task is configured");
            task = new PriceModelPricingTask();
        }

        PriceModelPricingTask.PriceModelLevel priceModelLevel = task.getPriceModelLevel(userId, itemId, task.getAttributes(fields));

        BigDecimal finalPrice = null;
        if (priceModelLevel.isPlanLevel()) {
            PlanBL planBl = new PlanBL(planId);
            for (PlanItemDTO planItem : planBl.getEntity().getPlanItems()) {
                if (planItem.getItem().getId() == itemId) {
                    finalPrice = planItem.getPrice(TimezoneHelper.companyCurrentDate(entityId)).getRate();
                    break;
                }
            }
        } else {
            finalPrice = getPrice(userId, BigDecimal.ONE, entityId);
        }

        return finalPrice;


    }

    /**
     * Will find the right price considering the user's special prices and which
     * currencies had been entered in the prices table.
     *
     *
     * @param userId user id
     * @param currencyId currency id
     * @param entityId entity id
     * @param order order being created or edited, maybe used for additional pricing calculations    @return The price in the requested currency. It always returns a price,
     * */
    public BigDecimal getPrice(Integer userId, Integer currencyId, BigDecimal quantity, Integer entityId, OrderDTO order, OrderLineDTO orderLine,
            boolean singlePurchase, Date eventDate) {

        if (currencyId == null || entityId == null) {
            throw new SessionInternalError("Can't get a price with null parameters. currencyId = " + currencyId +
                    " entityId = " + entityId);
        }

        CurrencyBL currencyBL;
        long startCurrencyLoadTime = System.currentTimeMillis();
        try {
            currencyBL = new CurrencyBL(currencyId);
            priceCurrencySymbol = currencyBL.getEntity().getSymbol();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
        LOG.debug("loadCurrency Took %s for user %s", (System.currentTimeMillis() - startCurrencyLoadTime), userId);
        Date pricingDate = null;
        if (eventDate != null) {
            pricingDate = eventDate;
        } else if (null != order) {
            pricingDate = order.getPricingDate();
        }

        // set price model company id
        item.setPriceModelCompanyId(entityId);
        // default "simple" price
        long loadPriceByCurrency = System.currentTimeMillis();
        BigDecimal price = getPriceByCurrency(pricingDate, item, userId, currencyId, quantity, order, orderLine);
        LOG.debug("loadPriceByCurrency Took %s for user %s", (System.currentTimeMillis() - loadPriceByCurrency), userId);

        // run a plug-in with external logic (rules), if available
        try {
            PluggableTaskManager<IPricing> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_ITEM_PRICING);
            IPricing myTask = taskManager.getNextClass();
            PriceContextDTO priceContext = PriceContextDTO.of(item, quantity, userId, currencyId, pricingFields, eventDate);
            while(myTask != null) {
                price = myTask.getPrice(priceContext, price, order, orderLine, singlePurchase);
                myTask = taskManager.getNextClass();
            }
        } catch (Exception e) {
            throw new SessionInternalError("Item pricing task error", ItemBL.class, e);
        }

        return price;
    }

    /**
     * Will find the right price considering the user's special prices,
     * currencies that have been entered in the prices table
     * and the mediation order Event Date.
     *
     * Useful for calls by the Mediation process
     *
     * @param userId user id
     * @param currencyId currency id
     * @param entityId entity id
     * @param quantity quantity
     * @param eventDate the date on which the order/event must be created
     * @return The price in the requested currency. It always returns a price,
     * otherwise an exception for lack of pricing for an item
     */
    public BigDecimal getPriceByEventDate(Integer userId, Integer currencyId, BigDecimal quantity, Integer entityId, Date eventDate) {

        if (currencyId == null || entityId == null) {
            throw new SessionInternalError("Can't get a price with null parameters. "
                    + "currencyId = " + currencyId
                    + " entityId = " + entityId);
        }

        CurrencyBL currencyBL;
        try {
            currencyBL = new CurrencyBL(currencyId);
            priceCurrencySymbol = currencyBL.getEntity().getSymbol();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        //price will be retrieved/converted based on exchange rate on the eventDate
        BigDecimal price = getPriceByCurrency(eventDate, item, userId, currencyId, null, null);
        LOG.debug("Got default 'simple' price for order pricing date %s as %s", eventDate, price);

        // run a plug-in with external logic (rules), if available
        try {
            PluggableTaskManager<IPricing> taskManager
            = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_ITEM_PRICING);
            IPricing myTask = taskManager.getNextClass();
            PriceContextDTO priceContext = PriceContextDTO.of(item, quantity, userId, currencyId, pricingFields, eventDate);
            while(myTask != null) {
                price = myTask.getPrice(priceContext, price, null, null, false);
                myTask = taskManager.getNextClass();
            }
        } catch (Exception e) {
            throw new SessionInternalError("Item pricing task error", ItemBL.class, e);
        }

        return price;
    }

    /**
     * Returns an ItemDTO constructed for the given language and entity, priced for the
     * given user and currency.
     *
     *
     * @param languageId id of the users language
     * @param userId id of the user purchasing the item
     * @param entityId id of the entity
     * @param currencyId id of the currency   @return item dto
     * @throws SessionInternalError if an internal exception occurs processing request
     */
    public ItemDTO getDTO(Integer languageId, Integer userId, Integer entityId, Integer currencyId)
            throws SessionInternalError {
        return getDTO(languageId, userId, entityId, currencyId, BigDecimal.ONE, null, null, false, null);
    }


    /**
     * Returns an ItemDTO constructed for the given language and entity, priced for the
     * given user, currency and the amount being purchased.
     *
     * @param languageId id of the users language
     * @param userId id of the user purchasing the item
     * @param entityId id of the entity
     * @param currencyId id of the currency
     * @param quantity quantity being purchased
     * @return item dto
     * @throws SessionInternalError if an internal exception occurs processing request
     */
    public ItemDTO getDTO(Integer languageId, Integer userId, Integer entityId, Integer currencyId, BigDecimal quantity) {
        return getDTO(languageId, userId, entityId, currencyId, quantity, null, null, false, null);
    }

    /**
     * Returns an ItemDTO constructed for the given language and entity, priced for the
     * given user, currency and the amount being purchased.
     *
     * If an order is given, then the order quantities will impact the price calculations
     * for item prices that include usage.
     *
     * @param languageId id of the users language
     * @param userId id of the user purchasing the item
     * @param entityId id of the entity
     * @param currencyId id of the currency
     * @param quantity quantity being purchased
     * @param order order that this item is to be added to. may be null if no order operation.
     * @return item dto
     * @throws SessionInternalError if an internal exception occurs processing request
     */
    public ItemDTO getDTO(Integer languageId, Integer userId, Integer entityId, Integer currencyId, BigDecimal quantity,
            OrderDTO order) throws SessionInternalError {

        return getDTO(languageId, userId, entityId, currencyId, quantity, order, null, false, null);
    }


    /**
     * Returns an ItemDTO constructed for the given language and entity, priced for the
     * given user, currency and the amount being purchased.
     *
     * If an order is given, then the order quantities will impact the price calculations
     * for item prices that include usage.
     *
     *
     *
     *
     * @param languageId id of the users language
     * @param userId id of the user purchasing the item
     * @param entityId id of the entity
     * @param currencyId id of the currency
     * @param quantity quantity being purchased
     * @param order order that this item is to be added to. may be null if no order operation.
     * @return item dto
     * @throws SessionInternalError if an internal exception occurs processing request
     */
    public ItemDTO getDTO(Integer languageId, Integer userId, Integer entityId, Integer currencyId, BigDecimal quantity,
            OrderDTO order, OrderLineDTO orderLine, boolean singlePurchase, Date eventDate) throws SessionInternalError {

        ItemDTO dto = new ItemDTO(
                item.getId(),
                item.getInternalNumber(),
                item.getGlCode(),
                new CompanyDAS().find(entityId),
                item.getDescription(languageId),
                item.getDeleted(),
                currencyId,
                null,
                item.getPercentage(),
                null, // to be set right after
                item.getHasDecimals(),
                item.getAssetManagementEnabled(),item.isPercentage());

        // if item is also attached to some child entities
        dto.setEntities(item.getEntities());

        dto.setGlobal(item.isGlobal());

        // set priceModelCompany for which prices must be loaded
        dto.setPriceModelCompanyId(entityId);
        dto.setDefaultPrices(item.getDefaultPrices());
        dto.setRatingConfigurations(item.getRatingConfigurations());

        // calculate a true price using the pricing plug-in, pricing takes into
        // account plans, special prices and the quantity of the item being purchased
        if (currencyId != null) {
            dto.setPrice(getPrice(userId, currencyId, quantity, entityId, order, orderLine, singlePurchase, eventDate));

            if (item.isPercentage()) {
                dto.setPercentage(dto.getPrice());
                dto.setIsPercentage(true);
            } else {
                dto.setPercentage(null);
                dto.setIsPercentage(false);
            }
        }

        // set the types
        Integer types[] = new Integer[item.getItemTypes().size()];
        int n = 0;
        for (ItemTypeDTO type : item.getItemTypes()) {
            types[n++] = type.getId();
            dto.setOrderLineTypeId(type.getOrderLineTypeId());
        }
        dto.setTypes(types);

        // set excluded types
        Integer excludedTypes[] = new Integer[item.getExcludedTypes().size()];
        int i = 0;
        for (ItemTypeDTO type : item.getExcludedTypes()) {
            excludedTypes[i++] = type.getId();
        }
        dto.setExcludedTypeIds(excludedTypes);

        // set account types
        dto.setStandardAvailability(item.isStandardAvailability());
        Integer accountTypes[] = new Integer[item.getAccountTypeAvailability().size()];
        int j = 0;
        for (AccountTypeDTO type : item.getAccountTypeAvailability()) {
            accountTypes[j++] = type.getId();
        }
        dto.setAccountTypeIds(accountTypes);

        dto.setMetaFields(item.getMetaFields());
        dto.setDependencies(item.getDependencies());
        dto.setOrderLineMetaFields(item.getOrderLineMetaFields());

        dto.setActiveSince(item.getActiveSince());
        dto.setActiveUntil(item.getActiveUntil());

        dto.setStandardPartnerPercentage(item.getStandardPartnerPercentage());
        dto.setMasterPartnerPercentage(item.getMasterPartnerPercentage());
        dto.setReservationDuration(Util.convertFromMsToMinutes(item.getReservationDuration()));
        dto.setIsPlan(item.hasPlans());

        LOG.debug("Got item: %s , price: %s", dto.getId(), dto.getPrice());

        return dto;
    }

    public static final ItemDTO getDTO(ItemDTOEx other) {
        ItemDTO retValue = new ItemDTO();
        CompanyDAS companyDAS = new CompanyDAS();

        if (other.getId() != null) {
            retValue.setId(other.getId());
        }

        if(other.getEntityId() != null) {
            retValue.setEntity(companyDAS.find(other.getEntityId()));
        }

        retValue.setNumber(other.getNumber());
        retValue.setGlCode(other.getGlCode());
        retValue.setDeleted(other.getDeleted());
        retValue.setHasDecimals(other.getHasDecimals());
        retValue.setDescription(other.getDescription());
        retValue.setTypes(other.getTypes());
        retValue.setExcludedTypeIds(other.getExcludedTypes());
        retValue.setStandardAvailability(other.isStandardAvailability());
        retValue.setAccountTypeIds(other.getAccountTypes());
        retValue.setPromoCode(other.getPromoCode());
        retValue.setCurrencyId(other.getCurrencyId());
        retValue.setPrice(other.getPriceAsDecimal());
        retValue.setOrderLineTypeId(other.getOrderLineTypeId());

        retValue.setAssetManagementEnabled(other.getAssetManagementEnabled());

        retValue.setDependencies(ItemDependencyBL.toDto(other.getDependencies(), retValue));

        retValue.setEntities(AssetBL.convertToCompanyDTO(other.getEntities()));
        retValue.setGlobal(other.isGlobal());

        /*if(retValue.getEntityId() != null) {
        	MetaFieldBL.fillMetaFieldsFromWS(retValue.getEntityId(), retValue, other.getMetaFieldsMap().get(retValue.getEntityId()));
        }*/

        if(retValue.isGlobal()){
            if(retValue.getEntity() != null) {
                List<Integer> allEntities = new CompanyDAS().getChildEntitiesIds(other.getEntityId());
                allEntities.add(other.getEntityId());

                for(Integer id: allEntities){
                    MetaFieldBL.fillMetaFieldsFromWS(id, retValue, other.getMetaFieldsMap().get(id));
                }

                if (other.getOrderLineMetaFields() != null) {
                    for (MetaFieldWS metaField : other.getOrderLineMetaFields()) {
                        retValue.getOrderLineMetaFields().add(MetaFieldBL.getDTO(metaField,retValue.getEntityId()));
                    }
                }
            }
        } else {
            for(Integer id : other.getEntities()) {
                LOG.debug("other.getMetaFieldsMap().get(id)  %s", (Object)other.getMetaFieldsMap().get(id));
                MetaFieldBL.fillMetaFieldsFromWS(id, retValue, other.getMetaFieldsMap().get(id));

                if (other.getOrderLineMetaFields() != null) {
                    for (MetaFieldWS metaField : other.getOrderLineMetaFields()) {
                        retValue.getOrderLineMetaFields().add(MetaFieldBL.getDTO(metaField,retValue.getEntityId()));
                    }
                }
            }

        }

        // convert PriceModelWS to PriceModelDTO
        CompanyDTO priceModelCompany = other.getPriceModelCompanyId() != null ? companyDAS.find(other.getPriceModelCompanyId()) : null;
        retValue.setDefaultPricesByCompany(PriceModelBL.getDTO(other.getDefaultPrices()), priceModelCompany);
        retValue.setPriceModelCompanyId(other.getPriceModelCompanyId());
        retValue.setMasterPartnerPercentage(other.getMasterPartnerPercentageAsDecimal());
        retValue.setStandardPartnerPercentage(other.getStandardPartnerPercentageAsDecimal());

        //adding rating configuration
        if(other.getRatingConfigurations()!=null) {
            retValue.setRatingConfigurations(RatingConfigurationBL.convertMapWSToDTO(other.getRatingConfigurations()));
        }

        // #7514 - Plans Enhancement
        retValue.setActiveSince(other.getActiveSince());
        retValue.setActiveUntil(other.getActiveUntil());

        /* #10256 - Asset Reservation */
        //        We always need to save asset value as it's not nullable field in database.
        if (other.getReservationDuration() == null || other.getReservationDuration() == 0) {
            //            If other.getReservationDuration() does not have any value, we should add default value saved in Preference.
            retValue.setReservationDuration(Util.convertFromMinutesToMs(Integer.parseInt(PreferenceBL.getPreferenceValue(other.getEntityId(), CommonConstants.PREFERENCE_ASSET_RESERVATION_DURATION))));
        } else {
            retValue.setReservationDuration(Util.convertFromMinutesToMs(other.getReservationDuration()));
        }
        retValue.setIsPlan(other.getIsPlan());

        return retValue;
    }

    public ItemDTOEx getWS(ItemDTO other) {
        if (other == null) {
            other = item;
        }

        return getItemDTOEx(other);
    }

    public static ItemDTOEx getItemDTOEx(ItemDTO other){

        ItemDTOEx retValue = new ItemDTOEx();
        retValue.setId(other.getId());

        if(other.getEntity() != null) {
            retValue.setEntityId(other.getEntity().getId());
        }

        if(other.getEntities() != null) {
            retValue.setEntities(new ArrayList<Integer>(other.getChildEntitiesIds()));
        }

        retValue.setNumber(other.getInternalNumber());
        retValue.setGlCode(other.getGlCode());
        retValue.setDeleted(other.getDeleted());
        retValue.setHasDecimals(other.getHasDecimals());
        retValue.setDescription(other.getDescription());
        retValue.setDescriptions(getDescriptions(other.getId()));
        retValue.setTypes(other.getTypes());
        retValue.setExcludedTypes(other.getExcludedTypeIds());
        retValue.setAccountTypes(other.getAccountTypeIds());
        retValue.setStandardAvailability(other.isStandardAvailability());
        retValue.setPromoCode(other.getPromoCode());
        retValue.setCurrencyId(other.getCurrencyId());
        retValue.setPrice(other.getPrice());
        retValue.setOrderLineTypeId(other.getOrderLineTypeId());
        retValue.setIsPlan(other.getIsPlan());

        retValue.setAssetManagementEnabled(other.getAssetManagementEnabled());
        retValue.setOrderLineMetaFields(new MetaFieldWS[other.getOrderLineMetaFields().size()]);
        int index = 0;
        for(MetaField metaField :  other.getOrderLineMetaFields()) {
            retValue.getOrderLineMetaFields()[index] = MetaFieldBL.getWS(metaField);
            index++;
        }

        retValue.setDependencies(ItemDependencyBL.toWs(other.getDependencies()));

        retValue.setGlobal(other.isGlobal());

        // Get meta field values of all the entities and then set them
        // set each entity's meta fields in map also
        MetaFieldValueWS[] metaFields = null;


        if(other.isGlobal()) {
            List<Integer> companyIds = new CompanyDAS().getChildEntitiesIds(other.getEntityId());
            companyIds.add(other.getEntityId());
            for (int entityId :companyIds) {
                MetaFieldValueWS[] childMetaFields = MetaFieldBL.convertMetaFieldsToWS(entityId, other);
                retValue.getMetaFieldsMap().put(entityId, childMetaFields);

                metaFields = (MetaFieldValueWS[]) ArrayUtils.addAll(metaFields, childMetaFields);
            }
        } else {
            for (int entityId : other.getChildEntitiesIds()) {
                MetaFieldValueWS[] childMetaFields = MetaFieldBL.convertMetaFieldsToWS(entityId, other);
                retValue.getMetaFieldsMap().put(entityId, childMetaFields);

                metaFields = (MetaFieldValueWS[]) ArrayUtils.addAll(metaFields, childMetaFields);
            }
        }
        retValue.setMetaFields(metaFields);

        // convert PriceModelDTO to PriceModelWS
        // Get price of current company if not found get global price
        retValue.setPriceModelCompanyId(other.getPriceModelCompanyId());
        SortedMap<Date, PriceModelDTO> prices = other.getDefaultPricesByCompany(other.getPriceModelCompanyId());

        if(prices == null) {
            prices = other.getGlobalDefaultPrices();
            retValue.setPriceModelCompanyId(null);
        } else {
            retValue.setPriceModelCompanyId(other.getPriceModelCompanyId());
        }

        if(prices != null) {
            retValue.setDefaultPrices(PriceModelBL.getWS(prices));
            // today's price
            retValue.setDefaultPrice(PriceModelBL.getWsPriceForDate(retValue.getDefaultPrices(), TimezoneHelper.companyCurrentDate(other.getEntityId())));
        }

        //Add RatingConfiguration
        SortedMap<Date, RatingConfigurationDTO> ratingConfiguration = other.getRatingConfigurations();
        if (ratingConfiguration != null) {
            retValue.setRatingConfigurations(RatingConfigurationBL.convertMapDTOToWS(ratingConfiguration));
        }

        retValue.setStandardPartnerPercentage(other.getStandardPartnerPercentage());
        retValue.setMasterPartnerPercentage(other.getMasterPartnerPercentage());

        retValue.setActiveSince(other.getActiveSince());
        retValue.setActiveUntil(other.getActiveUntil());
        retValue.setReservationDuration(other.getReservationDuration());

        return retValue;
    }

    private static List<InternationalDescriptionWS> getDescriptions(Integer itemId) {
        List<InternationalDescriptionWS> descriptionWSList = new LinkedList<>();
        List<InternationalDescriptionDTO> descriptionDTOList = new ItemDAS().getDescriptions(itemId);
        descriptionDTOList.forEach(internationalDescriptionDTO ->
        descriptionWSList.add(new InternationalDescriptionWS(
                internationalDescriptionDTO.getId().getLanguageId(),
                internationalDescriptionDTO.getContent()
                )));
        return descriptionWSList;
    }

    /**
     * @return
     */
    public String getPriceCurrencySymbol() {
        return priceCurrencySymbol;
    }

    /**
     * Returns all items for the given entity.
     * @param entityId
     * The id of the entity.
     * @return an array of all items
     */
    public ItemDTOEx[] getAllItems(Integer entityId) {
        EntityBL entityBL = new EntityBL(entityId);
        CompanyDTO entity = entityBL.getEntity();
        Collection<ItemDTO> itemEntities = itemDas.findByEntityId(entityId);
        ItemDTOEx[] items = new ItemDTOEx[itemEntities.size()];

        // iterate through returned item entities, converting them into a DTO
        int index = 0;
        for (ItemDTO item : itemEntities) {
            set(item.getId());
            items[index++] = getWS(getDTO(entity.getLanguageId(), null, entityId, entity.getCurrencyId()));
        }

        return items;
    }

    /**
     * Returns all items for the given item type (category) id. If no results
     * are found an empty array is returned.
     *
     * @see ItemDAS#findAllByItemType(Integer)
     *
     * @param itemTypeId item type (category) id
     * @param entityId	company id of which price will be used
     * @return array of found items, empty if none found
     */
    public ItemDTOEx[] getAllItemsByType(Integer itemTypeId, Integer entityId) {
        List<ItemDTO> results = new ItemDAS().findAllByItemType(itemTypeId);
        ItemDTOEx[] items = new ItemDTOEx[results.size()];

        int index = 0;
        for (ItemDTO item : results) {
            // set caller company id of item to get price model
            item.setPriceModelCompanyId(entityId);
            items[index++] = getWS(item);
        }

        return items;
    }

    public void setPricingFields(List<PricingField> fields) {
        pricingFields = fields;
    }

    public List<PricingField> getPricingFields(){
        return pricingFields;
    }


    public void validateUniqueProductCode(ItemDTO dto, List<Integer> entities, boolean isNew){
        LOG.debug("Validating product code : "+dto.getNumber());
        if(forceUniqueProductCode(entities) && !isUniqueProductCode(dto, entities, isNew)){
            throw new SessionInternalError("Product Number Is A Duplicate", new String[] {
                    "ItemDTOEx,number,validation.duplicate.error"
            });
        }
    }

    public boolean forceUniqueProductCode(List<Integer> entities){
        int preferenceUniqueProductCode = 0;
        boolean value = false;
        try {
            for (Integer entityId : entities) {
                preferenceUniqueProductCode =
                        PreferenceBL.getPreferenceValueAsIntegerOrZero(
                                entityId, Constants.PREFERENCE_UNIQUE_PRODUCT_CODE);
                if (1 == preferenceUniqueProductCode) {
                    value = true;
                }
            }
        } catch (EmptyResultDataAccessException e) {
            // default will be used
        }

        return value;
    }

    public static boolean isUniqueProductCode(ItemDTO item, Integer entityId, boolean isNew) {
        Long productCodeUsageCount = new ItemDAS().findProductCountByInternalNumber(item.getInternalNumber(), entityId, isNew, item.getId());
        if(productCodeUsageCount == 0) {
            LOG.debug("Its a unique product code ");
            return true;
        }
        LOG.debug("Its a duplicate product code");
        return false;
    }

    public static boolean isUniqueProductCode(ItemDTO item, List<Integer> entities, boolean isNew) {
        for (Integer entityId : entities) {
            Long productCodeUsageCount = new ItemDAS().findProductCountByInternalNumber(item.getInternalNumber(), entityId, isNew, item.getId());
            if(productCodeUsageCount != 0) {
                LOG.debug("Its a duplicate product code ");
                return false;
            }
        }
        LOG.debug("Its a unique product code");
        return true;
    }

    private void validateProductOrderLinesMetaFields(Collection<MetaField> newMetaFields) throws SessionInternalError {
        Collection<MetaField> currentMetaFields = item != null && item.getId() > 0 ? item.getOrderLineMetaFields() : new LinkedList<MetaField>();
        if(currentMetaFields.size()>0){
            MetaFieldBL.validateMetaFieldsChanges(newMetaFields, currentMetaFields);
        }
    }

    /**
     * Save new metafields, update existed meta fields, return ID of meta fields to remove
     * @param newMetaFields collection of entered metafileds
     * @return collection of IDs of metafields to be removed
     */
    private Collection<Integer> updateProductOrderLineMetaFields(Collection<MetaField> newMetaFields) {
        return MetaFieldBL.updateMetaFieldsCollection(newMetaFields, item.getOrderLineMetaFields());
    }

    /**
     * This method removes MetaFields, that no longer used by product. No validation is performed
     * Cal this method after removing links to MetaField from other entitties in DB
     * @param unusedMetaFieldIds ids of metafields for remove
     */
    private void deleteUnusedProductOrderLineMetaFields(Collection<Integer> unusedMetaFieldIds) {
        MetaFieldBL metaFieldBL = new MetaFieldBL();
        //delete metafields not linked to the product anymore
        for(Integer id : unusedMetaFieldIds) {
            metaFieldBL.delete(id);
        }
    }

    /**
     * Merge properties from dto metafield to persisted one
     * @param destination persisted metafield
     * @param source dto metafield with updated properties
     */
    private void mergeBasicProperties(MetaField destination, MetaField source) {
        destination.setName(source.getName());
        destination.setPrimary(source.getPrimary());
        destination.setValidationRule(source.getValidationRule());
        destination.setDataType(source.getDataType());
        destination.setDefaultValue(source.getDefaultValue());
        destination.setDisabled(source.isDisabled());
        destination.setMandatory(source.isMandatory());
        destination.setDisplayOrder(source.getDisplayOrder());
        destination.setFieldUsage(source.getFieldUsage());
    }

    /**
     * Calculates all the parents and childs of a given id
     *
     * @param entityId
     * @return
     */
    public List<Integer> getParentAndChildIds(Integer entityId) {
        Integer parentId = getRootEntityId(entityId);
        List<Integer> entities = new ArrayList<Integer>(0);

        entities.add(parentId);
        entities.addAll(findChilds(parentId));

        return entities;
    }

    private List<Integer> findChilds(Integer parentId) {
        List<Integer> entities = new ArrayList<Integer>();

        List<CompanyDTO> childs = new CompanyDAS().findChildEntities(parentId);
        for(CompanyDTO child : childs) {
            entities.add(child.getId());
            entities.addAll(findChilds(child.getId()));
        }

        return entities;
    }

    private Integer getRootEntityId(Integer entityId) {
        CompanyDTO company = new CompanyDAS().find(entityId);
        if(company.getParent() == null) {
            return entityId;
        } else {
            return getRootEntityId(company.getParent().getId());
        }
    }


    /**
     * Get all items by given company
     */
    public List<ItemDTOEx> getAllItemsByEntity(Integer entityId) {
        EntityBL entityBL = new EntityBL(entityId);
        CompanyDTO entity = entityBL.getEntity();
        CompanyDAS das = new CompanyDAS();

        boolean isRoot = das.isRoot(entityId);
        List<Integer> allCompanies = das.getChildEntitiesIds(entityId);
        allCompanies.add(entityId);

        List<ItemDTO> items = itemDas.findItems(entityId, allCompanies, isRoot);

        List<ItemDTOEx> ws = new ArrayList<ItemDTOEx>();
        for (ItemDTO item : items) {
            set(item.getId());
            ws.add(getWS(getDTO(entity.getLanguageId(), null, entityId, entity.getCurrencyId())));
        }

        return ws;
    }

    public boolean canBeDeleted(int id) {
        return new PlanItemBL().findPlanItemsCountByItemId(id) == 0;
    }

    public boolean itemHasVariableUsagePricing(Date pricingDate, Integer entityId) {
        return getEntity().getPrice(pricingDate, entityId).getStrategy().isVariableUsagePricing();
    }

    public Integer getOrderLineTypeId() {
        Integer type = null;
        for (ItemTypeDTO itemType : item.getItemTypes()) {
            type = itemType.getOrderLineTypeId();
        }
        return type;
    }

    public BigDecimal getTaxRate(Date invoiceGenerationDate, String taxTableName, String taxDateFormat) {
        @SuppressWarnings("rawtypes")
        MetaFieldValue taxSchemeMetaField = item.getMetaField(CommonConstants.TAX_SCHEME);
        return taxSchemeMetaField != null ? itemDas.getTaxRate((String) taxSchemeMetaField.getValue(), taxTableName,
                invoiceGenerationDate, taxDateFormat) :  BigDecimal.ZERO;
    }
}
