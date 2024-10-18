package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddlDocDtls {

    @JsonProperty("Info")
    private String info;

    public String getInfo() { return info; }

    public void setInfo(String info) { this.info = info; }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AddlDocDtls [info=");
        builder.append(info);
        builder.append("]");
        return builder.toString();
    }
}


