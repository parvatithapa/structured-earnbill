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

/*
 * Created on Aug 11, 2004
 */
package com.sapienter.jbilling.server.mediation.converter.common;


import java.util.*;

/**
 * @author Emil
 */
public class Util {

    /**
     * Basic CSV line concatination with characters escaping
     *
     * @param values values for concatination
     * @param fieldSeparator character for fields separation
     * @return concatinated string
     */
    public static String concatCsvLine(List<String> values, String fieldSeparator) {

        if (values == null || values.isEmpty()) return null;
        StringBuilder builder = new StringBuilder(escapeStringForCsvFormat(values.get(0), fieldSeparator));
        for (int i = 1; i < values.size(); i++) {
            //add separator for 'not last' element
            builder.append(fieldSeparator);
            builder.append(escapeStringForCsvFormat(values.get(i), fieldSeparator));
        }
        return builder.toString();
    }

    private static String escapeStringForCsvFormat(String str, String fieldSeparator) {
        if (str == null) return "";
        //is escaping fieldSeparators and line separators by quotes needed
        boolean inQuotes = str.indexOf(fieldSeparator) != -1 || str.indexOf('\n') != -1;
        StringBuilder builder = new StringBuilder();
        if (inQuotes) builder.append('\"');
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            //escaping quote by duplicating it
            if (ch == '\"') {
                builder.append('\"');
            }
            builder.append(ch);
        }
        if (inQuotes) builder.append('\"');
        return builder.toString();
    }
}
