package com.sapienter.jbilling.server.util.core;/*
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
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by marcomanzi on 5/30/14.
 */
public class JBillingController {

    private static final Logger logger = LoggerFactory.getLogger(JBillingController.class);
    private JbillingAPI api;
    private int callerId;
    private int languageId;

    public JBillingController(JbillingAPI api) {
        this.api = api;
        callerId = api.getCallerCompanyId();
        languageId = api.getCallerLanguageId();
    }

    public UserWS createUser(String username, Integer currencyId,
                                    Integer accountTypeId, boolean doCreate){
        //Create - This passes the password validation routine.
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName(username);
        newUser.setPassword("123qwe");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(Integer.valueOf(401));
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(new Boolean(false));
        newUser.setAccountTypeId(accountTypeId);
        logger.debug("User properties set");

        if (doCreate) {
            logger.debug("Creating user ...");
            newUser.setUserId(api.createUser(newUser));
        }
        logger.debug("User created with id:{}", newUser.getUserId());
        return newUser;
    }

    public int createAccountTypeWithName (String name) {
        AccountTypeWS accountTypeWS = accountFor(name);
        if (accountTypeWS != null) {
            return accountTypeWS.getId();
        }
        AccountTypeWS accountType = new AccountTypeWS();
        accountType.setName(name, languageId);
        accountType.setInvoiceDesign("invoice_design");
        accountType.setCurrencyId(1);
        accountType.setLanguageId(1);
        accountType.setInvoiceDeliveryMethodId(1);
        return api.createAccountType(accountType);
    }

    private AccountTypeWS accountFor(String name) {
        AccountTypeWS[] allAccountTypes = api.getAllAccountTypes();
        for (AccountTypeWS accountTypeWS: allAccountTypes) {
            if (accountTypeWS.getEntityId().equals(callerId) &&
                    accountTypeWS.getDescription(languageId).getContent().equals(name)) {
                return accountTypeWS;
            }
        }
        return null;
    }
}
