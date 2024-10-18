package com.sapienter.jbilling.server.user;

import lombok.ToString;

@ToString
public class CreateUserResponseWS {

	private String accoutNumber;
	private Integer userId;
	private Integer entityId;

	public static CreateUserResponseWS of(String accoutNumber, Integer userId, Integer entityId) {
		return new CreateUserResponseWS(accoutNumber, userId, entityId);
	}

	private CreateUserResponseWS(String accoutNumber, Integer userId, Integer entityId) {
		this.accoutNumber = accoutNumber;
		this.userId = userId;
		this.entityId = entityId;
	}

	public String getAccoutNumber() {
		return accoutNumber;
	}

	public Integer getUserId() {
		return userId;
	}

	public Integer getEntityId() {
		return entityId;
	}
}