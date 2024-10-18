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

import javax.jws.WebService;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.SpaPlanWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.spa.CustomerEmergency911AddressWS;
import com.sapienter.jbilling.server.spa.SpaHappyFox;
import com.sapienter.jbilling.server.spa.SpaImportWS;
import com.sapienter.jbilling.server.util.search.SearchResultString;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@WebService(targetNamespace = "http://jbilling/", name = "distributelApiService")
public interface JbillingDistributelAPI {

    // Distributel APIs

    public PlanWS getQuote(Integer planId, String province, String date, String languageId, Integer... optionalPlanIds);
    public String[] getSupportedModemsByPlan(Integer planId);
    public Integer processSpaImport(SpaImportWS spaImportWS);
    public SearchResultString getOptionalPlansSearchResultByPlan(Integer planId);
    public SearchResultString getSupportedModemsSearchResultByPlan(Integer planId);
    public PlanWS[] getOptionalPlansByPlan(Integer planId) throws SessionInternalError;
    public PlanWS[] getPlans(String provinceCode, String userType);
    public SpaPlanWS[] getSpaPlansWS(String province, String userType);
    public boolean setFurtherOrderAndAssetInformation(String banffAccountId, String trackingNumber, String courier, String serialNumber, String macAddress, String model, String serviceAssetIdentifier);
    public Integer processSpaImportInternalProcess(SpaImportWS spaImportWS);
    public OrderWS[] getOrdersWithActiveUntil(Integer userId, Date activeUntil);
    public OrderWS[] getOrdersWithOrderChangeEffectiveDate(Integer userId, Date activeSince);
    public Map<String, BigDecimal> getCanadianTaxDataTableValuesForUser(Integer userId, Date effectiveDate);
    public List<AssetWS> getAssetsByMetaFieldValue(String mfName, String mfValue);
    public List<Integer> findOrderIdsByAssetIds(List<Integer> assetIds);
    public CustomerEmergency911AddressWS getCustomerEmergency911AddressByPhoneNumber(String phoneNumber);
    public SpaHappyFox getCustomerTicketInfoResult(SpaHappyFox spaHappyFox);

}
