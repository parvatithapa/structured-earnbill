package com.sapienter.jbilling.appdirect.subscription.http;

import com.sapienter.jbilling.appdirect.subscription.http.exception.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;


public class ResponseErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.UNAUTHORIZED) {
            throw new UnauthorizedException("Unauthorized request, check credentials");
        }
        if (status == HttpStatus.NOT_FOUND) {
            return;
        }
        super.handleError(response);
    }
}
