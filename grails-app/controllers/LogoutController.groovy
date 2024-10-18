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


import com.sapienter.jbilling.server.user.IUserSessionBean
import grails.plugin.springsecurity.SpringSecurityUtils

class LogoutController {
	static scope = "singleton"
	IUserSessionBean userSession
	/**
	 * Index action. Redirects to the Spring security logout uri.
	 */
	def filterService
	def index () {
		userSession.logout(session['user_id'])

		def filters = filterService.getCurrentFilters();
        filters?.each{
            if (!it.name.isEmpty())
                it.visible = false
				it.clear()
        }
		redirect uri: SpringSecurityUtils.securityConfig.logout.filterProcessesUrl // '/j_spring_security_logout'
	}
}
