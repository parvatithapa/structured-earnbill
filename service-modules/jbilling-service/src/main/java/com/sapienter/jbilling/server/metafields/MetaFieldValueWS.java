/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.metafields;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Alexander Aksenov
 * @since 09.10.11
 */
@ApiModel(value = "MetaFieldValue Data", description = "MetaFieldValueWS Model")
public class MetaFieldValueWS implements Serializable {

    private static final long serialVersionUID = 20130704L;

    private Integer groupId;
    private Object defaultValue;
    @Valid
    private MetaFieldWS metaField;

    private Integer id;

    @Size(min = 0, max = 1000, message = "validation.error.size,0,1000")
    private String stringValue;
    private char[] charValue;
    @ConvertToTimezone
    private Date dateValue;
    private Boolean booleanValue;
    @Digits(integer = 12, fraction = 10, message="validation.error.not.a.number")
    private String decimalValue;
    private Integer integerValue;
    private String[] listValue;

    public MetaFieldValueWS() {
        this.metaField = new MetaFieldWS();
    }

    public MetaFieldValueWS(String fieldName, Integer groupId, DataType dataType, Boolean mandatory, Object value) {
        this.metaField = new MetaFieldWS();
        this.metaField.setName(fieldName);
        this.metaField.setDataType(dataType);
        this.metaField.setMandatory(mandatory);
        this.groupId = groupId;
        this.setValue(value);
    }

    public MetaFieldValueWS(MetaFieldWS metaField, Integer groupId, Object value) {
        this.groupId = groupId;
        this.metaField = metaField;
        this.setValue(value);
    }

    public MetaFieldValueWS clone() {
    	MetaFieldValueWS ws = new MetaFieldValueWS();
    	ws.setFieldName(this.metaField.getName());
        ws.getMetaField().setDataType(this.metaField.getDataType());
        ws.getMetaField().setMandatory(this.metaField.isMandatory());
        ws.getMetaField().setDisplayOrder(this.metaField.getDisplayOrder());
        ws.getMetaField().setEntityId(this.metaField.getEntityId());
        ws.getMetaField().setEntityType(this.metaField.getEntityType());
        ws.getMetaField().setDisabled(this.metaField.isDisabled());
        ws.getMetaField().setFieldUsage(this.metaField.getFieldUsage());
    	ws.setGroupId(this.groupId);
    	ws.setDefaultValue(this.defaultValue);
    	
    	ws.setStringValue(this.stringValue);
    	ws.setDateValue(this.dateValue);
    	ws.setBooleanValue(this.booleanValue);
    	ws.setDecimalValue(this.decimalValue);
    	ws.setIntegerValue(this.integerValue);
    	ws.setListValue(this.listValue);
        ws.setCharValue(this.charValue);
    	
    	return ws;
    }

    @ApiModelProperty(value = "The meta-field name for which this value is provided")
    public String getFieldName() {
        return this.metaField.getName();
    }

    public void setFieldName(String fieldName) {
        this.metaField.setName(fieldName);
    }

    public MetaFieldWS getMetaField(){
        return this.metaField;
    }

    //Maintaining the setter to avoid unmarshling error
    public void setMetaField(MetaFieldWS metaField) {
        this.metaField = metaField;
    }
    
    @ApiModelProperty(value = "The id of the meta-field value entity")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @XmlTransient
    @JsonIgnore
    public Object getValue() {
        if (getStringValue() != null) {
            return getStringValue();
        } else if (getDateValue() != null) {
            return getDateValue();
        } else if (getBooleanValue() != null) {
            return getBooleanValue();
        } else if (getDecimalValue() != null) {
            return getDecimalValueAsDecimal();
        } else if (getIntegerValue() != null) {
            return getIntegerValue();
        } else if (getListValue() != null) {
            return getListValueAsList();
        } else if (getCharValue() != null) {
            return getCharValue();
        }

        return null;
    }

