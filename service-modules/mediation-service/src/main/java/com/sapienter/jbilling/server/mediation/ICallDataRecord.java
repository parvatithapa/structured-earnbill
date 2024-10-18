package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.item.PricingField;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Interface implemented by CDRs
 */
public interface ICallDataRecord extends Serializable {
    Date getProcessingDate();

    void setProcessingDate(String processingTime);

    int getPosition();

    void setPosition(int position);

    List<PricingField> getFields();

    void setFields(List<PricingField> fields);

    void addField(PricingField field, boolean isKey);

    String getKey();

    void setKey(String key);

    void appendKey(String key);

    List<String> getErrors();

    void addError(String error);

    String getRecordId();

    void setRecordId(String recordId);

    Integer getEntityId();

    void setEntityId(Integer entityId);

    Integer getMediationCfgId();

    void setMediationCfgId(Integer mediationCfgId);

    PricingField getField(String name);
}
