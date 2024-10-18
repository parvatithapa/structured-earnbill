package com.sapienter.jbilling.appdirect.subscription;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomAttributeWS {
	/**
	 * The name of the custom attribute
	 */
	private String name;
	/**
	 * Attribute type (text or multi-select)
	 */
	private String attributeType;
	/**
	 * The value for a text custom attributes
	 */
	private String value;
	/**
	 * The values selected for a multi-select custom attribute
	 */
	private Set<String> valueKeys;
}