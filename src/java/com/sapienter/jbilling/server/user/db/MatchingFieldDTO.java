package com.sapienter.jbilling.server.user.db;



import com.sapienter.jbilling.common.MissingRequiredFieldError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldResult;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.db.RouteDTO;
import com.sapienter.jbilling.server.pricing.db.RouteRateCardDTO;
import com.sapienter.jbilling.server.pricing.strategy.AbstractPricingStrategy;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;

@Entity
@Table(name = "matching_field")
@javax.persistence.TableGenerator(
        name = "matching_field_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "matching_field",
        allocationSize = 1
)
public class MatchingFieldDTO implements Serializable {

    private Integer id;
    private Integer orderSequence;
    private Boolean required;
    private String description;
    private String mediationField;
    private String matchingField;
    private MatchingFieldType type;
    private RouteDTO route;
    private RouteRateCardDTO routeRateCard;
    private Integer versionNum;
    private String mandatoryFieldsQuery;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "matching_field_GEN")
    @Column(name = "id", nullable = false, unique = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    @Column(name = "required")
    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    @Column(name = "mediation_field")
    public String getMediationField() {
        return mediationField;
    }

    public void setMediationField(String mediationField) {
        this.mediationField = mediationField;
    }
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = true)
    public RouteDTO getRoute() {
        return route;
    }

    public void setRoute(RouteDTO route) {
        this.route = route;
    }
    @Column(name="order_sequence")
    public Integer getOrderSequence() {
        return orderSequence;
    }

    public void setOrderSequence(Integer orderSequence) {
        this.orderSequence = orderSequence;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 25)
    public MatchingFieldType getType() {
        return type;
    }

    public void setType(MatchingFieldType type) {
        this.type = type;
    }

	@Column(name = "mandatory_fields_query")
    public String getMandatoryFieldsQuery() {
		return mandatoryFieldsQuery;
	}

	public void setMandatoryFieldsQuery(String mandatoryFieldsQuery) {
		this.mandatoryFieldsQuery = mandatoryFieldsQuery;
	}

	@Version
    @Column(name = "optlock")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }
    @Column(name = "matching_field")
    public String getMatchingField() {
        return matchingField;
    }

    public void setMatchingField(String matchingField) {
        this.matchingField = matchingField;
    }
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_rate_card_id", nullable = true)
    public RouteRateCardDTO getRouteRateCard() {
        return routeRateCard;
    }

    public void setRouteRateCard(RouteRateCardDTO routeRateCard) {
        this.routeRateCard = routeRateCard;
    }

    @Override
    public String toString() {
        return "MatchingFieldDTO{" +
                "id=" + id +
                ", orderSequence=" + orderSequence +
                ", required=" + required +
                ", description='" + description + '\'' +
                ", mediationField='" + mediationField + '\'' +
                ", matchingField='" + matchingField + '\'' +
                ", type='" + type + '\'' +
                ", route=" + route +
                ", routeRateCard=" + routeRateCard +
                ", versionNum=" + versionNum +
                '}';
    }

    @Transient
    public void apply(List<PricingField> pricingFields, MatchingFieldResult result) {
        if (!StringUtils.isEmpty(getMediationField())) {
            PricingField searchValue = AbstractPricingStrategy.find(pricingFields, getMediationField());
            if (null != searchValue) {
                getType().apply(this, searchValue, result);
            }
		} else {
            throw new MissingRequiredFieldError("Missing matching field value.", new String[] {
                    "MatchingFieldDTO,name,missing.value.for.mandatory.matching.field"
            });
		}
    }
}
