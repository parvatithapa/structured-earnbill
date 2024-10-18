package com.sapienter.jbilling.saml.integration.service;

import com.google.common.base.Preconditions;
import com.sapienter.jbilling.client.EntityDefaults;
import com.sapienter.jbilling.client.authentication.JBillingPasswordEncoder;
import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.saml.SamlUtil;
import com.sapienter.jbilling.saml.integration.APIResult;
import com.sapienter.jbilling.saml.integration.oauth.OAuthPhaseInterceptor;
import com.sapienter.jbilling.saml.integration.oauth.OAuthUrlSigner;
import com.sapienter.jbilling.saml.integration.oauth.OAuthUrlSignerImpl;
import com.sapienter.jbilling.saml.integration.remote.type.ErrorCode;
import com.sapienter.jbilling.saml.integration.remote.type.EventType;
import com.sapienter.jbilling.saml.integration.remote.vo.*;
import com.sapienter.jbilling.saml.integration.remote.vo.saml.SamlRelyingPartyWS;
import com.sapienter.jbilling.saml.integration.util.IntegrationUtils;
import com.sapienter.jbilling.saml.integration.util.ssl.SniSslSocketFactory;
import com.sapienter.jbilling.saml.model.ApplicationProfile;
import com.sapienter.jbilling.server.company.CompanyInformationTypeWS;
import com.sapienter.jbilling.server.company.task.SystemAdminCopyTask;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDeliveryMethodDTO;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.IUserSessionBean;
import com.sapienter.jbilling.server.user.RoleBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.*;
import com.sapienter.jbilling.server.user.permisson.db.RoleDAS;
import com.sapienter.jbilling.server.user.permisson.db.RoleDTO;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import com.sapienter.jbilling.server.util.db.LanguageDTO;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.naming.NamingException;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Integration Service for Integration controller.
 * It Implements the IntegrationService interface.
 * It process events from external IDP for Company creation, User creation and user deletion.
 * It uses IUserSessionBean and WebServicesSessionSpringBean for user, company and metafield creation.
 * It uses PluginAwareResourceBundleMessageSource for Notification creation.
 */

