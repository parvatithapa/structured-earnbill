package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentMethodHelper;
import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Payment Method Type Builder,
 * currently can be used to create
 * three 'templates' payment method types
 * CC, ACH and Cheque.
 * Also can be used for creating
 * payment method types from scratch.
 *
 * @author Vojislav Stanojevikj
 * @since 10-JUN-2016.
 */
public class PaymentMethodTypeBuilder extends AbstractBuilder {

    private String code;
    private String methodName;
    private boolean isRecurring = false;
    private boolean allAccountType = false;
    private Integer templateId;
    private Integer owningEntityId;
    private List<Integer> accountTypes = new ArrayList<>();
    private List<MetaFieldWS> metaFields = new ArrayList<>();

    private PaymentMethodTypeBuilder(JbillingAPI api, TestEnvironment testEnvironment, String code){
        super(api, testEnvironment);
        this.code = code;
    }

    public static PaymentMethodTypeBuilder getBuilder(
            JbillingAPI api, TestEnvironment testEnvironment, String code){
        return new PaymentMethodTypeBuilder(api, testEnvironment, code);
    }

    public PaymentMethodTypeBuilder isRecurring(boolean isRecurring){
        this.isRecurring = isRecurring;
        return this;
    }

    public PaymentMethodTypeBuilder withMethodName(String methodName){
        this.methodName = methodName;
        return this;
    }

    public PaymentMethodTypeBuilder allAccountType(boolean allAccountType){
        this.allAccountType = allAccountType;
        return this;
    }

    public PaymentMethodTypeBuilder withTemplateId(Integer templateId){
        this.templateId = templateId;
        return this;
    }

    public PaymentMethodTypeBuilder withOwningEntityId(Integer owningEntityId){
        this.owningEntityId = owningEntityId;
        return this;
    }

    public PaymentMethodTypeBuilder addAccountType(Integer accountTypeId){
        this.accountTypes.add(accountTypeId);
        return this;
    }

    public PaymentMethodTypeBuilder withAccountTypes(List<Integer> accountTypes){
        this.accountTypes = accountTypes;
        return this;
    }

    public PaymentMethodTypeBuilder addMetaField(MetaFieldWS metaField){
        this.metaFields.add(metaField);
        return this;
    }

    public PaymentMethodTypeBuilder withMetaFields(List<MetaFieldWS> metaFields){
        this.metaFields = metaFields;
        return this;
    }

    public PaymentMethodTypeWS build(){

        PaymentMethodTypeWS paymentMethodType = new PaymentMethodTypeWS();
        paymentMethodType.setAllAccountType(allAccountType);
        MetaFieldWS[] metaFields = this.metaFields.toArray(new MetaFieldWS[this.metaFields.size()]);
        if (null != templateId){
            PaymentMethodTemplateWS paymentMethodTemplateWS = api.getPaymentMethodTemplate(templateId);
            Set<MetaFieldWS> templateMetaFields = paymentMethodTemplateWS.getMetaFields();
            if (templateMetaFields != null && templateMetaFields.size() > 0) {
                metaFields = new MetaFieldWS[templateMetaFields.size()];
                Integer i = 0;
                for (MetaFieldWS metaField : templateMetaFields) {
                    MetaFieldWS mf = copyMetaField(metaField);
                    mf.setEntityId(owningEntityId);
                    mf.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
                    metaFields[i] = mf;
                    i++;
                }
            }
        }
        paymentMethodType.setMetaFields(metaFields);
        paymentMethodType.setAccountTypes(accountTypes);
        paymentMethodType.setOwningEntityId(owningEntityId);
        paymentMethodType.setTemplateId(templateId);
        paymentMethodType.setMethodName(methodName);
        paymentMethodType.setIsRecurring(isRecurring);
        return persistEntityAndUpdateEnvironment(paymentMethodType, code);

    }

    public PaymentMethodTypeWS buildCCPaymentMethodType(){
        PaymentMethodTypeWS paymentMethodType = PaymentMethodHelper.buildCCTemplateMethod(api);
        paymentMethodType.setIsRecurring(isRecurring);
        return persistEntityAndUpdateEnvironment(paymentMethodType, code);
    }

    public PaymentMethodTypeWS buildACHPaymentMethodType(){
        PaymentMethodTypeWS paymentMethodType = PaymentMethodHelper.buildACHTemplateMethod(api);
        paymentMethodType.setIsRecurring(isRecurring);
        return persistEntityAndUpdateEnvironment(paymentMethodType, code);
    }

    public PaymentMethodTypeWS buildChequePaymentMethodType(){
        PaymentMethodTypeWS paymentMethodType = PaymentMethodHelper.buildChequeTemplateMethod(api);
        paymentMethodType.setIsRecurring(isRecurring);
        return persistEntityAndUpdateEnvironment(paymentMethodType, code);
    }

    private PaymentMethodTypeWS persistEntityAndUpdateEnvironment(PaymentMethodTypeWS paymentMethodType, String code){
        Integer paymentMethodTypeId = api.createPaymentMethodType(paymentMethodType);
        paymentMethodType = api.getPaymentMethodType(paymentMethodTypeId);
        testEnvironment.add(code, paymentMethodTypeId,
                paymentMethodType.getMethodName(), api, TestEntityType.PAYMENT_METHOD_TYPE);
        return paymentMethodType;
    }

    private MetaFieldWS copyMetaField(MetaFieldWS metaField) {
        MetaFieldWS mf = new MetaFieldWS();
        mf.setDataType(metaField.getDataType());
        mf.setDefaultValue(metaField.getDefaultValue());
        mf.setDisabled(metaField.isDisabled());
        mf.setDisplayOrder(metaField.getDisplayOrder());
        mf.setFieldUsage(metaField.getFieldUsage());
        mf.setFilename(metaField.getFilename());
        mf.setMandatory(metaField.isMandatory());
        mf.setName(metaField.getName());
        mf.setValidationRule(metaField.getValidationRule());
        mf.setPrimary(metaField.isPrimary());

        // set rule id to 0 so a new rule will be created
        if (mf.getValidationRule() != null) {
            mf.getValidationRule().setId(0);
        }
        return mf;
    }

}
