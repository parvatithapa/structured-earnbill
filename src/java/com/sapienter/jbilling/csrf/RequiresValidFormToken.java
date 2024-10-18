package com.sapienter.jbilling.csrf;

import java.lang.annotation.*;

/**
 * Created by vivek on 20/7/15.
 */

@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresValidFormToken {

    /**
     * Whether to display an error or be silent (default).
     * @return  <code>true</code> if an error should be shown
     */
    boolean error() default false;
}
