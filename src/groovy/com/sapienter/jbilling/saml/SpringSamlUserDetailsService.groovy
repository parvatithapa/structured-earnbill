package com.sapienter.jbilling.saml

import com.sapienter.jbilling.client.authentication.CompanyUserDetails
import com.sapienter.jbilling.client.authentication.JBillingPasswordEncoder
import com.sapienter.jbilling.client.authentication.UserService
import com.sapienter.jbilling.client.authentication.model.EncryptedLicense
import com.sapienter.jbilling.client.authentication.model.User
import com.sapienter.jbilling.client.authentication.util.UsernameHelper
import com.sapienter.jbilling.client.util.Constants
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.license.LicenseManager
import com.sapienter.jbilling.server.metafields.DataType
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.user.*
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import grails.plugin.springsecurity.userdetails.GormUserDetailsService
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.saml.SAMLCredential
import org.springframework.security.saml.userdetails.SAMLUserDetailsService

/**
 * A {@link GormUserDetailsService} extension to read attributes from a LDAP-backed 
 * SAML identity provider. It also reads roles from database
 *
 * @author Aamir Ali
 */
@SuppressWarnings("deprecation")
class SpringSamlUserDetailsService extends GormUserDetailsService implements SAMLUserDetailsService {
    // Spring bean injected configuration parameters
    Map samlUserAttributeMappings
    IWebServicesSessionBean iWebServicesSessionBean = (IWebServicesSessionBean) Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    IUserSessionBean userSessionBean = (IUserSessionBean) Context.getBean(Context.Name.USER_SESSION);
    private UserService userService;

