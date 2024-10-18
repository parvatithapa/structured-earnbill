package com.sapienter.jbilling.server.util.api;

import javax.jws.WebService;

/**
 * Created by wajeeha on 5/12/18.
 */
@WebService(targetNamespace = "http://jbilling/", name = "deutscheTelecomApiService")
public interface JbillingDeutscheTelecomAPI {

    // Deutsche Telecom APIs

    public Long uploadDefaultPrices(String sourceFilePath,String errorFilePath);
    public Long uploadAccountTypePrices(String sourceFilePath,String errorFilePath);
    public Long uploadCustomerLevelPrices(String sourceFilePath,String errorFilePath);
    public Long uploadPlanPrices(String sourceFilePath,String errorFilePath);
    public Long downloadDefaultPrices(String sourceFilePath,String errorFilePath);
    public Long downloadAccountLevelPrices(String sourceFilePath,String errorFilePath);
    public Long downloadCustomerLevelPrices(String sourceFilePath,String errorFilePath);
    public Long downloadPlans(String sourceFilePath,String errorFilePath);
    public Long downloadIndividualDefaultPrice(String sourceFilePath,String errorFilePath, String productCode);
    public Long downloadIndividualAccountLevelPrice(String sourceFilePath,String errorFilePath, String accountId);
    public Long downloadIndividualCustomerLevelPrice(String sourceFilePath,String errorFilePath, String accountIdentificationCode);
    public Long downloadIndividualPlan(String sourceFilePath,String errorFilePath, String planNumber);
}