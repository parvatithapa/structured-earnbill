package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.mediation.cache.BasicLoaderImpl;
import com.sapienter.jbilling.server.mediation.cache.ILoader;
import com.sapienter.jbilling.server.mediation.task.IMediationReader;
import com.sapienter.jbilling.server.mediation.task.StatelessJDBCReader;
import com.sapienter.jbilling.server.pricing.cache.RouteBasedRateCardFinder;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedSet;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class RouteRateCardBeanFactory {
    private final RouteRateCardDTO routeRateCardDTO;

    public RouteRateCardBeanFactory(RouteRateCardDTO routeRateCardDTO) {
        this.routeRateCardDTO = routeRateCardDTO;
    }

    public String getRouteRateCardUpdaterBeanName() {
        return toBeanName(routeRateCardDTO.getTableName()) + "Updater";
    }

    public ITableUpdater getRouteRateCardUpdaterInstance() {
        return getFromContextWithBeanRegisteringForCluster(getRouteRateCardUpdaterBeanName());
    }

    public AbstractBeanDefinition getTableDescriptorBeanDefinition() {
        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(BasicTableDescriptor.class);
        beanDef.setLazyInit(false);
        beanDef.setInitMethodName("init");

        beanDef.addPropertyReference("jdbcTemplate", Context.Name.JDBC_TEMPLATE.getName());
        beanDef.addPropertyValue("tableName", routeRateCardDTO.getTableName());

        return beanDef.getBeanDefinition();
    }

    public AbstractBeanDefinition getRouteRateCardUpdaterAggregateBeanDefinitions(Set<String> beanNames) {
        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(AggregateRouteUpdater.class);

        ManagedSet set = new ManagedSet();
        for(String beanName : beanNames) {
            set.add(new RuntimeBeanReference(beanName));
        }

        beanDef.addPropertyValue("updaters", set);
        return beanDef.getBeanDefinition();
    }

    public AbstractBeanDefinition getReaderBeanDefinition(Integer entityId) {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("table_name", routeRateCardDTO.getTableName());
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
        return toBeanName(routeRateCardDTO.getTableName()) + "Reader";
    }

    public IMediationReader getReaderInstance() {
        return getFromContextWithBeanRegisteringForCluster(getReaderBeanName());
    }

    public AbstractBeanDefinition getLoaderBeanDefinition(String readerBeanName) {
        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(BasicLoaderImpl.class);
        beanDef.setLazyInit(false);
        beanDef.setInitMethodName("init");
        beanDef.setDestroyMethodName("destroy");

        beanDef.addPropertyReference("jdbcTemplate", Context.Name.MEMCACHE_JDBC_TEMPLATE.getName());
        beanDef.addPropertyReference("transactionTemplate", Context.Name.MEMCACHE_TX_TEMPLATE.getName());
        beanDef.addPropertyReference("reader", readerBeanName);
        beanDef.addPropertyValue("tableName", routeRateCardDTO.getTableName());
        beanDef.addPropertyValue("indexName", routeRateCardDTO.getTableName() + "_idx");
        beanDef.addPropertyValue("indexColumnNames", StringUtils.join(new RouteBasedRateCardBL().getMatchingFieldColumns(routeRateCardDTO.getId()),','));


        return beanDef.getBeanDefinition();
    }
    public AbstractBeanDefinition getFinderBeanDefinition(String loaderBeanName) {
        BeanDefinitionBuilder beanDef = BeanDefinitionBuilder.rootBeanDefinition(RouteBasedRateCardFinder.class);
        beanDef.setLazyInit(true);
        beanDef.setInitMethodName("init");

        beanDef.addConstructorArgReference(Context.Name.JDBC_TEMPLATE.getName());
        if(loaderBeanName == null) {
            beanDef.addConstructorArgValue(null);
        } else {
            beanDef.addConstructorArgReference(loaderBeanName);
        }

        return beanDef.getBeanDefinition();
    }

    public Map<String, BeanDefinition> getRouteRateCardUpdaterDependentBeanDefinitions() {
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
            definitions.put(getRouteRateCardUpdaterBeanName() + "Bean" + i, beanDef.getBeanDefinition());
        }

        return definitions;
    }

    public String getLoaderBeanName() {
        return toBeanName(routeRateCardDTO.getTableName()) + "Loader";
    }

    public ILoader getLoaderInstance() {
        return getFromContextWithBeanRegisteringForCluster(getLoaderBeanName());
    }


    public String getFinderBeanName() {
        return toBeanName(routeRateCardDTO.getTableName()) + "Finder";
    }

    public RouteBasedRateCardFinder getFinderInstance() {
        return getFromContextWithBeanRegisteringForCluster(getFinderBeanName());
    }

    /**
     * This method try to retrieve route rate card associated beans from context. The first trial fall back to the register of spring beans for the route rate
     * card to recover from the possibility that you are requesting this to another instance in a jBilling cluster respect to the one where you created the route
     * rate card
     */
    private synchronized <T> T getFromContextWithBeanRegisteringForCluster(String beanName) {
        try {
            return Context.getBean(beanName);
        } catch (NoSuchBeanDefinitionException e) {
            new RouteBasedRateCardBL(routeRateCardDTO).registerSpringBeans();
        }
        return Context.getBean(beanName);
    }

    public String getTableDescriptorBeanName() {
        return toBeanName(routeRateCardDTO.getTableName()) + "Descriptor";
    }

    public ITableDescriptor getTableDescriptorInstance() {
        return getFromContextWithBeanRegisteringForCluster(getTableDescriptorBeanName());
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
