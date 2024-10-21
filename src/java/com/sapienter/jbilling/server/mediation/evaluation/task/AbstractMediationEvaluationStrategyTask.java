package com.sapienter.jbilling.server.mediation.evaluation.task;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

public abstract class AbstractMediationEvaluationStrategyTask extends PluggableTask implements IMediationEvaluationStrategyTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected EventLogger eLogger = EventLogger.getInstance();
    protected MediationEvaluationStrategyData data;
    protected OrderBL orderBL = null;

    AbstractMediationEvaluationStrategyTask() {
        data = new MediationEvaluationStrategyData();
        data.setCache(Context.getBean(Context.Name.CACHE));
        data.setCacheModel(Context.getBean(Context.Name.CACHE_MODEL_RW));
    }

    abstract Date calculateActualEventDate();
    abstract String buildCacheKey(boolean newOrderPerMediationRun);
    abstract Date getActiveSinceDate(Map<Integer, Map<String,Date>> activeSinceDateMapByUser);

    public Integer getCurrent(Integer userId, Date eventDate, Integer itemId, String mediationProcessId,
            OrderDTO subscriptionOrder, String assetIdentifier) {
        logger.debug("getting the current order");
        bindData(userId, eventDate, itemId, mediationProcessId, subscriptionOrder, assetIdentifier);
        Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(getEntityId(), com.sapienter.jbilling.client.util.Constants.PREFERENCE_ONE_ORDER_PER_MEDIATION_RUN);
        boolean newOrderPerMediationRun = mediationProcessId != null && Integer.valueOf(1).equals(prefValue);
        // find in the cache
        String cacheKey = buildCacheKey(newOrderPerMediationRun);

        Integer retValue = (Integer) data.getCache().getFromCache(cacheKey, data.getCacheModel());
        logger.debug("Retrieved from cache {}, order id: {}", cacheKey, retValue);

        // a hit is only a hit if the order is still active and is not deleted. Sometimes when the order gets deleted
        // it wouldn't be removed from the cache.
        OrderDTO cachedOrder = new OrderDAS().findByIdAndIsDeleted(retValue, false);
        if (null != cachedOrder && OrderStatusFlag.INVOICE.equals(cachedOrder.getOrderStatus().getOrderStatusFlag())
                && cachedOrder.getIsMediated()) {
            logger.debug("Cache hit for {}", retValue);
            return retValue;
        }

        UserBL userBL = new UserBL(userId);
        MainSubscriptionDTO mainSubscription = userBL.getEntity().getCustomer().getMainSubscription();
        Integer entityId = null;
        Integer currencyId = null;
        if (null == mainSubscription) {
            return null;
        }

        // find user entity & currency
        try {
            entityId = userBL.getEntity().getCompany().getId();
            currencyId = userBL.getEntity().getCurrency().getId();
        } catch (Exception e) {
            throw new SessionInternalError("Error looking for user entity of currency",AbstractMediationEvaluationStrategyTask.class, e);
        }
        // if main subscription preference is not set 
        // do not use the main subscription
        if (!isMainSubscriptionUsed(entityId)) {
            return null;
        }

        // loop through future periods until we find a usable current order
        int futurePeriods = 0;
        boolean orderFound = false;
        // create the order
        if (null == orderBL) {
            orderBL = new OrderBL();
        }
        Map<Integer, Map<String,Date>> activeSinceDateMapByUser = new HashMap<>();
        do {
            Date activeSinceDate = getActiveSinceDate(activeSinceDateMapByUser);
            Date newOrderDate = calculateDate(futurePeriods, mainSubscription);
            logger.debug("Calculated one time date: {}, for future periods: {}", newOrderDate, futurePeriods);

            if (null != activeSinceDate && eventDate.compareTo(activeSinceDate) >= 0
                    && activeSinceDate.after(newOrderDate)) {
                newOrderDate = activeSinceDate;
            }

            if (null == newOrderDate) {
                // this is an error, there isn't a good date give the event date and
                // the main subscription order
                logger.error("Could not calculate order date for event. Event date is before the order active since date.");
                return null;
            }

            // now that the date is set, let's see if there is a one-time order for that date
            boolean somePresent = false;
            if(!newOrderPerMediationRun) {
                try {
                    List<OrderDTO> rows = new OrderDAS().findOneTimersByDate(userId, newOrderDate, Boolean.TRUE);
                    logger.debug("Found {} one-time orders for new order date: {}", rows.size(), newOrderDate);
                    Optional<OrderDTO> found = rows.stream()
                                                   .findFirst();
                    if(found.isPresent()) {
                        somePresent = true;
                        orderBL.set(found.get().getId());
                        logger.debug("Found existing one-time order");
                        orderFound = true;
                    }
                } catch (Exception e) {
                    throw new SessionInternalError("Error looking for one time orders", AbstractMediationEvaluationStrategyTask.class, e);
                }
            }
            if (somePresent && !orderFound) {
                eLogger.auditBySystem(entityId, userId,
                                      Constants.TABLE_PUCHASE_ORDER,
                                      orderBL.getEntity().getId(),
                                      EventLogger.MODULE_MEDIATION,
                                      EventLogger.CURRENT_ORDER_FINISHED,
                                      null, null, null);

            } else if (!somePresent) {
                // there aren't any one-time orders for this date at all, create one
                create(newOrderDate, currencyId, entityId);
                orderFound = true;
                logger.debug("Created new one-time order");
            }

            // non present -> create new one with correct date
            // some present & none found -> try next date
            // some present & found -> use the found one
            futurePeriods++;
        } while (!orderFound);  
        // the result is in 'order'
        retValue = orderBL.getEntity().getId();

        logger.debug("Caching order {} with key {}", retValue, cacheKey);
        data.getCache().putInCache(cacheKey, data.getCacheModel(), retValue);

        logger.debug("Returning {}", retValue);
        return retValue;
    }

    private boolean isMainSubscriptionUsed(Integer entityId) {
        logger.debug("checking for main subscription used");
        int preferenceUseCurrentOrder = 0;
        try {
            preferenceUseCurrentOrder = 
                PreferenceBL.getPreferenceValueAsIntegerOrZero(
                        entityId, Constants.PREFERENCE_USE_CURRENT_ORDER);
        } catch (EmptyResultDataAccessException e) {
            logger.error("error in isMainSubscriptionUsed {}", e);
            // default preference will be used
        }
        return preferenceUseCurrentOrder != 0;
    }

    public Integer create(Date activeSince, Integer currencyId, Integer entityId) {
        logger.debug("creating the order for activeSince={}, currencyId={}, entityId={}", activeSince, currencyId, entityId);
        OrderDTO currentOrder = new OrderDTO();
        currentOrder.setCurrency(new CurrencyDTO(currencyId));
        currentOrder.setIsMediated(Boolean.TRUE);

        // notes
        try {
            EntityBL entity = new EntityBL(entityId);
            ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", entity.getLocale());
            currentOrder.setNotes(bundle.getString("order.current.notes"));
        } catch (Exception e) {
            throw new SessionInternalError("Error setting the new order notes", AbstractMediationEvaluationStrategyTask.class, e);
        } 

        currentOrder.setActiveSince(activeSince);
        // create the order
        if (orderBL == null) {
            orderBL = new OrderBL();
        }

        orderBL.set(currentOrder);
        orderBL.addRelationships(data.getUserId(), Constants.ORDER_PERIOD_ONCE, currencyId);

        return orderBL.create(entityId, null, currentOrder);
    }

    private Date calculateDate(int futurePeriods, MainSubscriptionDTO mainSubscription) {
        GregorianCalendar cal = new GregorianCalendar();

        logger.debug("To begin with eventDate is {}", data.getEventDate());

        // calculate the event date with the added future periods
        // default cal to actual event date
        Date actualEventDate = calculateActualEventDate();
        cal.setTime(actualEventDate);
        for (int f = 0; f < futurePeriods; f++) {
            if (CalendarUtils.isSemiMonthlyPeriod(mainSubscription.getSubscriptionPeriod().getPeriodUnit())) {
                cal.setTime(CalendarUtils.addSemiMonthyPeriod(cal.getTime()));
            } else {
                cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()), 
                                            mainSubscription.getSubscriptionPeriod().getValue());
            }
        }
        // set actual event date based on future periods
        actualEventDate = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.DATE, mainSubscription.getNextInvoiceDayOfPeriod() - 1);

        while (cal.getTime().after(actualEventDate)) {
            cal.add(MapPeriodToCalendar.map(mainSubscription.getSubscriptionPeriod().getPeriodUnit().getId()),
                -mainSubscription.getSubscriptionPeriod().getValue());
        }

        logger.debug("After period adjustment, the date arrived for current order is {}", cal.getTime());

        return cal.getTime();
    }

    private void bindData(Integer userId, Date eventDate, Integer itemId, String mediationProcessId, OrderDTO subscriptionOrder, String assetIdentifier) {
        data.setUserId(userId);
        data.setEventDate(eventDate);
        data.setItemId(itemId);
        data.setMediationProcessId(mediationProcessId);
        data.setSubscriptionOrder(subscriptionOrder);
        data.setAssetIdentifier(assetIdentifier);
    }
}
