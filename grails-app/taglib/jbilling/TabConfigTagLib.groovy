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

import grails.plugin.springsecurity.SpringSecurityUtils


class TabConfigTagLib {

	static namespace = "jB"

    /**
     * Will render the body of the tag if the current user has access to the tab
     *
     * attr tab - jbilling.Tab object.
     */
	def userCanAccessTab = { attrs, body ->
        def tab = attrs.tab

        if (tab.requiredRole) {
            attrs.roles = tab.requiredRole
            out << sec.ifAllGranted(attrs, body)
        } else if (tab.accessUrl) {
            attrs.url = tab.accessUrl
            out << sec.access(attrs, body)
        }
	}

    /**
     * Will render the body of the tag if the current user does not have access to the tab
     *
     * attr tab - jbilling.Tab object.
     */
    def userCanNotAccessTab = { attrs, body ->
        def tab = attrs.tab

        if (tab.requiredRole) {
            attrs.roles = tab.requiredRole
            out << sec.ifNotGranted(attrs, body)
        } else if (tab.accessUrl) {
            attrs.url = tab.accessUrl
            out << sec.noAccess(attrs, body)
        }
    }
}
