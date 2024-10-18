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

import com.sapienter.jbilling.server.util.api.JbillingDeutscheTelecomAPI
import groovy.transform.CompileStatic

/**
 * Grails managed remote service bean to expose DeutscheTelecom specific web-services.
 * This bean delegates to DeutscheTelecomWebServicesSessionSpringBean for actual implementation.
 */
@CompileStatic
class DeutscheTelecomApiService implements JbillingDeutscheTelecomAPI {

    @Delegate JbillingDeutscheTelecomAPI deutscheTelecomWebServicesSession

    static transactional = true

    static expose = ['hessian', 'cxfjax', 'httpinvoker']

}
