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
    "inv",
    "exp_typ"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString
public class Exp {

    @JsonProperty("inv")
    public List<Inv> inv = new ArrayList<Inv>();
    @JsonProperty("exp_typ")
    public String expTyp;

}
