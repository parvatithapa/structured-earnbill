package com.sapienter.jbilling.server.quantity.rater;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.exception.QuantityRatingException;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.quantity.QuantityRater;
import com.sapienter.jbilling.server.quantity.QuantityRatingContext;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageQueryRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.UsageQueryRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.UsageRecord;
import com.sapienter.jbilling.server.ratingUnit.domain.RatingUnit;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageConfiguration;
import com.sapienter.jbilling.server.usageRatingScheme.scheme.IUsageRatingScheme;
import com.sapienter.jbilling.server.usageratingscheme.service.UsageRatingSchemeBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;

@Service
public class RatingSchemeBasedQuantityRater implements QuantityRater {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ConcurrentMap<String, BigDecimal> processLocalUsageCache;
    private LoadingCache<Integer, NavigableMap<Date, IUsageRatingSchemeModel>> ratingSchemeCache;

    @PostConstruct
    @Override
    public void init() {
        processLocalUsageCache = new ConcurrentHashMap<>(128, 0.8f, 4);
        ratingSchemeCache = CacheBuilder.newBuilder()
                .concurrencyLevel(4)
                .build(ratingSchemeLoader);
    }

    @Override
    public void reset() {
        this.processLocalUsageCache.clear();
        this.ratingSchemeCache.invalidateAll();
    }


    @Override
    public final BigDecimal rate(BigDecimal quantity, QuantityRatingContext context) throws IllegalStateException {

        BigDecimal result;
        try {
            resolveRatingScheme(context);
            result = calculate(quantity, context);

        } catch (SessionInternalError e) {
            logger.error("Failed to convert quantity", e);
            throw new IllegalStateException(e);
        }

        logger.info("Resolved quantity: {}", result);
        return result;
    }

    private BigDecimal calculate(BigDecimal quantity, QuantityRatingContext context) {

        IUsageRatingSchemeModel ratingSchemeModel = context.getRatingScheme();

        if (ratingSchemeModel == null) {
            logger.info("NO rating scheme for item {}, resolved quantity {}",
                    context.getItemId(), quantity);

            return quantity;
        }

        IUsageRatingScheme ratingScheme = ratingSchemeModel.getRatingScheme();

        IUsageConfiguration usageConfig = ratingScheme.getUsageConfiguration();
        IUsageRecord usage = UsageRecord.ZERO;

        if (usageConfig.requiresUsage()) {
            logger.info("Rating scheme needs usage till date.");
            usage = getUsage(quantity, context);
        }

        logger.debug("Proceeding to resolve quantity, rating-scheme-model: {}, quantity: {}",
                ratingSchemeModel, quantity);

        return ratingScheme.compute(ratingSchemeModel, quantity,
                usage, context.getPricingFields());
    }


    private void resolveRatingScheme(QuantityRatingContext context) {

        IUsageRatingSchemeModel ratingScheme = null;
        try {
            ratingScheme = getRatingScheme(context.getItemId(),
                    context.getEventDate());
        } catch (ExecutionException e) {
            logger.error("Fatal: error resolving rating scheme", e);
        }

        context.setRatingScheme(ratingScheme);
    }


    private IUsageRatingSchemeModel getRatingScheme(Integer itemId, Date eventDate) throws ExecutionException {

        NavigableMap<Date, IUsageRatingSchemeModel> ratingSchemes = ratingSchemeCache.get(itemId);

        if (MapUtils.isEmpty(ratingSchemes)) {
            logger.debug("rating scheme null or empty.");
            return null;
        }

        IUsageRatingSchemeModel model;
        if (eventDate == null) {
            logger.debug("returning the oldest rating scheme");
            model = ratingSchemes.get(ratingSchemes.firstKey());

        } else {
            Map.Entry<Date, IUsageRatingSchemeModel> e = ratingSchemes.floorEntry(eventDate);
            if (e == null) {
                logger.debug("No rating scheme is effective");
                return null;
            }

            logger.info("Effective Rating scheme for date {} : {}", e.getKey(), e.getValue());
            model = e.getValue();
        }

        return model;
    }


    private IUsageRecord getUsage(BigDecimal quantity, QuantityRatingContext context) {

        MainSubscriptionDTO mainSubscription = getUserMainSubscription(context.getUserId());
        Date startDate = getStartDate(mainSubscription, context.getEventDate(), context.getRatingScheme());

        IUsageQueryRecord queryRecord = UsageQueryRecord.builder()
                .item(context.getItemId())
                .user(context.getUserId())
                .entity(context.getEntityId())
                .startDate(startDate)
                .endDate(getEndDate(mainSubscription, startDate, context.getRatingScheme()))
                .resource(context.getResourceId())
                .mediationProcessId(context.getMediationProcessId())
                .build();

        IUsageRecord usage = getRawUsage(context, queryRecord);
        logger.debug("Raw usage till date: {}", usage.getQuantity());

        if (context.getRatingUnit() != RatingUnit.NONE) {
            BigDecimal ratedUsageQty = RatingUnitBasedQuantityRater
                    .rate(context.getRatingUnit(), usage.getQuantity());
            usage = UsageRecord.withNewQuantity(usage, ratedUsageQty);
            logger.info("Rated usage till date (before this process): {}", usage.getQuantity());
        }

        BigDecimal currentProcessQty = getUsageFromCache(quantity, queryRecord);
        usage = UsageRecord.withNewQuantity(usage, currentProcessQty.add(usage.getQuantity()));
        logger.info("Total raw usage till date: {}", usage.getQuantity());

        return usage;
    }

