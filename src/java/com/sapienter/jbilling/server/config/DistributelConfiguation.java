package com.sapienter.jbilling.server.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.distributel.DistributelPriceHelperService;
import com.sapienter.jbilling.server.distributel.DistributelPriceJobConstants;
import com.sapienter.jbilling.server.distributel.DistributelPriceRowMapper;
import com.sapienter.jbilling.server.distributel.DistributelPriceUpdateRequest;

@Configuration
public class DistributelConfiguation {

    @Bean
    @Scope(scopeName = "step")
    ItemReader<DistributelPriceUpdateRequest> priceUpdateReversalReader(@Qualifier("dataSource") DataSource dataSource,
            @Value("#{jobParameters}") Map<String, JobParameter> jobParameters,
            @Value("#{stepExecution.stepName}") String stepName,
            DistributelPriceRowMapper distributelPriceRowMapper) {
        JdbcPagingItemReader<DistributelPriceUpdateRequest> databaseReader = new JdbcPagingItemReader<>();
        databaseReader.setDataSource(dataSource);
        databaseReader.setRowMapper(distributelPriceRowMapper);
        databaseReader.setSaveState(false);
        databaseReader.setFetchSize(1000);
        databaseReader.setPageSize(1000);
        PagingQueryProvider queryProvider = createQueryProvider(jobParameters, stepName);
        databaseReader.setQueryProvider(queryProvider);
        List<String> dates = Arrays.asList((String) jobParameters.get(
                DistributelPriceJobConstants.PARAM_PROCESSING_DATE_NAME).getValue());
        databaseReader.setParameterValues(Collections.singletonMap("dates", dates));
        return databaseReader;
    }

    private PagingQueryProvider createQueryProvider(Map<String, JobParameter> jobParameters, String stepName) {
        PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
        JobParameters jobParams = new JobParameters(jobParameters);
        String tableName = null;
        String where = null;
        if (DistributelPriceJobConstants.PRICE_UPDATE_INCREASE_STEP_NAME.equals(stepName)) {
            tableName = jobParams.getString(DistributelPriceJobConstants.PARAM_PRICE_INCREASE_DATA_TABLE_NAME);
            where = "WHERE scheduled_date_for_adjustment IN (:dates)";
        } else if (DistributelPriceJobConstants.PRICE_UPDATE_REVERSE_STEP_NAME.equals(stepName)) {
            tableName = jobParams.getString(DistributelPriceJobConstants.PARAM_PRICE_REVERSAL_DATA_TABLE_NAME);
            where = "WHERE scheduled_date_for_reversal IN (:dates)";
        }
        Assert.notNull(tableName, "tableName may not be null");
        Assert.notNull(where, "whereClause may not be null");
        queryProvider.setSelectClause("SELECT *");
        queryProvider.setFromClause("FROM " + tableName);
        queryProvider.setWhereClause(where);
        queryProvider.setSortKeys(Collections.singletonMap("id", Order.ASCENDING));
        return queryProvider;
    }

    @Bean
    DistributelPriceHelperService distributelPriceService() {
        return new DistributelPriceHelperService();
    }
}
