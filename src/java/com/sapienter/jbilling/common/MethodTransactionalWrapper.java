package com.sapienter.jbilling.common;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Krunal Bhavsar
 *
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
public class MethodTransactionalWrapper implements IMethodTransactionalWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(MethodTransactionalWrapper.class);
    private static final String ERROR_MESSAGE = "Error ";

    /**
     * Method wraps given action in new transaction
     * @param action
     * @return
     */
    @Override
    public <T> T execute(Callable<T> action) {
        try {
            return action.call();
        } catch(Exception ex) {
            LOG.error(ERROR_MESSAGE, ex);
            throw new SessionInternalError(ex);
        }
    }


    /**
     * Method wraps given action in new transaction
     * @param action
     * @return
     */
    @Override
    public void execute(Runnable action) {
        try {
            action.run();
        } catch(Exception ex) {
            LOG.error(ERROR_MESSAGE, ex);
            throw new SessionInternalError(ex);
        }
    }

    /**
     * Method wraps given action in new transaction
     * @param action
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, value = "transactionManager")
    public <T> T executeInNewTransaction(Callable<T> action) {
        try {
            return action.call();
        } catch(Exception ex) {
            LOG.error(ERROR_MESSAGE, ex);
            throw new SessionInternalError(ex);
        }
    }


    /**
     * Method wraps given action in new transaction
     * @param action
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, value = "transactionManager")
    public void executeInNewTransaction(Runnable action) {
        try {
            action.run();
        } catch(Exception ex) {
            LOG.error(ERROR_MESSAGE, ex);
            throw new SessionInternalError(ex);
        }
    }


    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, value = "transactionManager")
    public <T> T executeInReadOnlyTx(Callable<T> action) {
        try {
            return action.call();
        } catch(Exception ex) {
            LOG.error(ERROR_MESSAGE, ex);
            throw new SessionInternalError(ex);
        }
    }


    @Override
    @Async("asyncTaskExecutor")
    public void executeAsync(Runnable action) {
        try {
            action.run();
        } catch(Exception ex) {
            LOG.error(ERROR_MESSAGE, ex);
            throw new SessionInternalError(ex);
        }

    }
}
