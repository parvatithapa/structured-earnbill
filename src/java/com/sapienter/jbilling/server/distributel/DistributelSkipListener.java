package com.sapienter.jbilling.server.distributel;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.beans.factory.annotation.Value;

public class DistributelSkipListener implements SkipListener<DistributelPriceUpdateRequest, DistributelPriceUpdateRequest> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ERROR_FORMAT = "User %d price update failed because of Errors: [%s]";
    private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

    @Value("#{jobExecutionContext['error-file-path']}")
    private String errorFilePath;

    @Value("#{jobParameters['data_table_name']}")
    private String tableName;

    @Override
    public void onSkipInRead(Throwable ex) {
        logger.error("Failed Read!", ex);
    }

    @Override
    public void onSkipInWrite(DistributelPriceUpdateRequest request, Throwable ex) {
        logger.error("Failed Writing {} !", request, ex);
    }

    @Override
    public void onSkipInProcess(DistributelPriceUpdateRequest request, Throwable ex) {
        logger.error("Failed Processing {} !", request, ex);
        if(StringUtils.isNotEmpty(errorFilePath)) {
            try {
                String errorMessage = String.format(ERROR_FORMAT, request.getCustomerId(), ex.getLocalizedMessage()) + LINE_SEPARATOR;
                Files.write(Paths.get(errorFilePath), errorMessage.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                logger.error("writing Failed to path {}", errorFilePath, e);
            }
        }
        DistributelHelperUtil.updateRequestStatus(request, DistributelPriceJobConstants.REUQUEST_FAILED_STATUS, tableName);
    }

}
