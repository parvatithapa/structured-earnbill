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

package com.sapienter.jbilling.client.usagePool

import com.sapienter.jbilling.server.notification.NotificationMediumType
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS
import com.sapienter.jbilling.server.usagePool.UsagePoolWS
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

import com.sapienter.jbilling.server.usagePool.UsagePoolWS

/**
 * UsagePoolHelper 
 *
 * @author Amol Gadre
 * @since 29-Nov-2012
 */

class UsagePoolHelper {

    static def addConsumptionActions(UsagePoolWS usagePool, GrailsParameterMap actionsMap) {
        def numberOfConsumption = actionsMap.size() / 6
        for (indexAction in 1..numberOfConsumption) {
            def actionParameters = actionsMap.findAll {k, v ->
                k.toString().startsWith(indexAction + ".")
            }
            def actionMap = [:]
            actionParameters.each{ k, v ->
                actionMap.put(k.toString().substring(2), v)
            }
            if (actionMap.percentage) {
                def mediumType = actionMap.mediumType.isEmpty() ? null : NotificationMediumType.valueOf(actionMap.mediumType);
                usagePool.addConsumptionActions(new UsagePoolConsumptionActionWS(percentage: actionMap.percentage, productId: actionMap.productId,
                        mediumType: mediumType, type: actionMap.type, notificationId: actionMap.notificationId))
            }
        }
    }

    static def UsagePoolWS  bindUsagePool(UsagePoolWS usagePool, GrailsParameterMap params) {
        // sort usagePool parameters by index
        def sorted = new TreeMap<Integer, GrailsParameterMap>()
        params.usagePool.each{ k, v ->
            if (v instanceof Map)
                sorted.put(k, v)
        }

        def root = null
        usagePool = null != usagePool ? usagePool : null

        sorted.each{ i, usageParams ->
            if (usagePool == null) {
                usagePool = root = new UsagePoolWS()
            }
            if (i.equals("consumptionActions")) {
                addConsumptionActions(usagePool, usageParams);
            } else {
                // bind usagePool (can't use bindData() since this isn't a controllerx)
                def args =  [ usagePool, usageParams, [exclude:[]] ]
                new BindDynamicMethod().invoke(usagePool, 'bind', (Object[]) args)
            }
        }
        return usagePool;
    }

}
