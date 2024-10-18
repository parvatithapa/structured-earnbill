package com.sapienter.jbilling.server.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.ignition.ServiceProfile;

@Configuration
public class IgnitionConfiguration {

    @Bean
    @Scope(scopeName = "prototype")
    @Lazy
    Map<String, Map<String, ServiceProfile>> allServiceProfilesGroupedByBrand (int entityId) {
        return IgnitionUtility.getAllServiceProfilesGroupedByBrand(IgnitionConstants.DATA_TABLE_NAME, entityId);
    }
}
