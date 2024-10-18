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
import com.sapienter.jbilling.server.order.db.OrderDTO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Example mediation step that resolves current order for the user
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public class CurrentOrderResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CurrentOrderResolutionStep.class));

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult result, List<PricingField> fields) {

        // validate call duration
        PricingField duration = PricingField.find(fields, "duration");
        if (duration == null || duration.getIntValue().intValue() < 0) {
            result.setDone(true);
            result.addError("ERR-DURATION");
            return false;
        }

        // discard unanswered calls
        PricingField disposition = PricingField.find(fields, "disposition");
        if (disposition == null || !disposition.getStrValue().equals("ANSWERED")) {
            result.setDone(true);
            return false;
        }

        // validate that we were able to resolve the billable user, currency and date
        if (result.getCurrentOrder() == null) {
            if (result.getUserId() != null
                    && result.getCurrencyId() != null
                    && result.getEventDate() != null) {

                OrderDTO currentOrder = getCurrentOrder(result);
                result.setCurrentOrder(currentOrder);
                return true;
            }
        }

        LOG.debug("Mediation result " + result.getCdrRecordKey() + " cannot be processed!");
        return false;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
    }

    public OrderDTO getCurrentOrder(MediationStepResult result) {

        OrderDTO currentOrder = OrderBL.getOrCreateCurrentOrder(result.getUserId(),
                result.getEventDate(),
                null,
                result.getCurrencyId(),
                result.isPersist());

        return  currentOrder;
    }
}
