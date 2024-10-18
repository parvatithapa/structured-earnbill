package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.sourcereader.AbstractRemoteFileRetriever;
import com.sapienter.jbilling.server.mediation.sourcereader.SftpFileRetriever;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.springframework.integration.ftp.session.DefaultFtpsSessionFactory;

/**
 * Created by marcomanzi on 1/31/14.
 */
public class SftpRemoteCopyTask extends FtpRemoteCopyTask {

    protected static final FormatLogger LOG = new FormatLogger(Logger.getLogger(SftpRemoteCopyTask.class));

    protected static final String PARAM_PROT = "prot";
    protected static final String PARAM_PROTOCOL = "protocol";
    protected static final String PARAM_PROTOCOLS = "protocols";
    protected static final String PARAM_AUTH_VALUE = "authValue";


    protected static final ParameterDescription PARAM_PROT_DESC =
            new ParameterDescription(PARAM_PROT, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_PROTOCOL_DESC =
            new ParameterDescription(PARAM_PROTOCOL, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_PROTOCOLS_DESC=
            new ParameterDescription(PARAM_PROTOCOLS, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_AUTH_VALUE_DESC=
            new ParameterDescription(PARAM_AUTH_VALUE, true, ParameterDescription.Type.STR);

    {
        descriptions.add(PARAM_PROT_DESC);
        descriptions.add(PARAM_PROTOCOL_DESC);
        descriptions.add(PARAM_PROTOCOLS_DESC);
        descriptions.add(PARAM_AUTH_VALUE_DESC);
    }

    @Override
    protected AbstractRemoteFileRetriever retriever() throws PluggableTaskException {
        SftpFileRetriever retriever = Context.getBean("sftpRemoteFileRetriever");
        DefaultFtpsSessionFactory sessionFactory = (DefaultFtpsSessionFactory) retriever.getSessionFactory();
        sessionFactory.setProt(getParameterValueFor(PARAM_PROT));
        sessionFactory.setProtocol(getParameterValueFor(PARAM_PROT));
        sessionFactory.setProtocols(getParameterValueFor(PARAM_PROTOCOLS).split(","));
        sessionFactory.setAuthValue(getParameterValueFor(PARAM_AUTH_VALUE));
        return retriever;
    }
}
