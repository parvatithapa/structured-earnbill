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

import grails.plugin.springsecurity.SecurityTagLib
import grails.plugin.springsecurity.SpringSecurityUtils
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

/**
 * The tag library can be used to display links if the user has the specified permission, otherwise only the body will be shown
 */
class SecureLinkTagLib extends SecurityTagLib {

	static namespace = "jB"

    /**
     * @attr permissions The list of permissions. Access is granted if user as any.
     */
	Closure secRemoteLink = { attrs, body ->
        def permissions = attrs.remove('permissions')
        if(permissions) {
            if(SpringSecurityUtils.ifAnyGranted(permissions)) {
                out << g.remoteLink(attrs, body)
            } else {
                out << body()
            }
        } else {
            def attrsClone = attrs.clone()
            if(!attrsClone['url'] ) {
                if(!attrsClone['controller'] ) attrsClone['controller'] = pageScope.controllerName
                if(!attrsClone['action'] ) attrsClone['action'] = pageScope.actionName
            } else if(attrs['controller'] || attrs['action']) {
                attrs.remove('url')
            }

            if(hasAccess(attrsClone, 'secLink')) {
                out << g.remoteLink(attrs, body)
            } else {
                out << body()
            }
        }
	}

    /**
     * @attr permissions The list of permissions. Access is granted if user as any.
     */
    Closure secLink = { attrs, body ->
        def permissions = attrs.remove('permissions')
        if(permissions) {
            if(SpringSecurityUtils.ifAnyGranted(permissions)) {
                out << g.link(attrs, body)
            } else {
                out << body()
            }
        } else {
            def attrsClone = attrs.clone()
            if(!attrsClone['url'] ) {
                if(!attrsClone['controller'] ) attrsClone['controller'] = pageScope.controllerName
                if(!attrsClone['action'] ) attrsClone['action'] = pageScope.actionName
            } else if(attrs['controller'] || attrs['action']) {
                attrs.remove('url')
            }

            if(hasAccess(attrsClone, 'secLink')) {
                out << g.link(attrs, body)
            } else {
                out << body()
            }
        }
    }
}
