package com.sapienter.jbilling.server.util.api;

import com.sapienter.jbilling.server.util.RemoteContext;

/**
 * Created by wajeeha on 5/12/18.
 */
public class SpringDeutscheTelecomAPI implements JbillingDeutscheTelecomAPI {

    private JbillingDeutscheTelecomAPI dtAPI = null;

    public SpringDeutscheTelecomAPI() {
        this(RemoteContext.Name.API_CLIENT_DEUTSCHE_TELECOM);
    }

    public SpringDeutscheTelecomAPI(String beanName) {
        dtAPI = (JbillingDeutscheTelecomAPI) RemoteContext.getBean(beanName);
    }

    public SpringDeutscheTelecomAPI(RemoteContext.Name bean) {
        dtAPI = (JbillingDeutscheTelecomAPI) RemoteContext.getBean(bean);
    }

    // Deutsche Telecom APIs

    @Override
    public Long uploadDefaultPrices(String sourceFilePath, String errorFilePath) {
        return dtAPI.uploadDefaultPrices(sourceFilePath,errorFilePath);
    }

    @Override
    public Long uploadAccountTypePrices(String sourceFilePath, String errorFilePath) {
        return dtAPI.uploadAccountTypePrices(sourceFilePath,errorFilePath);
    }

    @Override
    public Long uploadCustomerLevelPrices(String sourceFilePath, String errorFilePath) {
        return dtAPI.uploadCustomerLevelPrices(sourceFilePath,errorFilePath);
    }

    @Override
    public Long uploadPlanPrices(String sourceFilePath, String errorFilePath) {
        return dtAPI.uploadPlanPrices(sourceFilePath,errorFilePath);
    }

    @Override
    public Long downloadDefaultPrices(String sourceFilePath, String errorFilePath) {
        return dtAPI.downloadDefaultPrices(sourceFilePath,errorFilePath);
    }

    @Override
    public Long downloadAccountLevelPrices(String sourceFilePath, String errorFilePath) {
        return dtAPI.downloadAccountLevelPrices(sourceFilePath,errorFilePath);
    }

    @Override
    public Long downloadCustomerLevelPrices(String sourceFilePath, String errorFilePath) {
        return dtAPI.downloadCustomerLevelPrices(sourceFilePath,errorFilePath);
    }

    @Override
    public Long downloadPlans(String sourceFilePath, String errorFilePath) {
        return dtAPI.downloadPlans(sourceFilePath,errorFilePath);
    }

    @Override
    public Long downloadIndividualDefaultPrice(String sourceFilePath,String errorFilePath, String productCode){
        return dtAPI.downloadIndividualDefaultPrice(sourceFilePath, errorFilePath, productCode);
    }

    @Override
    public Long downloadIndividualAccountLevelPrice(String sourceFilePath,String errorFilePath, String accountId){
        return dtAPI.downloadIndividualAccountLevelPrice(sourceFilePath, errorFilePath, accountId);
    }

    @Override
    public Long downloadIndividualCustomerLevelPrice(String sourceFilePath,String errorFilePath, String accountIdentificationCode){
        return dtAPI.downloadIndividualCustomerLevelPrice(sourceFilePath, errorFilePath, accountIdentificationCode);
    }

    @Override
    public Long downloadIndividualPlan(String sourceFilePath,String errorFilePath, String planNumber){
        return dtAPI.downloadIndividualPlan(sourceFilePath, errorFilePath, planNumber);
    }
}

