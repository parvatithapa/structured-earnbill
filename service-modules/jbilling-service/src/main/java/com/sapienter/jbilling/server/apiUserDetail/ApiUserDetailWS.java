package com.sapienter.jbilling.server.apiUserDetail;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sapienter.jbilling.server.usageratingscheme.DynamicAttributeLineWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Usage Rating Scheme Data", description = "UsageRatingSchemeWS model")
public class ApiUserDetailWS implements Serializable {

    private String userName;
    private String password;
    private Integer companyId;
    private String accessCode;

    @ApiModelProperty(value = "User name field of the model")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @ApiModelProperty(value = "Password of the user")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ApiModelProperty(value = "company id of the user")
    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    @ApiModelProperty(value = "Generated access token for the user")
    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }
}
