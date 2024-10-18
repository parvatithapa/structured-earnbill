/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.ratecards;

import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class Bug7935TestConfig {

    @Bean
    public DataSource dataSource() {
        SingleConnectionDataSource bean = new SingleConnectionDataSource();

        // This if for Jenkins to use a dynamic declaration of the DB name and user.
        String dbUser = System.getenv("JBILLING_DB_USER");
        String dbName = System.getenv("JBILLING_DB_NAME");

        bean.setUrl("jdbc:postgresql://localhost:5432/" + ((dbName == null || dbName.isEmpty()) ? "jbilling" : dbName));
        bean.setDriverClassName("org.postgresql.Driver");
        bean.setUsername((dbUser == null || dbUser.isEmpty()) ? "jbilling" : dbUser);
        bean.setPassword((dbUser == null || dbUser.isEmpty()) ? "jbilling" : "");
        bean.setSuppressClose(true);

        return bean;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Value("file:grails-app/conf/hibernate/hibernate.cfg.xml")
    private Resource hibernateCfgXmlFile;

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
        sessionFactory.setHibernateProperties(hibernateProperties());

        sessionFactory.setConfigLocation(hibernateCfgXmlFile);

        return sessionFactory;
    }

    @Bean
    @Autowired
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

    @Bean
    @Lazy
    public SingleConnectionDataSource memcacheDataSource() {
        SingleConnectionDataSource bean = new SingleConnectionDataSource();
        bean.setUrl("jdbc:hsqldb:mem:cacheDB");
        bean.setDriverClassName("org.hsqldb.jdbcDriver");
        bean.setUsername("sa");
        bean.setPassword("");

        return bean;
    }

    @Bean
    public DataSourceTransactionManager memcacheTransactionManager() {
        return new DataSourceTransactionManager(memcacheDataSource());
    }

    @Bean
    public TransactionTemplate memcacheTransactionTemplate() {
        return new TransactionTemplate(memcacheTransactionManager());
    }

    @Bean
    public JdbcTemplate memcacheJdbcTemplate() {
        return new JdbcTemplate(memcacheDataSource());
    }

    Properties hibernateProperties() {
        return new Properties() {
            {
                setProperty("hibernate.cache.provider_configuration_file_resource_path", "classpath:ehcache-hibernate.xml");
                setProperty("hibernate.cache.region.factory_cla­ss", "org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory");
                setProperty("hibernate.cache.use_query_cache", "false");

                setProperty("hibernate.connection.characterEncoding", "UTF-8");
                setProperty("hibernate.connection.release_mode", "after_statement");

                setProperty("hibernate.default_batch_fetch_size", "10");
                setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
                setProperty("hibernate.jdbc.batch_size", "100");
                setProperty("hibernate.use_sql_comments", "true");

                setProperty("hibernate.current_session_context_class", "thread");
            }
        };
    }
}
