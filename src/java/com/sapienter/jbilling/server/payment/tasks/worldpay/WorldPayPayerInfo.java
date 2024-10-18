package com.sapienter.jbilling.server.payment.tasks.worldpay;

import java.math.BigDecimal;
import com.sapienter.jbilling.server.payment.tasks.worldpay.WorldpayResult.WorldpayResultBuilder;

public class WorldPayPayerInfo {

	private String firstName;
	private String lastName;
	private String creditCardNumber;//
	private String email;
	private String expiryMonth;//
	private String expiryYear;//
	private BigDecimal amount;
	private String city;
	private String zip;
	private String countryCode;
	private String state;
	private String street;
	private String cardHolderName;//
	private char[] cvv;
	
	public String getFirstName() {
		return firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public String getCreditCardNumber() {
		return creditCardNumber;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getExpiryMonth() {
		return expiryMonth;
	}
	
	public String getExpiryYear() {
		return expiryYear;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	public String getCity() {
		return city;
	}
	
	public String getZip() {
		return zip;
	}
	
	public String getCountryCode() {
		return countryCode;
	}
	
	public String getState() {
		return state;
	}
	
	public String getStreet() {
		return street;
	}
	
	public String getCardHolderName() {
		return cardHolderName;
	}
	
	public char[] getCvv() {
		return cvv;
	}
	

	private WorldPayPayerInfo(WorldPayPayerInfoBuilder builder)
    {
        this.firstName=builder.firstName;
        this.lastName=builder.lastName;
        this.creditCardNumber=builder.creditCardNumber;
        this.email=builder.email;
        this.expiryMonth=builder.expiryMonth;
        this.expiryYear=builder.expiryYear;
        this.amount=builder.amount;
        this.city=builder.city;
        this.zip=builder.zip;
        this.countryCode=builder.countryCode;
        this.state=builder.state;
        this.street=builder.street;
        this.cardHolderName=builder.cardHolderName;
        this.cvv=builder.cvv;
    }

	public static class WorldPayPayerInfoBuilder{
	    private String firstName;
	    private String lastName;
	    private String creditCardNumber;//
	    private String email;
	    private String expiryMonth;//
	    private String expiryYear;//
	    private BigDecimal amount;
	    private String city;
	    private String zip;
	    private String countryCode;
	    private String state;
	    private String street;
	    private String cardHolderName;//
	    private char[] cvv;

	    public WorldPayPayerInfoBuilder setFirstName(String firstName) {
	        this.firstName = firstName;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setLastName(String lastName) {
	        this.lastName = lastName;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setCreditCardNumber(String creditCardNumber) {
	        this.creditCardNumber = creditCardNumber;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setEmail(String email) {
	        this.email = email;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setExpiryMonth(String expiryMonth) {
	        this.expiryMonth = expiryMonth;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setExpiryYear(String expiryYear) {
	        this.expiryYear = expiryYear;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setAmount(BigDecimal amount) {
	        this.amount = amount;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setCity(String city) {
	        this.city = city;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setZip(String zip) {
	        this.zip = zip;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setCountryCode(String countryCode) {
	        this.countryCode = countryCode;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setState(String state) {
	        this.state = state;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setStreet(String street) {
	        this.street = street;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setCardHolderName(String cardHolderName) {
	        this.cardHolderName = cardHolderName;
	        return this;
	    }
	    public WorldPayPayerInfoBuilder setCvv(char[] cvv) {
	        this.cvv = cvv;
	        return this;
	    }

        public WorldPayPayerInfo build()
        {
            return new WorldPayPayerInfo(this);
        }
    }

}
