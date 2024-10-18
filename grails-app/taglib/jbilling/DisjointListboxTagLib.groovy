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

import org.apache.commons.lang.StringEscapeUtils

/**
 *
 */
class DisjointListboxTagLib {

	static namespace = "jB"

    /**
     * @param id - Base element id used as base for the widget.
     * @param left - List of options to display on left. Each object must have a value and message property
     * @param right - List of options to display on right. Each object must have a value and message property
     * @param left-input - (optional) Name of hidden input that will contain the values of the listbox on the left. Default 'left-input'
     * @param right-input - (optional) Name of hidden input that will contain the values of the listbox on the right. Default 'right-input'
     * @param left-header - (optional) Message code to generate a header for the left hand list
     * @param right-header - (optional) Message code to generate a header for the right hand list
     *
     */
	def disjointListbox = { attrs, body ->
        def widgetId = attrs.id
        def leftOptions = attrs.left
        def rightOptions = attrs.right
        def leftFormInput = attrs.'left-input' ?: "left-input"
        def rightFormInput = attrs.'right-input'  ?: "right-input"
        def leftHeader = attrs.'left-header'
        def rightHeader = attrs.'right-header'

        out <<  '<div id="'+widgetId+'" class="disjoint-listbox">\n' +
                    '<input id="'+widgetId+'-left-order" type="hidden" name="'+leftFormInput+'" />\n' +
                    '<input id="'+widgetId+'-right-order" type="hidden" name="'+rightFormInput+'" />\n' +
                    '<div class="disjoint-listbox-vert-top disjoint-listbox-vert-btn-col">' +
                        '<div style="display:block">' +
                            '<a id="'+widgetId+'-left-up" class="submit3"><span>&#x25B2;</span></a>' +
                        '</div>' +
                        '<div style="display:block">' +
                            '<a id="'+widgetId+'-left-down" class="submit3"><span>&#x25BC;</span></a>' +
                        '</div>' +
                    '</div>\n' +
                    '<div class="disjoint-listbox-vert-top">' +
                        (leftHeader ? '<div class="disjoint-listbox-heading"><strong>'+g.message(code: leftHeader)+'</strong></div>' : '') +
                        '<select id="'+widgetId+'-group-left" multiple="true" >'

        leftOptions.each() {
            out <<          '<option value="'+it.value+'">'+StringEscapeUtils.escapeHtml(g.message(code: it.message))+'</option>'
        }
        out <<          '</select>' +
                    '</div>\n' +
                    '<div class="disjoint-listbox-vert-top disjoint-listbox-vert-btn-col">' +
                        '<div style="display:block">' +
                            '<a id="'+widgetId+'-to-left" class="submit3"><span><</span></a>' +
                        '</div>' +
                        '<div style="display:block">' +
                            '<a id="'+widgetId+'-to-left-all" class="submit3"><span><<</span></a>' +
                        '</div>' +
                        '<div style="display:block">' +
                            '<a id="'+widgetId+'-to-right" class="submit3"><span>></span></a>' +
                        '</div>' +
                        '<div style="display:block;">' +
                            '<a id="'+widgetId+'-to-right-all" class="submit3"><span>>></span></a>' +
                        '</div>' +
                    '</div>\n' +
                    '<div class="disjoint-listbox-vert-top">' +
                        (rightHeader ? '<div class="disjoint-listbox-heading"><strong>'+g.message(code: rightHeader)+'</strong></div>' : '') +
                        '<select id="'+widgetId+'-group-right" multiple="true" >'
        rightOptions.each() {
            out <<          '<option value="'+it.value+'">'+StringEscapeUtils.escapeHtml(g.message(code: it.message))+'</option>'
        }
        out <<          '</select>' +
                    '</div>'+
                    '<div class="disjoint-listbox-spacer"> </div>' +
                '</div>'
	}
}
