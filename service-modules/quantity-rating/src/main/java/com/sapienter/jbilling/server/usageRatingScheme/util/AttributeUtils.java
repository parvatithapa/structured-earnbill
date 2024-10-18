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

package com.sapienter.jbilling.server.usageRatingScheme.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.BigDecimalValidator;

import com.sapienter.jbilling.server.exception.QuantityRatingException;


public class AttributeUtils {


    private static final String PARSING_EXCEPTION="Cannot parse attribute value ";

    public static List<String> validateAttributes(Map<String, String> attributes, String schemeName,
                                                  List<IAttributeDefinition> definitions, Integer sequence) {

        List<String> errors = new ArrayList<>();

        for (IAttributeDefinition definition : definitions) {
            String value = attributes.get(definition.getName());
            validateAttribute(schemeName, value, errors, definition, sequence);
        }

        return errors;
    }

    private static void validateAttribute(String schemeName, String value, List<String> errors,
                                          IAttributeDefinition definition, Integer sequence) {

        if (definition.isRequired() && StringUtils.isBlank(value)) {
            errors.add(String.format("%s - Field '%s' is mandatory.",
                    schemeName, definition.getName(),
                    (sequence != null) ? "in row " + sequence : ""));

            return;
        }

        try {
            switch (definition.getType()) {
                case INTEGER:
                    parseInteger(value);
                    break;
                case DECIMAL:
                    parseDecimal(value);
                    break;
            }
        } catch (QuantityRatingException validationException) {
            errors.add(String.format("%s - For Field '%s' %s",
                    schemeName, definition.getName(),
                    validationException.getErrorMessages()[0]));
        }
    }

    public static Integer parseInteger(String value) {
        if (!StringUtils.isBlank(value)) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new QuantityRatingException(new StringBuilder().append(PARSING_EXCEPTION)
                  .append("'")
                  .append(value)
                  .append( "' as an integer.").toString(),
                        e, new String[] { "Value is not an integer." });
            }
        }
        return null;
    }

    public static BigDecimal parseDecimal(String value) {
        if (!StringUtils.isBlank(value)) {
            try {
                BigDecimal decimalValue = new BigDecimal(value);
                    if (!isValidBigDecimalValue(decimalValue)) {
                        throw new QuantityRatingException(new StringBuilder().append(PARSING_EXCEPTION)
                          .append("'")
                          .append(decimalValue)
                          .append("' as a decimal number.").toString()
                                , new String[] { "Value is not a valid decimal number." });
                    }

                return decimalValue;
            } catch (NumberFormatException e) {
                throw new QuantityRatingException(new StringBuilder().append(PARSING_EXCEPTION)
                  .append("'")
                  .append(value)
                  .append("' as a decimal number.").toString() ,
                       e, new String[] { "Value is not a decimal number." });
            }
        }
        return null;
    }

    private static boolean isValidBigDecimalValue(BigDecimal value) {
        BigDecimalValidator validator = new BigDecimalValidator();
        Double min = -999999999999.9999999999d;
        Double max = +999999999999.9999999999d; //12 integer, 10 fraction

        return validator.isInRange(value, min, max);
    }
}
