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

package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.mediation.cache.BasicLoaderImpl;
import com.sapienter.jbilling.server.mediation.cache.ILoader;
import com.sapienter.jbilling.server.mediation.task.IMediationReader;
import com.sapienter.jbilling.server.mediation.task.StatelessJDBCReader;
import com.sapienter.jbilling.server.pricing.cache.RouteFinder;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.StringUtils;
import org.hsqldb.util.DatabaseManager;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedSet;

import java.util.*;

/**
 * RouteBeanFactory
 *
 * @author Rahul Asthana
 */
public class RouteBeanFactory {

    private final RouteDTO routeDTO;

    static boolean check = false;
    public RouteBeanFactory(RouteDTO routeDTO) {
        this.routeDTO = routeDTO;
    }

    public AbstractBeanDefinition getReaderBeanDefinition(Integer entityId) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("table_name", routeDTO.getTableName());
        parameters.put("key_column_name", "id");
        parameters.put("batch_size", String.valueOf(RouteBL.BATCH_SIZE));

        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(StatelessJDBCReader.class);
        beanDef.setLazyInit(true);

        beanDef.addPropertyReference("jdbcTemplate", Context.Name.JDBC_TEMPLATE.getName());
        beanDef.addPropertyReference("dataSource", Context.Name.DATA_SOURCE.getName());
        beanDef.addPropertyValue("parameters", parameters);
        beanDef.addPropertyValue("entityId", entityId);

        return beanDef.getBeanDefinition();
    }
    public String getReaderBeanName() {
        return toBeanName(routeDTO.getTableName()) + "Reader";
    }

    public IMediationReader getReaderInstance() {
        return Context.getBean(getReaderBeanName());
    }

    public AbstractBeanDefinition getLoaderBeanDefinition(String readerBeanName) {
        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(BasicLoaderImpl.class);
        beanDef.setLazyInit(false);
        beanDef.setInitMethodName("init");
        beanDef.setDestroyMethodName("destroy");

        beanDef.addPropertyReference("jdbcTemplate", Context.Name.MEMCACHE_JDBC_TEMPLATE.getName());
        beanDef.addPropertyReference("transactionTemplate", Context.Name.MEMCACHE_TX_TEMPLATE.getName());
        beanDef.addPropertyReference("reader", readerBeanName);
        beanDef.addPropertyValue("tableName", routeDTO.getTableName());
        beanDef.addPropertyValue("indexName", routeDTO.getTableName() + "_idx");
        beanDef.addPropertyValue("indexColumnNames", StringUtils.join(new RouteBL().getMatchingFieldColumns(routeDTO.getId()),','));

        return beanDef.getBeanDefinition();
    }
    public AbstractBeanDefinition getFinderBeanDefinition(String loaderBeanName) {
        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(RouteFinder.class);
        beanDef.setLazyInit(true);
        beanDef.setInitMethodName("init");

        beanDef.addConstructorArgReference(Context.Name.MEMCACHE_JDBC_TEMPLATE.getName());
        if(loaderBeanName == null) {
            beanDef.addConstructorArgValue(null);
        } else {
            beanDef.addConstructorArgReference(loaderBeanName);
        }

        return beanDef.getBeanDefinition();
    }

    public String getLoaderBeanName() {
        return toBeanName(routeDTO.getTableName()) + "Loader";
    }

    public ILoader getLoaderInstance() {
        return Context.getBean(getLoaderBeanName());
    }


    public String getFinderBeanName() {
        return toBeanName(routeDTO.getTableName()) + "Finder";
    }

    public RouteFinder getFinderInstance() {
        return Context.getBean(getFinderBeanName());
    }

    /**
     * Gives the definition of a route table.
     *
     * @return
     */
    public AbstractBeanDefinition getTableDescriptorBeanDefinition() {
        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(BasicTableDescriptor.class);
        beanDef.setLazyInit(false);
        beanDef.setInitMethodName("init");

        beanDef.addPropertyReference("jdbcTemplate", Context.Name.JDBC_TEMPLATE.getName());
        beanDef.addPropertyValue("tableName", routeDTO.getTableName());

        return beanDef.getBeanDefinition();
    }

    public String getTableDescriptorBeanName() {
        return toBeanName(routeDTO.getTableName()) + "Descriptor";
    }

    public ITableDescriptor getTableDescriptorInstance() {
        return Context.getBean(getTableDescriptorBeanName());
    }

    /**
     * We need to update the HQL cache entry and the persistent entry. So the Aggregator will delegate the
     * update to 2 different implementations who's definitions are created in getRouteUpdaterDependentBeanDefinitions
     * @param beanNames
     * @return
     */
    public AbstractBeanDefinition getRouteUpdaterAggregateBeanDefinitions(Set<String> beanNames) {
        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(AggregateRouteUpdater.class);

        ManagedSet set = new ManagedSet();
        for(String beanName : beanNames) {
            set.add(new RuntimeBeanReference(beanName));
        }

        beanDef.addPropertyValue("updaters", set);
        return beanDef.getBeanDefinition();
    }

    /**
     * Bean definitions of updaters for HQL cache and the DB.
     *
     * @return
     */
    public Map<String, BeanDefinition> getRouteUpdaterDependentBeanDefinitions() {
        Map<String, BeanDefinition> definitions = new LinkedHashMap<String, BeanDefinition>(4);

        for( int i=0; i<2; i++) {
            BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(BasicRouteUpdater.class);
            beanDef.addPropertyReference("tableDescriptor", getTableDescriptorBeanName());

            if(i == 0) {
                beanDef.addPropertyReference("jdbcTemplate", Context.Name.JDBC_TEMPLATE.getName());
            } else {
                beanDef.addPropertyReference("jdbcTemplate", Context.Name.MEMCACHE_JDBC_TEMPLATE.getName());
                beanDef.addPropertyReference("transactionTemplate", Context.Name.MEMCACHE_TX_TEMPLATE.getName());
                //the table must be loaded before it can be updated
                beanDef.addDependsOn(getLoaderBeanName());
            }

            definitions.put(getRouteUpdaterBeanName() + "Bean" + i, beanDef.getBeanDefinition());
        }

        return definitions;
    }

    public String getRouteUpdaterBeanName() {
        return toBeanName(routeDTO.getTableName()) + "Updater";
    }

    public ITableUpdater getRouteUpdaterInstance() {
        return Context.getBean(getRouteUpdaterBeanName());
    }

    /**
     * Converts a rate card table name to a camelCase bean name to use registering
     * rating spring beans.
     *
     * @param tableName rate card table name
     */
    private static String toBeanName(String tableName) {
        StringBuilder builder = new StringBuilder();

        String[] tokens = tableName.split("_");
        for (int i = 0; i < tokens.length; i++) {
            if (i == 0) {
                builder.append(tokens[i]);
            } else {
                builder.append(StringUtils.capitalize(tokens[i]));
            }
        }

        return builder.toString();
    }
}
