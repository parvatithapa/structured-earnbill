package com.sapienter.jbilling.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class TestLogListener implements IInvokedMethodListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void beforeInvocation (IInvokedMethod method, ITestResult testResult) {
        if (! method.isTestMethod()) {
            return;
        }
        ITestNGMethod testNGMethod = method.getTestMethod();
        method.getTestMethod().setDescription(testNGMethod.getMethodName());
        logger.info("Starting  test: {}", methodName(testNGMethod));
    }

    public void afterInvocation (IInvokedMethod method, ITestResult testResult) {
        if (! method.isTestMethod()) {
            return;
        }
        logger.info("Finishing test: {}", methodName(method.getTestMethod()));
    }

    private String methodName (ITestNGMethod method) {
        return method.getRealClass().getSimpleName() + "." + method.getMethodName();
    }
}
