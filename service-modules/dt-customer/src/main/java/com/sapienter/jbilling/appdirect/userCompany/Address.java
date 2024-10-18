package com.sapienter.jbilling.appdirect.userCompany;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {

    private String street1;
    private String street2;
    private String city;
    private String state;
    private String zip;
    private String country;


}
