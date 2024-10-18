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

import org.apache.commons.lang.StringEscapeUtils


class MessagesTagLib {
	
	static namespace = "jB"
	
	def renderErrorMessages = { attrs, body ->
		out << render(template:"/errorTag")
	}

	def truncateLabel = { attrs, body ->

		String label = attrs.label
		Integer max = attrs.max ?: 15
		String suffix = attrs.suffix ?: '...'

		out << ((label && label.size() > max) ? (StringEscapeUtils.escapeHtml(label.substring(0, max)+suffix)) : StringEscapeUtils.escapeHtml(label))
	}
}
