package com.sapienter.jbilling.appdirect.userCompany;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyPayload {

    private String uuid;

    private long timestamp;

    private ResourceWS resource;

    private String resourceAction;

}