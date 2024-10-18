package com.sapienter.jbilling.server.user;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.sapienter.jbilling.server.util.cxf.CxfSMapStringObjectAdapter;
import com.wordnik.swagger.annotations.ApiModelProperty;

@SuppressWarnings("serial")
public class ContactInformationWS implements Serializable {

    private Integer userId;
    private String groupName;
    private Map<String,Object> metaFields = new HashMap<>();

  
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @XmlJavaTypeAdapter(CxfSMapStringObjectAdapter.class)
    @ApiModelProperty(value = "Used to collect any type of metafield")
    public Map<String, Object> getMetaFields() {
        return metaFields;
    }

    @JsonAnySetter
    public void setMetaField(String name, Object value) {
        metaFields.put(name, value);
    }

    public void setMetaFields(Map<String, Object> metaFields) {
        this.metaFields = metaFields;
    }

}
