package com.sapienter.jbilling.rest;

import java.math.BigDecimal;

/**
 * @author Vojislav Stanojevikj
 * @since 25-Oct-2016.
 */
final class RestPlanItem {

    private Integer itemId;
    private Integer periodId;
    private BigDecimal quantity;
    private BigDecimal price;

    public RestPlanItem(Integer itemId, Integer periodId, BigDecimal quantity, BigDecimal price) {
        this.itemId = itemId;
        this.periodId = periodId;
        this.quantity = quantity;
        this.price = price;
    }

    public Integer getItemId() {
        return itemId;
    }

    public Integer getPeriodId() {
        return periodId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestPlanItem)) return false;

        RestPlanItem that = (RestPlanItem) o;

        if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) return false;
        if (periodId != null ? !periodId.equals(that.periodId) : that.periodId != null) return false;
        if (quantity != null ? !quantity.equals(that.quantity) : that.quantity != null) return false;
        return !(price != null ? !price.equals(that.price) : that.price != null);

    }

    @Override
    public int hashCode() {
        int result = itemId != null ? itemId.hashCode() : 0;
        result = 31 * result + (periodId != null ? periodId.hashCode() : 0);
        result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RestPlanItem{" +
                "itemId=" + itemId +
                ", periodId=" + periodId +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}
