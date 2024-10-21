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
import org.springframework.context.ApplicationContextAware;

/**
 * Static factory for accessing Spring beans from the local container.
 */
public class Context {

    public static class InitializerBean implements ApplicationContextAware {

        @Override
        public void setApplicationContext (ApplicationContext ctx) {
            Context.setApplicationContext(ctx);
        }
    }

    // spring application context, injected by context aware clients
    private static ApplicationContext spring = null;

    // defined bean names
    public enum Name {
        // jbilling session beans
        ITEM_SESSION                    		("itemSession"),
        NOTIFICATION_SESSION            		("notificationSession"),
        CUSTOMER_SESSION                		("customerSession"),
        LIST_SESSION                    		("listSession"),
        USER_SESSION                    		("userSession"),
        INVOICE_SESSION                 		("invoiceSession"),
        ORDER_SESSION                   		("orderSession"),
        PLUGGABLE_TASK_SESSION          		("pluggableTaskSession"),
        PAYMENT_SESSION                 		("paymentSession"),
        MEDIATION_SESSION               		("mediationSession"),
        BILLING_PROCESS_SESSION         		("billingProcessSession"),
        PROVISIONING_PROCESS_SESSION    		("provisioningProcessSession"),
        CUSTOMER_USAGE_POOL_EVALUATION_SESSION	("customerUsagePoolEvaluationSession"),
        WEB_SERVICES_SESSION 					("webServicesSession"),
        ROUTE_SERVICE 					        ("routeService"),
        EDI_TRANSACTION_SESSION 				("ediTransactionSession"),
        PLUGINS_SESSION            		        ("pluginsSession"),
        JOB_EXECUTION_SESSION            		("jobExecutionSession"),

        // jbilling data access service beans
        DESCRIPTION_DAS     ("internationalDescriptionDAS"),
        JBILLING_TABLE_DAS  ("jbillingTableDAS"),
        PLUGGABLE_TASK_DAS  ("pluggableTaskDAS"),

        // jbilling beans
        PROVISIONING                        ("provisioning"),
        INTERNAL_EVENTS_RULES_TASK_CONFIG   ("internalEventsRulesTaskConfig"),

        // persistence
        DATA_SOURCE         ("dataSource"),
        DATA_SOURCE_UMS     ("dataSourceUms"),
        TRANSACTION_MANAGER ("transactionManager"),
        HIBERNATE_SESSION   ("sessionFactory"),
        JDBC_TEMPLATE       ("jdbcTemplate"),

        //batch
        BATCH_ASYNC_JOB_LAUNCHER    ("asyncJobLauncher"),
        BATCH_SYNC_JOB_LAUNCHER     ("jobLauncher"),

        BATCH_ASSET_LOAD_JOB            ("assetLoadJob"),
        BATCH_JOB_GENERATE_INVOICES     ("generateInvoicesJob"),
        BATCH_JOB_AGEING_PROCESS        ("ageingProcessJob"),
        BATCH_JOB_BAI_FILE_PROCESSING   ("bankJob"),

        // security
        SPRING_SECURITY_SERVICE ("springSecurityService"),
        AUTHENTICATION_MANAGER  ("authenticationManager"),
        PASSWORD_ENCODER        ("passwordEncoder"),
        PASSWORD_SERVICE        ("passwordService"),

        // cache
        CACHE                           ("cacheProviderFacade"),
        CACHE_MODEL_READONLY            ("cacheModelReadOnly"),
        CACHE_MODEL_RW                  ("cacheModelPTDTO"),
        CACHE_FLUSH_MODEL_RW            ("flushModelPTDTO"),
        CACHE_PRICE_MODEL_TASK          ("priceModelMediationCache"),

        CURRENCY_CACHE_MODEL            ("cacheModelCurrency"),
        CURRENCY_FLUSH_MODEL            ("flushModelCurrency"),

        PREFERENCE_CACHE_MODEL			("cacheModelPreference"),
        PREFERENCE_FLUSH_MODEL			("flushModelPreference"),

        // HSQLDB data loader cache
        MEMCACHE_DATASOURCE                 ("memcacheDataSource"),
        MEMCACHE_JDBC_TEMPLATE              ("memcacheJdbcTemplate"),
        MEMCACHE_TX_TEMPLATE                ("memcacheTransactionTemplate"),
        PRICING_FINDER                      ("pricingFinder"),
        NANPA_CALL_IDENTIFICATION_FINDER    ("callIdentificationFinder"),

        // jms
        JMS_TEMPLATE                            ("jmsTemplate"),
        PROCESSORS_DESTINATION                  ("processorsDestination"),

        // rabbit
        AQM_TEMPLATE                         ("amqpTemplate"),

        PROVISIONING_COMMANDS_DESTINATION       ("provisioningCommandsDestination"),
        PROVISIONING_COMMANDS_REPLY_DESTINATION ("provisioningCommandsReplyDestination"),
        NOTIFICATIONS_LOCALHOST_DESTINATION     ("notificationsLocalhostDestination"),
        CUSTOMER_CANCELLATION_DESTINATION       ("customerCancellationRequestDestination"),

