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

public interface DiameterUserLocator {

    public static final String ATTRIBUTE_SUBSCRIPTION_ID_DATA = "Subscription-Id-Data";

	public Integer findUserFromParameters(Integer entityId, List<PricingField> pricingFields);
}
