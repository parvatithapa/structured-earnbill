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

import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanChangeRequestWS {
    private Integer entityId;
    private Integer userId;
    private List<SubscriptionWS> subscriptions;
    @ApiModelProperty(value = "Start date in the format yyyy-MM-dd HH:mm:ss")
    private String startDate;
    @ApiModelProperty(value = "Start date in the format yyyy-MM-dd HH:mm:ss")
    private String endDate;
    private Integer previousOrderId;
}
