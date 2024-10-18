package com.sapienter.jbilling.saml;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.client.authentication.util.SecuritySession;
import com.sapienter.jbilling.client.authentication.util.UsernameHelper;
import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by faizan on 2/15/17.
 */
public class SamlAuthenticationFilter extends SAMLProcessingFilter {
    private static final Logger LOG = Logger.getLogger(SamlAuthenticationFilter.class);
    public static final String FORM_CLIENT_ID_KEY = "j_client_id";
    private String usernameParameter = "j_username";
    private String passwordParameter = "j_password";
    private String clientIdParameter;
    private SecuritySession securitySession;
    private RegisterSessionAuthenticationStrategy registerSessionAuthenticationStrategy;

    public RegisterSessionAuthenticationStrategy getRegisterSessionAuthenticationStrategy() {
        return registerSessionAuthenticationStrategy;
    }

    public void setRegisterSessionAuthenticationStrategy(RegisterSessionAuthenticationStrategy registerSessionAuthenticationStrategy) {
        this.registerSessionAuthenticationStrategy = registerSessionAuthenticationStrategy;
    }


    public SamlAuthenticationFilter() {
    }

    public final String getClientIdParameter() {
        return this.clientIdParameter == null?"j_client_id":this.clientIdParameter;
    }

    public void setClientIdParameter(String clientIdParameter) {
        this.clientIdParameter = clientIdParameter;
    }

    public SecuritySession getSecuritySession() {
        return this.securitySession;
    }

    public void setSecuritySession(SecuritySession securitySession) {
        this.securitySession = securitySession;
    }

    protected String obtainPassword(HttpServletRequest request) {
        return request.getParameter(this.passwordParameter);
    }

    public void setUsernameParameter(String usernameParameter) {
        Assert.hasText(usernameParameter, "Username parameter must not be empty or null");
        this.usernameParameter = usernameParameter;
    }

    public void setPasswordParameter(String passwordParameter) {
        Assert.hasText(passwordParameter, "Password parameter must not be empty or null");
        this.passwordParameter = passwordParameter;
    }

    public final String getUsernameParameter() {
        return this.usernameParameter;
    }

    public final String getPasswordParameter() {
        return this.passwordParameter;
    }

    protected String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter(this.getUsernameParameter());
        String companyId = request.getParameter(this.getClientIdParameter());
        return UsernameHelper.buildUsernameToken(username, companyId);
    }

    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication result) throws IOException, ServletException {
        if(this.securitySession != null) {
            this.securitySession.setAttributes(request, response, (CompanyUserDetails)result.getPrincipal());
            registerSessionAuthenticationStrategy.onAuthentication(result,request,response);
        }

        super.successfulAuthentication(request, response, result);
    }

    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        LOG.debug("User " + failed.getAuthentication().getPrincipal() + " authentication failed!");
        if(this.securitySession != null) {
            this.securitySession.clearAttributes(request, response);
        }

        super.unsuccessfulAuthentication(request, response, failed);
    }
}
