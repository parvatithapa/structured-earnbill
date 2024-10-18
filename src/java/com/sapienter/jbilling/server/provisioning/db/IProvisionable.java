package com.sapienter.jbilling.server.provisioning.db;


import java.util.List;

public interface IProvisionable {

    public List getProvisioningCommands();

    public void setProvisioningStatus(ProvisioningStatusDTO provisioningStatus);

    public ProvisioningStatusDTO getProvisioningStatus();

    public Integer getProvisioningStatusId();
}
