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

package com.sapienter.jbilling.server.util.converter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ResourceBundle;

import org.apache.commons.beanutils.Converter;
import org.springframework.context.i18n.LocaleContextHolder;


/**
 * TimestampConverter
 *
 * @author Ashwinkumar Patra
 * @since 17/03/17
 */
public class TimestampConverter implements Converter {

    private DateTimeFormatter dateFormatter;

    public TimestampConverter() {
        this.dateFormatter = DateTimeFormatter.ofPattern(ResourceBundle.getBundle("entityNotifications", LocaleContextHolder.getLocale()).getString("format.date"));
    }

    @Override
    public String convert(Class type, Object date) {
        return LocalDateTime.ofInstant(((Date)  date).toInstant(), ZoneOffset.UTC).format(dateFormatter);
    }
}
