package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExpDtls {

    @JsonProperty("CntCode")
    private String cntCode;

    public String getCntCode() { return cntCode; }

    public void setCntCode(String cntCode) { this.cntCode = cntCode; }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExpDtls [cntCode=");
        builder.append(cntCode);
        builder.append("]");
        return builder.toString();
    }

}

