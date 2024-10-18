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

package jbilling

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sapienter.jbilling.appdirect.subscription.PayloadWS
import com.sapienter.jbilling.appdirect.subscription.ResourceWS
import com.sapienter.jbilling.appdirect.subscription.companydetails.AppdirectCompanyWS
import com.sapienter.jbilling.appdirect.subscription.http.AppdirectCompanyAPIClient
import com.sapienter.jbilling.appdirect.subscription.http.exception.AppdirectCompanyClientException
import com.sapienter.jbilling.appdirect.subscription.oauth.OAuthWS
import com.sapienter.jbilling.appdirect.subscription.productdetails.ProductDetailsWS
import com.sapienter.jbilling.appdirect.userCompany.CompanyPayload
import com.sapienter.jbilling.common.ProductNotValidException
import com.sapienter.jbilling.common.SessionInternalError
import com.sapienter.jbilling.server.item.CurrencyBL
import com.sapienter.jbilling.server.metafields.EntityType
import com.sapienter.jbilling.server.metafields.MetaFieldBL
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS
import com.sapienter.jbilling.server.metafields.MetaFieldWS
import com.sapienter.jbilling.server.metafields.db.MetaField
import com.sapienter.jbilling.server.user.AccountTypeWS
import com.sapienter.jbilling.server.user.CompanyWS
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.UserDTOEx
import com.sapienter.jbilling.server.user.UserWS
import com.sapienter.jbilling.server.util.Constants
import com.sapienter.jbilling.server.util.Context
import com.sapienter.jbilling.server.util.CurrencyWS
import com.sapienter.jbilling.server.util.LanguageWS
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean
import org.apache.commons.lang.ArrayUtils
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.jasypt.commons.CommonUtils
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

import java.lang.invoke.MethodHandles
import java.lang.reflect.Type

import static com.sapienter.jbilling.server.util.Constants.DeutscheTelekom.*
import static java.util.stream.Collectors.toList

