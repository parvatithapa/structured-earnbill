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

import java.io.File;
import java.util.Date;
import java.util.UUID;

import groovy.transform.CompileStatic

import com.sapienter.jbilling.server.diameter.DiameterResultWS;
import com.sapienter.jbilling.server.item.ItemDTOEx
import com.sapienter.jbilling.server.item.PricingField
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.order.OrderLineWS
import com.sapienter.jbilling.server.order.OrderWS
import com.sapienter.jbilling.server.sql.api.*;
import com.sapienter.jbilling.server.sql.api.db.*;
import com.sapienter.jbilling.server.user.UserCodeWS;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS
import com.sapienter.jbilling.server.util.IWebServicesSessionBean
import com.sapienter.jbilling.server.util.api.JbillingAPI

/**
 * Grails managed remote service bean for exported web-services. This bean delegates to
 * the WebServicesSessionBean just like the core JbillingAPI.
 */
@CompileStatic
class jBillingSalesforceService implements JbillingAPI {

    @Delegate(interfaces = false, methodAnnotations = true, includeTypes = JbillingAPI)
    IWebServicesSessionBean webServicesSession

    static transactional = true

    static expose = ['cxfjax']

    public OrderWS updateCurrentOrder(Integer userId, OrderLineWS[] lines, PricingField[] fields, Date date,
            String eventDescription) {

        return webServicesSession.updateCurrentOrder(userId, lines, PricingField.setPricingFieldsValue(fields), date,
                eventDescription);
    }

    public ValidatePurchaseWS validatePurchase(Integer userId, Integer itemId, PricingField[] fields) {
        return webServicesSession.validatePurchase(userId, itemId, PricingField.setPricingFieldsValue(fields));
    }

    public ValidatePurchaseWS validateMultiPurchase(Integer userId, Integer[] itemIds, PricingField[][] fields) {
        String[] pricingFields = null;
        if (fields != null) {
            pricingFields = new String[fields.length];
            for (int i = 0; i < pricingFields.length; i++) {
                pricingFields[i] = PricingField.setPricingFieldsValue(fields[i]);
            }
        }
        return webServicesSession.validateMultiPurchase(userId, itemIds, pricingFields);
    }

    public ItemDTOEx getItem(Integer itemId, Integer userId, PricingField[] fields) {
        return webServicesSession.getItem(itemId, userId, PricingField.setPricingFieldsValue(fields));
    }

    public void setAutoPaymentType(Integer userId, Integer autoPaymentType, boolean use) {
        webServicesSession.setAuthPaymentType(userId, autoPaymentType, use)
    }

    public Integer getAutoPaymentType(Integer userId) {
        return webServicesSession.getAuthPaymentType(userId)
    }

    public DiameterResultWS createSession(String sessionId, Date timestamp, BigDecimal units,
            PricingField[] data) {
        return webServicesSession.createSession(sessionId, timestamp, units, PricingField.setPricingFieldsValue(data))
    }

    public DiameterResultWS reserveUnits(String sessionId, Date timestamp, int units,
            PricingField[] data) {
        return webServicesSession.reserveUnits(sessionId, timestamp, units, PricingField.setPricingFieldsValue(data))
    }

    public DiameterResultWS updateSession(String sessionId, Date timestamp, BigDecimal usedUnits,
            BigDecimal reqUnits, PricingField[] data) {
        return webServicesSession.updateSession(sessionId, timestamp, usedUnits, reqUnits, PricingField.setPricingFieldsValue(data))
    }

    public UUID runRecycleForProcess(UUID processId) {
        return webServicesSession.runRecycleForMediationProcess(processId)
    }

    public UUID triggerMediationByConfigurationByFile(Integer cfgId, File file) {
	return webServicesSession.triggerMediationByConfigurationByFile(cfgId, file);
    }
	
    public JbillingMediationRecord[] getMediationRecordsByMediationProcessAndStatus(String mediationProcessId, Integer statusId) {
	return webServicesSession.getMediationRecordsByMediationProcessAndStatus(mediationProcessId, statusId);
    }

    public JbillingMediationRecord[] getMediationRecordsByStatusAndCdrType(UUID mediationProcessId, Integer page, Integer size, Date startDate, Date endDate, String status, String cdrType) {
        return webServicesSession.getMediationRecordsByStatusAndCdrType(mediationProcessId, page, size, startDate, endDate, status, cdrType);
	}

    public JbillingMediationErrorRecord[] getErrorsByMediationProcess(String mediationProcessId, int offset, int limit) {
	return webServicesSession.getErrorsByMediationProcess(mediationProcessId, offset, limit);
    }

    public QueryResultWS getQueryResult(String queryCode, QueryParameterWS[] parameters, Integer limit, Integer offSet) {
	return webServicesSession.getQueryResult(queryCode, parameters, limit, offSet);
    }

    public QueryParameterWS[] getParametersByQueryCode(String queryCode) {
	return webServicesSession.getParametersByQueryCode(queryCode);
    }
}

