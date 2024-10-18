package com.sapienter.jbilling.test.framework;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.builders.*;
import com.sapienter.jbilling.test.framework.builders.nges.EDIFileBuilder;
import com.sapienter.jbilling.test.framework.builders.nges.NGESBuilder;
import com.sapienter.jbilling.test.framework.builders.nges.NGESEnrollmentBuilder;

import java.util.Map;

/**
 * Created by marcolin on 06/11/15.
 */
public class TestEnvironmentBuilder extends AbstractTestEnvironment {

    private TestEnvironment testEnvironment;

    TestEnvironmentBuilder(TestEnvironment testEnvironment) {
        this.testEnvironment = testEnvironment;
    }

    public AccountTypeBuilder accountTypeBuilder(JbillingAPI api) {
        return AccountTypeBuilder.getBuilder(api, testEnvironment);
    }

    public ConfigurationBuilder configurationBuilder(JbillingAPI api) {
        return ConfigurationBuilder.getBuilder(api, testEnvironment);
    }

    public CustomerBuilder customerBuilder(JbillingAPI api) {
        return CustomerBuilder.getBuilder(api, testEnvironment);
    }

    public ItemBuilder itemBuilder(JbillingAPI api) {
        return ItemBuilder.getBuilder(api, testEnvironment);
    }

    public MediationConfigBuilder mediationConfigBuilder(JbillingAPI api) {
        return MediationConfigBuilder.getBuilder(api, testEnvironment);
    }

    public OrderBuilder orderBuilder(JbillingAPI api) {
        return OrderBuilder.getBuilder(api, testEnvironment);
    }

    public OrderChangeStatusBuilder orderChangeStatusBuilder(JbillingAPI api) {
        return OrderChangeStatusBuilder.getBuilder(api, testEnvironment);
    }

    public OrderPeriodBuilder orderPeriodBuilder(JbillingAPI api) {
        return OrderPeriodBuilder.getBuilder(api, testEnvironment);
    }

    public PaymentMethodTypeBuilder paymentMethodTypeBuilder(JbillingAPI api, String code) {
        return PaymentMethodTypeBuilder.getBuilder(api, testEnvironment, code);
    }

    public UsagePoolBuilder usagePoolBuilder(JbillingAPI api, String code) {
        return UsagePoolBuilder.getBuilder(api, testEnvironment, code);
    }

    public PlanBuilder planBuilder(JbillingAPI api, String code) {
        return PlanBuilder.getBuilder(api, testEnvironment, code);
    }

    public DiscountBuilder discountBuilder(JbillingAPI api) {
        return DiscountBuilder.getBuilder(api, testEnvironment);
    }

    public AssetBuilder assetBuilder(JbillingAPI api) {
        return AssetBuilder.getBuilder(api, testEnvironment);
    }

    public EDITypeBuilder ediTypeBuilder(JbillingAPI api){
        return new EDITypeBuilder(api,testEnvironment);
    }

    public NGESBuilder ngesBuilder(){
        return new NGESBuilder(this);
    }

    public CompanyBuilder companyBuilder(JbillingAPI api){
        return new CompanyBuilder(api, testEnvironment);
    }

    public Integer idForCode(String testCode) {
        return testEnvironment.idForCode(testCode);
    }

    public NGESEnrollmentBuilder enrollmentBuilder(JbillingAPI api){
        return new NGESEnrollmentBuilder(api, testEnvironment);
    }

    public PluginBuilder pluginBuilder(JbillingAPI api){
        return new PluginBuilder(api, testEnvironment);
    }

    public EDIFileBuilder ediFileBuilder(JbillingAPI api) {
        return new EDIFileBuilder(api, testEnvironment);
    }

    public void setCompanyLevelMetaFields(Map<JbillingAPI, MetaFieldValueWS[]> values) {
        testEnvironment.companyMetaFields = values;
    }

    public TestEnvironment env() {
        return testEnvironment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestEnvironmentBuilder that = (TestEnvironmentBuilder) o;

        return !(testEnvironment != null ? !testEnvironment.equals(that.testEnvironment) : that.testEnvironment != null);

    }

    @Override
    public int hashCode() {
        return testEnvironment != null ? testEnvironment.hashCode() : 0;
    }
}
