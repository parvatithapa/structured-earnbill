package com.sapienter.jbilling.server.payment;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ErrorWS  implements Serializable {

	String code;
	String message;
	
	public ErrorWS() {
		
	}
	
	@JsonCreator
	public ErrorWS(
				  @JsonProperty(value = "code") String code 
				, @JsonProperty(value = "message") String message) {
		this.code = code;
		this.message = message;
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
