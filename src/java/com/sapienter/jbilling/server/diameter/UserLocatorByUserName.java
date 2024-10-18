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
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

import java.util.List;

/**
 * Finds a user by its User name field.
 */
public class UserLocatorByUserName implements DiameterUserLocator {
	
	private String fieldName = "Subscription-Id-Data";
	
    @Override
    public Integer findUserFromParameters(Integer entityId, List<PricingField> params) {
        Integer userId = null;
        
        PricingFieldsHelper fields = new PricingFieldsHelper(params);

        if (params != null){
            String username = fields.getField(fieldName).getStrValue();
            UserDTO user = new UserDAS().findByUserName(username, entityId);

            if (user != null) {
                userId = user.getUserId();
            }
        }
        return userId;
    }

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
}
