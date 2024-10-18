package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by vivek on 31/10/14.
 */
public class BillingProcessConfigurationCopyTask extends AbstractCopyTask {

    BillingProcessConfigurationDAS billingProcessConfigurationDAS = null;
    CompanyDAS companyDAS = null;
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BillingProcessConfigurationCopyTask.class));

    private static final Class dependencies[] = new Class[]{};

    public Class[] getDependencies() {
        return dependencies;
    }

    public BillingProcessConfigurationCopyTask() {
        init();
    }

    public void init() {
        billingProcessConfigurationDAS = new BillingProcessConfigurationDAS();
        companyDAS = new CompanyDAS();
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        CompanyDTO targetEntity = new CompanyDAS().find(targetEntityId);
        List<BillingProcessConfigurationDTO> copyBillingProcessConfigurationDTOs = billingProcessConfigurationDAS.findAllByEntity(targetEntity);
        return copyBillingProcessConfigurationDTOs != null && !copyBillingProcessConfigurationDTOs.isEmpty();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        LOG.debug("Create BillingProcessConfigurationCopyTask");
        copyBillingConfiguration(entityId, targetEntityId);
    }

    private void copyBillingConfiguration(Integer entityId, Integer targetEntityId) {
        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
        List<BillingProcessConfigurationDTO> copyBillingProcessConfigurationDTOs = billingProcessConfigurationDAS.findAllByEntity(targetEntity);
        if (copyBillingProcessConfigurationDTOs.isEmpty()) {
            billingProcessConfigurationDAS.copyBillingProcessConfiguration(entityId, targetEntityId);
        }
    }
}
