package com.sapienter.jbilling.resources;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
public class AssetMetaFieldValueWS implements Serializable {
    @NotNull(message = "validation.error.notnull")
    private Integer assetId;
    @NotNull(message = "validation.error.notnull")
    private Map<String, String> metaFieldValues = new HashMap<>();

    @JsonCreator
    public AssetMetaFieldValueWS(@JsonProperty("assetId") Integer assetId,
            @JsonProperty("metaFieldValues") Map<String, String> metaFieldValues) {
        this.assetId = assetId;
        this.metaFieldValues = metaFieldValues;
    }

    @ApiModelProperty(value = "Asset Id", required = true)
    public Integer getAssetId() {
        return assetId;
    }

    @ApiModelProperty(value = "Asset metaFieldValues map", required = true)
    public Map<String, String> getMetaFieldValues() {
        return metaFieldValues;
    }


}
