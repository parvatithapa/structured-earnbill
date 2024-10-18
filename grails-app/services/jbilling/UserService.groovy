/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package jbilling

import com.sapienter.jbilling.server.timezone.TimezoneHelper

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.sapienter.jbilling.client.authentication.AuthenticationUserService
import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import com.sapienter.jbilling.client.authentication.model.User;
import com.sapienter.jbilling.common.LastPasswordOverrideError
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.security.JBCrypto
import com.sapienter.jbilling.server.user.IUserSessionBean
import com.sapienter.jbilling.server.user.UserBL
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.ResetPasswordCodeDTO
import com.sapienter.jbilling.server.user.db.UserCodeDTO
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserPasswordDAS
import com.sapienter.jbilling.server.user.permisson.db.PermissionDTO
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.CriteriaSpecification
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpSession

class UserService implements Serializable {

    static transactional = true

    def messageSource
//    AuthenticationUserService authenticationUserService
    IUserSessionBean userSession
    IWebServicesSessionBean webServicesSession

    /**
     * Returns a list of User Codes filtered by simple criteria. The given filterBy parameter will
     * be used match either the user code or external reference. T
     *
     * @param company company
     * @param params parameter map containing filter criteria
     * @return filtered list of products
     */
    def getFilteredUserCodes(GrailsParameterMap params) {
        // default filterBy message used in the UI
        def defaultFilter = messageSource.resolveCode('userCode.filterBy.default', session.locale).format((Object[]) [])

        // apply pagination arguments or not
        def pageArgs = [max: params.max, offset: params.offset,
                sort: (params.sort && params.sort != 'null') ? params.sort : 'id',
                order: (params.order && params.order != 'null') ? params.order : 'desc']

        def userId = params.int("id")

        // filter on identifier, external reference and validity
        def userCodes = UserCodeDTO.createCriteria().list(
                pageArgs
        ) {
            createAlias("user", "user", CriteriaSpecification.LEFT_JOIN)
            and {
                eq("user.id", userId)

                if (params.filterBy && params.filterBy != defaultFilter) {
                    or {
                        ilike('identifier', "%${params.filterBy}%")
                        ilike('externalReference', "%${params.filterBy}%")
                    }
                }
                if (params.active) {
                    or {
                        isNull('validTo')
                        gt('validTo', TimezoneHelper.currentDateForTimezone(session['company_timezone']))
                    }
                } else {
                    le('validTo', TimezoneHelper.currentDateForTimezone(session['company_timezone']))
                }
            }
        }

        return userCodes
    }

    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    def HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

    /**
     * Saves the user permissions
     *
     * @param userPermissions
     * @param userId
     */
    def savePermissions(Set<PermissionDTO> userPermissions, userId) {
        // save
        UserBL userBL = new UserBL(userId)
        return userBL.setPermissions(userPermissions, session['user_id'] as Integer)
    }

    def updatePassword (ResetPasswordCodeDTO resetCode, String newPassword) {

        UserWS userWS = webServicesSession.getUserWS(resetCode.user.id)
        //encode the password
		
        Integer passwordEncoderId = JBCrypto.getPasswordEncoderId(userWS.mainRoleId)
		String newPasswordEncoded = JBCrypto.encodePassword(passwordEncoderId, newPassword)
        //compare current password with last six
        List<String> passwords = new UserPasswordDAS().findLastSixPasswords(resetCode.user, newPasswordEncoded)
        for(String password: passwords) {
            if(JBCrypto.passwordsMatch(passwordEncoderId, password, newPassword)) {
				LastPasswordOverrideError lastEx = new LastPasswordOverrideError("Password is similar to one of the last six passwords. Please enter a unique Password.");
				
				throw lastEx;
            }
        }
        // do the actual password change
        saveUser(userWS.getUserName(),userWS.entityId, newPasswordEncoded, passwordEncoderId)
        userSession.deletePasswordCode(resetCode);
    }

    public Collection<GrantedAuthority> getAuthorities (String username, Integer entityId) {
        UserBL bl = new UserBL(username, entityId);
        UserDTO user = bl.getEntity();

        if(user == null){
            Integer parent =  bl.getParentCompany(entityId);
            bl = new UserBL(username, parent);
            user = bl.getEntity();
        }

        if (user != null) {
            Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

            for (PermissionDTO permission : bl.getPermissions()) {
                permission.initializeAuthority();
                authorities.add(permission);
            }

            for (RoleDTO role : user.getRoles()) {
                role.initializeAuthority();
                authorities.add(role);
            }

            return authorities;
        }

        return Collections.emptyList();
    }

    public boolean isLockoutEnforced (UserDTO user) {
        String result = userSession.getEntityPreference(user.getEntity().getId(), Constants.PREFERENCE_FAILED_LOGINS_LOCKOUT);
        if (null != result && !result.trim().isEmpty()) {
            int allowedRetries = Integer.parseInt(result);
            return allowedRetries > 0;
        }
        return false;
    }

    public void saveUser(String userName, Integer entityId, String newPasswordEncoded, Integer newScheme){
        UserBL bl = new UserBL(userName, entityId);
        bl.saveUserWithNewPasswordScheme(bl.getEntity().getId(), entityId, newPasswordEncoded, newScheme);
    }

    public User getUser(String username, Integer entityId, CompanyUserDetails userPrincipal) {

        CompanyDAS companyDAS = new CompanyDAS();
        if (userPrincipal != null && !companyDAS.getCurrentAndDescendants(userPrincipal.getCompanyId()).contains(entityId)) {
            def message = "Unauthorized access by caller '" + userPrincipal.getUsername() + "' (id " + userPrincipal.getCompanyId() + ")"
            log.warn(message)
            throw new SecurityException(message);
        }

        UserBL bl = new UserBL(username, entityId);
        UserDTO dto = bl.getEntity();

        if(dto == null){
            Integer parent =  bl.getParentCompany(entityId);
            bl = new UserBL(username, parent);
            dto = bl.getEntity();
        }

        if (dto != null) {
            User user = new User();
            user.setId(dto.getId());
            user.setUsername(dto.getUserName());
            user.setPassword(dto.getPassword()); // hashed password
            user.setEnabled(dto.isEnabled());
            user.setAccountExpired(bl.validateAccountExpired(dto.getAccountDisabledDate()));
            user.setCredentialsExpired(bl.isPasswordExpired());
            //the account is considered locked out if the lockout password is set
            if(isLockoutEnforced(dto)) {
                user.setAccountLocked(bl.isAccountLocked());
            }else{
                user.setAccountLocked(dto.isAccountLocked());
            }
            user.setLocale(bl.getLocale());
            user.setMainRoleId(bl.getMainRole());
            user.setCompanyId(entityId);
            user.setCurrencyId(dto.getCurrency().getId());
            user.setLanguageId(dto.getLanguage().getId());

            return user;
        }

        return null;
    }

    public void deleteRole(Integer roleId, Integer laguageId) throws SessionInternalError{
        userSession.deleteRole(roleId, laguageId)
    }
}
