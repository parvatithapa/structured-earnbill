package com.sapienter.jbilling.server.item;

import java.io.Serializable;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;

@SuppressWarnings("serial")
public class SwapAssetWS implements Serializable {

    @NotNull(message = "validation.error.notnull")
    private String existingIdentifier;
    @NotNull(message = "validation.error.notnull")
    private String newIdentifier;
    @Digits(integer = 12, fraction = 10, message = "validation.error.not.a.number", groups = {CreateValidationGroup.class, UpdateValidationGroup.class})
    private String amount;

    @JsonCreator
    public SwapAssetWS(@JsonProperty("existingIdentifier") String existingIdentifier,
            @JsonProperty("newIdentifier") String newIdentifier, @JsonProperty("amount") String amount) {
        this.existingIdentifier = existingIdentifier;
        this.newIdentifier = newIdentifier;
        this.amount = amount;
    }

    public String getExistingIdentifier() {
        return existingIdentifier;
    }

    public String getNewIdentifier() {
        return newIdentifier;
    }

    public String getAmount() {
        return amount;
    }
}
