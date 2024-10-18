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

import groovy.transform.CompileStatic

import com.sapienter.jbilling.server.util.api.JbillingDistributelAPI

/**
 * Grails managed remote service bean to expose Distributel specific web-services. 
 * This bean delegates to DistributelWebServicesSessionSpringBean for actual implementation.
 */
@CompileStatic
class DistributelApiService implements JbillingDistributelAPI {

	@Delegate JbillingDistributelAPI distributelWebServicesSession

    static transactional = true

    static expose = ['hessian', 'cxfjax', 'httpinvoker']

}
