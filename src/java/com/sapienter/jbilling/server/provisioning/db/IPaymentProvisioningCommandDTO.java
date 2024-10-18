package com.sapienter.jbilling.server.provisioning.db;

import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;

/**
 * Created by marcolin on 29/09/16.
 */
public interface IPaymentProvisioningCommandDTO {
    PaymentDTO getPayment();
    ProvisioningCommandStatus getCommandStatus();
}

