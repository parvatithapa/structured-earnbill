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

/**
 * This can be used together with the FlowHelper to create alternate flows (view/redirects/templates) through your controller.
 * The taglib will put additional hidden inputs into your html form based on property values passed to the page.
 * The FlowHelper must then be used in the controller to change for a change in flow.
 *
 * As an example /myAccount/editUser.gsp includes /user/_editForm.gsp and passes values for altFailureView and altRedirect
 * The form submits to UserController.save which will change it's normal flow with the help of the FlowHelper
 */
class FlowTagLib {

	static namespace = "jB"

	def flow = { attrs, body ->
        ['altSuccessView', 'altFailureView', 'altView', 'altSuccessTemplate', 'altFailureTemplate', 'altTemplate', 'altChain', 'altRedirect'].each {
            String value = jB.property([name: it])
            if(value) out << "<input type='hidden' name='"+it+"' value='"+value+"'/>"
        }
	}
}
