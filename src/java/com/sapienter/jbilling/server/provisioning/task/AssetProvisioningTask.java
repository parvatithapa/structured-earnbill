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

package com.sapienter.jbilling.server.provisioning.task;

import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.provisioning.db.IProvisionable;
import org.apache.log4j.Logger;

public class AssetProvisioningTask extends AbstractProvisioningTask {

    private static final Logger LOG = Logger.getLogger(ExampleProvisioningTask.class);

    @Override
    boolean isActionProvisionable(IProvisionable provisionable) {
        return true;
    }

    @Override
    public void add(AssetDTO asset, CommandManager c) {
        if (asset != null) {
            c.addCommand("asset_assigned_command");
            c.addParameter("msisdn", "12345");
            c.addParameter("imsi", "11111");
            LOG.debug("Added command for provisioning when an asset is added to an order. Order line" + asset.getOrderLine().getId());
        }
    }

    @Override
    public void create(AssetDTO asset, CommandManager c) {
        if (asset != null) {
            c.addCommand("new_asset_command");
            c.addParameter("msisdn", "12345");
            c.addParameter("imsi", "11111");
            LOG.debug("Added command for provisioning when new Asset created. Asset " + asset.getId());
        }
    }

    @Override
    public void update(AssetDTO asset, CommandManager c) {
        if (asset != null) {
            c.addCommand("updated_asset_command");
            c.addParameter("msisdn", "12345");
            c.addParameter("imsi", "11111");
            LOG.debug("Added command for provisioning when asset updated. Asset " + asset.getId());
        }
    }
}
