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
 * Created by hitesh on 3/8/16.
 */
public class ExportCustomerRow extends ExportRow {

    //(M) LDC Name
    protected String companyName;
    //(M) Customer zone value (customer lvl meta field)
    private String zone;
    //Supplier Id
    private String customerAcct;
    //(M) Customer is terminated or not.
    private String active;
    //(M) Residential or Commercial.
    protected String customerType;
    //(M) Electricity or Gas(E or G).
    protected String commodity;
    //(M) Plan’s Internal Number
    protected String productId;
    //Username
    private String custumerUsername;
    //(M) Customer Name.
    protected String customerName;
    //(M) Service Address 1
    protected String serviceAddressLine1;
    //(O) Service Address 2
    protected String serviceAddressLine2;
    //(M) Service City
    protected String serviceCity;
    //(M) Service State
    protected String serviceState;
    //(M) Service address Zip code
    protected String serviceZip;
    //(O) Service Phone.               ,
    protected String servicePhone;
    //(M)
    protected String communicationMode;
    //(M) Email.
    protected String email;
    //(M) Contact Name
    protected String contactName;
    //(M)Contact address 1
    protected String contactAddressLine1;
    //(O) Contact address 2
    protected String contactAddressLine2;
    //(M) Contact city
    protected String contactAddressCity;
    //(M) Contact state
    protected String contactAddressState;
    //(M) Zip code
    protected String contactAddressZip;
    //(O) Contact Phone
    protected String contactPhone;
    //(M) Utility_Account_Number
    protected String utilityAccountNumber;
    //(M) Interval or Non Interval or Unknown
    protected String meterType;
    //(M) Enrollment Date when customer got enrolled in system.
    private String productStartDate;
    //(O) Life Support
    protected String lifeSupport;
    //(O) It will be only for Rate ready customer. Rate.
    private String fixedPrice;
    //(O) Adder fee.
    protected String adder;
    //(M) Duration.
    protected String contractLength;
    //?
    private String swingUpperLimit;
    //?
    private String swingLowerLimit;
    //(O) Tax exempt Percentage
    private String taxExemptPercentage;
    //(O) City Tax if overridden at customer level
    protected String cityTax;
    //(O) Country Tax if overridden at customer level
    protected String countyTax;
    //(O) State Tax if overridden at customer level
    protected String stateTax;
    //(O)  Federal tax if overridden at customer level
    protected String federalTax;
    //?
    protected String gRTTabable;
    //?
    protected String gRTNonTaxable;
    //?
    private String annualUsage;
    //?
    private String uofM;
    //?
    private String discountAmount;
    //?
    private String discountMultiplier;
    //?
    private String receiveableType;
    //?
    private String billParty;
    //?
    private String billCalcParty;
    //?
    private String meterCalcParty;
    //?
    private String lineLoss1;
    //?
    private String lineLoss2;
    //Actual Start Date
    protected String customerStartDate;
    //?
    private String usageFlowDate;
    //?
    private String customerIncrement;
    //?
    private String contractID;
    //?
    private String contractStatus;
    //(O) SalesAgent1
    protected String salesAgent1;
    //(O) SalesRate1
    protected String salesRate1;
    //(O) SalesAgent 2
    protected String salesAgent2;
    //(O) SalesRate2
    protected String salesRate2;
    //(O) SalesAgent 3
    protected String salesAgent3;
    //(O) SalesRate3
    protected String salesRate3;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getCustomerAcct() {
        return customerAcct;
    }

