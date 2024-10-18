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
import com.sapienter.jbilling.common.CommonConstants
import com.sapienter.jbilling.license.exception.LicenseExpiredException
import com.sapienter.jbilling.license.exception.LicenseInvalidException
import com.sapienter.jbilling.license.exception.LicenseMissingException
import com.sapienter.jbilling.saml.SamlUtil
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.PreferenceBL
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import jbilling.ControllerUtil
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.context.SecurityContextHolder as SCH
import org.springframework.security.saml.SAMLEntryPoint
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import com.sapienter.jbilling.server.user.db.CompanyDTO

class LoginController {

	IWebServicesSessionBean webServicesSession
	static scope = "singleton"

	/**
	 * Dependency injection for the authenticationTrustResolver.
	 */
	def authenticationTrustResolver

	/**
	 * Dependency injection for the springSecurityService.
	 */
    def springSecurityService

	/**
	 * Default action; redirects to 'defaultTargetUrl' if logged in, /login/auth otherwise.
	 */
	def index () {
		if (springSecurityService.isLoggedIn()) {
			redirect uri: SpringSecurityUtils.securityConfig.successHandler.defaultTargetUrl
		} else {
			redirect action: 'auth', params: params
		}
	}

	/**
	 * Show the login page.
	 */
	def auth () {

		def config = SpringSecurityUtils.securityConfig
		if (springSecurityService.isLoggedIn()) {
			redirect uri: config.successHandler.defaultTargetUrl
			return
		}
		def errorMsg = chainModel?.errorMsg
		def companyId = params.companyId
		// If there is a companyId in the url as a parameter
		if (companyId) {
			// Check if the companyId is integer
			companyId = params.int('companyId')
			if (companyId) {
				companyId = CompanyDTO.get(companyId)?.id
				// Check if company with that id exists
				if (companyId) {
					session['COMPANY_ID'] = companyId
				} else {
					flash.error = message(code: 'login.company.id.error')
				}
			} else {
				flash.error = message(code: 'login.company.id.type.error')
			}
		}
        if(errorMsg){
            flash.error = errorMsg
        }
		String view = 'auth'
		String postUrl = "${request.contextPath}${config.apf.filterProcessesUrl}"
		render view: view, model: [postUrl: postUrl,companyId: companyId]
	}

	def authenticateViaIdp () {
		def config = SpringSecurityUtils.securityConfig

		if (springSecurityService.isLoggedIn()) {
			redirect uri: SpringSecurityUtils.securityConfig.successHandler.defaultTargetUrl
		} else {
			def companyId = params.get("j_client_id")
			def ssoActive = 0
			if (null != companyId) {
				ssoActive = PreferenceBL.getPreferenceValue(companyId as int, CommonConstants.PREFERENCE_SSO) as int
			}

			if (ssoActive) {
				if (null != companyId) {
					def contextPath = "${request.contextPath}"

					def url = "${contextPath}${SAMLEntryPoint.FILTER_URL}"
					def defaultIdp = SamlUtil.getDefaultIdpUrl(companyId as Integer)

					if(null != defaultIdp && !defaultIdp.isEmpty()) {
						url += "?" + SAMLEntryPoint.IDP_PARAMETER + "=" + defaultIdp
						log.debug("Final redirect url : " + url)
						redirect(url: url)
					} else {
						log.error("No default Idp is configured for company : " + companyId)
						flash.error = message(code: 'default.idp.error')
					}
				} else {
					log.error("No default SSO Enabled user and Idp is configured for company : " + companyId + " and username : " + username)
					flash.error = message(code: 'sso.not.enabled.error')
				}

				String postUrl = "${request.contextPath}${config.apf.filterProcessesUrl}"
				render view: 'auth', model: [postUrl            : postUrl,
											 rememberMeParameter: config.rememberMe.parameter]
			} else {
				log.error("SSO is not active for company : " + companyId)
				flash.error = message(code: 'sso.not.active.error')
				String postUrl = "${request.contextPath}${config.apf.filterProcessesUrl}"
				render view: 'auth', model: [postUrl            : postUrl,
											 rememberMeParameter: config.rememberMe.parameter]
			}
		}
	}

