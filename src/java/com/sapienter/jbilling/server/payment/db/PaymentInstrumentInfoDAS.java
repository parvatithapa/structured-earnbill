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

import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * 
 * @author khobab
 *
 */
public class PaymentInstrumentInfoDAS extends AbstractDAS<PaymentInstrumentInfoDTO> {

    private final static String INSTRUMENT_ID_SQL =
            "select p.instrument_id from payment_instrument_info p " +
                    "  where p.payment_id = :payment";


    public Integer getInstrumentIdOfPayment(Integer payment) {
        Query sqlQuery = getSession().createSQLQuery(INSTRUMENT_ID_SQL);
        sqlQuery.setParameter("payment", payment);
        sqlQuery.setMaxResults(1);
        return (Integer) sqlQuery.uniqueResult();
    }


}
