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

package com.sapienter.jbilling.server.mediation.step.pricing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;

/**
* Mediation step that resolves the item price
*
* @author Panche Isajeski
* @since 12/16/12
*/
public class PricingResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PricingResolutionStep.class));

    private ItemBL itemLoader;

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult mediationResult, List<PricingField> fields) {

        for (Object lineObject : mediationResult.getLines()) {
            OrderLineDTO line = (OrderLineDTO) lineObject;
            if (line.getPrice() == null) {
                LOG.debug("Calculating price for line " + line);

                if (itemLoader == null) {
                    itemLoader = new ItemBL();
                }
                itemLoader.set(line.getItemId());
                itemLoader.setPricingFields(fields);
                BigDecimal price = itemLoader.getPrice(mediationResult.getUserId(),
                        mediationResult.getCurrencyId(),
                        line.getQuantity(),
                        entityId,
                        (OrderDTO) mediationResult.getCurrentOrder(),
                        line,
                        true, mediationResult.getEventDate());
                LOG.debug("Pricing step price resolved to %s", price);
                line.setPrice(price);
            }

            if (line.getPrice() != null && line.getQuantity() != null) {
                line.setAmount(line.getPrice().multiply(line.getQuantity()));
            }
        }

        return true;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }

    public void setItemLoader(ItemBL itemLoader) {
        this.itemLoader = itemLoader;
    }
}
