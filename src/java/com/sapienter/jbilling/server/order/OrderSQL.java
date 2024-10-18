/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.order;

/**
 * @author Emil
 */
public interface OrderSQL {

    // This one is used for root and clerks
    String listInternal = " SELECT po.id, po.id, bu.user_name, c.organization_name, po.create_datetime " +
                          "   FROM purchase_order AS po, base_user AS bu, AS contact c " +
                          "  WHERE po.deleted = 0 " +
                          "    AND bu.entity_id = ? " +
                          "    AND po.user_id = bu.id " +
                          "    AND c.user_id = bu.id ";

    // PARTNER: will show only customers that belong to this partner
    String listPartner = "SELECT po.id, po.id, bu.user_name, c.organization_name, po.create_datetime " +
                         "  FROM purchase_order po, base_user bu, customer cu, partner pa, contact c " +
                         " WHERE po.deleted = 0 " +
                         "   AND bu.entity_id = ? " +
                         "   AND po.user_id = bu.id" +
                         "   AND cu.partner_id = pa.id " +
                         "   AND pa.user_id = ? " +
                         "   AND cu.user_id = bu.id " +
                         "   AND c.user_id = bu.id ";

    String listCustomer = "SELECT po.id, po.id, bu.user_name, c.organization_name, po.create_datetime " +
                          "  FROM purchase_order po, base_user bu, contact c " +
                          " WHERE po.deleted = 0 " +
                          "   AND po.user_id = ? " +
                          "   AND po.user_id = bu.id " +
                          "   AND c.user_id = bu.id ";

    String listByProcess = "  SELECT po.id, po.id, bu.user_name, po.create_datetime " +
                           "    FROM purchase_order po, base_user bu, billing_process bp, order_process op " +
                           "   WHERE bp.id = ? " +
                           "     AND po.user_id = bu.id " +
                           "     AND op.billing_process_id = bp.id " +
                           "     AND op.order_id = po.id " +
                           "ORDER BY 1 DESC";

    String getAboutToExpire = "    SELECT o.id, o.active_until, o.notification_step " +
                              "      FROM purchase_order AS o " +
                              "INNER JOIN base_user AS bu ON bu.id = o.user_id " +
                              "INNER JOIN order_status AS os ON os.id = o.status_id " +
                              "     WHERE o.active_until >= ? " +
                              "       AND o.active_until <= ? " +
                              "       AND o.notify = 1 " +
                              "       AND os.order_status_flag = " + OrderStatusFlag.INVOICE.ordinal() + // invoice
                              "       AND bu.entity_id = ? " +
                              "       AND (o.notification_step IS NULL OR o.notification_step < ?)";

    String getLatestByItemType = "    SELECT MAX(purchase_order.id) " +
                                 "      FROM purchase_order " +
                                 "INNER JOIN order_line on order_line.order_id = purchase_order.id " +
                                 "INNER JOIN item on item.id = order_line.item_id " +
                                 "INNER JOIN item_type_map on item_type_map.item_id = item.id " +
                                 "     WHERE purchase_order.user_id = ?" +
                                 "       AND item_type_map.type_id = ? " +
                                 "       AND purchase_order.deleted = 0";

    String getByUserAndPeriod = "SELECT id " +
                                "  FROM purchase_order " +
                                " WHERE user_id = ? " +
                                "   AND period_id = ? " +
                                "   AND deleted = 0";

}
