package com.sapienter.jbilling.server.payment;

import java.util.Map;

import javax.ws.rs.core.Response;

public interface IPaymentWebHookHandler {
	Response handleWebhookEvent(Map<String, Object> requestMap, Integer entityId);
	String gatewayName();
	Integer getAccountTypeId();
}
