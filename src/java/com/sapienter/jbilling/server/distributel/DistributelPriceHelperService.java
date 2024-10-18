package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

@Transactional(propagation = Propagation.REQUIRES_NEW)
public class DistributelPriceHelperService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name = "recordValidator")
    private PriceRecordValidator priceRecordValidator;
    @Resource(name = "webServicesSession")
    private IWebServicesSessionBean api;
    @Resource
    private UserDAS userDAS;
    @Resource
    private OrderDAS orderDAS;
    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * Creates new line on given order.
     * @param newLineRequest
     * @param tableName
     */
    public void addNewOrderLine(DistributelPriceUpdateRequest newLineRequest, String tableName) {
        Assert.notNull(newLineRequest, "newLineRequest may not be null!");
        Assert.notNull(tableName, "tableName may not be null!");
        priceRecordValidator.validateIncreaseAndReversal(newLineRequest);
        UserDTO user = userDAS.find(newLineRequest.getCustomerId());
        Integer entityId = user.getEntity().getId();
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            OrderLineWS orderLine = new OrderLineWS();
            Integer itemId = newLineRequest.getProductId();
            ItemDTOEx item = api.getItem(itemId, null, null);
            orderLine.setUseItem(false);
            orderLine.setCreateDatetime(new Date());
            orderLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
            orderLine.setQuantity(1);
            orderLine.setItemId(itemId);
            orderLine.setPrice(newLineRequest.getNewOrderLinePrice());
            orderLine.setAmount(newLineRequest.getNewOrderLinePrice());
            orderLine.setDescription(item.getDescription());
            // get order to add new order line.
            OrderWS order = api.getOrder(newLineRequest.getOrderId());
            List<OrderLineWS> lines = new ArrayList<>(Arrays.asList(order.getOrderLines()));
            // add new line on existing lines.
            lines.add(orderLine);
            order.setOrderLines(lines.toArray(new OrderLineWS[0]));
            OrderChangeStatusDTO applyStatus = new OrderChangeStatusDAS().findApplyStatus(entityId);
            Assert.notNull(applyStatus, "apply status not found for entity "+ entityId);
            // update new line on order.
            api.updateOrder(order, new OrderChangeWS[] {
                    OrderChangeBL.buildFromLine(orderLine, order, applyStatus.getId())
            });
            logger.debug("line {} added on order {}", orderLine, order.getId());
        }
        markRequestAsProcessed(newLineRequest, tableName);
    }

    /**
     * delete line for given product from order.
     * @param deleteRequest
     * @param tableName
     */
    public void deleteProduct(DistributelPriceUpdateRequest deleteRequest, String tableName) {
        Assert.notNull(deleteRequest, "deleteRequest may not be null!");
        Assert.notNull(tableName, "tableName may not be null!");
        priceRecordValidator.validateIncreaseAndReversal(deleteRequest);
        OrderDTO order = orderDAS.find(deleteRequest.getOrderId());
        if (OrderStatusFlag.FINISHED.equals(order.getOrderStatus().getOrderStatusFlag())) {
            logger.debug("order {} alreday finished, so skipping delete product request {}", order.getId(), deleteRequest);
            return;
        }
        Integer productId = deleteRequest.getProductId();
        List<Integer> linesToDelete = jdbcTemplate.queryForList("SELECT id FROM order_line WHERE order_id = "+
                order.getId() + " AND item_id = "+ productId, Integer.class);
        logger.debug("lines to delete {} for item {}", linesToDelete, productId);
        for(Integer lineId : linesToDelete) {
            // delete order change.
            jdbcTemplate.update("DELETE FROM order_change WHERE order_line_id = "+ lineId);
            // delete order line.
            jdbcTemplate.update("DELETE FROM order_line WHERE id = "+ lineId);
            logger.debug("line {} deleted for item {}", lineId, productId);
        }
        markRequestAsProcessed(deleteRequest, tableName);
    }

    private void markRequestAsProcessed(DistributelPriceUpdateRequest priceRequest, String tableName) {
        // mark request as processed.
        priceRequest.setStatus(DistributelPriceJobConstants.REUQUEST_SUCCESS_STATUS);
        DistributelHelperUtil.updateRequestStatus(priceRequest,
                DistributelPriceJobConstants.REUQUEST_SUCCESS_STATUS, tableName);
    }

}
