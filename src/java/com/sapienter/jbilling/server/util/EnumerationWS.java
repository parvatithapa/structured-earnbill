package com.sapienter.jbilling.server.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.server.security.WSSecured;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * Created by vojislav on 14.1.15.
 */
@ApiModel(value = "Enumeration data", description = "EnumerationWS model")
public class EnumerationWS implements Serializable, WSSecured {

    private Integer id;

    @Min(value = 1, message = "enumeration.entityId.negative")
    private Integer entityId;

    @NotNull(message="validation.error.notnull")
    @Size(min = 1, max = 50, message = "validation.error.size,1,50")
    private String name;

    @Valid
    private List<EnumerationValueWS> values;

    public EnumerationWS(){
        this(null);
    }

    public EnumerationWS(String name) {
        this(null, null, name);
    }

    public EnumerationWS(Integer id, Integer entityId, String name) {
        this(id, entityId, name, new ArrayList<EnumerationValueWS>());
    }

    public EnumerationWS(Integer id, Integer entityId, String name, List<EnumerationValueWS> values) {
        setId(id);
        setEntityId(entityId);
        setName(name);
        setValues(values);
    }

    @ApiModelProperty(value = "Unique identifier of the enumeration", required = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Unique identifier of the company for which this enumeration is defined", required = true)
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "Name of the enumeration", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "List of values for the enumeration")
    public List<EnumerationValueWS> getValues() {
        return values;
    }

    public void setValues(List<EnumerationValueWS> values) {
        this.values = values;
    }

    public boolean addValue(EnumerationValueWS valueWS){
        if(null == values){
            values = new ArrayList<EnumerationValueWS>();
        }
        return values.add(valueWS);
    }

    public boolean addValue(String value){
        if(null == value){
            return false;
        }
        if (null == values){
            values = new ArrayList<EnumerationValueWS>();
        }
        return values.add(new EnumerationValueWS(value));
    }

    @Override
    @JsonIgnore
    public Integer getOwningEntityId() {
        return this.entityId;
    }

    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @Override
    public String toString() {
        return "EnumerationWS{" +
                "id=" + id +
                ", entityId=" + entityId +
                ", name='" + name + '\'' +
                ", values=" + values +
                '}';
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof EnumerationWS)) {
            return false;
        }
        EnumerationWS enumeration = (EnumerationWS) object;
        return nullSafeEquals(this.id, enumeration.id) &&
                nullSafeEquals(this.entityId, enumeration.entityId) &&
                nullSafeEquals(this.name, enumeration.name) &&
                nullSafeEquals(this.values, enumeration.values);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(entityId);
        result = 31 * result + nullSafeHashCode(name);
        result = 31 * result + nullSafeHashCode(values);
        return result;
    }
}
