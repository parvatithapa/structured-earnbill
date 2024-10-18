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

package com.sapienter.jbilling.server.order.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapienter.jbilling.server.item.ItemDependencyType;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemDependencyDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * @author Alexander Aksenov
 * @since 18.06.13
 */
public class OrderHierarchyValidator {

    public final static String ERR_HIERARCHY_TOO_BIG = "OrderWS,hierarchy,error.order.hierarchy.too.big";
    public final static int MAX_ORDERS_COUNT = 100;
    public final static int MAX_LEVELS = 3;
    public final static String COMMA = ",";
    public final static String ERR_CYCLES_IN_HIERARCHY = "OrderWS,hierarchy,error.order.hierarchy.contains.cycles";
    public final static String ERR_INCORRECT_ORDERS_PARENT_CHILD_LINK = "OrderWS,hierarchy,error.order.hierarchy.incorrect.order.parent.child.link";
    public final static String ERR_INCORRECT_LINES_PARENT_CHILD_LINK = "OrderWS,hierarchy,error.order.hierarchy.incorrect.line.parent.child.link";
    public final static String ERR_INCORRECT_PARENT_CHILD_RELATIONSHIP = "OrderWS,hierarchy,error.order.hierarchy.incorrect.parent.child.relationship";
    public final static String ERR_INCORRECT_ACTIVE_SINCE = "OrderWS,hierarchy,error.order.hierarchy.incorrect.active.since";
    public final static String ERR_INCORRECT_ACTIVE_UNTIL = "OrderWS,hierarchy,error.order.hierarchy.incorrect.active.until";
    public final static String ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET = "OrderWS,hierarchy,error.order.hierarchy.product.mandatory.dependency.not.meet";
    public final static String ERR_NON_LEAF_ORDER_DELETE = "OrderWS,hierarchy,error.order.hierarchy.non.leaf.order.delete";
    public final static String ERR_UNLINKED_HIERARCHY = "OrderWS,hierarchy,error.order.hierarchy.unlinked";
    public final static String PRODUCT_PARENT_DEPENDENCY_EXIST = "OrderWS,hierarchy,error.order.hierarchy.cannot.delete.order.delete.parent.first";
    public final static String MANDATORY_PRODUCT_ERROR_MESSAGE = "&nbsp;&nbsp;&nbsp;&nbsp;For product %s you need to add the following dependencies:";
    public final static String PRODUCT_DEPENDENCIES_ERROR_MESSAGE = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Product %d from Category %s";
    public final static String MANDATORY_MAXIMUM_DEPENDENCY_EXCEEDED = "&nbsp;&nbsp;&nbsp;&nbsp;For product %s maximum number (%s) of the dependent product %s is exceeded";

    private Map<Key<OrderDTO>, OrderHierarchyDto> ordersMap = new HashMap<>();
    private Map<Key<OrderLineDTO>, OrderLineHierarchyDto> orderLinesMap = new HashMap<>();

    class OrderHierarchyDto {
        protected Integer orderId;
        protected OrderHierarchyDto parentOrder;
        protected Set<OrderHierarchyDto> childOrders = new HashSet<>();
        protected Date activeSince;
        protected Date activeUntil;
        protected Set<OrderLineHierarchyDto> orderLines = new HashSet<>();

        protected int visited = -1;
        protected boolean updated;
        protected OrderStatusFlag orderStatusFlag;
    }

    class OrderLineHierarchyDto {
        protected OrderHierarchyDto order;
        protected Integer orderLineId;
        protected OrderLineHierarchyDto parentOrderLine;
        protected Set<OrderLineHierarchyDto> childOrderLines = new HashSet<>();
        protected Integer productId;
        protected Integer quantity;

        //list contains 2 integers, the minimum and maximum quantities
        protected Map<Integer, List<Integer>> dependentProducts = new HashMap<>();

        protected int visited = -1;
    }

    static class Key<T> {
        protected int id;

