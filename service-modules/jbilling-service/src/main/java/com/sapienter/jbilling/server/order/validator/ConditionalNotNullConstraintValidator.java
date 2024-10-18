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

package com.sapienter.jbilling.server.order.validator;

import com.sapienter.jbilling.common.FormatLogger;

import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * Discount Order Line Type Validator
 *
 * @author Mahesh Shivarkar
 * @since 10/12/2018
 */
public class ConditionalNotNullConstraintValidator implements ConstraintValidator<ConditionalNotNullConstraint, Object> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ConditionalNotNullConstraintValidator.class));

    private String orderLineItem;
    private String orderLineType;

    public void initialize(final ConditionalNotNullConstraint orderLine) {
    	orderLineItem = orderLine.item();
    	orderLineType = orderLine.type();
    }

    public boolean isValid(Object object, ConstraintValidatorContext constraintValidatorContext) {
        try {
            Class klass = object.getClass();

            Integer orderLineItemId = (Integer) getAccessorMethod(klass, orderLineItem).invoke(object);
            Integer orderLineTypeId = (Integer) getAccessorMethod(klass, orderLineType).invoke(object);
            
            String className = klass.getSimpleName();
            LOG.debug("className: "+className);
            LOG.debug("orderLineItemId: "+orderLineItemId);
            LOG.debug("orderLineTypeId: "+orderLineTypeId);
            
            if(className.equals("OrderLineWS")) {
                if (orderLineItemId == null && orderLineTypeId == 4) {
                	return true;
                } else if (orderLineItemId == null && orderLineTypeId != 4) {
                	return false;
                }
            }

        } catch (IllegalAccessException e) {
            LOG.debug("Illegal access to the property fields.");
        } catch (NoSuchMethodException e) {
            LOG.debug("Missing JavaBeans getter/setter methods.");
        } catch (InvocationTargetException e) {
            LOG.debug("Property field cannot be accessed.");
        } catch (ClassCastException e) {
            LOG.debug("Property does not contain a integer object.");
        }

        return true;
    }

    /**
     * Returns the accessor method for the given property name. This assumes
     * that the property follows normal getter/setter naming conventions so that
     * the method name can be resolved introspectively.
     *
     * @param klass class of the target object
     * @param propertyName property name
     * @return accessor method
     */
    public Method getAccessorMethod(Class klass, String propertyName) throws NoSuchMethodException {
        return klass.getMethod("get" + WordUtils.capitalize(propertyName));
    }
}
