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

import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;
import org.hibernate.Query;

/**
 * 
 * @author abimael
 *
 */
public class PaymentResultDAS extends AbstractDAS<PaymentResultDTO>{

    private final static String QUERY = "SELECT paymentResult " +
            "FROM InternationalDescriptionDTO description, PaymentResultDTO paymentResult " +
            "WHERE description.id.tableId = :tableId " +
            "AND description.id.foreignId = paymentResult.id " +
            "AND description.id.languageId = :language " +
            "AND description.content = :name";

    public PaymentResultDTO findPaymentResultByName(String name, Integer language) {
        JbillingTableDAS jbTableDAS = Context.getBean(Context.Name.JBILLING_TABLE_DAS);

        Query query = getSession().createQuery(QUERY);
        query.setParameter("tableId", jbTableDAS.findByName("payment_result").getId());
        query.setParameter("language", language);
        query.setParameter("name", name);

        return (PaymentResultDTO) query.uniqueResult();
    }
}