        // Diameter RO support
        DIAMETER_HELPER       ("diameterHelper"),
        DIAMETER_USER_LOCATOR ("diameterUserLocator"),
        DIAMETER_ITEM_LOCATOR ("diameterItemLocator"),

        //mediation transaction
        MEDIATION_TRANSACTION_MANAGER ("jbillingMediationTransactionManager"),

        // mediation config
        EXAMPLE_MEDIATION_STEP_CONFIGURATION           ("exampleMediationStepConfig"),

        //cdr to jmr resolver
        MEDIATION_FORMAT                    ("mediationFormat"),
        MEDIATION_CDR_RESOLVER              ("mediationCdrResolver"),
        MEDIATION_RECORD_LINE_CONVERTER     ("mediationRecordLineConverter"),
        RECYCLE_MEDIATION_JOB_LAUNCHER		("recycleCDRJobLauncher"),
        PROCESSORS_DESTINATION_AUTO         ("autoPaymentProcessorsDestination"),

        // EDI Processor
        BATCH_EDI_INVOICE_TRANSACTION_PROCESS         	("invoiceReadProcess"),
        BATCH_EDI_METER_TRANSACTION_PROCESS         	("meterReadProcess"),
        BATCH_EDI_PAYMENT_TRANSACTION_PROCESS         	("paymentReadProcess"),
        BATCH_EDI_ENROLLMENT_RESPONSE_PARSER_PROCESS    ("enrollmentResponseParserProcess"),
        BATCH_EDI_CUSTOMER_TERMINATION_PROCESS         	("customerTerminationProcess"),
        BATCH_EDI_ACKNOWLEDGEMENT_PROCESS         	    ("acknowledgementProcess"),
        BATCH_EDI_ESCO_TERMINATION_PROCESS         	    ("escoTerminationProcess"),
        BATCH_EDI_CHANGE_REQUEST_PROCESS         	    ("changeRequestProcess"),

        // Quartz Scheduler
        JOB_SCHEDULER                                   ("jobScheduler"),
        //integration
        FTPS_REMOTE_FILE_RETRIEVER                      ("ftpsRemoteFileRetriever"),

        // misc
        CAI                 ("cai"),
        MMSC                ("mmsc"),
        VELOCITY            ("velocityEngine"),
        DATA_ENCRYPTER      ("dataEncrypter"),
        JMR_PROCESSOR       ("JMRProcessor"),
        CIPHER_BEAN         ("cipherBean"),
        SESSION_REGISTRY    ("sessionRegistry"),

        METHOD_TRANSACTIONAL_WRAPPER                    ("methodTransactionalWrapper"),

        APPDIRECT_COMPANY_API_CLIENT                    ("appdirectCompanyAPIClient"),

        TASK_EXECUTOR       ("taskExecutor"),

        //Bulk product upload
        BATCH_PRODUCT_DEFAULT_PRICES_LOAD_JOB        ("productDefaultPriceLoadJob"),
        BATCH_PRODUCT_ACCOUNT_PRICES_LOAD_JOB        ("productAccountPriceLoadJob"),
        BATCH_PRODUCT_CUSTOMER_LEVEL_PRICES_LOAD_JOB ("productCustomerPriceLoadJob"),
        BATCH_PLAN_PRICES_LOAD_JOB                   ("planPriceLoadJob"),
        BATCH_PRODUCT_DEFAULT_PRICES_DOWNLOAD_JOB    ("productDefaultPriceDownloadJob"),
        BATCH_PRODUCT_ACCOUNT_PRICES_DOWNLOAD_JOB    ("productAccountPriceDownloadJob"),
        BATCH_PRODUCT_CUSTOMER_PRICES_DOWNLOAD_JOB   ("productCustomerPriceDownloadJob"),
        BATCH_PRODUCT_PLANS_DOWNLOAD_JOB             ("plansDownloadJob"),

        //Ignition batch jobs
        BATCH_JOB_GENERATE_IGNITION_PAYMENTS            ("generateIgnitionPaymentsJob"),
        BATCH_JOB_UPDATE_CUSTOMER_NEXT_ACTION_DATE      ("updateIgnitionCustomerJob"),

        //DT Cluster cache clear topic
        DT_CLSUTER_CACHE_CLEAR_DESTINATION           ("dtLocalCacheClearDestination"),
        BATCH_JOB_DISPATCH_INVOICE_EMAILS     ("invoiceEmailDispatcherJob");
        
        private String name;
        Name(String name) { this.name = name; }
        public String getName() { return name; }
    }

    // static factory cannot be instantiated
    private Context() {
    }

    public static void setApplicationContext(ApplicationContext ctx) {
        spring = ctx;
    }

    public static ApplicationContext getApplicationContext() {
        return spring;
    }

    /**
     * Returns a Spring Bean of type T for the given Context.Name
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

    public static <T> T getBean(Class<T> type) {
        return type.cast(getApplicationContext().getBean(type));
    }
}
