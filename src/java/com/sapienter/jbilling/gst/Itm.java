package com.sapienter.jbilling.gst;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "csamt",
        "rt",
        "txval",
        "iamt"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class Itm {

    @JsonProperty("csamt")
    public Integer csamt;
    @JsonProperty("rt")
    public Integer rt;
    @JsonProperty("txval")
    public BigDecimal txval;
    @JsonProperty("iamt")
    public Float iamt;

}
