package com.sapienter.jbilling.server.fullcreative;

import static com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.UNDER_INITIAL_INCREMENT_THRESHOLD_QUANTITY_FIELD_NAME;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.tasks.TelcoUsageManagerTask;
import com.sapienter.jbilling.server.mediation.JMRQuantity;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderHelper;
import com.sapienter.jbilling.server.order.OrderLineBL;
import com.sapienter.jbilling.server.order.OrderServiceImpl;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.PriceModelResolutionContext;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.tasks.PriceModelPricingTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
public class FullCreativeOrderServiceImpl extends OrderServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String dnisPricingFieldName;
    @Autowired
    private MediationQuantityHelperService quantityHelperService;

    public FullCreativeOrderServiceImpl(String dnisPricingFieldName) {
        this.dnisPricingFieldName = dnisPricingFieldName;
        Assert.hasLength(dnisPricingFieldName, "dnisPricingFieldName can not be null or empty!");
    }

    private static final String SQL = "SELECT COUNT(*) FROM pluggable_task WHERE entity_id  = :entityId AND "
            + "type_id = (SELECT id FROM pluggable_task_type WHERE class_name = :className)";

    private boolean isTelcoUsageManagerPluginConfigured(Integer entityId) {
        SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
        SQLQuery query = sf.getCurrentSession().createSQLQuery(SQL);
        query.setParameter("entityId", entityId);
        query.setParameter("className", TelcoUsageManagerTask.class.getName());
        BigInteger count = (BigInteger) query.uniqueResult();
        return count.intValue()!=0;
    }

    private boolean itemHasFlatPricing(OrderDTO order, Integer itemId, Integer entityId) {
        PriceModelPricingTask priceModelPricingTask = null;
        try {
            PluggableTaskManager<PriceModelPricingTask> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_ITEM_PRICING);
            priceModelPricingTask = taskManager.getNextClass();
            if (priceModelPricingTask == null) {
                logger.error("No task is configured");
                throw new SessionInternalError("No Price Model Pricing Task Available");
            }
        } catch(PluggableTaskException e) {
            throw new SessionInternalError("Error Handling Price Model Pricing Task");
        }
        Date pricingDate = priceModelPricingTask.getPricingDate(order);
        int userId = order.getBaseUserByUserId().getUserId();

        PriceModelResolutionContext modelResolutionContext = PriceModelResolutionContext.builder(itemId)
                .user(userId)
                .pricingDate(pricingDate)
                .attributes(Collections.emptyMap())
                .build();

        SortedMap<Date, PriceModelDTO> models = priceModelPricingTask.getPricesByHierarchy(modelResolutionContext);
        if(MapUtils.isNotEmpty(models)) {
            PriceModelDTO model = PriceModelBL.getPriceForDate(models, pricingDate);
            return model.getType().equals(PriceModelStrategy.FLAT);
        }
        ItemDTO item = new ItemDAS().find(itemId);
        return item.isFlatPricingProduct();
    }

    @Override
    protected List<OrderLineDTO> copyOldOrderLinesAndUpdateJMROnLine(OrderDTO order, JbillingMediationRecord jmr) {
        OrderBL bl = new OrderBL(order);
        UserBL userBL = new UserBL(jmr.getUserId());
        String callIdentifier = getDNISValue(jmr.getPricingFields());
        Integer entityId = userBL.getEntityId(jmr.getUserId());
        List<OrderLineDTO> oldLines = OrderHelper.copyOrderLinesToDto(getLines(order, callIdentifier, jmr.getItemId(), entityId));
        Integer itemId = jmr.getItemId();
        BigDecimal quantity = jmr.getQuantity();
        // add the line to the current order
        bl.addItem(itemId, quantity, userBL.getLanguage(), jmr.getUserId(), entityId,
                userBL.getCurrencyId(), getCallDataRecords(jmr.getPricingFields()), true, jmr.getEventDate());

        List<OrderLineDTO> updatedLines = getLines(bl.getDTO(), callIdentifier, itemId, entityId);
        // set isMediated flag true if line pass from mediation.
        for(OrderLineDTO line : updatedLines) {
            logger.debug("Setting Mediated Quantity {} on order line {}", quantity, line.getId());
            line.setMediatedQuantity(quantity);
            line.setMediated(true);
        }

        return oldLines;
    }

    @Override
    protected void fireOrderLineQuantitiesEvent(List<OrderLineDTO> oldLines, OrderDTO order, JbillingMediationRecord jmr) {
        OrderBL bl = new OrderBL(order);
        Integer entityId = order.getBaseUserByUserId().getEntity().getId();
        String callIdentifier = getDNISValue(jmr.getPricingFields());
        bl.checkOrderLineQuantities(oldLines, getLines(order, callIdentifier, jmr.getItemId(), entityId), entityId,
                bl.getDTO().getId(), true, false);
    }

    @Override
    protected List<OrderLineDTO> calculateDifflines(List<OrderLineDTO> oldLines, OrderDTO order, JbillingMediationRecord jmr) {
        String callIdentifier = getDNISValue(jmr.getPricingFields());
        Integer entityId = order.getBaseUserByUserId().getEntity().getId();
        List<OrderLineDTO> diffLines = OrderLineBL.diffOrderLines(oldLines, getLines(order, callIdentifier, jmr.getItemId(), entityId));
        if(CollectionUtils.isEmpty(diffLines)) {
            logger.debug("Call identifier {} with {} quantity for order {} is not billable", callIdentifier, jmr.getQuantity(), order.getId());
        }
        return diffLines;
    }

    @Override
    protected void processLines(OrderDTO order, Integer languageId,
            Integer entityId, Integer userId, Integer currencyId,
            String pricingFields, Integer itemId) {
        logger.debug("Processing order lines for item {}", itemId);
        List<PricingField> pricingFieldsList = Arrays.asList(PricingField.getPricingFieldsValue(pricingFields));
        String callIdentifier = PricingField.find(pricingFieldsList, dnisPricingFieldName).getStrValue();
        PricingField underInitialQunaity = PricingField.find(pricingFieldsList, UNDER_INITIAL_INCREMENT_THRESHOLD_QUANTITY_FIELD_NAME);
        for(OrderLineDTO line : getLines(order, callIdentifier, itemId, entityId)) {
            if( null != underInitialQunaity && quantityHelperService.isQauntityExceeded(userId, callIdentifier) == 0) {
                line.incrementFreeCallCounter();
            }
            new OrderBL(order).processLine(line, languageId, entityId, userId, currencyId, pricingFields);
        }

    }

    @Override
    public JMRQuantity resolveQuantity(JbillingMediationRecord jmr, PricingField[] fields) {
        JMRQuantity.JMRQuantityBuilder resolvedQuantity = JMRQuantity.builder();
        Integer userId = jmr.getUserId();
        try {
            List<PricingField> pricingFields = Arrays.asList(fields);
            PricingField underInitialQunaity = PricingField.find(pricingFields, UNDER_INITIAL_INCREMENT_THRESHOLD_QUANTITY_FIELD_NAME);
            String callIdentifer = PricingField.find(pricingFields, dnisPricingFieldName).getStrValue();
            logger.debug("before changing quantity for user {} call identifer {} is {}", userId, callIdentifer, jmr.getQuantity());
            if(null == underInitialQunaity || quantityHelperService.isQauntityExceeded(userId, callIdentifer) <= 0) {
                logger.debug("free call limit not exceeded for user {} for call identifier {}", userId, callIdentifer);
                return JMRQuantity.NONE;
            }
            logger.debug("free call limit exceeded for user {} for call identifier {}", userId, callIdentifer);
            resolvedQuantity.quantity(underInitialQunaity.getDecimalValue());
            return resolvedQuantity.build();
        } catch(Exception ex) {
            logger.error("qunaity resolution failed for user {} for record key {}", userId, jmr.getRecordKey(), ex);
            resolvedQuantity.errors("ERROR-QUANTITY-RESOLUTION");
            return resolvedQuantity.build();
        }
    }

    private List<PricingField> getPricingFields(String pricingFields) {
        return Arrays.stream(PricingField.getPricingFieldsValue(pricingFields))
                .collect(Collectors.toList());
    }

    private String getDNISValue(String pricingFields) {
        PricingField dnisField = PricingField.find(getPricingFields(pricingFields), dnisPricingFieldName);
        return dnisField.getStrValue();
    }

    private List<OrderLineDTO> getLines(OrderDTO order, String dnisValue, Integer itemId, Integer entityId) {
        if(isTelcoUsageManagerPluginConfigured(entityId) && itemHasFlatPricing(order, itemId, entityId)) {
            return order.getLines()
                    .stream()
                    .filter(line -> line.getCallIdentifier()!=null && line.getItemId().equals(itemId))
                    .filter(line -> dnisValue.equals(line.getCallIdentifier()))
                    .collect(Collectors.toList());
        } else {
            return order.getLines()
                    .stream()
                    .filter(line -> itemId.equals(line.getItemId()))
                    .collect(Collectors.toList());
        }

    }

}
