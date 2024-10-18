package com.sapienter.jbilling.rest

import grails.plugin.springsecurity.annotation.Secured

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

class UtilController {
	static scope = "singleton"

    @Secured(["isAuthenticated()", "API_120"])
    def restApi () {
    }
}
