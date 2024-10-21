/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech.
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.client.crm;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.crm.task.CRMIntegrationTask;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Transactional
@Service
public class CRMHelperService {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Resource
	private PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS;
	private static final String INTERNAL_EVENTS_PLUGIN_INTERFACE_NAME = IInternalEventsTask.class.getName();
	private static final String CRM_INTEGRATION_CLASS_NAME = CRMIntegrationTask.class.getName();

	/**
	 * fetch plugin param value from CRMIntegrationTask if Configured
	 * @param entityId entity id
	 * @param paramName plugin param name
	 * @return plugin param value from CRMIntegrationTask if Configured else null
	 */
	public String getPluginParamValue(Integer entityId, String paramName) {
		CRMIntegrationTask crmIntegrationTask = loadCRMIntegrationPluginForEntity(entityId);
		return null != crmIntegrationTask ? crmIntegrationTask.getParameters().get(paramName) : null;
	}

	/**
	 * fetch all plugin param values from CRMIntegrationTask if Configured in Map
	 * @param entityId entityId
	 * @return all plugin param values from CRMIntegrationTask if Configured in Map else empty map
	 */
	public Map<String, String> getAllPluginParams(Integer entityId) {
		CRMIntegrationTask crmIntegrationTask = loadCRMIntegrationPluginForEntity(entityId);
		return null != crmIntegrationTask ? crmIntegrationTask.getParameters() : new HashMap<>();
	}

	/**
	 * fetch the CRMIntegrationTask Plugin if configured for the caller company <<EntityId>>
	 * @param entityId entityId
	 * @return return the CRMIntegrationTask Plugin object if configured else null
	 */
	private CRMIntegrationTask loadCRMIntegrationPluginForEntity(Integer entityId) {
		try {
			PluggableTaskManager<IInternalEventsTask> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_INTERNAL_EVENT);
			// Fetch CRM integration task if available
			Optional<PluggableTaskDTO> crmTask = taskManager.getAllTasks().stream()
				.filter(task -> task.getType().getClassName().equals(CRM_INTEGRATION_CLASS_NAME)).findFirst();

			if(crmTask.isPresent()) {
				IInternalEventsTask task = taskManager.getInstance(CRM_INTEGRATION_CLASS_NAME, INTERNAL_EVENTS_PLUGIN_INTERFACE_NAME, crmTask.get());
				if(!(task instanceof CRMIntegrationTask)) {
					throw new SessionInternalError("CRM Integration plugin not configured",new String[] { "no CRM Integration plugin "
						+ "configured for entityId "+ entityId }, HttpStatus.INTERNAL_SERVER_ERROR.value());
				}
				return (CRMIntegrationTask) task;
			}
			else {
				// Handle case where CRM integration task is not found
				logger.warn("No CRM Integration task found for entityId: {}", entityId);
				return null;
			}
		} catch (Exception e) {
			logger.error("loadCRMIntegrationPluginForEntity failed!", e);
			throw new SessionInternalError("loadCRMIntegrationPluginForEntity failed", new String[]{ "loadCRMIntegrationPluginForEntity "
				+ "failed for entityId "+ entityId }, HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}

	/**
	 * fetch the InvoiceNumber from the provided invoiceId
	 * @param invoiceId
	 * @return invoiceNumber
	 */
	public String getInvoiceNumber(Integer invoiceId) {
		InvoiceDAS invoiceDAS = new InvoiceDAS();
		InvoiceDTO invoiceDTO = invoiceDAS.findNow(invoiceId);
		return invoiceDTO.getNumber();
	}
}
