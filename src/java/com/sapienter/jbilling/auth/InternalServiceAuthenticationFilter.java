package com.sapienter.jbilling.auth;

import grails.plugin.springsecurity.SpringSecurityUtils;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Base64;

@Component("internalServiceAuthenticationFilter")
class InternalServiceAuthenticationFilter extends AbstarctFilter {

    private static final String AUTHORIZATION_HEADER = "X-Internal-Service-Authorization";
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    @Override
    public void afterPropertiesSet() {
        Assert.notNull(this.getAuthenticationEntryPoint(), "An AuthenticationEntryPoint is required");
    }

    @ToString
    class CredContainer {
        private Integer userId;
        private Integer entityId;
        private String serviceName;
    }

    /**
     * Extracts userId and entityId
     *
     * @param token
     * @return
     */
    private CredContainer extractUserAndEntityId(String token) {
        try {
            token = new String(Base64.getDecoder().decode(token));
            String[] creds = token.split(",");
            if (3 != creds.length) {
                throw new BadCredentialsException("invalid token passed");
            }
            CredContainer credContainer = new CredContainer();
            credContainer.userId = Integer.parseInt(creds[0]);
            credContainer.entityId = Integer.parseInt(creds[1]);
            credContainer.serviceName = creds[2];
            return credContainer;
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new BadCredentialsException("invalid token passed");
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(token)) {
            try {
                CredContainer credContainer = extractUserAndEntityId(token);
                log.debug("autenticating with credentials={}", credContainer);
                Integer entityId = credContainer.entityId;
                //TODO check service config for entityId (create service registry or map for each entityId)
                String serviceName = credContainer.serviceName;
                String username = findUsernameByIdAndEntityId(credContainer.userId, entityId);
                if (authenticationIsRequired(username)) {
                    // re authenticating user.
                    SpringSecurityUtils.reauthenticate(username + ";" + entityId, StringUtils.EMPTY);
                    log.debug("userId={} authenticated for service={}, entityId="
                        + "{}", credContainer.userId, serviceName, entityId);
                }
            } catch (Exception exception) {
                SecurityContextHolder.clearContext();
                log.error("Authentication request failed", exception);
                if (exception instanceof AuthenticationException) {
                    getAuthenticationEntryPoint().commence(request, response, (AuthenticationException) exception);
                } else {
                    response.sendError(500, exception.getMessage());
                }
                return;
            }
        }
        filterChain.doFilter(req, res);
    }
}