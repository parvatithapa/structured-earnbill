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
import com.sapienter.jbilling.server.util.Context
import grails.plugin.springsecurity.SpringSecurityUtils

class LogoutController {
	static scope = "singleton"
	/**
	 * Index action. Redirects to the Spring security logout uri.
	 */
	def filterService
	def index () {
        //get bean to update event audit log during log out
        IUserSessionBean iUserSessionBean = (IUserSessionBean) Context.getBean(Context.Name.USER_SESSION)
        iUserSessionBean.logout(session['user_id'])

		def filters = filterService.getCurrentFilters();
        filters?.each{
            if (!it.name.isEmpty())
                it.visible = false
				it.clear()
        }
		redirect uri: SpringSecurityUtils.securityConfig.logout.filterProcessesUrl // '/j_spring_security_logout'
	}
}
