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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;

public class OrderJsonBuilder {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final PlanDTO EMPTY_PLAN = new PlanDTO();
    private static final String UNION = "%s~%s";
    private static final String DELIMITER = "\""+UNION+"\"";
    private static final String DOUBLE_QUOTE_FORMAT = "\"%s\"";
    private static final String EMPTY = "\"\"";
    private static final String SEPARATOR = ",\n";

    public static String build(OrderDTO order) throws JsonProcessingException {
        Integer languageId = order.getBaseUserByUserId().getLanguage().getId();
        Integer entityId = order.getBaseUserByUserId().getEntity().getId();

        List<ItemDTO> lineItems = order.getLines()
                                       .stream()
                                       .map(OrderLineDTO::getItem)
                                       .filter(item -> !item.isPlan())
                                       .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(order.getChildOrders())) {
            order.getChildOrders()
                 .stream()
                 .flatMap(childOrder -> childOrder.getLines().stream())
                 .map(OrderLineDTO::getItem)
                 .filter(item -> null != item && !item.isPlan())
                 .collect(Collectors.toCollection(() -> lineItems));
        }

        if (order.getParentOrder() != null) {
            order.getLines()
                 .stream()
                 .map(OrderLineDTO::getItem)
                 .filter(item -> null != item && !item.isPlan())
                 .collect(Collectors.toCollection(() -> lineItems));
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        OrderLineDAS orderLineDAS = new OrderLineDAS();
        OrderWS orderWS = new OrderBL(order).getWS(languageId);
        orderWS.setParentOrder(null);
        orderWS.setChildOrders(null);

        return new StringBuilder().append("{")
                                  .append("\"orderid\": ")
                                  .append(doubleQuoteFormat(order.getId()))
                                  .append(SEPARATOR)
                                  .append("\"dubaiid\": ")
                                  .append(doubleQuoteFormat(order.getBaseUserByUserId().getUserName()))
                                  .append(SEPARATOR)
                                  .append("\"reference_no\": ")
                                  .append(doubleQuoteFormat(Objects.toString(MetaFieldBL.getMetaFieldValueNullSafety(order.getMetaField("Reference No")))))
                                  .append(SEPARATOR)
                                  .append("\"change_timestamp\": ")
                                  .append(doubleQuoteFormat(DateConvertUtils.asLocalDateTime(new Date()).format(TIMESTAMP_FORMATTER)))
                                  .append(SEPARATOR)
                                  .append("\"purchase_date\": ")
                                  .append(doubleQuoteFormat(DateConvertUtils.asLocalDate(order.getActiveSince()).format(FORMATTER)))
                                  .append(SEPARATOR)
                                  .append("\"expiry_date\": ")
                                  .append(doubleQuoteFormat(order.getActiveUntil() == null ? Objects.toString(order.getActiveUntil())
                                                                                           : DateConvertUtils.asLocalDate(order.getActiveUntil()).format(FORMATTER)))
                                  .append(SEPARATOR)
                                  .append("\"usage\":")
                                  .append(EMPTY)
                                  .append(SEPARATOR)
                                  .append("\"status\": ")
                                  .append(doubleQuoteFormat(order.getOrderStatus().getDescription(languageId)))
                                  .append(SEPARATOR)
                                  .append("\"selected_dependencies\": [")
                                  .append(addDependencyInformation(getItems(getPlan(order.getLines())), lineItems, languageId))
                                  .append("\n]")
                                  .append(SEPARATOR)
                                  .append("\"upgrade_flag\": ")
                                  .append(doubleQuoteFormat(Boolean.toString(order.getUpgradeOrderId() != null)))
                                  .append(SEPARATOR)
                                  .append("\"renew_flag\": ")
                                  .append(doubleQuoteFormat(Boolean.toString(order.getRenewOrderId() != null)))
                                  .append(SEPARATOR)
                                  .append("\"upgrade_orderid\": ")
                                  .append(doubleQuoteFormat(order.getUpgradeOrderId()))
                                  .append(SEPARATOR)
                                  .append("\"renew_orderid\": ")
                                  .append(doubleQuoteFormat(order.getRenewOrderId()))
                                  .append(SEPARATOR)
                                  .append("\"parent_orderid\": ")
                                  .append(doubleQuoteFormat(order.getParentUpgradeOrderId()))
                                  .append(SEPARATOR)
                                  .append("\"prov_details\": [")
                                  .append(addMetaFields(order.getLines()
                                                             .stream()
                                                             .flatMap(line -> line.getMetaFields().stream())
                                                             .collect(Collectors.toList())))
                                  .append("\n]")
                                  .append(SEPARATOR)
                                  .append("\"plan_details\": {")
                                  .append(addPlanInformation(getPlan(order.getLines()), languageId, entityId))
                                  .append("}")
                                  .append(SEPARATOR)
                                  .append("\"order_object\":")
                                  .append(mapper.writeValueAsString(orderWS))
                                  .append("\n}")
                                  .toString();
    }

    private static String doubleQuoteFormat(Object s) {
        return String.format(DOUBLE_QUOTE_FORMAT, s);
    }

