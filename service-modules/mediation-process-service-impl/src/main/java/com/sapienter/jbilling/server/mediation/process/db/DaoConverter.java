package com.sapienter.jbilling.server.mediation.process.db;

import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.process.db.MediationProcessDAO;

/**
 * Created by andres on 19/10/15.
 */
public class DaoConverter {

    public static MediationProcess getMediationProcess(MediationProcessDAO dao) {
        return new MediationProcess(dao.getId(), dao.getEntityId(), dao.getConfigurationId(),
                dao.getGlobal(), dao.getStartDate(), dao.getEndDate(),dao.getRecordsProcessed(),
                dao.getDoneAndBillable(), dao.getErrors(), dao.getDuplicates(), dao.getDoneAndNotBillable(),
                dao.getOrderAffectedCount(), dao.getAggregated(), dao.getFileName());
    }

    public static MediationProcessDAO getMediationProcessDAO (MediationProcess mediationProcess) {
        return new MediationProcessDAO(mediationProcess.getId(), mediationProcess.getEntityId(),
                mediationProcess.getConfigurationId(), mediationProcess.getGlobal(),
                mediationProcess.getStartDate(), mediationProcess.getEndDate(), mediationProcess.getRecordsProcessed(),
                mediationProcess.getDoneAndBillable(), mediationProcess.getErrors(), mediationProcess.getDuplicates(),
                mediationProcess.getDoneAndNotBillable(), mediationProcess.getOrderAffectedCount(),
                mediationProcess.getAggregated(), mediationProcess.getFileName());
    }
}