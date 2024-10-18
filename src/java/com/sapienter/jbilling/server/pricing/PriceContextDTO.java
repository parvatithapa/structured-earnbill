package com.sapienter.jbilling.server.pricing;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDTO;

public class PriceContextDTO {

    private ItemDTO item;
    private BigDecimal quantity;
    private Integer userId;
    private Integer currencyId;
    private List<PricingField> fields;
    private Date eventDate;
    
    private PriceContextDTO(ItemDTO item, BigDecimal quantity, Integer userId, Integer currencyId, List<PricingField> fields, Date eventDate) {
        this.item = item;
        this.quantity = quantity;
        this.userId = userId;
        this.currencyId = currencyId;
        this.fields = fields;
        this.eventDate = eventDate;
    }
    
    public static PriceContextDTO of(ItemDTO item, BigDecimal quantity, Integer userId, Integer currencyId, List<PricingField> fields, Date eventDate) {
        return new PriceContextDTO(item, quantity, userId, currencyId, fields, eventDate);
    }

    public ItemDTO getItem() {
        return item;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public List<PricingField> getFields() {
        return fields;
    }

    public Date getEventDate() {
        return eventDate;
    }
    
}