    public void setValue(Object value) {
        setStringValue(null);
        setDateValue(null);
        setBooleanValue(null);
        setDecimalValue(null);
        setIntegerValue(null);
        setCharValue(null);

        if (value == null) return;

        if (value instanceof String) {
            setStringValue((String) value);
        } else if (value instanceof Date) {
            setDateValue((Date) value);
        } else if (value instanceof Boolean) {
            setBooleanValue((Boolean) value);
        } else if (value instanceof BigDecimal) {
            setBigDecimalValue((BigDecimal) value);
        } else if (value instanceof Integer) {
            setIntegerValue((Integer) value);
        } else if (value instanceof List) {
            // store List<String> as String[] for WS-compatible mode, perform manual convertion
            setListValue(((List<String>) value).toArray(new String[((List<String>) value).size()]));
        } else if (value instanceof String[]) {
            setListValue((String[]) value);
        } else if (value instanceof char[]) {
            setCharValue((char[]) value);
        }
    }

    @ApiModelProperty(value = "Is this meta-field value disabled or not")
    @JsonProperty(value = "disabled")
    public boolean isDisabled() {
        return this.metaField.isDisabled();
    }

    public void setDisabled(boolean disabled) {
        this.metaField.setDisabled(disabled);
    }

    @ApiModelProperty(value = "Is this meta-field value mandatory or not")
    @JsonProperty(value = "mandatory")
    public boolean isMandatory() {
        return this.metaField.isMandatory();
    }

    public void setMandatory(boolean mandatory) {
        this.metaField.setMandatory(mandatory);
    }

    @ApiModelProperty(value = "The data type of the meta-field value")
    public DataType getDataType() {
        return this.metaField.getDataType();
    }

    public void setDataType(DataType dataType) {
        this.metaField.setDataType(dataType);
    }

