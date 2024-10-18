/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.nges.export.row;

/**
 * Created by hitesh on 30/9/16.
 */
public class ExportEnrollmentRow extends ExportCustomerRow {

    //(M) Enrollment status
    private String status;
    //(O) charge product name1
    private String charge1;
    //(O) charge product name2
    private String charge2;
    //(O)
    private String taxExemptionCode;
    //(O)
    private String taxDiscountPercentage;
    //(O)
    private String customerSpecificRate;
    //(O)
    private String calculateTaxManually;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCharge1() {
        return charge1;
    }

    public void setCharge1(String charge1) {
        this.charge1 = charge1;
    }

    public String getCharge2() {
        return charge2;
    }

    public void setCharge2(String charge2) {
        this.charge2 = charge2;
    }

    public String getTaxExemptionCode() {
        return taxExemptionCode;
    }

    public void setTaxExemptionCode(String taxExemptionCode) {
        this.taxExemptionCode = taxExemptionCode;
    }

    public String getTaxDiscountPercentage() {
        return taxDiscountPercentage;
    }

    public void setTaxDiscountPercentage(String taxDiscountPercentage) {
        this.taxDiscountPercentage = taxDiscountPercentage;
    }

    public String getCustomerSpecificRate() {
        return customerSpecificRate;
    }

    public void setCustomerSpecificRate(String customerSpecificRate) {
        this.customerSpecificRate = customerSpecificRate;
    }

    public String getCalculateTaxManually() {
        return calculateTaxManually;
    }

    public void setCalculateTaxManually(String calculateTaxManually) {
        this.calculateTaxManually = calculateTaxManually;
    }

    public static String getHeader() {
        return "companyName,status,enrollmentType,commodity,productId,contractLength,utilityAccountNumber,customerStartDate,lifeSupport,meterType,passThroughCharges1,passThroughCharges2,taxExemptionCode,taxDiscountPercentage,customerName," +
                "serviceAddressLine1,serviceAddressLine2,serviceCity,serviceState,serviceZip,servicePhone,email,contactName,contactAddressLine1,contactAddressLine2," +
                "contactAddressCity,contactAddressState,contactAddressZip,contactPhone,communicationMode,adderFee,customerSpecificRate,calculateTaxManually,cityTax,countyTax," +
                "stateTax,federalTax,gRTTaxable,gRTNonTaxable,salesAgent1,salesRate1,salesAgent2,salesRate2,salesAgent3,salesRate3";
    }

    public static String getErrorFileHeader() {
        return "enrollment_id,error_message";
    }

    @Override
    public String getRow() {
        return super.row = companyName + "," +
                status + "," +
                customerType + "," +
                commodity + "," +
                productId + "," +
                contractLength + "," +
                utilityAccountNumber + "," +
                customerStartDate + "," +
                lifeSupport + "," +
                meterType + "," +
                charge1 + "," +
                charge2 + "," +
                taxExemptionCode + "," +
                taxDiscountPercentage + "," +
                customerName + "," +
                serviceAddressLine1 + "," +
                serviceAddressLine2 + "," +
                serviceCity + "," +
                serviceState + "," +
                serviceZip + "," +
                servicePhone + "," +
                email + "," +
                contactName + "," +
                contactAddressLine1 + "," +
                contactAddressLine2 + "," +
                contactAddressCity + "," +
                contactAddressState + "," +
                contactAddressZip + "," +
                contactPhone + "," +
                communicationMode + "," +
                adder + "," +
                customerSpecificRate + "," +
                calculateTaxManually + "," +
                cityTax + "," +
                countyTax + "," +
                stateTax + "," +
                federalTax + "," +
                gRTTabable + "," +
                gRTNonTaxable + "," +
                salesAgent1 + "," +
                salesRate1 + "," +
                salesAgent2 + "," +
                salesRate2 + "," +
                salesAgent3 + "," +
                salesRate3;
    }
}
