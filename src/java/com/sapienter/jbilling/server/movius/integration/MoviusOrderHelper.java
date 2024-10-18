package com.sapienter.jbilling.server.movius.integration;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.movius.db.OrgCountPositionDAS;
import com.sapienter.jbilling.server.mediation.movius.db.OrgCountPositionDTO;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

public class MoviusOrderHelper {

	private static final Logger LOG = LoggerFactory.getLogger(MoviusOrderHelper.class);

    private Integer userId;
    private BigDecimal quantity;
    private BigDecimal charges;
    private Integer itemId;
    private OrderWS order;
    private String orgId;
    private String billableOrgId;
    private IWebServicesSessionBean sessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
    private Integer entityId;
    private SimpleDateFormat originationDateFormatter = new SimpleDateFormat(MoviusConstants.ORIGINATION_DATE_FORMAT);

    private MoviusOrderHelper(Integer userId, BigDecimal quantity, BigDecimal charges, Integer itemId, Integer entityId) {
        this.userId = userId;
        this.quantity = quantity;
        this.charges = charges;
        this.itemId = itemId;
        this.entityId = entityId;
    }

    private MoviusOrderHelper(OrderWS order, BigDecimal quantity, BigDecimal charges, Integer itemId, Integer entityId) {
        this.order = order;
        this.userId = order.getUserId();
        this.quantity = quantity;
        this.charges = charges;
        this.itemId = itemId;
        this.entityId = entityId;
    }

    public MoviusOrderHelper addOrgId(String orgId) {
        this.orgId = orgId;
        return this;
    }

    public MoviusOrderHelper addBillableOrgId(String billableOrgId) {
        this.billableOrgId = billableOrgId;
        return this;
    }

    public static MoviusOrderHelper of(Integer userId, BigDecimal quantity, BigDecimal charges, Integer itemId, Integer entityId) {
        return new MoviusOrderHelper(userId, quantity, charges, itemId, entityId);
    }

    public static MoviusOrderHelper of(OrderWS order, BigDecimal quantity, BigDecimal charges, Integer itemId, Integer entityId) {
        return new MoviusOrderHelper(order, quantity, charges, itemId, entityId);
    }

    public MoviusOrderHelper create(Integer orderStatusId, Integer orderPeriodId) {
        Integer currencyId  = new UserBL(userId).getCurrencyId();
        OrderWS orderWS = buildOrderWS(userId, itemId, quantity.intValue(), charges, currencyId, orderPeriodId);
        OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(orderWS, orderStatusId);
        Integer orderId = sessionBean.createOrder(orderWS, changes);
        this.order = sessionBean.getOrder(orderId);
        createOrgCountPositionRecord(orderId, quantity, BigDecimal.ZERO, itemId);
        return this;
    }

