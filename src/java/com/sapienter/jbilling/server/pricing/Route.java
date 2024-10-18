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
package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.user.db.MatchingFieldDTO;

import java.io.Serializable;
import java.util.Set;

/**
 * Route
 *
 * @author Panche Isajeski
 * @since 22-Aug-2013
 */
public interface Route extends Serializable {

    public String getTableName();

    public Set<MatchingFieldDTO> getMatchingFields();

}
