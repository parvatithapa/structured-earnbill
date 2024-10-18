package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.builders.nges.NGESAccountTypeBuild;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;
import com.sapienter.jbilling.test.framework.TestEntityType;

import java.util.*;

/**
 * Created by marcolin on 06/11/15.
 * modified by Sumit Bhatt
 */
public class CustomerBuilder extends AbstractMetaFieldBuilder<CustomerBuilder> {

    private static final Integer DEFAULT_ACCOUNT_TYPE_ID = Integer.valueOf(1);
    private String username;
    private String password;
    private boolean addTimeToUsername = true;
    private List<PaymentInformationWS> paymentInstruments = new ArrayList<>();
    private Integer accountTypeId = DEFAULT_ACCOUNT_TYPE_ID;
    private Integer parentId;
    private Date nextInvoiceDate;
    private MainSubscriptionWS mainSubscription;
    private Boolean isParent = Boolean.FALSE;
    private Integer currencyId ;
    private Boolean createCredentials = Boolean.FALSE;
    private JbillingAPI api;
    private TestEnvironment testEnvironment;
	private String invoiceDesign;
	private Integer invoiceDeliveryMethodId;

    public CustomerBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        this.api = api;
        this.testEnvironment = testEnvironment;
    }

    public static CustomerBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment){
        return new CustomerBuilder(api, testEnvironment);
    }

    public CustomerBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public CustomerBuilder addPaymentInstrument(PaymentInformationWS paymentInstrument){
        this.paymentInstruments.add(paymentInstrument);
        return this;
    }

    public CustomerBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public CustomerBuilder withPaymentInstruments(List<PaymentInformationWS> paymentInstruments){
        this.paymentInstruments = paymentInstruments;
        return this;
    }
    public CustomerBuilder withAccountTypeId(Integer accountTypeId){
        this.accountTypeId = accountTypeId;
        return this;
    }

    public CustomerBuilder withAITGroup(NGESAccountTypeBuild.AIT aitGroup, Map<String, Object> values) {

        AccountInformationTypeWS[] aits = api.getInformationTypesForAccountType(accountTypeId);
        Optional searchObject = Arrays.stream(aits).filter(ait -> ait.getName().equals(aitGroup.getName())).findFirst();
        if (!searchObject.isPresent()) {
            throw new IllegalStateException("No valid AIT found");
        }
        final AccountInformationTypeWS accountInformation = (AccountInformationTypeWS) searchObject.get();
        values.entrySet().stream().forEach(e-> withMetaField(e.getKey(), e.getValue(), accountInformation.getId()));
        return this;
    }
    
    public CustomerBuilder withAITGroup(String aitGroupName , Map<String, Object> values) {

        AccountInformationTypeWS[] aits = api.getInformationTypesForAccountType(accountTypeId);
        Optional searchObject = Arrays.stream(aits).filter(ait -> ait.getName().equals(aitGroupName)).findFirst();
        if (!searchObject.isPresent()) {
            throw new IllegalStateException("No valid AIT found");
        }
        final AccountInformationTypeWS accountInformation = (AccountInformationTypeWS) searchObject.get();
        values.entrySet().stream().forEach(e-> withMetaField(e.getKey(), e.getValue(), accountInformation.getId()));
        return this;
    }

    public CustomerBuilder withParentId(Integer parentId) {

        this.parentId = parentId;
        return this;
    }

    public CustomerBuilder withNextInvoiceDate(Date nextInvoiceDate) {

        this.nextInvoiceDate = nextInvoiceDate;
        return this;
    }

    public CustomerBuilder withMainSubscription(MainSubscriptionWS mainSubscription) {

        this.mainSubscription = mainSubscription;
        return this;
    }

    public CustomerBuilder withIsParent(Boolean isParent) {
        this.isParent = isParent;
        return this;
    }
    
    public CustomerBuilder withCurrency(Integer currencyId) {
        this.currencyId = currencyId;
        return this;
    }

    public CustomerBuilder withInvoiceDesign(String invoiceDesign) {
        this.invoiceDesign = invoiceDesign;
        return this;
    }

    public CustomerBuilder withInvoiceDeliveryMethod(Integer invoiceDeliveryMethodId) {
        this.invoiceDeliveryMethodId = invoiceDeliveryMethodId;
        return this;
    }

    public UserWS build() {
        UserWS newUser = new UserWS();
        newUser.setIsParent(isParent);
        newUser.setInvoiceDesign(invoiceDesign);
        newUser.setUserId(0);
        newUser.setInvoiceDeliveryMethodId(invoiceDeliveryMethodId);
        if (addTimeToUsername)
            newUser.setUserName(username + System.currentTimeMillis());
        else
            newUser.setUserName(username);

        if (null != password) {
            newUser.setCreateCredentials(true);
            newUser.setPassword(password);
        }
        newUser.setLanguageId(api.getCallerLanguageId());
        newUser.setMainRoleId(Constants.ROLE_CUSTOMER);
        newUser.setAccountTypeId(accountTypeId);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        if (null != currencyId) {
            newUser.setCurrencyId(currencyId);
        } else {
            newUser.setCurrencyId(api.getCallerCurrencyId());
        }
        newUser.setParentId(parentId);

        if (null != nextInvoiceDate) {
            newUser.setNextInvoiceDate(nextInvoiceDate);
        }
        if (null != mainSubscription) {
            newUser.setMainSubscription(mainSubscription);
        }
        if (accountTypeId.intValue() == DEFAULT_ACCOUNT_TYPE_ID.intValue()){
            newUser.setMetaFields(new MetaFieldValueWS[]{ApiBuilderHelper.getMetaFieldValueWS("contact.email", newUser.getUserName() + "@test.com")});
        }else {
            newUser.setMetaFields(buildMetaField());
        }
        if (paymentInstruments.size() != 0){
            newUser.setPaymentInstruments(paymentInstruments);
        }
        newUser = api.getUserWS(api.createUser(newUser));
        testEnvironment.add(username, newUser.getUserId(), newUser.getUserName(), api, TestEntityType.CUSTOMER);
        return newUser;
    }

    public CustomerBuilder addTimeToUsername(boolean addTimeToUsername) {
        this.addTimeToUsername = addTimeToUsername;
        return this;
    }

    public PaymentInformationBuilder paymentInformation(){
        return new PaymentInformationBuilder();
    }

    public class PaymentInformationBuilder {

        private Integer userId;
        private Integer processingOrder;
        private Integer paymentMethodTypeId;
        private Integer paymentMethodId;
        private List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();

        public PaymentInformationBuilder withUserId(Integer userId){
            this.userId = userId;
            return this;
        }

        public PaymentInformationBuilder withProcessingOrder(Integer processingOrder){
            this.processingOrder = processingOrder;
            return this;
        }

        public PaymentInformationBuilder withPaymentMethodTypeId(Integer paymentMethodTypeId){
            this.paymentMethodTypeId = paymentMethodTypeId;
            return this;
        }

        public PaymentInformationBuilder withPaymentMethodId(Integer paymentMethodId){
            this.paymentMethodId = paymentMethodId;
            return this;
        }

        public PaymentInformationBuilder addMetaFieldValue(MetaFieldValueWS metaFieldValue){
            this.metaFieldValues.add(metaFieldValue);
            return this;
        }

        public PaymentInformationBuilder withMetaFieldValues(List<MetaFieldValueWS> metaFieldValues){
            this.metaFieldValues = metaFieldValues;
            return this;
        }

        public PaymentInformationWS build(){

            PaymentInformationWS paymentInformation = new PaymentInformationWS();
            paymentInformation.setUserId(userId);
            paymentInformation.setPaymentMethodId(paymentMethodId);
            paymentInformation.setPaymentMethodTypeId(paymentMethodTypeId);
            paymentInformation.setProcessingOrder(processingOrder);
            paymentInformation.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[metaFieldValues.size()]));

            return paymentInformation;
        }

    }
}
