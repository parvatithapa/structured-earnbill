/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2013] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.pricing.cache;

import com.sapienter.jbilling.server.pricing.MatchingRecord;
import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;

/**
 * Filter Callback
 *
 * @author Panche Isajeski
 * @since 22-Aug-2013
 */
public interface FilterCallback {

    public static final FilterCallback ACCEPT_ALL = new FilterCallback() {

        public boolean accept(MatchingFieldDTO matchingFieldDTO, MatchingRecord record) {
            return true;
        }
    };

    abstract boolean accept(MatchingFieldDTO matchingFieldDTO, MatchingRecord record) throws Exception;
}
