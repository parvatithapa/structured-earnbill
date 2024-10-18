package com.sapienter.jbilling.server.item.tasks;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.ToString;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;

@ToString
public class ItemPurchaseManagerContext {

    private ItemPurchaseManagerContext(Integer itemId, BigDecimal quantity, Integer userId) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.userId = userId;
    }

    private Integer itemId;
    private BigDecimal quantity;
    private Integer languageId;
    private Integer userId;
    private Integer entityId;
    private Integer currencyId;
    private OrderDTO order;
    private List<CallDataRecord> records;
    private List<OrderLineDTO> lines;
    private boolean singlePurchase;
    private String sipUri;
    private Date eventDate;

    public Integer getItemId() {
        return itemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public OrderDTO getOrder() {
        return order;
    }

    public List<CallDataRecord> getRecords() {
        return records;
    }

    public List<OrderLineDTO> getLines() {
        return lines;
    }

    public boolean isSinglePurchase() {
        return singlePurchase;
    }

    public String getSipUri() {
        return sipUri;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public static Builder build(Integer userId, Integer itemId, BigDecimal quantity) {
        return new Builder(userId, itemId, quantity);
    }
    public static class Builder {

        private ItemPurchaseManagerContext instance;

        private Builder(Integer userId, Integer itemId, BigDecimal quantity) {
            instance = new ItemPurchaseManagerContext(itemId, quantity, userId);
        }

        public Builder addLanguageId(Integer languageId) {
            instance.languageId = languageId;
            return this;
        }

        public Builder addOrderLines(List<OrderLineDTO> lines) {
            instance.lines = lines;
            return this;
        }

        public Builder addCurrencyId(Integer currencyId) {
            instance.currencyId = currencyId;
            return this;
        }

        public Builder addEntityId(Integer entityId) {
            instance.entityId = entityId;
            return this;
        }

        public Builder isSingePurchase(Boolean isSingePurchase) {
            instance.singlePurchase = isSingePurchase;
            return this;
        }

        public Builder addOrder(OrderDTO order) {
            instance.order = order;
            return this;
        }

        public Builder addCallDataRecords(List<CallDataRecord> callDataRecords) {
            instance.records = callDataRecords;
            return this;
        }

        public Builder addSipUri(String sipUri) {
            instance.sipUri = sipUri;
            return this;
        }

        public Builder addEventDate(Date eventDate) {
            if(null != eventDate) {
                instance.eventDate = new Date(eventDate.getTime());
            }
            return this;
        }

        public ItemPurchaseManagerContext build() {
            return instance;
        }
    }
}
