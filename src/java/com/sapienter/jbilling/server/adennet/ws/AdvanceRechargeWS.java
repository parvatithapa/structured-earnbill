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

package com.sapienter.jbilling.server.adennet.ws;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AdvanceRechargeWS {

    private Long id;
    private Long ledgerId;
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
    private StatusEnum status;
    private String source;
    private String createdBy;
    private String sourceOfCancellation;
    private String canceledBy;

     public enum StatusEnum {
        PENDING("PENDING"),
        PROCESSING("PROCESSING"),
        PROCESSED("PROCESSED"),
        CANCELLED("CANCELLED"),
        TRANSACTION_REFUNDED ("TRANSACTION_REFUNDED");

        private String value;

        StatusEnum(String value) {
            this.value = value;
        }

        public static StatusEnum fromValue(String input) {
            for (StatusEnum b : StatusEnum.values()) {
                if (b.value.equals(input)) {
                    return b;
                }
            }
            return null;
        }

        public String getValue() {
            return value;
        }
    }
}
