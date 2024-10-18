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

package com.sapienter.jbilling.server.diameter;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.PricingField;

import java.util.List;


public class ItemLocatorByInternalNumber implements DiameterItemLocator {

    private String fieldName = "Rating-Group";

    public ItemDTO findItem(List<PricingField> params, Integer entityId) {
        for (PricingField f : params) {
            if (fieldName.equals(f.getName())) {
                return new ItemDAS().findItemByInternalNumber(f.getStrValue(), entityId);
            }
        }
        return null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
