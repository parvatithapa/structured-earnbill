/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech.
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.crm.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class CRMPayment implements JSONObject {
    private final String paymentTypeId = "CUSTOMER_PAYMENT";
    private String partyIdFrom;
    private String partyIdTo;
    private BigDecimal amount;
    private String statusId;
    private String paymentMethodId;
    private String invoiceId;
    private String paymentGatewayResponseId;
    private String comments;
}
