/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.provisioning.db;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.persistence.*;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandType;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.UUID;

@Entity
@DiscriminatorValue("asset")
public class AssetProvisioningCommandDTO extends ProvisioningCommandDTO implements IAssetProvisioningCommandDTO {

    private AssetDTO asset;

    public AssetProvisioningCommandDTO() {}

    public AssetProvisioningCommandDTO(AssetDTO assetDTO) {
        this.asset = assetDTO;

        setEntity(getAsset().getEntity());
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(
            name = "asset_provisioning_command_map",
            joinColumns = @JoinColumn(name = "provisioning_command_id"),
            inverseJoinColumns = @JoinColumn(name = "asset_id")
    )
    public AssetDTO getAsset() {
        return this.asset;
    }

    public void setAsset(AssetDTO assetDTO) {
        this.asset = assetDTO;
    }

    @Transient
    @Override
    public ProvisioningCommandType getCommandType() {
        return ProvisioningCommandType.ASSET;
    }

    @Transient
    @Override
    public void postCommand(MapMessage message, String eventType) throws JMSException {
        FormatLogger LOG = new FormatLogger(Logger.getLogger(AssetProvisioningCommandDTO.class));
        EventLogger eLogger = EventLogger.getInstance();

        UUID uid = UUID.randomUUID();

        message.setStringProperty("id", uid.toString());
        LOG.debug("set message property id=" + uid.toString());
        message.setIntProperty("commandId", getId());
        LOG.debug("set message commandId=" + getId());
        message.setStringProperty("command", getName());
        LOG.debug("set message command=" + getName());
        message.setIntProperty("entityId", getEntity() != null ? getEntity().getId() : 0);
        LOG.debug("set message property entityId=" + getEntity().getId());
        message.setIntProperty("asset_id", getAsset().getId());
        LOG.debug("set message property asset_id=" + getAsset().getId());

        for (Map.Entry<String, String> param : getCommandParameters().entrySet())
        {
            message.setStringProperty(param.getKey(), param.getValue());
            LOG.debug("set Message property : (" + param.getKey() + ","
                    + param.getValue() + ")");
        }

        if (asset != null) {
            asset.setProvisioningStatusId(Constants.PROVISIONING_STATUS_PENDING_ACTIVE);
        }

        LOG.debug("Sending message for command '" + getName() + "'");
    }
}
