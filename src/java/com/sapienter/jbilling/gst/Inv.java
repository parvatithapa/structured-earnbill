package com.sapienter.jbilling.gst;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "val",
    "itms",
    "flag",
    "irn",
    "srctyp",
    "idt",
    "irngendate",
    "inum",
    "chksum"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class Inv {

    @JsonProperty("val")
    public BigDecimal val;
    @JsonProperty("itms")
    public List<Itm> itms = new ArrayList<Itm>();
    @JsonProperty("flag")
    public String flag;
    @JsonProperty("irn")
    public String irn;
    @JsonProperty("srctyp")
    public String srctyp;
    @JsonProperty("idt")
    public String idt;
    @JsonProperty("irngendate")
    public String irngendate;
    @JsonProperty("inum")
    public String inum;
    @JsonProperty("chksum")
    public String chksum;

}