    public MoviusOrderHelper addNewLine(Integer orderStatusId) {

        OrderChangeWS[] oldOrderChanges = sessionBean.getOrderChanges(order.getId());

        List<OrderChangeWS> newChanges = new ArrayList<>();
        
        Integer subScriptionItemId = MoviusTaskUtils.getSubscriptionItemIdByEntity(entityId);
        
        Optional<OrderLineWS> subScriptionLine = getLineByItemId(order.getOrderLines(), subScriptionItemId);

        OrderChangeWS subscriptionOrderChange = null;
        if(subScriptionLine.isPresent()) {
            subscriptionOrderChange = createSubScriptionOrderChangeIfApplicable(oldOrderChanges, orderStatusId, BigDecimal.ZERO);
            if(null != subscriptionOrderChange) {
                newChanges.add(subscriptionOrderChange);
            }
        }
        
        OrderLineWS newLine = buildOrderLine(itemId, quantity.intValue(), charges);
        List<OrderLineWS> lines = Arrays.stream(order.getOrderLines())
                .collect(Collectors.toList());

        lines.add(newLine);

        order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));

        newChanges.add(OrderChangeBL.buildFromLine(newLine, order, orderStatusId));
        order.setProrateFlag(order.getProrateFlag());

        sessionBean.updateOrder(order, newChanges.toArray(new OrderChangeWS[newChanges.size()]));

        createOrgCountPositionRecord(getOrderId(), quantity, BigDecimal.ZERO, itemId);

        if(null != subscriptionOrderChange) {
            updateLineQunaity(subScriptionLine.get(), subscriptionOrderChange.getQuantityAsDecimal(), 
                    subScriptionLine.get().getPriceAsDecimal());
        }

        return this;
    }

    private void updateLineQunaity(OrderLineWS line, BigDecimal quantity, BigDecimal price) {
            line.setQuantity(quantity);
            line.setPriceAsDecimal(price);
            line.setAmountAsDecimal(price.multiply(quantity));
            sessionBean.updateOrderLine(line);
    }
    
    private boolean isPriceUpdated(OrderWS orderWS, BigDecimal price, Integer itemId) {
        if(null != orderWS && null != price && null != itemId) {
            BigDecimal latestPrice = BigDecimal.ZERO;
            for (OrderLineWS orderLineWS : orderWS.getOrderLines()) {
                if (0 == orderLineWS.getItemId().compareTo(itemId)) {
                    latestPrice = latestPrice.add(orderLineWS.getPriceAsDecimal());
                }
            }
            Integer result = price.compareTo(latestPrice);
            if (result != 0) {
                return true;
            }
        }
        return false;
    }
    
    public MoviusOrderHelper updateOrderLine(Integer orderStatusId) {
        boolean priceUpdated = isPriceUpdated(order, charges, itemId);
        OrderChangeWS[] oldOrderChanges = sessionBean.getOrderChanges(order.getId());
        List<OrderChangeWS> newChanges = new ArrayList<>();
        OrgCountPositionDTO orgCountPositionDTO = new OrgCountPositionDAS().
                findByOrgIdOrderIdAndItemId(orgId, getOrderId(), itemId, entityId);

        BigDecimal currentOrgCountPosition = Objects.nonNull(orgCountPositionDTO) ? orgCountPositionDTO.getCount() : BigDecimal.ZERO;

        boolean quantityUpdated = currentOrgCountPosition.compareTo(quantity)!= 0;
        if ( quantityUpdated || priceUpdated) {
            Integer subScriptionItemId = MoviusTaskUtils.getSubscriptionItemIdByEntity(entityId);
            
            Optional<OrderLineWS> subScriptionLine = getLineByItemId(order.getOrderLines(), subScriptionItemId);
            OrderChangeWS subscriptionOrderChange = null;
            if(subScriptionLine.isPresent() && quantityUpdated) {
                subscriptionOrderChange = createSubScriptionOrderChangeIfApplicable(oldOrderChanges, orderStatusId, currentOrgCountPosition);
                if(null != subscriptionOrderChange) {
                    newChanges.add(subscriptionOrderChange);
                }
            }
            OrderChangeWS originationChange = getUpdatedOrderChange(order.getOrderLines(), oldOrderChanges, itemId, orderStatusId);
            newChanges.add(originationChange);
            if(quantityUpdated) {
                BigDecimal calculatedCurrentQuantity = (currentOrgCountPosition.subtract(quantity)).negate();
                originationChange.setQuantityAsDecimal(originationChange.getQuantityAsDecimal().add(calculatedCurrentQuantity));
                
                if(Objects.nonNull(orgCountPositionDTO)) {
                    updateOrgCountPositionRecord(orgCountPositionDTO, quantity, currentOrgCountPosition);
                } else {
                    createOrgCountPositionRecord(getOrderId(), quantity, BigDecimal.ZERO, itemId);
                }
            }
            if(priceUpdated) {
                originationChange.setPriceAsDecimal(charges);
            }

            order.setProrateFlag(order.getProrateFlag());

            sessionBean.updateOrder(order, newChanges.toArray(new OrderChangeWS[0]));

            //Updated the quantity to fix https://appdirect.jira.com/browse/JBMOV-55
            Optional<OrderLineWS> existingOrderLine = getLineByItemId(order.getOrderLines(), itemId);
            if(existingOrderLine.isPresent()) {
                OrderLineWS existingLine = existingOrderLine.get();
                updateLineQunaity(existingLine, originationChange.getQuantityAsDecimal(), charges);
            }
            if(subScriptionLine.isPresent() && quantityUpdated && subscriptionOrderChange != null) {
                updateLineQunaity(subScriptionLine.get(), subscriptionOrderChange.getQuantityAsDecimal(), 
                        subScriptionLine.get().getPriceAsDecimal());
            }
        }
        return this;
    }

    private OrderChangeWS createSubScriptionOrderChangeIfApplicable(OrderChangeWS[] oldOrderChanges, Integer orderStatusId, BigDecimal currentOrgCountPosition) {
        OrderLineWS[] lines = order.getOrderLines();
        Integer subScriptionItemId = MoviusTaskUtils.getSubscriptionItemIdByEntity(entityId);
        BigDecimal calculatedCurrentQuantity = quantity;
        
        OrgCountPositionDAS orgCountPositionDAS  = new OrgCountPositionDAS();
        OrgCountPositionDTO subScriptionPositionDTO = orgCountPositionDAS.findByOrgIdOrderIdAndItemId(orgId, getOrderId(), subScriptionItemId, entityId);
        
        if(!currentOrgCountPosition.equals(BigDecimal.ZERO)) {
            calculatedCurrentQuantity = (currentOrgCountPosition.subtract(calculatedCurrentQuantity)).negate();
        }

        BigDecimal originationQuantity = getOriginationLineQuantity(lines, calculatedCurrentQuantity, subScriptionItemId);
        
        OrderChangeWS updatedSubScriptionOrderChange = getUpdatedOrderChange(lines, oldOrderChanges, subScriptionItemId, orderStatusId);
        
        if(Objects.isNull(subScriptionPositionDTO)) {
            subScriptionPositionDTO = orgCountPositionDAS.findByOrgIdOrderIdAndItemId(billableOrgId, getOrderId(), subScriptionItemId, entityId);
        }
        
        if (updatedSubScriptionOrderChange != null) {
	        // Added not null check for test case of MoviusOriginationChargesCreateUpdateTaskTest
	        // since monthly order with subscription created manually for testing purpose.
	        if(Objects.nonNull(subScriptionPositionDTO)) {
	            updateOrgCountPositionRecord(subScriptionPositionDTO, originationQuantity, updatedSubScriptionOrderChange.getQuantityAsDecimal());
	        } else {
	            subScriptionPositionDTO = new OrgCountPositionDTO(billableOrgId, billableOrgId,
	                    originationQuantity, updatedSubScriptionOrderChange.getQuantityAsDecimal(), getOrderId(), subScriptionItemId, entityId);
	            OrgCountPositionDAS das = new OrgCountPositionDAS();
	            Integer id = das.save(subScriptionPositionDTO).getId();
	            LOG.debug("Created SubScription Org Count Position Record {} ", id);
	        }
	        updatedSubScriptionOrderChange.setQuantity(originationQuantity);
        }

        return updatedSubScriptionOrderChange;
    }

    private OrderChangeWS getUpdatedOrderChange(OrderLineWS[] orderLineWSArray, OrderChangeWS[] oldOrderChanges, Integer itemId, Integer orderStatusId) {
        Optional<OrderChangeWS> existingOrderChange = getOrderChange(oldOrderChanges, itemId);
        if(existingOrderChange.isPresent()) {
            Date startDate = existingOrderChange.get().getStartDate();
            if(originationDateFormatter.format(new Date()).equals(originationDateFormatter.format(startDate))) {
                return existingOrderChange.get();
            } else {
                OrderChangeWS oldChange = existingOrderChange.get();
                sessionBean.updateOrderChangeEndDate(oldChange.getId(), new Date());
                OrderLineWS tempOrderLine = null;
                for (OrderLineWS orderLineWS : orderLineWSArray) {
                    if (0 == orderLineWS.getItemId().compareTo(itemId)) {
                        tempOrderLine = orderLineWS;
                        break;
                    }
                }
                if (tempOrderLine != null) {
					OrderChangeWS updatedChange = OrderChangeBL.buildFromLine(tempOrderLine, order, orderStatusId);
					updatedChange.setQuantity(tempOrderLine.getQuantityAsDecimal());
					return updatedChange;
                }
            }
        } 
        return null;
    }

    private Optional<OrderChangeWS> getOrderChange(OrderChangeWS[] changes, Integer itemId){
        return Arrays.stream(changes).filter(orderChangeWS ->
        (0 == orderChangeWS.getItemId().compareTo(itemId) && null == orderChangeWS.getEndDate())).sorted(Comparator.comparingInt(OrderChangeWS::getId).reversed()).
        findFirst();
    }

    private BigDecimal getOriginationLineQuantity(OrderLineWS[] oldOrderLines, BigDecimal currentQuantity, Integer subscriptionItemId) {
        BigDecimal oldQuantity;
        oldQuantity = Arrays.stream(oldOrderLines).filter(orderLineWS -> 0 != orderLineWS.getItemId().compareTo(subscriptionItemId)).
                map(OrderLineWS::getQuantityAsDecimal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return oldQuantity.add(currentQuantity);
    }

    private Optional<OrderLineWS> getLineByItemId(OrderLineWS[] lines, Integer itemId) {
        return Arrays.stream(lines)
                .filter(line -> line.getItemId().equals(itemId))
                .findFirst();
    }
    private OrderWS buildOrderWS(Integer userId, Integer itemId, Integer quantity, BigDecimal price, Integer currencyId, Integer orderPeriodId) {
        OrderLineWS[] orderLineWSArray = new OrderLineWS[1];
        orderLineWSArray[0] = buildOrderLine(itemId, quantity, price);
        OrderWS orderWS = new OrderWS();
        orderWS.setActiveSince(new Date());
        orderWS.setOrderLines(orderLineWSArray);
        orderWS.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        orderWS.setPeriod(orderPeriodId);
        orderWS.setUserId(userId);
        orderWS.setCurrencyId(currencyId);
        orderWS.setProrateFlag(true);
        return orderWS;
    }

    private OrderLineWS buildOrderLine(Integer itemId, Integer quantity, BigDecimal price) {
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(quantity);
        line.setDescription(getItemDescription(itemId));
        line.setItemId(itemId);
        line.setPrice(price);
        line.setAmount(price);
        line.setPriceAsDecimal(price);
        line.setAmountAsDecimal(price);
        return line;
    }

    private String getItemDescription(Integer itemId){
        ItemDTO item = new ItemDAS().find(itemId);
        return item.getDescription(item.getEntity().getLanguageId());
    }

    private Integer createOrgCountPositionRecord(Integer orderId, BigDecimal count, BigDecimal oldCount, Integer itemId) {
        OrgCountPositionDAS das = new OrgCountPositionDAS();
        OrgCountPositionDTO record = new OrgCountPositionDTO(orgId, billableOrgId, count, oldCount, orderId, itemId, entityId);
        Integer recordId =  das.save(record).getId();
        LOG.debug("Created Org Count Record {} for order {} with Quantity {}", record, orderId, count);
        return recordId;
    }

    private Integer updateOrgCountPositionRecord(OrgCountPositionDTO record, BigDecimal count, BigDecimal oldCount) {
        OrgCountPositionDAS das = new OrgCountPositionDAS();
        if(count.intValue() == 0) {
            record.setDeleted(1);
        }
        record.setCount(count);
        record.setOldCount(oldCount);
        record.setLastUpdatedDate(TimezoneHelper.serverCurrentDate());
        Integer recordId =  das.save(record).getId();
        LOG.debug("Updated Org Count Record {} for order {} with Quantity {}", record, record.getOrderId(), count);
        return recordId;
    }

    public Integer getOrderId () {
        return Objects.nonNull(order) ? order.getId() : null;
    }

}
