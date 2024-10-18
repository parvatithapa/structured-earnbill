/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.diameter;

import com.sapienter.jbilling.server.item.PricingField;

import java.util.List;

public class BasicCdrKeyResolver implements CdrKeyResolver {
    private final static String DIAMETER_ID= "diameter";

    public String resolve (String sessionId, List<PricingField> fields) {
        return DIAMETER_ID+"-"+sessionId;
    }
}
