package com.sapienter.jbilling.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RetryConcurrentOperation {
 
    /**
     * Specify exception for which operation should be retried.
     */
    Class<? extends Throwable>[] on();
 
    /**
     * Sets the number of times to retry the operation. The default is 1, means it will execute at least once.
     */
    int retries() default 1;
    
    
}
