/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2017] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.servicesummary;

import java.util.Date;

public class ServiceSummaryWS {

    private int id;
    private Integer invoiceId;
    private Integer userId;
    private Integer invoiceLineId;
    private Integer itemId;
    private Integer planId;
    private String planDescription;
    private String serviceDescription;
    private String serviceId;
    private Date startDate;
    private Date endDate;
    private String displayIdentifier;
    private boolean isPlan = false;
    private Integer subscriptionOrderId;
    private Integer creditNoteId;

    public ServiceSummaryWS() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getInvoiceLineId() {
        return invoiceLineId;
    }

    public void setInvoiceLineId(Integer invoiceLineId) {
        this.invoiceLineId = invoiceLineId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getPlanDescription() {
        return planDescription;
    }

    public void setPlanDescription(String planDescription) {
        this.planDescription = planDescription;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public boolean getIsPlan() {
        return isPlan;
    }

    public void setIsPlan(boolean isPlan) {
        this.isPlan = isPlan;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDisplayIdentifier() {
        return displayIdentifier;
    }

    public void setDisplayIdentifier(String displayIdentifier) {
        this.displayIdentifier = displayIdentifier;
    }

    public Integer getSubscriptionOrderId() {
        return subscriptionOrderId;
    }

    public void setSubscriptionOrderId(Integer subscriptionOrderId) {
        this.subscriptionOrderId = subscriptionOrderId;
    }
    
    public Integer getCreditNoteId() {
        return creditNoteId;
    }

    public void setCreditNoteId(Integer creditNoteId) {
        this.creditNoteId = creditNoteId;
    }

	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ServiceSummaryWS [id=");
        builder.append(id);
        builder.append(", invoiceId=");
        builder.append(invoiceId);
        builder.append(", userId=");
        builder.append(userId);
        builder.append(", invoiceLineId=");
        builder.append(invoiceLineId);
        builder.append(", itemId=");
        builder.append(itemId);
        builder.append(", planId=");
        builder.append(planId);
        builder.append(", planDescription=");
        builder.append(planDescription);
        builder.append(", serviceDescription=");
        builder.append(serviceDescription);
        builder.append(", assetIdentifier=");
        builder.append(serviceId);
        builder.append(", startDate=");
        builder.append(startDate);
        builder.append(", endDate=");
        builder.append(endDate);
        builder.append(", isPlan=");
        builder.append(isPlan);
        builder.append(", subscriptionOrderId=");
        builder.append(subscriptionOrderId);
        builder.append(", creditNoteId=");
        builder.append(creditNoteId);
        builder.append("]");
        return builder.toString();
    }
}
