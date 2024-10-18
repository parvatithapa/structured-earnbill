package com.sapienter.jbilling.server.process.task;/*
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

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

/**
 * Created by marcomanzi on 3/3/14.
 */
public abstract class AbstractFileExchangeTask extends PluggableTask implements IFileExchangeTask {

    protected static final String PARAM_DOWNLOAD = "download";

    protected static final ParameterDescription PARAM_DOWNLOAD_DESC =
            new ParameterDescription(PARAM_DOWNLOAD, true, ParameterDescription.Type.BOOLEAN);

    {
        descriptions.add(PARAM_DOWNLOAD_DESC);
    }

    protected String getParameterValueFor(String parameterName) throws PluggableTaskException {
        String parameter = getParameter(parameterName, "");
        //check that we have the parameter
        if (parameter.trim().isEmpty()) {
            throw new PluggableTaskException("Parameter " + parameterName + " not specified");
        }
        return parameter;
    }

    protected boolean existParameter(String parameterName) {
        String parameter = getParameter(parameterName, "");
        return !parameter.trim().isEmpty();
    }

    public boolean isDownloadTask() throws PluggableTaskException {
        return Boolean.parseBoolean(getParameterValueFor(PARAM_DOWNLOAD));
    }
}
