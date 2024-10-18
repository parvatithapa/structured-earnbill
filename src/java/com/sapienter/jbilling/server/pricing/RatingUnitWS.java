package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.server.security.WSSecured;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Digits;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *  Rating Unit WS
 *
 *  @author Panche Isajeski
 *  @since 27-Aug-2013
 */
public class RatingUnitWS implements WSSecured, Serializable {

    private Integer id;
    private Integer entityId;

    @NotEmpty(message="validation.error.notnull")
    private String name;
    @NotEmpty(message="validation.error.notnull")
    private String priceUnitName;
    @NotEmpty(message="validation.error.notnull")
    private String incrementUnitName;

    @NotEmpty(message="validation.error.notnull")
    @Digits(integer=12, fraction=10, message="validation.error.not.a.number")
    private String incrementUnitQuantity;

    private boolean canBeDeleted;

    public RatingUnitWS() {
    }

    public String getIncrementUnitQuantity() {
        return incrementUnitQuantity;
    }

    public BigDecimal getIncrementUnitQuantityAsDecimal() {
        return incrementUnitQuantity != null ? new BigDecimal(incrementUnitQuantity) : null;
    }

    public void setIncrementUnitQuantity(String incrementUnitQuantity) {
        if(!StringUtils.isEmpty(incrementUnitQuantity)) {
            this.incrementUnitQuantity = incrementUnitQuantity;
        } else {
            this.incrementUnitQuantity = null;
        }
    }

    public void setIncrementUnitQuantityAsDecimal(BigDecimal incrementUnitQuantity) {
        this.incrementUnitQuantity = (incrementUnitQuantity != null ? incrementUnitQuantity.toString() : null);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPriceUnitName() {
        return priceUnitName;
    }

    public void setPriceUnitName(String priceUnitName) {
        this.priceUnitName = priceUnitName;
    }

    public String getIncrementUnitName() {
        return incrementUnitName;
    }

    public void setIncrementUnitName(String incrementUnitName) {
        this.incrementUnitName = incrementUnitName;
    }

    public boolean isCanBeDeleted() {
        return canBeDeleted;
    }
    
    public void setIsCanBeDeleted(boolean canBeDeleted){
    	this.canBeDeleted = canBeDeleted;
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
