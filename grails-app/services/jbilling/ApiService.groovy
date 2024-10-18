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

import com.sapienter.jbilling.server.util.IWebServicesSessionBean

/**
 * Grails managed remote service bean for exported web-services. This bean delegates to
 * the WebServicesSessionBean just like the core JbillingAPI.
 */
@CompileStatic
class ApiService implements IWebServicesSessionBean {

    @Delegate IWebServicesSessionBean webServicesSession

    static transactional = true

    static expose = ['hessian', 'cxfjax', 'httpinvoker']
}
