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
package com.sapienter.jbilling.server.payment.blacklist;

import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.*;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDAS;
import com.sapienter.jbilling.server.payment.blacklist.db.BlacklistDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Util;

/**
 * Filters contact names.
 */
public class NameFilter implements BlacklistFilter {

    public Result checkPayment(PaymentDTOEx paymentInfo) {
        return checkUser(paymentInfo.getUserId());
    }

    public Result checkUser(Integer userId) {
        Integer entityId = new UserDAS().find(userId).getCompany().getId();

        ContactDTO userContact = new ContactDAS().findContact(userId);
        //check in the contact
        if (userContact != null) {
            if(null != userContact.getFirstName() || null != userContact.getLastName()){
                List<BlacklistDTO> blacklist = new BlacklistDAS().filterByName(
                        entityId, userContact.getFirstName(), userContact.getLastName());

                if (!blacklist.isEmpty()) {
                    ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
                    return new Result(true,
                            bundle.getString("payment.blacklist.name_filter"));
                }
            }
        }

        //check against meta fields
        CustomerDAS customerDAS = new CustomerDAS();
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        Integer customerId = customerDAS.getCustomerId(userId);
        if(null != customerId){
            List<Integer> aitIds = customerDAS.getCustomerAccountInfoTypeIds(customerId);
            for(Integer ait : aitIds){
                String firstName = null;
                String lastName = null;
                Date effectiveDate = TimezoneHelper.companyCurrentDate(entityId);
                
                List<Integer> firstNameIds =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.FIRST_NAME, ait, effectiveDate);
                Integer firstNameId = null != firstNameIds && firstNameIds.size() > 0 ?
                        firstNameIds.get(0) : null;

                List<Integer> lastNameIds =
                        metaFieldDAS.getCustomerFieldValues(customerId, MetaFieldType.LAST_NAME, ait, effectiveDate);
                Integer lastNameId = null != lastNameIds && lastNameIds.size() > 0 ?
                        lastNameIds.get(0) : null;

                if(null != firstNameId) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(firstNameId);
                    firstName = null != value.getValue() ? (String) value.getValue() : null;
                }

                if(null != lastNameId) {
                    MetaFieldValue value = metaFieldDAS.getStringMetaFieldValue(lastNameId);
                    lastName = null != value.getValue() ? (String) value.getValue() : null;
                }

                if(null != firstName || null != lastName) {

                    List<BlacklistDTO> blacklist = new BlacklistDAS().filterByName(
                            entityId,
                            firstName,
                            lastName);

                    if (!blacklist.isEmpty()) {
                        ResourceBundle bundle = Util.getEntityNotificationsBundle(userId);
                        return new Result(true,
                                bundle.getString("payment.blacklist.name_filter"));
                    }
                }
            }
        }

        return new Result(false, null);
    }

    public String getName() {
        return "Name blacklist filter";
    }
}
