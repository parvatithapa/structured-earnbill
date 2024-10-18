package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.SessionInternalError;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.BigDecimalValidator;
import org.joda.time.LocalTime;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by marcolin on 29/10/15.
 */
public class ParseHelper {

    /**
     * Parses the given value as LocalTime. If the value cannot be parsed, an exception will be thrown.
     *
     * @param value value to parse
     * @return parsed LocalTime
     * @throws com.sapienter.jbilling.common.SessionInternalError if value cannot be parsed as LocalTime
     */
    public static LocalTime parseTime(String value) {
        String[] time = value.split(":");

        if (time.length != 2)
            throw new SessionInternalError("Cannot parse attribute value '" + value + "' as a time of day.",
                    new String[]{"validation.error.not.time.of.day"});

        try {
            return new LocalTime(Integer.valueOf(time[0]), Integer.valueOf(time[1]));
        } catch (NumberFormatException e) {
            throw new SessionInternalError("Cannot parse attribute value '" + value + "' as a time of day.",
                    new String[]{"validation.error.not.time.of.day"});
        }
    }

    /**
     * Parses the given value as an Integer. If the value cannot be parsed, an exception will be thrown.
     *
     * @param value value to parse
     * @return parsed integer
     * @throws SessionInternalError if value cannot be parsed as an integer
     */
    public static Integer parseInteger(String value) {
        if (value != null) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new SessionInternalError("Cannot parse attribute value '" + value + "' as an integer.",
                        new String[]{"validation.error.not.a.integer"});
            }
        }
        return null;
    }


    public static BigDecimal getDecimal(Map<String, String> attributes, String name) {
        return parseDecimal(attributes.get(name));
    }

    /**
     * Parses the given value as a BigDecimal. If the value cannot be parsed, an exception will be thrown.
     *
     * @param value value to parse
     * @return parsed integer
     * @throws SessionInternalError if value cannot be parsed as an BigDecimal
     */
    public static BigDecimal parseDecimal(String value) {
        if (value != null) {
            try {
                if (StringUtils.isEmpty(value)) {
                    return null;
                } else {
                    //validate decimal attribute's value for range
                    BigDecimal decimalValue = new BigDecimal(value);

                    if (decimalValue != null) {
                        BigDecimalValidator validator = new BigDecimalValidator();
                        Double min = -999999999999.9999999999d;
                        Double max = +999999999999.9999999999d; //12 integer, 10 fraction

                        if(!validator.isInRange(decimalValue, min, max))
                            throw new SessionInternalError("Cannot parse attribute value '" + decimalValue + "' as a decimal number.",
                                    new String[] { "validation.error.invalid.rate.or.fraction" });
                    }
                    return decimalValue;
                }
            } catch (NumberFormatException e) {
                throw new SessionInternalError("Cannot parse attribute value '" + value + "' as a decimal number.",
                        new String[]{"validation.error.not.a.number"});
            }
        }
        return null;
    }
}