class DtCustomerService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMddHHmm")

    WebServicesSessionSpringBean webServicesSession

    @Transactional
    UserWS createDTCustomer(String entityIdentifier, PayloadWS payloadWS) throws ProductNotValidException{

        try {
            CompanyWS companyWS = webServicesSession.getCompanyByMetaFieldValue(entityIdentifier)
            String productIdentifier = payloadWS.getResource().getContent().getProduct().getId()
            checkValidProduct(companyWS, productIdentifier)
            UserWS parentCustomerWS = createParentCustomer(payloadWS, entityIdentifier, companyWS)
            UserWS childCustomer = createChildCustomer(payloadWS, parentCustomerWS, entityIdentifier, companyWS)

            return childCustomer
        } catch (Exception e) {
            logger.error("Error creating user, moving to error table", e)
            logger.debug("Failed Request Payload: {}", new Gson().toJson(payloadWS))
            throw e
        }
    }

    @Transactional
    void deleteDTCustomer(String entityIdentifier, PayloadWS payloadWS) {

        String subscriptionId =  payloadWS.getResource().getUuid()

        CompanyWS companyWS = webServicesSession.getCompanyByMetaFieldValue(entityIdentifier)
        if (companyWS == null) {
            logger.error("Company not found with metaField ${entityIdentifier}")
            throw new SessionInternalError("Company not found with entityId ${entityIdentifier}",
                    HttpStatus.SC_NOT_FOUND)
        }

        UserWS existingCustomer = webServicesSession.getUserByCustomerMetaFieldAndCompanyId(
                subscriptionId, APPDIRECT_SUBSCRIPTION_IDENTIFIER, companyWS.getId())
        if (existingCustomer == null) {
            logger.error("Customer/Subscription not found with subscription metaField ${subscriptionId}")
            throw new SessionInternalError("Customer not found with subscriptionId ${subscriptionId}",
                    HttpStatus.SC_NOT_FOUND)
        }

        webServicesSession.removeUser(existingCustomer.getUserId(), webServicesSession.getCallerId())
    }

    @Transactional
    void updateCompany(String entityIdentifier, CompanyPayload companyPayload) {

        CompanyWS companyWS = webServicesSession.getCompanyByMetaFieldValue(entityIdentifier)
        if (companyWS == null) {
            logger.error("Company not found with metaField ${entityIdentifier}")
            throw new SessionInternalError("Company not found with entityId ${entityIdentifier}",
                    HttpStatus.SC_NOT_FOUND)
        }

        String customerUuid =  companyPayload.getResource().getUuid()
        String type = companyPayload.getResource().getType()
        String action = companyPayload.getResourceAction()
        String companyName = companyPayload.getResource().getContent().getName()

        if (customerUuid != null && RESOURCE_TYPE_COMPANY.equalsIgnoreCase(type)
                && PAYLOAD_ACTION_CHANGED.equalsIgnoreCase(action)) {

            UserWS existingCustomer = webServicesSession.getUserByCustomerMetaFieldAndCompanyId(
                    customerUuid, EXTERNAL_ACCOUNT_IDENTIFIER, companyWS.getId())

            if (existingCustomer != null) {
                existingCustomer.setUserName(companyName)
                Integer customerId = existingCustomer.getId()

                List<UserWS> childCustomers = webServicesSession.getUsersByParentId(customerId)

                webServicesSession.updateUserWithCompanyId(existingCustomer, companyWS.getId())

                for (UserWS childCustomer : childCustomers) {
                    MetaFieldValueWS[] metaFields = childCustomer.getMetaFields()
                    childCustomer.setUserName(companyName + "-" +
                            getMetafieldValue(EXTERNAL_ACCOUNT_IDENTIFIER, metaFields))

                    webServicesSession.updateUserWithCompanyId(childCustomer, companyWS.getId())
                }

            } else {
                logger.error("Customer/Subscription not found with subscription metaField ${customerUuid}", )
                throw new SessionInternalError("Customer not found for Id ${customerUuid}", HttpStatus.SC_NOT_FOUND)
            }
        }
    }


    /* internal methods */

    private String getMetafieldValue(String metafieldName, MetaFieldValueWS[] metaFields) {

        if (metaFields != null) {
            for (MetaFieldValueWS metaField : metaFields) {
                if(metafieldName.equalsIgnoreCase(metaField.getMetaField().getName())) {
                    return metaField.getStringValue()
                }
            }
        }
        return null
    }

    private UserWS createParentCustomer(PayloadWS payloadWS, String entityIdentifier, CompanyWS companyWS) {

        if (payloadWS != null && payloadWS.getResource() != null && payloadWS.getResource().getContent() != null &&
                payloadWS.getResource().getContent().getCompany() != null &&
                payloadWS.getResource().getContent().getCompany().getId() != null) {

            String customerUuid =  payloadWS.getResource().getContent().getCompany().getId()
            UserWS existingCustomer
            try {
                existingCustomer = webServicesSession.getUserByCustomerMetaFieldAndCompanyId(
                        customerUuid, EXTERNAL_ACCOUNT_IDENTIFIER, companyWS.getId())

            } catch (SessionInternalError sie) {
                logger.info("Okay to proceed creating a new parent customer")
            }

            if (existingCustomer != null) {
                logger.info("Parent Customer with uuid ${entityIdentifier} already exists")
                return existingCustomer
            }

            return createCustomer(payloadWS, null, companyWS, true)
        }
    }

    private UserWS createChildCustomer(PayloadWS payloadWS, UserWS parentCustomerWS,
                                       String entityIdentifier, CompanyWS companyWS) {

        if (payloadWS != null && payloadWS.getResource() != null && payloadWS.getResource().getContent() != null &&
                payloadWS.getResource().getContent().getExternalAccountId() != null) {

            ResourceWS resource = payloadWS.getResource()
            if(RESOURCE_TYPE_SUBSCRIPTION.equals(resource.getType())) {
                String customerUuid =  resource.getContent().getExternalAccountId()
                UserWS existingCustomer
                try {
                    existingCustomer = webServicesSession.getUserByCustomerMetaFieldAndCompanyId(
                            customerUuid, EXTERNAL_ACCOUNT_IDENTIFIER, companyWS.getId())

                } catch (SessionInternalError sie) {
                    logger.info("Okay to proceed creating a new child customer")
                }
                if (existingCustomer != null) {
                    logger.info("Child Customer with uuid ${entityIdentifier} already exists")
                    return existingCustomer
                }

                return createCustomer(payloadWS, parentCustomerWS, companyWS, false)
            }
        }
    }

    private UserWS createCustomer(PayloadWS payloadWS, UserWS parentCustomer, CompanyWS companyWS, boolean isParent) {

        String customerUuid = getCustomerUuid(isParent, payloadWS)
        UserWS userWS = massageAndCreateUserWSObject(payloadWS, parentCustomer, companyWS, isParent)

        if(isParent) {
            OAuthWS oAuthWS = getOAuthWS(companyWS)
            String companyName = getCompanyNameFromMarketplace(customerUuid, oAuthWS)
            userWS.setUserName(companyName)
            userWS.setCompanyName(companyName)

        } else if(parentCustomer != null) {
            userWS.setUserName(parentCustomer.getUserName() + "-" + customerUuid)
            userWS.setCompanyName(parentCustomer.getUserName() + "-" + customerUuid)
        }

        Integer userId = webServicesSession.createUserWithCompanyId(userWS, userWS.getEntityId())
        userWS.setUserId(userId)

        return userWS
    }

    private String getCompanyNameFromMarketplace(String customerUuid, OAuthWS oAuthWS) {
        AppdirectCompanyWS appdirectCompany
        try {
            AppdirectCompanyAPIClient apiClient = Context.getBean(Context.Name.APPDIRECT_COMPANY_API_CLIENT)
            appdirectCompany = apiClient.getCompanyDetails(customerUuid,
                    oAuthWS.baseApiUrl,
                    oAuthWS.consumerKey,
                    oAuthWS.consumerSecret)

        } catch (AppdirectCompanyClientException e) {
            logger.error("Failed request", e)
            String fallback = getFallbackCompanyName(customerUuid)
            logger.warn("Un-categorized error, falling back to generated company name ${fallback}")
            return fallback
        }

        if (appdirectCompany == null) {
            logger.error("Company not found in marketplace (404)")
            throw new SessionInternalError("Company not found", HttpStatus.SC_INTERNAL_SERVER_ERROR)
        }
        return appdirectCompany.getName()
    }

    private String getFallbackCompanyName(String customerId) {
        return "Default_${customerId.replace('-', '')}._${dateTimeFormatter.print(new Date().getTime())}"
    }

    private String getCustomerUuid(boolean isParent, PayloadWS payloadWS) {

        String customerUuid
        ResourceWS resource = payloadWS.getResource()
        if (isParent) {
            customerUuid = payloadWS.getResource().getContent().getCompany().getId()
        } else {
            customerUuid = resource.getContent().getExternalAccountId()
        }
        return customerUuid
    }

    private String getAppDirectSubscriptionId(boolean isParent, PayloadWS payloadWS) {
        return isParent ? null : payloadWS.getResource().getUuid()
    }

    private Integer getAppDirectProductId(boolean isParent, PayloadWS payloadWS) {

        String appDirectProductId
        ResourceWS resource = payloadWS.getResource()

        if (isParent) {
            appDirectProductId = null
        } else {
            appDirectProductId = resource.getContent().getProduct().getId()
        }

        return appDirectProductId == null ? 0 : Integer.valueOf(appDirectProductId)
    }

    private UserWS massageAndCreateUserWSObject(PayloadWS payloadWS, UserWS parentCustomer,
                                                CompanyWS companyWS, boolean isParent) {

        String customerUuid = getCustomerUuid(isParent, payloadWS)
        String appDirectSubscriptionId = getAppDirectSubscriptionId(isParent, payloadWS)
        Integer productId = getAppDirectProductId(isParent, payloadWS)

        // Get the account type for DT customer using the act_id metafield set at the customer level
        MetaFieldValueWS metaFieldValueWS = companyWS.getMetaFieldByName(ACCOUNT_TYPE_ID)
        Integer accountTypeId = metaFieldValueWS.getIntegerValue();
        AccountTypeWS accountTypeWS = webServicesSession.getAccountType(accountTypeId);

        Collection<MetaField> metaFields = MetaFieldBL.getAvailableFieldsList(companyWS.getId(), EntityType.CUSTOMER)

        Map<Integer, List<MetaField>> aitMetaFieldsMap = MetaFieldExternalHelper
                .getAvailableAccountTypeFieldsMap(accountTypeWS.getId())

        List<MetaFieldWS> metaFieldWSList = metaFields.stream()
                .map({ dto -> MetaFieldBL.getWS(dto) })
                .collect(toList())

        MetaFieldValueWS[] customerMetaFieldValueWSArray = getMetaFieldValues(metaFieldWSList,
                customerUuid, appDirectSubscriptionId, productId)
        MetaFieldValueWS[] aitMetaFieldValueWSArray = getMetaFieldValues(aitMetaFieldsMap,
                customerUuid, appDirectSubscriptionId, productId)
        MetaFieldValueWS[] metaFieldValueWSArray = (MetaFieldValueWS[]) ArrayUtils.addAll(customerMetaFieldValueWSArray, aitMetaFieldValueWSArray)

        UserWS userWS = getUserWS(accountTypeWS)
        userWS.setUserName(customerUuid)
        userWS.setEntityId(companyWS.getId())
        userWS.setMetaFields(metaFieldValueWSArray)

        if(parentCustomer != null) {
            userWS.setIsParent(false)
            userWS.setParentId(parentCustomer.getUserId())
        }
        return userWS
    }

    private OAuthWS getOAuthWS(CompanyWS companyWS) {
        MetaFieldValueWS[] metaFieldValueWSes = companyWS.getMetaFields()
        OAuthWS oAuthWS = new OAuthWS()
        for(MetaFieldValueWS metaFieldValueWS: metaFieldValueWSes) {
            if(APPDIRECT_CONSUMER_KEY.equals(metaFieldValueWS.getMetaField().getName())) {
                oAuthWS.setConsumerKey(metaFieldValueWS.getValue())

            } else if(APPDIRECT_CONSUMER_SECRET.equals(metaFieldValueWS.getMetaField().getName())) {
                oAuthWS.setConsumerSecret(metaFieldValueWS.getValue())

            } else if(APPDIRECT_COMPANY_API_BASE_URL.equals(metaFieldValueWS.getMetaField().getName())) {
                oAuthWS.setBaseApiUrl(metaFieldValueWS.getValue())
            }
        }
        return oAuthWS
    }

    private MetaFieldValueWS[] getMetaFieldValues(Map<Integer, List<MetaField>> aitMetaFieldsMap,
                                                  String customerUuid, String appDirectSubscriptionId,
                                                  Integer productId) {

        List<MetaFieldValueWS> aitMetaFieldValueWSList = new ArrayList<>()
        for(Map.Entry<Integer, List<MetaField>> entry: aitMetaFieldsMap.entrySet()) {
            List<MetaFieldWS> aitMetaFieldWSList = entry.getValue().stream()
                    .map({ dto -> MetaFieldBL.getWS(dto) })
                    .collect(toList())

            for(MetaFieldWS metaFieldWS: aitMetaFieldWSList) {
                if(metaFieldWS.isMandatory()) {
                    MetaFieldValueWS metaFieldValueWS = createMetaFieldValueWS(metaFieldWS,
                            customerUuid, appDirectSubscriptionId, productId)
                    metaFieldValueWS.setGroupId(entry.getKey())
                    if(metaFieldValueWS.getValue() != null) {
                        aitMetaFieldValueWSList.add(metaFieldValueWS)
                    }
                }
            }
        }
        return aitMetaFieldValueWSList.toArray(new MetaFieldValueWS[aitMetaFieldValueWSList.size()])
    }

    private MetaFieldValueWS[] getMetaFieldValues(List<MetaFieldWS> metaFieldWSList, String customerUuid,
                                                  String appDirectSubscriptionId, Integer productId) {

        MetaFieldValueWS[] metaFieldValueWSArray = new MetaFieldValueWS[metaFieldWSList.size()]
        int i = 0
        metaFieldWSList.each {
            MetaFieldWS metaFieldWS ->
                MetaFieldValueWS metaFieldValueWS = createMetaFieldValueWS(metaFieldWS, customerUuid,
                        appDirectSubscriptionId, productId)
                metaFieldValueWSArray[i++] = metaFieldValueWS
        }
        return metaFieldValueWSArray
    }

    private MetaFieldValueWS createMetaFieldValueWS(MetaFieldWS metaFieldWS, String customerUuid,
                                                    String appDirectSubscriptionId, Integer productId) {

        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS(metaFieldWS, null, null)

        switch(metaFieldWS.getName()){
            case EXTERNAL_ACCOUNT_IDENTIFIER:
                metaFieldValueWS.setValue(customerUuid)
                break
            case APPDIRECT_SUBSCRIPTION_IDENTIFIER:
                if(CommonUtils.isNotEmpty(appDirectSubscriptionId)){
                    metaFieldValueWS.setValue(appDirectSubscriptionId)
                }
                break
            case APPDIRECT_PRODUCT_IDENTIFIER:
                if(productId != 0){
                    metaFieldValueWS.setValue(productId)
                }
                break
            case LAST_NAME:
                metaFieldValueWS.setValue(STR_DUMMY)
                break
            case FIRST_NAME:
                metaFieldValueWS.setValue(STR_DUMMY)
                break
            case COUNTRY:
                metaFieldValueWS.setValue(COUNTRY_CODE)
                break
            case STATE_PROVINCE:
                metaFieldValueWS.setValue(STR_DUMMY)
                break
            case POSTAL_CODE:
                metaFieldValueWS.setValue(STR_DUMMY)
                break
            case CITY:
                metaFieldValueWS.setValue(STR_DUMMY)
                break
            case EMAIL_ADDRESS:
                metaFieldValueWS.setValue(STR_DUMMY + System.currentTimeMillis() + "@yopmail.com")
        }
        metaFieldValueWS
    }

    private UserWS getUserWS(AccountTypeWS accountTypeWS) {
        UserWS userWS = new UserWS()
        userWS.setAccountTypeId(accountTypeWS.getId())
        userWS.setLanguageId(accountTypeWS.getLanguageId())
        userWS.setCurrencyId(accountTypeWS.getCurrencyId())
        ContactWS contactWS = getContactWS()
        userWS.setContact(contactWS)
        userWS.setLanguage("English")
        userWS.setMainRoleId(Constants.TYPE_CUSTOMER)
        userWS.setStatusId(UserDTOEx.STATUS_ACTIVE)
        userWS.setSubscriberStatusId(1)
        userWS.setIsParent(true)
        userWS.setInvoiceDeliveryMethodId(Constants.D_METHOD_NONE)
        return userWS
    }

    private ContactWS getContactWS() {
        ContactWS contactWS = new ContactWS()
        contactWS.setAddress1("dummy")
        contactWS.setStateProvince("dummy")
        contactWS.setCountryCode("DE")
        return contactWS
    }

    private List<ProductDetailsWS> getProductDetailsFromMetaField(MetaFieldValueWS metaFieldValueWS) {
        if(metaFieldValueWS == null) {
            return new ArrayList<ProductDetailsWS>()
        }
        Type listType = new TypeToken<ArrayList<ProductDetailsWS>>(){}.getType()
        Gson gson = new Gson()
        List<ProductDetailsWS> productDetailsWSList = gson.fromJson(metaFieldValueWS.getStringValue(),listType)
        return  productDetailsWSList
    }

    private void checkValidProduct(CompanyWS companyWS, String productIdentifier) throws ProductNotValidException {

        MetaFieldValueWS metaFieldValueWS = companyWS.getMetaFieldByName(APPDIRECT_PRODUCT_DETAILS)
        List<ProductDetailsWS> productDetailsWSList = getProductDetailsFromMetaField(metaFieldValueWS)
        boolean validProduct = false

        for (ProductDetailsWS productDetailsWS : productDetailsWSList) {
            if (productIdentifier.equals(productDetailsWS.getProductIdentifier())) {
                validProduct = true
                break
            }
        }
        if (!validProduct) {
            logger.warn("Not valid Product")
            throw new ProductNotValidException("Product is not valid. This capability should be built on " +
                    "Marketplace side. Potential DDos threat!")
        }
    }
}
