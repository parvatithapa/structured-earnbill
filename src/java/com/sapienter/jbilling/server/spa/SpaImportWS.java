package com.sapienter.jbilling.server.spa;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by pablo_galera on 10/01/17.
 */
public class SpaImportWS implements Serializable {

    private String apiId;
    private String transactionId;
    private Date transactionDate;
    private String staffIdentifier;
    private String customerName;
    private String customerCompany;
    private String phoneNumber1;
    private String phoneNumber2;
    private String emailAddress;
    private Date emailVerified;
    private String language;
    private List<SpaAddressWS> addresses;
    private List<SpaProductsOrderedWS> productsOrdered;
    private SpaPaymentCredentialWS paymentCredential;
    private SpaPaymentResultWS paymentResult;
    private String taxExempt;
    private String taxExemptionCode;
    private String comments;
    private String confirmationNumber;
    private String communicationSent;
    private String requiredAdjustmentDetails;
    private Integer customerId;
    
    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getStaffIdentifier() {
        return staffIdentifier;
    }

    public void setStaffIdentifier(String staffIdentifier) {
        this.staffIdentifier = staffIdentifier;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerCompany() {
        return customerCompany;
    }

    public void setCustomerCompany(String customerCompany) {
        this.customerCompany = customerCompany;
    }

    public String getPhoneNumber1() {
        return phoneNumber1;
    }

    public void setPhoneNumber1(String phoneNumber1) {
        this.phoneNumber1 = phoneNumber1;
    }

    public String getPhoneNumber2() {
        return phoneNumber2;
    }

    public void setPhoneNumber2(String phoneNumber2) {
        this.phoneNumber2 = phoneNumber2;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Date getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Date emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<SpaProductsOrderedWS> getProductsOrdered() {
        return productsOrdered;
    }

    public void setProductsOrdered(List<SpaProductsOrderedWS> productsOrdered) {
        this.productsOrdered = productsOrdered;
    }

    public SpaPaymentCredentialWS getPaymentCredential() {
        return paymentCredential;
    }

    public void setPaymentCredential(SpaPaymentCredentialWS paymentCredential) {
        this.paymentCredential = paymentCredential;
    }

    public SpaPaymentResultWS getPaymentResult() {
        return paymentResult;
    }

    public void setPaymentResult(SpaPaymentResultWS paymentResult) {
        this.paymentResult = paymentResult;
    }

    public String getTaxExempt() {
        return taxExempt;
    }

    public void setTaxExempt(String taxExempt) {
        this.taxExempt = taxExempt;
    }

    public String getTaxExemptionCode() {
        return taxExemptionCode;
    }

    public void setTaxExemptionCode(String taxExemptionCode) {
        this.taxExemptionCode = taxExemptionCode;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public String getCommunicationSent() {
        return communicationSent;
    }

    public void setCommunicationSent(String communicationSent) {
        this.communicationSent = communicationSent;
    }

    public List<SpaAddressWS> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<SpaAddressWS> addresses) {
        this.addresses = addresses;
    }

    public String getRequiredAdjustmentDetails() {
        return requiredAdjustmentDetails;
    }

    public void setRequiredAdjustmentDetails(String requiredAdjustmentDetails) {
        this.requiredAdjustmentDetails = requiredAdjustmentDetails;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("apiId: ").append(apiId);
        stringBuilder.append("\ntransactionId: ").append(transactionId);
        stringBuilder.append("\ntransactionDate: ").append(transactionDate);
        stringBuilder.append("\nstaffIdentifier: ").append(staffIdentifier);
        stringBuilder.append("\ncustomerId: ").append(customerId);
        stringBuilder.append("\ncustomerName: ").append(customerName);
        stringBuilder.append("\ncustomerCompany: ").append(customerCompany);
        stringBuilder.append("\nphoneNumber1: ").append(phoneNumber1);
        stringBuilder.append("\nphoneNumber2: ").append(phoneNumber2);
        stringBuilder.append("\nemailAddress: ").append(emailAddress);
        stringBuilder.append("\nemailVerified: ").append(emailVerified);
        stringBuilder.append("\nlanguage: ").append(language);
        stringBuilder.append("\ntaxExempt: ").append(taxExempt);
        stringBuilder.append("\ntaxExemptionCode: ").append(taxExemptionCode);
        stringBuilder.append("\ncomments: ").append(comments);
        stringBuilder.append("\nconfirmationNumber: ").append(confirmationNumber);
        stringBuilder.append("\ncommunicationSent: ").append(communicationSent);
        stringBuilder.append("\nproductsOrdered: ");
        if (addresses != null) {
            for (SpaAddressWS address : addresses) {
                stringBuilder.append("\naddress: ").append(address);
            }
        }
        if (productsOrdered != null) {
            for (int i = 0; i < productsOrdered.size(); i++) {
                stringBuilder.append("\n - product ordered ").append(i).append(": ").append(productsOrdered.get(i));
            }
        }
        stringBuilder.append("\npaymentCredential: ").append(paymentCredential);
        stringBuilder.append("\npaymentResult: ").append(paymentResult);
        return stringBuilder.toString();
    }

    public SpaAddressWS getAddress(AddressType addressType) {
        for (SpaAddressWS address : addresses) {
            if (addressType.equals(AddressType.getByName(address.getAddressType()))) {
                return address;
            }
        }
        return null;
    }

    public boolean isUpdateCustomer() {
        return customerId != null;
    }
    
}
