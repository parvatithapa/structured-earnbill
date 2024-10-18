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
package com.sapienter.jbilling.server.item.tasks;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderHelper;
import com.sapienter.jbilling.server.order.OrderLinePlanItemDTOEx;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineInfo;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pricing.strategy.AbstractPricingStrategy;
import com.sapienter.jbilling.server.pricing.strategy.RateCardPricingStrategy;


public class BasicItemManager extends PluggableTask implements IItemPurchaseManager {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ItemDTO item = null;
    private OrderLineDTO latestLine = null;

    @Override
    public void addItem(ItemPurchaseManagerContext context) throws TaskException {

        Integer itemId = context.getItemId();
        BigDecimal quantity = context.getQuantity();
        OrderDTO order = context.getOrder();
        logger.debug("Adding {} of item {} to order {}", quantity, itemId, order);

        // validate decimal quantity
        validateContext(context);

        // build the order line
        OrderLineDTO newLine = getOrderLine(context);

        List<CallDataRecord> records = context.getRecords();
        List<PricingField> pricingFields = null;
        if(CollectionUtils.isNotEmpty(records)) {
            pricingFields = records.get(0).getFields();
        }
        List<OrderLineDTO> lines = context.getLines();
        // check if line already exists on the order & update
        String callIdentifier = getCallIdentifierFromPricingFields(pricingFields);
        OrderLineDTO oldLine;
        if(StringUtils.isNotEmpty(callIdentifier)) {
            newLine.setCallIdentifier(callIdentifier);
            oldLine = order.getLine(itemId, callIdentifier);
        } else {
            oldLine = order.getOldLine(itemId);
        }
        if (oldLine != null && lines != null) {
            OrderLineDTO updatedOldLine = OrderHelper.findOrderLineWithId(lines, oldLine.getId());
            if (updatedOldLine != null && updatedOldLine.getDeleted() == 1) {
                oldLine = null;
            }
        }
        if (oldLine == null) {
            if(StringUtils.isNotEmpty(callIdentifier)) {
                newLine.setCallCounter(1l);
                newLine.setCallIdentifier(callIdentifier);
            }
            addNewLine(order, newLine);
        } else {
            if(StringUtils.isNotEmpty(callIdentifier)) {
                Long callCounter = oldLine.getCallCounter();
                newLine.setCallCounter(++callCounter);
                newLine.setCallIdentifier(callIdentifier);
                oldLine.setCallIdentifier(callIdentifier);
            }
            updateExistingLine(order, newLine, oldLine);
        }
    }

    protected void validateContext(ItemPurchaseManagerContext context) throws TaskException {
        Integer itemId = context.getItemId();
        ItemBL itemBL = new ItemBL(itemId);

        // validate decimal quantity
        if (context.getQuantity().remainder(Constants.BIGDECIMAL_ONE).compareTo(BigDecimal.ZERO) > 0 &&
                itemBL.getEntity().getHasDecimals().equals(0)) {
            latestLine = null;
            throw new ItemDecimalsException("Item " + itemId + " does not allow decimal quantities.");
        }
    }

    protected void setMetaFieldsOnLine(OrderLineDTO line, ItemPurchaseManagerContext context) {
        logger.debug("setting meta fields on line");
    }

    protected String getCallIdentifierFromPricingFields(List<PricingField> pricingFields) {
        return StringUtils.EMPTY;
    }

    /**
     * Add a new line to the order
     *
     * @param order order
     * @param newLine new line to add
     */
    protected void addNewLine(OrderDTO order, OrderLineDTO newLine) {
        logger.debug("Adding new line to order: {}", newLine);

        newLine.setPurchaseOrder(order);
        order.getLines().add(newLine);

        this.latestLine = newLine;
    }

