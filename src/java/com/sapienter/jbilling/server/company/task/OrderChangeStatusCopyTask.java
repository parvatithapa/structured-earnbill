package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.order.OrderChangeStatusBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by vivek on 18/11/14.
 */
public class OrderChangeStatusCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderChangeStatusCopyTask.class));
    OrderChangeStatusDAS orderChangeStatusDAS = null;
    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        orderChangeStatusDAS = new OrderChangeStatusDAS();
        List<OrderChangeStatusDTO> orderChangeStatusDTOs = orderChangeStatusDAS.findAllOrderChangeStatuses(targetEntityId);
        return orderChangeStatusDTOs != null && !orderChangeStatusDTOs.isEmpty();
    }

    public OrderChangeStatusCopyTask() {
        init();
    }

    private void init() {
        orderChangeStatusDAS = new OrderChangeStatusDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        CompanyDTO targetEntity = new CompanyDAS().find(targetEntityId);
        LOG.debug("Create OrderChangeStatusCopyTask");
        List<OrderChangeStatusDTO> orderChangeStatusDTOs = orderChangeStatusDAS.findAllOrderChangeStatuses(entityId);
        for (OrderChangeStatusDTO orderChangeStatusDTO : orderChangeStatusDTOs) {
            OrderChangeStatusWS orderChangeStatusWS = OrderChangeStatusBL.getWS(orderChangeStatusDTO);
            orderChangeStatusWS.setId(null);
            orderChangeStatusWS.setEntityId(targetEntityId);
            OrderChangeStatusDTO copyOrderChangeStatusDTO = OrderChangeStatusBL.getDTO(orderChangeStatusWS);
            copyOrderChangeStatusDTO.setCompany(targetEntity);
            copyOrderChangeStatusDTO = OrderChangeStatusBL.createOrderChangeStatus(copyOrderChangeStatusDTO, targetEntityId);

            copyOrderChangeStatusDTO.setDescription(orderChangeStatusDTO.getDescription(targetEntity.getLanguageId()), targetEntity.getLanguageId());
            copyOrderChangeStatusDTO = orderChangeStatusDAS.save(copyOrderChangeStatusDTO);
            CopyCompanyUtils.oldNewOrderChangeStatusMap.put(orderChangeStatusDTO.getId(), copyOrderChangeStatusDTO.getId());
        }
        LOG.debug("OrderChangeStatusCopyTask has been completed.");
    }
}