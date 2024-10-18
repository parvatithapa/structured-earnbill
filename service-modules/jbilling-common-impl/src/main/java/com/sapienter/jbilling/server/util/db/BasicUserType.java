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

package com.sapienter.jbilling.server.util.db;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.Serializable;

/**
 * 
 * @author Igor Poteryaev <igor.poteryaev@jbilling.com>
 * @since  2015-04-10
 *
 */
public abstract class BasicUserType implements UserType {

    @Override
    public Object assemble (Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object deepCopy (Object value) throws HibernateException {
        return value;
    }

    @Override
    public Serializable disassemble (Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public boolean equals (Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public int hashCode (Object x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object replace (Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
