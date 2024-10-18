package com.sapienter.jbilling.gst;

import lombok.Data;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "samt",
        "rt",
        "uqc",
        "qty",
        "num",
        "txval",
        "camt",
        "hsn_sc",
        "iamt",
        "desc"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class Datum {

    @JsonProperty("samt")
    public Float samt;
    @JsonProperty("rt")
    public int rt;
    @JsonProperty("uqc")
    public String uqc;
    @JsonProperty("qty")
    public Float qty;
    @JsonProperty("num")
    public Integer num;
    @JsonProperty("txval")
    public BigDecimal txval;
    @JsonProperty("camt")
    public Float camt;
    @JsonProperty("hsn_sc")
    public String hsnSc;
    @JsonProperty("iamt")
    public Float iamt;
    @JsonProperty("desc")
    public String desc;

}
