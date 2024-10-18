package com.sapienter.jbilling.server.invoice.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@SuppressWarnings("serial")
public class SpcDetailedBillingReportDTO implements Serializable {
    
    private Integer billingProcessId;
    private Integer invoiceId;
    private Date invoiceDate;
    private String revenueGlCode;
    private Integer userId;
    private String userName;
    private String productCode;
    private String callIdentifier;
    private String planOrProductName;
    private Date producEndDate;
    private String serviceEmail;
    private String serviceNumber;
    private String serviceType;
    private String serviceDescription;
    private String costsGlCode;
    private String planType;
    private String taxCode;
    private BigDecimal salesExGst;
    private BigDecimal gst;
    private String rollupCode;
    private String superRollupCode;
    private String superSuperRollupCode;
    private String tariffCode;
    private BigDecimal costOfService;
    private String origin;
    private String tariffDescription;
    private String rollupDescription;
    private String superRollupDescription;
    private String superSuperRollupDescription;
    private Date fromDate;
    private Date toDate;

    public SpcDetailedBillingReportDTO() { }
    
    public SpcDetailedBillingReportDTO(Integer billingProcessId, Integer invoiceId, Date invoiceDate) {
        this.billingProcessId = billingProcessId;
        this.invoiceId = invoiceId;
        this.invoiceDate = invoiceDate;
    }

    public Integer getBillingProcessId() {
        return billingProcessId;
    }

    public void setBillingProcessId(Integer billingProcessId) {
        this.billingProcessId = billingProcessId;
    }
    
    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }
    
    public String getRevenueGlCode() {
        return revenueGlCode;
    }

    public void setRevenueGlCode(String revenueGlCode) {
        this.revenueGlCode = revenueGlCode;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getCallIdentifier() {
        return callIdentifier;
    }

    public void setCallIdentifier(String callIdentifier) {
        this.callIdentifier = callIdentifier;
    }

    public String getPlanOrProductName() {
        return planOrProductName;
    }

    public void setPlanOrProductName(String planOrProductName) {
        this.planOrProductName = planOrProductName;
    }

    public Date getProducEndDate() {
        return producEndDate;
    }

    public void setProducEndDate(Date producEndDate) {
        this.producEndDate = producEndDate;
    }

    public String getServiceEmail() {
        return serviceEmail;
    }

    public void setServiceEmail(String serviceEmail) {
        this.serviceEmail = serviceEmail;
    }

    public String getServiceNumber() {
        return serviceNumber;
    }

    public void setServiceNumber(String serviceNumber) {
        this.serviceNumber = serviceNumber;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getCostsGlCode() {
        return costsGlCode;
    }

    public void setCostsGlCode(String costsGlCode) {
        this.costsGlCode = costsGlCode;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public BigDecimal getSalesExGst() {
        return salesExGst;
    }

    public void setSalesExGst(BigDecimal salesExGst) {
        this.salesExGst = salesExGst;
    }

    public BigDecimal getGst() {
        return gst;
    }

    public void setGst(BigDecimal gst) {
        this.gst = gst;
    }

    public String getRollupCode() {
        return rollupCode;
    }

    public void setRollupCode(String rollupCode) {
        this.rollupCode = rollupCode;
    }

    public String getSuperRollupCode() {
        return superRollupCode;
    }

    public void setSuperRollupCode(String superRollupCode) {
        this.superRollupCode = superRollupCode;
    }

    public String getSuperSuperRollupCode() {
        return superSuperRollupCode;
    }

    public void setSuperSuperRollupCode(String superSuperRollupCode) {
        this.superSuperRollupCode = superSuperRollupCode;
    }

    public String getTariffCode() {
        return tariffCode;
    }

    public void setTariffCode(String tariffCode) {
        this.tariffCode = tariffCode;
    }

    public BigDecimal getCostOfService() {
        return costOfService;
    }

    public void setCostOfService(BigDecimal costOfService) {
        this.costOfService = costOfService;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getTariffDescription() {
        return tariffDescription;
    }

    public void setTariffDescription(String tariffDescription) {
        this.tariffDescription = tariffDescription;
    }

    public String getRollupDescription() {
        return rollupDescription;
    }

    public void setRollupDescription(String rollupDescription) {
        this.rollupDescription = rollupDescription;
    }

    public String getSuperRollupDescription() {
        return superRollupDescription;
    }

    public void setSuperRollupDescription(String superRollupDescription) {
        this.superRollupDescription = superRollupDescription;
    }

    public String getSuperSuperRollupDescription() {
        return superSuperRollupDescription;
    }

    public void setSuperSuperRollupDescription(String superSuperRollupDescription) {
        this.superSuperRollupDescription = superSuperRollupDescription;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}
