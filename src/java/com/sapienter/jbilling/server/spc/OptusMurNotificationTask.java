package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.mur.OptusMurJMREvent;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.task.UsageOrderReRater;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;

public class OptusMurNotificationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final String USAGE_POOL_NAME_LABLE = "name";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] { OptusMurJMREvent.class };

    public static Date formateDate(Date date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);
            return (Date) dateFormat.parseObject(dateFormat.format(date));
        } catch (ParseException ex) {
            throw new SessionInternalError("formateDate failed!", ex);
        }
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        try {
            logger.debug("processing optus mur notification {}", event);
            OptusMurJMREvent murJMREvent = (OptusMurJMREvent) event;
            List<JbillingMediationRecord> jmrs = murJMREvent.getJmrs();
            long startTime = System.currentTimeMillis();
            if(CollectionUtils.isNotEmpty(jmrs)) {
                JMRRepository jmrRepository = Context.getBean(JMRRepository.class);
                ItemDAS itemDAS = new ItemDAS();
                CustomerUsagePoolDAS customerUsagePoolDAS = new CustomerUsagePoolDAS();
                OrderDAS orderDAS = new OrderDAS();
                Map<UsageKey, BigDecimal> oldUsageQuantityMap = new HashMap<>();
                for(JbillingMediationRecord jmr : jmrs) {
                    logger.debug("Notifying User {} for item {} for cdr type {}", jmr.getUserId(), jmr.getItemId(), jmr.getCdrType());
                    String assetNumber = jmr.getSource();
                    OrderDTO subscriptionOrder = orderDAS.findOrderByUserAssetIdentifierEffectiveDate(
                            jmr.getUserId(), assetNumber, jmr.getEventDate());
                    if(null == subscriptionOrder) {
                        logger.debug("Subscription Order not found for user {} for number {}", jmr.getUserId(), assetNumber);
                        continue;
                    }
                    
                    Date nextInvoiceDate = subscriptionOrder.getUser().getCustomer().getNextInvoiceDate();
                    Map<UsageOrderReRater.UsagePeriod, Date> customerPeriodMap = UsageOrderReRater.getBillablePeriodFromSubscriptionOrder(subscriptionOrder);
                    Date startDate = formateDate(customerPeriodMap.get(UsageOrderReRater.UsagePeriod.CYCLE_START_DATE));
                    Date endDate = formateDate(customerPeriodMap.get(UsageOrderReRater.UsagePeriod.CYCLE_END_DATE));
                    
                    if (null != nextInvoiceDate && 
                		(jmr.getEventDate().after(nextInvoiceDate) || 
                		jmr.getEventDate().equals(nextInvoiceDate))) {

                    	OrderPeriodDTO customerBillingCycle = 
                    			subscriptionOrder.getUser().getCustomer().
                    			getMainSubscription().getSubscriptionPeriod();
                    	GregorianCalendar cal = new GregorianCalendar();
	                    cal.setTime(nextInvoiceDate);
	                    cal.add(MapPeriodToCalendar.map(customerBillingCycle.getUnitId()), customerBillingCycle.getValue());
	                    startDate = nextInvoiceDate;
	                    endDate = cal.getTime();
                    }
                    
                    Integer userId = jmr.getUserId();
                    Integer itemId = jmr.getItemId();
                    String cdrType = jmr.getCdrType();
                    UserDTO user = subscriptionOrder.getBaseUserByUserId();
                    Integer customerId = user.getCustomer().getId();
                    logger.debug("OPTUS MUR userId {}", userId);
                    logger.debug("OPTUS MUR subscriptionOrder {}", subscriptionOrder.getId());
                    logger.debug("OPTUS MUR assetNumber {}", assetNumber);
                    logger.debug("OPTUS MUR itemId {}", itemId);
                    logger.debug("OPTUS MUR startDate {} and endDate", startDate, endDate);
                    
                    UsageKey key = UsageKey.of(subscriptionOrder.getId(), assetNumber, itemId, cdrType, startDate, endDate);
                    BigDecimal oldUsageQuantity = oldUsageQuantityMap.get(key);
                    logger.debug("OPTUS MUR oldUsageQuantity {}", oldUsageQuantity);
                    if(null == oldUsageQuantity) {
                        // first fetch from optus mur usage map.
                        oldUsageQuantity = getOldUsage(userId, itemId, assetNumber, startDate, endDate);
                        logger.debug("OPTUS MUR oldUsageQuantity inside if {}", oldUsageQuantity);
                        if(null == oldUsageQuantity || 0 == BigDecimal.ZERO.compareTo(oldUsageQuantity)) {
                            long queryStartTime = System.currentTimeMillis();
                            // second fetch from jbilling_mediation_record table.
                            oldUsageQuantity = jmrRepository.sumOfJMRQuantityForUserAssetItemAndCdrTypeForDateRange(userId, itemId, cdrType,
                                    JbillingMediationRecord.STATUS.PROCESSED.name(), false, startDate, endDate, assetNumber);
                            logger.debug("query execution took {} in miliseconds", (System.currentTimeMillis() - queryStartTime));
                            logger.debug("OPTUS MUR oldUsageQuantity before insert{}", oldUsageQuantity);
                            if(null == oldUsageQuantity) {
                                oldUsageQuantity = BigDecimal.ZERO;
                            }
                            // insert old usage in optus mur usage table.
                            if (!isOptusMurRecordPresent(userId, itemId, assetNumber, startDate, endDate)) {
                            	createOptusMurUsageMap(userId, itemId, assetNumber, startDate, endDate, oldUsageQuantity);	
                            }
                        }
                        oldUsageQuantityMap.put(key, oldUsageQuantity);
                    }

                    ItemDTO mediatedItem = itemDAS.findNow(itemId);
                    //notify jmr
                    long notifyStartTime = System.currentTimeMillis();
                    if (jmr.getEventDate().after(endDate)) {
                        logger.debug("JMR event date: {} is after end date of billing period: {}, for user id: {}",
                                jmr.getEventDate(), endDate, user.getId());
                        continue;
                    }

                    List<UsagePoolDTO> usagePools = new ArrayList(subscriptionOrder.getPlanFromOrder().getUsagePools());
                    Collections.sort(usagePools, UsagePoolDTO.UsagePoolsByPrecedenceOrCreatedDateComparator);
                    
                    notifyJMR(jmr, oldUsageQuantity, mediatedItem, usagePools, user.getLanguageIdField(), 
                    		subscriptionOrder.getId(), user.getId());
                    
                    logger.debug("time taken to execute notify {} miliseconds for a jmr of user {}",
                            (System.currentTimeMillis() - notifyStartTime), user.getId());
                    oldUsageQuantity = oldUsageQuantityMap.get(key); // fetch previous old usage quantity.
                    oldUsageQuantity = oldUsageQuantity.add(jmr.getQuantity()); // add current notified jmr quantity in it.
                    // update new quantity on optus mur usage table.
                    updateOptusMurUsageMap(userId, itemId, assetNumber, startDate, endDate, oldUsageQuantity);
                    oldUsageQuantityMap.put(key, oldUsageQuantity); // update old usage quantity.
                }
                logger.debug("user {}, total jmrs {} notified, time taken {} in miliseconds",
                        jmrs.get(0).getUserId(), jmrs.size(), (System.currentTimeMillis() - startTime));
            }
        } catch(Exception ex) {
            throw new PluggableTaskException("Error in OptusMurNotificationTask", ex);
        }
    }

    private void notifyJMR(JbillingMediationRecord jmr, BigDecimal oldUsageQuantity,
            ItemDTO mediatedItem, List<UsagePoolDTO> usagePools, Integer languageId, Integer subscriptionOrderId, Integer userId) {
        BigDecimal newUsageQuantity = oldUsageQuantity.add(jmr.getQuantity());
        for (UsagePoolDTO usagePoolDto : usagePools) {
            if (usagePoolDto.getAllItems().contains(mediatedItem)) {
                if (newUsageQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                logger.debug("new Usage for user {} is {} for item {}", jmr.getUserId(), newUsageQuantity, mediatedItem.getId());
                List<BigDecimal> notifyPercentages = findNotificationPercentages(getPercentages(),
                        usagePoolDto.getQuantity(),
                        oldUsageQuantity, newUsageQuantity);
                if (CollectionUtils.isNotEmpty(notifyPercentages)) {
                    String currentDataBoost = null;
                    String currentUsagePoolName = usagePoolDto.getDescription(languageId, USAGE_POOL_NAME_LABLE);
                    boolean isDataBoost = currentUsagePoolName.contains(SPCConstants.DATA_BOOST_NAME);
                    if (contains(notifyPercentages, ONE_HUNDRED)) {
                        if (isDataBoost) {
                            currentDataBoost = SPCConstants.DATA_BOOST_TOKEN +
                                    StringUtils.substringAfter(currentUsagePoolName, SPCConstants.DATA_BOOST_NAME);
                        }
                    }

                    if ((isDataBoost && contains(notifyPercentages, ONE_HUNDRED)) || (!isDataBoost)) {
                        if (isDataBoost) {
                            // 100 percentage notification for data boost.
                            notifyPercentages = Collections.singletonList(ONE_HUNDRED);
                        }
                        logger.debug("notify percentages {}", notifyPercentages);
                        OptusMurNotificationEvent murNotificationEvent = new OptusMurNotificationEvent(getEntityId(), oldUsageQuantity,
                                newUsageQuantity, notifyPercentages, jmr, getParameters(), currentDataBoost, subscriptionOrderId, userId);
                        logger.debug("notifying optus mur event {}", murNotificationEvent);
                        EventManager.process(murNotificationEvent);
                    }
                }
                if (newUsageQuantity.compareTo(usagePoolDto.getQuantity()) > 0) {
                    newUsageQuantity = newUsageQuantity.subtract(usagePoolDto.getQuantity(), MathContext.DECIMAL128);
                    oldUsageQuantity = newUsageQuantity.subtract(jmr.getQuantity(), MathContext.DECIMAL128);
                } else {
                    break;
                }
            }
        }
    }

    private List<BigDecimal> findNotificationPercentages(List<BigDecimal> notificationPercentages,
            BigDecimal freeQuantity, BigDecimal oldUsageQuantity, BigDecimal newUsageQuantity) {
        List<BigDecimal> applicablePercentages = new ArrayList<>();
        BigDecimal oldUsagePercentage = (oldUsageQuantity.multiply(ONE_HUNDRED)).divide(freeQuantity, CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND);
        BigDecimal newUsagePercentage = (newUsageQuantity.multiply(ONE_HUNDRED)).divide(freeQuantity, CommonConstants.BIGDECIMAL_SCALE, CommonConstants.BIGDECIMAL_ROUND);
        for (BigDecimal percentage : notificationPercentages) {
            if (oldUsagePercentage.compareTo(percentage) >= 0) {
                continue;
            }

            if (newUsagePercentage.compareTo(percentage) >= 0) {
                applicablePercentages.add(percentage);
            }
        }
        return applicablePercentages;
    }

    public static boolean contains(List<BigDecimal> values, BigDecimal valueToSearch) {
        if(CollectionUtils.isEmpty(values)) {
            throw new SessionInternalError("values can not be null to method contains");
        }
        if(null == valueToSearch) {
            throw new SessionInternalError("valueToSearch can not be null to method contains");
        }
        for(BigDecimal value : values) {
            if(value.compareTo(valueToSearch) == 0) {
                return true;
            }
        }
        return false;
    }

    private List<BigDecimal> getPercentages() {
        List<BigDecimal> percentages = new ArrayList<>();
        for(Entry<String, String> paramEntry : getParameters().entrySet()) {
            String percentage = paramEntry.getKey();
            if(!NumberUtils.isNumber(percentage)) {
                logger.error("{} is not number", percentage);
                throw new IllegalArgumentException("Please Enter valid percentage number");
            }
            percentages.add(new BigDecimal(percentage));
        }
        return percentages;
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private static final String SQL_INSERT_OPTUS_MUR_USAGE =
            "INSERT INTO optus_mur_usage_map (user_id, item_id, asset_number, from_date, to_date, quantity, create_date) "
                    + "VALUES (?,?,?,?,?,?,?)";

    private static final String SQL_UPDATE_OPTUS_MUR_USAGE =
            "UPDATE optus_mur_usage_map SET quantity = ? WHERE user_id = ? "
                    + "AND item_id = ? AND asset_number = ? AND from_date = ?";

    private static final String SQL_GET_OPTUS_MUR_USAGE = "SELECT quantity FROM optus_mur_usage_map WHERE user_id = ? "
            + "AND item_id = ? AND asset_number = ? AND from_date = ? AND to_date = ?";
    
    private static final String SQL_IS_OPTUS_MUR_RECORD_EXIST = "SELECT user_id FROM optus_mur_usage_map WHERE user_id = ? "
            + "AND item_id = ? AND asset_number = ? AND from_date = ? AND to_date = ?";
    /**
     * Inserts optus mur usage map record in database.
     * @param userId
     * @param itemId
     * @param assetNumber
     * @param fromDate
     * @param toDate
     * @param quantity
     */
    private void createOptusMurUsageMap(Integer userId, Integer itemId, String assetNumber,
            Date fromDate, Date toDate, BigDecimal quantity) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        long startTime = System.currentTimeMillis();
        jdbcTemplate.update(SQL_INSERT_OPTUS_MUR_USAGE, userId, itemId,
                assetNumber, fromDate, toDate, quantity, formateDate(new Date()));
        logger.debug("createOptusMurUsageMap took {} for user {} and item {}",
                (System.currentTimeMillis() - startTime), userId, itemId);
        logger.debug("optus usage map created of user {} with item {} and quantity {} and asset number {} "
                + "between period [from-{}, to-{}] ", userId, itemId, quantity, assetNumber, fromDate, toDate);
    }

    /**
     * Updates new quantity based on match record in database.
     * @param userId
     * @param itemId
     * @param assetNumber
     * @param fromDate
     * @param toDate
     * @param quantity
     */
    private void updateOptusMurUsageMap(Integer userId, Integer itemId, String assetNumber,
            Date fromDate, Date toDate, BigDecimal quantity) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        long startTime = System.currentTimeMillis();
        jdbcTemplate.update(SQL_UPDATE_OPTUS_MUR_USAGE, quantity, userId, itemId, assetNumber, fromDate);
        logger.debug("updateOptusMurUsageMap took {} for user {} and Item {}",
                (System.currentTimeMillis() - startTime), userId, itemId);
        logger.debug("optus usage map updated of user {} with item {} and new quantity {} and asset number {} "
                + "between period [from-{}, to-{}] ", userId, itemId, quantity, assetNumber, fromDate, toDate);
    }

    /**
     * Returns usage quantity based on matched record in database.
     * @param userId
     * @param itemId
     * @param assetNumber
     * @param fromDate
     * @param toDate
     * @return
     */
    private BigDecimal getOldUsage(Integer userId, Integer itemId, String assetNumber,
            Date fromDate, Date toDate) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        try {
            long startTime = System.currentTimeMillis();
            BigDecimal oldUsage = jdbcTemplate.queryForObject(SQL_GET_OPTUS_MUR_USAGE,
                    BigDecimal.class, userId, itemId, assetNumber, fromDate, toDate);
            logger.debug("getOldUsage took {} for user {} item {} and usage {}", (System.currentTimeMillis() - startTime), userId, itemId, oldUsage);
            return oldUsage;
        } catch(EmptyResultDataAccessException noRow) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Returns true if Optus MUR Record present for given criteria.
     * @param userId
     * @param itemId
     * @param assetNumber
     * @param fromDate
     * @param toDate
     * @return
     */
    private boolean isOptusMurRecordPresent(Integer userId, Integer itemId, String assetNumber,
            Date fromDate, Date toDate) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        try {
            Integer optusUserId = jdbcTemplate.queryForObject(SQL_IS_OPTUS_MUR_RECORD_EXIST,
                    Integer.class, userId, itemId, assetNumber, fromDate, toDate);
            return (null != optusUserId);
        } catch(EmptyResultDataAccessException noRow) {
            return false;
        }
    }

    @EqualsAndHashCode
    @ToString
    private static class UsageKey {
        private Integer subscriptionOrderId;
        private String assetNumber;
        private Integer itemId;
        private String cdrType;
        private Date startDate;
        private Date endDate;

        private UsageKey(Integer subscriptionOrderId, String assetNumber, Integer itemId,
                String cdrType, Date startDate, Date endDate) {
            this.subscriptionOrderId = subscriptionOrderId;
            this.assetNumber = assetNumber;
            this.itemId = itemId;
            this.cdrType = cdrType;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        static UsageKey of(Integer subscriptionOrderId, String assetNumber, Integer itemId,
                String cdrType, Date startDate, Date endDate) {
            return new UsageKey(subscriptionOrderId, assetNumber,
                    itemId, cdrType, startDate, endDate);
        }
    }
}
