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
package com.sapienter.jbilling.server.user.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class ResetPasswordCodeDAS extends AbstractDAS<ResetPasswordCodeDTO> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UserDAS.class));

    public ResetPasswordCodeDTO findByUser(UserDTO user){
        Criteria criteria = getSession().createCriteria(ResetPasswordCodeDTO.class)
                        .add(Restrictions.eq("user", user));

        return (ResetPasswordCodeDTO) criteria.uniqueResult();
    }

    public ResetPasswordCodeDTO findByToken(String token){
        Criteria criteria = getSession().createCriteria(ResetPasswordCodeDTO.class)
                .add(Restrictions.eq("token", token));

        return (ResetPasswordCodeDTO) criteria.uniqueResult();
    }
}
