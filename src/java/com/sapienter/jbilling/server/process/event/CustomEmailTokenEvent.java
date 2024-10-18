package com.sapienter.jbilling.server.process.event;

import java.util.Map;

import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.system.event.Event;

public class CustomEmailTokenEvent implements Event {
	
	private final Integer entityId;
	private final Integer userId;
	private final MessageDTO message;
	private final Map<String, Object> messageParameters;
	
	
	public CustomEmailTokenEvent(Integer entityId, Integer userId,
			MessageDTO message, Map<String, Object> messageParameters) {
		this.entityId = entityId;
		this.userId = userId;
		this.message = message;
		this.messageParameters = messageParameters;
	}

	public CustomEmailTokenEvent(Integer entityId, Integer userId,
			MessageDTO message) {
		this(entityId,userId,message,null);
	}

	public CustomEmailTokenEvent(Integer entityId, Integer userId) {
		this(entityId,userId,null,null);
	}
	
	public CustomEmailTokenEvent(Integer entityId, Integer userId,
			Map<String, Object> messageParameters) {
		this(entityId,userId,null,messageParameters);
	}
	
	@Override
	public String getName() {
		return "Custom Email Token Event";
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}

	public Integer getUserId() {
		return userId;
	}
	
	public MessageDTO getMessage() {
		return message;
	}

	public Map<String, Object> getParameters() {
		return messageParameters;
	}
}