    private static PlanDTO getPlan(List<OrderLineDTO> orderLines) {
        return orderLines.stream()
                         .filter(line -> line.getItem().isPlan() && line.getDeleted() == 0)
                         .map(line -> line.getItem().getPlans().iterator().next())
                         .findFirst()
                         .orElse(EMPTY_PLAN);
    }

    private static List<ItemDTO> getItems(PlanDTO plan) {
        return plan.getPlanItems()
                   .stream()
                   .map(PlanItemDTO::getItem)
                   .collect(Collectors.toList());

    }

    private static String addMetaFields(List<MetaFieldValue> metaFields) {
        if (CollectionUtils.isEmpty(metaFields)) {
            return doubleQuoteFormat(StringUtils.EMPTY);
        }

        return metaFields.stream()
                         .map(mf -> String.format(DELIMITER, mf.getField().getName(), Objects.toString(mf.getValue())))
                         .collect(Collectors.joining(SEPARATOR));
    }

    private static String addDependencyInformation(List<ItemDTO> planItems, List<ItemDTO> lineItems, Integer languageId) {
        if (CollectionUtils.isEmpty(planItems)) {
            return doubleQuoteFormat(StringUtils.EMPTY);
        }

        Set<Integer> dependencyIds = planItems.stream()
                                              .flatMap(item -> item.getDependencies().stream())
                                              .filter(itemDependency -> itemDependency.getType().equals(ItemDependencyType.ITEM_TYPE))
                                              .map(dependency -> (ItemTypeDTO) dependency.getDependent())
                                              .map(ItemTypeDTO::getId)
                                              .collect(Collectors.toSet());

        planItems.stream()
                 .flatMap(item -> item.getDependencies().stream())
                 .filter(itemDependency -> itemDependency.getType().equals(ItemDependencyType.ITEM_TYPE))
                 .map(ItemDependencyDTO::getItem)
                 .flatMap(item -> item.getItemTypes().stream())
                 .map(ItemTypeDTO::getId)
                 .collect(Collectors.toCollection(() -> dependencyIds));

        return lineItems.stream()
                        .filter(item -> item.getItemTypes()
                                            .stream()
                                            .anyMatch(itemType -> dependencyIds.contains(itemType.getId())))
                        .distinct()
                        .map(item -> String.format(DELIMITER, item.getItemTypes().iterator().next().getDescription(),
                                                              item.getDescription(languageId)))
                        .collect(Collectors.joining(SEPARATOR));
    }

    private static String addPlanInformation(PlanDTO plan, Integer languageId, Integer entityId) {
        if (plan == null || EMPTY_PLAN.equals(plan)) {
            return StringUtils.EMPTY;
        }

        return new StringBuilder().append("\"plan_name\": ")
                                  .append(doubleQuoteFormat(plan.getItem().getDescription(languageId)))
                                  .append(SEPARATOR)
                                  .append("\"planid\": ")
                                  .append(doubleQuoteFormat(plan.getId()))
                                  .append(SEPARATOR)
                                  .append("\"plan_number\": ")
                                  .append(doubleQuoteFormat(plan.getPlanSubscriptionItem().getInternalNumber()))
                                  .append(SEPARATOR)
                                  .append("\"description\": ")
                                  .append(doubleQuoteFormat(plan.getDescription()))
                                  .append(SEPARATOR)
                                  .append("\"category\": [")
                                  .append(plan.getPlanSubscriptionItem()
                                              .getItemTypes()
                                              .stream()
                                              .map(category -> doubleQuoteFormat(category.getDescription()))
                                              .collect(Collectors.joining(SEPARATOR)))
                                  .append("]")
                                  .append(SEPARATOR)
                                  .append("\"contract\": [")
                                  .append(EMPTY)
                                  .append("]")
                                  .append(SEPARATOR)
                                  .append("\"planItems\": [")
                                  .append(addPlanItemsInformation(plan.getPlanItems(), languageId, entityId))
                                  .append("]")
                                  .toString();
    }

    private static String addPlanItemsInformation(List<PlanItemDTO> planItems, Integer languageId, Integer entityId) {
        if (CollectionUtils.isEmpty(planItems)) {
            return doubleQuoteFormat(StringUtils.EMPTY);
        }

        Integer setupCategoryId = MetaFieldBL.getMetaFieldIntegerValueNullSafety(new EntityBL(entityId).getEntity().getMetaField(Constants.SETUP_META_FIELD));
        return planItems.stream()
                        .filter(planItem -> planItem.getItem()
                                                    .getItemTypes()
                                                    .stream()
                                                    .noneMatch(itemType -> setupCategoryId != null && itemType.getId() == setupCategoryId))
                        .map(item -> String.format(DELIMITER, item.getItem().getDescription(languageId),
                                     String.format(UNION, Objects.toString(MetaFieldBL.getMetaFieldValueNullSafety(item.getItem().getMetaField("Value"))),
                                                          Objects.toString(MetaFieldBL.getMetaFieldValueNullSafety(item.getItem().getMetaField("Unit"))))))
                        .collect(Collectors.joining(SEPARATOR));
    }
}
