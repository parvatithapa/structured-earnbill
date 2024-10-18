package com.sapienter.jbilling.appdirect.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParameterWS {

	private String name;

	private String value;

}
