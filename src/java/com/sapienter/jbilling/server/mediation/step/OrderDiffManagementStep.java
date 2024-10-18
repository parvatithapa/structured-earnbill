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

import java.util.ArrayList;
import java.util.List;

/**
* Mediation step that calculates the order diff lines.
*
* @author: Panche Isajeski
* @since: 12/16/12
*/
public class OrderDiffManagementStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderDiffManagementStep.class));

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult mediationResult, List<PricingField> fields) {
        OrderDTO currentOrder = (OrderDTO) mediationResult.getCurrentOrder();
        if (currentOrder.getLines() != null && mediationResult.getOldLines() != null) {
            List<OrderLineDTO> oldLines = new ArrayList<>();
            for (Object object: mediationResult.getOldLines())
                oldLines.add((OrderLineDTO) object);

            List<OrderLineDTO> diffLines = OrderLineBL.diffOrderLines(oldLines,
                    currentOrder.getLines());
            List<Object> diffLinesObject = new ArrayList<>();
            for (OrderLineDTO orderLineDTO: diffLines) diffLinesObject.add(orderLineDTO);
            // calculate diff
            mediationResult.setDiffLines(diffLinesObject);

            // check order line quantities
            if (mediationResult.isPersist()) {
                new OrderBL().checkOrderLineQuantities(oldLines,
                        currentOrder.getLines(),
                        entityId,
                        currentOrder.getId(),
                        true,
                        false);
            }
        }

        return true;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }

}
