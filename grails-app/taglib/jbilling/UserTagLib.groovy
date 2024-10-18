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

import com.sapienter.jbilling.server.metafields.MetaFieldType
import com.sapienter.jbilling.server.user.contact.db.ContactDTO
import com.sapienter.jbilling.server.user.db.CustomerDTO
import com.sapienter.jbilling.server.user.db.UserDTO
import com.sapienter.jbilling.server.util.IWebServicesSessionBean


class UserTagLib {

	static namespace = "jB"

    IWebServicesSessionBean webServicesSession

    /**
     * Write to out the value of name in the pageScope, request.getAttribute, params, or flash
     * @param name
     */
    def userFriendlyName = { attrs, body ->
        if(null == session['user_id']) {
            out << 'not logged in'
        }
        ContactDTO contact = ContactDTO.findByUserId(session['user_id'])
        if(contact?.firstName && contact?.lastName) {
            out << contact.firstName + ' ' + contact.lastName
        } else {
            CustomerDTO customer = CustomerDTO.findByBaseUser(new UserDTO(session['user_id']))
            def firstName = null, lastName = null
            customer?.customerAccountInfoTypeMetaFields.each {
                def fieldUsage = it.metaFieldValue.field.fieldUsage
                if(fieldUsage == MetaFieldType.FIRST_NAME) {
                    firstName = it.metaFieldValue.value
                } else if(fieldUsage == MetaFieldType.LAST_NAME) {
                    lastName = it.metaFieldValue.value
                }
            }

            if(firstName || lastName) {
                out << firstName?:firstName + ' ' + lastName?:lastName
            } else {
                out << sec.loggedInUserInfo([field:"plainUsername"])
            }
        }
    }
}
