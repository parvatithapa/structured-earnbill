package com.gurock.testrail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by branko on 6/6/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestRailClass {

    String suite() default "";
    String parent() default "";
    String description() default "";
}
