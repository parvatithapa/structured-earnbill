package com.sapienter.jbilling.gst;

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
    "flag",
    "data",
    "chksum"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class Hsn {

    @JsonProperty("flag")
    public String flag;
    @JsonProperty("data")
    public List<Datum> data = new ArrayList<Datum>();
    @JsonProperty("chksum")
    public String chksum;

}
