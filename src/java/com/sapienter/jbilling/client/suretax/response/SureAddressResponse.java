package com.sapienter.jbilling.client.suretax.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sapienter.jbilling.client.suretax.IResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown=true)
public class SureAddressResponse implements IResponse {
    @JsonProperty("Message")
    public String message;
    @JsonProperty("FirmName")
    public String firmName;
    @JsonProperty("PrimaryAddressLine")
    public String address1;
    @JsonProperty("SecondaryAddressLine")
    public String address2;
    @JsonProperty("Urbanization")
    public String urbanization;
    @JsonProperty("City")
    public String city;
    @JsonProperty("County")
    public String county;
    @JsonProperty("State")
    public String state;
    @JsonProperty("ZIPCode")
    public String zipCode;
    @JsonProperty("ZIPPlus4")
    public String ZIPPlus4;
    @JsonProperty("Latitude")
    public String latitude;
    @JsonProperty("Longitude")
    public String longitude;
    public String jsonString;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFirmName() {
        return firmName;
    }

    public void setFirmName(String firmName) {
        this.firmName = firmName;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getUrbanization() {
        return urbanization;
    }

    public void setUrbanization(String urbanization) {
        this.urbanization = urbanization;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getZIPPlus4() {
        return ZIPPlus4;
    }

    public void setZIPPlus4(String ZIPPlus4) {
        this.ZIPPlus4 = ZIPPlus4;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    @Override
    public String toString() {
        return "SureAddressResponse{" +
                "message='" + message + '\'' +
                ", firmName='" + firmName + '\'' +
                ", address1='" + address1 + '\'' +
                ", address2='" + address2 + '\'' +
                ", urbanization='" + urbanization + '\'' +
                ", city='" + city + '\'' +
                ", county='" + county + '\'' +
                ", state='" + state + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", ZIPPlus4='" + ZIPPlus4 + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                '}';
    }
}
