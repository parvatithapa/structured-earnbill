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
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RechargeRequestWS {
    private Integer entityId;
    private Integer userId;
    @Builder.Default
    private Boolean isSimIssued=false;
    private String subscriberNumber;
    private PrimaryPlanWS primaryPlan;
    private List<FeeWS> fees;
    private List<AddOnProductWS> addOnProducts;
    private BigDecimal rechargeAmount;
    @Builder.Default
    private Boolean activatePrimaryPlanImmediately = false;
    private String rechargeDateTime;
    private String rechargedBy;
    private String source;
    private String sourceRefId;
    private String governorate;
    private String bankTimeStamp ;
    private Map<String, Object> additionalFields;
    private Integer orderId;

}
