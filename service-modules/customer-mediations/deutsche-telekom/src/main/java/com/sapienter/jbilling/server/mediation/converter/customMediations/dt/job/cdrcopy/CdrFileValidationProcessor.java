package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrcopy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.invoke.MethodHandles;

public class CdrFileValidationProcessor implements ItemProcessor<File, CombinedCdrProcessResult> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public CombinedCdrProcessResult process(File csvFile) throws Exception {
        CombinedCdrProcessResult result = new CombinedCdrProcessResult(csvFile);
        try(BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line = reader.readLine().trim();
            if(!line.startsWith("10")) {
                result.addError(CombinedCdrProcessResult.ERROR_NO_HEADER);
                result.invalid();
            } else if(!line.endsWith("text")) {
                result.addError(CombinedCdrProcessResult.ERROR_NOT_TEXT_TYPE);
                result.invalid();
            }
        }
        logger.debug("Processing {}, result {}", csvFile, result.getErrors());
        return result;
    }

}
