package com.sapienter.jbilling.server.item;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("serial")
public class SwapAssetWS implements Serializable {

    @NotNull(message = "validation.error.notnull")
    private String existingIdentifier;
    @NotNull(message = "validation.error.notnull")
    private String newIdentifier;

    @JsonCreator
    public SwapAssetWS(@JsonProperty("existingIdentifier") String existingIdentifier,
            @JsonProperty("newIdentifier") String newIdentifier) {
        this.existingIdentifier = existingIdentifier;
        this.newIdentifier = newIdentifier;
    }

    public String getExistingIdentifier() {
        return existingIdentifier;
    }

    public String getNewIdentifier() {
        return newIdentifier;
    }
}
