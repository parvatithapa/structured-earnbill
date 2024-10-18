package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.util.csv.Exportable;

import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

/**
 * Created by marcolin on 06/10/15.
 */
/**
 *         Basic record to mediate. It has all the information cooked so
 *         jBilling can update the current order without any processing.
 *
 */
public class JbillingMediationErrorRecord implements Serializable, Exportable {

    private Integer jBillingCompanyId = null;
    private Integer mediationCfgId = null;
    private String recordKey = null;
    private String errorCodes = null;
    private UUID processId = null;
	private UUID id = null;

    // these are pricing fields needed to resolve pricing. For example, the
    // destination number dialed for
    // long distance pricing based on a rate card
    // The String will later be processed with PricingField.getPricingFieldsValue()
    private String pricingFields = null;
    private String status = null;
    private Date processingDate = null;

    public JbillingMediationErrorRecord() {}

    public JbillingMediationErrorRecord(Integer jBillingCompanyId, Integer mediationCfgId,
                                        String recordKey, String errorCodes, String pricingFields, UUID processId, String status, UUID id) {
        super();
        this.jBillingCompanyId = jBillingCompanyId;
        this.recordKey = recordKey;
        this.mediationCfgId = mediationCfgId;
        this.errorCodes = errorCodes;
        this.pricingFields = pricingFields;
        this.processId = processId;
        this.status = status;
        this.id = id;
    }

    @Override
    public String toString() {
        return "JbillingMediationRecord{" +
                " jBillingCompanyId='" + jBillingCompanyId + '\'' +
                ", mediationCfgId=" + mediationCfgId +
                ", recordKey=" + recordKey +
                ", processId=" + processId +
                ", errorCode='" + errorCodes +
                ", id='" + id + '\'' +
                '}';
    }

    public Integer getjBillingCompanyId() {
        return jBillingCompanyId;
    }

    public void setjBillingCompanyId(Integer jBillingCompanyId) {
        this.jBillingCompanyId = jBillingCompanyId;
    }

    public Integer getMediationCfgId() {
        return mediationCfgId;
    }

    public void setMediationCfgId(Integer mediationCfgId) {
        this.mediationCfgId = mediationCfgId;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    public String getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(String errorCodes) {
        this.errorCodes = errorCodes;
    }

    public String getPricingFields() {
        return pricingFields;
    }

    public void setPricingFields(String pricingFields) {
        this.pricingFields = pricingFields;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

    public Date getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(Date processingDate) {
        this.processingDate = processingDate;
    }

    private String capitalize(String name) {
		   return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
	
	@Override
	public String[] getFieldNames() {
		List<String> fieldNames = new ArrayList<String>();
		fieldNames.addAll(Arrays.asList("Process Id","Event Key","Error Codes","Company Id","MediationCfg Id"));
		if(null!=pricingFields && !pricingFields.isEmpty()) {
			PricingField[] pFields = PricingField.getPricingFieldsValue(pricingFields);
			fieldNames.addAll(Arrays.stream(pFields)
									.map(field -> capitalize(field.getName()))
									.collect(Collectors.toList()));
		}
		return fieldNames.toArray(new String[0]);
	}

    @Override
    public Object[][] getFieldValues() {
    	List<Object> fieldValues = new ArrayList<Object>();
    	fieldValues.addAll(Arrays.asList(processId,recordKey,errorCodes,jBillingCompanyId,mediationCfgId));
        if(null!=pricingFields && !pricingFields.isEmpty()) {
        	PricingField[] pFields = PricingField.getPricingFieldsValue(pricingFields);
        	fieldValues.addAll(Arrays.stream(pFields)
        				 			 .map(field -> field.getValue())
        				 			 .collect(Collectors.toList()));
        				 
        }
        return Stream.of(fieldValues)
			    	 .map(fields -> fields.stream().toArray(Object[]::new))
			    	 .toArray(Object[][]::new);
    }

    public static Set<String> getPricingHeaders(List<JbillingMediationErrorRecord> errors){
        if (errors != null && !errors.isEmpty()) {
            return errors.stream()
                         .filter(error -> ( null!=error.getPricingFields() && !error.getPricingFields().isEmpty()) )
                         .flatMap(error -> Arrays.stream(PricingField.getPricingFieldsValue(error.getPricingFields()))
                                                 .map(PricingField::getName))
                         .collect(Collectors.toSet());
        }

        return Collections.EMPTY_SET;
    }
}
