package com.sapienter.jbilling.server.payment.tasks.stripe.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @Package: com.sapienter.jbilling.server.payment.tasks.stripe.dto 
 * @author: Amey Pelapkar   
 * @date: 21-Apr-2021 3:25:27 pm
 *
 */
@Data
@Builder
public class Payer {
	private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String street;
    private String street2;
    private String city;
    private String state;
    private String zip;
    private String countryCode;
    private String stripeCustomerId;    
}