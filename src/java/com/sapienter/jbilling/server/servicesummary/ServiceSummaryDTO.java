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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@TableGenerator(
        name            = "service_summary_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "service_summary",
        allocationSize  = 100)
@Table(name = "service_summary")
public class ServiceSummaryDTO {

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

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "service_summary_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "plan_id", nullable = true)
    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    @Column(name = "invoice_id", nullable = false)
    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    @Column(name = "user_id", nullable = false)
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Column(name = "invoice_line_id", nullable = false)
    public Integer getInvoiceLineId() {
        return invoiceLineId;
    }

    public void setInvoiceLineId(Integer invoiceLineId) {
        this.invoiceLineId = invoiceLineId;
    }

    @Column(name = "item_id", nullable = true)
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @Column(name = "plan_description", nullable = true)
    public String getPlanDescription() {
        return planDescription;
    }

    public void setPlanDescription(String planDescription) {
        this.planDescription = planDescription;
    }

    @Column(name = "service_description", nullable = false)
    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    @Column(name = "service_id", nullable = true)
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    @Column(name = "is_plan")
    public boolean getIsPlan() {
        return isPlan;
    }

    public void setIsPlan(boolean isPlan) {
        this.isPlan = isPlan;
    }

    @Column(name = "start_date", nullable = true)
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column(name = "end_date", nullable = true)
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Column(name = "display_identifier", nullable = true)
    public String getDisplayIdentifier() {
        return displayIdentifier;
    }

    public void setDisplayIdentifier(String displayIdentifier) {
        this.displayIdentifier = displayIdentifier;
    }

    @Column(name = "subscription_order_id", nullable = true)
    public Integer getSubscriptionOrderId() {
        return subscriptionOrderId;
    }

    public void setSubscriptionOrderId(Integer subscriptionOrderId) {
        this.subscriptionOrderId = subscriptionOrderId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ServiceSummaryDTO [id=");
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
        builder.append("]");
        return builder.toString();
    }
}
