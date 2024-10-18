package com.sapienter.jbilling.server.order.db;

import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.csv.DynamicExport;
import com.sapienter.jbilling.server.util.csv.ExportableWrapper;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.EntityType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderExportableWrapper implements ExportableWrapper<OrderDTO> {

    private static final Map<Integer, Map<String, String>> ORDER_LINE_META_FIELD_MAP = new ConcurrentHashMap<>();
    private static final List<String> PURCHASE_ORDER_FIELDS = getPurchaseOrderFields();
    private static final List<String> orderLineFields = getOrderLineFields();
    private static final String SEPARATOR = ", ";

    private Map<Integer, Map<Integer, List<String>>> orderAndOrderLineAssetIdentifierMap;
    private Integer orderId;
    private OrderLineDAS lineDAS;
    private MetaFieldDAS metaFieldDAS;
    private DynamicExport dynamicExport = DynamicExport.NO;

    private void init(Integer orderId) {
        this.orderId = orderId;
        lineDAS = new OrderLineDAS();
        metaFieldDAS = new MetaFieldDAS();
    }

    public OrderExportableWrapper(Integer orderId, DynamicExport dynamicExport) {
        init(orderId);
        setDynamicExport(dynamicExport);
    }

    public OrderExportableWrapper(Integer orderId) {
        init(orderId);
    }

    public OrderExportableWrapper() {
    }

    @Override
    public String[] getFieldNames() {
        OrderDTO order = getWrappedInstance();
        if (order != null && dynamicExport.equals(DynamicExport.YES)) {
            return getOrderAndMetaFieldsFieldNames();
        }

        return getOrderFieldNames();
    }

    public String[] getOrderAndMetaFieldsFieldNames() {
        ORDER_LINE_META_FIELD_MAP.clear();

        List<String> fieldsList = new ArrayList<>();
        fieldsList.addAll(PURCHASE_ORDER_FIELDS);
        fieldsList.addAll(orderLineFields);
        fieldsList.addAll(getOrderLineMetafieldNames().keySet());

        return fieldsList.toArray(new String[fieldsList.size()]);
    }

    public Object[][] getOrderAndMetaFieldsWithValues() {
        OrderDTO order = getWrappedInstance();
        List<Object[]> values = new ArrayList<>();
        List<String> allOrderAssets = getAllOrderAssets();
        UserDTO user = order.getBaseUserByUserId();
        Integer languageId = user.getLanguage().getId();
        // main invoice row
        values.add(
                new Object[]{
                        order.getId(),
                        user.getId(),
                        user.getUserName(),
                        order.getOrderStatus() != null ? order.getOrderStatus().getDescription(languageId) : null,
                        order.getOrderPeriod() != null ? order.getOrderPeriod().getDescription(languageId) : null,
                        order.getOrderBillingType() != null ? order.getOrderBillingType().getDescription(languageId) : null,
                        order.getCurrency() != null ? order.getCurrency().getDescription(languageId) : null,
                        order.getTotal(),
                        order.getActiveSince(),
                        order.getActiveUntil(),
                        order.getCycleStarts(),
                        order.getCreateDate(),
                        getNextBillableDay(order),
                        order.getProrateFlag(),
                        allOrderAssets.isEmpty() ? StringUtils.EMPTY : String.join(SEPARATOR, allOrderAssets),
                        null,
                        order.getNotes()
                }
        );

        // indented row for each order line
        values.addAll(lineDAS.findOrderLinesByOrder(order.getId()).stream()
                .map(this::getOrderLineValues)
                .collect(Collectors.toList()));

        return values.toArray(new Object[values.size()][]);
    }

    @Override
    public Object[][] getFieldValues() {
        this.orderAndOrderLineAssetIdentifierMap = new AssetDAS().findAssetsIdentifierByOrderId(orderId);
        if (dynamicExport.equals(DynamicExport.YES)) {
            return getOrderAndMetaFieldsWithValues();
        }
        return getOrderFieldValues();
    }

    @Override
    public OrderDTO getWrappedInstance() {
        return new OrderDAS().find(orderId);
    }


    public String[] getOrderFieldNames() {
        return new String[]{
                "id",
                "userId",
                "userName",
                "status",
                "period",
                "billingType",
                "currency",
                "total",
                "activeSince",
                "activeUntil",
                "cycleStart",
                "createdDate",
                "nextBillableDay",
                "notes",

                // order lines
                "lineItemId",
                "lineProductCode",
                "lineQuantity",
                "linePrice",
                "lineAmount",
                "lineDescription"
        };
    }

    @SuppressWarnings("unchecked")
    public Object[][] getOrderFieldValues() {
        OrderDTO order = getWrappedInstance();
        List<Object[]> values = new ArrayList<>();
        Integer languageId = order.getBaseUserByUserId().getLanguage().getId();
        List<OrderLineDTO> lineDTOs = order.getLines();
        Collections.sort(lineDTOs);
        OrderLineDTO orderLineDto = new OrderLineDTO();
        if (CollectionUtils.isNotEmpty(lineDTOs)) {
            orderLineDto = lineDTOs.get(0);
            lineDTOs.remove(0);
        }
        // main invoice row
        values.add(
                new Object[]{
                        orderId,
                        order.getBaseUserByUserId() != null ? order.getBaseUserByUserId().getId() : null,
                        order.getBaseUserByUserId() != null ? order.getBaseUserByUserId().getUserName() : null,
                        order.getOrderStatus() != null ? order.getOrderStatus().getDescription(languageId) : null,
                        order.getOrderPeriod() != null ? order.getOrderPeriod().getDescription(languageId) : null,
                        order.getOrderBillingType() != null ? order.getOrderBillingType().getDescription(languageId) : null,
                        order.getCurrency() != null ? order.getCurrency().getDescription(languageId) : null,
                        order.getTotal(),
                        order.getActiveSince(),
                        order.getActiveUntil(),
                        order.getCycleStarts(),
                        order.getCreateDate(),
                        getNextBillableDay(order),
                        order.getNotes(),
                        orderLineDto.getItem() == null ? null : orderLineDto.getItem().getId(),
                        orderLineDto.getItem() == null ? null : orderLineDto.getItem().getInternalNumber(),
                        orderLineDto.getQuantity(),
                        orderLineDto.getPrice(),
                        orderLineDto.getAmount(),
                        orderLineDto.getDescription()
                }
        );

        // indented row for each order line
        // padding for the main invoice columns
        // order line
        lineDTOs.stream()
                .filter(line -> line.getDeleted() == 0)
                .forEach(line -> values.add(
                        new Object[]{
                                // padding for the main invoice columns
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,

                                // order line
                                line.getItem() == null ? null : line.getItem().getId(),
                                line.getItem() == null ? null : line.getItem().getInternalNumber(),
                                line.getQuantity(),
                                line.getPrice(),
                                line.getAmount(),
                                line.getDescription()
                        }
                ));

        return values.toArray(new Object[values.size()][]);
    }


    private List<String> getLineAssets(Integer lineId) {
        List<String> assets = new ArrayList<>();
        for (Entry<Integer, Map<Integer, List<String>>> orderAssetIdentifierEntry : orderAndOrderLineAssetIdentifierMap.entrySet()) {
            for (Entry<Integer, List<String>> orderLineAssetIdentifierEntry : orderAssetIdentifierEntry.getValue().entrySet()) {
                if (lineId.equals(orderLineAssetIdentifierEntry.getKey())) {
                    assets.addAll(orderLineAssetIdentifierEntry.getValue());
                    break;
                }
            }
        }
        return assets;
    }

    private Object[] getOrderLineValues(Integer lineId) {

        Map<String, String> orderLineLevelMetaFieldMap = getOrderLineMetafieldNames();
        int orderLineMetaFieldsOffset = PURCHASE_ORDER_FIELDS.size() + orderLineFields.size();
        Object[] objects = new Object[orderLineMetaFieldsOffset + orderLineLevelMetaFieldMap.size()];

        // padding for the main order columns
        for (int index = 0; index < PURCHASE_ORDER_FIELDS.size(); index++) {
            objects[index] = null;
        }

        int orderLineFieldsOffset = PURCHASE_ORDER_FIELDS.size();
        OrderLineFieldsWrapper orderLineFieldsWrapper = lineDAS.getOrderLineValuesByLineId(lineId);
        objects[orderLineFieldsOffset++] = orderLineFieldsWrapper.getItemId();

        List<String> lineAssets = getLineAssets(lineId);
        objects[orderLineFieldsOffset++] = lineAssets.isEmpty() ? StringUtils.EMPTY : String.join(SEPARATOR, lineAssets);
        objects[orderLineFieldsOffset++] = orderLineFieldsWrapper.getQuantity();
        objects[orderLineFieldsOffset++] = orderLineFieldsWrapper.getPrice();
        objects[orderLineFieldsOffset++] = orderLineFieldsWrapper.getAmount();
        objects[orderLineFieldsOffset++] = orderLineFieldsWrapper.getDescription();
        objects[orderLineFieldsOffset] = orderLineFieldsWrapper.getCallCounter();

        Map<String, String> metaFieldMap = metaFieldDAS.getOrderLineMetaFieldValue(lineId);
        for (Entry<String, String> metaField : orderLineLevelMetaFieldMap.entrySet()) {
            orderLineLevelMetaFieldMap.put(metaField.getKey(), metaFieldMap.get(metaField.getKey()));
        }

        int i = orderLineMetaFieldsOffset;
        for (String identifier : orderLineLevelMetaFieldMap.values()) {
            objects[i++] = identifier;
        }

        return objects;
    }

    private static List<String> getPurchaseOrderFields() {

        List<String> fieldsList = new ArrayList<>();

        fieldsList.add("id");
        fieldsList.add("userId");
        fieldsList.add("userName");
        fieldsList.add("status");
        fieldsList.add("period");
        fieldsList.add("billingType");
        fieldsList.add("currency");
        fieldsList.add("total");
        fieldsList.add("activeSince");
        fieldsList.add("activeUntil");
        fieldsList.add("cycleStart");
        fieldsList.add("createdDate");
        fieldsList.add("nextBillableDay");
        fieldsList.add("Prorate Flag");
        fieldsList.add("Asset Identifiers");
        fieldsList.add("notes");

        return fieldsList;
    }

    private static List<String> getOrderLineFields() {
        List<String> fieldsList = new ArrayList<>();

        fieldsList.add("lineItemId");
        fieldsList.add("lineAssets");
        fieldsList.add("lineQuantity");
        fieldsList.add("linePrice");
        fieldsList.add("lineAmount");
        fieldsList.add("lineDescription");
        fieldsList.add("callCounter");

        return fieldsList;
    }

    private Map<String, String> getOrderLineMetafieldNames() {
        Map<String, String> orderLineLevelMetaFieldMap;
        Integer entityId = getWrappedInstance().getBaseUserByUserId().getCompany().getId();
        orderLineLevelMetaFieldMap = ORDER_LINE_META_FIELD_MAP.get(entityId);
        if (null == orderLineLevelMetaFieldMap) {
            orderLineLevelMetaFieldMap = new HashMap<>();
            List<MetaField> orderLineLevelMetaFields = new MetaFieldDAS().getAvailableFields(getWrappedInstance().getBaseUserByUserId().getCompany().getId(),
                    new EntityType[]{EntityType.ORDER_LINE}, true);

            for (MetaField metaField : orderLineLevelMetaFields) {
                orderLineLevelMetaFieldMap.put(metaField.getName(), null);
            }

            ORDER_LINE_META_FIELD_MAP.put(entityId, orderLineLevelMetaFieldMap);
        }
        return new HashMap<>(orderLineLevelMetaFieldMap);
    }

    private List<String> getAllOrderAssets() {
        List<String> assets = new ArrayList<>();
        for (Entry<Integer, Map<Integer, List<String>>> orderAssetIdentifierEntry : orderAndOrderLineAssetIdentifierMap.entrySet()) {
            for (Entry<Integer, List<String>> orderLineAssetIdentifierEntry : orderAssetIdentifierEntry.getValue().entrySet()) {
                assets.addAll(orderLineAssetIdentifierEntry.getValue());
            }
        }
        return assets;
    }


    @Override
    public void setDynamicExport(DynamicExport dynamicExport) {
        this.dynamicExport = dynamicExport;
    }

    private Date getNextBillableDay(OrderDTO order) {
        if (order.getNextBillableDay() != null) {
            if (!order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
                return order.getNextBillableDay();
            } else {
                return null;
            }
        }

        return order.getActiveSince();
    }
}
