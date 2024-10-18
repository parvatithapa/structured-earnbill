package com.sapienter.jbilling.server.provisioning.db;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;

/**
 * Created by marcolin on 29/09/16.
 */
public interface IOrderProvisioningCommandDTO {

    OrderDTO getOrder();
    ProvisioningCommandStatus getCommandStatus();

}
