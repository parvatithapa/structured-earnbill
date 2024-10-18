/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.common.steps;


import com.sapienter.jbilling.server.customer.CustomerService;
import com.sapienter.jbilling.server.customer.User;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import org.apache.log4j.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Panche Isajeski
 * @since 12/18/12
 */
public class JMRUserLoginResolutionStep extends AbstractUserResolutionStep<MediationStepResult> {

    private String usernameField = null;
    private CustomerService customerService;

    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        PricingField username = PricingField.find(context.getPricingFields(), getUsernameField());
        if (username != null) {
            return setUserOnResult(context.getResult(), buildUserMap(customerService.resolveUserByUsername(
                    context.getEntityId(), username.getStrValue())));
        }
        return false;
    }

    private  Map<String, Object> buildUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        if (user != null) {
            userMap.put(MediationStepResult.USER_ID, user.getId());
            userMap.put(MediationStepResult.CURRENCY_ID, user.getCurrencyId());
            if (user.getDeleted()) {
                userMap.put(USER_EXPIRED, user.getDeleted());
            }
        }
        return userMap;
    }

    public String getUsernameField() {
        return usernameField;
    }

    public void setUsernameField(String usernameField) {
        this.usernameField = usernameField;
    }

}
