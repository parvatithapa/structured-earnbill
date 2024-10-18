package com.sapienter.jbilling.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sapienter.jbilling.server.process.BillingProcessInfoBL;
import com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask;

@Configuration
public class BillingConfiguration {

    @Bean
    public BillingProcessInfoBL billingProcessInfoBL () {
        return new BillingProcessInfoBL();
    }

    @Bean
    public BasicBillingProcessFilterTask basicBillingProcessFilterTask () {
        return new BasicBillingProcessFilterTask();
    }
}
