package com.sapienter.jbilling.server.pluggableTask.dt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.UndoMediationFilterTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

public class DTUndoMediationFilterTask extends PluggableTask implements UndoMediationFilterTask {

	private static final String METERED_USAGE_INTEGRATION_TASK= "com.sapienter.jbilling.server.meteredUsage.MeteredUsageIntegrationTask";
	private static final String ORDER_STATUS_UPLOADED="order_status_uploaded";



	public List<Integer> getOrderIdsEligibleForUndoMediation(UUID mediationProcessId) throws TaskException {

		MediationService service = Context.getBean(MediationService.BEAN_NAME);
		MediationProcessService mediationProcessService = Context.getBean("mediationProcessService");
		try {

			MediationProcess process = mediationProcessService.getMediationProcess(mediationProcessId);
			PluggableTaskDTO pluggableTaskDTO = new PluggableTaskBL().getByClassAndCategoryAndEntity(METERED_USAGE_INTEGRATION_TASK, Constants.PLUGGABLE_TASK_SCHEDULED, process.getEntityId());
			if (pluggableTaskDTO != null) {
				Optional<String> orderUploadedStatus = pluggableTaskDTO.getParameters().stream()
					.filter(param -> ORDER_STATUS_UPLOADED.equalsIgnoreCase(param.getName().trim()))
					.map(param -> param.getValue())
					.findAny();
				return service.getOrdersForMediationProcessByStatusExcluded(mediationProcessId, 
						Integer.parseInt(orderUploadedStatus.get().trim()));
			}else{
				throw new SessionInternalError("Unable to find MeteredUsageIntegrationTask plugin in Scheduled Plug-ins Category");
			}

		} catch(Exception e){
			throw new TaskException("Exception in getting orders for Undo Mediation : "+e.getMessage());
		}

	}
}
