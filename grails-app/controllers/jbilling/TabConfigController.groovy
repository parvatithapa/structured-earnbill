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

import com.sapienter.jbilling.csrf.RequiresValidFormToken
import grails.plugin.springsecurity.annotation.Secured

@Secured(["isAuthenticated()"])
class TabConfigController {
	static scope = "prototype"

    def index () {
        redirect action: "show"
    }

    def show () {
        def tabConfiguration = session[TabConfigurationService.SESSION_USER_TABS].tabConfigurationTabs.findAll{it.tab.parentTab == null}
        [tabConfigurationTabs: tabConfiguration]
    }

    @RequiresValidFormToken
    def save () {
        TabConfiguration tabConfiguration = session[TabConfigurationService.SESSION_USER_TABS]
        List<Long> visiblesTabs = params["visible-order"].tokenize(",")*.toLong()
        int i = 0

        tabConfiguration.tabConfigurationTabs.each {
            if(visiblesTabs.contains(it.tab.id)) {
                it.displayOrder = visiblesTabs.indexOf(it.tab.id)
                it.visible = true
            } else {
                it.displayOrder = visiblesTabs.size() + i
                it.visible = false
                i++
            }
        }

        def newTabs = tabConfiguration.tabConfigurationTabs.collect()
        tabConfiguration.tabConfigurationTabs.clear()
        tabConfiguration.tabConfigurationTabs.addAll(newTabs)

        tabConfiguration.save(flush: true)
        session[TabConfigurationService.SESSION_USER_TABS] = tabConfiguration
        redirect action: "show"
    }
}
