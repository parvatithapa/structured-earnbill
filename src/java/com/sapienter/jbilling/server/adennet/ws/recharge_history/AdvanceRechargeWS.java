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
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceRechargeWS {
    private Long id;
    private Long txnId;
    private Integer entityId;
    private Integer userId;
    private Long subscriberNumber;
    private Integer planId;
    private String planDescription;
    private BigDecimal usageQuota;
    private Integer validityInDays;
    private BigDecimal planPrice;
    private String rechargeDate;
    private BigDecimal rechargeAmount;
    private BigDecimal totalRechargeAmount;
}
