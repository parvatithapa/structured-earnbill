package com.sapienter.jbilling.server.util;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Size;
import java.io.Serializable;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * Created by vojislav on 14.1.15.
 */
@ApiModel(value = "Enumeration value data", description = "EnumerationValueWS model")
public class EnumerationValueWS implements Serializable {

    private Integer id;
    @Size(min = 1, max = 50, message = "enumeration.value.missing")
    private String value;

    public EnumerationValueWS() {}

    public EnumerationValueWS(Integer id) {
        this(id, null);
    }

    public EnumerationValueWS(String value){
        this(null, value);
    }

    public EnumerationValueWS(Integer id, String value) {
        setId(id);
        setValue(value);
    }

    @ApiModelProperty(value = "Unique identifier of the enumeration value", required = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "The enumeration value.", required = true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "EnumerationValueWS{" +
                "id=" + id +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof EnumerationValueWS)) {
            return false;
        }
        EnumerationValueWS enumerationValue = (EnumerationValueWS) object;
        return nullSafeEquals(this.id, enumerationValue.id) &&
                nullSafeEquals(this.value, enumerationValue.value);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(value);
        return result;
    }
}
