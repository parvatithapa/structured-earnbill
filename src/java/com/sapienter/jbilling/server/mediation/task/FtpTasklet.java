package com.sapienter.jbilling.server.mediation.task;

import com.sapienter.jbilling.server.mediation.converter.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.sourcereader.AbstractRemoteFileRetriever;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.util.Context;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.lang.invoke.MethodHandles;
import java.util.Map;

public class FtpTasklet implements Tasklet {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public enum FtpMethod {FTP, FTPS, SFTP}

    protected static final String PARAM_SERVER = "server";
    protected static final String PARAM_SERVER_PORT = "server_port";
    protected static final String PARAM_SERVER_USER = "user";
    protected static final String PARAM_SERVER_PASSWORD = "password";
    protected static final String PARAM_REMOTE_PATH = "remote_path";
    protected static final String PARAM_REMOTE_MOVE_TO_PATH = "remote_move_to_path";
    protected static final String PARAM_REMOTE_DELETE = "remote_delete";
    protected static final String PARAM_LOCAL_PATH = "local_path";
    protected static final String PARAM_SUFFIX = "suffix";
    protected static final String PARAM_TEMP_FOLDER = "temp_folder";
    protected static final String PARAM_FILE_NAME_REGEX = "file_name_regex";
    protected static final String PARAM_RECURSIVE = "recursive";
    protected static final String PARAM_UNCOMPRESS = "uncompress";
    protected static final String PARAM_OVERWRITE = "overwrite";
    protected static final String PARAM_FTP_METHOD = "ftp_method";
    protected static final String PARAM_SSH_KEY_FILE = "ssh_key_file";
    protected static final String PARAM_SSH_KEY_FILE_PASSPHRASE = "ssh_key_file_passphrase";

    private Map<String, Object> jobParams;

