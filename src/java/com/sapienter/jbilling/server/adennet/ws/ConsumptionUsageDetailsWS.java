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
public class ConsumptionUsageDetailsWS {
    private Long id;
    private Integer userId;
    private Long subscriberNumber;
    private Integer planId;
    private String planDescription;
    private String startDate;
    private String endDate;
    private BigDecimal initialQuantity;
    private BigDecimal availableQuantity;
    private String status;
    private Integer orderId;
    private String dataType;
    private Boolean isAddOn;
}
