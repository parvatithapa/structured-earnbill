package com.sapienter.jbilling.server.invoiceTemplate.report;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by Klim on 11.12.13.
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface Field {

    String description() default "";
    FieldType type() default FieldType.Field;
    Class<?> valueClass() default Object.class;
    Class<? extends FieldProcessor> processor() default DefaultFieldProcessor.class;
}
