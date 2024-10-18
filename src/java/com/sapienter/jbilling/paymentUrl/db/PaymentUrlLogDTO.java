package com.sapienter.jbilling.paymentUrl.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.NoArgsConstructor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Entity
@Table(name="payment_url_log")
@NoArgsConstructor
public class PaymentUrlLogDTO implements Serializable {

    private Integer id;
    private Integer invoiceId;
    private String paymentUrlRequestPayload;
    private String mobileRequestPayload;
    private String paymentUrlResponse;
    private String paymentStatusResponse;
    private Status status;
    private String paymentUrl;
    private String paymentProvider;
    private Date createdAt = new Date();
    private Integer entityId;
    private BigDecimal paymentAmount;
    private String webhookResponse;
    private String gatewayId;
    private PaymentUrlType paymentUrlType;


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_url_log_generator")
    @SequenceGenerator(name = "payment_url_log_generator", sequenceName = "payment_url_log_seq", allocationSize = 1)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name = "invoice_id")
    public Integer getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
    }

    @Column(name = "entity_id", nullable = false)
    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Column(name = "mobile_request_payload")
    public String getMobileRequestPayload() {
        return mobileRequestPayload;
    }

    public void setMobileRequestPayload(String mobileRequestPayload) {
        this.mobileRequestPayload = mobileRequestPayload;
    }

    @Column(name = "payment_url_payload")
    public String getPaymentUrlRequestPayload() {
        return paymentUrlRequestPayload;
    }

    public void setPaymentUrlRequestPayload(String paymentUrlRequestPayload) {
        this.paymentUrlRequestPayload = paymentUrlRequestPayload;
    }

    @Column(name = "payment_url_response")
    public String getPaymentUrlResponse() {
        return paymentUrlResponse;
    }

    public void setPaymentUrlResponse(String paymentUrlResponse) {
        this.paymentUrlResponse = paymentUrlResponse;
    }

    @Column(name = "payment_status_response")
    public String getPaymentStatusResponse() {
        return paymentStatusResponse;
    }

    public void setPaymentStatusResponse(String paymentStatusResponse) {
        this.paymentStatusResponse = paymentStatusResponse;
    }

    @Column(name = "webhook_response")
    public String getWebhookResponse() {
        return webhookResponse;
    }

    public void setWebhookResponse(String webhookResponse) {
        this.webhookResponse = webhookResponse;
    }

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false)
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Column(name = "payment_url")
    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    @Column(name = "payment_provider")
    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    @Column(name = "create_datetime", nullable = false)
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Column(name = "payment_amount", nullable = false, precision = 17, scale = 17)
    public BigDecimal getPaymentAmount() {
        return this.paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    @Column(name = "gateway_id")
    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type")
    public PaymentUrlType getPaymentUrlType() {
        return paymentUrlType;
    }

    public void setPaymentUrlType(PaymentUrlType paymentUrlType) {
        this.paymentUrlType = paymentUrlType;
    }


    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
