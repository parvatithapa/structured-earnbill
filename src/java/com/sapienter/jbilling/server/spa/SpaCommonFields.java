package com.sapienter.jbilling.server.spa;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpaCommonFields implements Serializable{

    private static final long serialVersionUID = 1L;
    private String lob;
    private String phoneNumber;
    private String fullCustomerName;
    private String servicePostalCode;
    private String serviceType;
    private String accountNumber;
    private String emailAddress;
    private String serviceAddress;
    private String language;
    private String security;
    private String accountSecurity;
    private String city;
    private String province;

    public SpaCommonFields() {
    }

    public SpaCommonFields( String phoneNumber,
            String fullCustomerName, String serviceType,
             String servicePostalCode) {
        super();
        this.phoneNumber = phoneNumber;
        this.fullCustomerName = fullCustomerName;
        this.serviceType = serviceType;
        this.servicePostalCode = servicePostalCode;
    }

    @JsonProperty("LOB")
    public String getLob() {
        return lob;
    }

    @JsonProperty("LOB")
    public void setLob(String lob) {
        this.lob = lob;
    }

    @JsonProperty("PhoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @JsonProperty("PhoneNumber")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @JsonProperty("FullCustomerName")
    public String getFullCustomerName() {
        return fullCustomerName;
    }

    @JsonProperty("FullCustomerName")
    public void setFullCustomerName(String fullCustomerName) {
        this.fullCustomerName = fullCustomerName;
    }

    @JsonProperty("ServiceType")
    public String getServiceType() {
        return serviceType;
    }
 
    @JsonProperty("ServiceType")
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @JsonProperty("ServicePostalCode")
    public String getServicePostalCode() {
        return servicePostalCode;
    }

    @JsonProperty("ServicePostalCode")
    public void setServicePostalCode(String servicePostalCode) {
        this.servicePostalCode = servicePostalCode;
    }

    @JsonProperty("AccountNumber")
    public String getAccountNumber() {
        return accountNumber;
    }

    @JsonProperty("AccountNumber")
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @JsonProperty("EmailAddress")
    public String getEmailAddress() {
        return emailAddress;
    }

    @JsonProperty("EmailAddress")
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @JsonProperty("ServiceAddress")
    public String getServiceAddress() {
        return serviceAddress;
    }

    @JsonProperty("ServiceAddress")
    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    @JsonProperty("Language")
    public String getLanguage() {
        return language;
    }

    @JsonProperty("Language")
    public void setLanguage(String language) {
        this.language = language;
    }

    @JsonProperty("Security")
    public String getSecurity() {
        return security;
    }

    @JsonProperty("Security")
    public void setSecurity(String security) {
        this.security = security;
    }

    @JsonProperty("AccountSecurity")
    public String getAccountSecurity() {
        return accountSecurity;
    }

    @JsonProperty("AccountSecurity")
    public void setAccountSecurity(String accountSecurity) {
        this.accountSecurity = accountSecurity;
    }

    @JsonProperty("City")
    public String getCity() {
        return city;
    }

    @JsonProperty("City")
    public void setCity(String city) {
        this.city = city;
    }

    @JsonProperty("Province")
    public String getProvince() {
        return province;
    }

    @JsonProperty("Province")
    public void setProvince(String province) {
        this.province = province;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpaCommonFields [lob=");
        builder.append(lob);
        builder.append(", phoneNumber=");
        builder.append(phoneNumber);
        builder.append(", fullCustomerName=");
        builder.append(fullCustomerName);
        builder.append(", servicePostalCode=");
        builder.append(servicePostalCode);
        builder.append(", serviceType=");
        builder.append(serviceType);
        builder.append(", accountNumber=");
        builder.append(accountNumber);
        builder.append(", emailAddress=");
        builder.append(emailAddress);
        builder.append(", serviceAddress=");
        builder.append(serviceAddress);
        builder.append(", language=");
        builder.append(language);
        builder.append(", security=");
        builder.append(security);
        builder.append(", accountSecurity=");
        builder.append(accountSecurity);
        builder.append(", city=");
        builder.append(city);
        builder.append(", province=");
        builder.append(province);
        builder.append("]");
        return builder.toString();
    }

}
