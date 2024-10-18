package com.sapienter.jbilling.server.user.event;

import com.sapienter.jbilling.server.system.event.Event;

public class UserDeletedEvent implements Event {

	private Integer entityId;
	private Integer userId;

	public UserDeletedEvent(Integer entityId, Integer userId) {
		this.entityId = entityId;
		this.userId = userId;
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}

	@Override
	public String getName() {
		return "UserDeletedEvent-"+getEntityId();
	}

	public Integer getUserId() {
		return userId;
	}
}
