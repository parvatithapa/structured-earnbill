package com.sapienter.jbilling.server.mediation.custommediation.spc.jobs;

import static com.sapienter.jbilling.server.mediation.converter.MediationJobs.addJob;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationUtil;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.validator.mediation.MediationJobParameterValidator;

import java.util.Arrays;
import java.util.List;

/**
 * @author Neelabh
 * @since Dec 18, 2018
 */
public class SPCMediationJobsInit implements MediationJob {

    private static final long serialVersionUID = 3734598262343757581L;

    private String lineConverter;
    private String resolver;
    private String writer;
    private String job;
    private String recycleJob;

    public SPCMediationJobsInit() {

    }

    static {
        //Registering SPC Mediation Jobs
        addJob(new SPCMediationJobsInit(SPCMediationUtil.MEDIATION_JOB_LAUNCHER_BEAN_NAME, SPCMediationUtil.RECORD_LINE_CONVERTER_BEAN_NAME,
                SPCMediationUtil.CDR_RESOLVER_BEAN_NAME, SPCMediationUtil.JMR_DEFAULT_WRITER_BEAN, 
                SPCMediationUtil.RECYCLE_JOB_LAUNCHER_BEAN_NAME));
        addJob(new SPCMediationJobsInit(SPCMediationUtil.RECYCLE_JOB_LAUNCHER_BEAN_NAME, SPCMediationUtil.RECORD_LINE_CONVERTER_BEAN_NAME,
                SPCMediationUtil.CDR_RESOLVER_BEAN_NAME, SPCMediationUtil.JMR_DEFAULT_WRITER_BEAN, null));
    }

    public SPCMediationJobsInit(String job, String lineConverter, String resolver, String writer, String recycleJob) {
        this.lineConverter = lineConverter;
        this.job = job;
        this.resolver = resolver;
        this.writer = writer;
        this.recycleJob = recycleJob;
    }

    @Override
    public String getResolver() {
        return resolver;
    }

    @Override
    public String getWriter() {
        return writer;
    }

    @Override
    public String getJob() {
        return job;
    }

    @Override
    public boolean handleRootRateTables() {
        return false;
    }

    @Override
    public boolean needsInputDirectory() {
        return true;
    }

    @Override
    public String getLineConverter() {
        return lineConverter;
    }

    @Override
    public String getRecycleJob() {
        return recycleJob;
    }

    @Override
    public String getOrderServiceBeanName() {
        return "spcOrderService";
    }

    @Override
    public List<String> getCdrTypes() {
        return Arrays.asList(MediationServiceType.getCdrTypes());
    }

    @Override
    public MediationJobParameterValidator getParameterValidator() {
        return Context.getBean("defaultValidator");
    }

}
