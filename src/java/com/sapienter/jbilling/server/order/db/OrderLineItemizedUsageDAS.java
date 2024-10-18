package com.sapienter.jbilling.server.order.db;

import java.util.List;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class OrderLineItemizedUsageDAS extends AbstractDAS<OrderLineItemizedUsageDTO> {

    private static final String GET_ITEMIZED_USAGES_FOR_ORDER_SQL =
            "SELECT * FROM order_line_itemized_usage "
                    + "WHERE order_line_id "
                    + "IN (SELECT id FROM order_line WHERE order_id = :orderId)";

    @SuppressWarnings("unchecked")
    public List<OrderLineItemizedUsageDTO> getItemizedUsagesForOrder(Integer orderId) {
        return getSession().createSQLQuery(GET_ITEMIZED_USAGES_FOR_ORDER_SQL)
                .addEntity(getPersistentClass())
                .setParameter("orderId", orderId)
                .list();
    }
}
