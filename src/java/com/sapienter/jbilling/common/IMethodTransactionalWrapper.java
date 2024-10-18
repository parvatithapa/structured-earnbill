package com.sapienter.jbilling.common;

import java.util.concurrent.Callable;
/**
 *
 * @author Krunal Bhavsar
 *
 */
public interface IMethodTransactionalWrapper {

    <T> T execute(Callable<T> action);
    void execute(Runnable action);
    <T> T executeInNewTransaction(Callable<T> action);
    void executeInNewTransaction(Runnable action);

    <T> T executeInReadOnlyTx(Callable<T> action);

    void executeAsync(Runnable action);

}
