package com.sapienter.jbilling.utils

import com.sapienter.jbilling.common.SessionInternalError

import org.apache.http.HttpStatus

import javax.ws.rs.core.Response

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.ErrorDetails

import java.lang.SecurityException

import com.sapienter.jbilling.server.payment.SecurePaymentWS;

/**
 * @author Vojislav Stanojevikj
 * @since 23-Aug-2016.
 */
final class RestErrorHandler {
	
	public static final String ERROR_MSG_NO_DATA_FOUND = "No data found.";
	
	private RestErrorHandler(){}

    /**
     *
     * Utility method that can be used to map a specific
     * <code>error</code> with a error code into a Http response.
     *
     * @param error {@link SessionInternalError}
     * @return {@link Response} http response
     * @throws IllegalArgumentException if the <code>error</code> does not contain error code or the code is less than {@code 400} or greater than {@code 507}.
     * @see RestErrorHandler#hasErrorStatusCode(com.sapienter.jbilling.common.SessionInternalError)
     */
    public static Response mapErrorToHttpResponse(Exception error){

        if (error instanceof SessionInternalError){
            if (!hasErrorStatusCode(error)){
                throw new IllegalArgumentException("Status code is not a error code!")
            }
            return Response.status(error.errorCode).entity(error.errorDetails).build()
        } else if(error instanceof java.lang.SecurityException ){
			String[] errorMessages = [ error.getMessage().contains("Unauthorized") ?  ERROR_MSG_NO_DATA_FOUND : error.getMessage()]
			ErrorDetails errorDetails =  ErrorDetails.newInstance(null, errorMessages , Response.Status.NOT_FOUND.getStatusCode())
			return Response.status(Response.Status.NOT_FOUND.getStatusCode()).entity(errorDetails).build()
		}
		
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
    }

    /**
     *
     * Simple check if the supplied <code>error</code>
     * has error code inside.
     *
     * @param error {@link SessionInternalError}
     * @return true or false
     */
    public static boolean hasErrorStatusCode(SessionInternalError error){

        return error && error.errorCode &&
                error.errorCode >= HttpStatus.SC_BAD_REQUEST &&
                error.errorCode <= HttpStatus.SC_INSUFFICIENT_STORAGE
    }
	
	/** 
	 * Return appropriate response status
	 * @param securePaymentWS
	 * @return
	 */
	public static Response mapStatusToHttpResponse(SecurePaymentWS  securePaymentWS){
		if (securePaymentWS!=null && securePaymentWS.isFailed()){
			return Response.status(402).entity(securePaymentWS).build();
		}
		return Response.ok().entity(securePaymentWS).build();
	}
}