    private String server;
    private String serverPort;
    private String user;
    private String password;
    private String remotePath;
    private String remoteMoveToPath;
    private boolean remoteDelete;
    private String localPath;
    private String suffix;
    private String tempFolder;
    private String fileNameRegex;
    private boolean recursive;
    private boolean uncompress;
    private boolean overwrite = false;
    private FtpMethod ftpMethod = FtpMethod.FTP;
    private String sshKeyFile;
    private String sshKeyFilePassphrase;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        try {
            jobParams = chunkContext.getStepContext().getJobParameters();

            logger.info("Remote copy started");
            AbstractRemoteFileRetriever retriever = this.retriever();
            retriever.setRemotePath(getRemotePath());
            retriever.setLocalPath(getLocalPath());
            String suffix = getSuffix();
            if (suffix != null) {
                retriever.setSuffix(suffix);
            }
            String remoteMoveToPath = getRemoteMoveToPath();
            if (remoteMoveToPath != null) {
                retriever.setRemoteMoveToPath(remoteMoveToPath);
            }
            String fileNameRegex = getFileNameRegex();
            if (fileNameRegex != null) {
                retriever.setFileNameRegex(fileNameRegex);
            }
            retriever.setTempFolder(getTempFolder());
            retriever.setRecursive(isRecursive());
            retriever.setUncompress(isUncompress());
            retriever.setDeleteRemote(isRemoteDelete());
            retriever.setOverwrite(isOverwrite());
            if(getSshKeyFile() != null) {
                retriever.copyToLocalFileSystemFrom(getUser(),
                    getSshKeyFile(),
                    getSshKeyFilePassphrase(),
                    getServer(),
                    Integer.parseInt(getServerPort()));
            } else {
                retriever.copyToLocalFileSystemFrom(getUser(),
                    getPassword(),
                    getServer(),
                    Integer.parseInt(getServerPort()));
            }
            logger.info("Remote copy ended");
        } catch (PluggableTaskException e) {
            logger.error("Problem executing FtpRemoteCopyTask", e);
        }
        return RepeatStatus.FINISHED;
    }

    private boolean existParameter(String name) {
        return jobParams.get(name) != null;
    }

    private String getStringParameterValueFor(String name, String defaultValue) {
        Object value = jobParams.get(name);
        if(value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    private String getStringParameterValueFor(String name) throws Exception {
        Object value = jobParams.get(name);
        if(value == null || value.toString().trim().isEmpty()) {
            throw new PluggableTaskException("Parameter " + name + " not specified");
        }
        return value.toString().trim();
    }

    private boolean getBoolParameterValueFor(String name, boolean defaultValue) throws Exception {
        Object value = jobParams.get(name);
        if(value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.toString().trim());
    }

    protected AbstractRemoteFileRetriever retriever() throws PluggableTaskException {
        switch (getFtpMethod()) {
            case FTP: return Context.getBean("ftpRemoteFileRetriever");
            case FTPS: return Context.getBean("ftpsRemoteFileRetriever");
            case SFTP: return Context.getBean("sftpRemoteFileRetriever");
        }
        return Context.getBean("ftpRemoteFileRetriever");
    }

    public String getServer() throws Exception {
        if(server != null && !server.trim().isEmpty()) {
            return server;
        }
        return getStringParameterValueFor(PARAM_SERVER);
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getServerPort() throws Exception {
        if(serverPort != null && !serverPort.trim().isEmpty()) {
            return serverPort;
        }
        return getStringParameterValueFor(PARAM_SERVER_PORT);
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public String getUser() throws Exception {
        if(user != null && !user.trim().isEmpty()) {
            return user;
        }
        return getStringParameterValueFor(PARAM_SERVER_USER);
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() throws Exception {
        if(password != null && !password.trim().isEmpty()) {
            return password;
        }
        return getStringParameterValueFor(PARAM_SERVER_PASSWORD);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public FtpMethod getFtpMethod() {
        try {
            ftpMethod = FtpMethod.valueOf(getStringParameterValueFor(PARAM_FTP_METHOD).toUpperCase().trim());
        } catch (Exception e) { }
        return ftpMethod;
    }

    public void setFtpMethod(FtpMethod ftpMethod) {
        this.ftpMethod = ftpMethod;
    }

    public void setFtpMethodString(String ftpMethodString) {
        try {
            ftpMethod = FtpMethod.valueOf(ftpMethodString.toUpperCase().trim());
        } catch (Exception e) {
            logger.warn("Unable to set FTP method of type %s", ftpMethodString);
        }
    }

    public String getRemotePath() throws Exception {
        if(remotePath != null && !remotePath.trim().isEmpty()) {
            return remotePath;
        }
        return getStringParameterValueFor(PARAM_REMOTE_PATH);
    }

    public String getSshKeyFile() {
        if(sshKeyFile != null && !sshKeyFile.trim().isEmpty()) {
            return sshKeyFile;
        }
        return getStringParameterValueFor(PARAM_SSH_KEY_FILE, null);
    }

    public void setSshKeyFile(String sshKeyFile) {
        this.sshKeyFile = sshKeyFile;
    }

    public String getSshKeyFilePassphrase() {
        if(sshKeyFilePassphrase != null && !sshKeyFilePassphrase.trim().isEmpty()) {
            return sshKeyFilePassphrase;
        }
        return getStringParameterValueFor(PARAM_SSH_KEY_FILE_PASSPHRASE, null);
    }

    public void setSshKeyFilePassphrase(String sshKeyFilePassphrase) {
        this.sshKeyFilePassphrase = sshKeyFilePassphrase;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getRemoteMoveToPath() throws Exception {
        if(remoteMoveToPath != null && !remoteMoveToPath.trim().isEmpty()) {
            return remoteMoveToPath;
        }
        return existParameter(PARAM_REMOTE_MOVE_TO_PATH) ? getStringParameterValueFor(PARAM_REMOTE_MOVE_TO_PATH) : null;
    }

    public void setRemoteMoveToPath(String remoteMoveToPath) {
        this.remoteMoveToPath = remoteMoveToPath;
    }

    public boolean isRemoteDelete() throws Exception {
        return getBoolParameterValueFor(PARAM_REMOTE_DELETE, remoteDelete);
    }

    public void setRemoteDelete(boolean remoteDelete) {
        this.remoteDelete = remoteDelete;
    }

    public boolean isOverwrite() throws Exception {
        return getBoolParameterValueFor(PARAM_OVERWRITE, overwrite);
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public String getLocalPath() throws Exception {
        if(localPath != null && !localPath.trim().isEmpty()) {
            return localPath;
        }
        return getStringParameterValueFor(PARAM_LOCAL_PATH);
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getSuffix() throws Exception {
        if(suffix != null && !suffix.trim().isEmpty()) {
            return suffix;
        }
        return existParameter(PARAM_SUFFIX) ? getStringParameterValueFor(PARAM_SUFFIX) : null;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getTempFolder() throws Exception {
        if(tempFolder != null && !tempFolder.trim().isEmpty()) {
            return tempFolder;
        }
        return getStringParameterValueFor(PARAM_TEMP_FOLDER);
    }

    public void setTempFolder(String tempFolder) {
        this.tempFolder = tempFolder;
    }

    public String getFileNameRegex() throws Exception {
        if(fileNameRegex != null && !fileNameRegex.trim().isEmpty()) {
            return fileNameRegex;
        }
        return existParameter(PARAM_FILE_NAME_REGEX) ? getStringParameterValueFor(PARAM_FILE_NAME_REGEX) : null;
    }

    public void setFileNameRegex(String fileNameRegex) {
        this.fileNameRegex = fileNameRegex;
    }

    public boolean isRecursive() throws Exception {
        return getBoolParameterValueFor(PARAM_RECURSIVE, recursive);
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public boolean isUncompress() throws Exception {
        return getBoolParameterValueFor(PARAM_UNCOMPRESS, uncompress);
    }

    public void setUncompress(boolean uncompress) {
        this.uncompress = uncompress;
    }
}
