/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.client.authentication;

import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import grails.plugin.springsecurity.web.access.expression.WebExpressionConfigAttribute;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.FilterInvocation;

import java.util.Collection;

/**
 * A spring security {@link AccessDecisionVoter} implementation that handles individual user permission
 * access requests. User permission authorities are a uppercase string containing the permission type
 * and individual permission id (e.g., "WEB_SERVICES_120", "USER_CREATION_10").
 *
 * This voter abstains from un-supported access requests.
 *
 * @see com.sapienter.jbilling.server.user.UserBL#getPermissions()
 * @see com.sapienter.jbilling.server.user.permisson.db.PermissionDTO#getAuthority()
 *
 * @author Brian Cowdery
 * @since 04-10-2010
 */
public class PermissionVoter implements AccessDecisionVoter<Object> {

    protected static final String PERMISSION_ATTRIBUTE_REGEX = "[A-Z_]+\\d+";

    private String rolePrefix = RoleDTO.ROLE_AUTHORITY_PREFIX;

    private String requestUrlForMobileAPI = "";

    private String accessAttributeForMobileAPI = "";

    /**
     * Supports access decisions for permission authorities, where the string does not
     * start with the "ROLE_" authority prefix.
     *
     * @param attribute authority to check
     * @return true if supported, false if not
     */
    public boolean supports(ConfigAttribute attribute) {
        String value = getAttributeValue(attribute);
        return value != null && !value.startsWith(getRolePrefix()) && value.matches(PERMISSION_ATTRIBUTE_REGEX);
    }

    public boolean supports(Class<?> clazz) {
        return true;
    }

    public int vote(Authentication authentication, Object o, Collection<ConfigAttribute> attributes) {
        int result = ACCESS_ABSTAIN; // abstain unless we find a supported attribute
        String requestUrl = getRequestUrl(o);

        for (ConfigAttribute attribute : attributes) {
            if (this.supports(attribute)) {
                result = ACCESS_DENIED;

                // grant access only if the user has an authority matching the request
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String attributeValue = getAttributeValue(attribute);
                    if (attributeValue.equals(authority.getAuthority())) {
                        Integer accessGranted = validateAccess(requestUrl, attributeValue);
                        if (accessGranted != null) return accessGranted;
                    }
                }
            }
        }

        return result;
    }

    private Integer validateAccess(String requestUrl, String attributeValue) {
        if(StringUtils.isBlank(getAccessAttributeForMobileAPI()) || StringUtils.isBlank(getRequestUrlForMobileAPI())) {
            // if both the parameters are BLANK then ACCESS is GRANTED since Mobile APIs are not configured
            return ACCESS_GRANTED;
        }
        if(StringUtils.isNotBlank(requestUrl) && requestUrl.contains(getRequestUrlForMobileAPI())) {
            // if requestUrl is not BLANK and equals to the requestURL for Mobile APIs parameter then check if Attribute for Mobile API Access is Provided
            if(attributeValue.equals(getAccessAttributeForMobileAPI())) {
                // Since attribute for Mobile API access is provided, ACCESS is GRANTED
                return ACCESS_GRANTED;
            }
        } else {
            if(!attributeValue.equals(getAccessAttributeForMobileAPI())) {
                // Since the request URL is not equal to requestURL for Mobile APIs, ACCESS is GRANTED
                return ACCESS_GRANTED;
            }
        }
        return null;
    }

    private static String getRequestUrl(Object o) {
        if (o instanceof FilterInvocation) {
            return ((FilterInvocation) o).getRequestUrl();
        }
        return "";
    }

    public String getAttributeValue(ConfigAttribute attribute) {
        if (attribute instanceof WebExpressionConfigAttribute) {
            return ((WebExpressionConfigAttribute) attribute).getAuthorizeExpression().getExpressionString();
        } else {
            return null != attribute ? attribute.getAttribute() : null;
        }
    }

    public String getRolePrefix() {
        return rolePrefix;
    }

    public void setRolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
    }

    public String getRequestUrlForMobileAPI() {
        return requestUrlForMobileAPI;
    }

    public void setRequestUrlForMobileAPI(String requestUrlForMobileAPI) {
        this.requestUrlForMobileAPI = requestUrlForMobileAPI;
    }

    public String getAccessAttributeForMobileAPI() {
        return accessAttributeForMobileAPI;
    }

    public void setAccessAttributeForMobileAPI(String accessAttributeForMobileAPI) {
        this.accessAttributeForMobileAPI = accessAttributeForMobileAPI;
    }
}
