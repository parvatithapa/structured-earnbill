//TODO MODULARIZATION: MEDIATION 2.0 USED IN UPDATE CURRENT ORDER
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

package com.sapienter.jbilling.server.mediation.step;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderLineBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Example optional step for recalculating taxes to the order
 *
 * @author Panche Isajeski
 * @since 12/17/12
 */
public class RecalculateTaxMediationStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(RecalculateTaxMediationStep.class));

    private static final Integer TAX_ITEM_ID = 1;

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult mediationResult, List<PricingField> fields) {

        LOG.debug("Recalculating taxes for order");

        OrderDTO currentOrder = (OrderDTO) mediationResult.getCurrentOrder();
        OrderLineDTO taxLine = find(currentOrder, TAX_ITEM_ID);
        if (taxLine == null) {
            OrderLineBL.addItem(currentOrder, TAX_ITEM_ID);
        }

        OrderBL orderBL = new OrderBL(currentOrder);
        orderBL.recalculate(entityId);

        return true;
    }

    public static OrderLineDTO find(OrderDTO order, Integer itemId) {
        for (OrderLineDTO line : order.getLines()) {
            if (line.getItemId().equals(itemId)) {
                return line;
            }
        }
        return null;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }
}
