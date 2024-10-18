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

import java.lang.invoke.MethodHandles;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.server.util.Constants;

/**
 * @author Emil
 */
public class InvoiceLineComparator implements Comparator<InvoiceLineDTO> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public int compare(InvoiceLineDTO perA, InvoiceLineDTO perB) {
        int retValue = Integer.compare(perA.getOrderPosition(), perB.getOrderPosition());

        // if orderPosition is the same, then we need further comparison
        if (retValue == 0) {
            try {
                // the line type should tell first
                if (perA.getTypeId() == Constants.INVOICE_LINE_TYPE_SUB_ACCOUNT
                        && perB.getTypeId() == Constants.INVOICE_LINE_TYPE_SUB_ACCOUNT) {
                    // invoice lines have to be grouped by user, find out both users
                    retValue = perA.getSourceUserId().compareTo(perB.getSourceUserId());

                    if (retValue != 0) {
                        // these are lines for two different users, so they are different enough now
                        return retValue;
                    }
                }
                // use the number
                if (perA.getItem() != null && perB.getItem() != null) {
                    String itemNumberA = perA.getItem().getNumber();
                    String itemNumberB = perB.getItem().getNumber();
                    if (itemNumberA == null && itemNumberB == null) {
                        retValue = Integer.compare(perA.getItem().getId(), perB.getItem().getId());
                    } else if (itemNumberA == null) {
                        retValue = 1;
                    } else if (itemNumberB == null) {
                        retValue = -1;
                    } else {
                        retValue = itemNumberA.compareTo(itemNumberB);
                        if (retValue == 0) {
                            // if comparing same item, then use its id to compare
                            retValue = Integer.compare(perA.getId(), perB.getId());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Comparing invoice lines {} {}", perA, perB, e);
                retValue = 0;
            }
        } 

        return retValue;
    }

}
