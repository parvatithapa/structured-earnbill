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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptWS {
    private String receiptNumber;
    private String receiptDate;
    private Integer userId;
    private String subscriberNumber;
    private String receiptType;
    private BigDecimal totalReceiptAmount;
    private String operationType;
    private String userName;
    private String address;
    private String contactNumber;
    private String email;
    private String createdBy;

    private PrimaryPlanWS primaryPlanWS;
    private List<FeeWS> feeWSList;
    private List<AddOnProductWS> addOnProductWS;

    private BigDecimal topUpAmount;
}
