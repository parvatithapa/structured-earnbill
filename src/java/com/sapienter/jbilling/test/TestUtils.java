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

package com.sapienter.jbilling.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.sapienter.jbilling.server.util.InternationalDescriptionWS;

public class TestUtils {

    public static <T> List<T> arrayToList (final T[] array) {
        final List<T> newList = new ArrayList<T>(array.length);

        for (final T element : array) {
            newList.add(element);
        }
        return newList;
    }

    public static <T> List<T> arrayToFixedSizeList (final T[] array) {
        return Arrays.asList(array);
    }

    public static List<InternationalDescriptionWS> buildDescriptions (InternationalDescriptionWS... values) {
        return arrayToList(values);
    }

    public static final String             TEST_DATE_FORMAT = "MM/dd/yyyy";

    private static final DateTimeFormatter DateParser       = DateTimeFormat.forPattern(TEST_DATE_FORMAT);

    /*
     * utility methods
     */
    protected static Date AsDate (String dateStr) {
        return DateParser.parseLocalDate(dateStr).toDateTimeAtStartOfDay().toDate();
    }

    public static Date AsDate (int year, int month, int day) {
        return new LocalDate(year, month, day).toDateTimeAtStartOfDay().toDate();
    }

    protected static String AsString (Date date) {
        return DateParser.print(new LocalDate(date).toDateTimeAtStartOfDay());
    }

    public static MetaFieldValueWS find(MetaFieldValueWS[] values, String name) {
        if(values != null ) {
            for(MetaFieldValueWS value : values) {
                if(value.getFieldName().equals(name)) {
                    return value;
                }
            }
        }
        return null;
    }
}
