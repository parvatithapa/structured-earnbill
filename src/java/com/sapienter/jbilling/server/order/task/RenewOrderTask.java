package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.order.IOrderSessionBean;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.EditOrderEvent;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sapienter.jbilling.server.util.time.PeriodUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by javierrivero on 4/3/18.
 */
public class RenewOrderTask extends AbstractCronTask {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public String getTaskName() {
        return "renew order task , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("Starting renew task");
        IOrderSessionBean orderSessionBean = Context.getBean(Context.Name.ORDER_SESSION);
        Integer categoryId = MetaFieldBL.getMetaFieldIntegerValueNullSafety(new EntityBL(getEntityId()).getEntity().getMetaField(Constants.SETUP_META_FIELD));
        OrderStatusDAS statusDAS = new OrderStatusDAS();

        OrderDAS orderDAS = new OrderDAS();
        List<OrderDTO> orderDTOs = orderDAS.getOrdersByRenewAndActiveUntil(Util.truncateDate(new Date()));

        Map<OrderDTO, Integer> renewedOrders = new HashMap<>(); 
        ChronoUnit chronoUnit;
        PeriodUnit period;
        for (OrderDTO order : orderDTOs) {
            period = order.valueOfPeriodUnit();
            if (order.getOrderPeriod().getPeriodUnit().getId() == PeriodUnitDTO.MONTH) {
                chronoUnit = ChronoUnit.MONTHS;
            } else if (order.getOrderPeriod().getPeriodUnit().getId() == PeriodUnitDTO.WEEK) {
                chronoUnit = ChronoUnit.WEEKS;
            } else if (order.getOrderPeriod().getPeriodUnit().getId() == PeriodUnitDTO.YEAR) {
                chronoUnit = ChronoUnit.YEARS;
            } else {
                chronoUnit = ChronoUnit.DAYS;
            }

            OrderDTO clonedOrder = new OrderDTO(order);
            clonedOrder.setId(null);
            clonedOrder.setVersionNum(null);
            clonedOrder.getOrderProcesses().clear();
            clonedOrder.getDiscountLines().clear();
            clonedOrder.setNextBillableDay(null);
            clonedOrder.getLines().clear();
            clonedOrder.setParentUpgradeOrderId(order.getId());
            clonedOrder.setActiveSince(order.getActiveUntil());
            clonedOrder.setCreateDate(new Date());
            clonedOrder.getChildOrders().clear();
            clonedOrder.setActiveUntil(DateConvertUtils.asUtilDate(period.addTo(DateConvertUtils.asLocalDate(clonedOrder.getActiveSince()),
                    chronoUnit.between(DateConvertUtils.asLocalDate(order.getActiveSince()),
                            DateConvertUtils.asLocalDate(order.getActiveUntil())))));

            Integer newOrderId = orderSessionBean.createUpdate(getEntityId(),
                                                               clonedOrder.getUserId(),
                                                               clonedOrder.getBaseUserByUserId().getLanguage().getId(),
                                                               clonedOrder,
                                                               order.getLines()
                                                                    .stream()
                                                                    .filter(line -> line.getItem()
                                                                                        .getItemTypes()
                                                                                        .stream()
                                                                                        .noneMatch(itemType -> categoryId!= null && itemType.getId() == categoryId))
                                                                    .flatMap(line -> line.getOrderChanges().stream())
                                                                    .peek(orderChange -> orderChange.setId(null))
                                                                    .peek(orderChange -> orderChange.setOrderLine(null))
                                                                    .peek(orderChange -> orderChange.setOrder(clonedOrder))
                                                                    .collect(Collectors.toList()),
                                                               Collections.emptyList());

            OrderDTO newOrder = orderSessionBean.getOrder(newOrderId);
            newOrder.getChildOrders().forEach(childOrder -> {
                childOrder.setLines(childOrder.getLines()
                                              .stream()
                                              .filter(line -> new ItemDAS().find(line.getItemId())
                                                                           .getItemTypes()
                                                                           .stream()
                                                                           .noneMatch(itemType -> categoryId != null && itemType.getId() == categoryId))
                                              .collect(Collectors.toList()));
                if (childOrder.getLines().isEmpty()) {
                    newOrder.getChildOrders().remove(childOrder);
                    orderSessionBean.delete(childOrder.getId(), null);
                }
            });

            orderSessionBean.setStatus(order.getId(),
                    statusDAS.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, getEntityId()),
                    order.getUserId(),
                    order.getBaseUserByUserId().getLanguage().getId());

            orderDAS.detach(order);
            renewedOrders.put(order, newOrderId);
        }
        
        renewedOrders.forEach( (order, renewedOrderId) -> {
            OrderDTO originalOrder = orderDAS.findNow(order.getId());
            originalOrder.setRenewOrderId(renewedOrderId);
            orderSessionBean.save(originalOrder);
            EventManager.process(new EditOrderEvent(getEntityId(), originalOrder));
        });

        logger.info("Renew Task finish");
    }
}
