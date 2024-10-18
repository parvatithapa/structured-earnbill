package com.sapienter.jbilling.server.spc;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.STR;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.order.event.OrderUpdatedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Context;

public class SpcOrderValidationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ParameterDescription PARAM_EXCULDED_CATEGORIES = new ParameterDescription("exculded_categories", true, STR);
    private static final ParameterDescription PARAM_ORDER_LINE_SERVICE_IDENTIFIER_MF_NAME = new ParameterDescription("OrderLine level service identifier mf name", true, STR);
    private static final ParameterDescription PARAM_ASSET_SERVICE_IDENTIFIER_MF_NAME = new ParameterDescription("Asset level service identifier mf name", true, STR);

    public SpcOrderValidationTask() {
        descriptions.add(PARAM_EXCULDED_CATEGORIES);
        descriptions.add(PARAM_ORDER_LINE_SERVICE_IDENTIFIER_MF_NAME);
        descriptions.add(PARAM_ASSET_SERVICE_IDENTIFIER_MF_NAME);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[] {
        NewOrderEvent.class,
        OrderUpdatedEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        logger.debug("processing event {} for entity {}", event, getEntityId());
        try {
            Integer orderId;
            if(event instanceof NewOrderEvent) {
                NewOrderEvent newOrderEvent = (NewOrderEvent) event;
                orderId = newOrderEvent.getOrder().getId();
            } else {
                OrderUpdatedEvent orderUpdatedEvent = (OrderUpdatedEvent) event;
                orderId = orderUpdatedEvent.getOrderId();
            }
            validateOrder(orderId);
        } catch(PluggableTaskException ex) {
            throw ex;
        } catch (Exception e) {
            logger.error("order validation failed");
            throw new PluggableTaskException("error in order validation", e);
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /**
     * Validates order
     * @param orderId
     * @throws PluggableTaskException
     */
    private void validateOrder(Integer orderId) throws PluggableTaskException {
        OrderDTO order = Context.getBean(OrderDAS.class).findNow(orderId);
        if(order.getIsMediated()) {
            logger.debug("skip order validation for mediated order {}", order.getId());
            return;
        }
        OrderDTO parentOrder = order.getParentOrder();
        if(null!= parentOrder) {
            for(OrderLineDTO orderLine : parentOrder.getLines()) {
                ItemDTO lineItem = orderLine.getItem();
                if(null == lineItem) {
                    logger.debug("skipping line {} since no item found on order {}", orderLine.getId(), parentOrder.getId());
                    continue;
                }
                if(lineItem.isPlan()) {
                    logger.debug("no need of validation since order {} is child order of {}", order.getId(), parentOrder.getId());
                    return;
                }
            }
        }
        Map<String, Object> paramMap = validateAndReturnParam();
        @SuppressWarnings("unchecked")
        List<Integer> categories = (List<Integer>) paramMap.get(PARAM_EXCULDED_CATEGORIES.getName());
        logger.debug("exlcuded categories {}", categories);
        String assetServiceMfName = (String) paramMap.get(PARAM_ORDER_LINE_SERVICE_IDENTIFIER_MF_NAME.getName());
        List<String> userSubscribedAssetServiceNumbers = collectAssetServiceNumbersForUser(order.getUserId());
        for(OrderLineDTO line : order.getLines()) {
            ItemDTO lineItem = line.getItem();
            if(null == lineItem) {
                logger.debug("skipping line {} since no item found on order {}", line.getId(), order.getId());
                continue;
            }
            boolean shoudSkip = false;
            for(Integer typeId : categories) {
                if(lineItem.belongsToCategory(typeId)) {
                    logger.debug("skipping line {} since item {} belongs to excuded categories", line.getId(), lineItem.getId());
                    shoudSkip = true;
                    break;
                }
            }
            if(shoudSkip) {
                continue;
            }
            if(lineItem.isPlan()) {
                logger.debug("skipping line {} since plan item found on order {}", line.getId(), order.getId());
                break;
            }
            if(CollectionUtils.isNotEmpty(line.getAssets())) {
                logger.debug("skipping line {} since asset enabled item found on order {}", line.getId(), order.getId());
                continue;
            }
            @SuppressWarnings("unchecked")
            MetaFieldValue<String> assetServiceNumber = line.getMetaField(assetServiceMfName);
            if(null == assetServiceNumber || StringUtils.isEmpty(assetServiceNumber.getValue())) {
                logger.error("{} not specified on line {} for order {}",assetServiceMfName, line.getId(), order.getId());
                throw new PluggableTaskException(assetServiceMfName + " not specified on order "+ order.getId());
            }
            if(!userSubscribedAssetServiceNumbers.contains(assetServiceNumber.getValue())) {
                logger.debug("order line {} contains invalid service identifier {}", line.getId(), assetServiceNumber.getValue());
                throw new PluggableTaskException("order line "+ line.getId() + " contains invalid "+ assetServiceNumber.getValue() + " service identifier");
            }
        }
    }

    /**
     * Validates plugin parameters and return values in Map.
     * @return
     * @throws PluggableTaskException
     */
    private Map<String, Object> validateAndReturnParam() throws PluggableTaskException {
        Integer entityId = getEntityId();
        String excludedParamName = PARAM_EXCULDED_CATEGORIES.getName();
        String exculdedCategoriesParam = getMandatoryStringParameter(excludedParamName);
        Map<String, Object> paramMap = new HashMap<>();
        List<Integer> categories = new ArrayList<>();
        for(String category : exculdedCategoriesParam.split(",")) {
            if(!NumberUtils.isNumber(category)) {
                logger.debug("invalid value {} passed to parameter {} for entity {}", exculdedCategoriesParam, excludedParamName, entityId);
                throw new PluggableTaskException("invalid value " + exculdedCategoriesParam + " passed to "+ excludedParamName);
            }
            categories.add(Integer.parseInt(category));
        }
        paramMap.put(excludedParamName, categories);
        String orderLineLevelMfParamName = PARAM_ORDER_LINE_SERVICE_IDENTIFIER_MF_NAME.getName();
        String orderLineLevelMfName = getMandatoryStringParameter(orderLineLevelMfParamName);
        MetaField orderLineLevelMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER_LINE }, orderLineLevelMfName);
        if(null == orderLineLevelMf) {
            logger.error("{} not present on order line level metafield for entity {}", orderLineLevelMfName, entityId);
            throw new PluggableTaskException(orderLineLevelMfName + " not found on order line level for entity "+ entityId);
        }
        paramMap.put(orderLineLevelMfParamName, orderLineLevelMfName);

        String assetServiceMfParamName = PARAM_ASSET_SERVICE_IDENTIFIER_MF_NAME.getName();
        String assetServiceMfName = getMandatoryStringParameter(assetServiceMfParamName);
        MetaField assetMetaField = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ASSET }, assetServiceMfName);
        if(null == assetMetaField) {
            logger.error("{} not present on asset level metafield for entity {}", assetServiceMfName, entityId);
            throw new PluggableTaskException(assetServiceMfName + " not found on asset level for entity "+ entityId);
        }
        return paramMap;
    }

    /**
     *
     * @param userId
     * @return
     * @throws PluggableTaskException
     */
    private List<String> collectAssetServiceNumbersForUser(Integer userId) throws PluggableTaskException {
        String assetServiceNumberMfName = getMandatoryStringParameter(PARAM_ASSET_SERVICE_IDENTIFIER_MF_NAME.getName());
        List<String> assetServiceNumbers = new ArrayList<>();
        for(OrderDTO order : new OrderDAS().findRecurringOrders(userId)) {
            for(AssetDTO asset : order.getAssets()) {
                @SuppressWarnings("unchecked")
                MetaFieldValue<String> assetServiceNumber = asset.getMetaField(assetServiceNumberMfName);
                if(null!= assetServiceNumber && StringUtils.isNotEmpty(assetServiceNumber.getValue())) {
                    assetServiceNumbers.add(assetServiceNumber.getValue());
                } else {
                    logger.debug("{}'s value not set on  asset {}", assetServiceNumberMfName, asset.getId());
                    assetServiceNumbers.add(asset.getIdentifier());
                }
            }
        }
        logger.debug("asset service numbers {} for user {}", assetServiceNumbers, userId);
        return assetServiceNumbers;
    }

}
