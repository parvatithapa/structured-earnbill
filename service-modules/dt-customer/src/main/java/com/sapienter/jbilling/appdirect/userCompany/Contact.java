package com.sapienter.jbilling.appdirect.userCompany;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Contact{

     private String phoneNumber;
     private String ims;
     private String homePhone;
     private String mobilePhone;
     private Address address ;


}