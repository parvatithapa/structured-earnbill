package com.sapienter.jbilling.resources;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
public class CustomerMetaFieldValueWS implements Serializable {

    @NotNull(message = "validation.error.notnull")
    private Integer userId;
    @NotNull(message = "validation.error.notnull")
    private Map<String, String> metaFieldValues = new HashMap<>();


    @JsonCreator
    public CustomerMetaFieldValueWS(@JsonProperty(value = "userId") Integer userId,
            @JsonProperty("metaFieldValues") Map<String, String> metaFieldValues) {
        this.userId = userId;
        this.metaFieldValues = metaFieldValues;
    }

    @ApiModelProperty(value = "Customer metaFieldValues map", required = true)
    public Map<String, String> getMetaFieldValues() {
        return metaFieldValues;
    }

    @ApiModelProperty(value = "user id", required = true)
    public Integer getUserId() {
        return userId;
    }

}