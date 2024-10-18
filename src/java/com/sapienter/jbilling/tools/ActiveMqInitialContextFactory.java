package com.sapienter.jbilling.tools;

import net.sf.ehcache.distribution.jms.JMSCacheManagerPeerProviderFactory;
import net.sf.ehcache.distribution.jms.JMSUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jndi.ActiveMQInitialContextFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by marcolin on 29/06/16.
 */
public class ActiveMqInitialContextFactory  extends ActiveMQInitialContextFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Context getInitialContext(Hashtable environment)
            throws NamingException
    {
        setActiveMQUrlForEhCache(environment);

        Map<String, Object> data = new ConcurrentHashMap<>();
        String factoryBindingName = (String) environment.get(JMSUtil.TOPIC_CONNECTION_FACTORY_BINDING_NAME);
        String topicBindingName = (String) environment.get(JMSUtil.REPLICATION_TOPIC_BINDING_NAME);
        String queueBindingName = (String) environment.get(JMSUtil.GET_QUEUE_BINDING_NAME);
        String queueFactoryBindingName = (String) environment.get(JMSUtil.GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME);
        try {
            ActiveMQConnectionFactory connectionFactory = createConnectionFactory(environment);
            connectionFactory.setTrustAllPackages(Boolean.TRUE);
            data.put(factoryBindingName, connectionFactory);
            data.put(queueFactoryBindingName, connectionFactory);
            data.put(topicBindingName, createTopic(topicBindingName));
            data.put(queueBindingName, createQueue(queueBindingName));
        } catch (URISyntaxException e) {
            throw new NamingException("Error initialization ConnectionFactory with message "
                    + e.getMessage());
        }
        return createContext(environment, data);
    }

    //This is done because is not possible to set in the <cacheManagerPeerProviderFactory>
    // a System environment variable value
    private void setActiveMQUrlForEhCache(Hashtable environment) {
        String jbillingActiveMqBrokerUrl = System.getenv("JBILLING_ACTIVE_MQ_BROKER_URL");
        if (jbillingActiveMqBrokerUrl != null) {
            environment.put("java.naming.provider.url", jbillingActiveMqBrokerUrl);
        }
    }

}