    private Date getStartDate(
            MainSubscriptionDTO mainSubscription,
            Date eventDate,
            IUsageRatingSchemeModel model) {

        if (model.getRatingScheme().getUsageConfiguration().hasResetCycle()) {
            return model.getRatingScheme().getUsageConfiguration().getCycleStartDate(model, eventDate);
        } else {
            return getCycleStartDate(eventDate, mainSubscription);
        }
    }

    private Date getEndDate(
            MainSubscriptionDTO mainSubscription,
            Date startDate,
            IUsageRatingSchemeModel model) {

        if (model.getRatingScheme().getUsageConfiguration().hasResetCycle()) {
            return model.getRatingScheme().getUsageConfiguration().getCycleEndDate(model, startDate);
        } else {
            return getCycleEndDate(startDate, mainSubscription);
        }
    }

    private IUsageRecord getRawUsage(QuantityRatingContext context, IUsageQueryRecord queryRecord) {
        Optional<IUsageRecord> usageOpt = context.getRatingScheme().getRatingScheme()
                .getUsageConfiguration().getUsage(context.getRatingScheme(), queryRecord);

        IUsageRecord usage;
        if (!usageOpt.isPresent() || (usage = usageOpt.get()).getQuantity() == null) {
            logger.debug("No usage so far !");
            usage = UsageRecord.ZERO;
        }
        return usage;
    }

    private BigDecimal getUsageFromCache(BigDecimal quantity, IUsageQueryRecord queryRecord) {
        String key = queryRecord.getKey();
        logger.debug("Key: {}", key);

        BigDecimal result = processLocalUsageCache.compute(key, (k, v) ->
                v == null ? quantity : v.add(quantity));
        result = result.subtract(quantity);

        logger.info("Usage in this process {}", result);
        return result;
    }

    private final CacheLoader ratingSchemeLoader = new CacheLoader<Integer,
            NavigableMap<Date, IUsageRatingSchemeModel>>() {

        @Override
        public NavigableMap<Date, IUsageRatingSchemeModel> load(Integer itemId) {
            try {
                return new UsageRatingSchemeBL().findAllByItem(itemId);
            } catch (Exception e) {
                logger.error("Failed to load rating scheme",e);
                throw e;
            }
        }
    };


    private MainSubscriptionDTO getUserMainSubscription(Integer userId) {
        CustomerDTO customer = new UserDAS().find(userId).getCustomer();
        if (customer == null) {
           throw new QuantityRatingException(new StringBuilder().append("(Fatal) Customer unavailable, userId: ")
             .append(userId)
             .toString());
        }

        MainSubscriptionDTO mainSubscription = customer.getMainSubscription();
        if (mainSubscription == null) {
            throw new QuantityRatingException(new StringBuilder().append("(Fatal) Main subscription unavailable, userId: ")
              .append(userId)
              .toString());
        }

        logger.debug("Main subscription : {}", mainSubscription);
        return mainSubscription;
    }


    private Date getCycleStartDate(Date eventDate, MainSubscriptionDTO mainSubscription) {
        Integer dayOfPeriod = mainSubscription.getNextInvoiceDayOfPeriod();
        OrderPeriodDTO period = mainSubscription.getSubscriptionPeriod();

        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(eventDate);

        Integer eventDay = cal.get(MapPeriodToCalendar.mapDayOfPeriod(period.getUnitId()));

        if (eventDay < dayOfPeriod) {
            if (Calendar.MONTH == MapPeriodToCalendar.map(period.getUnitId())) {
                int maxDay = getMonthMaxDay(eventDate);
                cal.set(Calendar.DAY_OF_MONTH, Integer.min(dayOfPeriod, maxDay));
                cal.add(Calendar.MONTH, -1);
                if (dayOfPeriod > maxDay) {
                    cal.set(Calendar.DAY_OF_MONTH, dayOfPeriod);
                }
            } else {
                cal.set(MapPeriodToCalendar.mapDayOfPeriod(period.getUnitId()), dayOfPeriod);
                cal.add(MapPeriodToCalendar.map(period.getUnitId()), -1);
            }
        } else {
            if (Calendar.DAY_OF_YEAR != MapPeriodToCalendar.map(period.getUnitId())) {
                cal.set(MapPeriodToCalendar.mapDayOfPeriod(period.getUnitId()), dayOfPeriod);
            }
        }

        return Util.truncateDate(cal.getTime());
    }


    private Date getCycleEndDate(Date cycleStartDate, MainSubscriptionDTO mainSubscription) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(cycleStartDate);

        OrderPeriodDTO period = mainSubscription.getSubscriptionPeriod();
        calendar.add(MapPeriodToCalendar.map(period.getUnitId()), 1);

        return calendar.getTime();
    }


    private Integer getMonthMaxDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
}
