/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool;

import java.util.ArrayList;
import java.util.List;

/**
 * UsagePoolResetValueEnum
 * An enum with all possible values of Reset Value field on FUP.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public enum UsagePoolResetValueEnum {
	NO_CHANGES ("No Changes"),
	ADD_THE_INITIAL_VALUE ("Add The Initial Value"),
	RESET_TO_INITIAL_VALUE ("Reset To Initial Value"),
	ZERO ("Zero"),
	HOURS_PER_CALENDER_MONTH("Hours Per Calendar Month");
	
	private final String resetValue;
	
	UsagePoolResetValueEnum(String resetValue) {
		this.resetValue = resetValue;
	}
	
	public String getResetValue() {
		return this.resetValue;
	}
	
	/**
	 * A convenience method that returns all Reset Values 
	 * as a List of String.
	 * @return List<String>
	 */
	public static List<String> getResetValues() {
		List<String> resetValues = new ArrayList<String>(0);
		for (UsagePoolResetValueEnum usagePoolResetValue : UsagePoolResetValueEnum.values()) {
			resetValues.add(usagePoolResetValue.getResetValue());
		}
		return resetValues;
	}
	
	/**
	 * A convenience method that gives UsagePoolResetValueEnum
	 * for the String resetValue provided to it.
	 * @param resetValue
	 * @return UsagePoolResetValueEnum
	 */
	public static UsagePoolResetValueEnum getUsagePoolResetValueEnumByValue(String resetValue) {
		for (UsagePoolResetValueEnum rv : UsagePoolResetValueEnum.values()) {
			if (rv.getResetValue().equals(resetValue)) {
				return rv;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return getResetValue();
	}
}
