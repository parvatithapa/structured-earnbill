package com.sapienter.jbilling.einvoice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class DispDtls {

    @JsonProperty("Nm")
    private String dispatcherName;
    @JsonProperty("Addr1")
    private String addr1;
    @JsonProperty("Addr2")
    private String addr2;
    @JsonProperty("Loc")
    private String location;
    @JsonProperty("Stcd")
    private String stateCode;
    @JsonProperty("Pin")
    private Integer pinCode;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DispDtls [dispatcherName=");
        builder.append(dispatcherName);
        builder.append(", addr1=");
        builder.append(addr1);
        builder.append(", addr2=");
        builder.append(addr2);
        builder.append(", loc=");
        builder.append(location);
        builder.append(", pin=");
        builder.append(pinCode);
        builder.append(", stcd=");
        builder.append(stateCode);
        builder.append("]");
        return builder.toString();
    }
}
