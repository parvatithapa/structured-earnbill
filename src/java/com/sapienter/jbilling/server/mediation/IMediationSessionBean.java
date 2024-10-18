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


import com.sapienter.jbilling.common.InvalidArgumentException;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;

import java.util.List;

/**
 * @author emilc
 */
public interface IMediationSessionBean {

    public List<MediationConfiguration> getAllConfigurations(Integer entityId, boolean includeGlobal);

    public Integer createConfiguration(MediationConfiguration cfg, Integer callingCompanyId, Integer executorId);

    public List<MediationConfiguration> updateAllConfiguration(List<MediationConfiguration> configurations, Integer callingCompanyId, Integer executorId) throws InvalidArgumentException;

    public void delete(Integer cfgId, Integer companyId, Integer executorId);

    public MediationConfiguration getMediationConfiguration(Integer configurationId);
    
    List<MediationConfigurationWS> getMediationConfigurations(List<Integer> entities);
}
