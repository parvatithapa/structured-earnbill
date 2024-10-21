package com.sapienter.jbilling.test.framework.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;

/**
 * Created by marcomanzicore on 26/11/15.
 */
public class AccountTypeBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AccountTypeBuilder.class);
    private String description;
    private Boolean useExactDescription = false;
    private String invoiceDesign;
    private String creditLimit;
    private String creditNotificationLimit1;
    private String creditNotificationLimit2;
    private Integer[] paymentMethodTypeIds;
    private Integer entityId;
    private MainSubscriptionWS mainSubscription;
    public static final Integer MONTHLY_PERIOD = Integer.valueOf(2);//fixed constant for now
    public static final Integer DEFAULT_INVOICE_DELIVERY_METHOD = Integer.valueOf(1);
    private Map<String, List<MetaFieldWS>> accountInformationTypeName = new HashMap<>();
    private List<MetaFieldWS> accountInformationTypeMetaFields = new ArrayList<>();

    private AccountTypeBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public static AccountTypeBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        return new AccountTypeBuilder(api, testEnvironment);
    }

    public AccountTypeBuilder withName(String description) {
        this.description = description;
        return this;
    }


    public AccountTypeBuilder useExactDescription(Boolean useExactDescription) {
        this.useExactDescription = useExactDescription;
        return this;
    }

    public AccountTypeBuilder withInvoiceDesign(String invoiceDesign){
        this.invoiceDesign = invoiceDesign;
        return this;
    }

    public AccountTypeBuilder withCreditLimit(String creditLimit){
        this.creditLimit = creditLimit;
        return this;
    }

    public AccountTypeBuilder withCreditNotificationLimit1(String creditNotificationLimit1){
        this.creditNotificationLimit1 = creditNotificationLimit1;
        return this;
    }

    public AccountTypeBuilder withCreditNotificationLimit2(String creditNotificationLimit2){
        this.creditNotificationLimit2 = creditNotificationLimit2;
        return this;
    }

    public AccountTypeBuilder withPaymentMethodTypeIds(Integer[] paymentMethodTypeIds){
        this.paymentMethodTypeIds = paymentMethodTypeIds;
        return this;
    }

    public AccountTypeBuilder withEntityId(Integer entityId){
        this.entityId = entityId;
        return this;
    }

    public AccountTypeBuilder addAccountInformationType(String accountInformationTypeName, Map<String, DataType> informationTypeMetaFields) {
    	List<MetaFieldWS> listMetaField = new ArrayList<>();
    	informationTypeMetaFields.forEach((key, value) -> {
    		
    		if (key.equals("PO Box")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.POST_BOX, null));
    		}
    		if (key.equals("Country")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.COUNTRY_CODE, null));
    		}
    		if (key.equals("Post Code")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.POSTAL_CODE, null));
    		}
    		if (key.equals("State")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.STATE_PROVINCE, null));
    		}
    		if (key.equals("Street Name")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.STREET_NAME, null));
    		}
    		if (key.equals("City")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.CITY, null));
    		}
    		if (key.equals("Street Number")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.STREET_NUMBER, null));
    		}
    		if (key.equals("Title")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.TITLE, null));
    		}
    		if (key.equals("First Name")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.FIRST_NAME, null));
    		}
    		if (key.equals("Last Name")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.LAST_NAME, null));
    		}
    		if (key.equals("Business Name")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.BUSINESS_NAME, null));
    		}
    		if (key.equals("Date of Birth")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.DATE, null));
    		}
    		if (key.equals("Email Address")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.EMAIL, null));
    		}
    		if (key.equals("Contact Number")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.PHONE_NUMBER, null));
    		}
    		if (key.equals("Street Type")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.STREET_TYPE, null));
    		}
    		if (key.equals("direct_marketing")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), null, null));
    		}
    		if (key.equals("Sub Premises")) {
    			listMetaField.add(ApiBuilderHelper.getMetaFieldWithValidationRule(key, value, EntityType.ACCOUNT_TYPE, api.getCallerCompanyId(), MetaFieldType.SUB_PREMISES, null));
    		}
    	});
    	
    	if (listMetaField.isEmpty()) {
			this.accountInformationTypeName.put(accountInformationTypeName, informationTypeMetaFields.entrySet().stream().map(entry ->
	        ApiBuilderHelper.getMetaFieldWS(entry.getKey(), entry.getValue(), EntityType.ACCOUNT_TYPE, api.getCallerCompanyId()))
	        .collect(Collectors.toList()));
		} else {
			this.accountInformationTypeName.put(accountInformationTypeName, listMetaField);
		}
    	
        return this;
    }

    public AccountTypeBuilder withMainSubscription(Integer periodId, Integer dayOfPeriod){
        this.mainSubscription = new MainSubscriptionWS(periodId, dayOfPeriod);
        return this;
    }

    public AccountTypeWS build() {
        // If customer or enrollment is created with account type, it is not possible to delete account type.
        // So, once account type is created and need to create a new one with same name, it will return the already existing account type.
        AccountTypeWS[] accountTypes = api.getAllAccountTypes();
        if (accountTypes != null && accountTypes.length > 0) {
            Optional searchObject = Arrays.stream(accountTypes).filter(accountType -> accountType.getDescription(api.getCallerLanguageId()).getContent().equals(description)).findFirst();
            if (searchObject.isPresent()) {
                AccountTypeWS accountType = (AccountTypeWS) searchObject.get();
                logger.debug("Account type is already exist : {}",  accountType.getDescription(api.getCallerLanguageId()).getContent());
                testEnvironment.add(description, accountType.getId(), accountType.getDescription(api.getCallerLanguageId()).getContent(), api, TestEntityType.ACCOUNT_TYPE);
                return accountType;
            }
        }
        AccountTypeWS accountType = new AccountTypeWS();
        accountType.setEntityId(api.getCallerCompanyId());
        accountType.setLanguageId(api.getCallerLanguageId());
        accountType.setCurrencyId(api.getCallerCurrencyId());
        accountType.setEntityId(entityId);
        accountType.setInvoiceDeliveryMethodId(DEFAULT_INVOICE_DELIVERY_METHOD);
        accountType.setMainSubscription(null == mainSubscription ? new MainSubscriptionWS(MONTHLY_PERIOD, 1)
        : mainSubscription);
        accountType.setDescriptions(createDescriptions());
        accountType.setInvoiceDesign(invoiceDesign);
        accountType.setCreditLimit(creditLimit);
        accountType.setCreditNotificationLimit1(creditNotificationLimit1);
        accountType.setCreditNotificationLimit2(creditNotificationLimit2);
        accountType.setPaymentMethodTypeIds(paymentMethodTypeIds);
        Integer accountTypeId = api.createAccountType(accountType);
        AccountTypeWS accountTypeCreated = api.getAccountType(accountTypeId);
        testEnvironment.add(description, accountTypeId, accountTypeCreated.getDescription(accountType.getLanguageId()).getContent(), api, TestEntityType.ACCOUNT_TYPE);

        accountInformationTypeName.forEach((accountInformationTypeName, metaFields) ->  {
            if (metaFields.size() == 0) {
                throw new IllegalArgumentException("Account type with Information type should have at least one metafield");
            }
            createInformationType(accountTypeCreated, accountInformationTypeName, metaFields);
        });

        return accountTypeCreated;
    }

    private void createInformationType(AccountTypeWS accountTypeCreated, String accountInformationTypeName, List<MetaFieldWS> metaFields) {
        AccountInformationTypeWS accountInformationTypes = new AccountInformationTypeWS();
        accountInformationTypes.setEntityId(api.getCallerCompanyId());
        accountInformationTypes.setName(accountInformationTypeName);
        accountInformationTypes.setAccountTypeId(accountTypeCreated.getId());
        accountInformationTypes.setMetaFields(metaFields.toArray(new MetaFieldWS[0]));
        Integer accountInformationTypeId = api.createAccountInformationType(accountInformationTypes);

        testEnvironment.add(accountInformationTypeName, accountInformationTypeId, accountInformationTypeName, api,
                TestEntityType.ACCOUNT_INFORMATION_TYPE);
        accountTypeCreated.setInformationTypeIds(Arrays.asList(accountInformationTypeId).toArray(new Integer[0]));
        api.updateAccountType(accountTypeCreated);
    }

    private List<InternationalDescriptionWS> createDescriptions() {
        Integer apiLanguageId = api.getCallerLanguageId();
        return Arrays.asList(new InternationalDescriptionWS(apiLanguageId, useExactDescription?description:description + System.currentTimeMillis()));
    }
}
