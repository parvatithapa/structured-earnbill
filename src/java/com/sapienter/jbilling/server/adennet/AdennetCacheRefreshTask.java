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

package com.sapienter.jbilling.server.adennet;

import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.event.PlanUpdatedEvent;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.UserDeletedEvent;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestOperations;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sapienter.jbilling.server.adennet.AdennetExternalConfigurationTask.USAGE_MANAGEMENT_SERVICE_URL;
import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.INT;

public class AdennetCacheRefreshTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription ADENNET_EXTERNAL_TASK_PLUGIN_ID =
            new ParameterDescription("adennet_external_task_plugin_id", true, INT);

    private static final String REFRESH_USER_CACHE_URL = "refresh-cache/user";
    private static final String REFRESH_PLAN_CACHE_URL = "refresh-cache/plan/";

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
            UserDeletedEvent.class,
            AssetEvent.class,
            PlanUpdatedEvent.class
    };

    public AdennetCacheRefreshTask() {
        descriptions.add(ADENNET_EXTERNAL_TASK_PLUGIN_ID);
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
         AdennetHelperService adennetHelperService = Context.getBean(AdennetHelperService.class);
        RestOperations externalClient = Context.getBean("externalClient");

        String umsBaseUrl = adennetHelperService.getValueFromExternalConfigParams(USAGE_MANAGEMENT_SERVICE_URL);
        String userCacheRefreshUrl, planCacheRefreshUrl;
        if (!umsBaseUrl.endsWith("/")) {
            userCacheRefreshUrl = umsBaseUrl + "/" + REFRESH_USER_CACHE_URL;
            planCacheRefreshUrl = umsBaseUrl + "/" + REFRESH_PLAN_CACHE_URL;
        } else {
            userCacheRefreshUrl = umsBaseUrl + REFRESH_USER_CACHE_URL;
            planCacheRefreshUrl = umsBaseUrl + REFRESH_PLAN_CACHE_URL;
        }
        if (event instanceof UserDeletedEvent) {
            UserDeletedEvent userDeletedEvent = (UserDeletedEvent) event;
            List<OrderDTO> planOrders = new OrderDAS().findByUserSubscriptions(userDeletedEvent.getUserId());
            if (CollectionUtils.isEmpty(planOrders)) {
                UserDTO userDTO = UserBL.getUserEntity(userDeletedEvent.getUserId());
                if (userDTO != null) {
                    AssetDTO assetDTO = new AssetDAS().getAssetByIdentifier(userDTO.getUserName());
                    if (assetDTO != null) {
                        UserCacheRequest userCacheRequest = new UserCacheRequest(userDeletedEvent.getUserId(),
                                assetDTO.getSubscriberNumber(), 0, UserAction.USER_DELETED);
                        logger.debug("userCacheRefreshUrl={}, userCacheRequest={}", userCacheRefreshUrl, userCacheRequest);
                        externalClient.postForEntity(userCacheRefreshUrl, userCacheRequest, Void.class);
                    }
                }
            } else {
                for (OrderDTO planOrder : planOrders) {
                    PlanDTO plan = planOrder.getPlanFromOrder();
                    List<AssetDTO> planOrderAssets = planOrder.getAssets();
                    if (null != plan && CollectionUtils.isNotEmpty(planOrderAssets)) {
                        UserCacheRequest userCacheRequest = new UserCacheRequest(userDeletedEvent.getUserId(),
                                planOrderAssets.get(0).getSubscriberNumber(), plan.getId(), UserAction.USER_DELETED);
                        logger.debug("userCacheRefreshUrl={}, userCacheRequest={}", userCacheRefreshUrl, userCacheRequest);
                        externalClient.postForEntity(userCacheRefreshUrl, userCacheRequest, Void.class);
                    }
                }
            }
        } else if (event instanceof AssetEvent) {
            AssetEvent assetEvent = (AssetEvent) event;

            UserCacheRequest userCacheRequest = new UserCacheRequest(
                    assetEvent.getUserId(),
                    assetEvent.getIdentifier(),
                    assetEvent.getPlanId(),
                    assetEvent.getUserAction()
            );
            logger.info("AssetEvent::userCacheRefreshUrl={}, userCacheRequest={}", userCacheRefreshUrl, userCacheRequest);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserCacheRequest> httpEntity = new HttpEntity<>(userCacheRequest, httpHeaders);

            externalClient.postForEntity(userCacheRefreshUrl, httpEntity, Void.class);

        } else if (event instanceof PlanUpdatedEvent) {
            PlanUpdatedEvent planUpdatedEvent = (PlanUpdatedEvent) event;
            HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());

            logger.debug("planCacheRefreshUrl={}, planId={}", planCacheRefreshUrl, planUpdatedEvent.getPlan().getId());
            externalClient.postForEntity(planCacheRefreshUrl + planUpdatedEvent.getPlan().getId(), request, void.class);
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private Map<String, String> collectExternalTaskParams(PluggableTaskDTO externalTaskPlugin) {
        Map<String, String> externalTaskParam = new HashMap<>();
        for (PluggableTaskParameterDTO parameter : externalTaskPlugin.getParameters()) {
            Object value = parameter.getIntValue();
            if (value == null) {
                value = parameter.getStrValue();
                if (value == null) {
                    value = parameter.getFloatValue();
                }
            }
            externalTaskParam.put(parameter.getName(), value.toString());
        }
        return externalTaskParam;
    }

}
