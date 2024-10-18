package com.sapienter.jbilling.server.integration.common.appdirect.client;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.sapienter.jbilling.appdirect.vo.BillingAPIResult;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.FreeSubscriptionExpiredException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.NetworkTimeoutException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.RemoteServerException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.SubscriptionNotFoundException;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.SubscriptionUsageNotAllowed;
import com.sapienter.jbilling.server.integration.common.appdirect.client.exception.UnAuthorizedTransientException;


/**
 * Created by tarun.rathor on 2/15/18.
 */
public class ClientResponseErrorHandler implements org.springframework.web.client.ResponseErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatus status = response.getStatusCode();
        return status.is4xxClientError() || status.is5xxServerError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus status = response.getStatusCode();
        // Could a be transient OAuth 1.0 issue
        if (status == HttpStatus.UNAUTHORIZED) {
            throw new UnAuthorizedTransientException(response.getStatusText());
        }
        String body = IOUtils.toString(response.getBody());
        body = body.toLowerCase();

        // Log the response and status for failures
        if (!(status == HttpStatus.OK || status == HttpStatus.ACCEPTED)) {
            logger.warn("HTTP Status={}", response.getStatusText());
            logger.warn("Response body={}", body);
        }

        BillingAPIResult billingAPIResult = null;

        Gson gson = new GsonBuilder().create();
        try {
            billingAPIResult = gson.fromJson(body, BillingAPIResult.class);
        } catch (JsonParseException e) {
            billingAPIResult = null;
        }

        // If the API returns failure then parse the message and classify failure modes
        if (billingAPIResult != null && !billingAPIResult.isSuccess()) {
            String message = billingAPIResult.getMessage();
            if(StringUtils.isEmpty(message)) {
                //throw generic exception// retry
            }
            message = message.toLowerCase();
            // Does it match the message 'The free trial has expired for this order'
            if(status ==  HttpStatus.INTERNAL_SERVER_ERROR &&
                        message.contains("expired") &&
                        message.contains("free") &&
                        message.contains("trial")) {
                throw new FreeSubscriptionExpiredException(message);
            // Does it match the message 'Metered usage report is not allowed on this entitlement'
            } else if(status == HttpStatus.BAD_REQUEST &&
                        message.contains("not") &&
                        message.contains("allowed") &&
                        message.contains("entitlement")) {
                throw new SubscriptionUsageNotAllowed(message);
            }
        } else {
            if (status == HttpStatus.NOT_FOUND &&
                        body.contains("not") &&
                        body.contains("found")) {
                    throw new SubscriptionNotFoundException("Subscription not found on marketplace");

            } else if (status == HttpStatus.REQUEST_TIMEOUT ||
                    status == HttpStatus.GATEWAY_TIMEOUT ||
                    status == HttpStatus.SERVICE_UNAVAILABLE) {
                throw new NetworkTimeoutException(response.getStatusText());

            } else if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
                throw new RemoteServerException(response.getStatusText());

            } else {
                throw new RestClientException(response.getStatusText());
            }
        }
    }
}
