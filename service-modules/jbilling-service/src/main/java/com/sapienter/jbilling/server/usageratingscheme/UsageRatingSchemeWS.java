package com.sapienter.jbilling.server.usageratingscheme;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedSet;

@ApiModel(value = "Usage Rating Scheme Data", description = "UsageRatingSchemeWS model")
public class UsageRatingSchemeWS implements Serializable {

    private Integer id;
    private Integer entityId;
    private String ratingSchemeCode;
    private String ratingSchemeType;
    private Map<String, String> fixedAttributes;

    private boolean usesDynamicAttributes = false;
    private String dynamicAttributeName;
    private SortedSet<DynamicAttributeLineWS> dynamicAttributes;

    @ApiModelProperty(value = "The id of the Usage Rating Scheme entity")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @ApiModelProperty(value = "code given for the usage rating scheme")
    public String getRatingSchemeCode() {
        return ratingSchemeCode;
    }

    public void setRatingSchemeCode(String ratingSchemeCode) {
        this.ratingSchemeCode = ratingSchemeCode;
    }

    @ApiModelProperty(value = "Type of the usage rating scheme ")
    public String getRatingSchemeType() {
        return ratingSchemeType;
    }

    public void setRatingSchemeType(String ratingSchemeType) {
        this.ratingSchemeType = ratingSchemeType;
    }

    @ApiModelProperty(value = "Fixed attributes of the rating scheme like size, start for Tiered Linear scheme")
    public Map<String, String> getFixedAttributes() {
        return fixedAttributes;
    }

    public void setFixedAttributes(Map<String, String> fixedAttributes) {
        this.fixedAttributes = fixedAttributes;
    }

    @ApiModelProperty(value = "if the rating scheme usage dynamic attributes or not ")
    public boolean isUsesDynamicAttributes() {
        return usesDynamicAttributes;
    }

    public void setUsesDynamicAttributes(boolean usesDynamicAttributes) {
        this.usesDynamicAttributes = usesDynamicAttributes;
    }

    @ApiModelProperty(value = "retrieve dynamic attribute name of rating scheme ")
    public String getDynamicAttributeName() {
        return dynamicAttributeName;
    }

    public void setDynamicAttributeName(String dynamicAttributeName) {
        this.dynamicAttributeName = dynamicAttributeName;
    }

    @ApiModelProperty(value = "retrieve the list dynamic attributes of rating scheme ")
    public SortedSet<DynamicAttributeLineWS> getDynamicAttributes() {
        return dynamicAttributes;
    }

    public void setDynamicAttributes(SortedSet<DynamicAttributeLineWS> dynamicAttributes) {
        this.dynamicAttributes = dynamicAttributes;
    }
}
