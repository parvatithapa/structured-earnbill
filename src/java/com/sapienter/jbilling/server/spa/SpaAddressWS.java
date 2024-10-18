package com.sapienter.jbilling.server.spa;

import java.io.Serializable;

/**
 * Created by pablo_galera on 16/01/17.
 */
public class SpaAddressWS implements Serializable {

    private String addressType;
    private String postalCode;
    private String streetNumber;
    private String streetNumberSufix;
    private String streetName;
    private String streetType;
    private String streetAptSuite;
    private String streetDirecton;
    private String city;
    private String province;

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public String getStreetNumberSufix() {
        return streetNumberSufix;
    }

    public void setStreetNumberSufix(String streetNumberSufix) {
        this.streetNumberSufix = streetNumberSufix;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getStreetType() {
        return streetType;
    }

    public void setStreetType(String streetType) {
        this.streetType = streetType;
    }

    public String getStreetAptSuite() {
        return streetAptSuite;
    }

    public void setStreetAptSuite(String streetAptSuite) {
        this.streetAptSuite = streetAptSuite;
    }

    public String getStreetDirecton() {
        return streetDirecton;
    }

    public void setStreetDirecton(String streetDirecton) {
        this.streetDirecton = streetDirecton;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addressType: ").append(addressType);
        stringBuilder.append("\npostalCode: ").append(postalCode);
        stringBuilder.append("\nstreetNumber: ").append(streetNumber);
        stringBuilder.append("\nstreetNumberSufix: ").append(streetNumberSufix);
        stringBuilder.append("\nstreetName: ").append(streetName);
        stringBuilder.append("\nstreetType: ").append(streetType);
        stringBuilder.append("\nstreetAptSuite: ").append(streetAptSuite);
        stringBuilder.append("\nstreetDirecton: ").append(streetDirecton);
        stringBuilder.append("\ncity: ").append(city);
        stringBuilder.append("\nprovince: ").append(province);
        return stringBuilder.toString();
    }



}
