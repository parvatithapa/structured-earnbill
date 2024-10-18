package com.sapienter.jbilling.server.mediation.converter.db;
import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;



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
@Entity
@Table(name = "jbilling_mediation_error_record")
@IdClass(JbillingMediationErrorRecordId.class)
public class JbillingMediationErrorRecordDao implements Serializable {

    @Id
    @Column(name="jbilling_entity_id")
    private Integer jBillingCompanyId = null;
    @Id
    @Column(name="mediation_cfg_id")
    private Integer mediationCfgId = null;
    @Id
    @Column(name="record_key")
    private String recordKey = null;
    @Column(name="error_codes")
    private String errorCodes = null;
    @Column(name="process_id")
    private UUID processId = null;
    @Id
    @Column(name="id")
    private UUID id = null;

    @OneToOne(mappedBy = "errorRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private JMErrorUsageRecordDao errorUsageRecord;

    // these are pricing fields needed to resolve pricing. For example, the
    // destination number dialed for
    // long distance pricing based on a rate card
    // The String will later be processed with PricingField.getPricingFieldsValue()
    @Column(name="pricing_fields", length=1000)
    private String pricingFields = null;

    @Column(name="status")
    private String status = null;

    public JbillingMediationErrorRecordDao() {}

    public JbillingMediationErrorRecordDao(Integer jBillingCompanyId, Integer mediationCfgId,
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
                " jBillingEntityId='" + jBillingCompanyId + '\'' +
                ", mediationCfgId=" + mediationCfgId +
                ", recordKey=" + recordKey +
                ", status=" + status +
                ", errorCode='" + errorCodes + 
                ", id='" + id +'\'' +
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

    public JMErrorUsageRecordDao getErrorUsageRecord() {
        return errorUsageRecord;
    }

    public void setErrorUsageRecord(JMErrorUsageRecordDao errorUsageRecord) {
        this.errorUsageRecord = errorUsageRecord;
        errorUsageRecord.setErrorRecord(this);
    }
}
