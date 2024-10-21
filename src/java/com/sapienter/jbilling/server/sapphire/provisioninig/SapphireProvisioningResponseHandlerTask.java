package com.sapienter.jbilling.server.sapphire.provisioninig;

import static com.sapienter.jbilling.server.sapphire.provisioninig.OrderProvisioninigStatus.PENDING_CHANGE_OF_PLAN;
import static com.sapienter.jbilling.server.sapphire.provisioninig.OrderProvisioninigStatus.PLAN_CHANGED;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningHelper.updateCustomerProvisioningStatusMf;
import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningHelper.updateOrderProvisioningStatusMf;
import static com.sapienter.jbilling.server.sapphire.provisioninig.UserProvisioninigStatus.ACTIVATED;
import static com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants.ORDER_PERIOD_ID_PARAM_NAME;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.google.common.collect.MapDifference.ValueDifference;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.item.event.AssetUpdatedEvent;
import com.sapienter.jbilling.server.item.event.SwapAssetsEvent;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.OrderMetaFieldUpdateEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.sapphire.ChangeOfPlanEvent;
import com.sapienter.jbilling.server.sapphire.SapphireHelper;
import com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupConstants;
import com.sapienter.jbilling.server.sapphire.signupprocess.SapphireSignupProcessTask;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

public class SapphireProvisioningResponseHandlerTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME =
            new ParameterDescription("customer provisioning metafield name", true, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_ORDER_PROVISIONING_STATUS_MF_NAME =
            new ParameterDescription("order provisioning metafield name", true, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_NEW_ORDER_ID_MF_NAME =
            new ParameterDescription("new order id metafield name", true, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_SIGNUP_PLUGIN_ID =
            new ParameterDescription("Sapphire signup plugin id", true, ParameterDescription.Type.INT);

    public SapphireProvisioningResponseHandlerTask() {
        descriptions.add(PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME);
        descriptions.add(PARAM_ORDER_PROVISIONING_STATUS_MF_NAME);
        descriptions.add(PARAM_SIGNUP_PLUGIN_ID);
        descriptions.add(PARAM_NEW_ORDER_ID_MF_NAME);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] EVENTS = new Class[] {
        SwapAssetsEvent.class,
        ChangeOfPlanEvent.class,
        OrderMetaFieldUpdateEvent.class,
        AssetUpdatedEvent.class
    };

    /**
     * Sets active since date and status on order and it's child order.
     * @param order
     * @param activeSinceDate
     */
    private void setOrderStatusActiveSinceDateOnOrder(OrderWS order, Date activeSinceDate, OrderStatusWS status) {
        if(null!= activeSinceDate) {
            order.setActiveSince(activeSinceDate);
        }
        order.setOrderStatusWS(status);
        for(OrderWS child : order.getChildOrders()) {
            if(!child.getOrderStatusWS().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
                setOrderStatusActiveSinceDateOnOrder(child, activeSinceDate, status);
            }
        }
    }

    private void activateOrder(OrderWS order) throws PluggableTaskException {
        IWebServicesSessionBean api = Context.getBean(Name.WEB_SERVICES_SESSION);
        Integer entityId = getEntityId();
        Date newActiveSinceDate = TimezoneHelper.companyCurrentDate(entityId);
        logger.debug("order {} old active since date {} and new active since date {}", order.getId(), order.getActiveSince(), newActiveSinceDate);
        OrderStatusWS activeStatus = new OrderStatusWS();
        activeStatus.setId(new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, entityId));
        setOrderStatusActiveSinceDateOnOrder(order, newActiveSinceDate, activeStatus);
        api.updateOrder(order, OrderChangeBL.buildFromOrder(order, getOrderChangeApplyStatus(api)));
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        try {
            logger.debug("processing {}", event);
            Integer entityId = getEntityId();
            validateReqiredParameters(entityId);
            if(event instanceof SwapAssetsEvent) {
                SwapAssetsEvent swapAssetsEvent = (SwapAssetsEvent) event;
                if(!isEnrollement(swapAssetsEvent.getOldNewAssetMap())) {
                    logger.debug("task only process swap asset for enrollment");
                    return;
                }
                Integer orderId = swapAssetsEvent.getOrderId();
                IWebServicesSessionBean api = Context.getBean(Name.WEB_SERVICES_SESSION);
                OrderWS order = api.getOrder(orderId);
                if(isOrderRecurring(order)) {
                    activateOrder(order);
                } else {
                    logger.debug("order {} is not recurring order", order.getId());
                    List<OrderDTO> orders =  new OrderDAS().findRecurringOrders(order.getUserId(),
                            new OrderStatusFlag[] { OrderStatusFlag.NOT_INVOICE });
                    logger.debug("fecthed all pending recurring orders for user {}", order.getUserId());
                    for(OrderDTO pendingOrder : orders) {
                        activateOrder(api.getOrder(pendingOrder.getId()));
                    }
                }
                logger.debug("updated order {}", orderId);
                String customerProvisioningParamName = PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME.getName();
                String customerProvisioningMfName = getParameters().get(customerProvisioningParamName);
                updateCustomerProvisioningStatusMf(customerProvisioningMfName, order.getUserId(), ACTIVATED);
            } else if(event instanceof ChangeOfPlanEvent) {
                ChangeOfPlanEvent changeOfPlanEvent = (ChangeOfPlanEvent) event;
                Integer signupPluginId = Integer.parseInt(getMandatoryStringParameter(PARAM_SIGNUP_PLUGIN_ID.getName()));
                Map<String, String> signupParameters = collectParametersFromPlugin(signupPluginId, entityId);
                createOrder(changeOfPlanEvent, signupParameters);
            } else if(event instanceof OrderMetaFieldUpdateEvent) {
                IWebServicesSessionBean api = Context.getBean(Name.WEB_SERVICES_SESSION);
                OrderMetaFieldUpdateEvent orderMetaFieldUpdateEvent = (OrderMetaFieldUpdateEvent) event;
                Map<String, ValueDifference<Object>> orderMetaFieldDiff = orderMetaFieldUpdateEvent.getDiffMap();
                if(MapUtils.isEmpty(orderMetaFieldDiff)) {
                    logger.debug("order meta field diff not found for order {}", orderMetaFieldUpdateEvent.getOrderId());
                    return;
                }
                String orderProvisioningParamName = PARAM_ORDER_PROVISIONING_STATUS_MF_NAME.getName();
                String orderProvisioningMfName = getParameters().get(orderProvisioningParamName);
                if(!orderMetaFieldDiff.containsKey(orderProvisioningMfName)) {
                    logger.debug("{} not updated on order {}", orderProvisioningMfName, orderMetaFieldUpdateEvent.getOrderId());
                    return;
                }

                String orderProvisoiningStatus = (String) orderMetaFieldDiff.get(orderProvisioningMfName).rightValue();
                if(!PLAN_CHANGED.getStatus().equals(orderProvisoiningStatus)) {
                    logger.debug("skip swap asset for order {} since {} did not changed to {}", orderMetaFieldUpdateEvent.getOrderId(),
                            orderProvisioningMfName, PLAN_CHANGED);
                    return;
                }

                OrderWS oldOrder = api.getOrder(orderMetaFieldUpdateEvent.getOrderId());
                if(!isOrderRecurring(oldOrder)) {
                    logger.debug("change of plan only perform for monthly order and order {} is not monthly", oldOrder.getId());
                    return;
                }
                Calendar activeUntilDate = Calendar.getInstance();
                activeUntilDate.setTime(TimezoneHelper.companyCurrentDate(entityId));
                activeUntilDate.add(Calendar.DAY_OF_MONTH, -1);
                oldOrder.setActiveUntil(activeUntilDate.getTime());
                logger.debug("set newActiveUntilDate {} on old order {}", activeUntilDate.getTime(), oldOrder.getId());
                Map<Integer, List<Integer>> oldOrderItemAssetsMap = createItemAssetsMapFromOrder(oldOrder);
                logger.debug("Item asset map {} created from old order {}", oldOrderItemAssetsMap, oldOrder.getId());
                AssetDAS assetDAS = new AssetDAS();
                Map<Integer, List<Integer>> replacedItemAssetsMap = new HashMap<>();
                if(MapUtils.isNotEmpty(oldOrderItemAssetsMap)) {
                    for(OrderLineWS orderLineWS : oldOrder.getOrderLines()) {
                        if(null!= orderLineWS.getItemId() &&
                                oldOrderItemAssetsMap.containsKey(orderLineWS.getItemId())) {
                            List<Integer> assetIds = oldOrderItemAssetsMap.get(orderLineWS.getItemId());
                            for(Integer assetId : assetIds) {
                                AssetDTO assetDTO = assetDAS.findNow(assetId);
                                if(null == assetDTO) {
                                    logger.debug("asset {} not found", assetId);
                                    continue;
                                }
                                if(!assetDTO.getIdentifier().startsWith(SapphireSignupConstants.ASSET_PREFIX)) {
                                    Integer dummyAssetId = SapphireHelper.createAsset(orderLineWS.getItemId());
                                    List<Integer> replaceAssets = replacedItemAssetsMap.get(orderLineWS.getItemId());
                                    if(CollectionUtils.isEmpty(replaceAssets)) {
                                        replaceAssets = new ArrayList<>();
                                    }
                                    replaceAssets.add(assetId);
                                    replacedItemAssetsMap.put(orderLineWS.getItemId(), replaceAssets);
                                    assetIds.set(assetIds.indexOf(assetId), dummyAssetId);
                                    logger.debug("asset {} replaced with dummy asset {}", assetId, dummyAssetId);
                                }

                            }
                            logger.debug("updating line {} with assets {}", orderLineWS.getId(), assetIds);
                            orderLineWS.setAssetIds(assetIds.toArray(new Integer[0]));
                        }
                    }
                }

                Calendar startDate = Calendar.getInstance();
                startDate.setTime(TimezoneHelper.companyCurrentDate(entityId));
                startDate.add(Calendar.DAY_OF_MONTH, -2);
                Integer statusId = getOrderChangeApplyStatus(api);
                OrderChangeWS[] oldOrderChanges = OrderChangeBL.buildFromOrder(oldOrder, statusId);
                for(OrderChangeWS oldOrderChangeWS : oldOrderChanges) {
                    oldOrderChangeWS.setStartDate(startDate.getTime());
                }
                api.updateOrder(oldOrder, oldOrderChanges);
                logger.debug("old order {} updated", oldOrder.getId());

                String newOrderIdParamName = PARAM_NEW_ORDER_ID_MF_NAME.getName();
                String newOrderIdMfName = getParameters().get(newOrderIdParamName);
                MetaFieldValueWS newOrderIdFieldValue = findOrderMetaFieldByName(oldOrder, newOrderIdMfName);
                if(null == newOrderIdFieldValue || null == newOrderIdFieldValue.getValue()) {
                    logger.debug("{} not found on order level meta field for entity {}", newOrderIdMfName, entityId);
                    return;
                }
                OrderWS newOrder = api.getOrder(newOrderIdFieldValue.getIntegerValue());
                if(newOrder == null) {
                    logger.debug("invalid {} order id saved on order {}", newOrderIdFieldValue.getValue(), oldOrder.getId());
                    return;
                }
                Map<Integer, List<Integer>> newOrderItemAssetsMap = createItemAssetsMapFromOrder(newOrder);
                logger.debug("Item asset map {} created from new order {}", newOrderItemAssetsMap, newOrder.getId());
                OrderStatusWS activeStatus = new OrderStatusWS();
                activeStatus.setId(new OrderStatusDAS().getDefaultOrderStatusId(OrderStatusFlag.INVOICE, entityId));
                setOrderStatusActiveSinceDateOnOrder(newOrder, null, activeStatus);
                if(MapUtils.isNotEmpty(newOrderItemAssetsMap) &&
                        MapUtils.isNotEmpty(replacedItemAssetsMap)) {
                    for(OrderLineWS orderLineWS : newOrder.getOrderLines()) {
                        if(null!= orderLineWS.getItemId() &&
                                newOrderItemAssetsMap.containsKey(orderLineWS.getItemId())) {
                            List<Integer> assetIds = newOrderItemAssetsMap.get(orderLineWS.getItemId());
                            for(Integer assetId : assetIds) {
                                AssetDTO assetDTO = assetDAS.findNow(assetId);
                                if(null == assetDTO) {
                                    logger.debug("asset {} not found", assetId);
                                    continue;
                                }
                                if(assetDTO.getIdentifier().startsWith(SapphireSignupConstants.ASSET_PREFIX) &&
                                        replacedItemAssetsMap.containsKey(orderLineWS.getItemId())) {
                                    List<Integer> replaceAssets = replacedItemAssetsMap.get(orderLineWS.getItemId());
                                    if(CollectionUtils.isNotEmpty(replaceAssets)) {
                                        Integer replaceAssetId = replaceAssets.remove(0);
                                        logger.debug("replaced dummy asset {} with asset {}", assetId, replaceAssetId);
                                        assetIds.set(assetIds.indexOf(assetId), replaceAssetId);
                                    }
                                }

                            }
                            logger.debug("updating line {} with assets {}", orderLineWS.getId(), assetIds);
                            orderLineWS.setAssetIds(assetIds.toArray(new Integer[0]));
                        }
                    }
                    api.updateOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, statusId));
                    logger.debug("new order {} updated", newOrder.getId());
                }

                Optional<OrderWS> oldChild = findAssetEnabledChildOrder(oldOrder);
                if(!oldChild.isPresent()) {
                    logger.debug("no asset enabled child order found on old order {}", oldOrder.getId());
                    return;
                }

                Optional<OrderWS> newChild = findAssetEnabledChildOrder(newOrder);
                if(!newChild.isPresent()) {
                    logger.debug("no asset enabled child order found on new order {}", newOrder.getId());
                    return;
                }
                OrderWS newChildOrder = newChild.get();
                List<Integer> oldOrderAssetEnabledItems = collectItemIdsFromOrder(oldChild.get(), true);
                List<Integer> oldOrderItems = collectItemIdsFromOrder(oldChild.get(), false);
                for(OrderLineWS line : newChildOrder.getOrderLines()) {
                    if((oldOrderAssetEnabledItems.contains(line.getItemId()) && lineHasDummyAsset(line))
                            || oldOrderItems.contains(line.getItemId())) {
                        logger.debug("deleting line {} from new child order {}", line.getId(), newChildOrder.getId());
                        line.setDeleted(1);
                    }
                }
                api.updateOrder(newChildOrder, OrderChangeBL.buildFromOrder(newChildOrder, statusId));
                logger.debug("updated newchild order {}", newChildOrder.getId());
            } else if(event instanceof AssetUpdatedEvent) {
                AssetUpdatedEvent assetUpdatedEvent = (AssetUpdatedEvent) event;
                AssetDTO asset = assetUpdatedEvent.getAsset();
                AssetDAS assetDAS = Context.getBean(AssetDAS.class);
                if(asset.getIdentifier().startsWith(SapphireSignupConstants.ASSET_PREFIX) &&
                        asset.getAssetStatus().getIsAvailable() == 1) {
                    logger.debug("deleting dummy asset {}", asset.getId());
                    asset.setDeleted(1);
                    assetDAS.save(asset);
                }
            }
        } catch(PluggableTaskException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PluggableTaskException("erorr in SapphireProvisioninigResponseHandlerTask", ex);
        }
    }

    /**
     * collect assets from {@link OrderWS}
     * @param order
     * @return
     */
    private List<Integer> collectItemIdsFromOrder(OrderWS order, boolean onlyAssetEnabled) {
        Stream<OrderLineWS> lineStream = Arrays.stream(order.getOrderLines());
        lineStream = lineStream.filter(line -> onlyAssetEnabled ? ArrayUtils.isNotEmpty(line.getAssetIds()) :
            ArrayUtils.isEmpty(line.getAssetIds()));
        return lineStream
                .map(OrderLineWS::getItemId)
                .collect(Collectors.toList());
    }

    private boolean lineHasDummyAsset(OrderLineWS line) {
        if(ArrayUtils.isEmpty(line.getAssetIds())) {
            return false;
        }
        AssetDAS assetDAS = new AssetDAS();
        for(Integer assetId : line.getAssetIds()) {
            AssetDTO assetDTO = assetDAS.findNow(assetId);
            if(null == assetDTO) {
                continue;
            }
            if(assetDTO.getIdentifier().startsWith(SapphireSignupConstants.ASSET_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private Optional<OrderWS> findAssetEnabledChildOrder(OrderWS order) {
        if(ArrayUtils.isEmpty(order.getChildOrders())) {
            return Optional.empty();
        }
        for(OrderWS child : order.getChildOrders()) {
            for(OrderLineWS line : child.getOrderLines()) {
                if(ArrayUtils.isNotEmpty(line.getAssetIds())) {
                    return Optional.of(child);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isOrderRecurring(OrderWS order) {
        return !Constants.ORDER_PERIOD_ONCE.equals(order.getPeriod());
    }

    /**
     * Create ItemAssetMap from {@link OrderWS}
     * @param order
     * @return
     */
    private Map<Integer, List<Integer>> createItemAssetsMapFromOrder(OrderWS order) {
        Map<Integer, List<Integer>> itemAssetsMap = new HashMap<>();
        for(OrderLineWS orderLineWS : order.getOrderLines()) {
            if(null!= orderLineWS.getItemId() && ArrayUtils.isNotEmpty(orderLineWS.getAssetIds())) {
                itemAssetsMap.put(orderLineWS.getItemId(), Arrays.stream(orderLineWS.getAssetIds())
                        .collect(Collectors.toList()));
            }
        }
        return itemAssetsMap;
    }

    private MetaFieldValueWS findOrderMetaFieldByName(OrderWS order, String metaFieldName) {
        for(MetaFieldValueWS metaFieldValueWS : order.getMetaFields()) {
            if(metaFieldValueWS.getFieldName().equals(metaFieldName)) {
                return metaFieldValueWS;
            }
        }
        return null;
    }
    /**
     * Creates an order for given {@link ChangeOfPlanEvent}
     * @param changeOfPlanEvent
     * @throws PluggableTaskException
     */
    private void createOrder(ChangeOfPlanEvent changeOfPlanEvent, Map<String, String> signupPluginParams) throws PluggableTaskException {
        OrderDTO oldOrder = new OrderDAS().findNow(changeOfPlanEvent.getOrderId());
        Integer entityId = changeOfPlanEvent.getEntityId();
        String orderProvisioningParamName = PARAM_ORDER_PROVISIONING_STATUS_MF_NAME.getName();
        String orderProvisioningMfName = getParameters().get(orderProvisioningParamName);
        updateOrderProvisioningStatusMf(orderProvisioningMfName, oldOrder.getId(), PENDING_CHANGE_OF_PLAN);
        UserDTO user = oldOrder.getBaseUserByUserId();
        Integer orderPeriodId = Integer.parseInt(signupPluginParams.get(ORDER_PERIOD_ID_PARAM_NAME));
        IWebServicesSessionBean api = Context.getBean(Name.WEB_SERVICES_SESSION);
        OrderWS order = new OrderWS();
        order.setActiveSince(TimezoneHelper.companyCurrentDate(entityId));
        order.setUserId(user.getId());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(orderPeriodId);
        order.setProrateFlag(true);
        order.setCurrencyId(user.getCurrencyId());
        SapphireHelper.setOrderStatusAsPending(order, entityId);
        ItemDAS itemDAS = new ItemDAS();
        Integer itemId = itemDAS.findItemByInternalNumber(changeOfPlanEvent.getNewPlanCode(), entityId).getId();
        logger.debug("Creating Subscription order for user {} with subscription item {}", user.getId(), itemId);
        OrderLineWS subscriptionLine = new OrderLineWS();
        subscriptionLine.setItemId(itemId);
        subscriptionLine.setQuantity(BigDecimal.ONE);
        subscriptionLine.setUseItem(true);
        ItemDTOEx item = api.getItem(itemId, null, null);
        subscriptionLine.setTypeId(item.getOrderLineTypeId());
        subscriptionLine.setDescription(item.getDescription());
        order.setOrderLines(new OrderLineWS[] { subscriptionLine });
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, getOrderChangeApplyStatus(api));
        List<Integer> inventoryItemIds = SapphireHelper.getInventoryAllocationProductIds(signupPluginParams);
        for (OrderChangeWS orderChange : orderChanges) {
            orderChange.setAppliedManually(1);
            if (orderChange.getItemId().intValue() == itemId) {
                ItemDTO oldPlan = itemDAS.findItemByInternalNumber(changeOfPlanEvent.getExistingPlanCode(), entityId);
                List<Integer> oldPlanItemIds = SapphireHelper.collecteItemIdsFromPlanItems(
                        SapphireHelper.getAssetEnabledPlanItemsFromSubscriptionItem(oldPlan.getId()));
                List<OrderChangePlanItemWS> orderChangePlanItems = new ArrayList<>();
                for(PlanItemDTO planItem : SapphireHelper.getAssetEnabledPlanItemsFromSubscriptionItem(itemId)) {
                    OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                    orderChangePlanItem.setItemId(planItem.getItem().getId());
                    orderChangePlanItem.setId(0);
                    orderChangePlanItem.setBundledQuantity(planItem.getBundle().getQuantity().intValue());
                    Integer assetEnabledItemId = planItem.getItem().getId();
                    List<Integer> assetIds = new ArrayList<>();
                    if(CollectionUtils.isNotEmpty(inventoryItemIds)
                            && inventoryItemIds.contains(assetEnabledItemId) &&
                            !oldPlanItemIds.contains(assetEnabledItemId)) {
                        List<AssetDTO> assets = SapphireHelper.findAssetByItemWithLock(entityId, assetEnabledItemId, orderChangePlanItem.getBundledQuantity());
                        if(assets.size()!= orderChangePlanItem.getBundledQuantity()) {
                            logger.debug("insufficient asset for item {}", assetEnabledItemId);
                            throw new SessionInternalError("insufficient asset for item " + assetEnabledItemId);
                        }
                        assetIds.addAll(assets.stream().map(AssetDTO::getId).collect(Collectors.toList()));
                    } else {
                        for(int i=0; i<orderChangePlanItem.getBundledQuantity(); i++) {
                            assetIds.add(SapphireHelper.createAsset(assetEnabledItemId));
                        }
                    }
                    logger.debug("Creating order with assets {} for user {}", assetIds, user.getId());
                    orderChangePlanItem.setAssetIds(ArrayUtils.toPrimitive(assetIds.toArray(new Integer[0])));
                    orderChangePlanItems.add(orderChangePlanItem);
                }
                orderChange.setOrderChangePlanItems(orderChangePlanItems.toArray(new OrderChangePlanItemWS[0]));
            }
        }
        order.setId(api.createUpdateOrder(order, orderChanges));
        changeOfPlanEvent.setNewOrderId(order.getId());
        String newOrderIdParamName = PARAM_NEW_ORDER_ID_MF_NAME.getName();
        String newOrderIdMfName = getParameters().get(newOrderIdParamName);
        MetaField newOrderIdMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER }, newOrderIdMfName);
        oldOrder.setMetaField(newOrderIdMf, order.getId());
        logger.debug("subscription order {} created for user {}", order.getId(), user.getId());
    }

    /**
     * Creates {@link PluggableTaskParameterDTO} map for given plug in id.
     * @param pluginId
     * @param entityId
     * @return
     */
    private Map<String, String> collectParametersFromPlugin(Integer pluginId, Integer entityId) {
        PluggableTaskDAS pluggableTaskDAS = Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
        PluggableTaskDTO signupPlugin = pluggableTaskDAS.findNow(pluginId);
        Assert.notNull(signupPlugin, "SapphireSignupProcessTask not configured for entity " + entityId);
        Map<String, String> parameters = new HashMap<>();
        for(PluggableTaskParameterDTO parameterDTO : signupPlugin.getParameters()) {
            parameters.put(parameterDTO.getName(), parameterDTO.getValue());
        }
        return parameters;
    }

    private Integer getOrderChangeApplyStatus(IWebServicesSessionBean api) throws PluggableTaskException {
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        for(OrderChangeStatusWS orderChangeStatus : list) {
            if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return orderChangeStatus.getId();
            }
        }
        throw new PluggableTaskException("No order Change status found for entity "+ api.getCallerCompanyId());
    }

    private boolean isEnrollement(Map<Integer, Integer> assetMap) {
        AssetDAS assetDAS = new AssetDAS();
        for(Entry<Integer, Integer> oldNewAssetEntry : assetMap.entrySet()) {
            AssetDTO oldAsset = assetDAS.find(oldNewAssetEntry.getKey());
            AssetDTO newAsset = assetDAS.find(oldNewAssetEntry.getValue());
            if(oldAsset.getIdentifier().startsWith(SapphireSignupConstants.ASSET_PREFIX) &&
                    !newAsset.getIdentifier().startsWith(SapphireSignupConstants.ASSET_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return EVENTS;
    }

    /**
     * Validates plugin parameters
     * @param entityId
     * @throws PluggableTaskException
     */
    private void validateReqiredParameters(Integer entityId) throws PluggableTaskException {
        String customerProvisioningParamName = PARAM_CUSTOMER_PROVISIONING_STATUS_MF_NAME.getName();
        String customerProvisioningMfName = getParameters().get(customerProvisioningParamName);
        if(StringUtils.isEmpty(customerProvisioningMfName)) {
            logger.error("{} parameter not configured for plugin {} for entity {}", customerProvisioningParamName, this.getClass().getSimpleName(), entityId);
            throw new PluggableTaskException("parameter "+ customerProvisioningMfName + " not configured for plugin "+
                    this.getClass().getSimpleName() + " for entity "+ entityId);
        }
        MetaField provisioningMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.CUSTOMER }, customerProvisioningMfName);
        if(null == provisioningMf) {
            logger.error("{} not present on customer level metafield for entity {}", customerProvisioningMfName, entityId);
            throw new PluggableTaskException(customerProvisioningMfName + " not found on customer level for entity "+ entityId);
        }

        String orderProvisioningParamName = PARAM_ORDER_PROVISIONING_STATUS_MF_NAME.getName();
        String orderProvisioningMfName = getParameters().get(orderProvisioningParamName);
        if(StringUtils.isEmpty(orderProvisioningMfName)) {
            logger.error("{} parameter not configured for plugin {} for entity {}", orderProvisioningParamName, this.getClass().getSimpleName(), entityId);
            throw new PluggableTaskException("parameter "+ orderProvisioningMfName + " not configured for plugin "+
                    this.getClass().getSimpleName() + " for entity "+ entityId);
        }
        MetaField orderProvisioningMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER }, orderProvisioningMfName);
        if(null == orderProvisioningMf) {
            logger.error("{} not present on Order level metafield for entity {}", orderProvisioningMfName, entityId);
            throw new PluggableTaskException(orderProvisioningMfName + " not found on order level for entity "+ entityId);
        }

        String newOrderIdParamName = PARAM_NEW_ORDER_ID_MF_NAME.getName();
        String newOrderIdMfName = getParameters().get(newOrderIdParamName);
        if(StringUtils.isEmpty(newOrderIdMfName)) {
            logger.error("{} parameter not configured for plugin {} for entity {}", newOrderIdParamName, this.getClass().getSimpleName(), entityId);
            throw new PluggableTaskException("parameter "+ newOrderIdParamName + " not configured for plugin "+
                    this.getClass().getSimpleName() + " for entity "+ entityId);
        }
        MetaField newOrderIdMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER }, newOrderIdMfName);
        if(null == newOrderIdMf) {
            logger.error("{} not present on order level metafield for entity {}", newOrderIdMfName, entityId);
            throw new PluggableTaskException(newOrderIdMfName + " not found on order level for entity "+ entityId);
        }

        Integer signupPluginId = Integer.parseInt(getMandatoryStringParameter(PARAM_SIGNUP_PLUGIN_ID.getName()));
        PluggableTaskDAS pluggableTaskDAS = Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
        PluggableTaskDTO signupPlugin = pluggableTaskDAS.findNow(signupPluginId);
        if(null == signupPlugin) {
            logger.error("{} not configured for entity {}", SapphireSignupProcessTask.class.getSimpleName(), entityId);
            throw new PluggableTaskException(SapphireSignupProcessTask.class.getSimpleName() + " not configured for entity "+ entityId);
        }
    }

}
