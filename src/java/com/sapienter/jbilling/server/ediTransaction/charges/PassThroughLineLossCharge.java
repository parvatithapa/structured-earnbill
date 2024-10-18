/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.ediTransaction.charges;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Created by hitesh on 29/8/16.
 */
public class PassThroughLineLossCharge extends AbstractCharges {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PassThroughLineLossCharge.class));

    @Override
    public void applyCharge() {
        if (isApplicableForCharges()) {
            Integer languageId = userDTO.getLanguageIdField();
            ItemDTO item = new ItemDAS().findItemByInternalNumber(internalNumber, companyId);
            if (item == null) {
                LOG.error("Item not found for " + internalNumber + " internal number");
                throw new SessionInternalError("Item not found for " + internalNumber + " internal number");
            }
            createOrderLine(orderDTO, item.getId(), item.getDescription(languageId), totalConsumption, null, true);
        }
    }

    @Override
    public boolean isApplicableForCharges() {
        LOG.debug("check charges applicability");
        CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField = userDTO.getCustomer().getCustomerAccountInfoTypeMetaField(FileConstants.PASS_THROUGH_CHARGES_META_FIELD);
        if (customerAccountInfoTypeMetaField != null && customerAccountInfoTypeMetaField.getMetaFieldValue() != null && customerAccountInfoTypeMetaField.getMetaFieldValue().getValue() != null) {
            this.internalNumber = customerAccountInfoTypeMetaField.getMetaFieldValue().getValue().toString();
            if (isRateReadyCustomer()) {
                String orderNotes = "Skip " + internalNumber + " Process For Rate Ready Customer.";
                LOG.info(orderNotes);
                addNotes(orderNotes);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * This method used for check the customer is Rate Ready or not.
     *
     * @return true/false.
     */
    private boolean isRateReadyCustomer() {
        return customerType.equals(FileConstants.BILLING_MODEL_RATE_READY);
    }

    /**
     * This method used for adding the notes at order level.
     *
     * @return Nothing.
     */
    public void addNotes(String orderNotes) {
        if (StringUtils.isNotEmpty(orderDTO.getNotes())) {
            orderDTO.setNotes(orderDTO.getNotes() + "\n" + orderNotes);
        } else {
            orderDTO.setNotes(orderNotes);
        }
    }
}
