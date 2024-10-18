package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.server.mediation.sourcereader.FtpFileRetriever;
import com.sapienter.jbilling.server.util.Context;

/**
 * Created by marcomanzi on 1/31/14.
 */
public class FtpsRemoteCopyTask extends FtpRemoteCopyTask {

    @Override
    protected FtpFileRetriever retriever() {
        return Context.getBean("ftpsRemoteFileRetriever");
    }

}
