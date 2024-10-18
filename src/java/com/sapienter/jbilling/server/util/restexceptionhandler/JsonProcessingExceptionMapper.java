package com.sapienter.jbilling.server.util.restexceptionhandler;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sapienter.jbilling.common.ErrorDetails;

@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Response toResponse(JsonProcessingException exception) {
        ErrorDetails errorDetails = ErrorDetails.newInstance(UUID.randomUUID().toString(),
                new String[] { "Invalid JSON request passed", exception.getOriginalMessage() },
                Response.Status.BAD_REQUEST.getStatusCode());
        logger.error("message = {}, error", errorDetails.getUuid(), exception);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}