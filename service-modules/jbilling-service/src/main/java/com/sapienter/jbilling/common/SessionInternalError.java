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

package com.sapienter.jbilling.common;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.ws.WebFault;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.CommonConstants;

@WebFault(name = "SessionInternalError", targetNamespace = "http://jbilling/")
public class SessionInternalError extends RuntimeException {

    protected SessionInternalErrorMessages sessionInternalErrorMessages = new SessionInternalErrorMessages();
    protected ErrorDetails errorDetails;

    public SessionInternalError() {
        this((String)null);
    }

    public SessionInternalError(String s) {
        this(s, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    public SessionInternalError(String s, int errorCode) {
        super(s);
        errorDetails = ErrorDetails.newInstance(null, null, errorCode);
    }

    public SessionInternalError(String s, Class className, Exception e) {
        this(s, className, e, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
    public SessionInternalError(String s, Class className, Exception e, int errorCode) {
        super(e);
        if(e instanceof SessionInternalError) {
            SessionInternalError internal = (SessionInternalError) e;
            errorDetails = ErrorDetails.newInstance(null, internal.getErrorMessages(),
                    internal.getErrorCode(), internal.getParams());
        } else {
            errorDetails = ErrorDetails.newInstance(null, null, errorCode);
        }
        FormatLogger log = new FormatLogger(Logger.getLogger(className));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();

        log.fatal("Internal error: " + (e.getMessage() == null ? "" : e.getMessage().replaceAll("%", "_")) + "\n" + sw.toString().replaceAll("%", "_"));

    }

    public SessionInternalError(Exception e) {
        this(e, null);
    }

    public SessionInternalError(Exception e, String[] errorMessages) {
        this(e, errorMessages, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    public SessionInternalError(Exception e, String[] errorMessages, int errorCode) {
        super(e);
        if(e instanceof SessionInternalError){
            SessionInternalError internal = (SessionInternalError) e;
            errorDetails = ErrorDetails.newInstance(null, internal.getErrorMessages(),
                    internal.getErrorCode(), internal.getParams());
        } else {
            errorDetails = ErrorDetails.newInstance(null, errorMessages, errorCode);
        }

        FormatLogger log = new FormatLogger(Logger.getLogger("com.sapienter.jbilling"));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        log.fatal("Internal error: " + (e.getMessage() == null ? "" : e.getMessage().replaceAll("%", "_")) + "\n" + sw.toString().replaceAll("%", "_"));
    }

    public SessionInternalError(Exception e, int errorCode){
        super(e);
        if(e instanceof SessionInternalError){
            SessionInternalError internal = (SessionInternalError) e;
            errorDetails = ErrorDetails.newInstance(null, internal.getErrorMessages(),
                    errorCode, internal.getParams());
        } else {
            errorDetails = ErrorDetails.newInstance(null, null, errorCode);
        }
        FormatLogger log = new FormatLogger(Logger.getLogger("com.sapienter.jbilling"));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        log.fatal("Internal error: " + (e.getMessage() == null ? "" : e.getMessage().replaceAll("%", "_")) + "\n" + sw.toString().replaceAll("%", "_"));
    }

    public SessionInternalError(String message, Throwable e, String uuid) {
        super(message);

        copyErrorInformation(e, uuid);

        FormatLogger log = new FormatLogger(Logger.getLogger("com.sapienter.jbilling"));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        log.fatal("Internal error %s \n %s ", (e.getMessage() == null ? "" : e.getMessage().replaceAll("%", "_")), sw.toString().replaceAll("%", "_"));
    }

    public SessionInternalError(String message, Throwable e) {
        super(message + " Cause: " + e.getMessage(), e);
        errorDetails = ErrorDetails.newInstance(null, null);
    }

    public SessionInternalError(String message, Throwable e, String[] errors) {
        super(message + getErrorsAsString(errors), e);
        errorDetails = ErrorDetails.newInstance(null, errors);
    }

    public SessionInternalError(String message, String[] errors) {
        super(message + getErrorsAsString(errors));
        errorDetails = ErrorDetails.newInstance(null, errors);
    }

    public SessionInternalError(String[] errors) {
        super(getErrorsAsString(errors));
        errorDetails = ErrorDetails.newInstance(null, errors);
    }

    public SessionInternalError(String message, String[] errors, int errorCode) {
        super(message + getErrorsAsString(errors));
        errorDetails = ErrorDetails.newInstance(null, errors, errorCode);
    }

    public SessionInternalError(String message, String errors, int errorCode) {
        this(message, errors.split(","), errorCode);
    }

    public SessionInternalError(String message, String[] errors, String[] params) {
        super(message + getErrorsAsString(errors));
        errorDetails = ErrorDetails.newInstance(null, errors, HttpStatus.SC_INTERNAL_SERVER_ERROR, params);
    }

    public SessionInternalError(String message, String[] errors, String[] params, int errorCode) {
        super(message + getErrorsAsString(errors));
        errorDetails = ErrorDetails.newInstance(null, errors, errorCode, params);
    }

    private static String getErrorsAsString(String[] errors){
        StringBuilder builder = new StringBuilder();
        if (errors != null) {
            builder.append(". Errors: ");
            for (String error : errors) {
                builder.append(error);
                builder.append(System.getProperty("line.separator"));
            }
        }
        return builder.toString();
    }

    public String[] getErrorMessages() {
        return this.errorDetails.getErrorMessages();
    }

    /*
    CXF uses this method because of @WebFault annotation
     */
    public SessionInternalErrorMessages getFaultInfo() {
        return this.errorDetails.getSessionInternalErrorMessages();
    }

    public String[] getParams() {
        return this.errorDetails.getParams();
    }

    public boolean hasParams() {
        return getParams() != null && getParams().length > 0;
    }

    public String getUuid() {
        return this.getUuid();
    }

    public int getErrorCode() {
        return this.errorDetails.getErrorCode();
    }

    public ErrorDetails getErrorDetails() {
        return null != errorDetails ? ErrorDetails.copyOf(errorDetails) :
            null;
    }

    private void copyErrorInformation(Throwable throwable, String uuid) {
        if(throwable instanceof SessionInternalError){
            SessionInternalError internal = (SessionInternalError) throwable;
            errorDetails = ErrorDetails.newInstance(uuid, internal.getErrorMessages(),
                    internal.getErrorCode(), internal.getParams());
        } else if(throwable instanceof SecurityException){
        	SecurityException internal = (SecurityException) throwable;
            errorDetails = ErrorDetails.newInstance(uuid, new String []{throwable.getMessage()} , CommonConstants.ERROR_CODE_UNAUTHORIZED_DATA_ACCESS);
        }else {
            errorDetails = ErrorDetails.newInstance(uuid, null);
        }
    }

}
