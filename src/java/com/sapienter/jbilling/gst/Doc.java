package com.sapienter.jbilling.gst;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "cancel",
    "num",
    "totnum",
    "from",
    "to",
    "net_issue"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class Doc {

    @JsonProperty("cancel")
    public int cancel;
    @JsonProperty("num")
    public int num;
    @JsonProperty("totnum")
    public int totnum;
    @JsonProperty("from")
    public String from;
    @JsonProperty("to")
    public String to;
    @JsonProperty("net_issue")
    public int netIssue;

}
