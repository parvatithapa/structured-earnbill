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

/*
 * Created on Apr 15, 2003
 *
 */
package com.sapienter.jbilling.server.pluggableTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;


public abstract class PluggableTask {

	public static final DateTimeFormatter PARAMETER_DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd-HHmm");

	public final List<ParameterDescription> descriptions = new ArrayList<>();

	protected Map<String, String> parameters = null;
	private Integer entityId = null;
	protected PluggableTaskDTO task = null;

	public List<ParameterDescription> getParameterDescriptions() {
		return descriptions;
	}

	protected Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public Integer getTaskId() {
		return task.getId();
	}

	public void initializeParamters(PluggableTaskDTO task)
			throws PluggableTaskException {
		Collection<PluggableTaskParameterDTO> dBparameters = task.getParameters() != null ? task.getParameters() :
			Collections.emptyList();
		parameters = new HashMap<>();
		entityId = task.getEntityId();
		this.task = task;
		if (dBparameters.size() < task.getType().getMinParameters()) {
			throw new PluggableTaskException("Type [" + task.getType().getClassName() + "] requires at least " +
					task.getType().getMinParameters() + " parameters." +
					dBparameters.size() + " found.");
		}

		if (dBparameters.isEmpty()) {
			return;
		}

		for (PluggableTaskParameterDTO parameter : dBparameters) {
			Object value = parameter.getIntValue();
			if (value == null) {
				value = parameter.getStrValue();
				if (value == null) {
					value = parameter.getFloatValue();
				}
			}

			// change: all the parameters will be strings in jB3. TODO: drop the int_value, float_value columns
			parameters.put(parameter.getName(), value.toString());
		}
	}

	/**
	 * Returns the plug-in parameter value as a String if it exists, or
	 * returns the given default value if it doesn't
	 *
	 * @param key plug-in parameter name
	 * @param defaultValue default value if parameter not defined
	 * @return parameter value, or default if not defined
	 */
	protected String getParameter(String key, String defaultValue) {
		String value = parameters.get(key);
		return StringUtils.isNotBlank(value) ? value : defaultValue;
	}

	/**
	 * Returns the plug-in parameter value as an Integer if it exists, or
	 * returns the given default value if it doesn't
	 *
	 * @param key plug-in parameter name
	 * @param defaultValue default value if parameter not defined
	 * @return parameter value, or default if not defined
	 */
	protected Integer getParameter(String key, Integer defaultValue) throws PluggableTaskException {
		String value = parameters.get(key);

		try {
			return StringUtils.isNotBlank(value) ? Integer.parseInt(value) : defaultValue;
		} catch (NumberFormatException e) {
			throw new PluggableTaskException(key + " could not be parsed as an integer!", e);
		}
	}

	/**
	 * Returns the plug-in parameter value as a boolean value if it exists, or
	 * returns the given default value if it doesn't.
	 *
	 * "true" and "True" equals Boolean.TRUE, all other values equate to false.
	 *
	 * @param key plug-in parameter name
	 * @param defaultValue default value if parameter not defined
	 * @return parameter value, or default if not defined
	 */
	protected Boolean getParameter(String key, Boolean defaultValue) {
		String value = parameters.get(key);
		return StringUtils.isNotBlank(value) ? (value).equalsIgnoreCase("true") : defaultValue;
	}

	/**
	 * Returns the plug-in parameter value as a Date value if it exists, or
	 * returns the given default value if it doesn't.
	 *
	 * Parameter date strings must be in the format "yyyyMMdd-HHmm"
	 *
	 * @param key plug-in parameter name
	 * @param defaultValue default value if parameter not defined
	 * @return parameter value, or default if not defined
	 * @throws PluggableTaskException thrown if parameter could not be parsed as a date
	 */
	protected Date getParameter(String key, Date defaultValue) throws PluggableTaskException {
		String value = parameters.get(key);

		try {
			return StringUtils.isNotBlank(value) ? PARAMETER_DATE_FORMAT.parseDateTime(value).toDate() : defaultValue;
		} catch (IllegalArgumentException e) {
			throw new PluggableTaskException(key + " could not be parsed as a date!", e);
		}
	}

	/**
	 * Return the parameter value as a string or throws an exception if it doesn't exist
	 * @param parameterName - name of the paramter
	 * @return the parameter value
	 * @throws PluggableTaskException - if the parameter is null or empty
	 */
	protected String getMandatoryStringParameter(String parameterName) throws PluggableTaskException {
		String parameter = getParameter(parameterName, "");
		//check that we have the parameter
		if (parameter.trim().isEmpty()) {
			throw new PluggableTaskException("Parameter " + parameterName + " not specified");
		}
		return parameter;
	}

	/**
	 * Check if a parameter exists with the name
	 * @param parameterName - parameter name to check
	 * @return true if parameter exists else false
	 */
	protected boolean existParameter(String parameterName) {
		String parameter = getParameter(parameterName, "");
		return !parameter.trim().isEmpty();
	}

	public boolean validate() {
		if (getParameterDescriptions() != null) {
			// validate that those required are present
			for (ParameterDescription param: getParameterDescriptions()) {
				if (param.isRequired() && (parameters == null || !parameters.containsKey(param.getName()))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * The getter and setter methods for the class field parameters
	 * is provided only for the sole purpose of injecting a pluggable
	 * task via spring configuration for tests that run without the
	 * jbilling running.
	 * @return
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * This method return the current date according to the timezone of the company
	 * of the pluggable task
	 * @return current date
	 */
	public Date companyCurrentDate() {
		return TimezoneHelper.companyCurrentDate(entityId);
	}

	/**
	 * Returns true if we create only 1 per PriceModelDTO object. Used for caching.
	 *
	 * @return
	 */
	public boolean isSingleton() {
		return false;
	}

	public void afterSetup() {

	}
}