    /**
     * Update an existing line on the order with the quantity and dollar amount of the new line.
     *
     * @param order order
     * @param newLine new order line
     * @param oldLine existing order line to be updated
     */
    protected void updateExistingLine(OrderDTO order, OrderLineDTO newLine, OrderLineDTO oldLine) {
        OrderLineDAS lineDAS = new OrderLineDAS();
        OrderLineDTO dbLine = lineDAS.findNow(oldLine.getId());
        if(null!=dbLine) {
            oldLine = dbLine;
        }
        oldLine.setOldOrderLineInfo(new OrderLineInfo(oldLine.getQuantity(), oldLine.getAmount(),
                oldLine.getFreeUsagePoolQuantity(), oldLine.getPrice()));
        logger.debug("Updating existing order with line quantity & amount: {}", newLine);

        BigDecimal quantity = oldLine.getQuantity().add(newLine.getQuantity());
        oldLine.setQuantity(quantity);

        BigDecimal amount = oldLine.getAmount().add(newLine.getAmount());
        oldLine.setAmount(amount);
        Long callCounter = newLine.getCallCounter();
        oldLine.setCallCounter(callCounter);
        this.latestLine = oldLine;

    }

    /**
     * Builds a new order line for the given item, currency and user. The item will be priced according
     * to the quantity purchased, the order it is being added to and the user's own prices.
     *
     * @see ItemBL#getDTO(Integer, Integer, Integer, Integer, java.math.BigDecimal, com.sapienter.jbilling.server.order.db.OrderDTO)
     *
     * @param itemId item id
     * @param languageId language id
     * @param userId user id
     * @param currencyId currency id
     * @param quantity quantity being purchased
     * @param entityId entity id
     * @param order order the line will be added to
     * @return new order line
     */
    protected OrderLineDTO getOrderLine(ItemPurchaseManagerContext context) {
        Integer itemId = context.getItemId();
        // item BL with pricing fields
        ItemBL itemBl = new ItemBL(itemId);
        List<CallDataRecord> records = context.getRecords();
        if (records != null) {
            List<PricingField> fields = new ArrayList<>();
            for (CallDataRecord record : records) {
                PricingField.addAll(fields, record.getFields());
            }

            logger.debug("Including {} field(s) for pricing.", fields.size());
            itemBl.setPricingFields(fields);
        }
        Integer languageId = context.getLanguageId();
        Integer entityId   = context.getEntityId();
        Integer currencyId = context.getCurrencyId();
        BigDecimal quantity = context.getQuantity();
        OrderDTO order = context.getOrder();
        boolean singlePurchase = context.isSinglePurchase();
        Date eventDate = context.getEventDate();
        Integer userId = context.getUserId();
        // get the item with the price populated for the quantity being purchased
        this.item = itemBl.getDTO(languageId, userId, entityId, currencyId, quantity, order, null, singlePurchase, eventDate);
        logger.debug("Item {} priced as {}", itemId, item.getPrice());

        // There is one array of OrderLinePlanItemDTOEx in a orderLine, for editable and non-editable plan.
        // Extract it for next use to overwrite planItemDto.item values.
        List<OrderLineDTO> lines = context.getLines();
        OrderLinePlanItemDTOEx[] orderLinePlanItemDTOs = null;
        if (lines != null) {
            for (OrderLineDTO line : lines) {
                if (line.getOrderLinePlanItems() != null && line.getOrderLinePlanItems().length > 0) {
                    orderLinePlanItemDTOs = line.getOrderLinePlanItems();
                    break;
                }
            }
        }

        OrderLinePlanItemDTOEx overwriteOrderLinePlanItemDTO = null;
        if (orderLinePlanItemDTOs != null) {
            for(OrderLinePlanItemDTOEx orderLinePlanItemDTOEx : orderLinePlanItemDTOs) {
                if (orderLinePlanItemDTOEx.getItemId() == item.getId() && orderLinePlanItemDTOEx.getLineCreated() == null) {
                    overwriteOrderLinePlanItemDTO = orderLinePlanItemDTOEx;
                    break;
                }
            }
        }
        // build the order line
        OrderLineDTO line = new OrderLineDTO();
        line.setItem(item);
        line.setPercentage(item.isPercentage());
        line.setQuantity(quantity != null ? quantity : BigDecimal.ZERO);

        // set meta fields on OrderLine.
        setMetaFieldsOnLine(line, context);
        // set line price
        line.setPrice( null != item.getPrice() ? item.getPrice() : BigDecimal.ZERO );

        // calculate total line dollar amount
        if ( item.isPercentage() ) {
            line.setAmount(item.getPercentage());
        } else {
            if ( null != line.getPrice() && null != line.getQuantity() ) {
                line.setAmount(line.getPrice().multiply(line.getQuantity()));
            } else {
                line.setAmount(BigDecimal.ZERO);
            }
        }

        if (overwriteOrderLinePlanItemDTO != null) {
            // mark this OrderLinePlanItemDTOEx as used already for line creation
            overwriteOrderLinePlanItemDTO.setLineCreated(line);
            // set orderLinePlanItemDTOs to created line for usage in nested plan items
            // (BasicItemManager will be called recursively to created lines for nested plans - orderLinePlanItemDTOEx will be needed)
            line.setOrderLinePlanItems(orderLinePlanItemDTOs);
            //set UseItem as false if description was changed
            String itemDescription = item.getDescription(languageId);
            if (StringUtils.isNotEmpty(itemDescription) && overwriteOrderLinePlanItemDTO.getDescription() != null
                    && !itemDescription.equals(overwriteOrderLinePlanItemDTO.getDescription())) {
                line.setUseItem(false);
            }

            line.setDescription(overwriteOrderLinePlanItemDTO.getDescription());

            if (Integer.valueOf(1).equals(item.getAssetManagementEnabled())) {
                Set<AssetDTO> assetDTOSet = new HashSet<>();
                for(Integer assetId: overwriteOrderLinePlanItemDTO.getAssetIds()) {
                    AssetDTO assetDto = findAssetEntityOrDto(assetId, order);
                    assetDTOSet.add(assetDto);
                }
                line.setAssets(assetDTOSet);
            }
            if (overwriteOrderLinePlanItemDTO.getMetaFields() != null && !overwriteOrderLinePlanItemDTO.getMetaFields().isEmpty()) {
                //set the meta fields
                MetaFieldHelper.updateMetaFieldsWithValidation(item.getEntity().getLanguageId(), item.getOrderLineMetaFields(), line,
                        overwriteOrderLinePlanItemDTO);
            }

        } else {
            line.setDescription(item.getDescription(languageId));
            line.setSipUri(context.getSipUri());

            if (Integer.valueOf(1).equals(item.getAssetManagementEnabled()) && lines != null) {

                for (OrderLineDTO orderLineDTO : lines) {
                    if (CollectionUtils.isNotEmpty(orderLineDTO.getItem().getPlans())) {
                        Set<AssetDTO> tmpAssets = new HashSet<>();
                        for (AssetDTO tmpAssetDTO: orderLineDTO.getAssets()) {
                            AssetDTO assetDto = findAssetEntityOrDto(tmpAssetDTO.getId(), order);
                            tmpAssets.add(assetDto);
                        }
                        line.setAssets(tmpAssets);
                    }
                }
            }
        }

        // round dollar amount
        if ( null != line.getAmount()) {
            line.setAmount(line.getAmount().setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
        }

        line.setDeleted(0);
        line.setTypeId(item.getOrderLineTypeId());
        line.setEditable(OrderBL.lookUpEditable(item.getOrderLineTypeId()));
        line.setDefaults();


        logger.debug("Built new order line: {}", line);

        //This code updates the records parameter with the modifications in the pricingField list.
        //this is used in DiameterBL#settleReservation
        List<PricingField> modifiedFields = itemBl.getPricingFields();
        if (modifiedFields != null && AbstractPricingStrategy.find(modifiedFields, RateCardPricingStrategy.FROM_SETTLE_RESERVATION) != null) {
            records.clear();
            CallDataRecord record = new CallDataRecord();
            record.setFields(modifiedFields);
            records.addAll(new ArrayList<>(Arrays.asList(record)));
        }

        return line;
    }

    private AssetDTO findAssetEntityOrDto(Integer assetId, OrderDTO order) {
        AssetBL assetBL = new AssetBL();
        AssetDTO entity = assetBL.find(assetId);
        // return dto for new order
        if (order == null || order.getId() == null || order.getId() <= 0) {
            return new AssetDTO(entity);
        } else {
            // compare current order with ref in hibernate context
            OrderDAS orderDAS = new OrderDAS();
            if (orderDAS.find(order.getId()) == order) {
                return entity; // return entity for persisted order
            } else {
                return new AssetDTO(entity);
            }
        }
    }

    public ItemDTO getItem() {
        return item;
    }

    public OrderLineDTO getLatestLine() {
        return latestLine;
    }
}
