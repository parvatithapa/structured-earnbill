package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by vivekmaster146 on 8/8/14.
 */
public class CustomerPriceWS implements Serializable {
    private Integer id;
    private PriceModelWS priceModel;
    private  Integer precedence;
    private Integer model_map_id;
    private Integer itemId;
    private Integer baseUserId;
    @ConvertToTimezone
    private Date startDate;


    public Integer getId() {

        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBaseUserId() {
        return baseUserId;
    }

    public void setBaseUserId(Integer baseUserId) {
        this.baseUserId = baseUserId;
    }

    public PriceModelWS getPriceModel() {
        return priceModel;
    }

    public void setPriceModel(PriceModelWS priceModel) {
        this.priceModel = priceModel;
    }

    public Integer getPrecedence() {
        return precedence;
    }

    public void setPrecedence(Integer precedence) {
        this.precedence = precedence;
    }

    public Integer getModel_map_id() {
        return model_map_id;
    }

    public void setModel_map_id(Integer model_map_id) {
        this.model_map_id = model_map_id;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }


    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

}
