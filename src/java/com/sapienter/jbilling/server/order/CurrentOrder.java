/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.order;

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
import org.springmodules.cache.CachingModel;
import org.springmodules.cache.FlushingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
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

public class CurrentOrder {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EventLogger eLogger = EventLogger.getInstance();

    protected final Date eventDate;
    protected final Integer userId;
    protected final Integer itemId;
    protected final Integer entityId;
    protected final String mediationProcessId;

    protected UserBL user;
    // current order
    protected OrderBL orderBl = null;

    // cache management
    protected CacheProviderFacade cache;
    protected CachingModel cacheModel;
    protected FlushingModel flushModel;
    
    public CurrentOrder(Integer userId, Date eventDate, Integer itemId) {
        this(userId, eventDate, itemId, null, null);
    }

    public CurrentOrder(Integer userId, Date eventDate, Integer itemId, String mediationProcessId, Integer entityId) {
    	if (userId == null) throw new IllegalArgumentException("Parameter userId cannot be null!");
        if (eventDate == null) throw new IllegalArgumentException("Parameter eventDate cannot be null!");

        this.userId = userId;
        this.eventDate = eventDate;
        this.itemId = itemId;
        this.mediationProcessId = mediationProcessId;
        this.entityId = entityId;
        this.user = new UserBL(userId);
        
        cache = (CacheProviderFacade) Context.getBean(Context.Name.CACHE);
        cacheModel = (CachingModel) Context.getBean(Context.Name.CACHE_MODEL_RW);
        flushModel = (FlushingModel) Context.getBean(Context.Name.CACHE_FLUSH_MODEL_RW);
        logger.debug("Current order constructed with user %s event date %s", userId, eventDate);
    }

