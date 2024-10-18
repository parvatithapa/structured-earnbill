package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.db.OrderStatusBL;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderStatusDTO;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vivek on 18/11/14.
 */
public class OrderStatusCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderStatusCopyTask.class));
    public static final String ACTIVE = "Active";
    public static final String SUSPENDED = "Suspended";
    public static final String FINISHED = "Finished";
    public static final String SUSPENDED_AGEING_AUTO = "Suspended ageing(auto)";

    OrderStatusDAS orderStatusDAS = null;
    CompanyDAS companyDAS = null;

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<OrderStatusDTO> orderStatusDTOs = orderStatusDAS.findAll(targetEntityId);
        return orderStatusDTOs != null && !orderStatusDTOs.isEmpty();
    }

    public OrderStatusCopyTask() {
        init();
    }

    public void init() {
        orderStatusDAS = new OrderStatusDAS();
        companyDAS = new CompanyDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.

        LOG.debug("Create OrderStatusCopyTask");
        CompanyWS targetEntity = EntityBL.getCompanyWS(companyDAS.find(targetEntityId));
        List<OrderStatusDTO> orderStatusDTOs = orderStatusDAS.findAll(entityId);
        List<OrderStatusFlag> currentStatusFlags = new ArrayList<>();
        for (OrderStatusDTO orderStatusDTO : orderStatusDTOs) {
            OrderStatusWS orderStatusWS = OrderStatusBL.getOrderStatusWS(orderStatusDTO);
            orderStatusWS.setId(0);
            orderStatusWS.setEntity(targetEntity);
            currentStatusFlags.add(orderStatusDTO.getOrderStatusFlag());
            Integer orderStatusId = createUpdateOrderStatus(orderStatusWS, targetEntityId);
            CopyCompanyUtils.oldNewOrderStatusMap.put(orderStatusDTO.getId(), orderStatusId);
        }

        validateAndCreateDefaultOrderStatus(targetEntity, currentStatusFlags);

        LOG.debug("OrderStatusCopyTask has been completed");
    }

    /**
     * Validates if all default OrderStatus are configured and creates those are not
     *
     * @param entity the entity that owns the new order status
     * @param statusFlags current configured status
     */
    private void validateAndCreateDefaultOrderStatus(CompanyWS entity, List<OrderStatusFlag> statusFlags) {
        if (!statusFlags.contains(OrderStatusFlag.INVOICE)) {
            createOrderStatus(OrderStatusFlag.INVOICE, ACTIVE, entity);
        }
        if (!statusFlags.contains(OrderStatusFlag.NOT_INVOICE)) {
            createOrderStatus(OrderStatusFlag.NOT_INVOICE, SUSPENDED, entity);
        }
        if (!statusFlags.contains(OrderStatusFlag.FINISHED)) {
            createOrderStatus(OrderStatusFlag.FINISHED, FINISHED, entity);
        }
        if(!statusFlags.contains(OrderStatusFlag.SUSPENDED_AGEING)) {
            createOrderStatus(OrderStatusFlag.SUSPENDED_AGEING, SUSPENDED_AGEING_AUTO, entity);
        }
    }

    /**
     * Creates an order status from params
     *
     * @param statusFlag type of order status
     * @param description order status description
     * @param entity the entity that owns the new order status
     */
    private void createOrderStatus(OrderStatusFlag statusFlag, String description, CompanyWS entity) {
        OrderStatusWS invoiceStatus = new OrderStatusWS(0, entity, statusFlag, description);
        createUpdateOrderStatus(invoiceStatus, entity.getId());
    }

    public Integer createUpdateOrderStatus(OrderStatusWS orderStatusWS, Integer targetEntityId)
            throws SessionInternalError {
        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
        OrderStatusBL orderStatusBL = new OrderStatusBL();
        for (InternationalDescriptionWS desc : orderStatusWS.getDescriptions()) {
            try {
                if (!orderStatusBL.isOrderStatusValid(orderStatusWS, targetEntityId, desc.getContent())) {
                    throw new SessionInternalError("Order status exist ", new String[]{"OrderStatusWS,status,validation.error.status.already.exists"});
                }
            } catch (Exception e) {
                throw new SessionInternalError(e);
            }
        }
        Integer orderId = createOrderStatus(orderStatusWS, targetEntityId, targetEntity.getLanguageId());
        return orderId;
    }

    public Integer createOrderStatus(OrderStatusWS orderStatusWS, Integer entityId, Integer languageId) throws SessionInternalError
    {
        OrderStatusDTO newOrderStatus = OrderStatusBL.getDTOWithTargetEntity(orderStatusWS);
        newOrderStatus = new OrderStatusDAS().createOrderStatus(newOrderStatus);
        newOrderStatus.setDescription(orderStatusWS.getDescription(), languageId);
        return newOrderStatus.getId();
    }
}
