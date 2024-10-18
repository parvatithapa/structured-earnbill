package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Aspect
@Component
public class SPCMediationCacheInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Cache<String, Object> cache;

    @PostConstruct
    void init() {
        cache =  CacheBuilder.newBuilder()
                .concurrencyLevel(10) // Concurrency level 4 (Thread Safe Cache)
                .removalListener(entry -> logger.debug("removing key {} from cache", entry.getKey()))
                .maximumSize(100000) // maximum records can be cached
                .expireAfterWrite(10, TimeUnit.MINUTES) // cache will expire after 10 minutes.
                .<String,Object>build();
    }

    /**
     * Constructs key for current executing method.
     * @param proceedingJoinPoint
     * @return
     */
    private String constructKey(ProceedingJoinPoint proceedingJoinPoint) {
        String key = proceedingJoinPoint.getSignature().getName();
        if(null!= proceedingJoinPoint.getArgs()) {
            key = key + "-" + Arrays.stream(proceedingJoinPoint.getArgs())
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.joining("-"));
        }
        return key;
    }

    @Around("execution(* com.sapienter.jbilling.server.spc.SPCMediationHelperServiceImpl.*(..))")
    public Object cacheResult(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        if(signature.getMethod().isAnnotationPresent(NoCache.class)) {
            logger.debug("method {} is non cacheable", signature.getName());
            // executing method.
            return proceedingJoinPoint.proceed();
        }
        String key = constructKey(proceedingJoinPoint);
        logger.debug("key {} constructed for method {}", key, signature.getName());
        Object value = cache.getIfPresent(key); // try to fetch from cache.
        if(null!= value) {
            logger.debug("value {} found for key {}", value, key);
        }
        if(null == value) {
            // execute actual method.
            value = proceedingJoinPoint.proceed();
            if(null!= value) {
                // put value on cache.
                logger.debug("putting value {} for key {}", value, key);
                cache.put(key, value);
            }
        }
        return value;
    }

    /**
     * clears cache.
     */
    public void clearCache() {
        logger.debug("before clearing cache size {}", cache.size());
        logger.debug("cache stats {}", cache.stats());
        cache.invalidateAll();
        logger.debug("cleared cache");
        logger.debug("After clearing cache size {}", cache.size());
    }
}