    @ApiModelProperty(value = "The value as raw object representation")
    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        if (defaultValue != null && defaultValue instanceof Collection) {
            // default value is the first in list
            if (((Collection) defaultValue).isEmpty()) {
                this.defaultValue = null;
            } else {
                this.defaultValue = ((Collection) defaultValue).iterator().next();
            }
        } else {
            this.defaultValue = defaultValue;
        }
    }

    @ApiModelProperty(value = "The ordered number for this meta-field value")
    public Integer getDisplayOrder() {
        return this.metaField.getDisplayOrder();
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.metaField.setDisplayOrder(displayOrder);
    }

    @ApiModelProperty(value = "The String value if DataType is STRING")
    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    @ApiModelProperty(value = "The Date value if DataType is CHAR")
    public char[] getCharValue() {
        return charValue;
    }

    public void setCharValue(char[] charValue) {
        this.charValue =  charValue;
    }

    @ApiModelProperty(value = "The Date value if DataType is DATE")
    public Date getDateValue() {
        return dateValue;
    }

    public void setDateValue(Date dateValue) {
        this.dateValue = dateValue;
    }

    @ApiModelProperty(value = "The Boolean value if DataType is BOOLEAN")
    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    @JsonIgnore
    public String getDecimalValue() {
        return decimalValue;
    }

    @ApiModelProperty(value = "The Decimal value if DataType is DECIMAL")
    @JsonProperty("decimalValue")
    public BigDecimal getDecimalValueAsDecimal() {
        return Util.string2decimal(decimalValue);
    }

    @JsonIgnore
    public void setDecimalValue(String decimalValue) {
        this.decimalValue = decimalValue;
    }

    @JsonProperty("decimalValue")
    public void setBigDecimalValue(BigDecimal decimalValue) {
        this.decimalValue = decimalValue != null ? decimalValue.toPlainString() : null;
    }

    @ApiModelProperty(value = "The Integer value if DataType is INTEGER")
    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    @ApiModelProperty(value = "The Array of String values if DataType is LIST")
    public String[] getListValue() {
        return listValue;
    }

    public void setListValue(String[] listValue) {
        this.listValue = listValue;
    }

    /**
     * Call this method instead of getValue() for metaField with type LIST, because
     * storing data inside MetaFieldValueWS as String[] for WS-complaint mode.
     *
     * @return value as java.util.List for LIST meta field type. null otherwise.
     */
    @XmlTransient
    @JsonIgnore
    public List getListValueAsList() {
        if (listValue != null) {
            return new LinkedList<String>(Arrays.asList(listValue));
        } else {
            return null;
        }
    }

    @ApiModelProperty(value = "The id of the meta-field group in which the meta-field name belongs")
    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    /**
     * Entity Id should be compared along with name to identify the meta field.
     */
    @ApiModelProperty(value = "Owning enitity")
    public Integer getEntityId() {
        return this.metaField.getEntityId();
    }

    public void setEntityId(Integer entityId) {
        this.metaField.setEntityId(entityId);
    }

    @Override
    public String toString() {
        String encrypted = "******";
        Object value = (MetaFieldType.PAYMENT_CARD_NUMBER == this.metaField.getFieldUsage() || MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED == this.metaField.getFieldUsage()) ? encrypted : getValue();
        return "MetaFieldValueWS{" +
                "id=" + id +
                ", fieldName='" + this.metaField.getName() + '\'' +
                ", groupId=" + groupId +
                ", dataType=" + this.metaField.getDataType() +
                ", value=" + value +
                ", entity=" + this.metaField.getEntityId() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetaFieldValueWS that = (MetaFieldValueWS) o;

        if (! nullSafeEquals(id, that.id)) return false;
        if (! nullSafeEquals(metaField, that.metaField)) return false;
        if (! nullSafeEquals(booleanValue, that.booleanValue)) return false;
        if (! nullSafeEquals(dateValue, that.dateValue)) return false;
        if (! Util.decimalEquals(getDecimalValueAsDecimal(), that.getDecimalValueAsDecimal())) return false;
        if (! nullSafeEquals(decimalValue, that.decimalValue)) return false;
        if (! nullSafeEquals(defaultValue, that.defaultValue)) return false;
        if (! nullSafeEquals(groupId, that.groupId)) return false;
        if (! nullSafeEquals(integerValue, that.integerValue)) return false;
        if (!Arrays.equals(listValue, that.listValue)) return false;
        if (! nullSafeEquals(stringValue, that.stringValue)) return false;
        if (! nullSafeEquals(charValue, that.charValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = metaField.hashCode();
        result = 31 * result + nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(groupId);
        result = 31 * result + nullSafeHashCode(defaultValue);
        result = 31 * result + nullSafeHashCode(stringValue);
        result = 31 * result + nullSafeHashCode(dateValue);
        result = 31 * result + nullSafeHashCode(booleanValue);
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getDecimalValueAsDecimal()));
        result = 31 * result + nullSafeHashCode(integerValue);
        result = 31 * result + nullSafeHashCode(listValue);
        result = 31 * result + nullSafeHashCode(charValue);
        return result;
    }

    public void clearCharValue(){
        if(this.charValue!=null)
            Arrays.fill(this.charValue, ' ');
    }

    public static Comparator<MetaFieldValueWS> defaultComparator() {
        return new Comparator<MetaFieldValueWS>() {
            @Override
            public int compare(MetaFieldValueWS o1, MetaFieldValueWS o2) {
                int result = 0;
                if(o1.getFieldName() != null && o2.getFieldName() != null) {
                    result = o1.getFieldName().compareTo(o2.getFieldName());
                }
                if(result == 0) {
                    if(o1.getGroupId() != null && o2.getGroupId() != null) {
                        result = o1.getGroupId().compareTo(o2.getGroupId());
                        if(result == 0) {
                            result = o1.getId().compareTo(o2.getId());
                        }
                    }
                }
                return result;
            }
        };
    }
}
