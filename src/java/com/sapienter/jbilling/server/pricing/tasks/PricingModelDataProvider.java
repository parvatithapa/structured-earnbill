package com.sapienter.jbilling.server.pricing.tasks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sapienter.jbilling.server.mediation.cache.CacheProvider;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;


@Component
public final class PricingModelDataProvider implements CacheProvider {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String BEAN_NAME = "pricingModelDataProvider";

    private PricingModelPersistentDataProvider persistentDataProvider;

    private Cache<String, NavigableMap<Date, PriceModelDTO>> priceModelCache;

    @PostConstruct
    @Override
    public void init() {
        priceModelCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .build();
    }

    @Override
    public void reset() {
        this.priceModelCache.invalidateAll();
    }


    NavigableMap<Date, PriceModelDTO> getProductPriceModel(Integer itemId, Integer entityId, boolean useCache) {

        NavigableMap<Date, PriceModelDTO> models;
        models = getPriceModel(
                () -> "DE-" + itemId + "-" + entityId,
                () -> loadPriceModels(() -> persistentDataProvider.getProductPrice(itemId, entityId)),
                useCache);

        if(MapUtils.isEmpty(models)) {
            logger.debug("No company specific price model was found. Searching for global price.");
            models = getPriceModel(
                    () -> "G-" + itemId,
                    () -> loadPriceModels(() -> persistentDataProvider.getProductGlobalPrice(itemId)),
                    useCache);
        }

        return models;
    }

    NavigableMap<Date, PriceModelDTO> getAccountTypePriceModel(
            CustomerDTO customer,
            Integer itemId,
            Date pricingDate,
            boolean useCache) {

        String key = new StringBuilder("AT-")
                .append(customer.getAccountType().getId())
                .append('-').append(itemId).append('-')
                .append(new SimpleDateFormat("yyyyMMdd").format(pricingDate))
                .toString();

        return getPriceModel(() -> key,
                () -> loadPriceModels(() ->
                        persistentDataProvider.getAccountTypePriceModel(customer, itemId, pricingDate)
                ),
                useCache);
    }

    NavigableMap<Date, PriceModelDTO> getCustomerPriceModel(
            Integer userId,
            Integer itemId,
            Date pricingDate,
            boolean useAttributes,
            Map<String, String> attributes,
            boolean useWildcardAttributes,
            boolean useCache) {

        StringBuilder key = new StringBuilder("CP-")
                .append(userId).append('-')
                .append(itemId).append('-')
                .append(new SimpleDateFormat("yyyyMMdd").format(pricingDate));

        if (useAttributes) {
            Set<String> attrSet = new TreeSet<>(attributes.values());
            key.append(attrSet.toString());
        }

        return getPriceModel(() -> key.toString(),
                () -> loadPriceModels(
                        () -> persistentDataProvider.getCustomerPriceModel(userId, itemId,
                                pricingDate, useAttributes, attributes, useWildcardAttributes)
                ),
                useCache);
    }

    NavigableMap<Date, PriceModelDTO> getCustomerPlanPriceModel(
            Integer userId,
            Integer itemId,
            Date pricingDate,
            boolean useAttributes,
            Map<String, String> attributes,
            boolean useWildcardAttributes,
            Boolean planPricingOnly,
            boolean useCache,
            Integer planId) {

        StringBuilder key = new StringBuilder("PP-")
                .append(userId).append('-')
                .append(itemId).append('-')
                .append(planPricingOnly).append('-')
                .append(planId).append('-')
                .append(new SimpleDateFormat("yyyyMMdd").format(pricingDate));

        if (useAttributes) {
            Set<String> attrSet = new TreeSet<>(attributes.values());
            key.append(attrSet.toString());
        }

        return getPriceModel(() -> key.toString(),
                () -> loadPriceModels(() ->
                        persistentDataProvider.getCustomersPlanPriceModel(userId, itemId,
                                pricingDate, useAttributes, attributes,
                                useWildcardAttributes, planPricingOnly, planId)
                ),
                useCache);
    }

    private NavigableMap<Date, PriceModelDTO> getPriceModel(
            Supplier<String> key,
            Callable<NavigableMap<Date, PriceModelDTO>> callable,
            boolean useCache) {

        try {
            if (useCache) {
                return priceModelCache.get(key.get(), callable);

            } else {
                return callable.call();
            }
        } catch (Exception e) {
            logger.error("Fatal: error retrieving pricing models", e);
            return null;
        }
    }

    // This additional level of redirection has been done to avoid the pesky
    // InvalidCacheLoadException when the CacheLoader loads a NULL value.
    private NavigableMap<Date, PriceModelDTO> loadPriceModels(
            Supplier<NavigableMap<Date, PriceModelDTO>> priceModelSupplier) {

        NavigableMap<Date, PriceModelDTO> models = priceModelSupplier.get();
        if (models == null) {
            models = new TreeMap<>();
        }

        return models;
    }

    public void setPersistentDataProvider(PricingModelPersistentDataProvider persistentDataProvider) {
        this.persistentDataProvider = persistentDataProvider;
    }
}
