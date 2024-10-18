package com.sapienter.jbilling.server.customer.report;

/**
 * @author Javier Rivero
 * @since 26/12/15.
 */
public class ComplianceReportDET {
    //DET
    private final String detRecordType = "DET";
    private Integer recordNumber;
    private String CRDUNSNumber;
    private String ESIIDNumber;
    private String customerFirstName;
    private String customerLastName;
    private String customerCompanyName;
    private String customerCompanyContactName;
    private String primaryPhoneNumber;
    private String primaryPhoneNumberExtension;
    private String termination;

    public ComplianceReportDET() {
        ESIIDNumber = "";
        customerFirstName = "";
        customerLastName = "";
        customerCompanyName = "";
        customerCompanyContactName = "";
        primaryPhoneNumber = "";
        primaryPhoneNumberExtension = "";
    }

    public String getDetRecordType() {
        return detRecordType;
    }

    public Integer getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(Integer recordNumber) {
        this.recordNumber = recordNumber;
    }

    public String getCRDUNSNumber() {
        return CRDUNSNumber;
    }

    public void setCRDUNSNumber(String CRDUNSNumber) {
        this.CRDUNSNumber = CRDUNSNumber;
    }

    public String getESIIDNumber() {
        return ESIIDNumber;
    }

    public void setESIIDNumber(String ESIIDNumber) {
        this.ESIIDNumber = ESIIDNumber;
    }

    public String getCustomerFirstName() {
        return customerFirstName;
    }

    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }

    public String getCustomerLastName() {
        return customerLastName;
    }

    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }

    public String getCustomerCompanyName() {
        return customerCompanyName;
    }

    public void setCustomerCompanyName(String customerCompanyName) {
        this.customerCompanyName = customerCompanyName;
    }

    public String getCustomerCompanyContactName() {
        return customerCompanyContactName;
    }

    public void setCustomerCompanyContactName(String customerCompanyContactName) {
        this.customerCompanyContactName = customerCompanyContactName;
    }

    public String getPrimaryPhoneNumber() {
        return primaryPhoneNumber;
    }

    public void setPrimaryPhoneNumber(String primaryPhoneNumber) {
        this.primaryPhoneNumber = primaryPhoneNumber;
    }

    public String getPrimaryPhoneNumberExtension() {
        return primaryPhoneNumberExtension;
    }

    public void setPrimaryPhoneNumberExtension(String primaryPhoneNumberExtension) {
        this.primaryPhoneNumberExtension = primaryPhoneNumberExtension;
    }

    public String getTermination() {
        return termination;
    }

    public void setTermination(String termination) {
        this.termination = termination;
    }
}
