package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DistributelPriceReader implements ItemReader<DistributelPriceUpdateRequest>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String SQL = "SELECT * FROM %s WHERE scheduled_date_for_adjustment IN (:dates)";

    @Value("#{stepExecution.jobExecutionId}")
    private Long jobExecutionId;

    @Value("#{stepExecution.stepName}")
    private String stepName;

    @Value("#{jobParameters['processing_date']}")
    private String processingDate;

    @Value("#{jobParameters['future_processing_date']}")
    private String futureProcessingDate;

    @Value("#{jobParameters['data_table_name']}")
    private String tableName;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private DistributelPriceRowMapper distributelPriceRowMapper;

    private List<DistributelPriceUpdateRequest> records;

    @Override
    public synchronized DistributelPriceUpdateRequest read() throws Exception {
        if (CollectionUtils.isNotEmpty(records)) {
            DistributelPriceUpdateRequest request = records.remove(0);
            logger.trace("Updating price {} for User ID: {} , order id {}, product id {} for entity {}",
                    request.getNewOrderLinePrice(), request.getCustomerId(), request.getOrderId(),
                    request.getProductId(), entityId);
            return request;
        }
        return null;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        logger.debug("stepName: {}, jobExecutionId: {}", stepName, jobExecutionId);
        logger.debug("Reader uses data table {}", tableName);
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        if (DistributelPriceJobConstants.PRICE_UPDATE_STEP_NAME.equals(stepName)) {
            parameters.addValue("dates", Arrays.asList(processingDate));
        }

        if (DistributelPriceJobConstants.USER_NOTE_CREATE_STEP_NAME.equals(stepName)) {
            parameters.addValue("dates", Arrays.asList(futureProcessingDate.split(",")));
        }

        records = namedParameterJdbcTemplate
                .query(String.format(SQL, tableName), parameters, distributelPriceRowMapper);
        logger.debug("fetch records {} for entity {}", records, entityId);
    }
}
