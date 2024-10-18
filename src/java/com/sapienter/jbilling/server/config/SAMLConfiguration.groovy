package com.sapienter.jbilling.server.config

import com.sapienter.jbilling.client.authentication.AuthenticationUserService
import com.sapienter.jbilling.saml.SamlAuthenticationFilter
import com.sapienter.jbilling.client.authentication.util.SecuritySession
import com.sapienter.jbilling.saml.GrailsSAMLAuthenticationProvider
import com.sapienter.jbilling.saml.SpringSamlUserDetailsService
import com.sapienter.jbilling.saml.security.saml.*
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.web.authentication.AjaxAwareAuthenticationFailureHandler
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager
import org.apache.velocity.app.VelocityEngine
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.opensaml.saml2.metadata.provider.MetadataProviderException
import org.opensaml.util.resource.ResourceException
import org.opensaml.xml.parse.StaticBasicParserPool
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.xml.SourceHttpMessageConverter
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.saml.*
import org.springframework.security.saml.context.SAMLContextProvider
import org.springframework.security.saml.context.SAMLContextProviderImpl
import org.springframework.security.saml.key.EmptyKeyManager
import org.springframework.security.saml.key.KeyManager
import org.springframework.security.saml.log.SAMLDefaultLogger
import org.springframework.security.saml.metadata.*
import org.springframework.security.saml.parser.ParserPoolHolder
import org.springframework.security.saml.processor.HTTPPostBinding
import org.springframework.security.saml.processor.SAMLBinding
import org.springframework.security.saml.processor.SAMLProcessorImpl
import org.springframework.security.saml.trust.MetadataCredentialResolver
import org.springframework.security.saml.userdetails.SAMLUserDetailsService
import org.springframework.security.saml.util.VelocityFactory
import org.springframework.security.saml.websso.*
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.*
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy
import org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@Configuration
public class SAMLConfiguration {

    @Autowired
    GrailsApplication grailsApplication;

    @Autowired
    SecuritySession securitySession

    @Autowired
    AuthenticationUserService jbillingUserService

    @Autowired
    RememberMeServices rememberMeServices

    @Autowired
    AuthenticationManager authenticationManager

