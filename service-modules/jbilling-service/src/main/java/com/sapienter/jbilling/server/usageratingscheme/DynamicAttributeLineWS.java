package com.sapienter.jbilling.server.usageratingscheme;

import java.io.Serializable;
import java.util.Map;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;


@ApiModel(value = "DynamicAttributeLine Data", description = "DynamicAttributeLineWS model")
public class DynamicAttributeLineWS implements Serializable,Comparable<DynamicAttributeLineWS> {

    private static final long serialVersionUID = -1L;
    
    private Integer id;
    private Integer sequence;

    private Map<String, String> attributes;

    @ApiModelProperty(value = "auto generated id of the dynamic attribute")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "User defined Sequence for the dynamic attribute. ")
    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    @ApiModelProperty(value = "Map with dynamic attributes key, value detail of a usage rating scheme")
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public int compareTo(DynamicAttributeLineWS o) {
        if (o == null)  return -1;

        return this.sequence.compareTo(o.sequence);
    }
}