@Service
public class IntegrationServiceImpl implements IntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationServiceImpl.class);

    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private IUserSessionBean userSessionBean;

    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;

    @Autowired
    private PluginAwareResourceBundleMessageSource messageSource;

    @Override
    public AppDirectIntegrationAPI getAppDirectIntegrationApi(String basePath, ApplicationProfile applicationProfile) {
        AppDirectIntegrationAPI api = JAXRSClientFactory.create(basePath, AppDirectIntegrationAPI.class);
        ClientConfiguration config = WebClient.getConfig(api);
        config.getOutInterceptors().add(new OAuthPhaseInterceptor(applicationProfile.getOauthConsumerKey(), applicationProfile.getOauthConsumerSecret()));
        overrideSslSocketFactory(config);
        return api;
    }

    private void overrideSslSocketFactory(ClientConfiguration config) {
        // Workaround (from http://javabreaks.blogspot.com/2015/12/java-ssl-handshake-with-server-name.html) to issue where SSL does not use SNI extension, possibly causing the wrong certificate chain to be retrieved from the server

        TLSClientParameters tlsClientParameters = config.getHttpConduit().getTlsClientParameters();
        if (tlsClientParameters == null) {
            tlsClientParameters = new TLSClientParameters();
            config.getHttpConduit().setTlsClientParameters(tlsClientParameters);
        }

        SSLSocketFactory sslFactory = tlsClientParameters.getSSLSocketFactory();
        if (sslFactory == null) {
            sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        SniSslSocketFactory sniSslFactory = new SniSslSocketFactory(sslFactory);
        tlsClientParameters.setSSLSocketFactory(sniSslFactory);
    }

    @Override
    @Transactional
    public APIResult processEvent(ApplicationProfile applicationProfile, String eventUrl, String token) {
        String basePath;
        if (applicationProfile.isLegacy()) {
            basePath = applicationProfile.getLegacyMarketplaceBaseUrl();
        } else {
            basePath = IntegrationUtils.extractBasePath(eventUrl);
            token = IntegrationUtils.extractToken(eventUrl);
        }

        logger.info("Event url: " + eventUrl);
        logger.info("Base Path: " + basePath);
        logger.info("Token: " + token);

        Preconditions.checkState(StringUtils.isNotBlank(basePath), "basePath should not be blank");
        Preconditions.checkState(StringUtils.isNotBlank(token), "token should not be blank");

        AppDirectIntegrationAPI api = getAppDirectIntegrationApi(basePath, applicationProfile);

        EventInfo eventInfo = api.readEvent(token);

        logger.debug("EventInfo: " + eventInfo);

        if (eventInfo == null || eventInfo.getType() == null) {
            return new APIResult(false, ErrorCode.UNKNOWN_ERROR, "Event info not found or invalid.");
        }
        if (StringUtils.isNotBlank(eventUrl) && !basePath.equals(eventInfo.getMarketplace().getBaseUrl())) {
            return new APIResult(false, ErrorCode.UNKNOWN_ERROR, "Event partner mismatch.");
        }

        logger.debug("EventType: " + eventInfo.getType().toString());

        switch (eventInfo.getType()) {
            case SUBSCRIPTION_ORDER:
                return processSubscriptionOrderEvent(eventInfo, applicationProfile);
            case USER_ASSIGNMENT:
                return processUserAssignmentEvent(eventInfo);
            case USER_UNASSIGNMENT:
                return processUserUnAssignmentEvent(eventInfo);
            default:
                return new APIResult(false, ErrorCode.UNKNOWN_ERROR, "Event type not supported by this endpoint: " + String.valueOf(eventInfo.getType()));
        }
    }

    private APIResult processSubscriptionOrderEvent(EventInfo eventInfo, ApplicationProfile applicationProfile) {
        Preconditions.checkState(eventInfo.getType() == EventType.SUBSCRIPTION_ORDER);

        CompanyDTO companyDTO = createCompany(eventInfo);
        Integer companyId = companyDTO.getId();

        userSessionBean.setEntityParameter(companyId, Constants.PREFERENCE_SSO, "1");
        Integer companyInfoTypeId = createCompanyInfoMetaFields(companyDTO);

        createUserMetaFields(companyId);
        createDefaultRoles(companyDTO);

        Integer creatorUserId = createCreatorUser(eventInfo, companyDTO);
        UserDTO creatorUserDTO = new UserDAS().find(creatorUserId);
        createCompanyContact(companyDTO, creatorUserDTO, eventInfo);

        new EntityDefaults(companyDTO, creatorUserDTO, companyDTO.getLanguage(), messageSource).init();

        try {
            userSessionBean.sendSSOEnabledUserCreatedEmailMessage(companyId, creatorUserId, 1);
        } catch (NotificationNotFoundException e) {
            e.printStackTrace();
        }
        SystemAdminCopyTask sysAdminCopyTask = new SystemAdminCopyTask();
        sysAdminCopyTask.setLogged(false);
        sysAdminCopyTask.create(companyId, companyId);

        if (eventInfo.hasLink(SAML_IDP_LINK)) {
            String samlIdpUrl = eventInfo.getLink(SAML_IDP_LINK).getHref();
            setCompanyInfoTypeMetaFieldValues(companyDTO, eventInfo, samlIdpUrl, applicationProfile, creatorUserId, companyInfoTypeId);
        }

        APIResult result = new APIResult(true, "Account created successfully.");
        result.setAccountIdentifier(Integer.toString(companyId));
        result.setUserIdentifier(Integer.toString(creatorUserId));
        logger.debug("Returning result of " + EventType.SUBSCRIPTION_ORDER.toString() + " event as " + result.toString());
        return result;
    }

    private void createUserMetaFields(Integer entityId) {
        MetaFieldWS userUUID = new MetaFieldWS();
        userUUID.setDataType(DataType.STRING);
        userUUID.setEntityType(EntityType.USER);
        userUUID.setEntityId(entityId);
        userUUID.setName(Constants.SSO_IDP_APPDIRECT_UUID_USER);
        userUUID.setDisplayOrder(1);
        userUUID.setPrimary(true);

        Integer userUUIDMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(userUUID, entityId);

        MetaFieldWS customerUUID = new MetaFieldWS();
        customerUUID.setDataType(DataType.STRING);
        customerUUID.setEntityType(EntityType.CUSTOMER);
        customerUUID.setEntityId(entityId);
        customerUUID.setName(Constants.SSO_IDP_APPDIRECT_UUID_CUSTOMER);
        customerUUID.setDisplayOrder(1);
        customerUUID.setPrimary(true);

        Integer customerUUIDMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(customerUUID, entityId);

        MetaFieldWS agentUUID = new MetaFieldWS();
        agentUUID.setDataType(DataType.STRING);
        agentUUID.setEntityType(EntityType.AGENT);
        agentUUID.setEntityId(entityId);
        agentUUID.setName(Constants.SSO_IDP_APPDIRECT_UUID_AGENT);
        agentUUID.setDisplayOrder(1);
        agentUUID.setPrimary(true);

        Integer agentUUIDMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(agentUUID, entityId);

        MetaFieldWS userIdpId = new MetaFieldWS();
        userIdpId.setDataType(DataType.INTEGER);
        userIdpId.setEntityType(EntityType.USER);
        userIdpId.setEntityId(entityId);
        userIdpId.setName(Constants.SSO_IDP_ID_USER);
        userIdpId.setDisplayOrder(2);
        userIdpId.setPrimary(true);
        userIdpId.setDisabled(true);

        Integer userIdpIdMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(userIdpId, entityId);

        MetaFieldWS agentIdpId = new MetaFieldWS();
        agentIdpId.setDataType(DataType.INTEGER);
        agentIdpId.setEntityType(EntityType.AGENT);
        agentIdpId.setEntityId(entityId);
        agentIdpId.setName(Constants.SSO_IDP_ID_AGENT);
        agentIdpId.setDisplayOrder(2);
        agentIdpId.setPrimary(true);
        agentIdpId.setDisabled(true);

        Integer agentIdpIdMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(agentIdpId, entityId);

        MetaFieldWS customerIdpId = new MetaFieldWS();
        customerIdpId.setDataType(DataType.INTEGER);
        customerIdpId.setEntityType(EntityType.CUSTOMER);
        customerIdpId.setEntityId(entityId);
        customerIdpId.setName(Constants.SSO_IDP_ID_CUSTOMER);
        customerIdpId.setDisplayOrder(2);
        customerIdpId.setPrimary(true);
        customerIdpId.setDisabled(true);

        Integer customerIdpIdMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(customerIdpId, entityId);

        MetaFieldWS userEnableSSO = new MetaFieldWS();
        userEnableSSO.setDataType(DataType.BOOLEAN);
        userEnableSSO.setEntityType(EntityType.USER);
        userEnableSSO.setEntityId(entityId);
        userEnableSSO.setName(Constants.SSO_ENABLED_USER);
        userEnableSSO.setDisplayOrder(3);
        userEnableSSO.setPrimary(true);

        Integer userEnableSSOMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(userEnableSSO, entityId);

        MetaFieldWS agentEnableSSO = new MetaFieldWS();
        agentEnableSSO.setDataType(DataType.BOOLEAN);
        agentEnableSSO.setEntityType(EntityType.AGENT);
        agentEnableSSO.setEntityId(entityId);
        agentEnableSSO.setName(Constants.SSO_ENABLED_AGENT);
        agentEnableSSO.setDisplayOrder(3);
        agentEnableSSO.setPrimary(true);

        Integer agentEnableSSOMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(agentEnableSSO, entityId);

        MetaFieldWS customerEnableSSO = new MetaFieldWS();
        customerEnableSSO.setDataType(DataType.BOOLEAN);
        customerEnableSSO.setEntityType(EntityType.CUSTOMER);
        customerEnableSSO.setEntityId(entityId);
        customerEnableSSO.setName(Constants.SSO_ENABLED_CUSTOMER);
        customerEnableSSO.setDisplayOrder(3);
        customerEnableSSO.setPrimary(true);

        Integer customerEnableSSOMetaFieldId = webServicesSessionBean.createMetaFieldWithEntityId(customerEnableSSO, entityId);
    }

    private Integer createCompanyInfoMetaFields(CompanyDTO companyDTO) {
        Integer entityId = companyDTO.getId();

        MetaFieldWS metaFieldWS1 = new MetaFieldWS();
        metaFieldWS1.setDataType(DataType.STRING);
        metaFieldWS1.setEntityType(EntityType.COMPANY_INFO);
        metaFieldWS1.setEntityId(entityId);
        metaFieldWS1.setMandatory(true);
        metaFieldWS1.setName(Constants.CIT_SAML_IDP_METADATA_URL);
        metaFieldWS1.setDisplayOrder(1);

        MetaFieldWS metaFieldWS2 = new MetaFieldWS();
        metaFieldWS2.setDataType(DataType.STRING);
        metaFieldWS2.setEntityType(EntityType.COMPANY_INFO);
        metaFieldWS2.setEntityId(entityId);
        metaFieldWS2.setMandatory(true);
        metaFieldWS2.setName(Constants.CIT_SAML_IDP_ENTITY_ID);
        metaFieldWS2.setDisplayOrder(2);

        MetaFieldWS metaFieldWS3 = new MetaFieldWS();
        metaFieldWS3.setDataType(DataType.BOOLEAN);
        metaFieldWS3.setEntityType(EntityType.COMPANY_INFO);
        metaFieldWS3.setEntityId(entityId);
        metaFieldWS3.setName(Constants.CIT_DEFAULT_META_FIELD_NAME);
        metaFieldWS3.setDisplayOrder(3);

        MetaFieldWS metaFieldWS4 = new MetaFieldWS();
        metaFieldWS4.setDataType(DataType.STRING);
        metaFieldWS4.setEntityType(EntityType.COMPANY_INFO);
        metaFieldWS4.setEntityId(entityId);
        metaFieldWS4.setMandatory(true);
        metaFieldWS4.setName(Constants.CIT_RESET_PASSWORD_URL_META_FIELD_NAME);
        metaFieldWS4.setDisplayOrder(4);

        MetaFieldWS metaFieldWS5 = new MetaFieldWS();
        metaFieldWS5.setDataType(DataType.ENUMERATION);
        metaFieldWS5.setEntityType(EntityType.COMPANY_INFO);
        metaFieldWS5.setEntityId(entityId);
        metaFieldWS5.setMandatory(true);
        metaFieldWS5.setName(Constants.CIT_DEFAULT_ROLE_META_FIELD_NAME);
        metaFieldWS5.setDisplayOrder(5);

        MetaFieldWS metaFieldWS6 = new MetaFieldWS();
        metaFieldWS6.setDataType(DataType.BOOLEAN);
        metaFieldWS6.setEntityType(EntityType.COMPANY_INFO);
        metaFieldWS6.setEntityId(entityId);
        metaFieldWS6.setName(Constants.CIT_JIT_META_FIELD_NAME);
        metaFieldWS6.setDisplayOrder(6);

        CompanyInformationTypeWS companyInformationTypeWS = new CompanyInformationTypeWS();
        companyInformationTypeWS.setCompanyId(entityId);
        companyInformationTypeWS.setName("IDP1");
        companyInformationTypeWS.setDisplayOrder(1);
        companyInformationTypeWS.setMetaFields(new MetaFieldWS[]{metaFieldWS1, metaFieldWS2, metaFieldWS3, metaFieldWS4, metaFieldWS5, metaFieldWS6});

        return webServicesSessionBean.createCompanyInformationTypeWithEntityId(companyInformationTypeWS, entityId);
    }

    private void setCompanyInfoTypeMetaFieldValues(CompanyDTO companyDTO, EventInfo eventInfo, String samlIdpUrl, ApplicationProfile applicationProfile, Integer userId, Integer companyInfoTypeId) {
        Integer entityId = companyDTO.getId();

        OAuthUrlSigner oauthUrlSigner = new OAuthUrlSignerImpl(applicationProfile.getOauthConsumerKey(), applicationProfile.getOauthConsumerSecret());
        URI signedIdpUri = URI.create(oauthUrlSigner.sign(samlIdpUrl + ".json"));
        SamlRelyingPartyWS idp = restTemplate.getForObject(signedIdpUri, SamlRelyingPartyWS.class);

        MetaFieldValueWS metaFieldValueWS1 = new MetaFieldValueWS();
        metaFieldValueWS1.setFieldName(Constants.CIT_SAML_IDP_METADATA_URL);
        metaFieldValueWS1.getMetaField().setDisplayOrder(1);
        metaFieldValueWS1.getMetaField().setDataType(DataType.STRING);
        metaFieldValueWS1.getMetaField().setEntityId(entityId);
        metaFieldValueWS1.setGroupId(companyInfoTypeId);
        metaFieldValueWS1.setStringValue(samlIdpUrl + ".samlmetadata.xml");

        MetaFieldValueWS metaFieldValueWS2 = new MetaFieldValueWS();
        metaFieldValueWS2.setFieldName(Constants.CIT_SAML_IDP_ENTITY_ID);
        metaFieldValueWS2.getMetaField().setDisplayOrder(2);
        metaFieldValueWS2.getMetaField().setDataType(DataType.STRING);
        metaFieldValueWS2.getMetaField().setEntityId(entityId);
        metaFieldValueWS2.setGroupId(companyInfoTypeId);
        metaFieldValueWS2.setStringValue(idp.getIdpIdentifier());

        MetaFieldValueWS metaFieldValueWS3 = new MetaFieldValueWS();
        metaFieldValueWS3.getMetaField().setDataType(DataType.BOOLEAN);
        metaFieldValueWS3.getMetaField().setEntityId(entityId);
        metaFieldValueWS3.setGroupId(companyInfoTypeId);
        metaFieldValueWS3.setFieldName(Constants.CIT_DEFAULT_META_FIELD_NAME);
        metaFieldValueWS3.getMetaField().setDisplayOrder(3);
        metaFieldValueWS3.setBooleanValue(true);

        MetaFieldValueWS metaFieldValueWS4 = new MetaFieldValueWS();
        metaFieldValueWS4.getMetaField().setDataType(DataType.STRING);
        metaFieldValueWS4.getMetaField().setEntityId(entityId);
        metaFieldValueWS4.setGroupId(companyInfoTypeId);
        metaFieldValueWS4.setFieldName(Constants.CIT_RESET_PASSWORD_URL_META_FIELD_NAME);
        metaFieldValueWS4.getMetaField().setDisplayOrder(4);
        metaFieldValueWS4.setStringValue(eventInfo.getMarketplace().getBaseUrl() + Constants.CIT_RESET_PASSWORD_URL);

        MetaFieldValueWS metaFieldValueWS5 = new MetaFieldValueWS();
        metaFieldValueWS5.getMetaField().setDataType(DataType.ENUMERATION);
        metaFieldValueWS5.getMetaField().setEntityId(entityId);
        metaFieldValueWS5.setGroupId(companyInfoTypeId);
        metaFieldValueWS5.setFieldName(Constants.CIT_DEFAULT_ROLE_META_FIELD_NAME);
        metaFieldValueWS5.getMetaField().setDisplayOrder(5);

        EnumerationWS enumerationWS = new EnumerationWS();
        enumerationWS.setName(Constants.CIT_DEFAULT_ROLE_META_FIELD_NAME);
        EnumerationValueWS enumerationValueWS1 = new EnumerationValueWS();
        enumerationValueWS1.setValue(Constants.CIT_TYPE_ROOT_ENUM_NAME);
        EnumerationValueWS enumerationValueWS2 = new EnumerationValueWS();
        enumerationValueWS2.setValue(Constants.CIT_TYPE_CLERK_ENUM_NAME);
        EnumerationValueWS enumerationValueWS3 = new EnumerationValueWS();
        enumerationValueWS3.setValue(Constants.CIT_TYPE_SYSTEM_ADMIN_ENUM_NAME);
        List<EnumerationValueWS> enumerationValueWSList = new ArrayList<>();
        enumerationValueWSList.add(enumerationValueWS1);
        enumerationValueWSList.add(enumerationValueWS2);
        enumerationValueWSList.add(enumerationValueWS3);
        enumerationWS.setValues(enumerationValueWSList);
        enumerationWS.setEntityId(entityId);

        Integer enumurationId = webServicesSessionBean.createUpdateEnumeration(enumerationWS);
        metaFieldValueWS5.setStringValue(Constants.CIT_TYPE_CLERK_ENUM_NAME);

        MetaFieldValueWS metaFieldValueWS6 = new MetaFieldValueWS();
        metaFieldValueWS6.getMetaField().setDataType(DataType.BOOLEAN);
        metaFieldValueWS6.getMetaField().setEntityId(entityId);
        metaFieldValueWS6.setGroupId(companyInfoTypeId);
        metaFieldValueWS6.setFieldName(Constants.CIT_JIT_META_FIELD_NAME);
        metaFieldValueWS6.getMetaField().setDisplayOrder(6);
        metaFieldValueWS6.setBooleanValue(true);

        CompanyWS companyWS = EntityBL.getCompanyWS(companyDTO);

        companyWS.setMetaFields(new MetaFieldValueWS[]{metaFieldValueWS1, metaFieldValueWS2, metaFieldValueWS3, metaFieldValueWS4, metaFieldValueWS5, metaFieldValueWS6});

        webServicesSessionBean.updateCompanyWithEntityId(companyWS, entityId, userId);
    }

    private CompanyDTO createCompany(EventInfo eventInfo) {
        CompanyDTO companyDTO = new CompanyDTO();
        CompanyDAS companyDAS = new CompanyDAS();

        if (null != companyDAS.findEntityByName(eventInfo.getPayload().getCompany().getName())) {
            companyDTO.setDescription(eventInfo.getPayload().getCompany().getName() + " " + System.currentTimeMillis());
        } else {
            companyDTO.setDescription(eventInfo.getPayload().getCompany().getName());
        }

        companyDTO.setCreateDatetime(new Date());

        LanguageDAS languageDAS = new LanguageDAS();
        LanguageDTO languageDTO = languageDAS.find(1);

        companyDTO.setLanguage(languageDTO);

        CurrencyDAS currencyDAS = new CurrencyDAS();
        CurrencyDTO currencyDTO = currencyDAS.find(1);

        companyDTO.setCurrency(currencyDTO);
        companyDTO.setTimezone("UTC");
        companyDTO.setDeleted(0);

        companyDTO = companyDAS.save(companyDTO);

        // Set Invoice delivery Method Here. Its a constant for all account type.
        setInvoiceDeliveryMethod(companyDTO);
        return companyDTO;
    }

    private void setInvoiceDeliveryMethod(CompanyDTO targetEntity) {
        InvoiceDeliveryMethodDAS invoiceDeliveryMethodDAS = new InvoiceDeliveryMethodDAS();
        List<InvoiceDeliveryMethodDTO> invoiceDeliveryMethodDTOs = invoiceDeliveryMethodDAS.findAll();
        for (InvoiceDeliveryMethodDTO invoiceDeliveryMethodDTO : invoiceDeliveryMethodDTOs) {
            invoiceDeliveryMethodDTO.getEntities().add(targetEntity);
            invoiceDeliveryMethodDAS.save(invoiceDeliveryMethodDTO);
        }
    }

    public void createCompanyContact(CompanyDTO companyDTO, UserDTO userDTO, EventInfo eventInfo) {
        ContactDTO companyContact = new ContactDTO();

        companyContact.setCountryCode(eventInfo.getPayload().getCompany().getCountry());
        String companyEmail = eventInfo.getPayload().getCompany().getEmail();
        String creatorEmail = eventInfo.getCreator().getEmail();
        companyContact.setEmail((companyEmail == null || companyEmail.isEmpty() || companyEmail.length() < 6) ? creatorEmail : companyEmail);
        companyContact.setPhoneNumber(eventInfo.getPayload().getCompany().getPhoneNumber());
        companyContact.setStateProvince("Arizona");
        companyContact.setAddress1("Temp Address");
        companyContact.setAddress2("Temp address 2");
        companyContact.setBaseUser(userDTO);
        companyContact.setCreateDate(new Date());
        companyContact.setCity("Arizona");
        companyContact.setDeleted(0);
        companyContact.setFaxNumber(eventInfo.getPayload().getCompany().getPhoneNumber());
        companyContact.setInclude(1);
        companyContact.setFirstName(eventInfo.getCreator().getFirstName());
        companyContact.setLastName(eventInfo.getCreator().getLastName());
        companyContact.setOrganizationName(eventInfo.getPayload().getCompany().getName());
        companyContact.setPostalCode("85000");

        new ContactBL().create(new ContactDTOEx(companyContact), com.sapienter.jbilling.server.util.Constants.TABLE_ENTITY, companyDTO.getId(), userDTO.getId());
    }

    private Integer createCreatorUser(EventInfo eventInfo, CompanyDTO company) {
        UserWS newUser = new UserWS();
        Integer entityId = company.getId();

        String randPassword = UserBL.generatePCICompliantPassword();
        JBillingPasswordEncoder passwordEncoder = new JBillingPasswordEncoder();

        newUser.setUserName(eventInfo.getCreator().getEmail());
        newUser.setPassword(passwordEncoder.encodePassword(randPassword, null));
        newUser.setLanguageId(CommonConstants.JIT_USER_LANGUAGE_ID);

        //get Role from customer attributes currently setting it to Root
        int roleId = 2;
        newUser.setMainRoleId(roleId);
        newUser.setIsParent(CommonConstants.JIT_USER_DEFAULT_FALSE);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setEntityId(entityId);
        newUser.setDeleted(CommonConstants.JIT_USER_DELETED);
        newUser.setCompanyName(company.getDescription());
        newUser.setCreateDatetime(new Date());
        newUser.setCurrencyId(CommonConstants.JIT_USER_CURRENCY_ID);
        newUser.setExcludeAgeing(CommonConstants.JIT_USER_DEFAULT_FALSE);
        newUser.setFailedAttempts(CommonConstants.JIT_USER_FAILED_ATTEMPTS);
        newUser.setIsAccountLocked(CommonConstants.JIT_USER_DEFAULT_FALSE);

        // add a contact
        ContactWS contact = new ContactWS();

        contact.setEmail(eventInfo.getCreator().getEmail());
        contact.setLastName(eventInfo.getCreator().getLastName());
        contact.setFirstName(eventInfo.getCreator().getFirstName());
        contact.setOrganizationName(company.getDescription());

        // Address will be sent via user attributes. For now default address is being used
        contact.setAddress1(CommonConstants.JIT_USER_FIRST_ADDRESS1);
        contact.setAddress2(CommonConstants.JIT_USER_LAST_ADDRESS2);
        contact.setCity(CommonConstants.JIT_USER_CITY);
        contact.setStateProvince(CommonConstants.JIT_USER_STATE);
        contact.setPostalCode(CommonConstants.JIT_USER_POSTAL_CODE);
        contact.setCountryCode(CommonConstants.JIT_USER_COUNTRY_CODE);
        contact.setInclude(CommonConstants.JIT_USER_DEFAULT_TRUE);

        newUser.setContact(contact);

        Integer idpGroupId = SamlUtil.getDefaultIdpViaIdpEntityURL(entityId, eventInfo.getMarketplace().getBaseUrl());

        String entityType = EntityType.USER.toString();

        MetaFieldWS[] metaField = webServicesSessionBean.getMetaFieldsByEntityId(entityId, entityType);
        if (null != metaField && metaField.length > 0) {
            MetaFieldValueWS ssoEnabledMetaFieldValue = null;
            MetaFieldValueWS ssoIdpIdMetaFieldValue = null;
            MetaFieldValueWS ssoIdpAppDirectUuidMetaFieldValue = null;
            for (int i = 0; i < metaField.length; i++) {
                if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_ENABLED_USER)) {
                    ssoEnabledMetaFieldValue = new MetaFieldValueWS(Constants.SSO_ENABLED_USER, null, DataType.BOOLEAN, true, true);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_ID_USER)) {
                    ssoIdpIdMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_ID_USER, null, DataType.INTEGER, false, idpGroupId);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_APPDIRECT_UUID_USER)) {
                    ssoIdpAppDirectUuidMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_APPDIRECT_UUID_USER, null, DataType.STRING, false, eventInfo.getCreator().getUuid());
                }
            }

            newUser.setMetaFields(new MetaFieldValueWS[]{ssoEnabledMetaFieldValue, ssoIdpIdMetaFieldValue, ssoIdpAppDirectUuidMetaFieldValue});
        }

        System.out.println("Creating user for User Assignment Event ...");
        Integer userId = 0;
        // do the creation
        try {
            userId = webServicesSessionBean.createUserForAppDirect(newUser, entityId, true);
        } catch (SessionInternalError e) {
            throw new SessionInternalError(e);
        }
        return userId;
    }

    void createDefaultRoles(CompanyDTO company) {

        List<Integer> defaultRoleList = new ArrayList<>();
        defaultRoleList.add(Constants.TYPE_ROOT);
        defaultRoleList.add(Constants.TYPE_CLERK);
        defaultRoleList.add(Constants.TYPE_CUSTOMER);
        defaultRoleList.add(Constants.TYPE_PARTNER);
        defaultRoleList.add(Constants.TYPE_SYSTEM_ADMIN);

        RoleBL roleService = new RoleBL();

        for (Integer roleTypeId : defaultRoleList) {

            RoleDTO role = new RoleDAS().findByRoleTypeIdAndCompanyId(roleTypeId, null);

            // check the initial role ( companyId = null )
            if (null == role) {
                // if not initial role set use the latest company role settings available
                EntityBL entityBL = new EntityBL();
                Integer[] companyIds;
                try {
                    companyIds = entityBL.getAllIDs();
                    role = new RoleDAS().findByRoleTypeIdAndCompanyId(
                            roleTypeId, companyIds[0]);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }

            if (null == role) {
                return;
            }

            RoleDTO newRole = new RoleDTO();
            newRole.getPermissions().addAll(role.getPermissions());
            newRole.setCompany(company);
            newRole.setRoleTypeId(roleTypeId);

            roleService.create(newRole);
            roleService.setDescription(1, role.getDescription(1) == null ? "" : role.getDescription());
            roleService.setTitle(1, role.getTitle(1) == null ? "" : role.getTitle(1));
        }
    }

    private APIResult processUserAssignmentEvent(EventInfo eventInfo) {
        Preconditions.checkState(eventInfo.getType() == EventType.USER_ASSIGNMENT);

        APIResult result;
        Integer accountId = null;
        try {
            accountId = Integer.valueOf(eventInfo.getPayload().getAccount().getAccountIdentifier());
            CompanyDTO company = new CompanyDAS().find(accountId);
            if (null != company) {
                // Create the new user.
                Integer userId = createUserAssignmentEvent(eventInfo, company);

                if (0 != userId) {
                    result = new APIResult(true, "Successfully created user: " + eventInfo.getPayload().getUser().getUuid());
                    result.setUserIdentifier(userId.toString());
                } else {
                    result = new APIResult(false, ErrorCode.ACCOUNT_NOT_FOUND, String.format("Could not find account with identifier %s", accountId.toString()));
                }
            } else {
                result = new APIResult(false, ErrorCode.ACCOUNT_NOT_FOUND, String.format("Could not find account with identifier %s", accountId.toString()));
            }
        } catch (ObjectNotFoundException | NumberFormatException e) {
            result = new APIResult(false, ErrorCode.ACCOUNT_NOT_FOUND, String.format("Could not find account with identifier %s", accountId.toString()));
        }
        logger.debug("Returning result of " + EventType.USER_ASSIGNMENT.toString() + " event as " + result.toString());
        return result;
    }

    private Integer createUserAssignmentEvent(EventInfo eventInfo, CompanyDTO company) {
        UserWS newUser = new UserWS();

        Integer entityId = Integer.valueOf(eventInfo.getPayload().getAccount().getAccountIdentifier());

        String randPassword = UserBL.generatePCICompliantPassword();
        JBillingPasswordEncoder passwordEncoder = new JBillingPasswordEncoder();

        newUser.setUserName(eventInfo.getPayload().getUser().getEmail());
        newUser.setPassword(passwordEncoder.encodePassword(randPassword, null));
        newUser.setLanguageId(CommonConstants.JIT_USER_LANGUAGE_ID);

        //get Role from customer attributes currently setting it to Root
        int roleId = SamlUtil.getDefaultIdpRole(entityId);
        newUser.setMainRoleId(roleId);
        newUser.setIsParent(CommonConstants.JIT_USER_DEFAULT_FALSE);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setEntityId(entityId);
        newUser.setDeleted(CommonConstants.JIT_USER_DELETED);
        newUser.setCompanyName(company.getDescription());
        newUser.setCreateDatetime(new Date());
        newUser.setCurrencyId(CommonConstants.JIT_USER_CURRENCY_ID);
        newUser.setExcludeAgeing(CommonConstants.JIT_USER_DEFAULT_FALSE);
        newUser.setFailedAttempts(CommonConstants.JIT_USER_FAILED_ATTEMPTS);
        newUser.setIsAccountLocked(CommonConstants.JIT_USER_DEFAULT_FALSE);

        // add a contact
        ContactWS contact = new ContactWS();

        contact.setEmail(eventInfo.getPayload().getUser().getEmail());
        contact.setFirstName(eventInfo.getPayload().getUser().getFirstName());
        contact.setLastName(eventInfo.getPayload().getUser().getLastName());
        contact.setOrganizationName(company.getDescription());

        // Address will be sent via user attributes. For now default address is being used
        contact.setAddress1(CommonConstants.JIT_USER_FIRST_ADDRESS1);
        contact.setAddress2(CommonConstants.JIT_USER_LAST_ADDRESS2);
        contact.setCity(CommonConstants.JIT_USER_CITY);
        contact.setStateProvince(CommonConstants.JIT_USER_STATE);
        contact.setPostalCode(CommonConstants.JIT_USER_POSTAL_CODE);
        contact.setCountryCode(CommonConstants.JIT_USER_COUNTRY_CODE);
        contact.setInclude(CommonConstants.JIT_USER_DEFAULT_TRUE);

        newUser.setContact(contact);

        Integer idpGroupId = SamlUtil.getDefaultIdpViaIdpEntityURL(entityId, eventInfo.getMarketplace().getBaseUrl());

        String entityType = EntityType.USER.toString();
        if (roleId == CommonConstants.TYPE_PARTNER) {
            entityType = EntityType.AGENT.toString();
        } else if (roleId == CommonConstants.TYPE_CUSTOMER) {
            entityType = EntityType.CUSTOMER.toString();
        }

        MetaFieldWS[] metaField = webServicesSessionBean.getMetaFieldsByEntityId(entityId, entityType);
        if (null != metaField && metaField.length > 0) {
            MetaFieldValueWS ssoEnabledMetaFieldValue = null;
            MetaFieldValueWS ssoIdpIdMetaFieldValue = null;
            MetaFieldValueWS ssoIdpAppDirectUuidMetaFieldValue = null;
            for (int i = 0; i < metaField.length; i++) {
                if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_ENABLED_USER)) {
                    ssoEnabledMetaFieldValue = new MetaFieldValueWS(Constants.SSO_ENABLED_USER, null, DataType.BOOLEAN, true, true);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_ENABLED_AGENT)) {
                    ssoEnabledMetaFieldValue = new MetaFieldValueWS(Constants.SSO_ENABLED_AGENT, null, DataType.BOOLEAN, true, true);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_ENABLED_CUSTOMER)) {
                    ssoEnabledMetaFieldValue = new MetaFieldValueWS(Constants.SSO_ENABLED_CUSTOMER, null, DataType.BOOLEAN, true, true);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_ID_USER)) {
                    ssoIdpIdMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_ID_USER, null, DataType.INTEGER, false, idpGroupId);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_ID_AGENT)) {
                    ssoIdpIdMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_ID_AGENT, null, DataType.INTEGER, false, idpGroupId);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_ID_CUSTOMER)) {
                    ssoIdpIdMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_ID_CUSTOMER, null, DataType.INTEGER, false, idpGroupId);
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_APPDIRECT_UUID_USER)) {
                    ssoIdpAppDirectUuidMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_APPDIRECT_UUID_USER, null, DataType.STRING, false, eventInfo.getPayload().getUser().getUuid());
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_APPDIRECT_UUID_AGENT)) {
                    ssoIdpAppDirectUuidMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_APPDIRECT_UUID_AGENT, null, DataType.STRING, false, eventInfo.getPayload().getUser().getUuid());
                } else if (metaField[i].getName().equalsIgnoreCase(Constants.SSO_IDP_APPDIRECT_UUID_CUSTOMER)) {
                    ssoIdpAppDirectUuidMetaFieldValue = new MetaFieldValueWS(Constants.SSO_IDP_APPDIRECT_UUID_CUSTOMER, null, DataType.STRING, false, eventInfo.getPayload().getUser().getUuid());
                }
            }

            newUser.setMetaFields(new MetaFieldValueWS[]{ssoEnabledMetaFieldValue, ssoIdpIdMetaFieldValue, ssoIdpAppDirectUuidMetaFieldValue});
        }

        System.out.println("Creating user for User Assignment Event ...");
        Integer userId = 0;
        // do the creation
        try {
            userId = webServicesSessionBean.createUserForAppDirect(newUser, entityId, false);
        } catch (SessionInternalError e) {
            throw new SessionInternalError(e);
        }

        return userId;
    }

    private APIResult processUserUnAssignmentEvent(EventInfo eventInfo) {
        Preconditions.checkState(eventInfo.getType() == EventType.USER_UNASSIGNMENT);
        APIResult result;
        try {
            Integer accountId = Integer.valueOf(eventInfo.getPayload().getAccount().getAccountIdentifier());
            String appDirectUuid = eventInfo.getPayload().getUser().getUuid();
            String executorUuid = eventInfo.getCreator().getUuid();
            boolean deleted = userSessionBean.deleteUserByEntityIdAndAppDirectUUID(accountId, appDirectUuid, executorUuid);
            if (deleted) {
                result = new APIResult(true, "Successfully deleted user: " + appDirectUuid);
            } else {
                result = new APIResult(false, ErrorCode.USER_NOT_FOUND, "No user found with uuid: " + appDirectUuid);
            }
        } catch (ObjectNotFoundException | NumberFormatException e) {
            // The user could not be found. Fail.
            result = new APIResult(false, ErrorCode.USER_NOT_FOUND, e.getMessage());
        }
        logger.debug("Returning result of " + EventType.USER_UNASSIGNMENT.toString() + " event as " + result.toString());
        return result;
    }
}