    @Autowired
    RegisterSessionAuthenticationStrategy registerSessionAuthenticationStrategy

    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler samlAuthenticationSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successRedirectHandler.setAlwaysUseDefaultTargetUrl(false);
        successRedirectHandler.setDefaultTargetUrl("/");
        return successRedirectHandler;
    }

    @Bean
    public DefaultRedirectStrategy defaultRedirectStrategy() {
        DefaultRedirectStrategy defaultRedirectStrategy = new DefaultRedirectStrategy();
        defaultRedirectStrategy.setContextRelative(SpringSecurityUtils.securityConfig.redirectStrategy.contextRelative);
        // false
        return defaultRedirectStrategy
    }

    @Bean
    public AjaxAwareAuthenticationFailureHandler samlAuthenticationFailureHandler() {
        AjaxAwareAuthenticationFailureHandler samlAuthenticationFailureHandler = new AjaxAwareAuthenticationFailureHandler();
        samlAuthenticationFailureHandler.setRedirectStrategy(defaultRedirectStrategy());
        samlAuthenticationFailureHandler.setDefaultFailureUrl(SpringSecurityUtils.securityConfig.failureHandler.defaultFailureUrl);//'/login/authfail?login_error=1'
        samlAuthenticationFailureHandler.setUseForward(SpringSecurityUtils.securityConfig.failureHandler.useForward);// false
        samlAuthenticationFailureHandler.setAjaxAuthenticationFailureUrl(SpringSecurityUtils.securityConfig.failureHandler.ajaxAuthFailUrl);// '/login/authfail?ajax=true'
        samlAuthenticationFailureHandler.setExceptionMappings(SpringSecurityUtils.securityConfig.failureHandler.exceptionMappings);// [:]

        return samlAuthenticationFailureHandler;
    }

    @Bean
    public MetadataDisplayFilter samlMetadataDisplayFilter() {
        return new MetadataDisplayFilter();
    }

    @Bean
    public MetadataGeneratorFilter samlMetadataGeneratorFilter() {
        return new MetadataGeneratorFilter(samlMetadataGenerator());
    }

    @Bean
    public SAMLEntryPoint samlEntryPoint() {
        SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
        samlEntryPoint.setDefaultProfileOptions(samlWebSsoProfileOptions());
        return samlEntryPoint;
    }

    @Bean
    public KeyManager samlKeyManager() {
        return new EmptyKeyManager();
    }

    @Bean
    public ExtendedMetadata samlSpExtendedMetadata() {
        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setIdpDiscoveryEnabled(false);
        extendedMetadata.setSignMetadata(false);
        extendedMetadata.setSigningKey(null);
        extendedMetadata.setEncryptionKey(null);
        return extendedMetadata;
    }

    @Bean
    public SAMLUserDetailsService samlUserDetailsService() {
        Map userAttributeMappings = ['email': 'user.emailAddress', 'entityId': 'companyEntitlement.externalVendorIdentifier',
                                     'firstName': 'user.firstName', 'lastName': 'user.lastName']

        SpringSamlUserDetailsService samlUserDetailsService = new SpringSamlUserDetailsService();
        samlUserDetailsService.setUserService(jbillingUserService);
        samlUserDetailsService.setGrailsApplication(grailsApplication);
        samlUserDetailsService.setSamlUserAttributeMappings(userAttributeMappings);

        return samlUserDetailsService;
    }

    @Bean
    public VelocityEngine samlVelocityEngine() {
        return VelocityFactory.getEngine();
    }

    @Bean(initMethod = "initialize")
    public StaticBasicParserPool samlParserPool() {
        return new StaticBasicParserPool();
    }

    @Bean(name = "parserPoolHolder")
    public ParserPoolHolder samlParserPoolHolder() {
        return new ParserPoolHolder();
    }

    @Bean
    public static SAMLBootstrap samlBootstrap() {
        return new SAMLBootstrap();
    }

    @Bean
    public MetadataCredentialResolver metadataResolver() {
        MetadataCredentialResolver metadataCredentialResolver = new MetadataCredentialResolver(samlIdpsMetadata(), samlKeyManager());
        metadataCredentialResolver.setUseXmlMetadata(true);
        metadataCredentialResolver.setUseExtendedMetadata(true);
        return metadataCredentialResolver;
    }

    @Bean
    public SAMLContextProvider samlContextProvider() {
        SAMLContextProviderImpl samlContextProvider = new SAMLContextProviderImpl();
        samlContextProvider.setMetadataResolver(metadataResolver());
        return samlContextProvider;
    }

    @Bean
    public SAMLDefaultLogger samlLogger() {
        return new SAMLDefaultLogger();
    }

    @Bean(name = "webSSOprofileConsumer")
    public WebSSOProfileConsumer samlWebSsoProfileConsumer() {
        WebSSOProfileConsumerImpl samlWebSsoProfileConsumer = new WebSSOProfileConsumerImpl();
        samlWebSsoProfileConsumer.setResponseSkew(10000);
        return samlWebSsoProfileConsumer;
    }

    @Bean(name = "hokWebSSOprofileConsumer")
    public WebSSOProfileConsumerHoKImpl samlWebSsoProfileConsumerHok() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    @Bean(name = "webSSOprofile")
    public WebSSOProfile samlWebSsoProfile() {
        return new WebSSOProfileImpl();
    }

    @Bean
    public WebSSOProfileECPImpl ecpprofile() {
        return new WebSSOProfileECPImpl();
    }

    @Bean
    public WebSSOProfileOptions samlWebSsoProfileOptions() {
        WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
        webSSOProfileOptions.setIncludeScoping(false);
        return webSSOProfileOptions;
    }

    @Bean
    public SingleLogoutProfileImpl logoutProfile() {
        return new SingleLogoutProfileImpl();
    }

    @Bean(destroyMethod = "cancel")
    public Timer samlMetadataProviderTimer() {
        return new Timer("samlMetadataProviderTimer", true);
    }

    @Bean(name = "metadata")
    public MetadataManager samlIdpsMetadata() throws MetadataProviderException, ResourceException {
        long maxCacheSize = 1000;
        long cacheExpirationMins = 10;
        return new OnDemandMetadataManager(
                samlMetadataLocationResolver(),
                samlIdpMetadataProviderLoader(),
                maxCacheSize,
                cacheExpirationMins,
                Collections.emptyList());
    }

    @Bean
    public MetadataProviderLoader samlIdpMetadataProviderLoader() {
        return new HttpMetadataProviderLoader(
                samlMetadataProviderTimer(),
                samlIdpMetadataHttpClient(),
                samlParserPool());
    }

    @Bean
    public MultiThreadedHttpConnectionManager samlIdpMetadataConnectionManager() {
        return new MultiThreadedHttpConnectionManager();
    }

    @Bean
    public HttpClient samlIdpMetadataHttpClient() {
        return new HttpClient(samlIdpMetadataConnectionManager());
    }

    @Bean
    public MetadataLocationResolver samlMetadataLocationResolver() {
        return new SamlMetadataLocationResolverImpl();
    }

    @Bean
    public MetadataGenerator samlMetadataGenerator() {
        String samlSpEntityId = grailsApplication.config.grails.serverURL

        MetadataGenerator metadataGenerator = new MetadataGenerator();
        metadataGenerator.setEntityId(samlSpEntityId);
        metadataGenerator.setExtendedMetadata(samlSpExtendedMetadata());
        metadataGenerator.setIncludeDiscoveryExtension(false);
        metadataGenerator.setRequestSigned(false);
        metadataGenerator.setKeyManager(samlKeyManager());
        return metadataGenerator;
    }

    @Bean
    public HTTPPostBinding samlHttpPostBinding() {
        return new HTTPPostBinding(samlParserPool(), samlVelocityEngine());
    }

    @Bean
    public SAMLProcessorImpl samlProcessor() {
        Collection<SAMLBinding> bindings = new ArrayList<>();
        bindings.add(samlHttpPostBinding());
        return new SAMLProcessorImpl(bindings);
    }

    @Bean
    public SessionFixationProtectionStrategy sessionFixationProtectionStrategy() {
        return new SessionFixationProtectionStrategy();
    }

    @Bean
    public LogoutHandler[] logoutHandler() {
        LogoutHandler[] logoutHandlers = new LogoutHandler[1];
        SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
        securityContextLogoutHandler.setInvalidateHttpSession(true);
        logoutHandlers[0] = securityContextLogoutHandler;
        return logoutHandlers;
    }

    @Bean
    public SAMLLogoutFilter samlLogoutFilter() {
        SAMLLogoutFilter samlLogoutFilter = new SAMLLogoutFilter(successLogoutHandler(), logoutHandler(), logoutHandler());
        return samlLogoutFilter;
    }

    @Bean
    public LogoutSuccessHandler successLogoutHandler() {
        SimpleUrlLogoutSuccessHandler simpleUrlLogoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
        simpleUrlLogoutSuccessHandler.setDefaultTargetUrl("/");
        return simpleUrlLogoutSuccessHandler;
    }

    @Bean
    public LogoutFilter samlLogoutProcessingFilter() {
        SAMLLogoutProcessingFilter samlLogoutProcessingFilter = new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
        return samlLogoutProcessingFilter;
    }

    @Bean
    public SamlAuthenticationFilter samlProcessingFilter() {
        SamlAuthenticationFilter samlAuthenticationFilter = new SamlAuthenticationFilter();
        samlAuthenticationFilter.setAuthenticationManager(authenticationManager);
        samlAuthenticationFilter.setAuthenticationSuccessHandler(samlAuthenticationSuccessHandler());
        samlAuthenticationFilter.setSessionAuthenticationStrategy(sessionFixationProtectionStrategy());
        samlAuthenticationFilter.setAuthenticationFailureHandler(samlAuthenticationFailureHandler());
        samlAuthenticationFilter.setRememberMeServices(rememberMeServices);
        samlAuthenticationFilter.setSecuritySession(securitySession);
        samlAuthenticationFilter.setRegisterSessionAuthenticationStrategy(registerSessionAuthenticationStrategy);
        return samlAuthenticationFilter;
    }

    @Bean
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
        GrailsSAMLAuthenticationProvider grailsSAMLAuthenticationProvider = new GrailsSAMLAuthenticationProvider();
        grailsSAMLAuthenticationProvider.setUserDetails(samlUserDetailsService());
        grailsSAMLAuthenticationProvider.setHokConsumer(samlWebSsoProfileConsumer());
        return grailsSAMLAuthenticationProvider;
    }

    @Bean
    public AbstractHandlerMethodAdapter annotationHandlerAdapter() {
        RequestMappingHandlerAdapter requestMappingHandlerAdapter = new RequestMappingHandlerAdapter();

        List<HttpMessageConverter> messageConverters = [
                new StringHttpMessageConverter(writeAcceptCharset: false),
                new ByteArrayHttpMessageConverter(),
                new FormHttpMessageConverter(),
                new SourceHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter()
        ]

        requestMappingHandlerAdapter.setMessageConverters(messageConverters);
        return requestMappingHandlerAdapter;
    }
}
