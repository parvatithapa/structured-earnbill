package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO;

public class CustomerNoteCreateProcessor implements ItemProcessor<DistributelPriceUpdateRequest, CustomerNoteDTO> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CONTENT_TEMPLATE = "Affected Order: %d - Current Order Total: %s  - New Order Total: %s  - "
            + "New Rate Effective as of: %s - Notification Sent on: %s";

    @Value("#{jobParameters['note_title']}")
    private String noteTitle;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Value("#{jobParameters['future_processing_date']}")
    private String futureProcessingDate;

    @Value("#{jobParameters['processing_date']}")
    private String processingDate;

    @Value("#{jobParameters['order_level_mf_name']}")
    private String orderLevelMfName;

    @Autowired
    private PriceRecordValidator recordValidator;

    @Override
    public CustomerNoteDTO process(DistributelPriceUpdateRequest request) {
        if (DistributelPriceJobConstants.REUQUEST_FAILED_STATUS.equals(request.getStatus())) {
            logger.debug(
                    "skipping customer note and order meta field value creation since record status is failed for user {} for entity {}",
                    request.getCustomerId(), entityId);
            return null;
        }
        recordValidator.validate(request);
        OrderBL orderBL = new OrderBL(request.getOrderId());
        OrderDTO order = orderBL.getDTO();
        if (order.getOrderPeriod().getPeriodUnit().isYearly()) {
            logger.debug("Order {} is yearly, so skipping", order.getId());
            return null;
        }
        logger.debug("Creating customer note for user {}", request.getCustomerId());
        CustomerNoteDTO note = createCustomerNote(request);
        logger.debug("Note Created {} for user {}", note, note.getUser().getId());
        addInvoiceNoteOnOrderLineMetaField(request.getOrderId(), request.getInvoiceNote());
        return note;
    }

    /**
     * Adds invoice line notes on Order level meta field
     *
     * @param orderId
     * @param noteToAddOnOrderLine
     */
    private void addInvoiceNoteOnOrderLineMetaField(Integer orderId, String noteToAddOnOrderLine) {
        OrderBL orderBL = new OrderBL(orderId);
        OrderDTO order = orderBL.getDTO();
        if (StringUtils.isEmpty(orderLevelMfName)) {
            logger.debug(
                    "Skipping addInvoiceNoteOnOrderLineMetaField since order_level_mf_name not found for entity id {}",
                    entityId);
            return;
        }

        if (StringUtils.isEmpty(noteToAddOnOrderLine)) {
            logger.debug("Skipping addInvoiceNoteOnOrderLineMetaField since invoice_note not found for order id {}",
                    orderId);
            return;
        }
        logger.debug("Adding note {} on order {}", noteToAddOnOrderLine, orderId);
        MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
        Integer companyId = order.getUser().getEntity().getId();
        MetaField orderlevelInvoiceNoteMf = metaFieldDAS.getFieldByName(companyId,
                new EntityType[] { EntityType.ORDER }, orderLevelMfName);
        if (null == orderlevelInvoiceNoteMf) {
            logger.debug("Meta field {} not defined on Order for entity {}", orderLevelMfName, companyId);
            return;
        }
        MetaFieldValue<String> orderLevelInvoiceNoteMfValue = new StringMetaFieldValue(orderlevelInvoiceNoteMf);
        orderLevelInvoiceNoteMfValue.setValue(noteToAddOnOrderLine);
        order.addMetaField(orderLevelInvoiceNoteMfValue);
        logger.debug("Note {} add on order {}", noteToAddOnOrderLine, orderId);
    }

    /**
     * Creates customer notes
     *
     * @param request
     * @return
     */
    private CustomerNoteDTO createCustomerNote(DistributelPriceUpdateRequest request) {
        CustomerNoteDTO note = new CustomerNoteDTO();
        UserBL userBL = new UserBL(request.getCustomerId());
        note.setCreationTime(TimezoneHelper.companyCurrentDate(entityId));
        note.setCompany(userBL.getEntity().getCompany());
        note.setNoteTitle(noteTitle);
        note.setUser(userBL.getEntity());
        note.setCustomer(userBL.getEntity().getCustomer());

        OrderBL orderBL = new OrderBL(request.getOrderId());
        OrderDTO order = orderBL.getDTO();
        BigDecimal currentTotalAmount = order.getLines().stream().filter(OrderLineDTO::hasItem)
                .filter(line -> line.getDeleted() == 0 && line.getItemId().equals(request.getProductId()))
                .map(OrderLineDTO::getAmount).reduce(BigDecimal.ZERO, (a1, a2) -> a1.add(a2));
        currentTotalAmount = currentTotalAmount.setScale(CommonConstants.BIGDECIMAL_SCALE_STR,
                CommonConstants.BIGDECIMAL_ROUND);

        BigDecimal futureTotalAmount = order.getLines().stream().filter(OrderLineDTO::hasItem)
                .filter(line -> line.getDeleted() == 0 && line.getItemId().equals(request.getProductId()))
                .map(OrderLineDTO::getQuantity).reduce(BigDecimal.ZERO, (a1, a2) -> a1.add(a2))
                .multiply(request.getNewOrderLinePrice());

        futureTotalAmount = futureTotalAmount.setScale(CommonConstants.BIGDECIMAL_SCALE_STR,
                CommonConstants.BIGDECIMAL_ROUND);

        String content = String.format(CONTENT_TEMPLATE, order.getId(), currentTotalAmount.toString(),
                futureTotalAmount.toString(), request.getScheduledDateForAdjustment(), processingDate);

        logger.debug("creating note content {} for date {} for user {}", content, processingDate, userBL.getEntity()
                .getId());
        note.setNoteContent(content);
        return note;
    }

}
