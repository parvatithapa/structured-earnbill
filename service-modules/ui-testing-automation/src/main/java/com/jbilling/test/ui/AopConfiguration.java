package com.jbilling.test.ui;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.interceptor.AbstractTraceInterceptor;
import org.springframework.aop.interceptor.CustomizableTraceInterceptor;
import org.springframework.aop.interceptor.PerformanceMonitorInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableAspectJAutoProxy
@Aspect
@Profile("trace")
public class AopConfiguration {

    @Bean
    public AbstractTraceInterceptor traceInterceptor () {

        CustomizableTraceInterceptor interceptor = new CustomizableTraceInterceptor();
        // @formatter:off
        interceptor.setEnterMessage(    "Enter - $[targetClassShortName].$[methodName]($[arguments]).");
        interceptor.setExitMessage(     "Exit  - $[targetClassShortName].$[methodName]($[arguments]) with return value $[returnValue], took $[invocationTime]ms.");
        interceptor.setExceptionMessage("Exit! - $[targetClassShortName].$[methodName]($[arguments]) with exception ($[exception])");
        // @formatter:on
        interceptor.setLoggerName("tracer");

        return interceptor;
    }

    /** Pointcut for execution of public methods of com.jbilling.test.ui and subpackages */
    @Pointcut("execution(public * com.jbilling.framework..*.*(..))")
    public void comJbillingFrameworksMethods () {
    }

    /** Pointcut for execution of public methods of com.jbilling.test.ui and subpackages */
    @Pointcut("execution(public * com.jbilling.test.ui..*.*(..))")
    public void comJbillingTestUiMethods () {
    }

    @Pointcut("comJbillingTestUiMethods() || comJbillingFrameworksMethods()")
    public void methodsTracer () {
    }

    @Bean
    public Advisor traceAdvisor () {

        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("com.jbilling.test.ui.AopConfiguration.methodsTracer()");
        return new DefaultPointcutAdvisor(pointcut, traceInterceptor());
    }

    @Pointcut("comJbillingTestUiMethods()")
    public void performanceMonitor () {
    }

    @Bean
    public AbstractTraceInterceptor performanceMonitorInterceptor () {
        PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor();
        interceptor.setLoggerName("tracer");
        return interceptor;
    }

    @Bean
    public Advisor performanceMonitorAdvisor () {

        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("com.jbilling.test.ui.AopConfiguration.performanceMonitor()");
        return new DefaultPointcutAdvisor(pointcut, performanceMonitorInterceptor());
    }
}
