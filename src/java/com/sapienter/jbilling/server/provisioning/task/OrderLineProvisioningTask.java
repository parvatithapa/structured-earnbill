package com.sapienter.jbilling.server.provisioning.task;

import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.provisioning.db.IProvisionable;
import org.apache.log4j.Logger;

/**
 *  Plugin for creating provisioning commands
 *  whenever an order line is provisioned via order change
 *
 * @author Panche Isajeski
 * @since Dec-20-2013
 */
public class OrderLineProvisioningTask extends AbstractProvisioningTask {

    private static final Logger LOG = Logger.getLogger(OrderLineProvisioningTask.class);

    // pluggable task parameters names
    public static final ParameterDescription PARAMETER_ORDER_CHANGE_STATUS_ID =
            new ParameterDescription("provisionable_order_change_status_id", true, ParameterDescription.Type.INT);

    //initializer for pluggable params
    {
        descriptions.add(PARAMETER_ORDER_CHANGE_STATUS_ID);
    }

    @Override
    boolean isActionProvisionable(IProvisionable provisionable) {
        if (provisionable instanceof OrderChangeDTO) {
            return isOrderChangeProvisionable((OrderChangeDTO) provisionable);
        }

        return false;
    }

    @Override
    void provisioning(OrderChangeDTO orderChange, CommandManager c) {

        if (orderChange != null) {
            c.addCommand("order_change_status_provisioning_command");
            c.addParameter("msisdn", "12345");
            c.addParameter("imsi", "11111");
            LOG.debug("Added command for provisioning when change order status. Order " + orderChange.getId());
        }


    }

    protected boolean isOrderChangeProvisionable(OrderChangeDTO orderChangeDTO) {

        Integer provisionableOrderChangeStatus = Integer.valueOf(parameters.get(
                PARAMETER_ORDER_CHANGE_STATUS_ID.getName()));
        return provisionableOrderChangeStatus.equals(orderChangeDTO.getUserAssignedStatus().getId());
    }
}