	/**
	 * Show denied page.
	 */
	def denied () {
		if (springSecurityService.isLoggedIn() &&
				authenticationTrustResolver.isRememberMe(SCH.context?.authentication)) {
			// have cookie but the page is guarded with IS_AUTHENTICATED_FULLY
			redirect action: 'full', params: params
		}
	}

	/**
	 * Login page for users with a remember-me cookie but accessing a IS_AUTHENTICATED_FULLY page.
	 */
	def full () {
		def config = SpringSecurityUtils.securityConfig
		render view: 'auth', params: params,
			model: [postUrl: "${request.contextPath}${config.apf.filterProcessesUrl}"]
	}

	/**
	 * Callback after a failed login. Redirects to the auth page with a warning message.
	 */
	def authfail () {

		def username = session[UsernamePasswordAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY]
		String msg = ''
		def exception = session[AbstractAuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY]
		if (exception) {
			if (exception instanceof AccountExpiredException) {
                msg = g.message(code: "springSecurity.errors.login.inactive")
			}
			else if (exception instanceof CredentialsExpiredException) {
				msg = g.message(code: "springSecurity.errors.login.passwordExpired")
				flash.error = msg
				redirect url: '/resetPassword/resetExpiryPassword'
				return
			}
			else if (exception instanceof DisabledException) {
                msg = g.message(code: "springSecurity.errors.login.disabled")
			}
			else if (exception instanceof LockedException) {
                msg = g.message(code: "springSecurity.errors.login.locked.temporary")
			}
            else if (exception instanceof LicenseMissingException) {
                msg = 'auth.fail.license.missing.exception'
            }
            else if (exception instanceof LicenseInvalidException) {
                msg = 'auth.fail.license.invalid.exception'
            }
            else if (exception instanceof LicenseExpiredException) {
                msg = 'auth.fail.license.expired.exception'
            } else if (exception instanceof InsufficientAuthenticationException) {
				msg = g.message(code: "springSecurity.errors.login.justInTime.disabled")
			} else if (exception instanceof InstantiationException) {
				msg = g.message(code: "springSecurity.errors.login.justInTime.creation")
			} else {
				if (grailsApplication.config.useUniqueLoginName) {
					msg = g.message(code: "uniqueName.errors.login.fail")
				} else {
					msg = g.message(code: "springSecurity.errors.login.fail")
				}
			}
		}

		if (springSecurityService.isAjax(request)) {

			render([error: msg] as JSON)
		}
		else {
            def loginError = params.login_error
            // At this point there should be login error
            if(loginError){
                def companyId = params.j_client_id ?:session['COMPANY_ID']
                // Delete all parameters
                params.clear()
                // Check if the error came from url with company id initially
                if(companyId && session['COMPANY_ID']){
                    // Put the companyId parameter
                    params.put('companyId', companyId)
                }
                // Put the login error parameter
                params.put('login_error', loginError)

				if(msg.empty) msg = g.message(code: "springSecurity.errors.login.fail")
            }
			chain action: 'auth', params: params, model: [errorMsg: msg]
		}
	}

	/**
	 * The Ajax success redirect url.
	 */
	def ajaxSuccess () {
		render([success: true, username: springSecurityService.authentication.name] as JSON)
	}

	/**
	 * The Ajax denied redirect url.
	 */
	def ajaxDenied () {
		render([error: 'access denied'] as JSON)
	}

	def getCompanyId () {
		def enteredUsername = params.userName
		def companyId = ControllerUtil.fetchCompanyIdFromUserName(enteredUsername)
		session['COMPANY_ID'] = companyId
		render(companyId)
	}
}
