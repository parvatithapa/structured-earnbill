/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.steps;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FC specific JMR pricing resolution step with custom fields.
 *
 * @author Krunal
 * @since 03/07/16
 */
public class JMRPricingResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(JMRPricingResolutionStep.class));
    private static final String DEFAULT_CSV_FILE_SEPARATOR = ",";

    private MediationHelperService mediationHelperService;

    public MediationHelperService getMediationHelperService() {
		return mediationHelperService;
	}

	public void setMediationHelperService(
			MediationHelperService mediationHelperService) {
		this.mediationHelperService = mediationHelperService;
	}

	/**
	 * populates city, state and country in pricing fields
	 * @param fields
	 * @param userId
	 */
	private void populateCtiyStateAndCountry(List<PricingField> fields, Integer userId) {
		PricingField callerId = PricingField.find(fields, "Caller_ID");
		try {
			if(null!=callerId && null!= callerId.getStrValue() && !callerId.getStrValue().isEmpty()
					&& callerId.getStrValue().length()> 6) {
				Integer userEntityId = getMediationHelperService().getUserCompanyByUserId(userId);
				String tableName = MediationCacheManager.getMetaFieldValue(MetaFieldName.NPA_NXX_TABEL_NAME.getMetaFieldName(), userEntityId);
				if(MediationCacheManager.isTablePresent(userEntityId, tableName)) {
					String[] NPANXX= callerId.getStrValue().substring(0, 6).split("(?<=\\G.{3})");
					Map<String, Object> result = MediationCacheManager.getCityStateCountryByNPANXX(NPANXX, tableName);
					PricingField city = new PricingField("city", result.getOrDefault("city", "").toString());
					PricingField state = new PricingField("state", result.getOrDefault("state", "").toString());
					PricingField country = new PricingField("country", result.getOrDefault("country", "").toString());
					fields.addAll(Arrays.asList(city, state, country));
				} else {
					LOG.debug("Table "+ tableName +" Does not Present!");
				}
			}
		} catch(Exception ex) {
			LOG.debug("Resolving Pricing Fields without City, State and Country Info", ex);
		}
	}

	@Override
    public boolean executeStep(MediationStepContext context) {
		try {
			List<PricingField> fields = context.getPricingFields();

			populateCtiyStateAndCountry(fields, context.getResult().getUserId());

			String pricing = fields.stream()
								   .map(PricingField::encode)
								   .collect(Collectors.joining(DEFAULT_CSV_FILE_SEPARATOR));
			context.getResult().setPricingFields(pricing);
			 LOG.debug("Pricing Fields -"+pricing);
			return true;
		} catch (Exception e) {
			context.getResult().addError("ERR-PRICING-FIELDS-NOT-FOUND");
			LOG.debug("Exception Occured in FC JMRPricingResolutionStep", e);
		    return false;
		}
    }

}
