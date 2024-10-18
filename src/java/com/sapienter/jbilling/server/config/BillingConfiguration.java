package com.sapienter.jbilling.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sapienter.jbilling.server.process.BillingProcessInfoBL;
import com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask;
import com.sapienter.jbilling.server.process.task.BasicUserFilterTask;

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

    @Bean
    public BasicUserFilterTask basicUserFilterTask() {
        return new BasicUserFilterTask();
    }
}
