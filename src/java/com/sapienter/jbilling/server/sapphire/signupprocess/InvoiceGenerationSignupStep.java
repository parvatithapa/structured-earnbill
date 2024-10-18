package com.sapienter.jbilling.server.sapphire.signupprocess;

import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import grails.plugin.springsecurity.SpringSecurityUtils;

import java.lang.invoke.MethodHandles;

import org.springframework.security.core.context.SecurityContextHolder;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.process.signup.SignupPlaceHolder;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

public class InvoiceGenerationSignupStep extends AbstractSapphireSignupStep {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public InvoiceGenerationSignupStep(IWebServicesSessionBean service, IMethodTransactionalWrapper txAction, boolean useNewTx, boolean isAsync) {
        super(service, txAction, useNewTx, isAsync);
    }

    @Override
    public void doExecute(SignupPlaceHolder holder) {
        try {
            Integer userId = holder.getSignUpResponse().getUserId();
            Integer entityId = holder.getEntityId();
            UserDTO user = new UserDAS().findByUserId(userId, entityId);
            SpringSecurityUtils.reauthenticate(user.getUserName()+";"+entityId, null);

            logger.debug("Generating invoice for user {}", userId);
            Integer[] invoices = getService().createInvoice(userId, false);
            if(ArrayUtils.isNotEmpty(invoices)) {
                logger.debug("Invoice Generated for user {} is {}", userId, invoices);
            }
        } catch(Exception ex) {
            logger.error("Error in Invoice Generation", ex);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ;
        }finally {
            // logout user once api calls are done.
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

}
