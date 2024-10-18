package com.sapienter.jbilling.server.config;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class SpringAsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "asyncTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(10);
        executor.setCorePoolSize(4);
        executor.setThreadNamePrefix("Async-Worker-Thread-");
        executor.initialize();
        executor.afterPropertiesSet();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {

            private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

            @Override
            public void handleUncaughtException(Throwable error, Method method, Object... args) {
                logger.debug("Exception message - [{}]", error.getMessage());
                logger.debug("Method name - [{}]", method.getName());
            }
        };
    }

}