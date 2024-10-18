package com.sapienter.jbilling.server.provisioning.db;

import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;

/**
 * Created by marcolin on 29/09/16.
 */
public interface IOrderLineProvisioningCommandDTO {

    ProvisioningCommandStatus getCommandStatus();
    OrderChangeDTO getOrderChange();
    OrderLineDTO getOrderLine();
}
