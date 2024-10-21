/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.adennet.ws.recharge_history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechargeTransactionWS {
    private Long id;
    private Integer entityId;
    private Integer userId;
    private String subscriberNumber;
    private BigDecimal rechargeAmount;
    private BigDecimal totalRechargeAmount;
    private Boolean isSimReIssued;
    private String type;
    private String transactionDate;
    private String source;
    private String status;
    private Boolean isSimIssued;
    private Boolean isWalletTopUp;
    private String sourceOfRefund;
    private String refundedBy;
    private String note;
    private Boolean isRefundable;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private Boolean isRefund;
    private BigDecimal refundAmount;
    private Boolean isActiveNow;
    private String refundDate;
    private Integer planId;
    private String planDescription;
    private Long parentTxnId;
}
