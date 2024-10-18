/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.provisioning;

public enum ProvisioningRequestStatus {
    SUBMITTED(0, "SUBMITTED"),
    SUCCESSFUL(1, "SUCCESSFUL"),
    FAILED(2, "FAILED"),
    UNAVAILABLE(3, "UNAVAILABLE"),
    RETRY(4, "RETRY"),
    ROLLBACK(5, "ROLLBACK"),
    CANCELLED(6, "CANCELLED");

    private Integer key;
    private String value;
    private ProvisioningRequestStatus(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString(){
        return value;
    }

    public Integer toInteger(){
        return key;
    }
}
