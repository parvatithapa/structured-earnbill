//TODO MODULARIZATION: MEDIATION 2.0 USED IN UPDATE CURRENT ORDER
package com.sapienter.jbilling.server.util;


import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepType;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.task.IMediationProcess;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
* Created by marcolin on 25/10/15.
*/
public class AbstractMediationStepResolverTask extends PluggableTask implements IMediationProcess {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AbstractMediationStepResolverTask.class));

    private Map<String, IMediationStep> steps = new LinkedHashMap<>();

    public void process(CallDataRecord record, MediationStepResult result)
            throws TaskException {

        if (result == null) {
            throw new TaskException("Results list cannot be null.");
        }


        // resolve mediation pricing fields
        result.setCurrencyId(1);

        if (record.getErrors().isEmpty()) {
            resolve(result, record.getFields());
        } else {
            LOG.debug("This Record " + record.getKey() + " has a format error in length or field values.");
            result.setDone(true);
            result.addError("ERR: " + record.getErrors().get(0));
        }


        // done!
        if (result.getUserId() != null && result.getCurrencyId() != null && result.getCurrentOrder() != null) {
            complete(result, record.getFields());
        }
    }

    /**
     * Resolve the given pricing fields into an item added to a customers current order.
     *
     * @param result result to process
     * @param fields pricing fields of the mediation record being processed
     */
    protected void resolve(MediationStepResult result, List<PricingField> fields) {
        // resolve target user, currency & event date
        if (result.getUserId() == null || result.getEventDate() == null) {

            executeMediationStep(MediationStepType.USER_CURRENCY, result, fields);
            executeMediationStep(MediationStepType.EVENT_DATE, result, fields);
        }

        //check if the result is set to done
        if (result.isDone()) {
            return;
        }

        // resolve current order
        executeMediationStep(MediationStepType.CURRENT_ORDER, result, fields);

        if (result.getCurrentOrder() != null) {
            // process line and item resolution step
            executeMediationStep(MediationStepType.ORDER_LINE_ITEM, result, fields);
        }
    }

    /**
     * Finish the mediation result.
     *
     * Adds the resolved lines to the user's current order, calculates the diff and marks the
     * mediation result as "done".
     *
     * If there are no lines resolved, then the record will simply be marked as "done" and
     * the mediation record will be handled as an error.
     *
     * @param result result to complete
     */
    protected void complete(MediationStepResult result, List<PricingField> fields) {

        if (result.getCurrentOrder() != null) {

            //add more steps
            executeMediationStep(MediationStepType.PRICING, result, fields);
            executeMediationStep(MediationStepType.ITEM_MANAGEMENT, result, fields);

            // save order - can be another mediation step
            executeMediationStep(MediationStepType.RECALCULATE_TAX, result, fields);
            if (result.isPersist()) {
                new OrderDAS().save((OrderDTO) result.getCurrentOrder());
            }

            executeMediationStep(MediationStepType.POST_PROCESS, result, fields);

            // calculate diff
            executeMediationStep(MediationStepType.DIFF_MANAGEMENT, result, fields);
        }

        result.setDone(true);
    }

    public void executeMediationStep(MediationStepType stepType, MediationStepResult result, List<PricingField> fields) {

        IMediationStep mediationStep = steps.get(stepType.toString());
        if (mediationStep != null) {
            mediationStep.executeStep(getEntityId(), result, fields);
        } else {
            LOG.warn("Mediation step not found for step type: " + stepType);
        }
    }

    public void setSteps(Map<String, IMediationStep> steps) {
        this.steps = steps;
    }
}