    public UserService getUserService() {
        return this.userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {

        if (credential) {
            String username = getSamlUsername(credential)
            if (!username) {
                throw new UsernameNotFoundException("No username supplied in saml response.")
            }
            String accountIdentifier = getAttributeValue(credential, samlUserAttributeMappings.entityId);
            Integer entityId = null != accountIdentifier ? Integer.parseInt(accountIdentifier) : null;

            if (null != entityId) {
                boolean justInTime = SamlUtil.getDefaultJITConfig(entityId);
                EncryptedLicense license = this.getUserService().getLicense(entityId);
                LicenseManager.validate(license.getLicenseKey(), license.getLicenseeName());

                User user = this.getUserService().getUser(username, entityId);
                if (!user && justInTime) {
                    try {
                        Integer roleId = SamlUtil.getDefaultIdpRole(entityId);
                        createUser(credential, username, entityId, roleId)
                    } catch (SessionInternalError e) {
                        return null;
                    }
                    user = this.getUserService().getUser(username, entityId);
                }

                if (user) {
                    log.debug "Loading database roles for $username..."
                    Collection authorities = this.userService.getAuthorities(username, entityId);
                    String usernameToken = UsernameHelper.buildUsernameToken(username, entityId);
                    return new CompanyUserDetails(usernameToken, user.getPassword(), user.isEnabled(), !user.isAccountExpired(), !user.isCredentialsExpired(), !user.isAccountLocked(), authorities, user.getLocale(), user.getId(), user.getMainRoleId(), user.getCompanyId(), user.getCurrencyId(), user.getLanguageId());
                } else {
                    throw new InsufficientAuthenticationException('Could not instantiate new user')
                }
            } else {
                throw new Exception('Could not instantiate new user')
            }
        }
    }

    private void createUser(SAMLCredential credential, String userName, Integer entityId, Integer roleId) {
        UserWS newUser = new UserWS();
        String randPassword = UserBL.generatePCICompliantPassword();
        JBillingPasswordEncoder passwordEncoder = new JBillingPasswordEncoder();
        newUser.setUserName(userName);
        newUser.setPassword(passwordEncoder.encodePassword(randPassword, null));
        newUser.setLanguageId(CommonConstants.JIT_USER_LANGUAGE_ID);
        newUser.setMainRoleId(roleId);
        newUser.setIsParent(CommonConstants.JIT_USER_DEFAULT_FALSE);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setEntityId(entityId);
        newUser.setDeleted(CommonConstants.JIT_USER_DELETED);
        String companyName = iWebServicesSessionBean.getCompanyByEntityId(entityId)?.getDescription();
        newUser.setCompanyName(null != companyName ? companyName : CommonConstants.JIT_USER_COMPANY_NAME);
        newUser.setCreateDatetime(new Date());
        newUser.setCurrencyId(CommonConstants.JIT_USER_CURRENCY_ID);
        newUser.setExcludeAgeing(CommonConstants.JIT_USER_DEFAULT_FALSE);
        newUser.setFailedAttempts(CommonConstants.JIT_USER_FAILED_ATTEMPTS);
        newUser.setIsAccountLocked(CommonConstants.JIT_USER_DEFAULT_FALSE);

        // add a contact
        ContactWS contact = new ContactWS();
        contact.setEmail(userName);
        String firstName = getAttributeValue(credential, samlUserAttributeMappings.firstName);
        contact.setFirstName(null != firstName ? firstName : CommonConstants.JIT_USER_FIRST_NAME);

        String lastName = getAttributeValue(credential, samlUserAttributeMappings.lastName);
        contact.setLastName(null != lastName ? lastName : CommonConstants.JIT_USER_LAST_NAME);
        contact.setOrganizationName(null != companyName ? companyName : CommonConstants.JIT_USER_COMPANY_NAME);
        contact.setAddress1(CommonConstants.JIT_USER_FIRST_ADDRESS1);
        contact.setAddress2(CommonConstants.JIT_USER_LAST_ADDRESS2);
        contact.setCity(CommonConstants.JIT_USER_CITY);
        contact.setStateProvince(CommonConstants.JIT_USER_STATE);
        contact.setPostalCode(CommonConstants.JIT_USER_POSTAL_CODE);
        contact.setCountryCode(CommonConstants.JIT_USER_COUNTRY_CODE);
        contact.setInclude(CommonConstants.JIT_USER_DEFAULT_TRUE);
        newUser.setContact(contact);

        Integer idpGroupId = SamlUtil.getDefaultIdpViaIdpEntityURL(entityId, credential.getRemoteEntityID());
        String entityType = EntityType.USER.toString()
        if (roleId == CommonConstants.TYPE_PARTNER) {
            entityType = EntityType.AGENT.toString();
        } else if (roleId == CommonConstants.TYPE_CUSTOMER) {
            entityType = EntityType.CUSTOMER.toString();
        }

        MetaFieldWS[] metaField = iWebServicesSessionBean.getMetaFieldsByEntityId(entityId, entityType)
        if (null != metaField && metaField.length > 0) {
            MetaFieldValueWS ssoEnabledMetaFieldValue = null;
            MetaFieldValueWS ssoIdpIdMetaFieldValue = null;
            MetaFieldValueWS ssoIdpAppDirectUuidMetaFieldValue = null;
            for (int i = 0; i < metaField.length; i++) {
                if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_ENABLED_USER)) {
                    ssoEnabledMetaFieldValue = new MetaFieldValueWS(Constants.SSO_ENABLED_USER, null, DataType.BOOLEAN, true, true);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_ENABLED_AGENT)) {
                    ssoEnabledMetaFieldValue = new MetaFieldValueWS(Constants.SSO_ENABLED_AGENT, null, DataType.BOOLEAN, true, true);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_ENABLED_CUSTOMER)) {
                    ssoEnabledMetaFieldValue = new MetaFieldValueWS(Constants.SSO_ENABLED_CUSTOMER, null, DataType.BOOLEAN, true, true);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_ID_USER)) {
                    ssoIdpIdMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_ID_USER, null, DataType.INTEGER, false, idpGroupId);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_ID_AGENT)) {
                    ssoIdpIdMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_ID_AGENT, null, DataType.INTEGER, false, idpGroupId);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_ID_CUSTOMER)) {
                    ssoIdpIdMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_ID_CUSTOMER, null, DataType.INTEGER, false, idpGroupId);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_APPDIRECT_UUID_USER)) {
                    ssoIdpAppDirectUuidMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_APPDIRECT_UUID_USER, null, DataType.STRING, false, credential.getNameID().getValue());
                }
            }

            newUser.setMetaFields([ssoEnabledMetaFieldValue, ssoIdpIdMetaFieldValue, ssoIdpAppDirectUuidMetaFieldValue] as MetaFieldValueWS[])
        }

        System.out.println("Creating just in time user ...");
        // do the creation
        try {
            Integer userId = iWebServicesSessionBean.createUser(newUser);
        } catch (SessionInternalError e) {
            throw new SessionInternalError(e)
        }
    }

    protected String getSamlUsername(credential) {
        if (samlUserAttributeMappings?.email) {
            def attribute = credential.getAttribute(samlUserAttributeMappings.email)
            def value = attribute?.attributeValues?.value
            return value?.first()
        } else {
            // if no mapping provided for username attribute then assume it is the returned subject in the assertion
            return credential.nameID?.value
        }
    }

    protected String getAttributeValue(SAMLCredential credential, String attributeName) {
        if (attributeName) {
            def attribute = credential.getAttribute(attributeName)
            def value = attribute?.attributeValues?.value
            return value?.first()
        } else {
            // if no mapping provided for attributeName then return null
            return null
        }
    }

    protected Object mapAdditionalAttributes(credential, user) {
        samlUserAttributeMappings.each { key, value ->
            def attribute = credential.getAttributeByName(value)
            def samlValue = attribute?.attributeValues?.value
            if (samlValue) {
                user."$key" = samlValue?.first()
            }
        }
        user
    }
}