    /**
     * Returns the ID of a one-time order, where to add an event.
     * Returns null if no applicable order
     *
     * @return order ID of the current order
     */
    public Integer getCurrent() {
        Integer prefValue = PreferenceBL.getPreferenceValueAsInteger(entityId, com.sapienter.jbilling.client.util.Constants.PREFERENCE_ONE_ORDER_PER_MEDIATION_RUN);
        boolean newOrderPerMediationRun = mediationProcessId != null && Integer.valueOf(1).equals(prefValue);
        // find in the cache
        String cacheKey = buildCacheKey(newOrderPerMediationRun);

        Integer retValue = (Integer) cache.getFromCache(cacheKey, cacheModel);
        logger.debug("Retrieved from cache '%s', order id: %s", cacheKey, retValue);

        // a hit is only a hit if the order is still active and is not deleted. Sometimes when the order gets deleted
        // it wouldn't be removed from the cache.
        OrderDTO cachedOrder = new OrderDAS().findByIdAndIsDeleted(retValue, false);
        if (null != cachedOrder && OrderStatusFlag.INVOICE.equals(cachedOrder.getOrderStatus().getOrderStatusFlag())
                && cachedOrder.getIsMediated()) {
            logger.debug("Cache hit for %s", retValue);
            return retValue;
        }

        MainSubscriptionDTO mainSubscription = user.getEntity().getCustomer().getMainSubscription();
        Integer entityId = null;
        Integer currencyId = null;
        if (null == mainSubscription) {
            return null;
        }

        // find user entity & currency
        try {
            entityId = user.getEntity().getCompany().getId();
            currencyId = user.getEntity().getCurrency().getId();
        } catch (Exception e) {
            throw new SessionInternalError("Error looking for user entity of currency",
                    CurrentOrder.class, e);
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
        if (null == orderBl) {
        	orderBl = new OrderBL();
        }
        Map<Integer, Map<String,Date>> activeSinceDateMapByUser = new HashMap<>();
        do {

            Date activeSinceDate = getActiveSinceDate(activeSinceDateMapByUser);
            Date newOrderDate = calculateDate(futurePeriods, mainSubscription);
            logger.debug("Calculated one timer date: " + newOrderDate + ", for future periods: " + futurePeriods);

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
                    logger.debug("Found %s one-time orders for new order date: %s", rows.size(), newOrderDate);
                    Optional<OrderDTO> found = rows.stream()
                                                   .findFirst();
                    if(found.isPresent()) {
                        somePresent = true;
                        orderBl.set(found.get().getId());
                        logger.debug("Found existing one-time order");
                        orderFound = true;
                    }
                } catch (Exception e) {
                    throw new SessionInternalError("Error looking for one time orders", CurrentOrder.class, e);
                }
            }
            if (somePresent && !orderFound) {
                eLogger.auditBySystem(entityId, userId,
                                      Constants.TABLE_PUCHASE_ORDER,
                                      orderBl.getEntity().getId(),
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
        retValue = orderBl.getEntity().getId();

        logger.debug("Caching order %s with key '%s'", retValue, cacheKey);
        cache.putInCache(cacheKey, cacheModel, retValue);

        logger.debug("Returning %s", retValue);
        return retValue;
    }

    protected String buildCacheKey(boolean newOrderPerMediationRun) {
        StringBuilder cacheKeyBuilder = new StringBuilder(userId.toString()).append(Util.truncateDate(eventDate));
        if(newOrderPerMediationRun) {
            cacheKeyBuilder.append(mediationProcessId);
        }
        return cacheKeyBuilder.toString();
    }

    protected Date getActiveSinceDate(Map<Integer, Map<String,Date>> activeSinceDateMapByUser) {
        Date activeSinceDate = null;
        if (null != itemId){
            Map<String, Date> activeSinceDateMapByItem = activeSinceDateMapByUser.getOrDefault(userId, new HashMap<>());
            activeSinceDate = activeSinceDateMapByItem.computeIfAbsent(String.valueOf(itemId), action -> orderBl.getSubscriptionOrderActiveSinceDateByUsageItem(userId, itemId));
            activeSinceDateMapByItem.put(String.valueOf(itemId), activeSinceDate);
            activeSinceDateMapByUser.put(userId, activeSinceDateMapByItem);
        }
        return activeSinceDate;
    }
    
    /**
     * Assumes that main subscription already exists for the customer
     * @param futurePeriods date for N periods into the future
     * @param mainSubscription Customer main subscription
     * @param includeMediatedLinesToCurrentMonth 
     * @return calculated period date for N future periods
     */
    private Date calculateDate(int futurePeriods, MainSubscriptionDTO mainSubscription) {
    	
        GregorianCalendar cal = new GregorianCalendar();

        logger.debug("To begin with eventDate is %s", eventDate);

        // calculate the event date with the added future periods
        // default cal to actual event date
        Date actualEventDate = getActualEventDate();
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

        logger.debug("After period adjustment, the date arrived for current order is %s", cal.getTime());

        return cal.getTime();
    }

    protected Date getActualEventDate() {
        return eventDate;
    }

    private boolean isMainSubscriptionUsed(Integer entityId) {
        int preferenceUseCurrentOrder = 0;
        try {
            preferenceUseCurrentOrder = 
            	PreferenceBL.getPreferenceValueAsIntegerOrZero(
            		entityId, Constants.PREFERENCE_USE_CURRENT_ORDER);
        } catch (EmptyResultDataAccessException e) {
            // default preference will be used
            }
        
        return preferenceUseCurrentOrder != 0;
	}

	/**
     * Creates a new one-time order for the given active since date.
     * @param activeSince active since date
     * @param currencyId currency of order
     * @param entityId company id of order
     * @return new order
     */
    public Integer create(Date activeSince, Integer currencyId, Integer entityId) {
        OrderDTO currentOrder = new OrderDTO();
        currentOrder.setCurrency(new CurrencyDTO(currencyId));
        currentOrder.setIsMediated(Boolean.TRUE);

        // notes
        try {
            EntityBL entity = new EntityBL(entityId);
            ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", entity.getLocale());
            currentOrder.setNotes(bundle.getString("order.current.notes"));
        } catch (Exception e) {
            throw new SessionInternalError("Error setting the new order notes", CurrentOrder.class, e);
        } 

        currentOrder.setActiveSince(activeSince);
        
        // create the order
        if (orderBl == null) {
            orderBl = new OrderBL();
        }

	    orderBl.set(currentOrder);
	    orderBl.addRelationships(userId, Constants.ORDER_PERIOD_ONCE, currencyId);

        return orderBl.create(entityId, null, currentOrder);
    }

	public Date getEventDate() {
		return new Date(eventDate.getTime());
	}

	public Integer getUserId() {
		return userId;
	}

	public Integer getItemId() {
		return itemId;
	}    

}
