//TODO MODULARIZATION: MEDIATION 2.0 USED IN UPDATE CURRENT ORDER
package com.sapienter.jbilling.server.mediation.step;

import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStep;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.AbstractMediationStepResolverTask;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Example mediation plug-in used by jbilling tests.
 * <p />
 * This implementation uses mediation steps to resolve the mediation bussiness logic
 *
 * @author Panche Isajeski
 * @since 12/16/12
 */
public class ExampleMediationStepResolverTask extends AbstractMediationStepResolverTask {

    private static final Logger LOG = Logger.getLogger(ExampleMediationStepResolverTask.class);

    @Override
    public void initializeParamters(PluggableTaskDTO task) throws PluggableTaskException {
        super.initializeParamters(task);

        Map<String, IMediationStep> config = Context.getBean(Context.Name.EXAMPLE_MEDIATION_STEP_CONFIGURATION);
        LOG.debug("Mediation step configuration for example mediation task: " + config);
        setSteps(config);
    }
}
