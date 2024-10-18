package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.security.WSSecured;

import javax.validation.constraints.*;
import java.io.Serializable;

public class MatchingFieldWS implements WSSecured, Serializable {

    private static final String TO_STRING = "MatchingFieldWS [id=%s, orderSequence=%s, required=%s, description=%s, mediationField=%s, matchingField=%s, type=%s, routeId=%s, routeRateCardId=%s, entityId=%s, longestValue=%s, mandatoryFieldsQuery=%s]";

    private Integer id;
    private String orderSequence;
    private Boolean required;
    private String description;
    private String mediationField;
    private String matchingField;
    private String type;
    private Integer routeId;
    private Integer routeRateCardId;
    private Integer entityId;

    // transient values
    private Integer longestValue;
    private Integer smallestValue;
    private String mandatoryFieldsQuery;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    @Digits(integer = 12, fraction = 10, message = "validation.error.not.a.number")
    public String getOrderSequence() {
        return orderSequence;
    }

    public void setOrderSequence(String orderSequence) {
        this.orderSequence = orderSequence;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Size(min = 1, message = "validation.error.notnull")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    @Size(min = 1, message = "validation.error.notnull")
    public String getMediationField() {
        return mediationField;
    }

    public void setMediationField(String mediationField) {
        this.mediationField = mediationField;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public void setRouteId(Integer routeId) {
        this.routeId = routeId;
    }

    public String getType() {
        return type;
    }

    public String getMatchingField() {
        return matchingField;
    }

    public void setMatchingField(String matchingField) {
        this.matchingField = matchingField;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getRouteRateCardId() {
        return routeRateCardId;
    }

    public void setRouteRateCardId(Integer routeRateCardId) {
        this.routeRateCardId = routeRateCardId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    @Deprecated
    public Integer getLongestValue() {
		return longestValue;
	}

    @Deprecated
	public void setLongestValue(Integer longestValue) {
		this.longestValue = longestValue;
	}

    @Deprecated
	public Integer getSmallestValue() {
		return smallestValue;
	}

    @Deprecated
    public void setSmallestValue(Integer smallestValue) {
        this.smallestValue = smallestValue;
    }

	public String getMandatoryFieldsQuery() {
		return mandatoryFieldsQuery;
	}

	public void setMandatoryFieldsQuery(String mandatoryFieldsQuery) {
		this.mandatoryFieldsQuery = mandatoryFieldsQuery;
	}

	@Override
	public String toString() {
		return String.format(TO_STRING, id, orderSequence, required, description,
                                        mediationField, matchingField, type, routeId,
						                routeRateCardId, entityId, longestValue, mandatoryFieldsQuery);
	}

    @Override
    public Integer getOwningEntityId() {
        return entityId;
    }

    @Override
    public Integer getOwningUserId() {
        return null;
    }
}