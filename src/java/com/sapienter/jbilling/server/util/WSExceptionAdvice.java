/*
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

package com.sapienter.jbilling.server.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ThrowsAdvice;

import com.sapienter.jbilling.common.SessionInternalError;

/**
 * Re-throws any exceptions from the API as SessionInternalErrors to
 * prevent server exception classes being required on the client.
 * Useful for remoting protocols such as Hessian which propagate the
 * exception stack trace from the server to the client.
 */
public class WSExceptionAdvice implements ThrowsAdvice {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void afterThrowing(Method method, Object[] args, Object target, Exception throwable) {
        throwException(method.getName(), throwable);
    }

    public void throwException(String methodName, Exception throwable) {
        // Avoid catching automatic validation exceptions
        String uuid = UUID.randomUUID().toString();
        String message = null;
        if (throwable instanceof SessionInternalError) {
            //someone explicitly throws SessionInternalError
            SessionInternalError sie = (SessionInternalError)throwable;
            message = "uuid=" + uuid + ", message=" + sie.getMessage();

            String[] messages = sie.getErrorMessages();
            if (ArrayUtils.isNotEmpty(messages)) {
                logger.debug("uuid={}, message=Validation Errors, errors= {}", uuid, Arrays.toString(messages));
            } else {
                logger.debug(message);
            }
        } else if (throwable instanceof SecurityException) {
            //someone explicitly throws SessionInternalError
            SecurityException sie = (SecurityException)throwable;
            message = "uuid=" + uuid + ", message" + sie.getMessage();
            logger.warn(message);
        } else {
            //unexpected exception happens
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            pw.close();
            message = throwable.getMessage();
            logger.debug("uuid={}, message={}, method={} \n {}", uuid, message, methodName, sw);

            message = "uuid=" + uuid + ", message=Error calling jBilling API, method=" + methodName;
        }

        //here we create a new exception and we are only including error information
        //for the exception. We are not giving away the original place from where the
        //exception was created since it can be viewed as security risk. It could
        //inadvertently reveal critical place in code, db table structure etc.
        //we are generating and giving away an UUID so that clients can gives a piece
        //if information that will help us track the original exception in our logs
        throw new SessionInternalError(message, throwable, uuid);
    }
}
