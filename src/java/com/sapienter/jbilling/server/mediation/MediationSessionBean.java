/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.mediation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.InvalidArgumentException;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.converter.MediationJobs;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.mediation.db.MediationConfigurationDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.EventLogger;

/**
 *
 * @author emilc
 **/
@Transactional( propagation = Propagation.REQUIRED )
public class MediationSessionBean implements IMediationSessionBean {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(MediationSessionBean.class));



    /**
     * Returns a list of all MediationConfiguration's for the given entity id. In case the
     * includeGlobal parameter is set to true and the company is a child then this method
     * also includes the gloabl mediation configurations from the root company.
     *
     * @param entityId entity id
     * @param includeGlobal whether to include global mediation configurations from parent company
     *
     * @return list of mediation configurations for entity, empty list if none found
     */
    public List<MediationConfiguration> getAllConfigurations(Integer entityId, boolean includeGlobal) {
        MediationConfigurationBL configurationBL = new MediationConfigurationBL();
        return configurationBL.getAllConfigurations(entityId, includeGlobal);
    }

    public MediationConfiguration getMediationConfiguration(Integer configurationId) {
        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();
        return cfgDAS.findNow(configurationId);
    }

    public Integer createConfiguration(MediationConfiguration cfg, Integer callingCompanyId, Integer executorId) {
        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();
        CompanyDAS companyDAS = new CompanyDAS();

        validateMediationConfiguration(MediationVersion.getMediationVersion(
                Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION)), cfg);

        if(null != cfg.getGlobal() && cfg.getGlobal().booleanValue() &&
                !companyDAS.isRoot(callingCompanyId)){
            throw new SessionInternalError("Can not create mediation configuration for entity id: "
                    + cfg.getEntityId() + ". Child companies can not create global mediation configurations", HttpStatus.SC_BAD_REQUEST);
        }

        cfg.setCreateDatetime(Calendar.getInstance().getTime());
        Integer cfgId = cfgDAS.save(cfg).getId();
        EventLogger.getInstance().audit(executorId, null,
                Constants.TABLE_MEDIATION_CFG, cfgId,
                EventLogger.MODULE_MEDIATION, EventLogger.ROW_CREATED, null,
                null, null);

        return cfgId;
    }

    public List<MediationConfiguration> updateAllConfiguration(
            List<MediationConfiguration> configurations, Integer callingCompanyId, Integer executorId)
            throws InvalidArgumentException {
        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();
        CompanyDAS companyDAS = new CompanyDAS();
        List<MediationConfiguration> retValue = new ArrayList<MediationConfiguration>();
        try {

            for (MediationConfiguration cfg : configurations) {

                if(!cfg.getEntityId().equals(callingCompanyId)){
                    throw new SessionInternalError("Can not update the mediation configuration with id: "
                            + cfg.getId() + ".The mediation configuration does not belong to the calling company.", HttpStatus.SC_BAD_REQUEST);
                }

                if(cfg.getGlobal().booleanValue() && !companyDAS.isRoot(callingCompanyId)){
                    throw new SessionInternalError("Can not update the mediation configuration with id: "
                            + cfg.getId() + ". Child companies can not create global mediation configurations", HttpStatus.SC_BAD_REQUEST);
                }

                // validate mediation config based on the version
                validateMediationConfiguration(MediationVersion.getMediationVersion(
                        Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION)), cfg);

                if(cfg.getId() != null && cfg.getId().intValue() > 0) {
                    MediationConfiguration config = cfgDAS.find(cfg.getId());
                    config.setName(cfg.getName());
                    config.setMediationJobLauncher(cfg.getMediationJobLauncher());
                    config.setGlobal(cfg.getGlobal());
                    config.setLocalInputDirectory(cfg.getLocalInputDirectory());
                    config.setCdrsForRecycle(cfg.getCdrsForRecycle());
                    config.setOrderValue(cfg.getOrderValue());
                    config.setPluggableTask(cfg.getPluggableTask());
                    config.setProcessor(cfg.getProcessor());
                    config.setRootRoute(cfg.getRootRoute());
                    retValue.add(config);
                } else {
                    retValue.add(cfgDAS.save(cfg));
                }
            }
            return retValue;
        } catch (EntityNotFoundException e1) {
            throw new InvalidArgumentException("Wrong data saving mediation configuration", 1, e1);
        } catch (InvalidArgumentException e2) {
            throw new InvalidArgumentException(e2);
        } catch (Exception e) {
            throw new SessionInternalError("Exception updating mediation configurations ", MediationSessionBean.class, e);
        }
    }

    public void delete(Integer cfgId, Integer companyId, Integer executorId) {


        MediationConfigurationDAS cfgDAS = new MediationConfigurationDAS();

        MediationConfiguration configuration = cfgDAS.find(cfgId);

        //TODO HACKATHON: MISSED IMPLEMENTATION
//        List<MediationProcess> mediationProcesses = new MediationProcessDAS().findAllByConfiguration(cfgId);
//        if (mediationProcesses.size() > 0) {
//            throw new SessionInternalError("Error deleting mediation configuration:", new String[]{
//                    "mediation.config.delete.failure"
//            });
//        }
        
        if(!configuration.getEntityId().equals(companyId)){
            throw new SessionInternalError("Can not delete the mediation configuration.",
                    new String[] { "The mediation configuration does not belong to the calling company" }, HttpStatus.SC_BAD_REQUEST);
        }
        
        cfgDAS.delete(configuration);

        EventLogger.getInstance().audit(executorId, null,
                Constants.TABLE_MEDIATION_CFG, cfgId,
                EventLogger.MODULE_MEDIATION, EventLogger.ROW_DELETED, null,
                null, null);
    }

    private void validateMediationConfiguration(MediationVersion version, MediationConfiguration cfg) throws InvalidArgumentException {

        switch (version) {
            case MEDIATION_VERSION_2_0:

                if (cfg.getPluggableTask() == null || cfg.getProcessor() == null) {
                    throw new InvalidArgumentException("Mediation configuration pluggable tasks not found: " + cfg, 1);
                }

                if (cfg.getPluggableTask().getEntityId() == null) {
                    PluggableTaskDAS pt = Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
                    PluggableTaskDTO task = pt.find(cfg.getPluggableTask().getId());
                    if (task != null && task.getEntityId().equals(cfg.getEntityId())) {
                        cfg.setPluggableTask(task);
                    } else {
                        throw new InvalidArgumentException("Task not found or " +
                                "entity of pluggable task is not the same when " +
                                "creating a new mediation configuration", 1);
                    }
                }

                break;

            case MEDIATION_VERSION_3_0:
            case MEDIATION_VERSION_4_0:

                String mediationJobLauncherName = cfg.getMediationJobLauncher();
                if (MediationJobs.getJobForName(mediationJobLauncherName) == null) {
                    throw new SessionInternalError("Mediation job launcher not found", new String[] {
                            "MediationConfigurationWS,mediationJobLauncher,validation.error.mediationJobLauncher.notFound"}, HttpStatus.SC_BAD_REQUEST
                    );
                }
                Boolean needsInputDirectory=MediationJobs.getJobForName(mediationJobLauncherName).needsInputDirectory();
                if(needsInputDirectory&&StringUtils.isBlank(cfg.getLocalInputDirectory())) {
                    throw new SessionInternalError("Local input directory not found");
                }

                if (needsInputDirectory&&!StringUtils.isBlank(cfg.getLocalInputDirectory())){
                  Path destinationDir = Paths.get(cfg.getLocalInputDirectory()+"/checkPermissions");
                    try{
                        Files.createDirectory(destinationDir);
                        Files.deleteIfExists(destinationDir);
                    }catch(IOException e){
                        LOG.error(e.getMessage(), e);
                        throw new SessionInternalError("Read or Write permissions not found on local input directory");
                    }

                }

                break;
        }
    }

    @Override
    public List<MediationConfigurationWS> getMediationConfigurations(List<Integer> entities) {
        return new MediationConfigurationDAS().findAllByEntities(entities)
                .stream()
                .map(MediationConfigurationBL::getWS)
                .collect(Collectors.toList());
    }

    
}
