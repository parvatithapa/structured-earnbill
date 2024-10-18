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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ApiModel(value = "Plugin data", description = "PluggableTaskWS model")
public class PluggableTaskWS implements java.io.Serializable, WSSecured {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Integer id;
    @NotNull(message="validation.error.notnull")
    @Min(value = 1, message = "validation.error.min,1")
    private Integer processingOrder;
    @Size(min=0, max = 1000, message = "validation.error.size,1,1000")
    private String notes;
    @NotNull(message="validation.error.notnull")
    private Integer typeId;
    private Map<String, String> parameters = new HashMap<String, String>();
    private int versionNumber;
    private Integer owningId;
    
    public PluggableTaskWS() {
    }
    
	public void setNotes(String notes) {
		this.notes = notes;
	}

    @ApiModelProperty(value = "Additional information for the plugin")
	public String getNotes() {
		return notes;
	}

    @ApiModelProperty(value = "Unique identifier of the plugin", required = true)
    public Integer getId() {
        return id;
    }


    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Order in which the plugin will be processed", required = true)
    public Integer getProcessingOrder() {
        return processingOrder;
    }


    public void setProcessingOrder(Integer processingOrder) {
        this.processingOrder = processingOrder;
    }

    @ApiModelProperty(value = "Unique identifier of the plugin type for the plugin")
    public Integer getTypeId() {
        return typeId;
    }


    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    @ApiModelProperty(value = "Name and value map of parameters for the plugin")
    public Map<String, String> getParameters() {
        return parameters;
    }


    public void setParameters(Hashtable<String, String> parameters) {
        this.parameters = parameters;
    }

    @JsonIgnore
    public int getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    @Override
    public String toString() {
        return "PluggableTaskWS [id=" + id + ", notes=" + notes
                + ", parameters=" + parameters + ", processingOrder="
                + processingOrder + ", typeId=" + typeId + ", versionNumber="
                + versionNumber + "]";
    }
    
    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return owningId;
    }
    public void setOwningEntityId(Integer owningId){
    	this.owningId = owningId;
    }
    
    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof PluggableTaskWS)) {
            return false;
        }
        PluggableTaskWS plugin = (PluggableTaskWS) object;
        return nullSafeEquals(this.id, plugin.id) &&
                nullSafeEquals(this.processingOrder, plugin.processingOrder) &&
                nullSafeEquals(this.notes, plugin.notes) &&
                nullSafeEquals(this.typeId, plugin.typeId) &&
                nullSafeEquals(this.parameters, plugin.parameters) &&
                nullSafeEquals(this.owningId, plugin.owningId);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(processingOrder);
        result = 31 * result + nullSafeHashCode(notes);
        result = 31 * result + nullSafeHashCode(typeId);
        result = 31 * result + nullSafeHashCode(parameters);
        result = 31 * result + nullSafeHashCode(owningId);
        return result;
    }
}
