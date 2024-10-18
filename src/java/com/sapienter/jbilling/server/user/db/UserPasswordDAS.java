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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.List;

public class UserPasswordDAS extends AbstractDAS<UserPasswordDTO> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(UserDAS.class));

    public List<String> findLastSixPasswords(UserDTO userDTO,String newPassword){
        Criteria criteria = getSession().createCriteria(UserPasswordDTO.class);
        criteria.add(Restrictions.eq("user",userDTO))
        .addOrder(Order.desc("dateCreated"))
        .setMaxResults(6)
        .setProjection(Projections.property("encryptedPassword"));
        return !criteria.list().isEmpty()?(List<String>) criteria.list():new ArrayList<String>();
    }
}
