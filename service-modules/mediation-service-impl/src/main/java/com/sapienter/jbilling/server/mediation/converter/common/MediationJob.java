package com.sapienter.jbilling.server.mediation.converter.common;

import java.io.Serializable;
import java.util.List;

import com.sapienter.jbilling.server.validator.mediation.MediationJobParameterValidator;

/**
 * Created by marcolin on 08/10/15.
 */
public interface MediationJob extends Serializable {
    String getLineConverter();
    String getRecycleJob();
    String getResolver();
    String getWriter();
    String getJob();
    boolean handleRootRateTables();
    boolean needsInputDirectory();
    String getOrderServiceBeanName();
    List<String> getCdrTypes();
    MediationJobParameterValidator getParameterValidator();
}
