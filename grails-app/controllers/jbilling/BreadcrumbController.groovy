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

package jbilling

import grails.plugin.springsecurity.annotation.Secured

/**
 * BreadcrumbController
 *
 * @author Brian Cowdery
 * @since  14-12-2010
 */
@Secured(["isAuthenticated()"])
class BreadcrumbController {
	static scope = "prototype"
    def breadcrumbService

    def index () {
        def crumbs = breadcrumbService.load()
        render template: "/layouts/includes/breadcrumbs", model: [crumbs: crumbs]
    }
}
