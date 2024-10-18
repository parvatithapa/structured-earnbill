package com.sapienter.jbilling.server.spa;

import java.io.Serializable;

@SuppressWarnings("serial")
public class CustomerEmergency911AddressWS implements Serializable {

    public static final CustomerEmergency911AddressWS PHONE_NUBER_NOT_FOUND_RESPONSE;
    public static final CustomerEmergency911AddressWS USER_NOT_FOUND_RESPONSE;

    static {
        PHONE_NUBER_NOT_FOUND_RESPONSE = new CustomerEmergency911AddressWS();
        USER_NOT_FOUND_RESPONSE = new CustomerEmergency911AddressWS();
        PHONE_NUBER_NOT_FOUND_RESPONSE.setReturnMessage("Phone Number Not found!");
        USER_NOT_FOUND_RESPONSE.setReturnMessage("User Not Found With Provided Phone Number!");
        USER_NOT_FOUND_RESPONSE.setReturnCode(0);
        PHONE_NUBER_NOT_FOUND_RESPONSE.setReturnCode(0);
    }

    private String addressType;
    private Integer returnCode;
    private String returnMessage;
    private String effectiveDate;
    private String customerName;
    private String phoneNumber;
    private String address;

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(Integer returnCode) {
        this.returnCode = returnCode;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }


}
