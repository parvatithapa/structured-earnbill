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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderHelper;
import com.sapienter.jbilling.server.order.OrderLinePlanItemDTOEx;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pricing.strategy.AbstractPricingStrategy;
import com.sapienter.jbilling.server.pricing.strategy.RateCardPricingStrategy;
import com.sapienter.jbilling.server.rule.RulesBaseTask;
import com.sapienter.jbilling.server.util.Constants;

/**
 * @author Mahesh Shivarkar
 * @since Mar 27, 2019
 */

public class SPCUsageManagerTask extends RulesBaseTask implements IItemPurchaseManager {

    // for the rules task, needed due to some class hierarchy problems further up
    // todo: remove when all rules plug-ins are deleted.
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(SPCUsageManagerTask.class));
    private ItemDTO item = null;
    private OrderLineDTO latestLine = null;

    public static final ParameterDescription VOIP_USAGE_FIELD_NAME =
            new ParameterDescription("VOIP_Usage_Field_Name", true, ParameterDescription.Type.STR);
    public static final ParameterDescription INTERNET_USAGE_FIELD_NAME =
            new ParameterDescription("Internate_Usage_Field_Name", true, ParameterDescription.Type.STR);

    {
        descriptions.add(VOIP_USAGE_FIELD_NAME);
        descriptions.add(INTERNET_USAGE_FIELD_NAME);
    }
    @Override
    protected FormatLogger getLog() { return LOG; }

    @Override
    public void addItem(Integer itemId, BigDecimal quantity, Integer languageId, Integer userId, Integer entityId,
            Integer currencyId, OrderDTO order, List<CallDataRecord> records,
            List<OrderLineDTO> lines, boolean singlePurchase, String sipUri, Date eventDate) throws TaskException {

        LOG.debug("Adding %s of item %s to order %s", quantity, itemId, order);

        ItemBL item = new ItemBL(itemId);

        // validate decimal quantity
        if (quantity.remainder(Constants.BIGDECIMAL_ONE).compareTo(BigDecimal.ZERO) > 0) {
            if (item.getEntity().getHasDecimals().equals(0)) {
                latestLine = null;
                throw new ItemDecimalsException("Item " + itemId + " does not allow decimal quantities.");
            }
        }

        String voipUsageFieldName = parameters.get(VOIP_USAGE_FIELD_NAME.getName());
        if (voipUsageFieldName == null) {
            throw new TaskException("Cannot find configured VOIP Usage Field Name: " + voipUsageFieldName);
        }

        String internateUsageFieldName = parameters.get(INTERNET_USAGE_FIELD_NAME.getName());
        if (internateUsageFieldName == null) {
            throw new TaskException("Cannot find configured Internet Usage Field Name: " + internateUsageFieldName);
        }

        //Getting the Values for setting the MetaField
        String usageValue = null;

        boolean isFound = false;
        if(CollectionUtils.isNotEmpty(records)) {
            List<PricingField> pricingFields = records.get(0).getFields();

            if(null!= PricingField.find(pricingFields, voipUsageFieldName) &&
                    null!= PricingField.find(pricingFields, internateUsageFieldName)) {
                throw new TaskException("invalid jmr record passed");
            }

            for (PricingField pricingField : records.get(0).getFields()) {
                String pricingFieldName = pricingField.getName();
                if (pricingFieldName.equals(voipUsageFieldName) || pricingFieldName.equals(internateUsageFieldName)) {
                    isFound = true;
                    usageValue = pricingField.getStrValue();
                    break;
                }
            }

            if (!isFound) {
                throw new TaskException("Usage field name of file does not match with configured field name parameter");
            } else if (null == usageValue) {
                throw new SessionInternalError("VOIP Usage or Internate Usage both should not be null");
            }
        }

        // build the order line
        OrderLineDTO newLine = getOrderLine(itemId, languageId, userId, currencyId, quantity, entityId, order,
                records, lines, singlePurchase, sipUri, eventDate, usageValue);
        LOG.debug("newLine %s created for Usage Value %s", newLine, usageValue);
        OrderLineDTO oldLine = null;
        OrderLineDTO updatedOldLine = null;
        // check if line already exists on the order & update
        if (null != records && !records.isEmpty() ) {
            oldLine = order.getLine(itemId, usageValue);
        } else {
            oldLine = order.getOldLine(itemId);
        }

        if (oldLine != null && lines != null) {
            updatedOldLine = OrderHelper.findOrderLineWithId(lines, oldLine.getId());
            if (updatedOldLine != null && updatedOldLine.getDeleted() == 1) {
                oldLine = null;
            }
        }
        if (oldLine == null) {
            if(usageValue!=null) {
                newLine.setCallCounter(1l);
            }

            addNewLine(order, newLine);

        } else {
            if (usageValue!=null) {
                Long callCounter = oldLine.getCallCounter();
                newLine.setCallCounter(++callCounter);
            }

            updateExistingLine(order, newLine, oldLine);
        }
    }

    /**
     * Add a new line to the order
     *
     * @param order order
     * @param newLine new line to add
     */
    protected void addNewLine(OrderDTO order, OrderLineDTO newLine) {
        LOG.debug("Adding new line to order: %s", newLine);
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
    protected void updateExistingLine(OrderDTO order, OrderLineDTO newLine, OrderLineDTO oldLine) throws TaskException{
        //todo: update order line assets
        OrderLineDAS lineDAS = new OrderLineDAS();
        OrderLineDTO dbLine = lineDAS.findNow(oldLine.getId());
        if(null!=dbLine) {
            oldLine = dbLine;
        }

        LOG.debug("Updating existing order with line quantity & amount: %s", newLine);

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
    protected OrderLineDTO getOrderLine(Integer itemId, Integer languageId, Integer userId, Integer currencyId,
            BigDecimal quantity, Integer entityId, OrderDTO order, List<CallDataRecord> records,
            List<OrderLineDTO> lines, boolean singlePurchase, String sipUri, Date eventDate,
            String fieldValue) {

        // item BL with pricing fields
        ItemBL itemBl = new ItemBL(itemId);
        if (records != null) {
            List<PricingField> fields = new ArrayList<PricingField>();
            for (CallDataRecord record : records) {
                PricingField.addAll(fields, record.getFields());
            }

            LOG.debug("Including %d field(s) for pricing.", fields.size());
            itemBl.setPricingFields(fields);
        }

        // get the item with the price populated for the quantity being purchased
        this.item = itemBl.getDTO(languageId, userId, entityId, currencyId, quantity, order, null, singlePurchase, eventDate);
        LOG.debug("Item %s priced as %s", itemId, item.getPrice());

        // There is one array of OrderLinePlanItemDTOEx in a orderLine, for editable and non-editable plan.
        // Extract it for next use to overwrite planItemDto.item values.
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
        MetaField metaField = null;
        EntityType[] type = {EntityType.ORDER_LINE};

        OrderLineDTO line = new OrderLineDTO();
        line.setItem(item);
        line.setQuantity(quantity != null ? quantity : BigDecimal.ZERO);
        if (null != order.getPlanItemId()) {
            // Setting the MetaFields in the OrderLine
            metaField = MetaFieldBL.getFieldByName(order.getUser().getCompany().getId(), type, Constants.PLAN_ITEM_ID);
            line.setMetaField(metaField, order.getPlanItemId());
        }

        // Setting the value in the OrderLine as a call Identifier
        line.setCallIdentifier(fieldValue);

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
            if (item.getDescription() != null && overwriteOrderLinePlanItemDTO.getDescription() != null
                    && !item.getDescription().equals(overwriteOrderLinePlanItemDTO.getDescription())) {
                line.setUseItem(false);
            }

            line.setDescription(overwriteOrderLinePlanItemDTO.getDescription());

            if (Integer.valueOf(1).equals(item.getAssetManagementEnabled())) {
                Set<AssetDTO> assetDTOSet = new HashSet<AssetDTO>();
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
            line.setDescription(item.getDescription());
            line.setSipUri(sipUri);

            if (Integer.valueOf(1).equals(item.getAssetManagementEnabled()) && lines != null) {

                for (OrderLineDTO orderLineDTO : lines) {
                    if (orderLineDTO.getItem().getPlans() != null && orderLineDTO.getItem().getPlans().size() > 0) {
                        Set<AssetDTO> tmpAssets = new HashSet<AssetDTO>();
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


        LOG.debug("Built new order line: %s", line);

        //This code updates the records parameter with the modifications in the pricingField list.
        //this is used in DiameterBL#settleReservation
        List<PricingField> modifiedFields = itemBl.getPricingFields();
        if (modifiedFields != null) {
            if (AbstractPricingStrategy.find(modifiedFields, RateCardPricingStrategy.FROM_SETTLE_RESERVATION) != null) {
                records.clear();
                CallDataRecord record = new CallDataRecord();
                record.setFields(modifiedFields);
                records.addAll(new ArrayList<>(Arrays.asList(record)));
            }
        }

        return line;
    }
    //todo: temp fix for NonUniqueObjectException, rewrite
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
