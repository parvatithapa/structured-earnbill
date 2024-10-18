package com.sapienter.jbilling.server.distributel;

import io.vavr.control.Try;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;



public class DistributelPriceProcessor implements ItemProcessor<DistributelPriceUpdateRequest, DistributelPriceUpdateRequest> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private PriceRecordValidator recordValidator;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Value("#{jobParameters['data_table_name']}")
    private String tableName;

    @Value("#{jobParameters['days_to_add']}")
    private Integer noOfDays;

    @Value("#{jobParameters['note_title']}")
    private String noteTitle;

    @Value("#{jobParameters['order_level_mf_name']}")
    private String orderLevelMfName;

    @Override
    public DistributelPriceUpdateRequest process(DistributelPriceUpdateRequest request) {
        recordValidator.validate(request);
        logger.debug("Processing request {} for entity {}", request, entityId);
        return Try.of(()-> request)
                .mapTry(this::updatePriceAndRemoveInvoiceNoteMfOnOrder)
                .onSuccess(out -> {
                    out.setStatus(DistributelPriceJobConstants.REUQUEST_SUCCESS_STATUS);
                    DistributelHelperUtil.updateRequestStatus(out, DistributelPriceJobConstants.REUQUEST_SUCCESS_STATUS, tableName);
                })
                .get();
    }

    /**
     * Removes invoiceNote MetaFieldValue if present
     * @param order
     */
    private void removeInvoiceNoteMfFromOrder(OrderDTO order) {
        @SuppressWarnings("rawtypes")
        List<MetaFieldValue> orderMfs = order.getMetaFields();
        if(CollectionUtils.isNotEmpty(orderMfs) &&
                StringUtils.isNotEmpty(orderLevelMfName)) {
            logger.debug("Order {} meta fields before remove {}", order.getId(), orderMfs);
            @SuppressWarnings("rawtypes")
            Optional<MetaFieldValue> invoiceNoteMF = orderMfs.stream()
                    .filter(value -> value.getField().getName().equals(orderLevelMfName))
                    .findFirst();
            if(invoiceNoteMF.isPresent()) {
                orderMfs.remove(invoiceNoteMF.get());
            }
            order.setMetaFields(orderMfs);
            logger.debug("removed {} from order {}", orderLevelMfName, order.getId());
            logger.debug("Order {} metafields {}", order.getId(), order.getMetaFields());
        }
    }

    private DistributelPriceUpdateRequest updatePriceAndRemoveInvoiceNoteMfOnOrder(DistributelPriceUpdateRequest request) {
        if(DistributelPriceJobConstants.REUQUEST_SUCCESS_STATUS.equals(request.getStatus())) {
            return null;
        }
        OrderBL orderBL = new OrderBL(request.getOrderId());
        OrderDTO order = orderBL.getDTO();
        if(OrderStatusFlag.FINISHED.equals(order.getOrderStatus().getOrderStatusFlag())) {
            return request;
        }
        removeInvoiceNoteMfFromOrder(order);
        Integer productId = request.getProductId();
        List<OrderLineDTO> lines = order.getLines()
                .stream()
                .filter(OrderLineDTO::hasItem)
                .filter(line -> line.getDeleted() == 0)
                .filter(line -> line.getItemId().equals(productId))
                .collect(Collectors.toList());
        logger.debug("Updating Order's {} lines {}", order.getId(), lines);
        for(OrderLineDTO line : lines) {
            line.setPrice(request.getNewOrderLinePrice());
            line.setAmount(line.getQuantity().multiply(line.getPrice()));

            line.getOrderChanges()
            .stream()
            .filter(change -> (change.getRemoval() == null || change.getRemoval() != 1))
            .filter(change -> change.getItem().getId() == productId)
            .forEach(change ->  change.setPrice(line.getPrice()));
            logger.debug("Price {} updated for line {}", line.getPrice(), line.getId());
        }
        return request;
    }

}
