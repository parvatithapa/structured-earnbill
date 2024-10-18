package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrcopy;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.helper.MediationHelperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation.PARAMETER_MEDIATION_CONFIG_ID_KEY;
import static com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY;

public class JobInitListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    private MediationHelperService mediationHelperService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String workingFolder = mediationHelperService.getMediationCdrFolderByConfigId(new Integer(jobExecution.getJobParameters().getString(PARAMETER_MEDIATION_CONFIG_ID_KEY)));
        jobExecution.getExecutionContext().put("work_folder", workingFolder);

        String fileLocation = jobExecution.getJobParameters().getString(PARAMETER_MEDIATION_FILE_PATH_KEY);
        moveCurrentFileToWorkFolder(fileLocation, workingFolder);

        UUID mediationProcess = (UUID) jobExecution.getExecutionContext()
                .get(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY);

        String archivePath = createArchiveDir(workingFolder, mediationProcess);
        jobExecution.getExecutionContext().put("archive_folder", archivePath);
    }

    private void moveCurrentFileToWorkFolder(String fileLocation, String workingFolder) {
        // This is a work around. Ideally, the file should be copied to the work_folder when the
        // mediation is triggered from the UI. At present, it is copied to a temp directory.
        // Must be fixed in the mediation config flow.
        Path filePath;
        if (fileLocation != null && Files.exists(filePath = Paths.get(fileLocation))) {
            Path destinationDir = Paths.get(workingFolder + "/cdr/");
            try {
                if (!Files.exists(destinationDir)) {
                    Files.createDirectory(destinationDir);
                }
                Path moved = Files.move(filePath, Paths.get(destinationDir.toString(),
                        filePath.getFileName().toString()));
                logger.info("Moved file to work_folder: {}", moved.toString());
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage());
            }
        }
    }

    private String createArchiveDir(String workingFolder, UUID mediationProcess) {
        Path archivePath = Paths.get(workingFolder, "archive");
        try {
            Files.createDirectory(archivePath);  // checks first if it exists first
        } catch (FileAlreadyExistsException e) {
            logger.info("Directory already exists, no-ops");
        } catch (IOException e) {
            logger.error("Fatal: Archive folder could not be created", e);
        }

        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        Path dir = archivePath;
        dir = createDirectory(dir.toString(), today.toString()).orElse(dir);

        if (mediationProcess != null) {
            dir = createDirectory(dir.toString(), mediationProcess.toString()).orElse(dir);
        }

        return dir.toString();
    }

    private Optional<Path> createDirectory(String base, String... paths) {
        Path dir = Paths.get(base, paths);
        try {
            return Optional.of(Files.createDirectory(dir));

        } catch (FileAlreadyExistsException e) {
            logger.info("Directory already exists, no-ops");
            return Optional.of(dir);

        }catch (IOException e) {
            logger.error("Error creating processed folder for today", e);
            return Optional.empty();
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String archive = (String) jobExecution.getExecutionContext().get("archive_folder");
        executor.execute(() -> Util.gzip(archive));
    }
}
