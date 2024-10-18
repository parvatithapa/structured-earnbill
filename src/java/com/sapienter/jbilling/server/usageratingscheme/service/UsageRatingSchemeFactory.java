package com.sapienter.jbilling.server.usageratingscheme.service;


import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme;
import com.sapienter.jbilling.server.usageratingscheme.domain.entity.UsageRatingSchemeTypeDTO;
import com.sapienter.jbilling.server.usageratingscheme.domain.repository.UsageRatingSchemeTypeDAS;

/**
 * Light-weight cache for getting the {@link com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme}
 * instances. It caches instance if they are defined "cacheable" else a new instance is created on each request.
 */
public class UsageRatingSchemeFactory {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String BEAN_NAME = "ratingSchemeFactory";

    private static final LoadingCache<String, CacheElement> cache;

    static {
        cache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .maximumSize(100)
                .build(new CacheLoader<String, CacheElement>() {
                    @Override
                    public CacheElement load(String name) {
                        return loadAndGet(name);
                    }
                });
    }

    public static IUsageRatingScheme getInstance(final String name) {

        CacheElement e;
        try {
            e = cache.get(name);

        } catch (ExecutionException ex) {
            throw new QuantityRatingException("Error retrieving scheme", ex.getCause());
        }

        if (e.isCacheable()) {
            return e.getInstance();

        } else {
            return createInstance(e.getKlass());
        }
    }

    private static CacheElement createCacheElement(UsageRatingSchemeTypeDTO type) {
        CacheElement e = new CacheElement(
                getClass(type.getImplClass()), type.isCacheable());

        if (type.isCacheable()) {
            e.setInstance(createInstance(e.getKlass()));
        }

        return e;
    }

    private static CacheElement loadAndGet(String name) {
        UsageRatingSchemeTypeDTO type = loadRatingSchemeType(name);
        CacheElement e = createCacheElement(type);
        cache.put(name, e);

        return e;
    }

    private static UsageRatingSchemeTypeDTO loadRatingSchemeType(String name) {
        UsageRatingSchemeTypeDAS das = new UsageRatingSchemeTypeDAS();
        return Optional
                .ofNullable(das.findOneByName(name))
                .orElseThrow(() ->
                        new QuantityRatingException("Rating scheme Not found with name " + name)
                );
    }

    private static Class<IUsageRatingScheme> getClass(String className) {

        Class ratingSchemeClass = loadClass(className);

        logger.info("Class {}", ratingSchemeClass);
        if (!IUsageRatingScheme.class.isAssignableFrom(ratingSchemeClass)) {
            throw new QuantityRatingException("Fatal - class doesn't implement interface IUsageRatingScheme");
        }

        return (Class<IUsageRatingScheme>) ratingSchemeClass;
    }

    private static IUsageRatingScheme createInstance(Class<IUsageRatingScheme> klass) {
        try {
            return klass.newInstance();

        } catch(InstantiationException | IllegalAccessException e) {
            throw new QuantityRatingException("Exception while instantiating rating scheme instance", e);
        }
    }

    private static Class loadClass(String className) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            return loader.loadClass(className);

        } catch (ClassNotFoundException e) {
            logger.info("Cannot load class from the current thread context class loader.");
        }

        try {
            return Class.forName(className);

        } catch (ClassNotFoundException e) {
            logger.error("Cannot load class from the caller class loader.", e);
        }
        return null;
    }

    private static class CacheElement {

        private Class<IUsageRatingScheme> klass;
        private IUsageRatingScheme instance;
        private boolean cacheable;

        public CacheElement(Class<IUsageRatingScheme> klass, boolean cacheable) {
            this.klass = klass;
            this.cacheable = cacheable;
        }

        public void setInstance(IUsageRatingScheme instance) {
            this.instance = instance;
        }

        public Class<IUsageRatingScheme> getKlass() {
            return klass;
        }

        public IUsageRatingScheme getInstance() {
            return instance;
        }

        public boolean isCacheable() {
            return cacheable;
        }
    }
}
