package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderServiceImpl;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.event.CustomerUsagePoolConsumptionEvent;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

/**
 * SPC Client specific OrderServiceImpl class to convert the Event Date time zone.
 *
 * @author Ashwinkumar
 * @since 19-Sep-2019
 *
 */
public class SPCOrderServiceImpl extends OrderServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private AssetDAS assetDAS;
    @Resource
    private ItemDAS itemDAS;
    @Resource
    private OrderLineDAS orderLineDAS;
    @Resource
    private OrderDAS orderDAS;
    @Resource(name = "jBillingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Resource
    private SpcHelperService spcHelperService;
    @Resource
    private CustomerUsagePoolDAS customerUsagePoolDAS;

    @Override
    protected Date getTimeZonedEventDate(Date eventDate, Integer entityId) {
        // get company level time zone
        String companyTimeZone = TimezoneHelper.getCompanyLevelTimeZone(entityId);
        Date newDate = Date.from(Instant.ofEpochMilli(eventDate.getTime()).atZone(ZoneId.of(companyTimeZone)).toLocalDateTime()
                .atZone(ZoneId.systemDefault()).toInstant());
        logger.debug(
                "SPCOrderServiceImpl - company id : {}, time zone : {}, event date : {}, converted event date : {}",
                entityId, companyTimeZone, eventDate, newDate);
        return newDate;
    }

    @Override
    protected OrderDTO getOrCreateCurrentOrder(Integer userId, Date eventDate, Integer itemId,
            Integer currencyId, boolean persist, String processId, Integer entityId, PricingField[] pricingFields){
        return OrderBL.getOrCreateCurrentOrder(userId, eventDate, itemId, currencyId, persist, processId, entityId, getAssetIdentifier(pricingFields));
    }

    private String getAssetIdentifier(PricingField[] pricingFields) {
        List<PricingField> pricingList = Arrays.asList(pricingFields);
        PricingField assetIdentifier = PricingField.find(pricingList, SPCConstants.FROM_NUMBER);
        if(null == assetIdentifier) {
            assetIdentifier = PricingField.find(pricingList, SPCConstants.USER_NAME);
        }
        return null != assetIdentifier ? assetIdentifier.getStrValue() : null;
    }

    private boolean isItemFoundMultipleTimes(List<PlanBasedFreeCallInfo> planBasedFreeCallInfos, Integer itemId) {
        int count = 0;
        for(PlanBasedFreeCallInfo planBasedFreeCallInfo : planBasedFreeCallInfos) {
            if(planBasedFreeCallInfo.getItems().contains(itemId)) {
                count = count + 1;
            }
        }
        return count >= 2;
    }

    private static final String ORDER_LINE_FREE_CALL_COUNTER_MF_NAME = "Free Call/SMS Counter";

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "transactionManager")
    public MediationEventResult addMediationEvent(JbillingMediationRecord jmr) {
        MediationEventResult mediationEventResult = new MediationEventResult();
        try {
            long startTime = System.currentTimeMillis();
            String callIdentifier = jmr.getSource();
            if(StringUtils.isEmpty(callIdentifier)) {
                throw new SessionInternalError("Asset Identifier not found, jmr source field is null");
            }
            long firstBlock = System.currentTimeMillis();
            BigDecimal quantity = jmr.getQuantity();
            if(0 == quantity.compareTo(BigDecimal.ZERO)) {
                mediationEventResult.setQuantityEvaluated(BigDecimal.ZERO);
                return mediationEventResult;
            }
            Integer userId = jmr.getUserId();
            Integer itemId = jmr.getItemId();

            //normal processing
            UserBL userbl = new UserBL(userId);

            //NOTE: should be the same as ownerEntityId at this point
            Integer companyId = userbl.getEntityId(userId);

            // get currency from the user
            Integer currencyId = userbl.getCurrencyId();

            PricingField[] pricingFields = PricingField.getPricingFieldsValue(jmr.getPricingFields());
            logger.debug("time taken {} miliseconds for get asset, user and pricing fields for user {}",
                    (System.currentTimeMillis() - firstBlock), userId);
            PricingField serviceType = PricingField.find(Arrays.asList(pricingFields), SPCConstants.SERVICE_TYPE);
            MediationServiceType mediationType = MediationServiceType.fromServiceName(serviceType.getStrValue());
            Date eventDate = jmr.getEventDate();
            // get the current order and init OrderBL
            long currentOrderTime = System.currentTimeMillis();
            OrderDTO mediatedOrder = getOrCreateCurrentOrder(userId, eventDate, itemId, currencyId, true,
                    jmr.getProcessId().toString(), companyId, pricingFields);
            mediationEventResult.setCurrentOrderId(mediatedOrder.getId());
            logger.debug("time taken {} miliseconds for getOrCreateCurrentOrder for user {}",
                    (System.currentTimeMillis() - currentOrderTime), userId);

            // get language from the caller
            Integer languageId = userbl.getLanguage();
            long resolveFreeCalls = System.currentTimeMillis();

            String planOrderId = jmr.getPricingFieldValueByName(SPCConstants.PURCHASE_ORDER_ID);
            logger.debug("Plan OrderId: {}", planOrderId);
            if(StringUtils.isEmpty(planOrderId)) {
                logger.debug("plan order not found for asset identifier {}", callIdentifier);
                throw new SessionInternalError("Plan order not found for asset identifier "+ callIdentifier);
            }
            OrderDTO planOrder = new OrderDAS().find(Integer.parseInt(planOrderId));
            List<PlanBasedFreeCallInfo> planBasedFreeCallInfos = spcHelperService.getPlanBasedFreeCallInfoForAsset(planOrder);
            boolean needToResolvePrice = true;
            if(CollectionUtils.isNotEmpty(planBasedFreeCallInfos)) {
                if(isItemFoundMultipleTimes(planBasedFreeCallInfos, itemId)) {
                    throw new SessionInternalError("item "+ itemId + " present multiple times in "
                            + "plan based free call configuration for user "+ userId);
                }
                for(PlanBasedFreeCallInfo planBasedFreeCallInfo : planBasedFreeCallInfos) {
                    List<Integer> items = planBasedFreeCallInfo.getItems();
                    if(items.contains(jmr.getItemId())) {
                        // adding one for current jmr in sumofCallCounters.
                        Long sumofCallCounters = spcHelperService.getCallCountersForItemsForActiveMediatedOrder(userId, items) + 1L;
                        if(0L == sumofCallCounters || sumofCallCounters <= planBasedFreeCallInfo.getFreeCallCount()) {
                            logger.debug("{} number of calls from {} marked as free call for configuration {}",
                                    sumofCallCounters, callIdentifier, planBasedFreeCallInfo);
                            needToResolvePrice = false;
                            break;
                        }
                    }
                }

            }
            logger.debug("time taken {} miliseconds for resolve free calls for user {}",
                    (System.currentTimeMillis() - resolveFreeCalls), userId);
            long resolveJMRPrice = System.currentTimeMillis();
            BigDecimal resolvedPrice = needToResolvePrice ? resolvePriceForJMR(mediatedOrder, jmr, eventDate) : BigDecimal.ZERO;
            logger.debug("time taken {} miliseconds for resolve JMR Price for user {}",
                    (System.currentTimeMillis() - resolveJMRPrice), userId);

            long updateOrderLine = System.currentTimeMillis();
            // create or update line on mediated order.
            OrderLineDTO updatedLine = createOrUpdateOrderLine(mediatedOrder, callIdentifier, itemId, languageId);
            if(!needToResolvePrice) {
                @SuppressWarnings("unchecked")
                MetaFieldValue<Integer> freeCallCounter = updatedLine.getMetaField(ORDER_LINE_FREE_CALL_COUNTER_MF_NAME);
                if(null == freeCallCounter) {
                    MetaField freeCallCounterMF = MetaFieldBL.getFieldByName(companyId,
                            new EntityType[] { EntityType.ORDER_LINE },
                            ORDER_LINE_FREE_CALL_COUNTER_MF_NAME);
                    if(null == freeCallCounterMF) {
                        logger.debug("meta field {} not defined at order line level for entity {}",
                                ORDER_LINE_FREE_CALL_COUNTER_MF_NAME, companyId);
                    } else {
                        freeCallCounter = new IntegerMetaFieldValue(freeCallCounterMF);
                        updatedLine.getMetaFields().add(freeCallCounter); // added free call counter mf value on order line.
                    }
                }
                if(null!= freeCallCounter) {
                    Integer value = freeCallCounter.getValue();
                    if(null == value) {
                        value = 1;
                    } else {
                        value = value + 1;

                    }
                    freeCallCounter.setValue(value);
                }
            }
            logger.debug("time taken {} miliseconds for update order line for user {}",
                    (System.currentTimeMillis() - updateOrderLine), userId);

            long applyUsagePoolStartTime = System.currentTimeMillis();
            if(needToResolvePrice) {
                UserDTO user = userbl.getEntity();
                // apply customer usage pool.
                long getSubscriptionOrder = System.currentTimeMillis();
                OrderDTO subscriptionOrder = orderDAS.findOrderByUserAssetIdentifierEffectiveDate(user.getId(),
                        callIdentifier, jmr.getEventDate());
                logger.debug("time taken {} miliseconds for get subscriptionOrder for user {}",
                        (System.currentTimeMillis() - getSubscriptionOrder), userId);
                long getUsagePool = System.currentTimeMillis();
                List<CustomerUsagePoolDTO> freeUsagePools = customerUsagePoolDAS.getCustomerUsagePoolsByCustomerIdAndOrderId(
                        user.getCustomer().getId(), subscriptionOrder.getId());
                logger.debug("time taken {} miliseconds for get Customer usage pools for user {}",
                        (System.currentTimeMillis() - getUsagePool), userId);
                if(CollectionUtils.isNotEmpty(freeUsagePools)) {
                    // sort based on preference or created date if preference is same
                    Collections.sort(freeUsagePools, CustomerUsagePoolDTO.CustomerUsagePoolsByPrecedenceOrCreatedDateComparator);
                    for (CustomerUsagePoolDTO freeUsagePool : freeUsagePools) {
                        if(0 == freeUsagePool.getQuantity().compareTo(BigDecimal.ZERO)) {
                            continue;
                        }
                        if (freeUsagePool.isActive(mediatedOrder.getActiveSince())
                                && quantity.compareTo(BigDecimal.ZERO) !=0
                                && isItemPresentOnUsagePool(itemId, freeUsagePool.getUsagePool().getId())) {
                            BigDecimal freeQuantity;
                            BigDecimal oldQuantity = freeUsagePool.getQuantity();
                            if(quantity.compareTo(freeUsagePool.getQuantity()) >=0) {
                                freeQuantity = freeUsagePool.getQuantity();
                                quantity = quantity.subtract(freeQuantity, MathContext.DECIMAL128);
                            } else {
                                freeQuantity = quantity;
                                quantity = BigDecimal.ZERO;
                            }
                            freeUsagePool.setQuantity(freeUsagePool.getQuantity()
                                    .subtract(freeQuantity, MathContext.DECIMAL128));
                            //create order line usage pool association.
                            updatedLine.createOrUpdateOrderLineUsagePool(freeUsagePool,
                                    freeQuantity, mediatedOrder.getActiveSince());
                            // fire consumption event
                            long consumptionStartTime = System.currentTimeMillis();
                            EventManager.process(new CustomerUsagePoolConsumptionEvent(companyId, freeUsagePool.getId(),
                                    oldQuantity, freeUsagePool.getQuantity(), jmr.getEventDate()));
                            logger.debug("time taken {} miliseconds for CustomerUsagePoolConsumptionEvent for user {}",
                                    (System.currentTimeMillis() - consumptionStartTime), userId);
                        }
                    }
                }
            }
            logger.debug("apply usage pool took {} miliseconds for single jmr of user {}",
                    (System.currentTimeMillis() - applyUsagePoolStartTime), userId);
            // add new mediated quantity and amount on order line
            BigDecimal newAmount = quantity.multiply(resolvedPrice, MathContext.DECIMAL128);
            updatedLine.addQuantityAndAmount(jmr.getQuantity(), newAmount);
            mediationEventResult.setAmountForChange(newAmount);
            //do processing for billable record
            mediationEventResult.setOrderLinedId(updatedLine.getId());
            mediationEventResult.setQuantityEvaluated(jmr.getQuantity());
            if(needToResolvePrice) {
                long fireJmrPostProcessorStartTime = System.currentTimeMillis();
                // fire jmr post processing task.
                fireJmrPostProcessorTask(jmr, mediatedOrder, mediationEventResult);
                logger.debug("time taken {} miliseconds for fireJmrPostProcessorTask for user {}",
                        (System.currentTimeMillis() - fireJmrPostProcessorStartTime), userId);
            }
            logger.debug("time taken to mediate single jmr is {} in miliseconds for user {}",
                    (System.currentTimeMillis() - startTime), jmr.getUserId());
            return mediationEventResult;
        } catch(Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.error("Exception while creating order", ex);
            mediationEventResult.setExceptionMessage(ExceptionUtils.getRootCauseMessage(ex));
            return mediationEventResult;
        }
    }

    /**
     * Creates or update {@link OrderLineDTO} on {@link OrderDTO}.
     * @param mediatedOrder
     * @param callIdentifier
     * @param itemId
     * @param languageId
     * @return
     */
    private OrderLineDTO createOrUpdateOrderLine(OrderDTO mediatedOrder, String callIdentifier, Integer itemId, Integer languageId) {
        OrderLineDTO lineToUpdate = orderLineDAS
                .findOrderLineByItemAndCallIdentifier(mediatedOrder.getId(), itemId, callIdentifier);
        if(null == lineToUpdate) {
            // create new order line if not found.
            lineToUpdate = new OrderLineDTO();
            lineToUpdate.setCallIdentifier(callIdentifier);
            ItemDTO item = itemDAS.find(itemId);
            lineToUpdate.setItemId(itemId);
            lineToUpdate.setDescription(item.getDescription(languageId));
            lineToUpdate.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
            lineToUpdate.setPurchaseOrder(mediatedOrder);
            lineToUpdate.setQuantity(BigDecimal.ZERO);
            lineToUpdate.setPrice(BigDecimal.ZERO);
            lineToUpdate.setAmount(BigDecimal.ZERO);
            lineToUpdate.setCreateDatetime(new Date());
            lineToUpdate.setUseItem(true);
            lineToUpdate = orderLineDAS.save(lineToUpdate);
            mediatedOrder.getLines().add(lineToUpdate);
        }
        lineToUpdate.incrementCallCounter();
        return lineToUpdate;
    }

    @Resource
    private SessionFactory sessionFactory;

    private static final String CHECK_ITEM_ON_USAGE_POOL_SQL =
            "select us.id from usage_pool us, usage_pool_item_type_map usitm, item_type_map itm "
                    + "where us.id = usitm.usage_pool_id "
                    + "and itm.type_id = usitm.item_type_id "
                    + "and itm.item_id = :itemId "
                    + "and us.id = :usagePoolId "
                    + "union all "
                    + "select us.id from usage_pool us, usage_pool_item_map usim "
                    + "where us.id = usim.usage_pool_id "
                    + "and usim.item_id = :itemId "
                    + "and us.id = :usagePoolId ";

    /**
     * checks given item present on usage pool,
     * if present then return true else false.
     * @param itemId
     * @param usagepoolId
     * @return
     */
    private boolean isItemPresentOnUsagePool(Integer itemId, Integer usagepoolId) {
        Session session = sessionFactory.getCurrentSession();
        @SuppressWarnings("unchecked")
        List<Integer> result = session.createSQLQuery(CHECK_ITEM_ON_USAGE_POOL_SQL)
        .setParameter("itemId", itemId)
        .setParameter("usagePoolId", usagepoolId)
        .list();
        if(CollectionUtils.isEmpty(result)) {
            return false;
        }
        logger.debug("item {} found on usage pool {}", itemId, usagepoolId);
        return true;
    }

    private BigDecimal resolvePriceForJMR(OrderDTO order, JbillingMediationRecord jmr, Date eventDate) {
        String codeString = jmr.getPricingFieldValueByName(SPCConstants.CODE_STRING);
        long itemLoadStartTime = System.currentTimeMillis();
        ItemBL itemBl = new ItemBL(jmr.getItemId());
        logger.debug("load item took {} miliseconds for user {}", (System.currentTimeMillis() - itemLoadStartTime), jmr.getUserId());
        itemBl.setPricingFields(Arrays.asList(PricingField.getPricingFieldsValue(jmr.getPricingFields())));
        long startQueryTime = System.currentTimeMillis();
        BigDecimal resolvedPrice = itemBl.getPrice(jmr.getUserId(), jmr.getCurrencyId(), jmr.getQuantity(),
                jmr.getjBillingCompanyId(), order, null, true, eventDate);
        logger.debug("price resolved {} for jmr {} ", resolvedPrice, jmr.getRecordKey());
        logger.debug("time taken {} miliseconds to resolve price for single jmr for user {} for code string {}",
                (System.currentTimeMillis() - startQueryTime), jmr.getUserId(), codeString);
        return resolvedPrice;
    }

}
