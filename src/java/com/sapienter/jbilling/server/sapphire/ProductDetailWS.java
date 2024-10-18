package com.sapienter.jbilling.server.sapphire;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
@ToString
public final class ProductDetailWS implements Serializable {

    @NotNull(message = "validation.error.notnull")
    private final String productCode;
    private final String[] assetIdentifiers;

    @JsonCreator
    public ProductDetailWS(@JsonProperty("productCode") String productCode,
            @JsonProperty("assetIdentifiers") String[] assetIdentifiers) {
        this.productCode = productCode;
        this.assetIdentifiers = assetIdentifiers;
    }

    @ApiModelProperty(value = "product code.", required = true)
    public String getProductCode() {
        return productCode;
    }

    @ApiModelProperty(value = "Asset identifiers for given product code, if product is asset enabled product.")
    public String[] getAssetIdentifiers() {
        return assetIdentifiers;
    }
}
