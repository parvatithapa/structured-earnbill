package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.sourceUploader.AbstractRemoteFileUploader;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;


public class FtpRemoteUploadTask extends AbstractFileExchangeTask {

    protected static final FormatLogger LOG = new FormatLogger(Logger.getLogger(FtpRemoteUploadTask.class));

    protected static final String PARAM_SERVER = "server";
    protected static final String PARAM_SERVER_PORT = "server_port";
    protected static final String PARAM_SERVER_USER = "user";
    protected static final String PARAM_SERVER_PASSWORD = "password";
    protected static final String PARAM_REMOTE_PATH = "remote_path";
    protected static final String PARAM_REMOTE_MOVE_TO_PATH = "remote_move_to_path";
    protected static final String PARAM_LOCAL_PATH = "local_path";
    protected static final String PARAM_SUFFIX = "suffix";
    protected static final String PARAM_TEMP_FOLDER = "temp_folder";
    protected static final String PARAM_FILE_NAME_REGEX = "file_name_regex";
    protected static final String PARAM_RECURSIVE = "recursive";
    protected static final String PARAM_UNCOMPRESS = "uncompress";

    protected static final ParameterDescription PARAM_SERVER_DESC =
            new ParameterDescription(PARAM_SERVER, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_SERVER_PORT_DESC =
            new ParameterDescription(PARAM_SERVER_PORT, true, ParameterDescription.Type.INT);
    protected static final ParameterDescription PARAM_SERVER_USER_DESC=
            new ParameterDescription(PARAM_SERVER_USER, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_SERVER_PASSWORD_DESC =
            new ParameterDescription(PARAM_SERVER_PASSWORD, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_REMOTE_DESC =
            new ParameterDescription(PARAM_REMOTE_PATH, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_REMOTE_MOVE_TO_PATH_DESC =
            new ParameterDescription(PARAM_REMOTE_MOVE_TO_PATH, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_LOCAL_DESC =
            new ParameterDescription(PARAM_LOCAL_PATH, true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_SUFFIX_DESC =
            new ParameterDescription(PARAM_SUFFIX , false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_TEMP_FOLDER_DESC =
            new ParameterDescription(PARAM_TEMP_FOLDER , true, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_FILE_NAME_REGEX_DESC =
            new ParameterDescription(PARAM_FILE_NAME_REGEX, false, ParameterDescription.Type.STR);
    protected static final ParameterDescription PARAM_RECURSIVE_DESC=
            new ParameterDescription(PARAM_RECURSIVE, false, ParameterDescription.Type.BOOLEAN);
    protected static final ParameterDescription PARAM_UNCOMPRESS_DESC=
            new ParameterDescription(PARAM_UNCOMPRESS, false, ParameterDescription.Type.BOOLEAN);

    {
        descriptions.add(PARAM_REMOTE_DESC);
        descriptions.add(PARAM_REMOTE_MOVE_TO_PATH_DESC);
        descriptions.add(PARAM_LOCAL_DESC);
        descriptions.add(PARAM_TEMP_FOLDER_DESC);
        descriptions.add(PARAM_SUFFIX_DESC);
        descriptions.add(PARAM_SERVER_DESC);
        descriptions.add(PARAM_SERVER_PORT_DESC);
        descriptions.add(PARAM_SERVER_USER_DESC);
        descriptions.add(PARAM_SERVER_PASSWORD_DESC);
        descriptions.add(PARAM_FILE_NAME_REGEX_DESC);
        descriptions.add(PARAM_RECURSIVE_DESC);
        descriptions.add(PARAM_UNCOMPRESS_DESC);
    }

    public void execute() {
        try {
            LOG.info("Copy to Remote started");
            AbstractRemoteFileUploader uploader = this.retriever();
            uploader.setRemotePath(getParameterValueFor(PARAM_REMOTE_PATH));
            uploader.setLocalPath(getParameterValueFor(PARAM_LOCAL_PATH));
            if (existParameter(PARAM_SUFFIX)) {
                uploader.setSuffix(getParameterValueFor(PARAM_SUFFIX));
            }
            if (existParameter(PARAM_REMOTE_MOVE_TO_PATH)) {
                uploader.setRemoteMoveToPath(getParameterValueFor(PARAM_REMOTE_MOVE_TO_PATH));
            }
            if (existParameter(PARAM_FILE_NAME_REGEX)) {
                uploader.setFileNameRegex(getParameterValueFor(PARAM_FILE_NAME_REGEX));
            }
            uploader.setTempFolder(getParameterValueFor(PARAM_TEMP_FOLDER));
            uploader.setRecursive(getParameter(PARAM_RECURSIVE_DESC.getName(), false));
            uploader.setUncompress(getParameter(PARAM_UNCOMPRESS_DESC.getName(), false));
            uploader.copyToRemoteFileSystemFrom(getParameterValueFor(PARAM_SERVER_USER),
                    getParameterValueFor(PARAM_SERVER_PASSWORD),
                    getParameterValueFor(PARAM_SERVER),
                    Integer.parseInt(getParameterValueFor(PARAM_SERVER_PORT)));
            LOG.info("Remote copy ended");
        } catch (PluggableTaskException e) {
            LOG.error("Problem executing FtpRemoteUploadTask", e);
        }
    }

    protected AbstractRemoteFileUploader retriever() throws PluggableTaskException {
        return Context.getBean("ftpRemoteFileUploader");
    }
}
