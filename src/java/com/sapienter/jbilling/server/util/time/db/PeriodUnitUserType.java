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

package com.sapienter.jbilling.server.util.time.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;

import com.sapienter.jbilling.server.util.time.PeriodUnit;
import com.sapienter.jbilling.server.util.db.PersistentEnum;
import com.sapienter.jbilling.server.util.db.PersistentEnumUserType;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Boilerplate class required by Hibernate.
 *
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 * @since  2015-04-10
 *
 */
public class PeriodUnitUserType extends PersistentEnumUserType<PeriodUnit> {

    @Override
    public Class<PeriodUnit> returnedClass () {
        return PeriodUnit.class;
    }
    @Override
    public Object nullSafeGet (ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
        int id = rs.getInt(names[0]);
        if (rs.wasNull()) {
            return null;
        }
        for (PersistentEnum value : returnedClass().getEnumConstants()) {
            if (id == value.getId()) {
                return value;
            }
        }
        throw new IllegalStateException("Unknown " + returnedClass().getSimpleName() + " id");
    }

    @Override
    public void nullSafeSet (PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.SMALLINT);
        } else {
            st.setInt(index, ((PersistentEnum) value).getId());
        }
    }
}
