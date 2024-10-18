package com.sapienter.jbilling.gst;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "gstin",
        "fp",
        "filing_typ",
        "gt",
        "cur_gt",
        "exp",
        "hsn",
        "doc_issue",
        "fil_dt"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class GSTReturn {
    @JsonProperty("gstin")
    public String gstin;
    @JsonProperty("fp")
    public String fp;
    @JsonProperty("filing_typ")
    public String filingTyp;
    @JsonProperty("gt")
    public int gt;
    @JsonProperty("cur_gt")
    public int curGt;
    @JsonProperty("exp")
    public List<Exp> exp = new ArrayList<Exp>();
    @JsonProperty("hsn")
    public Hsn hsn;
    @JsonProperty("doc_issue")
    public DocIssue docIssue;
    @JsonProperty("fil_dt")
    public String filDt;
}
