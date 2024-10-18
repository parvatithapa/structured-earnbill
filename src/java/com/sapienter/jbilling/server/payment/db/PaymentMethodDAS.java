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
package com.sapienter.jbilling.server.payment.db;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import java.util.ArrayList;
import java.util.List;

public class PaymentMethodDAS extends AbstractDAS<PaymentMethodDTO> {

    /**
     * Returns a list of all PaymentMethodDTO <b>except</b> CREDIT.
     * CREDIT is a special kind of method used only for credits.
     *
     * @return List<PaymentMethodDTO>
     */
    public List<PaymentMethodDTO> findAllValidMethods () {
        List<PaymentMethodDTO> methods = this.findAll();
        List<PaymentMethodDTO> validMethods = new ArrayList<PaymentMethodDTO>();
        for (PaymentMethodDTO paymentMethod : methods) {
                validMethods.add(paymentMethod);
        }
        return validMethods;
    }
}
