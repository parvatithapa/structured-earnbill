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

package com.sapienter.jbilling.server.pricing.util;

import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import org.hibernate.ObjectNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.pricing.strategy.ItemSelectorStrategy;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.BigDecimalValidator;
import org.joda.time.LocalTime;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleModel;
import com.sapienter.jbilling.server.pricing.strategy.PricingStrategy;

/**
 * Simple utilities for parsing price model attributes.
 *
 * @author Brian Cowdery
 * @since 02/02/11
 */
public class AttributeUtils {

    /**
     * Validates that all the required attributes of the given strategy are present and of the
     * correct type.
     *
     * @param attributes attribute map
     * @param strategy   strategy to validate against
     * @throws SessionInternalError if attributes are missing or of an incorrect type
     */
    public static void validateAttributes(Map<String, String> attributes, PricingStrategy strategy)
            throws SessionInternalError {

        String strategyName = strategy.getClass().getSimpleName();
        List<String> errors = new ArrayList<String>();

        for (AttributeDefinition definition : strategy.getAttributeDefinitions()) {
            String name = definition.getName();
            String value = attributes.get(name);

            //validate Item Selector
            validateItemSelector(strategy, value, name, errors);

            // validate required attributes
            if (definition.isRequired() && (value == null || value.trim().equals(""))) {
                errors.add(strategyName + "," + name + ",validation.error.is.required");
            }

            // validate attribute types
            try {
                switch (definition.getType()) {
                    case STRING:
                        // a string is a string...
                        break;
                    case TIME:
                        parseTime(value);
                        break;
                    case INTEGER:                    	
                        parseInteger(value);
                        break;
                    case DECIMAL:                    	
                        parseDecimal(value);
                        break;
                }
            } catch (SessionInternalError validationException) {
                errors.add(strategyName + "," + name + "," + validationException.getErrorMessages()[0]);
            }
        }

        // throw new validation exception with complete error list
        if (!errors.isEmpty()) {
            throw new SessionInternalError(strategyName + " attributes failed validation.",
                    errors.toArray(new String[errors.size()]));
        }
    }

    private static void validateItemSelector(PricingStrategy strategy, String value, String name, List<String> errors) {
        if(strategy.getClass().equals(ItemSelectorStrategy.class) && name.equals("1"))
        {
            try {
                ItemDTO item =new ItemDAS().find(Integer.parseInt(value));
                item.toString();
            }
            catch (Exception nf) {
                errors.add(ItemSelectorStrategy.class.getSimpleName() + "," + name + ",validation.error.item.not.found");
            }
        }
    }

    public static LocalTime getTime(Map<String, String> attributes, String name) {
        return parseTime(attributes.get(name));
    }

    /**
     * Parses the given value as LocalTime. If the value cannot be parsed, an exception will be thrown.
     *
     * @param value value to parse
     * @return parsed LocalTime
     * @throws SessionInternalError if value cannot be parsed as LocalTime
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

    public static Integer getInteger(Map<String, String> attributes, String name) {
        return parseInteger(attributes.get(name));
    }

    /**
     * Overloaded getInteger with checkEmpty boolean argument if passed as true
     * will check if there is an empty string or spaces being passed to parse.
     * @param attributes
     * @param name
     * @param checkEmpty
     * @return
     */
    public static Integer getInteger(Map<String, String> attributes, String name, boolean checkEmpty) {
        return parseInteger(attributes.get(name), checkEmpty);
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
    
    /**
     * Overloaded parseInteger with checkEmpty boolean argument if passed as true
     * will check if there is an empty string or spaces being passed to parse.
     * @param value
     * @param checkEmpty
     * @return
     */
    public static Integer parseInteger(String value, boolean checkEmpty) {
        if (value != null) {
            try {
            	if (checkEmpty && !value.trim().isEmpty())
            		return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new SessionInternalError("Cannot parse attribute value '" + value + "' as an integer.",
                                               new String[] { "validation.error.not.a.integer" });
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
