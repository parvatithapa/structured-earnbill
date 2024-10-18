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
import com.sapienter.jbilling.server.order.OrderLineBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
* Mediation step that adds a line for the the item
*
* @author: Panche Isajeski
* @since: 12/16/12
*/
public class ItemManagementStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ItemManagementStep.class));

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult mediationResult, List<PricingField> fields) {

        if (mediationResult.getOldLines() == null || mediationResult.getOldLines().isEmpty()) {
            OrderDTO currentOrder = (OrderDTO) mediationResult.getCurrentOrder();
            List<Object> objects = new ArrayList<>();
            objects.addAll(OrderLineBL.copy(currentOrder.getLines()));
            mediationResult.setOldLines(objects);
        }

        // update the lines in the current order
        for (Object line : mediationResult.getLines()) {
            addToOrderLine((OrderDTO) mediationResult.getCurrentOrder(), (OrderLineDTO) line, false);
        }

        return true;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }

    protected void addToOrderLine(OrderDTO currentOrder, OrderLineDTO orderLineDTO, boolean persist) {
        OrderLineBL.addLine(currentOrder, orderLineDTO, false);
    }
}
