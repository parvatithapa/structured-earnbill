/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.pluggableTask.admin;

public class ParameterDescription {
	public enum Type { STR, INT, FLOAT, DATE, BOOLEAN };
	
	private final String name;
	private final boolean required;
	private final Type type;
	private boolean isPassword;
	private final String defaultValue;

	public ParameterDescription(String name, boolean required, Type type, boolean isPassword) {
        this(name, required, type, isPassword, "");
    }

    public ParameterDescription(String name, boolean required, Type type, String defaultValue) {
        this(name, required, type, false, defaultValue);
    }

	public ParameterDescription(String name, boolean required, Type type, boolean isPassword, String defaultValue) {
		super();
		this.name = name;
		this.required = required;
		this.type = type;
		this.isPassword = isPassword;
        this.defaultValue = defaultValue;
	}

	public ParameterDescription(String name, boolean required, Type type) {
        this(name, required, type, false, "");
	}
	
	public boolean getIsPassword() {
		return isPassword;
	}
	
	public String getName() {
		return name;
	}
	public boolean isRequired() {
		return required;
	}
	public Type getType() {
		return type;
	}

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
	public String toString() {
		return "ParameterDescription [name=" + name + ", required=" + required
				+ ", type=" + type + "]";
	}
}
