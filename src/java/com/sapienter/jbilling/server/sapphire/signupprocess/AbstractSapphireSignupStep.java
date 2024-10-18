package com.sapienter.jbilling.server.sapphire.signupprocess;

import java.lang.invoke.MethodHandles;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.process.signup.SignupPlaceHolder;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * Class contains settings of transactions and async executions.
 * @author Krunal Bhavsar
 *
 */
abstract class AbstractSapphireSignupStep implements ISapphireSignupStep {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private IWebServicesSessionBean service;
    private IMethodTransactionalWrapper txAction;
    private boolean useNewTx = true;
    private boolean isAsync;

    public AbstractSapphireSignupStep(IWebServicesSessionBean service, IMethodTransactionalWrapper txAction, boolean useNewTx, boolean isAsync) {
        this.service = service;
        this.txAction = txAction;
        this.useNewTx = useNewTx;
        this.isAsync = isAsync;
    }

    public IWebServicesSessionBean getService() {
        return service;
    }
    public IMethodTransactionalWrapper getTxAction() {
        return txAction;
    }

    /**
     * Flushes and clear session after execution of each sign up step.
     */
    private void flushAndClear() {
        SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
        Session session = sf.getCurrentSession();
        session.flush();
        session.clear();
    }

    @Override
    public void executeStep(SignupPlaceHolder holder) {
        try {
            if(isAsync) { //  Asycn execution of method will use new tx different from calling thread tx, So no need to check for userNewTx field.
                txAction.executeAsync(()-> doExecute(holder));
            } else {
                if(useNewTx) {
                    txAction.executeInNewTransaction(()-> {
                        doExecute(holder);
                        flushAndClear();
                    });
                } else {
                    txAction.execute(()-> {
                        doExecute(holder);
                        flushAndClear();
                    });
                }
            }
        } catch(Exception ex) {
            logger.error("Error in executeStep!", ex);
            throw new SessionInternalError(ex);
        }
    }

    public abstract void doExecute(SignupPlaceHolder holder);

}
