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

package com.sapienter.jbilling.client.discount

import com.sapienter.jbilling.server.discount.DiscountWS
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.apache.commons.lang.StringUtils

/**
 * DiscountHelper 
 *
 * @author Amol Gadre
 * @since 29/11/12
 */
class DiscountHelper {

	private static def log = Logger.getLogger(this)	

    static def DiscountWS bindDiscount(DiscountWS discount, GrailsParameterMap params) {        
        def oldType = ""
        def newType = ""
        
        // sort price model parameters by index
        def sorted = new TreeMap<Integer, GrailsParameterMap>()
        params.discount.each{ k, v ->
            // all other fields are populated using bindData
            // lets only bind attributes, also we need oldType, newType below
            if ((k.startsWith("attribute.") && (k.endsWith(".name") || k.endsWith (".value")))
                    || k.equals("oldType") || k.equals("type")) {
                sorted.put(k, v)
                // lets not remove this debug, may be useful for debugging
                log.debug("******* k=${k}		v=${v}")
            }
        }
        if (discount == null) {
        	discount = new DiscountWS()
		}
		
		def attributeIndex = 0
		SortedMap<String, String> newSortedMap = new TreeMap<String, String>(sorted)
		while (newSortedMap.size() != 0) {
    		attributeIndex++
    		String key = newSortedMap.remove("attribute." + attributeIndex + ".name")
    		String value = newSortedMap.remove("attribute." + attributeIndex + ".value")
			newSortedMap.remove("attribute." + attributeIndex + "._value")
    		oldType = newSortedMap.remove("oldType")
    		newType = newSortedMap.remove("type")
    		if (key && !key.trim()?.isEmpty())
    			discount.attributes.put(key, value)
    	}
    	if (newType && newType.equalsIgnoreCase("RECURRING_PERIODBASED")) {
    		if (discount.attributes == null || 
    			(discount.attributes && 
    			 discount.attributes.get("isPercentage") == null)) {
    			discount.attributes.put("isPercentage", "No")
    		}
    	}

		// clear type specific attributes after a change in strategy
        if (oldType && StringUtils.trimToNull(oldType) && newType != oldType) {
            DiscountStrategyType oldStrategyType = DiscountStrategyType.valueOf(oldType)
            for (AttributeDefinition attribute : oldStrategyType.strategy.attributeDefinitions) {
                discount.attributes.remove(attribute.name)
            }
        }
		
        return discount
    }

}
