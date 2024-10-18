package com.sapienter.jbilling.fc;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
public class FullCreativeTestConfig {

    @Autowired
    private Environment environment;

    @Bean(name = "fullCreativeDataSource")
    public DataSource dataSource() throws PropertyVetoException {

        String dbUser = environment.getProperty("JBILLING_DB_USER") !=null ?
                    environment.getProperty("JBILLING_DB_USER") : "jbilling";

        String dbName = environment.getProperty("JBILLING_DB_NAME")!=null ?
                    environment.getProperty("JBILLING_DB_NAME") : "jbilling_test";

        String dbHost = environment.getProperty("JBILLING_DB_HOST")!=null ?
                    environment.getProperty("JBILLING_DB_HOST") : "localhost";

        String dbPort = environment.getProperty("JBILLING_DB_PORT")!=null ?
                    environment.getProperty("JBILLING_DB_PORT") : "5432";

        // N.B. db params string should starts with symbol "?"
        String dbParams   = environment.getProperty("JBILLING_DB_PARAMS")!=null ?
                    environment.getProperty("JBILLING_DB_PARAMS") : "";

        String dbPassword = environment.getProperty("JBILLING_DB_PASSWORD")!=null ?
                    environment.getProperty("JBILLING_DB_PASSWORD"): "";

        String url = String.format("jdbc:postgresql://%s:%s/%s%s", dbHost, dbPort, dbName, dbParams);

        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass("org.postgresql.Driver");
        dataSource.setJdbcUrl(url);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPassword);
        dataSource.setInitialPoolSize(3);
        dataSource.setMaxPoolSize(10);
        dataSource.setMinPoolSize(1);
        dataSource.setAcquireIncrement(3);
        dataSource.setMaxIdleTime(300);
        dataSource.setCheckoutTimeout(10000);
        dataSource.setTestConnectionOnCheckout(false);
        dataSource.setIdleConnectionTestPeriod(20);
        return dataSource;
    }

    @Bean(name = "fullCreativeJdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("fullCreativeDataSource") DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.afterPropertiesSet();
        return template;
    }

}
