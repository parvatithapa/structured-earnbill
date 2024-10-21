package com.sapienter.jbilling.auth;


import grails.plugin.springsecurity.SpringSecurityUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component("jwtAuthenticationFilter")
class JwtAuthenticationFilter extends AbstarctFilter {

    private static final String AUTHORIZATION_HEADER = "X-BH-Authorization";
    private static final String AUTHORIZATION_TOKEN_PREFIX = "Bearer ";

    @Resource
    private RememberMeServices rememberMeServices;
    @Resource
    private JwtAuthenticationService jwtAuthenticationService;

    @Override
    public void afterPropertiesSet() {
        if (null == rememberMeServices) {
            rememberMeServices = new NullRememberMeServices();
        }
        Assert.notNull(this.getAuthenticationEntryPoint(), "An AuthenticationEntryPoint is required");
    }

    private String extractToken(String token) {
        if (token.startsWith(AUTHORIZATION_TOKEN_PREFIX)) {
            token = token.substring(AUTHORIZATION_TOKEN_PREFIX.length());
        }
        return token;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        boolean debug = this.logger.isDebugEnabled();
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(header)) {
            try {
                String token = extractToken(header);
                Assert.hasLength(token, "token is required");
                // verifying token
                JwtDecodedTokenInfoWS verifiedToken = jwtAuthenticationService.verifyToken(new TokenVerificationRequestWS(token));
                if (verifiedToken.getStatus().equals(StatusWS.INVALID)) {
                    throw new BadCredentialsException(verifiedToken.getErrorMessage());
                }
                String username = (String) verifiedToken.getClaims().get("name");
                Integer entityId = (Integer) verifiedToken.getClaims().get("entityId");
                if (authenticationIsRequired(username)) {
                    // re authenticating user.
                    SpringSecurityUtils.reauthenticate(username + ";" + entityId, StringUtils.EMPTY);
                }
            } catch (AuthenticationException authenticationException) {
                SecurityContextHolder.clearContext();
                if (debug) {
                    this.logger.debug("Authentication request failed: " + authenticationException);
                }
                this.rememberMeServices.loginFail(request, response);
                getAuthenticationEntryPoint().commence(request, response, authenticationException);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}