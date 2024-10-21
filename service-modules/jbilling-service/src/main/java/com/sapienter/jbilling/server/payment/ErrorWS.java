package com.sapienter.jbilling.server.payment;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

import lombok.Data;

@Data
public class ErrorWS  implements Serializable {

	@JsonProperty(value = "code")
	private final String code;
	
	@JsonProperty(value = "message")
	private final String message;
	
	// Below constructor is required for StripeRestTest.java
	@JsonCreator
	public ErrorWS(
				  @JsonProperty(value = "code") String code 
				, @JsonProperty(value = "message") String message) {
		this.code = code;
		this.message = message;
	}
}
