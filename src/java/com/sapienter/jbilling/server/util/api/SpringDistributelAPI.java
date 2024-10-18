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

package com.sapienter.jbilling.server.util.api;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.SpaPlanWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.spa.CustomerEmergency911AddressWS;
import com.sapienter.jbilling.server.spa.SpaHappyFox;
import com.sapienter.jbilling.server.spa.SpaImportWS;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.search.SearchResultString;

public class SpringDistributelAPI implements JbillingDistributelAPI {

    private JbillingDistributelAPI distAPI = null;

    public SpringDistributelAPI() {
        this(RemoteContext.Name.API_CLIENT_DISTRIBUTEL);
    }

    public SpringDistributelAPI(String beanName) {
    	distAPI = (JbillingDistributelAPI) RemoteContext.getBean(beanName);
    }
    
    public SpringDistributelAPI(RemoteContext.Name bean) {
    	distAPI = (JbillingDistributelAPI) RemoteContext.getBean(bean);
    }

    // Distributel APIs

    @Override
    public PlanWS getQuote(Integer planId, String province, String date, String languageId, Integer... optionalPlanIds) {
		return distAPI.getQuote(planId, province, date,languageId, optionalPlanIds);
	}

    @Override
    public String[] getSupportedModemsByPlan(Integer planId) {
        return distAPI.getSupportedModemsByPlan(planId);
    }

    @Override
    public Integer processSpaImport(SpaImportWS spaImportWS) {
        return distAPI.processSpaImport(spaImportWS);
    }
    
    @Override
    public SearchResultString getOptionalPlansSearchResultByPlan(Integer planId){
        return distAPI.getOptionalPlansSearchResultByPlan(planId);
    }
    
    @Override
    public SearchResultString getSupportedModemsSearchResultByPlan(Integer planId){
        return distAPI.getSupportedModemsSearchResultByPlan(planId);
    }
    
    @Override
    public PlanWS[] getOptionalPlansByPlan(Integer planId) throws SessionInternalError {
        return distAPI.getOptionalPlansByPlan(planId);
    }
    
    @Override
    public PlanWS[] getPlans(String provinceCode, String userType){
        return distAPI.getPlans(provinceCode, userType);
    }
    
    @Override
    public SpaPlanWS[] getSpaPlansWS(String province, String userType) {
        return distAPI.getSpaPlansWS(province, userType);
    }

    @Override
    public boolean setFurtherOrderAndAssetInformation(String banffAccountId, String trackingNumber, String courier, String serialNumber, String macAddress, String model, String serviceAssetIdentifier) {
        return distAPI.setFurtherOrderAndAssetInformation(banffAccountId, trackingNumber, courier, serialNumber, macAddress, model, serviceAssetIdentifier);
    }

    @Override
    public Integer processSpaImportInternalProcess(SpaImportWS spaImportWS) {
        return distAPI.processSpaImportInternalProcess(spaImportWS);
    }

    @Override
    public OrderWS[] getOrdersWithActiveUntil(Integer userId, Date activeUntil) {
        return distAPI.getOrdersWithActiveUntil(userId, activeUntil);
    }

    @Override
    public OrderWS[] getOrdersWithOrderChangeEffectiveDate(Integer userId, Date activeSince) {
        return distAPI.getOrdersWithOrderChangeEffectiveDate(userId, activeSince);
    }

    @Override
    public Map<String, BigDecimal> getCanadianTaxDataTableValuesForUser(Integer userId, Date effectiveDate) {
        return distAPI.getCanadianTaxDataTableValuesForUser(userId, effectiveDate);
    }

    @Override
    public List<AssetWS> getAssetsByMetaFieldValue(String mfName, String mfValue) {
        return distAPI.getAssetsByMetaFieldValue(mfName, mfValue);
    }

    @Override
    public List<Integer> findOrderIdsByAssetIds(List<Integer> assetIds) {
        return distAPI.findOrderIdsByAssetIds(assetIds);
    }

	@Override
	public CustomerEmergency911AddressWS getCustomerEmergency911AddressByPhoneNumber(String phoneNumber) {
		return distAPI.getCustomerEmergency911AddressByPhoneNumber(phoneNumber);
	}

    @Override
    public SpaHappyFox getCustomerTicketInfoResult(SpaHappyFox spaHappyFox){
        return distAPI.getCustomerTicketInfoResult(spaHappyFox);
    }
}
