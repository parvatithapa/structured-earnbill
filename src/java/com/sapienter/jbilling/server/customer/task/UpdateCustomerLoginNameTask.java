/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.customer.task;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customer.event.UpdateCustomerEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

/**
 * Created by Ashok Kale on 12/04/2018.
 */
public class UpdateCustomerLoginNameTask extends PluggableTask implements IInternalEventsTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAMETER_LOGIN_NAME_META_FIELD_NAME =
            new ParameterDescription("Login Name Meta Field Name", true, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAMETER_ACCOUNT_INFORMATION_LOGIN_NAME_META_FIELD_GROUP_NAME =
            new ParameterDescription("AIT Meta Field Group Name", true, ParameterDescription.Type.STR);

    //initializer for pluggable params
    public UpdateCustomerLoginNameTask() {
        descriptions.add(PARAMETER_LOGIN_NAME_META_FIELD_NAME);
        descriptions.add(PARAMETER_ACCOUNT_INFORMATION_LOGIN_NAME_META_FIELD_GROUP_NAME);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        UpdateCustomerEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof UpdateCustomerEvent) {
            processUpdateCustomerLoginName((UpdateCustomerEvent) event);
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private void processUpdateCustomerLoginName(UpdateCustomerEvent event) {
        String aitGroupName = getAITGroupName();
        String loginNameFieldName = getLoginNameMetaFieldName();

        logger.debug("Account Information Type Group name {} and Meta-Field name {} ",aitGroupName, loginNameFieldName);

        if (StringUtils.isNotEmpty(aitGroupName) && StringUtils.isNotEmpty(loginNameFieldName)) {
            CustomerDTO customer = new CustomerDAS().findNow(event.getCustomerId());
            Integer entityId = customer.getBaseUser().getCompany().getId();

            AccountInformationTypeDTO contactInformationAIT = getAccountInformationTypeDTO(aitGroupName, customer.getAccountType().getInformationTypes());
            if (null != contactInformationAIT) {
                Integer contactInformationAITid = contactInformationAIT.getId();
                CustomerAccountInfoTypeMetaField customerAitMF = customer.getCurrentCustomerAccountInfoTypeMetaField(loginNameFieldName, contactInformationAITid) ;
                if (null != customerAitMF) {
                    String customerName = customerAitMF.getMetaFieldValue().getValue().toString();
                    String userName = customer.getBaseUser().getUserName();
                    if (!StringUtils.equals(customerName, userName)) {
                        if (new UserBL().exists(customerName, entityId)) {
                            logger.error("User already exists with username {} ", customerName);
                            throw new SessionInternalError(
                                    "User already exists with username",
                                    new String[] { "UserWS,userName,Login name is already in use "+ customerName }, HttpStatus.SC_BAD_REQUEST);
                        }
                        logger.debug("Customer new login name {} ",customerName);
                        customer.getBaseUser().setUserName(customerName);
                    }
                }
            }
        }
    }

    private AccountInformationTypeDTO getAccountInformationTypeDTO(String aitGroupName, Set<AccountInformationTypeDTO> contactInformationAITs) {
        Optional<AccountInformationTypeDTO> contactInformationAIT = contactInformationAITs.stream().filter(at -> at.getName().equals(aitGroupName)).findFirst();
        if (contactInformationAIT.isPresent()) {
            return contactInformationAIT.get();
        }
        return null;
    }

    public  String getAITGroupName() {
        return  parameters.get(PARAMETER_ACCOUNT_INFORMATION_LOGIN_NAME_META_FIELD_GROUP_NAME.getName());
    }

    public  String getLoginNameMetaFieldName() {
        return  parameters.get(PARAMETER_LOGIN_NAME_META_FIELD_NAME.getName());
    }
}
