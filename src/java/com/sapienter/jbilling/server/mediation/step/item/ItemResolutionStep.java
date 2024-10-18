/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.step.item;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractItemResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example mediation step that uses predefined item
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public class ItemResolutionStep extends AbstractItemResolutionStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ItemResolutionStep.class));

    private Integer itemId;
    private ItemDAS itemLoader;

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult mediationResult, List<PricingField> fields) {

        PricingField duration = PricingField.find(fields, "duration");
        PricingField destination = PricingField.find(fields, "dst");

        if (itemId != null) {
            OrderLineDTO line = newLine(itemId, duration.getDecimalValue());
            if (mediationResult.getLines() == null)
                mediationResult.setLines(new ArrayList<>());
            mediationResult.getLines().add(line);
        }

        LOG.debug("Number called = " + destination.getStrValue() + ", " + duration.getStrValue() + " minutes");
        mediationResult.setDescription("Phone call to " + destination.getStrValue());
        return true;
    }

    public OrderLineDTO newLine(Integer itemId, BigDecimal quantity) {
        OrderLineDTO line = new OrderLineDTO();
        line.setItemId(itemId);
        line.setQuantity(quantity);
        line.setDefaults();
        return line;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }

    @Override
    protected Map<String, Object> resolveItemByInternalNumber(Integer entityId, String internalNumber) {
        if (itemLoader == null) {
            itemLoader = new ItemDAS();
        }

        return buildItemMap(itemLoader.findItemByInternalNumber(internalNumber, entityId));
    }

    protected Integer resolveItemById(Integer entityId, Integer itemId) {
        if (itemLoader == null) {
            itemLoader = new ItemDAS();
        }

        return itemLoader.find(itemId).getId();
    }

    private Map<String, Object> buildItemMap(ItemDTO itemDTO) {
        Map<String, Object> itemMap = new HashMap<String, Object>();
        if (itemDTO != null) {
            itemMap.put(MediationStepResult.ITEM_ID, itemDTO.getId());
        }
        return itemMap;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public void setItemLoader(ItemDAS itemLoader) {
        this.itemLoader = itemLoader;
    }
}
