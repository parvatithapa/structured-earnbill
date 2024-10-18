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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.db.*;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDAS;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MediationBL
 *
 * @author Brian Cowdery
 * @since 21-10-2010
 */
public class MediationConfigurationBL {

    private static PluggableTaskDAS getPluggableTaskDAS() {
        return Context.getBean(Context.Name.PLUGGABLE_TASK_DAS);
    }

    /**
     * Convert a given MediationConfiguration into a MediationConfigurationWS web-service object.
     *
     * @param dto dto to convert
     * @return converted web-service object
     */
    public static final MediationConfigurationWS getWS(MediationConfiguration dto) {
        if (null == dto) return null;
        MediationConfigurationWS ws = new MediationConfigurationWS();
        ws.setId(dto.getId());
        ws.setEntityId(dto.getEntityId());
        ws.setPluggableTaskId(dto.getPluggableTask() != null ? dto.getPluggableTask().getId() : null);
        ws.setProcessorTaskId(dto.getProcessor() != null ? dto.getProcessor().getId() : null);
        ws.setName(dto.getName());
        ws.setOrderValue(dto.getOrderValue().toString());
        ws.setCreateDatetime(dto.getCreateDatetime() == null ? TimezoneHelper.serverCurrentDate() : dto.getCreateDatetime());
        ws.setVersionNum(dto.getVersionNum());
        ws.setMediationJobLauncher(dto.getMediationJobLauncher());
        ws.setGlobal(dto.getGlobal());
        ws.setLocalInputDirectory(dto.getLocalInputDirectory());
        ws.setRootRoute(dto.getRootRoute() != null ? dto.getRootRoute().getId() : null);
        ws.setLocalInputDirectory(dto.getLocalInputDirectory());
        return ws;
    }

    /**
     * Converts a list of MediationConfiguration objects into MediationConfigurationWS web-service objects.
     *
     * @see #getWS(MediationConfiguration)
     *
     * @param objects objects to convert
     * @return a list of converted DTO objects, or an empty list if ws objects list was empty.
     */
    public static List<MediationConfigurationWS> getWS(List<MediationConfiguration> objects) {
        List<MediationConfigurationWS> ws = new ArrayList<MediationConfigurationWS>(objects.size());
        for (MediationConfiguration dto : objects)
            ws.add(getWS(dto));
        return ws;
    }

    /**
     * Convert a given MediationConfigurationWS web-service object into a MediationConfiguration entity.
     *
     * For mediation 2.0, the MediationConfigurationWS must have a pluggable task ID or an exception will be thrown.
     * </p>
     * For mediation 3.0, the MediationConfigurationWS must have a mediationJobLauncher or an exception will be thrown.
     * @param ws ws object to convert
     * @return converted DTO object
     * @throws SessionInternalError if required field is missing
     */
    public static MediationConfiguration getDTO(MediationConfigurationWS ws) {
        if (ws != null) {

            String version = Util.getSysProp(Constants.PROPERTY_MEDIATION_VERSION);
            RouteDTO routeDTO = ws.getRootRoute() != null ? new RouteDAS().getRoute(ws.getRootRoute()) : null;

            switch (MediationVersion.getMediationVersion(version)) {
                case MEDIATION_VERSION_3_0:
                case MEDIATION_VERSION_4_0:
                	if (StringUtils.isBlank(ws.getMediationJobLauncher())) {
    					throw new SessionInternalError("MediationConfiguration must have a mediation job launcher specified.", 
    							new String[] { "MediationConfigurationWS,mediationJobLauncher,validation.mediation.record.joblauncher.not.exist" });
    				}
                    return new MediationConfiguration(ws, null, null, routeDTO);

                default:
                	if (ws.getPluggableTaskId() == null)
    					throw new SessionInternalError("MediationConfiguration must have a pluggable task id.", 
    						new String[] { "MediationConfigurationWS,pluggableTaskId,validation.mediation.record.plugabletaskId.not.exist" });

    				if (ws.getProcessorTaskId() == null)
    					throw new SessionInternalError("MediationConfiguration must have a processor task id.", 
    						new String[] { "MediationConfigurationWS,processorTaskId,validation.mediation.record.processorTaskId.not.exist" });

                    PluggableTaskDTO pluggableTask = getPluggableTaskDAS().find(ws.getPluggableTaskId());
                    PluggableTaskDTO processorTask = getPluggableTaskDAS().find(ws.getProcessorTaskId());

                    return new MediationConfiguration(ws, pluggableTask, processorTask, routeDTO);
            }
        }
        return null;
    }

    /**
     * Converts a list of MediationConfigurationWS web-service objects into MediationConfiguration objects.
     *
     * @see #getDTO(MediationConfigurationWS)
     *
     * @param objects web-service objects to convert
     * @return a list of converted WS objects, or an empty list if DTO objects list was empty.
     */
    public static List<MediationConfiguration> getDTO(List<MediationConfigurationWS> objects) {
        List<MediationConfiguration> dto = new ArrayList<MediationConfiguration>(objects.size());
        for (MediationConfigurationWS ws : objects)
            dto.add(getDTO(ws));
        return dto;
    }
    
    /**
     * This function checks if a particular mediation reader task is being used on a 
     * mediation configuration. If it is in use, it returns true, else returns false.
     * @param pluggableTaskId
     * @return true/false
     */
    public boolean isMediationReaderInUse(Integer pluggableTaskId) {
    	List<MediationConfiguration> mediationConfigurations = 
    			new MediationConfigurationDAS().findAllByPluggableTask(pluggableTaskId);
    	
    	if (mediationConfigurations != null && !mediationConfigurations.isEmpty())
    		return true;
    	else
    		return false;
    }
    
    /**
     * This function checks if a particular mediation processor task is being used on a 
     * mediation configuration. If it is in use, it returns true, else returns false.
     * @param pluggableTaskId
     * @return true/false
     */
    public boolean isMediationProcessorInUse(Integer pluggableTaskId) {
    	List<MediationConfiguration> mediationConfigurations = 
    			new MediationConfigurationDAS().findAllByProcessorTask(pluggableTaskId);
    	
    	if (mediationConfigurations != null && !mediationConfigurations.isEmpty())
    		return true;
    	else
    		return false;
    }

    public List<MediationConfiguration> getAllConfigurations(Integer entityId, boolean includeGlobal){
        CompanyDAS companyDAS = new CompanyDAS();
        MediationConfigurationDAS mediationConfigurationDAS = new MediationConfigurationDAS();
        if(!includeGlobal || companyDAS.isRoot(entityId)){
            return mediationConfigurationDAS.findAllByEntity(entityId);
        } else {
            Integer parentCompanyId = companyDAS.find(entityId).getParent().getId();
            return mediationConfigurationDAS.findAllByEntityIncludeGlobal(entityId, parentCompanyId);
        }
    }

    public static List<MediationConfiguration> getAllGlobalByCompany(Integer parentCompany) {
        MediationConfigurationDAS mediationConfigurationDAS = new MediationConfigurationDAS();
        return mediationConfigurationDAS.findAllByEntityIncludeGlobal(null, parentCompany);
    }
}
