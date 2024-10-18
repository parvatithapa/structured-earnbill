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
package com.sapienter.jbilling.server.util;

import java.io.Serializable;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ApiModel(value = "Preference data", description = "PreferenceWS model")
public class PreferenceWS implements Serializable {

    private Integer id;
    private PreferenceTypeWS preferenceType;
    private Integer tableId;
    private Integer foreignId;
    @Size(min=0, max=200, message="validation.error.max,200")
    private String value;
    @Digits(integer=12, fraction=0, message="validation.error.not.a.number.12.integer")
    private String intValue;

    public PreferenceWS() {
    }

    public PreferenceWS(PreferenceTypeWS preferenceType, String value) {
        this.preferenceType = preferenceType;
        this.value = value;
    }

    @ApiModelProperty(value = "Unique identifier of the preference", required = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Preference type of the preference")
    public PreferenceTypeWS getPreferenceType() {
        return this.preferenceType;
    }

    public void setPreferenceType(PreferenceTypeWS preferenceType) {
        this.preferenceType = preferenceType;
    }

    @JsonIgnore
    public Integer getTableId() {
        return tableId;
    }

    public void setTableId(Integer tableId) {
        this.tableId = tableId;
    }

    @JsonIgnore
    public Integer getForeignId() {
        return this.foreignId;
    }

    public void setForeignId(Integer foreignId) {
        this.foreignId = foreignId;
    }

    @ApiModelProperty(value = "Value of the preference")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PreferenceWS{"
               + "id=" + id
               + ", preferenceType=" + preferenceType
               + ", tableId=" + tableId
               + ", foreignId=" + foreignId
               + ", value='" + value + '\''
               + '}';
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof PreferenceWS)) {
            return false;
        }
        PreferenceWS preference = (PreferenceWS) object;
        return nullSafeEquals(this.id, preference.id) &&
                nullSafeEquals(this.preferenceType, preference.preferenceType) &&
                nullSafeEquals(this.tableId, preference.tableId) &&
                nullSafeEquals(this.foreignId, preference.foreignId) &&
                nullSafeEquals(this.value, preference.value) &&
                nullSafeEquals(this.intValue, preference.intValue);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(preferenceType);
        result = 31 * result + nullSafeHashCode(tableId);
        result = 31 * result + nullSafeHashCode(foreignId);
        result = 31 * result + nullSafeHashCode(value);
        result = 31 * result + nullSafeHashCode(intValue);
        return result;
    }
}
