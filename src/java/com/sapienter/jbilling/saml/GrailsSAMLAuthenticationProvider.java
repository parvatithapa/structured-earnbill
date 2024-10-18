package com.sapienter.jbilling.saml;

import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLRuntimeException;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLAuthenticationToken;
import org.springframework.security.saml.SAMLConstants;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.context.SAMLMessageContext;

import java.util.Collection;
import java.util.Date;

/**
 * A {@link org.springframework.security.saml.SAMLAuthenticationProvider} subclass to return
 * principal as UserDetails Object.
 *
 * @author feroz.panwaskar
 */
public class GrailsSAMLAuthenticationProvider extends SAMLAuthenticationProvider {

    private final static Logger log = LoggerFactory.getLogger(SAMLAuthenticationProvider.class);
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    /**
     * @param credential credential used to authenticate user
     * @param userDetail loaded user details, can be null
     * @return principal to store inside Authentication object
     */
    @Override
    protected Object getPrincipal(SAMLCredential credential, Object userDetail) {
        if (userDetail != null) {
            return userDetail;
        }

        return credential.getNameID().getValue();
    }

    /**
     * Attempts to perform authentication of an Authentication object. The authentication must be of type
     * SAMLAuthenticationToken and must contain filled SAMLMessageContext. If the SAML inbound message
     * in the context is valid, UsernamePasswordAuthenticationToken with name given in the SAML message NameID
     * and assertion used to verify the user as credential (SAMLCredential object) is created and set as authenticated.
     *
     * @param authentication SAMLAuthenticationToken to verify
     * @return UsernamePasswordAuthenticationToken with name as NameID value and SAMLCredential as credential object
     * @throws AuthenticationException user can't be authenticated due to an error
     */
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (!supports(authentication.getClass())) {
            throw new IllegalArgumentException("Only SAMLAuthenticationToken is supported, " + authentication.getClass() + " was attempted");
        }

        SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
        SAMLMessageContext context = token.getCredentials();

        if (context == null) {
            throw new AuthenticationServiceException("SAML message context is not available in the authentication token");
        }

        SAMLCredential credential;

        try {
            if (SAMLConstants.SAML2_WEBSSO_PROFILE_URI.equals(context.getCommunicationProfileId())) {
                credential = consumer.processAuthenticationResponse(context);
            } else if (SAMLConstants.SAML2_HOK_WEBSSO_PROFILE_URI.equals(context.getCommunicationProfileId())) {
                credential = hokConsumer.processAuthenticationResponse(context);
            } else {
                throw new SAMLException("Unsupported profile encountered in the context " + context.getCommunicationProfileId());
            }
        } catch (SAMLRuntimeException e) {
            log.debug("Error validating SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message", e);
        } catch (SAMLException e) {
            log.debug("Error validating SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message", e);
        } catch (ValidationException e) {
            log.debug("Error validating signature", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message signature", e);
        } catch (org.opensaml.xml.security.SecurityException e) {
            log.debug("Error validating signature", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message signature", e);
        } catch (DecryptionException e) {
            log.debug("Error decrypting SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error decrypting SAML message", e);
        }
        Object userDetails = null;
        try {
            userDetails = getUserDetails(credential);
            if (null == userDetails) {
                log.debug("Error occurred while user account creation.");
                throw new InstantiationException("Error occurred while user account creation.");
            }
        } catch (InsufficientAuthenticationException e) {
            log.debug("Your account not found and just in time creation is not enabled for company.");
            throw new InsufficientAuthenticationException(messages.getMessage("AbstractAccessDecisionManager.accessDenied",
                    "User account not found"));
        } catch (Exception e) {
            log.debug("We were not able to find a user with that login id and password for the selected company.");
            try {
                throw new Exception("We were not able to find a user with that login id and password for the selected company.");
            } catch (Exception e1) {
                log.debug("We were not able to find a user with that login id and password for the selected company.");
            }
        }

        if (null != userDetails && !((UserDetails) userDetails).isAccountNonLocked()) {
            log.debug("User account is locked");

            throw new LockedException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.locked",
                    "User account is locked"));
        }

        if (null != userDetails && !((UserDetails) userDetails).isEnabled()) {
            log.debug("User account is disabled");

            throw new DisabledException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.disabled",
                    "User is disabled"));
        }

        if (null != userDetails && !((UserDetails) userDetails).isAccountNonExpired()) {
            log.debug("User account is expired");

            throw new AccountExpiredException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.expired",
                    "User account has expired"));
        }
        Object principal = getPrincipal(credential, userDetails);
        Collection<? extends GrantedAuthority> entitlements = getEntitlements(credential, userDetails);

        Date expiration = getExpirationDate(credential);

        SAMLCredential authenticationCredential = isExcludeCredential() ? null : credential;
        ExpiringUsernameAuthenticationToken result = new ExpiringUsernameAuthenticationToken(expiration, principal, authenticationCredential, entitlements);
        result.setDetails(userDetails);

        samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.SUCCESS, context, result, null);

        return result;

    }
}
