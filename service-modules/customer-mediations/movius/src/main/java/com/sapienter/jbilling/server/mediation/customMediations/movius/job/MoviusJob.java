package com.sapienter.jbilling.server.mediation.customMediations.movius.job;

import static com.sapienter.jbilling.server.mediation.converter.MediationJobs.addJob;

import java.util.Arrays;
import java.util.List;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusUtil;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.validator.mediation.MediationJobParameterValidator;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class MoviusJob implements MediationJob {

    private static final long serialVersionUID = 3784598268883989587L;

    private String lineConverter;
    private String resolver;
    private String writer;
    private String job;
    private String recycleJob;

    public MoviusJob() {

    }

    /**
     * Registering Mediation Jobs
     */
    static {
        addJob(new MoviusJob(MoviusUtil.MEDIATION_JOB_CONFIGURATION_BEAN_NAME, MoviusUtil.RECORD_LINE_CONVERTER_BEAN_NAME,
				MoviusUtil.CDR_RESOLVER_BEAN_NAME, MoviusUtil.JMR_DEFAULT_WRITER_BEAN, MoviusUtil.RECYCLE_JOB_CONFIGURATION_BEAN_NAME));

        addJob(new MoviusJob(MoviusUtil.RECYCLE_JOB_CONFIGURATION_BEAN_NAME, MoviusUtil.RECORD_LINE_CONVERTER_BEAN_NAME,
				MoviusUtil.CDR_RESOLVER_BEAN_NAME, MoviusUtil.JMR_DEFAULT_WRITER_BEAN, null));
    }

    public MoviusJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
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
        return "moviusOrderService";
    }

    @Override
    public List<String> getCdrTypes() {
        return Arrays.asList("incoming-call",
                "outgoing-call",
                "incoming-sms-details",
                "outgoing-sms");
    }

    @Override
    public MediationJobParameterValidator getParameterValidator() {
        return Context.getBean("defaultValidator");
    }
}
