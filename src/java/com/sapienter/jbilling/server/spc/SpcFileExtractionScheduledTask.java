package com.sapienter.jbilling.server.spc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.IMethodTransactionalWrapper;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.mediation.db.MediationConfigurationDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;

public class SpcFileExtractionScheduledTask extends AbstractCronTask {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAM_SPC_MEDIATION_COMPRESEED_DIR_PATH =
            new ParameterDescription("spc_mediation_compressed_dir_path", true, ParameterDescription.Type.STR);

    private static final ParameterDescription PARAM_SPC_MEDIATION_CONFIG_ID =
            new ParameterDescription("spc_mediation_config_id", true, ParameterDescription.Type.INT);

    private static final ParameterDescription PARAM_SPC_MUR_MEDIATION_CONFIG_ID =
            new ParameterDescription("spc_mur_mediation_config_id", false, ParameterDescription.Type.INT);

    private static final List<String> SUPPORTED_COMPRESSED_FORMAT =
            Arrays.asList(ArchiveStreamFactory.TAR, ArchiveStreamFactory.ZIP);

    public SpcFileExtractionScheduledTask() {
        descriptions.add(PARAM_SPC_MEDIATION_COMPRESEED_DIR_PATH);
        descriptions.add(PARAM_SPC_MEDIATION_CONFIG_ID);
        descriptions.add(PARAM_SPC_MUR_MEDIATION_CONFIG_ID);
    }

    private String getConfigInputDirPath(Integer configId) {
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        return txAction.execute(()-> {
            MediationConfigurationDAS das = new MediationConfigurationDAS();
            MediationConfiguration config = das.findNow(configId);
            if(null!= config && StringUtils.isNotEmpty(config.getLocalInputDirectory())) {
                return config.getLocalInputDirectory();
            }
            return StringUtils.EMPTY;
        });
    }

