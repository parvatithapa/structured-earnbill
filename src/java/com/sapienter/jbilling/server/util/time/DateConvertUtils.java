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

package com.sapienter.jbilling.server.util.time;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateConvertUtils {

    public static LocalDate asLocalDate (java.util.Date date) {
        return asLocalDate(date, ZoneId.systemDefault());
    }

    public static LocalDate asLocalDate (java.util.Date date, ZoneId zone) {
        if (date == null)
            return null;

        if (date instanceof java.sql.Date)
            return ((java.sql.Date) date).toLocalDate();
        else
            return Instant.ofEpochMilli(date.getTime()).atZone(zone).toLocalDate();
    }

    public static java.util.Date asUtilDate (LocalDate date) {
        return java.util.Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static java.util.Date asUtilDate (LocalDateTime date) {
        return java.util.Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime asLocalDateTime (java.util.Date date) {
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static java.util.Date getNow() {
        return asUtilDate(LocalDate.now());
    }

    public static ZonedDateTime getZonedDateTime(String dateInString, String dateFormat,
            String homeTimeZoneId, String targetTimeZoneId) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(dateFormat).withZone(ZoneId.of(homeTimeZoneId));
        ZonedDateTime parse = ZonedDateTime.parse(dateInString, fmt);
        return parse.withZoneSameInstant(ZoneId.of(targetTimeZoneId));
    }

    public static ZonedDateTime getZonedDateTime(Date date, String homeTimeZoneId, String targetTimeZoneId) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of(homeTimeZoneId));
        return zdt.withZoneSameInstant(ZoneId.of(targetTimeZoneId));
    }

    public static Date getUtilDateWithZoneInfo(ZonedDateTime zdt, String toZone, String dateFormat) throws ParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        DateFormat format = new SimpleDateFormat(dateFormat);
        ZonedDateTime utcZDT = zdt.withZoneSameInstant(ZoneId.of(toZone));
        return format.parse(formatter.format(utcZDT));
    }
}
