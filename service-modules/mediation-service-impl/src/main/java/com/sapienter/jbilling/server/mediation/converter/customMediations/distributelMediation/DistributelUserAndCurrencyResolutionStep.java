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

package com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation;

import com.sapienter.jbilling.server.customer.CustomerService;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractUserResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;

import java.lang.Exception;
import java.lang.Integer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;

/**
 * Created by igutierrez on 25/01/17.
 */
public class DistributelUserAndCurrencyResolutionStep extends AbstractUserResolutionStep<MediationStepResult> {
    private CustomerService customerService;

    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            UserWS user = getUser(context);
            if (user != null) {
                String startStamp = PricingField.find(context.getPricingFields(), DistributelMediationConstant.INVOICE_DATE).getStrValue();
                Date date = new SimpleDateFormat(DistributelMediationConstant.DATE_FORMAT).parse(startStamp);
                String billingIdentifier = PricingField.find(context.getPricingFields(), DistributelMediationConstant.BILLING_IDENTIFIER).getStrValue();
                String subscriptionOrderID = PricingField.find(context.getPricingFields(), DistributelMediationConstant.SUBSCRIPTION_ORDER_ID).getStrValue();

                if (validateSubscriptionOrderID(context, subscriptionOrderID) && validateUserOrderActive(context, user, date) &&
                        validateBillingIdentifier(context, user, billingIdentifier)) {
                    return setUserOnResult(context.getResult(), buildUserMap(user));
                }
            } else {
                context.getResult().addError("User not found");

            }
            return false;
        } catch (Exception e) {
            context.getResult().addError("User not found");
            return false;
        }


    }

    private Map<String, Object> buildUserMap(UserWS user) {
        Map<String, Object> userMap = new HashMap<>();
        if (user != null) {
            userMap.put(MediationStepResult.USER_ID, user.getId());
            userMap.put(MediationStepResult.CURRENCY_ID, user.getCurrencyId());
            if (user.getDeleted() == 1) {
                userMap.put(USER_EXPIRED, true);
            }
        }
        return userMap;
    }

    private UserWS getUser(MediationStepContext context) {
        String accountField = PricingField.find(context.getPricingFields(), DistributelMediationConstant.CUSTOMER_NUMBER).getStrValue();
        return customerService.findUserById(Integer.parseInt(accountField));
    }

    private boolean hasValidPlanOrder(Integer userId, Integer entityId, Date date) {
        OrderWS order = customerService.findUserOrderByItemTypeDescription(entityId, userId, DistributelMediationConstant.MCF_RATED_ITEM_TYPE);
        if (order != null && (date.after(order.getActiveSince()) || date.equals(order.getActiveSince()))
                && (order.getActiveUntil() == null || date.before(order.getActiveUntil()) || date.equals(order.getActiveUntil()))) {
            return true;
        } else {
            return false;
        }
    }

    private boolean hasValidBillingIdentifier(Integer userId, String billingIdenfier) {
        OrderWS order = customerService.findUserOrderContainsAssetIdentifier(userId, billingIdenfier);
        if (order != null)
            return true;
        return false;
    }

    private boolean validateBillingIdentifier(MediationStepContext context, UserWS user, String billingIdenfier) {
        if (hasValidBillingIdentifier(user.getId(), billingIdenfier)) {
            return true;
        } else {
            context.getResult().addError("Unknown Billing Id");
            return false;
        }
    }

    private boolean validateUserOrderActive(MediationStepContext context, UserWS user, Date date) {
        if (hasValidPlanOrder(user.getId(), context.getEntityId(), date)) {
            return true;
        } else {
            context.getResult().addError("User has invalid order");
            context.getResult().addError("Inactive Customer");
            return false;
        }
    }

    private boolean validateSubscriptionOrderID(MediationStepContext context, String subscriptionOrderID) {
        try {
            Integer integerSubscriptionOrderID = Integer.parseInt(subscriptionOrderID);
            if (integerSubscriptionOrderID < 0) {
                context.getResult().addError("Invalid SubscriptionOrderID");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            context.getResult().addError("Invalid SubscriptionOrderID");
            return false;
        }

    }


}