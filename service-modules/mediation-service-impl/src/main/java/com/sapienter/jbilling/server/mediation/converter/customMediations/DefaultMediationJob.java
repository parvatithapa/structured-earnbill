package com.sapienter.jbilling.server.mediation.converter.customMediations;

import java.util.Collections;
import java.util.List;

import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.validator.mediation.MediationJobParameterValidator;

/**
 * Created by marcomanzicore on 25/11/15.
 */
@SuppressWarnings("serial")
public class DefaultMediationJob  implements MediationJob {

    private String lineConverter;
    private String resolver;
    private String writer;
    private String job;
    private String recycleJob;

    protected DefaultMediationJob(String job, String lineConverter, String resolver, String writer, String recycleJob) {
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
        return false;
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
        return OrderService.BEAN_NAME;
    }

    @Override
    public List<String> getCdrTypes() {
        return Collections.emptyList();
    }

    @Override
    public MediationJobParameterValidator getParameterValidator() {
        return Context.getBean("defaultValidator");
    }
}
