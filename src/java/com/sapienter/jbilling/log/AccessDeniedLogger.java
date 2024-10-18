package com.sapienter.jbilling.log;

import org.apache.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by nenad on 10/24/16.
 */
public class AccessDeniedLogger extends AccessDeniedHandlerImpl {

    private static final Logger log = Logger.getLogger(AccessDeniedLogger.class);

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
        log.error("Access denied for URI - " + httpServletRequest.getRequestURI());
        super.handle(httpServletRequest, httpServletResponse, e);
    }
}
