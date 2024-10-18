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

import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpSession

/**
 * TabConfigurationService.
 * Used to access the Tab configuration for the current user.
 *
 * @author Gerhard Maree
 * @since  25-03-2013
 */
class TabConfigurationService {

    public static final String SESSION_USER_TABS = "user_tabs"

    /**
     * Load the tab configuration for the currently logged in user.
     */
    def void load() {
        if (httpSession['user_id'] && !httpSession[SESSION_USER_TABS])
            httpSession[SESSION_USER_TABS] = getTabConfiguration()
    }

    /**
     * Returns the tab configuration for the currently logged in user.
     *
     * @return TabConfiguration.
     */
    def TabConfiguration getTabConfiguration() {
        def userId = httpSession["user_id"]
		def list =  TabConfiguration.withCriteria() {
            eq("userId", userId)
        }
        TabConfiguration tabConfiguration = list.isEmpty() ? null : list.get(0)
        return tabConfiguration ? checkForNewTabs(tabConfiguration): createDefaultConfiguration()
    }

    /**
     * Check if tabs have been added to the system.
     * @param tabConfiguration
     * @return
     */
    def TabConfiguration checkForNewTabs(TabConfiguration tabConfiguration) {
        def tabs = Tab.list([sort:"id", cache:true])
        if (tabs.size() == tabConfiguration.tabConfigurationTabs.size()) return tabConfiguration

        def tabsToAdd = tabs - tabConfiguration.tabConfigurationTabs?.tab
        def idx = tabConfiguration.tabConfigurationTabs.size()
        tabsToAdd.each {
            tabConfiguration.addToTabConfigurationTabs(new TabConfigurationTab([displayOrder: idx++, visible:false, tab: it]))
        }
        return tabConfiguration
    }

    /**
     * The default configuration is all tabs ordered by id.
     * @return
     */
    def TabConfiguration createDefaultConfiguration() {
        TabConfiguration tabConfiguration = new TabConfiguration([userId: httpSession["user_id"]])

        def tabs = Tab.list([sort:"defaultOrder", cache:true])

        def idx=0
        tabs.each {
            tabConfiguration.addToTabConfigurationTabs(new TabConfigurationTab([displayOrder: idx++, visible:true, tab: it]))
        }
        return tabConfiguration
    }

    /**
     * Returns the HTTP session
     *
     * @return http session
     */
    def HttpSession getHttpSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }

}
