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

package com.sapienter.jbilling.server.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Static factory for accessing remote Spring beans.
 */
public class RemoteContext {

    // spring application context for remote beans
    private static final ApplicationContext spring = new ClassPathXmlApplicationContext( new String[] {"/jbilling-remote-beans.xml"});

    // defined bean names
    public enum Name {
        API_CLIENT                  ("apiClient"),
        API_CLIENT_SOAP				("apiClient2"),
        API_CHILD_CLIENT			("apiClient4"),
        API_CLIENT_MORDOR			("apiClientMordor"),
        API_CLIENT_SYSADMIN			("apiClientSysAdmin"),

        API_CLIENT_OSCORP_ADMIN		("apiClientOscorpAdmin"),
        API_CLIENT_OSCORP_CUSTOMER	("apiClientOscorpCustomer"),
        API_CLIENT_FRENCH_SPEAKER   ("apiClientFrenchSpeaker"),
        API_CLIENT_PENDUNSUS1       ("apiClientPendunsus1"),

        API_CLIENT_O1_ADMIN			("apiClientO1Admin"),
        API_CLIENT_O1_CUSTOMER		("apiClientO1Customer"),

        API_CLIENT_O2_ADMIN			("apiClientO2Admin"),
        API_CLIENT_O2_CUSTOMER		("apiClientO2Customer"),

        API_CLIENT_CAPSULE_ADMIN	("apiClientCapsuleAdmin"),
        API_CLIENT_CAPSULE_CUSTOMER	("apiClientCapsuleCustomer"),

        API_CLIENT_C1_ADMIN			("apiClientC1Admin"),
        API_CLIENT_C1_CUSTOMER		("apiClientC1Customer"),

        API_CLIENT_DISTRIBUTEL      ("apiDistributelClient"),

        API_CLIENT_DEUTSCHE_TELECOM      ("apiDeutscheTelecomClient");


        private String name;

        Name(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // static factory cannot be instantiated
    private RemoteContext() {
    }

    public static ApplicationContext getApplicationContext() {
        return spring;
    }

    /**
     * Returns a Spring Bean of type T for the given RemoteContext.Name
     *
     * @param bean remote context name
     * @param <T> bean type
     * @return bean from remote context
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(Name bean) {
        return (T) getApplicationContext().getBean(bean.getName());
    }

    /**
     * Returns a Spring Bean of type T for the given name
     *
     * @param beanName bean name
     * @param <T> bean type
     * @return bean from remote context
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String beanName) {
        return (T)  getApplicationContext().getBean(beanName);
    }
}