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
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAndAssetAssociationResponseWS {
    private Integer userId;
    private Boolean isUserDeleted = false;
    private Integer entityId;
    private String identifier;//ICCID
    private String subscriberNumber;
    private Boolean isAssetDeleted = false;
    private Boolean isSuspended = false;
    private String assetStatus;
    private Boolean isUnaccounted = false;
}

