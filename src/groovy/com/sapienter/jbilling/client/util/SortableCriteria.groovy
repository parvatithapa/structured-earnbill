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

package com.sapienter.jbilling.client.util

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.hibernate.criterion.Order

import java.util.regex.Pattern

/**
 * Sortable 
 *
 * @author Brian Cowdery
 * @since 08/06/11
 */
class SortableCriteria {

    static String NO_ALIAS = "NONE";

    static def sort(GrailsParameterMap params, builder) {
        def sort = params.sort?.tokenize(',')?.collect { it.trim() }

        if (params.fetch) {
            def fetchs = params.fetch?.tokenize(',' as String)

            if (params.alias != NO_ALIAS) {
                // explicit alias definitions
                params.alias.each { alias, aliasPath ->
                    if(!fetchs.isEmpty() && !fetchs.getAt(0).equals("null")) {
                        builder.createAlias(aliasPath, alias, Integer.parseInt(fetchs.getAt(0) as String))
                        fetchs.drop(1)
                    }else{
                        builder.createAlias(aliasPath, alias)
                    }
                }
            }
        } else if (params.alias) {
            if (params.alias != NO_ALIAS) {
                // explicit alias definitions
                params.alias.each { alias, aliasPath ->
                    builder.createAlias(aliasPath, alias)
                }
            }
        } else {
            // try and automatically add aliases for sorted associations
            def associations = sort.findAll{ it.contains('.') }
            associations.collect{ it.substring(0, it.indexOf('.')) }.unique().each {
                builder.createAlias(it, it)
            }
        }

        // add order by clauses
        sort.each {
            builder.order(it, params.order)
        }
    }
    
    /**
    * New function for sort added for cases where sort is specified for attributes of an association
    * and alias for that association is already created in the controller list action query.
    * Using regular sort method from above was leading duplicate association path error with alias getting created twice.
    */
    static def buildSortNoAlias(Map params, builder) {
        def sort = params.sort?.tokenize(',')?.collect { it.trim() }

        // add order by clauses
        sort.each {
            builder.order(params.order=='asc' ?
                Order.asc(it).ignoreCase() :
                Order.desc(it).ignoreCase())
        }
    }

    /**
     * Extract a subset of entries from the map.
     * List can either contain keys or Patterns which will matched against all keys.
     *
     * @param params
     * @param names
     * @return
     */
    static def extractParameters(Map params, List names) {
        def aliases = [:]
        if (names) {
            names.each{
                if(it instanceof Pattern) {
                    params.each { k, v ->
                        if (it.matcher(k).matches()) {
                            aliases[k] = v
                        }
                    }
                } else if (params[it]) {
                    aliases[it] = params[it]
                }
            }
        }
        return aliases
    }
}

