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

package com.sapienter.jbilling.server.invoice;

import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;

import java.util.Comparator;

/**
 * Created by igutierrez on 10/5/16.
 */
public class InvoiceLineChildComparator implements Comparator<InvoiceLineDTO> {

    @Override
    public int compare(InvoiceLineDTO line1, InvoiceLineDTO line2) {
        if (line1.getSourceUserId().equals(line2.getSourceUserId())) {
            return line1.getDescription().compareTo(line2.getDescription());
        } else {
            return line1.getSourceUserId() - line2.getSourceUserId();
        }
    }
}
