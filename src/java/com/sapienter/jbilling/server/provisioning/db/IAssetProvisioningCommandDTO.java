package com.sapienter.jbilling.server.provisioning.db;

import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandStatus;

/**
 * Created by marcolin on 29/09/16.
 */
public interface IAssetProvisioningCommandDTO {

    AssetDTO getAsset();
    ProvisioningCommandStatus getCommandStatus();
}
