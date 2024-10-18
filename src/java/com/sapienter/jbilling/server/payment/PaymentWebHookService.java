package com.sapienter.jbilling.server.payment;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeCategoryDAS;
import grails.util.Holders;

@Transactional
public class PaymentWebHookService {

    private static final String WEBHOOK_HANDLER_INTERFACE_NAME = "com.sapienter.jbilling.server.payment.IPaymentWebHookHandler";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private PluggableTaskTypeCategoryDAS pluggableTaskTypeCategoryDAS;

    public Response handleWebHookEvent(String gatewayName, Integer entityId, HttpServletRequest request) {
        try {
            String requestBody = IOUtils.toString(request.getInputStream());
            /*Enumeration<String> headerNames = request.getHeaderNames();
            Map<String, String> headers = new HashMap<>();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }*/
            Map<String, String> headers = Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(element -> element, element -> request.getHeader(element)));

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("requestBody", requestBody);
            requestMap.put("headers", headers);
            IMethodTransactionalWrapper txWrapper = Holders.getApplicationContext().getBean(IMethodTransactionalWrapper.class);
            txWrapper.executeAsync(() -> {
                try {
                    Integer categoryId = pluggableTaskTypeCategoryDAS.findByInterfaceName(WEBHOOK_HANDLER_INTERFACE_NAME).getId();
                    PluggableTaskManager<IPaymentWebHookHandler> taskManager = new PluggableTaskManager<>(entityId, categoryId);
                    IPaymentWebHookHandler task = taskManager.getNextClass();
                    while (null != task) {
                        if (task.gatewayName().equalsIgnoreCase(gatewayName)) {
                            task.handleWebhookEvent(requestMap, entityId);
                        }
                        task = taskManager.getNextClass(); // fetch next task.
                    }
                } catch (Exception exception) {
                    logger.error("handleWebHookEvent failed!", exception);
                }
            });
            return Response.ok().build();
        } catch (Exception exception) {
            logger.error("handleWebHookEvent failed!", exception);
            Map<String, String> errorBody = Collections.singletonMap("error", ExceptionUtils.getStackTrace(exception));
            return Response.status(500)
                .entity(errorBody.toString())
                .build();
        }
    }
}
