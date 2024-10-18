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

import javax.validation.Constraint;
import javax.validation.Payload;

import java.lang.annotation.*;

/**
 * Skip validation of order line, when the order line is a discount line,
 * as discount line doesnot contain an item
 * Created by Mahesh.
 * Date: 10/11/18
 */

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalNotNullConstraintValidator.class)
@Documented

public @interface ConditionalNotNullConstraint {
    String message() default "validation.skip.discount.line";
    /**
     * Field name of the order line item
     * @return order line item
     */
    String item();

    /**
     * Field name of the order line type
     * @return order line type
     */
    String type();
    
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
