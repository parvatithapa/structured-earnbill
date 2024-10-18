package com.sapienter.jbilling.server.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by andres on 6/2/15.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Validator {

    Type type() default Type.VIEW;
    
    public enum Type {
        VIEW,
        EDIT,
        NONE
    }
    
}
