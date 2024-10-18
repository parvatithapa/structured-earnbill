package com.sapienter.jbilling.log;

import org.apache.log4j.MDC;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

/**
 * Created by nenad on 10/19/16.
 */
public class LoggerAspect implements MethodBeforeAdvice, AfterReturningAdvice {

    private static final String API_METHOD_KEY = "apiMethod";

    @Override
    public void before(Method method, Object[] objects, Object o) throws Throwable {
        MDC.put(API_METHOD_KEY, method.getName());
    }

    @Override
    public void afterReturning(Object o, Method method, Object[] objects, Object o1) throws Throwable {
        MDC.remove(API_METHOD_KEY);
    }
}