    private boolean isConfigPresentForEntity(Integer configId, Integer entityId) {
        IMethodTransactionalWrapper txAction = Context.getBean(IMethodTransactionalWrapper.class);
        return txAction.execute(()-> {
            MediationConfigurationDAS das = new MediationConfigurationDAS();
            MediationConfiguration config = das.findNow(configId);
            if(null == config) {
                logger.debug("config id {} not found for entity {}", configId, entityId);
                return false;
            }
            if(!config.getEntityId().equals(entityId)) {
                logger.debug("no mediation config {} present for entity {}", configId, entityId);
                return false;
            }
            return true;
        });
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            _init(context);
            Integer entityId = getEntityId();
            Integer spcMediationConfigId = getParameter(PARAM_SPC_MEDIATION_CONFIG_ID.getName(), -1);
            if(spcMediationConfigId.intValue() == -1) {
                logger.debug("{} param not configured for entity {}", PARAM_SPC_MEDIATION_CONFIG_ID.getName(), entityId);
                return;
            }
            if(!isConfigPresentForEntity(spcMediationConfigId, entityId)) {
                return;
            }
            String compressedDirPath = getParameter(PARAM_SPC_MEDIATION_COMPRESEED_DIR_PATH.getName(), StringUtils.EMPTY);
            if(StringUtils.isEmpty(compressedDirPath)) {
                logger.debug("{} param not found for entity {}", PARAM_SPC_MEDIATION_COMPRESEED_DIR_PATH.getName(), entityId);
                return;
            }
            File compressedDir = new File(compressedDirPath);
            if(!compressedDir.exists()) {
                logger.debug("invalid {} provided for entity {}", compressedDirPath, entityId);
                return;
            }
            String destinationpath = getConfigInputDirPath(spcMediationConfigId);

            if(StringUtils.isEmpty(destinationpath)) {
                logger.debug("{}  not found in config {} for entity {}", "destinationpath", spcMediationConfigId, entityId);
                return;
            }
            File desitnationDir = new File(destinationpath);
            if(!desitnationDir.exists()) {
                logger.debug("invalid {} provided in config {} for entity {}", desitnationDir, spcMediationConfigId, entityId);
                return;
            }
            for(File file : compressedDir.listFiles(file -> file.isFile() && !file.getName().contains("MUR"))) {
                extractFiles(file, compressedDir, desitnationDir);
            }

            Integer spcmurMediationConfigId = getParameter(PARAM_SPC_MUR_MEDIATION_CONFIG_ID.getName(), -1);
            if(spcmurMediationConfigId.intValue() == -1) {
                logger.debug("{} param not configured for entity {}", PARAM_SPC_MUR_MEDIATION_CONFIG_ID.getName(), entityId);
                return;
            }
            if(!isConfigPresentForEntity(spcmurMediationConfigId, entityId)) {
                return;
            }

            String murDesitnationPath = getConfigInputDirPath(spcmurMediationConfigId);

            if(StringUtils.isEmpty(murDesitnationPath)) {
                logger.debug("{}  not found for config {} for entity {}", "murDesitnationPath", spcmurMediationConfigId, entityId);
                return;
            }
            File murDestinationDir = new File(murDesitnationPath);
            if(!murDestinationDir.exists()) {
                logger.debug("invalid {} provided in config {} for entity {}", murDestinationDir, spcmurMediationConfigId, entityId);
                return;
            }
            for(File file : compressedDir.listFiles(file -> file.isFile() && file.getName().contains("MUR"))) {
                extractFiles(file, compressedDir, murDestinationDir);
            }

        } catch(Exception ex) {
            logger.error("error during decomprassion for entity {}", getEntityId(), ex);
        }
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    /**
     * extract files from given compressed file to given destination directory
     * @param file
     * @param compressedDir
     * @param destinationDir
     * @throws IOException
     * @throws ArchiveException
     */
    private void extractFiles(File file, File compressedDir, File destinationDir) throws IOException, ArchiveException {
        if (isTextFile(file)) {
            logger.debug("file {} is text", file.getName());
            FileUtils.copyFileToDirectory(file, destinationDir, true);
            FileUtils.moveFileToDirectory(file,
                    new File(compressedDir.getAbsolutePath() + File.separator + "Done"), true);
            logger.debug("moved file {} to dir {} for entity {}", file.getName(), destinationDir.getName(), getEntityId());
            return;
        }
        if(isgZipFile(file)) {
            logger.debug("file {} is gzip", file.getName());
            deCompressGzipFile(file,
                    new File(destinationDir, FilenameUtils.removeExtension(FilenameUtils.getName(file.getName()))));
            FileUtils.moveFileToDirectory(file,
                    new File(compressedDir.getAbsolutePath() + File.separator + "Done"), true);
            logger.debug("moved file {} to dir {} for entity {}", file.getName(), destinationDir.getName(), getEntityId());
            return;
        }
        String type = getCompressedFileType(file);
        logger.debug("file {} compression type is {}", file.getName(), type);
        if (!isSupportedFormat(type)) {
            logger.debug("file {} compression type is not supported {}", file.getName(), type);
            return;
        }

        try (InputStream is = new FileInputStream(file)) {
            try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(type, is)) {
                ArchiveEntry entry = null;
                while ((entry = ais.getNextEntry()) != null) {
                    if (!ais.canReadEntryData(entry)) {
                        continue;
                    }
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String fileName = FilenameUtils.getName(entry.getName());
                    if ((fileName.startsWith("Reseller") || fileName.startsWith("RESELLER"))
                            && !fileName.contains("UDR")) {
                        logger.debug("skipping {} since only udr file needs to extract", fileName);
                        continue;
                    }
                    byte[] content = IOUtils.toByteArray(ais);
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
                    if (isTextFile(content)) {
                        File destinationFile = new File(destinationDir, fileName);
                        logger.debug("copying file {} to dir {}", fileName, destinationFile);
                        FileUtils.copyInputStreamToFile(inputStream, destinationFile);
                    } else {
                        String fileType = getCompressedFileType(content);
                        File compressedFile = new File(compressedDir, fileName);
                        FileUtils.copyInputStreamToFile(inputStream, compressedFile);
                        if (isSupportedFormat(fileType)) {
                            extractFiles(compressedFile, compressedDir, destinationDir);
                        } else if (fileType.contains("gzip")) {
                            deCompressGzipFile(compressedFile,
                                    new File(destinationDir, FilenameUtils.removeExtension(fileName)));
                        } else {
                            logger.debug("unsupported file {} found in compressed file {}", fileName, file.getName());
                        }
                        FileUtils.deleteQuietly(compressedFile);
                    }

                }
            }
        }
        logger.debug("moving file {} to done dir ", file.getName());
        FileUtils.moveFileToDirectory(file, new File(compressedDir.getAbsolutePath() + File.separator + "Done"), true);
    }

    private static void deCompressGzipFile(File compressedFile, File decompressedFile) throws IOException {
        try (FileInputStream fileIn = new FileInputStream(compressedFile);
                GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn)) {
            FileUtils.copyInputStreamToFile(gZIPInputStream, decompressedFile);
        }
    }

    private boolean isTextFile(File file) throws IOException {
        Tika tika = Context.getBean(Tika.class);
        return tika.detect(file).contains("text");
    }

    private boolean isTextFile(byte[] data) {
        Tika tika = Context.getBean(Tika.class);
        return tika.detect(data).contains("text");
    }

    private boolean isgZipFile(File file) throws IOException {
        Tika tika = Context.getBean(Tika.class);
        return tika.detect(file).contains("gzip");
    }

    private String getCompressedFileType(File file) throws IOException {
        Tika tika = Context.getBean(Tika.class);
        return extractType(tika.detect(file));
    }

    private String extractType(String fileType) {
        fileType = fileType.substring(fileType.lastIndexOf('/') + 1);
        return fileType.substring(fileType.lastIndexOf('-') + 1);
    }

    private String getCompressedFileType(byte[] data) {
        Tika tika = Context.getBean(Tika.class);
        return extractType(tika.detect(data));
    }

    private boolean isSupportedFormat(String format) {
        for (String type : SUPPORTED_COMPRESSED_FORMAT) {
            if (type.equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }

}