    public void setCustomerAcct(String customerAcct) {
        this.customerAcct = customerAcct;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getCommodity() {
        return commodity;
    }

    public void setCommodity(String commodity) {
        this.commodity = commodity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getCustumerUsername() {
        return custumerUsername;
    }

    public void setCustumerUsername(String custumerUsername) {
        this.custumerUsername = custumerUsername;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getServiceAddressLine1() {
        return serviceAddressLine1;
    }

    public void setServiceAddressLine1(String serviceAddressLine1) {
        this.serviceAddressLine1 = serviceAddressLine1;
    }

    public String getServiceAddressLine2() {
        return serviceAddressLine2;
    }

    public void setServiceAddressLine2(String serviceAddressLine2) {
        this.serviceAddressLine2 = serviceAddressLine2;
    }

    public String getServiceCity() {
        return serviceCity;
    }

    public void setServiceCity(String serviceCity) {
        this.serviceCity = serviceCity;
    }

    public String getServiceState() {
        return serviceState;
    }

    public void setServiceState(String serviceState) {
        this.serviceState = serviceState;
    }

    public String getServiceZip() {
        return serviceZip;
    }

    public void setServiceZip(String serviceZip) {
        this.serviceZip = serviceZip;
    }

    public String getServicePhone() {
        return servicePhone;
    }

    public void setServicePhone(String servicePhone) {
        this.servicePhone = servicePhone;
    }

    public String getCommunicationMode() {
        return communicationMode;
    }

    public void setCommunicationMode(String communicationMode) {
        this.communicationMode = communicationMode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactAddressLine1() {
        return contactAddressLine1;
    }

    public void setContactAddressLine1(String contactAddressLine1) {
        this.contactAddressLine1 = contactAddressLine1;
    }

    public String getContactAddressLine2() {
        return contactAddressLine2;
    }

    public void setContactAddressLine2(String contactAddressLine2) {
        this.contactAddressLine2 = contactAddressLine2;
    }

    public String getContactAddressCity() {
        return contactAddressCity;
    }

    public void setContactAddressCity(String contactAddressCity) {
        this.contactAddressCity = contactAddressCity;
    }

    public String getContactAddressState() {
        return contactAddressState;
    }

    public void setContactAddressState(String contactAddressState) {
        this.contactAddressState = contactAddressState;
    }

    public String getContactAddressZip() {
        return contactAddressZip;
    }

    public void setContactAddressZip(String contactAddressZip) {
        this.contactAddressZip = contactAddressZip;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getUtilityAccountNumber() {
        return utilityAccountNumber;
    }

    public void setUtilityAccountNumber(String utilityAccountNumber) {
        this.utilityAccountNumber = utilityAccountNumber;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public String getProductStartDate() {
        return productStartDate;
    }

    public void setProductStartDate(String productStartDate) {
        this.productStartDate = productStartDate;
    }

    public String getLifeSupport() {
        return lifeSupport;
    }

    public void setLifeSupport(String lifeSupport) {
        this.lifeSupport = lifeSupport;
    }

    public String getFixedPrice() {
        return fixedPrice;
    }

    public void setFixedPrice(String fixedPrice) {
        this.fixedPrice = fixedPrice;
    }

    public String getAdder() {
        return adder;
    }

    public void setAdder(String adder) {
        this.adder = adder;
    }

    public String getContractLength() {
        return contractLength;
    }

    public void setContractLength(String contractLength) {
        this.contractLength = contractLength;
    }

    public String getSwingUpperLimit() {
        return swingUpperLimit;
    }

    public void setSwingUpperLimit(String swingUpperLimit) {
        this.swingUpperLimit = swingUpperLimit;
    }

    public String getSwingLowerLimit() {
        return swingLowerLimit;
    }

    public void setSwingLowerLimit(String swingLowerLimit) {
        this.swingLowerLimit = swingLowerLimit;
    }

    public String getTaxExemptPercentage() {
        return taxExemptPercentage;
    }

    public void setTaxExemptPercentage(String taxExemptPercentage) {
        this.taxExemptPercentage = taxExemptPercentage;
    }

    public String getCityTax() {
        return cityTax;
    }

    public void setCityTax(String cityTax) {
        this.cityTax = cityTax;
    }

    public String getCountyTax() {
        return countyTax;
    }

    public void setCountyTax(String countyTax) {
        this.countyTax = countyTax;
    }

    public String getStateTax() {
        return stateTax;
    }

    public void setStateTax(String stateTax) {
        this.stateTax = stateTax;
    }

    public String getFederalTax() {
        return federalTax;
    }

    public void setFederalTax(String federalTax) {
        this.federalTax = federalTax;
    }

    public String getgRTTabable() {
        return gRTTabable;
    }

    public void setgRTTabable(String gRTTabable) {
        this.gRTTabable = gRTTabable;
    }

    public String getgRTNonTaxable() {
        return gRTNonTaxable;
    }

    public void setgRTNonTaxable(String gRTNonTaxable) {
        this.gRTNonTaxable = gRTNonTaxable;
    }

    public String getAnnualUsage() {
        return annualUsage;
    }

    public void setAnnualUsage(String annualUsage) {
        this.annualUsage = annualUsage;
    }

    public String getUofM() {
        return uofM;
    }

    public void setUofM(String uofM) {
        this.uofM = uofM;
    }

    public String getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(String discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountMultiplier() {
        return discountMultiplier;
    }

    public void setDiscountMultiplier(String discountMultiplier) {
        this.discountMultiplier = discountMultiplier;
    }

    public String getReceiveableType() {
        return receiveableType;
    }

    public void setReceiveableType(String receiveableType) {
        this.receiveableType = receiveableType;
    }

    public String getBillParty() {
        return billParty;
    }

    public void setBillParty(String billParty) {
        this.billParty = billParty;
    }

    public String getBillCalcParty() {
        return billCalcParty;
    }

    public void setBillCalcParty(String billCalcParty) {
        this.billCalcParty = billCalcParty;
    }

    public String getMeterCalcParty() {
        return meterCalcParty;
    }

    public void setMeterCalcParty(String meterCalcParty) {
        this.meterCalcParty = meterCalcParty;
    }

    public String getLineLoss1() {
        return lineLoss1;
    }

    public void setLineLoss1(String lineLoss1) {
        this.lineLoss1 = lineLoss1;
    }

    public String getLineLoss2() {
        return lineLoss2;
    }

    public void setLineLoss2(String lineLoss2) {
        this.lineLoss2 = lineLoss2;
    }

    public String getCustomerStartDate() {
        return customerStartDate;
    }

    public void setCustomerStartDate(String customerStartDate) {
        this.customerStartDate = customerStartDate;
    }

    public String getUsageFlowDate() {
        return usageFlowDate;
    }

    public void setUsageFlowDate(String usageFlowDate) {
        this.usageFlowDate = usageFlowDate;
    }

    public String getCustomerIncrement() {
        return customerIncrement;
    }

    public void setCustomerIncrement(String customerIncrement) {
        this.customerIncrement = customerIncrement;
    }

    public String getContractID() {
        return contractID;
    }

    public void setContractID(String contractID) {
        this.contractID = contractID;
    }

    public String getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    public String getSalesAgent1() {
        return salesAgent1;
    }

    public void setSalesAgent1(String salesAgent1) {
        this.salesAgent1 = salesAgent1;
    }

    public String getSalesRate1() {
        return salesRate1;
    }

    public void setSalesRate1(String salesRate1) {
        this.salesRate1 = salesRate1;
    }

    public String getSalesAgent2() {
        return salesAgent2;
    }

    public void setSalesAgent2(String salesAgent2) {
        this.salesAgent2 = salesAgent2;
    }

    public String getSalesRate2() {
        return salesRate2;
    }

    public void setSalesRate2(String salesRate2) {
        this.salesRate2 = salesRate2;
    }

    public String getSalesAgent3() {
        return salesAgent3;
    }

    public void setSalesAgent3(String salesAgent3) {
        this.salesAgent3 = salesAgent3;
    }

    public String getSalesRate3() {
        return salesRate3;
    }

    public void setSalesRate3(String salesRate3) {
        this.salesRate3 = salesRate3;
    }

    public ExportCustomerRow buildAccountInfornation() {
        return this;
    }

    public ExportCustomerRow buildServiceInformation() {
        return this;
    }

    public ExportCustomerRow buildBillingInformation() {
        return this;
    }


    public static String getHeader() {
        return "companyName,zone,customerAcct,active,customerType,commodity,productId,custumerUsername,customerName," +
                "serviceAddressLine1,serviceAddressLine2,serviceCity,serviceState,serviceZip,servicePhone,communicationMode,email," +
                "contactName,contactAddressLine1,contactAddressLine2,contactAddressCity,contactAddressState,contactAddressZip,contactPhone," +
                "utilityAccountNumber,meterType,productStartDate,lifeSupport,fixedPrice,adder,contractLength,swingUpperLimit,swingLowerLimit," +
                "taxExemptPercentage,cityTax,countyTax,stateTax,federalTax,gRTTabable,gRTNonTaxable,annualUsage,uofM,discountAmount,discountMultiplier," +
                "receiveableType,billParty,billCalcParty,meterCalcParty,lineLoss1,lineLoss2,customerStartDate,usageFlowDate,customerIncrement,contractID," +
                "contractStatus,salesAgent1,salesRate1,salesAgent2,salesRate2,salesAgent3,salesRate3";
    }

    public static String getErrorFileHeader() {
        return "user_id,error_message";
    }

    @Override
    public String getRow() {

        return super.row = companyName + "," +
                zone + "," +
                customerAcct + "," +
                active + "," +
                customerType + "," +
                commodity + "," +
                productId + "," +
                custumerUsername + "," +
                customerName + "," +
                serviceAddressLine1 + "," +
                serviceAddressLine2 + "," +
                serviceCity + "," +
                serviceState + "," +
                serviceZip + "," +
                servicePhone + "," +
                communicationMode + "," +
                email + "," +
                contactName + "," +
                contactAddressLine1 + "," +
                contactAddressLine2 + "," +
                contactAddressCity + "," +
                contactAddressState + "," +
                contactAddressZip + "," +
                contactPhone + "," +
                utilityAccountNumber + "," +
                meterType + "," +
                productStartDate + "," +
                lifeSupport + "," +
                fixedPrice + "," +
                adder + "," +
                contractLength + "," +
                swingUpperLimit + "," +
                swingLowerLimit + "," +
                taxExemptPercentage + "," +
                cityTax + "," +
                countyTax + "," +
                stateTax + "," +
                federalTax + "," +
                gRTTabable + "," +
                gRTNonTaxable + "," +
                annualUsage + "," +
                uofM + "," +
                discountAmount + "," +
                discountMultiplier + "," +
                receiveableType + "," +
                billParty + "," +
                billCalcParty + "," +
                meterCalcParty + "," +
                lineLoss1 + "," +
                lineLoss2 + "," +
                customerStartDate + "," +
                usageFlowDate + "," +
                customerIncrement + "," +
                contractID + "," +
                contractStatus + "," +
                salesAgent1 + "," +
                salesRate1 + "," +
                salesAgent2 + "," +
                salesRate2 + "," +
                salesAgent3 + "," +
                salesRate3;
    }
}
