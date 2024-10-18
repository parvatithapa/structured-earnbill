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

package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.security.WSSecured;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * MediationConfigurationWS
 * 
 * @author Brian Cowdery
 * @since 21-10-2010
 */
@ApiModel(value = "MediationConfigurationWS", description = "Mediation Configuration model")
public class MediationConfigurationWS implements WSSecured, Serializable {

	private Integer id;
	private Integer entityId;
	private Integer processorTaskId;
	//@NotNull(message = "validation.error.notnull")
	private Integer pluggableTaskId;
	@NotNull(message="validation.error.notnull")
	@NotEmpty(message = "validation.error.notnull")
	@Size(min = 0, max = 150, message = "validation.error.size,0,150")
	private String name;
	@Digits(integer = 10, fraction = 0, message="mediation.validation.error.not.a.integer")
	@Min(value = 1, message = "validation.error.min,1")
	private String orderValue;
    @ConvertToTimezone
	private Date createDatetime;
	private Integer versionNum;
	private String mediationJobLauncher;
	private Boolean global= Boolean.FALSE;
	private Integer rootRoute;
	private String localInputDirectory;

	public MediationConfigurationWS() {
	}

    @ApiModelProperty(value = "Unique identifier of this mediation configuration")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

    @ApiModelProperty(value = "Current company ID")
	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

    @ApiModelProperty(value = "Unique identifier of the pluggable task for the mediation process.", required = true)
	public Integer getPluggableTaskId() {
		return pluggableTaskId;
	}

	public void setPluggableTaskId(Integer pluggableTaskId) {
		this.pluggableTaskId = pluggableTaskId;
	}

    @ApiModelProperty(value = "Unique identifier of the processor task for the mediation process.", required = true)
	public Integer getProcessorTaskId() {
		return processorTaskId;
	}

	public void setProcessorTaskId(Integer processorTaskId) {
		this.processorTaskId = processorTaskId;
	}

    @ApiModelProperty(value = "The name of this mediation run.", required = true)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    @ApiModelProperty(value = "The order of each mediation process run.", required = true)
	public Integer getOrderValue() {
		Integer value = null;
		try {
			if (StringUtils.trimToNull(orderValue) != null) {
				value = Integer.parseInt(orderValue);
			}
		} catch (NumberFormatException nfe) {
			value = null;
		}
		return value;
	}

	public void setOrderValue(String orderValue) {
		this.orderValue = orderValue;
	}

    @ApiModelProperty(value = "Date when the configuration was created")
	public Date getCreateDatetime() {
		return createDatetime;
	}

	public void setCreateDatetime(Date createDatetime) {
		this.createDatetime = createDatetime;
	}

    @JsonIgnore
    public Integer getOwningEntityId() {
        return getEntityId();
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningEntityId()}
     * @return null
     */
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @ApiModelProperty(value = "Name of the mediation job launcher.", required = true)
	public String getMediationJobLauncher() {
		return mediationJobLauncher;
	}

	public void setMediationJobLauncher(String mediationJobLauncher) {
		this.mediationJobLauncher = mediationJobLauncher;
	}

    @ApiModelProperty(value = "If this mediation process needs to be run through company hierarchy feature.")
	public Boolean getGlobal() {
		return global;
	}

	public void setGlobal(Boolean global) {
		this.global = global;
	}

    @JsonIgnore
	public Integer getVersionNum() {
		return versionNum;
	}

	public void setVersionNum(Integer versionNum) {
		this.versionNum = versionNum;
	}

    @ApiModelProperty(value = "Mediation is often set up to process different CDR formats. Each of these formats would result in a different mediation process being configured in the system. With routing it is likely that each process could wind up needing to move through different routing trees. This field is linked to the root route, if one is used")
	public Integer getRootRoute() {
		return rootRoute;
	}

	public void setRootRoute(Integer rootRoute) {
		this.rootRoute = rootRoute;
	}

    @ApiModelProperty(value = "The location of the repository where the records are kept. The actual value here (if needed) is determined as part of the customization process.")
	public String getLocalInputDirectory() {
		return localInputDirectory;
	}

	public void setLocalInputDirectory(String localInputDirectory) {
		this.localInputDirectory = localInputDirectory;
	}

	@Override
	public String toString() {
		return "MediationConfigurationWS{" + "id=" + id + ", entityId="
				+ entityId + ", pluggableTaskId=" + pluggableTaskId
				+ ", processorTaskId=" + processorTaskId + ", name='" + name
				+ '\'' + ", orderValue=" + orderValue + ", createDatetime="
				+ createDatetime + ", mediationJobLauncher="
				+ mediationJobLauncher + ", global=" + global + ", versionNum="
				+ versionNum + ", rootRoute=" + rootRoute + '}';
	}

}
