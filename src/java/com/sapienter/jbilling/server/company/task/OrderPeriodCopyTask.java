package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by vivek on 31/10/14.
 */
public class OrderPeriodCopyTask extends AbstractCopyTask {
    OrderPeriodDAS periodDas = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderPeriodCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        List<OrderPeriodDTO> orderPeriodDTO = new OrderPeriodDAS().getOrderPeriods(targetEntityId);
        return orderPeriodDTO != null && !orderPeriodDTO.isEmpty();
    }

    public OrderPeriodCopyTask() {
        init();
    }

    private void init() {
        periodDas = new OrderPeriodDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create OrderPeriodCopyTask");

        List<OrderPeriodDTO> orderPeriodDTO = periodDas.getOrderPeriods(entityId);

        for (OrderPeriodDTO orderPeriod : orderPeriodDTO) {
            OrderPeriodWS orderPeriodWS = OrderBL.getOrderPeriodWS(orderPeriod);
            OrderPeriodDTO periodDTO = new OrderPeriodDTO(orderPeriodWS.getPeriodUnitId(), orderPeriodWS.getValue(), targetEntityId);
            periodDTO = periodDas.save(periodDTO);
            if (orderPeriodWS.getDescriptions() != null
                    && orderPeriodWS.getDescriptions().size() > 0) {
                periodDTO.setDescription((orderPeriodWS
                        .getDescriptions().get(0)).getContent(), (orderPeriodWS.getDescriptions().get(0)).getLanguageId());
            }
            CopyCompanyUtils.oldNewOrderPeriodMap.put(orderPeriod.getId(), periodDTO.getId());
        }
        LOG.debug("OrderPeriodCopyTask has been completed.");
    }

}
