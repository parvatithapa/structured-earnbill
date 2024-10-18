package com.sapienter.jbilling.server.config;

import com.sapienter.jbilling.le.support.ReportDataProviderLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NetRevenueReportProvider {

    private static final String JAR_FILE_NAME = "jbilling-le-1.0.20210119-RELEASE-runtime.jar";

    private String jarResourcePath () {
        String resourceFileName = NetRevenueReportProvider.class.getClassLoader().getResource(JAR_FILE_NAME + ".enc").getFile();
        return resourceFileName.substring(0, resourceFileName.length() - 4); // strip ".enc" suffix
    }

    @Bean
    public ReportDataProviderLoader reportDataProviderLoader () {
        return new ReportDataProviderLoader(jarResourcePath());
    }

}
