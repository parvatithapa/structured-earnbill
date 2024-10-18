package com.sapienter.jbilling.server.quantity.rater;

import static com.sapienter.jbilling.server.util.QuantityRatingConstants.PRECISION_PROPERTY;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.quantity.QuantityRater;
import com.sapienter.jbilling.server.quantity.QuantityRatingContext;
import com.sapienter.jbilling.server.quantity.data.das.RatingUnitDAS;
import com.sapienter.jbilling.server.ratingUnit.domain.RatingUnit;
import com.sapienter.jbilling.server.util.QuantityRatingProperties;


public class RatingUnitBasedQuantityRater implements QuantityRater {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Integer PRECISION;

    @Getter @Setter
    private RatingUnitDAS ratingUnitDAS;

    private LoadingCache<Key, NavigableMap<Date, RatingUnit>> ratingUnitCache;

    static {
        String precisionStr = QuantityRatingProperties.get(PRECISION_PROPERTY, "6");
        PRECISION = Integer.parseInt(precisionStr);
    }

    @PostConstruct
    public void init() {
        ratingUnitCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .build(ratingUnitLoader);
    }


    @Override
    public void reset() {
        this.ratingUnitCache.invalidateAll();
    }


    @Override
    public final BigDecimal rate(BigDecimal quantity, QuantityRatingContext context) throws  IllegalStateException {
        try {
            resolveRatingUnit(context);
            return calculate(quantity, context);

        } catch (SessionInternalError e) {
            logger.error("Failed to convert quantity", e);
            throw new IllegalStateException(e);
        }
    }


    private BigDecimal calculate(BigDecimal quantity, QuantityRatingContext context) {

        RatingUnit ratingUnit = context.getRatingUnit();

        if (ratingUnit == RatingUnit.NONE || ratingUnit == null) {
            return quantity;
        }

        BigDecimal ratedQuantity = rate(ratingUnit, quantity);

        logger.info("Product= {}, Rating Unit= {}, Input Quantity= {}, Rated Quantity= {}",
            context.getItemId(), ratingUnit.getName(), quantity, ratedQuantity);

        return ratedQuantity;
    }


    private void resolveRatingUnit(QuantityRatingContext context) {

        RatingUnit ratingUnit = RatingUnit.NONE;
        try {
            ratingUnit = getRatingUnitForDate(context.getEntityId(),
                    context.getItemId(), context.getEventDate());
        } catch (ExecutionException e) {
            logger.error("Fatal: error resolving rating scheme", e);
        }

        context.setRatingUnit(ratingUnit);
    }


    private RatingUnit getRatingUnitForDate(Integer entityId, Integer itemId, Date date)
            throws ExecutionException {

        NavigableMap<Date, RatingUnit> ratingUnits =
                ratingUnitCache.get(new Key(entityId, itemId));

        if (MapUtils.isEmpty(ratingUnits)) {
            logger.debug("rating units null or empty.");
            return RatingUnit.NONE;
        }

        if (date == null) {
            logger.debug("returning the oldest rating unit");
            return ratingUnits.get(ratingUnits.firstKey());
        }

        Map.Entry<Date, RatingUnit> e = ratingUnits.floorEntry(date);
        if (e == null) {
            logger.debug("No rating unit is effective");
            return RatingUnit.NONE;
        }

        logger.info("Effective Rating unit for date {} : {}", e.getKey(), e.getValue());
        return e.getValue();
    }


    static BigDecimal rate(RatingUnit ratingUnit, BigDecimal quantity) {

        BigDecimal divisor = ratingUnit.getIncrementUnitQuantity();
        if (divisor != null && divisor.compareTo(BigDecimal.ZERO) > 0) {
            return quantity.divide(divisor, PRECISION, RoundingMode.UP);
        }
        return quantity;
    }


    private final CacheLoader ratingUnitLoader = new CacheLoader<Key, NavigableMap<Date, RatingUnit>>() {
        @Override
        public NavigableMap<Date, RatingUnit> load(Key key) {
            try {
                return ratingUnitDAS.getRatingUnit(key.entityId, key.itemId);
            } catch (Exception e) {
                logger.error("Failed to load rating unit",e);
                throw e;
            }
        }
    };


    // Cache Key
    private final class Key {
        private final Integer entityId;
        private final Integer itemId;

        Key(Integer entityId, Integer itemId) {
            this.entityId = entityId;
            this.itemId = itemId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(entityId, key.entityId) &&
                    Objects.equals(itemId, key.itemId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityId, itemId);
        }
    }
}
