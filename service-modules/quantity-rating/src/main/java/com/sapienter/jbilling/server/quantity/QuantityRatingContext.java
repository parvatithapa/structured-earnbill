package com.sapienter.jbilling.server.quantity;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.ratingUnit.domain.RatingUnit;
import com.sapienter.jbilling.server.usageRatingScheme.domain.IUsageRatingSchemeModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class QuantityRatingContext {

    private RatingUnit ratingUnit;
    private IUsageRatingSchemeModel ratingScheme;

    private Integer entityId;
    private Integer userId;
    private Integer itemId;
    private String  resourceId;
    private Date    eventDate;
    private String  mediationProcessId;
    private String  recordKey;
    private List<PricingField> pricingFields;
    private List<String> errors = new ArrayList<>();


    private QuantityRatingContext() {}

    public QuantityRatingContext(Integer entityId, Integer userId, Integer itemId,
                                 String resourceId, Date eventDate, String mediationProcessId,
                                 String recordKey, List<PricingField> pricingFields) {

        this.entityId = entityId;
        this.userId = userId;
        this.itemId = itemId;
        this.resourceId = resourceId;
        this.eventDate = eventDate;
        this.recordKey = recordKey;
        this.mediationProcessId = mediationProcessId;
        this.pricingFields = pricingFields;
    }

    public void setRatingUnit(RatingUnit ratingUnit) {
        this.ratingUnit = ratingUnit;
    }

    public void setRatingScheme(IUsageRatingSchemeModel ratingScheme) {
        this.ratingScheme = ratingScheme;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public RatingUnit getRatingUnit() {
        return ratingUnit;
    }

    public IUsageRatingSchemeModel getRatingScheme() {
        return ratingScheme;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public String getMediationProcessId() {
        return mediationProcessId;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public List<PricingField> getPricingFields() {
        return pricingFields;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String errorCode) {
        errors.add(errorCode);
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public boolean hasErrors() {
        return errors != null && errors.size() > 0;
    }
}
