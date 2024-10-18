package com.sapienter.jbilling.server.invoiceTemplate.report;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class, providing formatting functionality inside report;
 * actually uses copies of methods from {@link com.sapienter.jbilling.server.util.Util}
 * or delegates calls to them
 *
 * @author Klim
 */
public class FormatUtil {

    public static final String PARAMETER_NAME = "format_util";

    private final Locale locale;
    private final String currencySymbol;

    public FormatUtil(Locale locale, String currencySymbol) {
        this.locale = locale;
        this.currencySymbol = currencySymbol;
    }

    public String date(Date date) {
        return Util.formatDate(date, locale);
    }

    public String money(Number number) throws SessionInternalError {
        return convertToCurrency(number, currencySymbol);
    }

    public String money(Number number, String symbol) throws SessionInternalError {
       return convertToCurrency(number, symbol);
    }

    private String convertToCurrency(Number number, String symbol){
        try {
        	ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            ((DecimalFormat) format).applyPattern(bundle.getString("format.float"));

            return symbol +
                    (!symbol.isEmpty() ? " " : "") +
                    format.format(number.doubleValue());

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public String pcnt(Number number) throws SessionInternalError {
        try {
        	ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            ((DecimalFormat) format).applyPattern(bundle.getString("format.float"));

            return format.format(number.doubleValue()) + bundle.getString("format.percentage");
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public String dec(Number number) throws SessionInternalError {
        try {
        	ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            ((DecimalFormat) format).applyPattern(bundle.getString("format.float.invoice"));

            return format.format(number.doubleValue());

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Number parse(String src) {
        try {
        	ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", locale);
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            ((DecimalFormat) format).applyPattern(bundle.getString("format.float"));

            return format.parse(src);

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    private static boolean isNullOrZero(Number number) {
        if (number == null) {
            return true;
        } else if (number instanceof BigDecimal && ((BigDecimal) number).compareTo(BigDecimal.ZERO) == 0) {
            return true;
        } else if (number instanceof BigInteger && ((BigInteger) number).compareTo(BigInteger.ZERO) == 0) {
            return true;
        }
        return Double.compare(number.doubleValue(), 0.0) != 0;
    }

}
