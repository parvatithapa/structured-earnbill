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

import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

@ApiModel(value = "Preference type data", description = "PreferenceTypeWS model")
public class PreferenceTypeWS implements Serializable {

    private int id;
    private String description;
    private String defaultValue;
    private ValidationRuleWS validationRule;

    public PreferenceTypeWS() {
    }

    public PreferenceTypeWS(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Unique identifier of the preference type", required = true)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Description for the preference type")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Default value for the preference type")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @ApiModelProperty(value = "Validation rule for the preference type")
    public ValidationRuleWS getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(ValidationRuleWS validationRule) {
        this.validationRule = validationRule;
    }

    @Override
    public String toString() {
        return "PreferenceTypeWS{"
               + "id=" + id
               + ", description='" + description + '\''
               + ", defaultValue='" + defaultValue + '\''
               + ", validationRule='" + validationRule + '\''
               + '}';
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof PreferenceTypeWS)) {
            return false;
        }
        PreferenceTypeWS preferenceType = (PreferenceTypeWS) object;
        return nullSafeEquals(this.id, preferenceType.id) &&
                nullSafeEquals(this.description, preferenceType.description) &&
                nullSafeEquals(this.defaultValue, preferenceType.defaultValue) &&
                nullSafeEquals(this.validationRule, preferenceType.validationRule);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(description);
        result = 31 * result + nullSafeHashCode(defaultValue);
        result = 31 * result + nullSafeHashCode(validationRule);
        return result;
    }
}