        Key(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            if (id != key.id) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public void buildHierarchy(OrderDTO order) {
        addOrderToHierarchy(order);
    }

    public String validate() {
        return validate(null);
    }

    public String validate(Integer entityId) {
        resetVisited();
        String error = validateCycles();
        if (error != null) {
            return error;
        }
        error = validateHierarchySize();
        if (error != null) {
            return error;
        }
        error = validateParentChildLinks();
        if (error != null) {
            return error;
        }
        error = validateIsAllEntitiesLinked();
        if (error != null) {
            return error;
        }
        error = validateActiveSince();
        if (error != null) {
            return error;
        }
        error = validateActiveUntil();
        if (error != null) {
            return error;
        }
        error = validateProductDependencies(entityId);
        return error;
    }

    public String deleteOrder(Integer id) {
        if (id == null) {
            return null;
        }
        OrderHierarchyDto targetOrder = findOrder(id);
        if (targetOrder != null) {
            if (hasChildDependency(targetOrder)) {
                return PRODUCT_PARENT_DEPENDENCY_EXIST;
            }

            for (OrderLineHierarchyDto lineDto : targetOrder.orderLines) {
                if (lineDto.parentOrderLine != null) {
                    lineDto.parentOrderLine.childOrderLines.remove(lineDto);
                }
                for (OrderLineHierarchyDto childLineDto : lineDto.childOrderLines) {
                    childLineDto.parentOrderLine = null;
                }
                for (Key<OrderLineDTO> key : new HashSet<>(orderLinesMap.keySet())) {
                    if (orderLinesMap.get(key).equals(lineDto)) {
                        orderLinesMap.remove(key);
                        break;
                    }
                }
            }
            if (targetOrder.parentOrder != null) {
                targetOrder.parentOrder.childOrders.remove(targetOrder);
            }
            for (Key<OrderDTO> key : new HashSet<>(ordersMap.keySet())) {
                if (ordersMap.get(key).equals(targetOrder)) {
                    ordersMap.remove(key);
                    break;
                }
            }
        }
        return null;
    }

    public boolean hasChildDependency(OrderHierarchyDto targetOrder) {
        if (targetOrder.parentOrder == null) {
            return false;
        }

        OrderHierarchyDto parentOrder = targetOrder.parentOrder;

        Set<Integer> dependentProducts = new HashSet<>();
        for (OrderLineHierarchyDto orderLineHierarchyDto : parentOrder.orderLines) {
            dependentProducts.addAll(orderLineHierarchyDto.dependentProducts.keySet());
        }

        if (dependentProducts.isEmpty()) {
            return false;
        }

        for (OrderLineHierarchyDto orderLineHierarchyDto : parentOrder.orderLines) {
            if (dependentProducts.contains(orderLineHierarchyDto.productId)) {
                return false;
            }
        }

        for (OrderHierarchyDto orderHierarchyDto : parentOrder.childOrders) {

            if (orderHierarchyDto.orderId == targetOrder.orderId) {
                continue;
            }

            for (OrderLineHierarchyDto orderLineHierarchyDto : orderHierarchyDto.orderLines) {
                if (dependentProducts.contains(orderLineHierarchyDto.productId)) {
                    return false;
                }
            }
        }

        return true;
    }

    public int subOrderCount(Integer orderId) {
        OrderHierarchyDto targetOrder = findOrder(orderId);
        int subOrderCount = 0;
        if (targetOrder.parentOrder == null) {
            subOrderCount += targetOrder.childOrders.size();
            for (OrderHierarchyDto childOrder : targetOrder.childOrders) {
                subOrderCount += subOrderCount(childOrder.orderId);
            }
        }
        return subOrderCount;
    }

    public void updateOrdersInfo(Collection<OrderDTO> updatedOrders) {
        // prepare with new objects
        for (OrderDTO updatedOrder : updatedOrders) {
            OrderHierarchyDto orderHierarchyDto = findOrder(updatedOrder);
            if (orderHierarchyDto == null) {
                addOrderToHierarchy(updatedOrder);
                orderHierarchyDto = findOrder(updatedOrder);
            }
            orderHierarchyDto.updated = true;
            if (updatedOrder.getLines() != null) {
                for (OrderLineDTO line : updatedOrder.getLines()) {
                    if (findLine(line) == null) {
                        addLineToHierarchy(line);
                    }
                }
            }
        }
        for (OrderDTO updatedOrder : updatedOrders) {
            updateOrderInfo(updatedOrder);
        }
    }

    private void updateOrderInfo(OrderDTO updatedOrder) {
        OrderHierarchyDto orderHierarchyDto = findOrder(updatedOrder);
        if (orderHierarchyDto != null) {
            orderHierarchyDto.activeSince = updatedOrder.getActiveSince();
            orderHierarchyDto.activeUntil = updatedOrder.getActiveUntil();
            updateParentOrderInfo(orderHierarchyDto, updatedOrder);

            List<OrderLineDTO> newLines = new LinkedList<>(updatedOrder.getLines());
            // update existed lines, remove not needed lines in old hierarchy
            for (Iterator<OrderLineHierarchyDto> iter = orderHierarchyDto.orderLines.iterator(); iter.hasNext(); ) {
                OrderLineHierarchyDto line = iter.next();
                boolean found = false;
                for (Iterator<OrderLineDTO> newLinesIterator = newLines.iterator(); newLinesIterator.hasNext(); ) {
                    OrderLineDTO newLine = newLinesIterator.next();
                    if (newLine.getDeleted() > 0) {
                        OrderLineHierarchyDto targetLine = findLine(newLine);
                        if (targetLine != null) {
                            for (OrderLineHierarchyDto childLine : targetLine.childOrderLines) {
                                if (childLine.parentOrderLine.equals(targetLine)) {
                                    childLine.parentOrderLine = null;
                                }
                            }
                            if (targetLine.parentOrderLine != null) {
                                targetLine.parentOrderLine.childOrderLines.remove(targetLine);
                            }
                            orderLinesMap.remove(key(newLine));
                        }
                    } else if (findLine(newLine).equals(line)) {
                        // update line
                        line.productId = newLine.getItemId();
                        line.quantity = newLine.getQuantityInt();
                        line.dependentProducts.clear();
                        if (newLine.getItem() != null) {
                            for (ItemDependencyDTO dependency : newLine.getItem().getDependencies()) {
                                if (dependency.getMinimum() > 0) {
                                    line.dependentProducts.put(dependency.getDependentObjectId(), Arrays.asList(dependency.getMinimum(), dependency.getMaximum() != null ? dependency.getMaximum() : new Integer(-1)));
                                }
                            }
                        }
                        updateParentOrderLineInfo(line, newLine);
                        newLinesIterator.remove();
                        found = true;
                        break;
                    }
                }
                if (!found) { // remove line from old hierarchy, new one does not exists
                    if (line.parentOrderLine != null) {
                        line.parentOrderLine.childOrderLines.remove(line);
                    }
                    iter.remove();
                }
            }
            // add new lines to order
            for (OrderLineDTO newLine : newLines) {
                orderHierarchyDto.orderLines.add(findLine(newLine));
            }
        }
    }

    private void updateParentOrderLineInfo(OrderLineHierarchyDto line, OrderLineDTO newLine) {
        OrderLineHierarchyDto newParent = newLine.getParentLine() != null ? findLine(newLine.getParentLine()) : null;
        if (newParent != null) { // newParent can be removed from order hierarchy
            if (line.parentOrderLine != null) {
                if (!newParent.equals(line.parentOrderLine)) {
                    line.parentOrderLine.childOrderLines.remove(line);
                    line.parentOrderLine = newParent;
                    newParent.childOrderLines.add(line);
                }
            } else {
                line.parentOrderLine = newParent;
                newParent.childOrderLines.add(line);
            }
        } else if (line.parentOrderLine != null) {
            // reset link to parent only if previous parent orde was presented in hierarchy for update
            if (line.parentOrderLine.order.updated) {
                line.parentOrderLine.childOrderLines.remove(line);
                line.parentOrderLine = null;
            }
        }
    }

    private void updateParentOrderInfo(OrderHierarchyDto orderHierarchyDto, OrderDTO updatedOrder) {
        if (updatedOrder.getParentOrder() != null) {
            OrderHierarchyDto newParent = findOrder(updatedOrder.getParentOrder());
            if (orderHierarchyDto.parentOrder != null) {
                if (!newParent.equals(orderHierarchyDto.parentOrder)) {
                    orderHierarchyDto.parentOrder.childOrders.remove(orderHierarchyDto);
                    orderHierarchyDto.parentOrder = newParent;
                    newParent.childOrders.add(orderHierarchyDto);
                }
            } else {
                orderHierarchyDto.parentOrder = newParent;
                newParent.childOrders.add(orderHierarchyDto);
            }
        } else if (orderHierarchyDto.parentOrder != null) {
            // reset link to parent only if previous parent was presented in hierarchy for update
            if (orderHierarchyDto.parentOrder.updated) {
                orderHierarchyDto.parentOrder.childOrders.remove(orderHierarchyDto);
                orderHierarchyDto.parentOrder = null;
            }
        }
    }

    private String validateProductDependencies(Integer entityId) {
        StringBuilder dependencyErrors = new StringBuilder();
        ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
        ItemDAS itemDAS = new ItemDAS();
        ItemDTO item;

        if (entityId == null) {
            IWebServicesSessionBean api = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
            entityId = api.getCallerCompanyId();
        }

        for (OrderLineHierarchyDto lineHierarchyDto : allLines()) {
            boolean title = true;
            for (Integer mandatoryProductId : lineHierarchyDto.dependentProducts.keySet()) {
                Integer min = lineHierarchyDto.dependentProducts.get(mandatoryProductId).get(0);
                Integer max = lineHierarchyDto.dependentProducts.get(mandatoryProductId).get(1);

                int qtyFound = 0;
                for (OrderLineHierarchyDto childLine : lineHierarchyDto.childOrderLines) {
                    ItemTypeDTO category = itemTypeDAS.getById(mandatoryProductId, entityId, null);
                    if (null != category && category.getId() > 0 && null != category.getItems() && !category.getItems().isEmpty()) {
                        for (ItemDTO itemdto : category.getEntity().getItems()) {
                            if (itemdto.getId() == childLine.productId) {
                                qtyFound += childLine.quantity;
                                break;
                            }
                        }
                    } else {
                        if (mandatoryProductId.equals(childLine.productId)) {
                            qtyFound += childLine.quantity;
                            break;
                        }
                    }
                }

                if (lineHierarchyDto.order.childOrders != null && !lineHierarchyDto.order.childOrders.isEmpty() &&
                        lineHierarchyDto.childOrderLines.isEmpty()) {
                    for (OrderHierarchyDto orderHierarchyValidator : lineHierarchyDto.order.childOrders) {
                        for (OrderLineHierarchyDto childLine : orderHierarchyValidator.orderLines) {
                            ItemTypeDTO category = itemTypeDAS.getById(mandatoryProductId, entityId, null);
                            if (null != category && category.getId() > 0 && null != category.getItems() && !category.getItems().isEmpty()) {
                                for (ItemDTO itemdto : category.getEntity().getItems()) {
                                    if (itemdto.getId() == childLine.productId) {
                                        qtyFound += childLine.quantity;
                                        break;
                                    }
                                }
                            } else {
                                if (mandatoryProductId.equals(childLine.productId)) {
                                    qtyFound += childLine.quantity;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (qtyFound < min) {
                    if (title) {
                        dependencyErrors.append(COMMA);
                        dependencyErrors.append(String.format(MANDATORY_PRODUCT_ERROR_MESSAGE, lineHierarchyDto.productId));
                        title = false;
                    }

                    item = itemDAS.findNow(mandatoryProductId);
                    if (item != null && !item.getItemTypes().isEmpty()) {
                        for (ItemTypeDTO itemType : item.getItemTypes()) {
                            dependencyErrors.append(COMMA);
                            dependencyErrors.append(String.format(PRODUCT_DEPENDENCIES_ERROR_MESSAGE, mandatoryProductId, itemType.getDescription()));
                            break;
                        }
                    } else {
                        dependencyErrors.append(String.format(PRODUCT_DEPENDENCIES_ERROR_MESSAGE, mandatoryProductId, ""));
                    }
                } else if (max > 0 && qtyFound > max) {
                    dependencyErrors.append(COMMA);
                    dependencyErrors.append(String.format(MANDATORY_MAXIMUM_DEPENDENCY_EXCEEDED, lineHierarchyDto.productId, max, mandatoryProductId));
                }
            }
        }

        if (dependencyErrors.length() != 0) {
            dependencyErrors.insert(0, ERR_PRODUCT_MANDATORY_DEPENDENCY_NOT_MEET);
            return dependencyErrors.toString();
        }
        return null;
    }

    private String validateActiveSince() {
        for (OrderHierarchyDto order : allOrders()) {
            if (order.parentOrder != null && !order.orderStatusFlag.equals(OrderStatusFlag.FINISHED)) {
                if (order.activeSince != null && order.parentOrder.activeSince != null &&
                        order.activeSince.before(order.parentOrder.activeSince)) {
                    return ERR_INCORRECT_ACTIVE_SINCE;
                }
            }
        }
        return null;
    }

    private String validateActiveUntil() {
        for (OrderHierarchyDto order : allOrders()) {
            if (order.parentOrder != null && !order.orderStatusFlag.equals(OrderStatusFlag.FINISHED)) {
                if (order.activeUntil != null && order.parentOrder.activeUntil != null &&
                        order.activeUntil.after(order.parentOrder.activeUntil)) {
                    return ERR_INCORRECT_ACTIVE_UNTIL;
                }
            }
        }
        return null;
    }

    private String validateParentChildLinks() {
        //validate parent-child filling for orders
        for (OrderHierarchyDto order : allOrders()) {
            if (order.parentOrder != null && !order.parentOrder.childOrders.contains(order)) {
                return ERR_INCORRECT_ORDERS_PARENT_CHILD_LINK;
            }
        }
        //validate parent-child filling for lines
        for (OrderLineHierarchyDto orderLine : allLines()) {
            if (orderLine.parentOrderLine != null && !orderLine.parentOrderLine.childOrderLines.contains(orderLine)) {
                return ERR_INCORRECT_LINES_PARENT_CHILD_LINK;
            }
        }
        return null;
    }

    private String validateIsAllEntitiesLinked() {
        int step = -10;
        if (!allOrders().isEmpty()) {
            visitOrder(allOrders().iterator().next(), step);
        }
        for (OrderHierarchyDto orderHierarchyDto : allOrders()) {
            if (orderHierarchyDto.visited != step) {
                return ERR_UNLINKED_HIERARCHY;
            }
        }
        return null;
    }

    private void visitOrder(OrderHierarchyDto order, int step) {
        if (order.visited != step) {
            order.visited = step;
            if (order.parentOrder != null) {
                visitOrder(order.parentOrder, step);
            }
            for (OrderHierarchyDto childOrder : order.childOrders) {
                visitOrder(childOrder, step);
            }
        }
    }

    private String validateCycles() {
        // put step to visited field and check it later, is this node already visited at current step
        // using step number instead of flag (true/false) prevents flag reset in all collection
        int step = 1;
        for (OrderHierarchyDto orderDto : allOrders()) {
            OrderHierarchyDto parentDto = orderDto;
            parentDto.visited = step;
            while ((parentDto = parentDto.parentOrder) != null) {
                if (parentDto.visited == step) {
                    return ERR_CYCLES_IN_HIERARCHY;
                }
                parentDto.visited = step;
            }
            step++;
        }
        step = 1;
        for (OrderLineHierarchyDto orderLineDto : allLines()) {
            OrderLineHierarchyDto parentDto = orderLineDto;
            parentDto.visited = step;
            while ((parentDto = parentDto.parentOrderLine) != null) {
                if (parentDto.visited == step) {
                    return ERR_CYCLES_IN_HIERARCHY;
                }
                parentDto.visited = step;
            }
            step++;
        }
        // validate, that parent line - in parent or same order, not in child order
        for (OrderLineHierarchyDto orderLineDto : allLines()) {
            if (orderLineDto.parentOrderLine != null) {
                // check in current order first
                boolean found = orderLineDto.order.orderLines.contains(orderLineDto.parentOrderLine);
                OrderHierarchyDto parentOder = orderLineDto.order;
                while (!found && parentOder != null) {
                    if (parentOder.orderLines.contains(orderLineDto.parentOrderLine)) {
                        found = true;
                    }
                    parentOder = parentOder.parentOrder;
                }

                // Also validating on the order line Hierarchy because there are cases where orderline's(discout order ) parentOrderLine does not belong to the parent order of the discount order.
                OrderLineHierarchyDto parentOderLine = orderLineDto.parentOrderLine;
                while (!found && parentOderLine != null) {
                    OrderHierarchyDto parentOderLineOrder = parentOderLine.order;
                    if (parentOderLineOrder.orderLines.contains(orderLineDto.parentOrderLine)) {
                        found = true;
                    }
                    parentOderLine = parentOderLine.parentOrderLine;
                }

                if (!found) {
                    return ERR_INCORRECT_PARENT_CHILD_RELATIONSHIP;
                }
            }
        }

        return null;
    }

    private String validateHierarchySize() {
        if (allOrders().size() > MAX_ORDERS_COUNT) {
            return ERR_HIERARCHY_TOO_BIG;
        }
        for (OrderHierarchyDto orderDto : allOrders()) {
            int deep = 1;
            OrderHierarchyDto parentDto = orderDto;
            while ((parentDto = parentDto.parentOrder) != null) {
                deep++;
                if (deep > MAX_LEVELS) {
                    return ERR_HIERARCHY_TOO_BIG;
                }
            }
        }
        return null;
    }

    private void resetVisited() {
        for (OrderHierarchyDto dto : allOrders()) {
            dto.visited = -1;
        }
    }


    private OrderHierarchyDto addOrderToHierarchy(OrderDTO order) {
        if (order == null || order.getDeleted() > 0) {
            return null;
        }
        OrderHierarchyDto result = ordersMap.get(key(order));
        if (result != null) {
            return result;
        }
        result = new OrderHierarchyDto();
        // put to map for references from childs
        ordersMap.put(key(order), result);
        result.orderId = order.getId();
        result.activeSince = order.getActiveSince();
        result.activeUntil = order.getActiveUntil();
        result.parentOrder = addOrderToHierarchy(order.getParentOrder());
        result.orderStatusFlag = order.getOrderStatus().getOrderStatusFlag();
        if (result.parentOrder != null) {
            result.parentOrder.childOrders.add(result); // for updated orders
        }
        if (order.getChildOrders() != null) {
            for (OrderDTO childOrder : order.getChildOrders()) {
                OrderHierarchyDto childDTO = addOrderToHierarchy(childOrder);
                if (childDTO != null) { // child order can be deleted
                    result.childOrders.add(childDTO);
                }
            }
        }
        if (order.getLines() != null) {
            for (OrderLineDTO line : order.getLines()) {
                OrderLineHierarchyDto lineDto = addLineToHierarchy(line);
                if (lineDto != null) {
                    result.orderLines.add(lineDto);
                }
            }
        }
        return result;
    }

    private OrderLineHierarchyDto addLineToHierarchy(OrderLineDTO lineDTO) {
        if (lineDTO == null || lineDTO.getDeleted() > 0 || lineDTO.getPurchaseOrder().getDeleted() > 0) {
            return null;
        }
        OrderLineHierarchyDto result = orderLinesMap.get(key(lineDTO));
        if (result != null) {
            return result;
        }
        result = new OrderLineHierarchyDto();
        orderLinesMap.put(key(lineDTO), result);
        result.order = addOrderToHierarchy(lineDTO.getPurchaseOrder());
        result.orderLineId = lineDTO.getId();
        result.productId = lineDTO.getItemId();
        result.quantity = lineDTO.getQuantityInt();
        result.parentOrderLine = addLineToHierarchy(lineDTO.getParentLine());

        if (lineDTO.getItem() != null) {
            List<ItemDTO> productList = new ArrayList<>();
            ItemDTO itemDTO = lineDTO.getItem();
            if (itemDTO.isPlan()) {
                PlanDTO planForProduct = itemDTO.getPlans().iterator().next();
                for (PlanItemDTO planItemDTO : planForProduct.getPlanItems()) {
                    productList.add(planItemDTO.getItem());
                }
            } else {
                productList.add(itemDTO);
            }

            ItemDAS itemDAS = new ItemDAS();
            for (ItemDTO product : productList) {
                Collection<ItemDependencyDTO> productsDependencies = product.getDependenciesOfType(ItemDependencyType.ITEM);

                //add the product only:
                for (ItemDependencyDTO dependency : productsDependencies) {

                    if (dependency.getMinimum() >= 0) {
                        result.dependentProducts.put(dependency.getDependentObjectId(), Arrays.asList(dependency.getMinimum(), dependency.getMaximum() != null ? dependency.getMaximum() : new Integer(-1)));
                    }
                }

                //add the products of category, with min and max of category values:
                Collection<ItemDependencyDTO> categoriesDependencies = product.getDependenciesOfType(ItemDependencyType.ITEM_TYPE);
                for (ItemDependencyDTO dependencyCategory : categoriesDependencies) {
                    if (dependencyCategory.getMinimum() >= 0) {
                        //get the products of category
                        for (ItemDTO itemInCategory : itemDAS.findAllByItemType(dependencyCategory.getDependentObjectId())) {
                            if (!result.dependentProducts.containsKey(itemInCategory.getId())) {
                                result.dependentProducts.put(itemInCategory.getId(), Arrays.asList(dependencyCategory.getMinimum(), dependencyCategory.getMaximum() != null ? dependencyCategory.getMaximum() : new Integer(-1)));
                            }
                        }
                    }
                }
            }
        }
        result.parentOrderLine = addLineToHierarchy(lineDTO.getParentLine());
        if (result.parentOrderLine != null) {
            result.parentOrderLine.childOrderLines.add(result);
        }
        if (lineDTO.getChildLines() != null) {
            for (OrderLineDTO childLine : lineDTO.getChildLines()) {
                OrderLineHierarchyDto childDto = addLineToHierarchy(childLine);
                if (childDto != null) {
                    result.childOrderLines.add(childDto);
                }
            }
        }
        return result;
    }

    private OrderHierarchyDto findOrder(Integer id) {
        return ordersMap.get(key(id));
    }

    private OrderHierarchyDto findOrder(OrderDTO order) {
        return ordersMap.get(key(order));
    }

    private OrderLineHierarchyDto findLine(Integer id) {
        return orderLinesMap.get(lineKey(id));
    }

    private OrderLineHierarchyDto findLine(OrderLineDTO line) {
        return orderLinesMap.get(key(line));
    }

    private Collection<OrderHierarchyDto> allOrders() {
        return ordersMap.values();
    }

    private Collection<OrderLineHierarchyDto> allLines() {
        return orderLinesMap.values();
    }

    // change logic if exists orders with same hash, but not equals. Storing order needed for additional equality check
    private static Key<OrderDTO> key(OrderDTO order) {
        return new Key<>(order.getId() != null ? order.getId() : order.hashCode());
    }

    // change logic if exists orderLines with same hash, but not equals. Storing orderLine needed for additional equality check
    private static Key<OrderLineDTO> key(OrderLineDTO orderLine) {
        return new Key<>(orderLine.getId() > 0 ? orderLine.getId() : orderLine.hashCode());
    }

    private static Key<OrderDTO> key(int id) {
        return new Key<>(id);
    }

    private static Key<OrderLineDTO> lineKey(int id) {
        return new Key<>(id);
    }

}
