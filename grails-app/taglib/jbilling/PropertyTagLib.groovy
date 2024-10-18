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


class PropertyTagLib {

	static namespace = "jB"

    /**
     * Write to out the value of name in the pageScope, request.getAttribute, params, or flash
     * @param name
     */
    def property = { attrs, body ->
        out << (pageScope[attrs.name]) ?:
            (request.getAttribute(attrs.name)) ?:
            (params[attrs.name]) ?:
            (flash[attrs.name]) ?: ""
    }
}
