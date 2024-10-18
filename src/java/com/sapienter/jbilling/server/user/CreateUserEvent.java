package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.system.event.Event;

public final class CreateUserEvent implements Event {

	private final Integer entityId;
	private final CreateUserRequestWS createUserRequest;
	private Integer userId;

	public static CreateUserEvent of(Integer entityId, CreateUserRequestWS createUserRequest) {
		return new CreateUserEvent(entityId, createUserRequest);
	}

	private CreateUserEvent(Integer entityId, CreateUserRequestWS createUserRequest) {
		this.entityId = entityId;
		this.createUserRequest = createUserRequest;
	}

	public CreateUserRequestWS getCreateUserRequest() {
		return createUserRequest;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	@Override
	public Integer getEntityId() {
		return entityId;
	}

	@Override
	public String getName() {
		return "CreateUserEvent-"+ getEntityId();
	}

}