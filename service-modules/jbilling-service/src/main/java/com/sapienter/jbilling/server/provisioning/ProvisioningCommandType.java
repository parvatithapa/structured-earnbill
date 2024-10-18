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

import java.util.Arrays;

public enum ProvisioningCommandType {
    ORDER_LINE(0, "ORDER_LINE"),
    ORDER(1, "ORDER"),
    PAYMENT(2, "PAYMENT"),
    ASSET(3, "ASSET");

    private Integer key;
    private String value;
    ProvisioningCommandType(Integer key, String value) {
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

    public String getValue() {
        return value;
    }

    public static ProvisioningCommandType getEnum(String value){
        return Arrays.asList(ProvisioningCommandType.values()).stream()
                                                              .filter(x -> x.getValue().equals(value))
                                                              .findFirst().orElse(null);
    }
}
