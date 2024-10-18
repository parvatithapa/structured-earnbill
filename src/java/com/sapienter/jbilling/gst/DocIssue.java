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
    "doc_det",
    "chksum"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class DocIssue {

    @JsonProperty("flag")
    public String flag;
    @JsonProperty("doc_det")
    public List<DocDet> docDet = new ArrayList<DocDet>();
    @JsonProperty("chksum")
    public String chksum;

}
