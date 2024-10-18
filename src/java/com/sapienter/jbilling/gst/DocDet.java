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
    "docs",
    "doc_num"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class DocDet {

    @JsonProperty("docs")
    public List<Doc> docs = new ArrayList<Doc>();
    @JsonProperty("doc_num")
    public int docNum;

}
