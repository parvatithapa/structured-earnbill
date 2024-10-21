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

package com.sapienter.jbilling.auth;

import com.sapienter.jbilling.server.adennet.AdennetConstants;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class AdennetJwtTokenResponseWS {

    @ToString.Exclude
    private final String token;
    @ToString.Exclude
    private final String refreshToken;
    private final long createTime;
    private final String tokenType;
    private final String subscriberNumber;
    private final Boolean suspended;
    private final Boolean isAssetDeleted;
    private final Boolean isUserDeleted;
    private final String assetStatus;
    private final String languageDescription;

    public AdennetJwtTokenResponseWS(String token, String refreshToken, long createTime, String tokenType, String subscriberNumber, Boolean suspended, Boolean isAssetDeleted, Boolean isUserDeleted, String assetStatus, String languageDescription) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.createTime = createTime;
        this.tokenType = tokenType;
        this.subscriberNumber = subscriberNumber;
        this.suspended = suspended;
        this.isAssetDeleted = isAssetDeleted;
        this.isUserDeleted = isUserDeleted;
        this.assetStatus = assetStatus;
        this.languageDescription = languageDescription;
    }

}
