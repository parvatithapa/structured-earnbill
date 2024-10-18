package com.sapienter.jbilling.server.payment.tasks.stripe.dto;

/**
 * @Package: com.sapienter.jbilling.server.payment.tasks.stripe.dto 
 * @author: Amey Pelapkar   
 * @date: 21-Apr-2021 3:25:27 pm
 *
 */
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

    public Payer() {}

    public Payer(Integer id, String email, String firstName, String lastName, String street,
                     String city, String state, String zip, String countryCode) {
    	
    	this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.countryCode = countryCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreet2() { return street2; }

    public void setStreet2(String street2) { this.street2 = street2; }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getStripeCustomerId() {
		return stripeCustomerId;
	}

	public void setStripeCustomerId(String stripeCustomerId) {
		this.stripeCustomerId = stripeCustomerId;
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
    public String toString() {
    	String str = firstName +" "+lastName+" "+email+" "+city+" "+state+" "+zip+" "+countryCode;
    	return str;
    }
}
