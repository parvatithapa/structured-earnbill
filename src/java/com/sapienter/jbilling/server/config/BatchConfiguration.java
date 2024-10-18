package com.sapienter.jbilling.server.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sapienter.jbilling.batch.ageing.CollectionBatchService;
import com.sapienter.jbilling.batch.billing.BillingBatchJobService;
import com.sapienter.jbilling.batch.billing.BillingBatchService;
import com.sapienter.jbilling.batch.ignition.IgnitionBatchService;
import com.sapienter.jbilling.batch.support.NoOpWriter;
import com.sapienter.jbilling.batch.support.PartitionService;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration extends DefaultBatchConfigurer {

    @Autowired
    @Qualifier("dataSource")
    private DataSource dataSourceRef;

    @Override
    public void initialize () {
        setDataSource(dataSourceRef);
        super.initialize();
    }

    @Override
    protected JobRepository createJobRepository () throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSourceRef);
        factory.setTransactionManager(getTransactionManager());
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public BillingBatchJobService billingBatchJobService () {
        return new BillingBatchJobService();
    }

    @Bean
    public BillingBatchService billingBatchService () {
        return new BillingBatchService();
    }

    @Bean
    public CollectionBatchService collectionBatchService () {
        return new CollectionBatchService();
    }

    @Bean
    public IgnitionBatchService ignitionBatchService () {
        return new IgnitionBatchService();
    }

    @Bean
    public PartitionService partitionService () {
        return new PartitionService();
    }

    
    @Bean
    public NoOpWriter noopWriter () {
        return new NoOpWriter();
    }

    @Bean
    public SkipPolicy skipPolicy () {
        return new AlwaysSkipItemSkipPolicy();
    }
}
