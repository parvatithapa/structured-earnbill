package com.sapienter.jbilling.common;

import java.lang.invoke.MethodHandles;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import com.sapienter.jbilling.server.util.WSExceptionAdvice;

@Aspect
public class ExceptionInterceptor implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private WSExceptionAdvice exceptionAdvice;

    @AfterThrowing(pointcut = "@annotation(handleException)", throwing = "error")
    public void afterThrowingAdvice(JoinPoint jp, Exception error, HandleException handleException) {
        logger.error("excpetion in method {}", jp.getSignature().getName(), error);
        exceptionAdvice.throwException(jp.getSignature().getName(), error);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
