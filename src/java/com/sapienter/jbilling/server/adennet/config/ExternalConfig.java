/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.adennet.config;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.externalservice.configuration.ExternalConfigurationTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.sapienter.jbilling.server.adennet.AdennetConstants.EXTERNAL_SERVICE_CLASS_NAME;
import static com.sapienter.jbilling.server.adennet.AdennetConstants.EXTERNAL_SERVICE_INTERFACE_NAME;

@Slf4j
public class ExternalConfig {
    /**
     * Loads external config params for logged in entity.
     *
     * @return value
     */

    public String getValueFromExternalConfigParams(ParameterDescription param, Integer entityId) throws PluggableTaskException {
        try {
            PluggableTaskDAS pluggableTaskDAS = PluggableTaskDAS.getInstance();
            List<PluggableTaskDTO> pluggableTaskList = pluggableTaskDAS.findByEntityAndClassName(entityId, EXTERNAL_SERVICE_CLASS_NAME);
            if (CollectionUtils.isEmpty(pluggableTaskList)) {
                throw new SessionInternalError("AdennetExternalConfigurationTask is not configured for entity" + entityId, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
            PluggableTaskManager<ExternalConfigurationTask> pluggableTaskManager = new PluggableTaskManager<>(entityId,
                    pluggableTaskList.get(0).getType().getCategory().getId());
            ExternalConfigurationTask externalConfigurationTask = pluggableTaskManager.getInstance(
                    EXTERNAL_SERVICE_CLASS_NAME, EXTERNAL_SERVICE_INTERFACE_NAME, pluggableTaskList.get(0));
            return externalConfigurationTask.getExternalConfiguration().get(param.getName());
        } catch (PluggableTaskException pluggableTaskException) {
            log.error("loadExternalServiceConfigParams failed for entity={}", entityId, pluggableTaskException);
            throw pluggableTaskException;
        }
    }
}
