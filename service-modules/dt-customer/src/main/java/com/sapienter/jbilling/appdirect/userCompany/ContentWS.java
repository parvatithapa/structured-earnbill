package com.sapienter.jbilling.appdirect.userCompany;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentWS {

    private String id;

    private String name;

    private boolean enabled;

    private Date creationDate;

    private String status;

    private Contact contact;

    private String size;

    private String website;

    private String emailAddress;

    private String uuid;

    private  String externalId;

    private String countryCode;

    private Object attributes;

    private Object domains;

    private Object permissions;

    private String industry;

    private String salesAgent;

    private String dealer;

    private String defaultIdpUuid;

}
