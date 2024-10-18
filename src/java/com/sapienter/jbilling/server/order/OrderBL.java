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

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.resources.OrderMetaFieldValueWS;
import com.sapienter.jbilling.server.customerInspector.domain.ListField;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.db.DiscountDAS;
import com.sapienter.jbilling.server.discount.db.DiscountDTO;
import com.sapienter.jbilling.server.discount.db.DiscountLineDAS;
import com.sapienter.jbilling.server.discount.db.DiscountLineDTO;
import com.sapienter.jbilling.server.filter.Filter;
import com.sapienter.jbilling.server.filter.FilterConstraint;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.AssetBL;
import com.sapienter.jbilling.server.item.AssetReservationBL;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDecimalsException;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.PlanItemBL;
import com.sapienter.jbilling.server.item.PlanItemBundleBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemBundleDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDAS;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.item.event.AbstractAssetEvent;
import com.sapienter.jbilling.server.item.event.AssetAddedToOrderEvent;
import com.sapienter.jbilling.server.item.event.AssetUpdatedEvent;
import com.sapienter.jbilling.server.item.event.SwapAssetsEvent;
import com.sapienter.jbilling.server.item.tasks.IItemPurchaseManager;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.db.OrderBillingTypeDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineTierDTO;
import com.sapienter.jbilling.server.order.db.OrderLineTypeDAS;
import com.sapienter.jbilling.server.order.db.OrderLineTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderLineUsagePoolDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusBL;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderStatusDTO;
import com.sapienter.jbilling.server.order.event.EditOrderEvent;
import com.sapienter.jbilling.server.order.event.NewActiveSinceEvent;
import com.sapienter.jbilling.server.order.event.NewActiveUntilEvent;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.order.event.NewPriceEvent;
import com.sapienter.jbilling.server.order.event.NewQuantityEvent;
import com.sapienter.jbilling.server.order.event.NewStatusEvent;
import com.sapienter.jbilling.server.order.event.OrderDeletedEvent;
import com.sapienter.jbilling.server.order.event.OrderMetaFieldUpdateEvent;
import com.sapienter.jbilling.server.order.event.OrderUpdatedEvent;
import com.sapienter.jbilling.server.order.event.PeriodCancelledEvent;
import com.sapienter.jbilling.server.order.event.UpdateProrateFlagEvent;
import com.sapienter.jbilling.server.order.validator.OrderAssetsValidator;
import com.sapienter.jbilling.server.order.validator.OrderHierarchyValidator;
import com.sapienter.jbilling.server.pluggableTask.OrderProcessingTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.PeriodUnitDAS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandBL;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.provisioning.ProvisioningRequestWS;
import com.sapienter.jbilling.server.provisioning.db.OrderProvisioningCommandDTO;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDAS;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningRequestDTO;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningStatusDAS;
import com.sapienter.jbilling.server.provisioning.event.SubscriptionActiveEvent;
import com.sapienter.jbilling.server.provisioning.event.SubscriptionInactiveEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanSubscriptionEvent;
import com.sapienter.jbilling.server.usagePool.event.CustomerPlanUnsubscriptionEvent;
import com.sapienter.jbilling.server.usagePool.event.SwapPlanFUPTransferEvent;
import com.sapienter.jbilling.server.user.ContactBL;
import com.sapienter.jbilling.server.user.CustomerPriceBL;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserCodeDTO;
import com.sapienter.jbilling.server.user.db.UserCodeOrderLinkDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.DescriptionBL;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;

/**
 * @author Emil
 */
public class OrderBL extends ResultList implements OrderSQL {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String DISCOUNT_NOTE = "System generated Discount Order";
    private OrderDTO order = null;
    private OrderLineDAS orderLineDAS = null;
    private OrderPeriodDAS orderPeriodDAS = null;
    private OrderDAS orderDas = null;
    private OrderBillingTypeDAS orderBillingTypeDas = null;
    private DiscountLineDAS discountLineDas = null;
    private ProvisioningStatusDAS provisioningStatusDas = null;
    private AssetDAS assetDAS = null;
    private EventLogger eLogger = null;
    private boolean addBundleItems = true;

    public OrderBL(Integer orderId) {
        init();
        set(orderId);
    }

    public OrderBL() {
        init();
    }

    public OrderBL(OrderDTO order) {
        init();
        this.order = order;
    }

    public boolean isAddBundleItems() {
        return addBundleItems;
    }

    public void setAddBundleItems(boolean addBundleItems) {
        this.addBundleItems = addBundleItems;
    }

    /**
     * Method lookUpEditable.
     * Gets the row from order_line_type for the type specifed
     * @param type
     * The order line type to look.
     * @return Boolean
     * If it is editable or not
     * @throws SessionInternalError
     * If there was a problem accessing the entity bean
     */
    static public Boolean lookUpEditable(Integer type)
            throws SessionInternalError {
        Boolean editable = null;

        try {
            OrderLineTypeDAS das = new OrderLineTypeDAS();
            OrderLineTypeDTO typeBean = das.find(type);

            editable = typeBean.getEditable() == 1;
        } catch (Exception e) {
            logger.error("Exception looking up the editable flag of an order line type. Type = {}, {}", type, e);
            throw new SessionInternalError("Looking up editable flag");
        }

        return editable;
    }

    public static boolean validate(OrderWS dto) {
        boolean retValue = true;

        if (dto.getUserId() == null || dto.getPeriod() == null ||
                dto.getBillingTypeId() == null ||
                dto.getOrderLines() == null) {
            retValue = false;
        } else {
            for (int f = 0; f < dto.getOrderLines().length; f++) {
                if (!validate(dto.getOrderLines()[f])) {
                    retValue = false;
                    break;
                }
            }
        }
        return retValue;
    }

    public static boolean validate(OrderLineWS dto) {
        boolean retValue = true;

        if (dto.getTypeId() == null ||
                dto.getDescription() == null || dto.getQuantity() == null) {
            retValue = false;
        }

        return retValue;
    }

    public static OrderDTO getOrCreateCurrentOrder(Integer userId, Date eventDate, Integer itemId,
            Integer currencyId, boolean persist) {
        return getOrCreateCurrentOrder(userId, eventDate, itemId, currencyId, persist, null, null);
    }

    public static OrderDTO getOrCreateCurrentOrder(Integer userId, Date eventDate, Integer itemId,
            Integer currencyId, boolean persist, String processId, Integer entityId) {
        return getOrCreateCurrentOrder(userId, eventDate, itemId, currencyId, persist, processId, entityId, null);
    }

    /**
     * order is not attached to the session.
     *
     * @param userId
     * @param eventDate
     * @param currencyId
     * @param processId - mediation process id. Only required if you want one order per mediation run.
     * @param entityId - Only required per mediation run
     * @param assetIdentifier
     * @return
     */
    public static OrderDTO getOrCreateCurrentOrder(Integer userId, Date eventDate, Integer itemId,
            Integer currencyId, boolean persist, String processId, Integer entityId, String assetIdentifier) {
        CurrentOrder co = null;
        if(StringUtils.isNotBlank(assetIdentifier)) {
            co = new AssetBasedCurrentOrder(userId, eventDate, itemId, processId, entityId, assetIdentifier);
        } else {
            co = new CurrentOrder(userId, eventDate, itemId, processId, entityId);
        }
        Integer currentOrderId = co.getCurrent();
        if (currentOrderId == null) {
            UserBL userBL = new UserBL(co.getUserId());
            // this is almost an error, put them in a new order?
            currentOrderId = co.create(co.getEventDate(), userBL.getCurrencyId(), userBL.getEntityId(co.getUserId()));
            logger.warn("Created current one-time order without a suitable main subscription order: {}", currentOrderId);
        }

        OrderDAS orderDas = new OrderDAS();
        OrderDTO order = orderDas.find(currentOrderId);

        if (!persist) {
            order.touch();
            orderDas.detach(order);
        }

        return order;
    }

    /**
     * For the mediation process, get or create a recurring order. The returned
     * order is not attached to the session.
     *
     * @param userId
     * @param eventDate
     * @param currencyId
     * @return
     */
    public static OrderDTO getOrCreateRecurringOrder(Integer userId, Integer itemId, Date eventDate,
            Integer currencyId, boolean persist, Integer billingTypeId) {
        OrderDTO order;
        order = new OrderDAS().findRecurringOrder(userId, itemId);

        // if no recurring order is found then we create a new one.
        if (order == null) {
            Integer entityId = new UserBL().getEntityId(userId);
            order = new OrderDTO();
            order.setCurrency(new CurrencyDTO(currencyId));

            // add notes
            try {
                EntityBL entity = new EntityBL(entityId);
                ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", entity.getLocale());
                order.setNotes(bundle.getString("order.recurring.new.notes"));
            } catch (Exception e) {
                throw new SessionInternalError("Error setting the new order notes", CurrentOrder.class, e);
            }

            order.setActiveSince(eventDate);
            OrderBL orderBL = new OrderBL();
            orderBL.set(order);
            OrderPeriodDTO orderPeriod = new OrderPeriodDAS().findRecurringPeriod(entityId);

            if (orderPeriod == null) {
                logger.debug("No period different than One-Time was found.");
                return null;
            }

            orderBL.addRelationships(userId, orderPeriod.getId(), currencyId);
            order.setOrderBillingType(orderBL.orderBillingTypeDas.find(billingTypeId));
            Integer orderId = orderBL.create(entityId, null, order, null);

            OrderDAS orderDas = new OrderDAS();
            order = orderDas.find(orderId);

            if (!persist) {
                order.touch();
                orderDas.detach(order);
            }
        }

        return order;
    }

    public static OrderDTO createAsWithLine(OrderDTO order, Integer itemId, Double quantity) {
        return createAsWithLine(order, itemId, new BigDecimal(quantity).setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
    }

    public static OrderDTO createAsWithLine(OrderDTO order, Integer itemId, BigDecimal quantity) {
        // copy the current order
        OrderDTO newOrder = new OrderDTO(order);
        newOrder.setId(0);
        newOrder.setVersionNum(null);
        // the period needs to be in the session
        newOrder.setOrderPeriodId(order.getOrderPeriod().getId());
        // the status should be active
        OrderStatusDAS orderStatusDAS = new OrderStatusDAS();
        newOrder.setOrderStatus(orderStatusDAS.find(orderStatusDAS.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, order.getUser().getCompany().getId())));
        // but without the lines
        newOrder.getLines().clear();
        // but do get the new line in
        OrderLineBL.addItem(newOrder, itemId, quantity);

        return new OrderDAS().save(newOrder);
    }

    /**
     * Create or update existed hierarchy from updatedDtos and orderChanges.
     * Method will perform validation for hierarchy and assets during update
     * @param hierarchyRootOrder persisted hierarchy order root, null otherwise
     * @param updatedRootDTO updated orders tree root
     * @param orderChanges order changes to try to apply
     * @param onDate order changes apply date
     * @return update orders hierarchy root
     */
    public static OrderDTO updateOrdersFromDto(OrderDTO hierarchyRootOrder, OrderDTO updatedRootDTO,
            Collection<OrderChangeDTO> orderChanges, Date onDate, Integer entityId) {
        LinkedHashSet<OrderDTO> updatedOrders = OrderHelper.findOrdersInHierarchyFromRootToChild(updatedRootDTO);
        // parent could be updated, save previous parents in separate list
        LinkedHashSet<OrderDTO> previousParents = new LinkedHashSet<OrderDTO>();
        OrderDTO tmpOrder = hierarchyRootOrder != null ? hierarchyRootOrder.getParentOrder() : null;
        while (tmpOrder != null) {
            previousParents.add(tmpOrder);
            tmpOrder = tmpOrder.getParentOrder();
        }
        Map<OrderDTO, OrderDTO> updatedToPersistedOrdersMap = new HashMap<OrderDTO, OrderDTO>();
        Map<OrderLineDTO, OrderLineDTO> updatedToPersistedOrderLinesMap = new HashMap<OrderLineDTO, OrderLineDTO>();
        // first orders creation is possible
        if (hierarchyRootOrder == null) {
            hierarchyRootOrder = updatedRootDTO;
        }
        for (OrderDTO updatedDTO : updatedOrders) {
            if (updatedDTO.getId() == null) {
                updatedDTO.getLines().clear(); //order lines could be created only via orderChange
                updatedDTO.getChildOrders().clear(); // will be set later via parent link
                updatedToPersistedOrdersMap.put(updatedDTO, updatedDTO);
                if (updatedDTO.getParentOrder() != null) {
                    OrderDTO parentOrder = updatedToPersistedOrdersMap.get(updatedDTO.getParentOrder());
                    if (parentOrder != null) {
                        updatedDTO.setParentOrder(parentOrder);
                        parentOrder.getChildOrders().add(updatedDTO);
                    }
                }
            } else {
                OrderDTO targetOrder = OrderHelper.findOrderInHierarchy(hierarchyRootOrder, updatedDTO.getId());
                if (targetOrder == null) {
                    // search in previous parents
                    for (OrderDTO tmp : previousParents) {
                        if (tmp.getId().equals(updatedDTO.getId())) {
                            targetOrder = tmp;
                            break;
                        }
                    }
                    if (targetOrder == null) {
                        continue;
                    }
                }
                updatedToPersistedOrdersMap.put(updatedDTO, targetOrder);
                updateOrderOwnFields(targetOrder, updatedDTO, true);
                // if parent was set to NULL in input order, check that previous parent is in input hierarchy too
                // If previous parent is in input hierarchy - this means, current order parent was reset.
                // Otherwise - input hierarchy was trimmed at this point (part of hierarchy as input).
                // It DOES NOT mean, that parent should be reset to NULL in target order
                if (updatedDTO.getParentOrder() == null && targetOrder.getParentOrder() != null
                        && OrderHelper.findOrderInHierarchy(updatedRootDTO, targetOrder.getParentOrder().getId()) != null) {
                    targetOrder.getParentOrder().getChildOrders().remove(targetOrder);
                    targetOrder.setParentOrder(null);
                } else if (updatedDTO.getParentOrder() != null) {
                    // update parent order for target order if it was updated in input one
                    OrderDTO parentOrder = updatedToPersistedOrdersMap.get(updatedDTO.getParentOrder());
                    if (parentOrder != null) {
                        targetOrder.setParentOrder(parentOrder);
                        parentOrder.getChildOrders().add(targetOrder);
                    }
                }
                // only update lines, deleting and creating via orderChange
                for (OrderLineDTO line : targetOrder.getLines()) {
                    OrderLineDTO updatedLine = OrderHelper.findOrderLineWithId(updatedDTO.getLines(), line.getId());
                    if (updatedLine != null) {
                        updatedToPersistedOrderLinesMap.put(updatedLine, line);
                        updateOrderLineOwnFields(line, updatedLine);
                        // if parent was set to NULL in input line, check that previous parent line ordr  is in input hierarchy too
                        // If previous parent line order is in input hierarchy - this means, current line parent was reset.
                        // Otherwise - input hierarchy was trimmed at this point (part of hierarchy as input).
                        // It DOES NOT mean, that parent should be reset to NULL in target order line
                        if (updatedLine.getParentLine() == null && line.getParentLine() != null &&
                                OrderHelper.findOrderInHierarchy(updatedRootDTO, line.getParentLine().getPurchaseOrder().getId()) != null) {
                            line.getParentLine().getChildLines().remove(line);
                            line.setParentLine(null);
                        } else if (updatedLine.getParentLine() != null && updatedLine.getParentLine().getId() > 0) {
                            // update parent order line for target line if it was updated in input one
                            OrderLineDTO parentLine = updatedToPersistedOrderLinesMap.get(updatedLine.getParentLine());
                            if (parentLine != null) {
                                line.setParentLine(parentLine);
                                parentLine.getChildLines().add(line);
                            }
                        }
                    }
                }
            }
        }
        OrderChangeBL.applyChangesToOrderHierarchy(hierarchyRootOrder, orderChanges, onDate, true, entityId);
        return hierarchyRootOrder;
    }

    /**
     * Update fields of target order from updated input order. Order Lines will not be updated
     * @param targetOrder Target order for update
     * @param updatedOrder Input order for update from
     * @param updateAuditedFields Flag to indicate is update for audited order fields such as 'Active until', 'Active since', 'Status', etc is needed
     */
    private static void updateOrderOwnFields(OrderDTO targetOrder, OrderDTO updatedOrder, boolean updateAuditedFields) {
        targetOrder.setVersionNum(updatedOrder.getVersionNum());
        targetOrder.setOrderBillingType(updatedOrder.getOrderBillingType());
        targetOrder.setNotify(updatedOrder.getNotify());
        targetOrder.setDueDateUnitId(updatedOrder.getDueDateUnitId());
        targetOrder.setDueDateValue(updatedOrder.getDueDateValue());
        targetOrder.setDfFm(updatedOrder.getDfFm());
        targetOrder.setAnticipatePeriods(updatedOrder.getAnticipatePeriods());
        targetOrder.setOwnInvoice(updatedOrder.getOwnInvoice());
        targetOrder.setNotes(updatedOrder.getNotes());
        targetOrder.setNotesInInvoice(updatedOrder.getNotesInInvoice());
        // order cancellation fields
        targetOrder.setCancellationFee(updatedOrder.getCancellationFee());
        targetOrder.setCancellationFeePercentage(updatedOrder.getCancellationFeePercentage());
        targetOrder.setCancellationFeeType(updatedOrder.getCancellationFeeType());
        targetOrder.setCancellationMaximumFee(updatedOrder.getCancellationMaximumFee());
        targetOrder.setCancellationMinimumPeriod(updatedOrder.getCancellationMinimumPeriod());
        targetOrder.setAutoRenew(updatedOrder.isAutoRenew());
        targetOrder.setRenewNotification(updatedOrder.getRenewNotification());
        Integer entityId = targetOrder.getBaseUserByUserId().getCompany().getId();
        // update and validate custom fields
        targetOrder.updateMetaFieldsWithValidation(targetOrder.getBaseUserByUserId().getCompany().getLanguageId(), entityId, null, updatedOrder);
        if (updateAuditedFields) {
            targetOrder.setActiveUntil(updatedOrder.getActiveUntil());
            targetOrder.setActiveSince(updatedOrder.getActiveSince());
            if (updatedOrder.getOrderStatus() != null) {
                if ((updatedOrder.getOrderStatus().getId() > 0)
                        && !(targetOrder.getOrderStatus().getId() == (updatedOrder
                                .getOrderStatus().getId()))) {
                    targetOrder.setOrderStatus(new OrderStatusDAS()
                    .find(updatedOrder.getOrderStatus().getId()));
                }
            }
            targetOrder.setNextBillableDay(updatedOrder.getNextBillableDay());
            if (targetOrder.getOrderPeriod().getId() != updatedOrder.getOrderPeriod().getId()) {
                targetOrder.setOrderPeriod(new OrderPeriodDAS().find(updatedOrder.getOrderPeriod().getId()));
            }
        }

        targetOrder.setProrateFlag(updatedOrder.getProrateFlag());
        targetOrder.setParentUpgradeOrderId(updatedOrder.getParentUpgradeOrderId());
        targetOrder.setUpgradeOrderId(updatedOrder.getUpgradeOrderId());
        targetOrder.setRenewOrderId(updatedOrder.getRenewOrderId());
        targetOrder.setRenewNotification(updatedOrder.getRenewNotification());

        //update UserCodeOrderLinkDTO set
        UserBL.updateAssociateUserCodesToLookLikeTarget(targetOrder, updatedOrder.getUserCodeLinks(), "OrderWS,userCode");

    }

    /**
     * Update allowed for update without orderChange fields in Target Line from input Updated Line
     * @param targetLine Target Order Line for update
     * @param updatedLine Input updated order line
     */
    public static void updateOrderLineOwnFields(OrderLineDTO targetLine, OrderLineDTO updatedLine) {
        // update only allowed fields in line
        targetLine.setVersionNum(updatedLine.getVersionNum());
        targetLine.setUseItem(updatedLine.getUseItem());
        targetLine.setDescription(updatedLine.getDescription());
        targetLine.setAmount(updatedLine.getAmount());
        if (targetLine.getItem() != null) {
            MetaFieldHelper.updateMetaFieldsWithValidation(targetLine.getItem().getEntity().getLanguageId(),
                    targetLine.getItem().getOrderLineMetaFields(), targetLine, updatedLine);
        }
        //todo: is this fields updatable
        targetLine.setOrderLineType(updatedLine.getOrderLineType());
        targetLine.setProvisioningStatus(updatedLine.getProvisioningStatus());
    }

    /**
     * Expire any penalty order that were created via OverdueInvoicePenaltyTask
     * @param invoice
     */
    public static void expirePenaltyOrderForInvoice(InvoiceDTO invoice) {
        logger.debug("Search order activeSince {}", invoice.getDueDate());
        List<OrderDTO> orders= new OrderDAS().findPenaltyOrderForInvoice(invoice);
        if (null != orders && orders.size() > 0 ) {
            logger.debug("Found {} orders.", orders.size());
            for (OrderDTO order : orders) {
                if (order.getActiveUntil() == null) {
                    logger.debug("Line description: {}", order.getLines().get(0).getDescription());

                    InvoiceDTO temp = invoice;
                    while (temp != null) {
                        logger.debug("Invoice number {}", temp.getPublicNumber());
                        if (order.getLines().get(0).getDescription().contains("Overdue Penalty for Invoice Number " + temp.getPublicNumber())) {
                            logger.debug("Found penalty order with order line description as {}", order.getLines().get(0).getDescription());
                            //order is penalty, not one time, matches description
                            order.setActiveUntil(TimezoneHelper.companyCurrentDate(order.getUser().getCompany().getId()));
                            logger.debug("Expired the penalty Order {}", order.getId());
                        }

                        temp = temp.getInvoice();
                    }
                }
            }
        }
    }

    private void init() {
        discountLineDas = new DiscountLineDAS();
        eLogger = EventLogger.getInstance();
        orderLineDAS = new OrderLineDAS();
        orderPeriodDAS = new OrderPeriodDAS();
        orderDas = new OrderDAS();
        orderBillingTypeDas = new OrderBillingTypeDAS();
        provisioningStatusDas = new ProvisioningStatusDAS();
        assetDAS = new AssetDAS();
    }

    public OrderDTO getEntity() {
        return order;
    }

    public OrderPeriodDTO getPeriod(Integer language, Integer id) {
        return (orderPeriodDAS.find(id));
    }

    public void set(Integer id) {
        order = orderDas.find(id);
    }

    public void setForUpdate(Integer id) {
        order = orderDas.findForUpdate(id);
    }

    public void set(OrderDTO newOrder) {
        order = newOrder;
    }

    public OrderWS getWS(Integer languageId) {
        Map<OrderDTO, OrderWS> orderHierarchyMap = new HashMap<>();
        Map<OrderLineDTO, OrderLineWS> orderLinesHierarchyMap  = new HashMap<>();
        return getWS(languageId, order, orderHierarchyMap, orderLinesHierarchyMap);
    }

    public OrderWS[] getOrderMetaFieldMap(OrderWS[] orderWS) {
        List<OrderWS> orders = new ArrayList<>();
        Arrays.asList(orderWS).stream().filter(ws -> null != ws && null != ws.getMetaFields()).forEach(ws -> {
            Map<String,String> orderWsMap = new HashMap<>();
            Arrays.asList(ws.getMetaFields()).stream().filter(value -> null != value && null != value.getMetaField()).forEach( value -> {
                if (value.getValue() instanceof char[]) {
                    orderWsMap.put(value.getMetaField().getName(), String.valueOf((char[])value.getValue()));
                } else {
                    orderWsMap.put(value.getMetaField().getName(), String.valueOf(value.getValue()));
                }
            });
            ws.setOrderMetaFieldMap(orderWsMap);
            orders.add(ws);
        });
        return orders.toArray(new OrderWS[0]);
    }

    private OrderWS getWS(Integer languageId, OrderDTO orderDto, Map<OrderDTO, OrderWS> orderHierarchyMap, Map<OrderLineDTO, OrderLineWS> orderLinesHierarchyMap) {
        OrderWS retValue = orderHierarchyMap.get(orderDto);
        if (retValue == null) {
            retValue = new OrderWS(orderDto.getId(), orderDto.getBillingTypeId(),
                    orderDto.getNotify(), orderDto.getActiveSince(), orderDto.getActiveUntil(),
                    orderDto.getCreateDate(), orderDto.getNextBillableDay(),
                    orderDto.getCreatedBy(), orderDto.getStatusId(), OrderStatusBL.getOrderStatusWS(orderDto.getOrderStatus()), orderDto.getDeleted(),
                    orderDto.getCurrencyId(), orderDto.getLastNotified(),
                    orderDto.getNotificationStep(), orderDto.getDueDateUnitId(),
                    orderDto.getDueDateValue(), orderDto.getAnticipatePeriods(),
                    orderDto.getDfFm(), orderDto.getNotes(), orderDto.getNotesInInvoice(),
                    orderDto.getOwnInvoice(), orderDto.getOrderPeriod().getId(),
                    orderDto.getBaseUserByUserId().getId(),
                    orderDto.getVersionNum(),
                    null != orderDto.getFreeUsageQuantity() ? orderDto.getFreeUsageQuantity() : orderDto.getFreeUsagePoolsTotalQuantity(),
                            orderDto.getProrateFlag(),
                            orderDto.getProrateAdjustmentFlag());

            retValue.setTotal(orderDto.getTotal());

            retValue.setPeriodStr(orderDto.getOrderPeriod().getDescription(languageId));
            retValue.setStatusStr(orderDto.getOrderStatus() != null ? orderDto.getOrderStatus().getDescription(languageId) : null);
            retValue.setBillingTypeStr(orderDto.getOrderBillingType().getDescription(languageId));
            retValue.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(
                    new UserBL().getEntityId(orderDto.getBaseUserByUserId().getId()), orderDto));
            retValue.setAutoRenew(orderDto.isAutoRenew());
            retValue.setRenewNotification(orderDto.getRenewNotification());

            List<OrderLineWS> lines = new ArrayList<>();
            for (OrderLineDTO line : orderDto.getLines()) {
                if (line.getDeleted() == 0) {
                    lines.add(getOrderLineWS(line, orderLinesHierarchyMap));
                }
            }

            //discount lines
            List<DiscountLineWS> discountLines = new ArrayList<>();
            for (DiscountLineDTO dlDto : orderDto.getDiscountLines()) {
                DiscountLineWS ws = getDiscountLineWS(dlDto);
                discountLines.add(ws);
            }
            retValue.setDiscountLines(new DiscountLineWS[discountLines.size()]);
            discountLines.toArray(retValue.getDiscountLines());

            //this will initialized Generated Invoices in the OrderDTO instance
            orderDto.addExtraFields(languageId);
            retValue.setGeneratedInvoices(new InvoiceBL().DTOtoSimpleWS(new ArrayList<>(orderDto.getInvoices())));
            retValue.setOrderLines(new OrderLineWS[lines.size()]);
            lines.toArray(retValue.getOrderLines());

            retValue.setCancellationFee(orderDto.getCancellationFee());
            retValue.setCancellationFeePercentage(orderDto.getCancellationFeePercentage());
            retValue.setCancellationFeeType(orderDto.getCancellationFeeType());
            retValue.setCancellationMaximumFee(orderDto.getCancellationMaximumFee());
            retValue.setCancellationMinimumPeriod(orderDto.getCancellationMinimumPeriod());

            ProvisioningCommandBL commandBL = new ProvisioningCommandBL();
            retValue.setProvisioningCommands(commandBL.getCommandsList(orderDto));

            orderHierarchyMap.put(orderDto, retValue);
            if (orderDto.getParentOrder() != null) {
                retValue.setParentOrder(getWS(languageId, orderDto.getParentOrder(), orderHierarchyMap, orderLinesHierarchyMap));
            }

            retValue.setIsMediated(orderDto.getIsMediated());

            int index = 0;
            retValue.setChildOrders(new OrderWS[orderDto.getChildOrders().size()]);
            for (OrderDTO childOrder : orderDto.getChildOrders()) {
                retValue.getChildOrders()[index] = getWS(languageId, childOrder, orderHierarchyMap, orderLinesHierarchyMap);
                retValue.getChildOrders()[index].setParentOrderId(String.valueOf((retValue.getId())));
                index++;
            }

            String[] userCodes = UserBL.convertToUserCodeStringArray(orderDto.getUserCodeLinks());
            if (userCodes.length > 0) {
                retValue.setUserCode(userCodes[0]);
            }
        }
        logger.debug("Ret value ===" + retValue);

        retValue.setAccessEntities(getAccessEntities(orderDto.getUser()));
        if(orderDto.getId()!=null){
            List<OrderDTO> find = orderDas.findDiscountOrderByMetaFieldsValue(
                    orderDto.getUser().getCompany().getId(),
                    Constants.FREE_TRIAL_SUBSCRIPTION_ORDER_ID,
                    orderDto.getId());
            OrderDTO discountOrder = find.isEmpty() ? null : find.get(0);
            if(discountOrder!=null){
                retValue.setFreeTrial(Boolean.TRUE);
                retValue.setDiscountOrderFinished(Boolean.valueOf(discountOrder.isFinished()));
            }
        }

        logger.debug("Ret value === {}", retValue);
        return retValue;
    }

    private OrderWS getCopyWS(Integer languageId, OrderDTO orderDto, Map<OrderDTO, OrderWS> orderHierarchyMap, Map<OrderLineDTO, OrderLineWS> orderLinesHierarchyMap) {
        OrderWS retValue = orderHierarchyMap.get(orderDto);
        if (retValue == null) {
            retValue = new OrderWS(orderDto.getId(), orderDto.getBillingTypeId(),
                    orderDto.getNotify(), orderDto.getActiveSince(), orderDto.getActiveUntil(),
                    orderDto.getCreateDate(), orderDto.getNextBillableDay(),
                    orderDto.getCreatedBy(), orderDto.getStatusId(), OrderStatusBL.getOrderStatusWS(orderDto.getOrderStatus()), orderDto.getDeleted(),
                    orderDto.getCurrencyId(), orderDto.getLastNotified(),
                    orderDto.getNotificationStep(), orderDto.getDueDateUnitId(),
                    orderDto.getDueDateValue(), orderDto.getAnticipatePeriods(),
                    orderDto.getDfFm(), orderDto.getNotes(), orderDto.getNotesInInvoice(),
                    orderDto.getOwnInvoice(), orderDto.getOrderPeriod().getId(),
                    orderDto.getBaseUserByUserId().getId(),
                    orderDto.getVersionNum(),
                    null != orderDto.getFreeUsageQuantity() ? orderDto.getFreeUsageQuantity() : orderDto.getFreeUsagePoolsTotalQuantity(),
                            orderDto.getProrateFlag(),
                            orderDto.getProrateAdjustmentFlag());

            retValue.setTotal(orderDto.getTotal());

            retValue.setPeriodStr(orderDto.getOrderPeriod().getDescription(languageId));
            retValue.setStatusStr(orderDto.getOrderStatus() != null ? orderDto.getOrderStatus().getDescription(languageId) : null);
            retValue.setBillingTypeStr(orderDto.getOrderBillingType().getDescription(languageId));
            retValue.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(
                    new UserBL().getEntityId(orderDto.getBaseUserByUserId().getId()), orderDto));
            retValue.setAutoRenew(orderDto.isAutoRenew());
            retValue.setRenewNotification(orderDto.getRenewNotification());

            List<OrderLineWS> lines = new ArrayList<>();
            for (OrderLineDTO line : orderDto.getLines()) {
                if (line.getDeleted() == 0) {
                    lines.add(getOrderLineWS(line, orderLinesHierarchyMap));
                }
            }

            //discount lines
            List<DiscountLineWS> discountLines = new ArrayList<>();
            for (DiscountLineDTO dlDto : orderDto.getDiscountLines()) {
                DiscountLineWS ws = getDiscountLineWS(dlDto);
                discountLines.add(ws);
            }
            retValue.setDiscountLines(new DiscountLineWS[discountLines.size()]);
            discountLines.toArray(retValue.getDiscountLines());

            //this will initialized Generated Invoices in the OrderDTO instance
            orderDto.addExtraFields(languageId);
            retValue.setGeneratedInvoices(new InvoiceBL().DTOtoWS(new ArrayList<>(orderDto.getInvoices())));
            retValue.setOrderLines(new OrderLineWS[lines.size()]);
            lines.toArray(retValue.getOrderLines());

            retValue.setCancellationFee(orderDto.getCancellationFee());
            retValue.setCancellationFeePercentage(orderDto.getCancellationFeePercentage());
            retValue.setCancellationFeeType(orderDto.getCancellationFeeType());
            retValue.setCancellationMaximumFee(orderDto.getCancellationMaximumFee());
            retValue.setCancellationMinimumPeriod(orderDto.getCancellationMinimumPeriod());

            ProvisioningCommandBL commandBL = new ProvisioningCommandBL();

            retValue.setProvisioningCommands(commandBL.getCommandsList(orderDto));

            orderHierarchyMap.put(orderDto, retValue);
            if (orderDto.getParentOrder() != null) {
                retValue.setParentOrder(getCopyWS(languageId, orderDto.getParentOrder(), orderHierarchyMap, orderLinesHierarchyMap));
            }
            int index = 0;
            retValue.setChildOrders(new OrderWS[orderDto.getChildOrders().size()]);
            for (OrderDTO childOrder : orderDto.getChildOrders()) {
                retValue.getChildOrders()[index] = getCopyWS(languageId, childOrder, orderHierarchyMap, orderLinesHierarchyMap);
                retValue.getChildOrders()[index].setParentOrder(retValue);
                index++;
            }

            String[] userCodes = UserBL.convertToUserCodeStringArray(orderDto.getUserCodeLinks());
            if (userCodes.length > 0) {
                retValue.setUserCode(userCodes[0]);
            }

        }

        logger.debug("Ret value === {}", retValue);

        return retValue;
    }

    public OrderDTO getDTO() {
        return order;
    }

    /**
     * Calculates the order total price including the products directly added in order and
     * products added in order by a plan. (Editable or Non editable plan)
     *
     * @return the total calculated price of order
     */
    public static BigDecimal getTotalWithPlansAsDecimal(OrderWS ws,Integer languageId, Integer userId, Integer entityId, Integer currencyId) {
        BigDecimal lineTotal = BigDecimal.ZERO;

        for (OrderLineWS theOrderLineWs : ws.getOrderLines()) {
            // line is an editable plan
            List<PlanDTO> plans = new PlanBL().getPlansBySubscriptionItem(theOrderLineWs.getItemId());
            // line is a non editable plan
            if (plans != null && plans.size() > 0) {
                PlanDTO planDTO = plans.get(0);
                if (planDTO.getEditable() == 0) {
                    for (PlanItemDTO planItemDTO : planDTO.getPlanItems()) {
                        ItemDTO itemDTO = planItemDTO.getItem();
                        if (itemDTO.isPercentage()) {
                            if (planItemDTO.getBundle() != null && planItemDTO.getBundle().getQuantity() != null) {
                                lineTotal = lineTotal.add(itemDTO.getPrice()
                                        .multiply(planItemDTO.getBundle().getQuantity()));
                            }
                        } else {
                            if (planItemDTO.getBundle() != null && planItemDTO.getBundle().getQuantity() != null) {
                                lineTotal = lineTotal.add(planItemDTO.getPrice(TimezoneHelper.companyCurrentDate(entityId)).getRate()
                                        .multiply(planItemDTO.getBundle().getQuantity()));
                            }
                        }
                    }
                }
            }
        }

        return ws.getTotalAsDecimal() != null ? ws.getTotalAsDecimal().add(lineTotal) : lineTotal;
    }

    public static OrderPeriodWS getOrderPeriodWS(OrderPeriodDTO dto){
        if(null == dto) {
            return new OrderPeriodWS();
        }

        OrderPeriodWS ws = new OrderPeriodWS();
        ws.setId(dto.getId());
        ws.setEntityId(null != dto.getCompany() ? dto.getCompany().getId() : null);
        ws.setValue(dto.getValue());
        ws.setPeriodUnitId(null != dto.getPeriodUnit() ? dto.getPeriodUnit().getId() : null);
        ws.setDescriptions(getOrderPeriodDescriptions(dto.getId()));
        return ws;
    }

    private static List<InternationalDescriptionWS> getOrderPeriodDescriptions(int orderPeriodId) {
        JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
        JbillingTable table = tableDas.findByName(Constants.TABLE_ORDER_PERIOD);

        InternationalDescriptionDAS descriptionDas = Context.getBean(Context.Name.DESCRIPTION_DAS);
        Collection<InternationalDescriptionDTO> descriptionsDTO = descriptionDas.findAll(table.getId(), orderPeriodId, "description");

        List<InternationalDescriptionWS> descriptionsWS = new ArrayList<>();
        for (InternationalDescriptionDTO descriptionDTO : descriptionsDTO) {
            descriptionsWS.add(DescriptionBL.getInternationalDescriptionWS(descriptionDTO));
        }

        return descriptionsWS;
    }

    public static OrderProcessWS getOrderProcessWS(OrderProcessDTO dto){

        OrderProcessWS ws = new OrderProcessWS();
        ws.setId(dto.getId());
        ws.setBillingProcessId(dto.getBillingProcess() != null ? dto.getBillingProcess().getId() : null);
        ws.setOrderId(dto.getPurchaseOrder() != null ? dto.getPurchaseOrder().getId() : null);
        ws.setInvoiceId(dto.getInvoice() != null ? dto.getInvoice().getId() : null);
        ws.setPeriodsIncluded(dto.getPeriodsIncluded());
        ws.setPeriodStart(dto.getPeriodStart());
        ws.setPeriodEnd(dto.getPeriodEnd());
        ws.setReview(dto.getIsReview());
        ws.setOrigin(dto.getOrigin());
        return ws;
    }

    public void addItem(OrderDTO order, Integer itemID, BigDecimal quantity, Integer language, Integer userId,
            Integer entityId, Integer currencyId, List<CallDataRecord> records, List<OrderLineDTO> lines, boolean singlePurchase, String sipUri, Date eventDate) throws ItemDecimalsException {

        try {
            PluggableTaskManager<IItemPurchaseManager> taskManager = new PluggableTaskManager<>(entityId, Constants.PLUGGABLE_TASK_ITEM_MANAGER);
            IItemPurchaseManager myTask = taskManager.getNextClass();

            while (myTask != null) {
                myTask.addItem(itemID, quantity, language, userId, entityId, currencyId, order, records, lines, singlePurchase, sipUri, eventDate);
                myTask = taskManager.getNextClass();
            }

            // If customer is adding a plan item to the recurring order, and the customer does not
            // already hold a subscription to the plan, subscribe the customer and add all the plan
            // prices to the customer price map
            ItemDTO itemAdded = new ItemDAS().findNow(itemID);//is this item a plan?
            if (null != itemAdded && !itemAdded.getPlans().isEmpty()) {
                //TODO Should replace 'new Date()' with effectiveDate or orderChange.startDate for the new line/quantity
                if (!PlanBL.isSubscribed(userId, itemID, TimezoneHelper.companyCurrentDate(entityId))) {
                    PlanBL.subscribe(userId, itemID, TimezoneHelper.companyCurrentDate(entityId), null);
                }
            }

        } catch (PluggableTaskException e) {
            throw new SessionInternalError("Item Manager task error", OrderBL.class, e);
        } catch (TaskException e) {
            if (e.getCause() instanceof ItemDecimalsException) {
                throw (ItemDecimalsException) e.getCause();
            } else {
                // do not change this error text, it is used to identify the error
                throw new SessionInternalError("Item Manager task error", OrderBL.class, e);
            }
        }

    }

    public void addItem(OrderDTO order, Integer itemID, BigDecimal quantity, Integer language, Integer userId,
            Integer entityId, Integer currencyId, List<CallDataRecord> records, Date eventDate) throws ItemDecimalsException {
        addItem(order, itemID, quantity, language, userId, entityId, currencyId, records, null, false, null, eventDate);
    }

    public void addItem(Integer itemID, BigDecimal quantity, Integer language, Integer userId,
            Integer entityId, Integer currencyId, List<CallDataRecord> records, Date eventDate) {

        addItem(this.order, itemID,  quantity, language, userId, entityId, currencyId, records, null, false, null, eventDate);
    }

    public void addItem(Integer itemID, BigDecimal quantity, Integer language, Integer userId,
            Integer entityId, Integer currencyId, List<CallDataRecord> records, boolean singlePurchase, Date eventDate) {

        addItem(this.order, itemID,  quantity, language, userId, entityId, currencyId, records, null, singlePurchase, null, eventDate);
    }

    public void addItem(Integer itemID, BigDecimal quantity, Integer language, Integer userId, Integer entityId,
            Integer currencyId, Date eventDate) throws ItemDecimalsException {
        addItem(itemID, quantity, language, userId, entityId, currencyId, null, eventDate);
    }

    public void addItem(Integer itemID, Integer quantity, Integer language, Integer userId, Integer entityId,
            Integer currencyId, List<CallDataRecord> records, Date eventDate) throws ItemDecimalsException {
        addItem(itemID, new BigDecimal(quantity), language, userId, entityId, currencyId, records, eventDate);
    }

    public void addItem(Integer itemID, Integer quantity, Integer language, Integer userId, Integer entityId,
            Integer currencyId) throws ItemDecimalsException {
        addItem(itemID, new BigDecimal(quantity), language, userId, entityId, currencyId, null);
    }

    public void deleteItem(Integer itemID) {
        order.removeLine(itemID);
    }

    public String delete(Integer executorId) {
        String orderIds = "";
        OrderHierarchyValidator validator = new OrderHierarchyValidator();
        validator.buildHierarchy(order);
        String error = validator.deleteOrder(order.getId());
        if (error == null) {
            error = validator.validate(order.getUser().getEntity().getId());
        } else {
            return error;
        }

        if (error != null) {
            throw new SessionInternalError("Order deletion is impossible: " + error, new String[]{error});
        }

        List<OrderDTO> childOrders = getChildOrders(order);
        for (OrderDTO childOrder : childOrders) {
            orderIds += " " + childOrder.getId();
            deleteOrder(childOrder, executorId);
        }

        orderIds += " " + order.getId();
        deleteOrder(order, executorId);
        return orderIds;
    }

    public List<OrderDTO> getChildOrders(OrderDTO targetOrder) {
        List<OrderDTO> childOrders = new ArrayList<>();
        for (OrderDTO childOrder : targetOrder.getChildOrders()) {
            childOrders.add(childOrder);
            if (childOrder.getChildOrders().size() > 0) {
                childOrders.addAll(getChildOrders(childOrder));
            }
        }
        return childOrders;
    }

    private void deleteOrder(OrderDTO order, Integer executorId) {
        // the event is needed before the deletion
        EventManager.process(new OrderDeletedEvent(order.getBaseUserByUserId().getCompany().getId(), order));

        //asset create and update events
        List<AbstractAssetEvent> assetEvents = new ArrayList<>();
        //Customer plan un-subscription events
        List<CustomerPlanUnsubscriptionEvent> customerPlanUnsubscriptionEvents = new ArrayList<>();

        //unlink assets linked to any order line
        for (OrderLineDTO line : order.getLines()) {
            line.setDeleted(1);

            unlinkAssets(null, line, executorId, assetEvents, null);

            if (null != line.getItem() && line.getItem().hasPlans()) {
                customerPlanUnsubscriptionEvents.add(
                        new CustomerPlanUnsubscriptionEvent(
                                order.getBaseUserByUserId().getEntity().getId(),
                                order, Constants.CUSTOMER_PLAN_UNSUBSCRIBE_ORDER_DELETE, false)
                        );
            }
        }

        // remove customer plan subscriptions for this order
        if (order.getOrderPeriod().getId() != Constants.ORDER_PERIOD_ONCE) {
            removeCustomerPlans(order.getLines(), order.getUserId());
        }

        order.setDeleted(1);

        order.setDeletedDate(TimezoneHelper.companyCurrentDate(order.getUser().getEntity().getId()));
        orderDas.save(order);
        orderDas.flush();

        SubscriptionInactiveEvent newEvent = new SubscriptionInactiveEvent(executorId, order);
        EventManager.process(newEvent);
        logger.debug("OrderBL.delete(): generated SubscriptionInactiveEvent for order: {}", order.getId());
        eLogger.audit(executorId, order.getBaseUserByUserId().getId(),
                Constants.TABLE_PUCHASE_ORDER, order.getId(),
                EventLogger.MODULE_ORDER_MAINTENANCE,
                EventLogger.ROW_DELETED, null,
                null, null);

        //fire the asset events
        for (AbstractAssetEvent event : assetEvents) {
            EventManager.process(event);
        }

        //Fire the customer plan un-subscription events.
        //Presently there can be only one plan per order,
        //but in future we can have many plans on one order. Fire event for each plan.
        for (CustomerPlanUnsubscriptionEvent event : customerPlanUnsubscriptionEvents) {
            EventManager.process(event);
        }
    }

    /**
     * Method recalculate.
     * Goes over the processing tasks configured in the database for this
     * entity. The order entity is then modified.
     */
    public void recalculate(Integer entityId) throws SessionInternalError, ItemDecimalsException {
        logger.debug("Processing and order for reviewing.{}", order.getLines().size());
        // make sure the user is there
        UserDAS user = new UserDAS();
        order.setBaseUserByUserId(user.find(order.getBaseUserByUserId().getId()));
        // some things can't be null, otherwise hibernate complains
        order.setDefaults(entityId);
        order.touch();
        try {
            PluggableTaskManager taskManager = new PluggableTaskManager(
                    entityId, Constants.PLUGGABLE_TASK_PROCESSING_ORDERS);
            OrderProcessingTask task =
                    (OrderProcessingTask) taskManager.getNextClass();
            while (task != null) {
                task.doProcessing(order);
                task = (OrderProcessingTask) taskManager.getNextClass();
            }

        } catch (PluggableTaskException e) {
            logger.debug("Problems handling order processing task.", e);
            throw new SessionInternalError("Problems handling order " +
                    "processing task.");
        } catch (TaskException e) {
            if (e.getCause() instanceof ItemDecimalsException) {
                throw (ItemDecimalsException) e.getCause();
            }
            logger.debug("Problems excecuting order processing task.", e);
            throw new SessionInternalError("Problems executing order processing task.");
        }
    }

    /**
     * Loops through discount lines and performs 2 important validations:
     * 1. If the discount amount is zero, throw exception.
     * 2. If the discount amount is greater than order total or line amount or
     *    bundle item amount on which it is applied, then throw exception.
     * @throws SessionInternalError
     */
    public void validateDiscountLines() throws SessionInternalError {
        if (order.hasDiscountLines()) {
            BigDecimal totalDiscount = BigDecimal.ZERO;
            for (DiscountLineDTO discountLine : order.getDiscountLines()) {

                discountLine.setPurchaseOrder(order);
                DiscountDTO discount = discountLine.getDiscount();

                if (order.getOrderPeriod().getId() == Constants.ORDER_PERIOD_ONCE &&
                        discountLine.getDiscount().isPeriodBased()) {
                    throw new SessionInternalError("Periodic discount cannot be added to a Onetime Order.",
                            new String []{ "OrderWS,discountLines,periodic.discount.order.onetime," +
                                    order.getOrderPeriod().getDescription(
                                            order.getBaseUserByUserId().getLanguageIdField())});
                }

                BigDecimal discountableAmount = getDiscountableAmount(order, discountLine, true);
                BigDecimal discountAmount = discount.getDiscountAmount(discountableAmount);

                logger.debug("Discount Line: {}\n, discountableAmount: {}, discountAmount: {}", discountLine, discountableAmount, discountAmount);

                //#10625:fixes to show validation in if discount amount(in case of multiple
                //discount) greater then order amount.
                if (!discountLine.isPlanItemLevelDiscount()) {
                    totalDiscount= totalDiscount.add(discountAmount);
                }

                // Discount amount cannot be greater than line amount in case of line level discount
                // and cannot be greater than order total amount in case of order level discount
                if (discountAmount.abs().compareTo(discountableAmount) > 0) {
                    String formattedDiscountAmount = com.sapienter.jbilling.server.util.Util.
                            formatMoney(discountAmount, order.getUserId(), order.getCurrencyId(), false);
                    String formattedDiscountableAmount = com.sapienter.jbilling.server.util.Util.
                            formatMoney(discountableAmount, order.getUserId(), order.getCurrencyId(), false);
                    throw new SessionInternalError(String.format("Discount Amount %s is greater than Discountable Amount %s", formattedDiscountAmount, formattedDiscountableAmount),
                            new String []{ "DiscountWS,rate,discount.amount.greaterthan.line.or.order.amount," +
                                    formattedDiscountAmount + "," + formattedDiscountableAmount});
                }
            }

            if (order.getTotal().compareTo(BigDecimal.ZERO) > 0 && totalDiscount.compareTo(order.getTotal()) > 0) {
                String formattedDiscountTotal = com.sapienter.jbilling.server.util.Util.
                        formatMoney(totalDiscount, order.getUserId(), order.getCurrencyId(), false);
                String formattedOrderTotal = com.sapienter.jbilling.server.util.Util.
                        formatMoney(order.getTotal(), order.getUserId(), order.getCurrencyId(), false);
                throw new SessionInternalError(String.format("Discount Amount %s is greater than Discountable Amount %s", formattedDiscountTotal, formattedOrderTotal),
                        new String []{ "DiscountWS,rate,discount.amount.greaterthan.line.or.order.amount," +
                                formattedDiscountTotal + "," +formattedOrderTotal});
            }
        }
    }

    /**
     * This method will save order hierarchy, that is saving all orders and
     * order lines top (parent orders) and bottom (child orders) from this order
     *
     * This method should be used when the order hierarchy is created
     * without the order change mechanism.
     *
     * @param entityId entity
     * @param userAgentId current user
     * @param orderDto any order from hierarchy
     * @return id of persisted passed order
     * @throws SessionInternalError
     */
    public Integer create(Integer entityId, Integer userAgentId, OrderDTO orderDto)
            throws SessionInternalError {
        return create(entityId, userAgentId, orderDto, null);
    }

    /**
     * Set the line asset status to the default order saved status
     * Checks
     *  - item allows asset mangement
     *  - line quantity match the number of assets
     *  - asset is not linked to another order line
     *  - if asset is linked for the first time check that the old status is available
     * @param line order line to validate
     * @param unlinkedAssets - assets which have been removed from another line in this tx and are eligible for adding
     * @param eventsToFire assetChangeEvents to fire later
     */
    //    private void validateAssets(OrderLineDTO line, Integer executorId, Map<Integer, AssetDTO> unlinkedAssets, List<AbstractAssetEvent> eventsToFire) {
    //
    //        String errorCode = OrderAssetsValidator.validateAssets(line, unlinkedAssets);
    //        if (errorCode != null) {
    //            throw new SessionInternalError("Error during assets validation: " + errorCode, new String[] {errorCode});
    //        }
    //        if (line.getItem() == null || line.getItem().getAssetManagementEnabled() == 0) {
    //            return;
    //        }
    //        ItemDAS itemDAS = new ItemDAS();
    //
    //        ItemDTO itemDto = itemDAS.find(line.getItem().getId());
    //        AssetStatusDTO orderSavedStatus =  itemDto.findItemTypeWithAssetManagement() != null ?
    //                                                itemDto.findItemTypeWithAssetManagement().findOrderSavedStatus() : null;
    //
    //        //map assets from the new order instance (line) to assets in unlinkedAssets
    //        //if an asset has been unlinked from one line and linked to another we have to use the same
    //        //object instance that was removed in this line
    //        Map<AssetDTO, AssetDTO> assetsToReplace = new HashMap<AssetDTO, AssetDTO>(2);
    //
    //        for(AssetDTO asset : line.getAssets()) {
    //            //check if this asset was removed from another line
    //            if(unlinkedAssets.containsKey(asset.getId())) {
    //                AssetDTO assetDTO = unlinkedAssets.get(asset.getId());
    //                assetDTO.setOrderLine(line);
    //                assetsToReplace.put(asset, assetDTO);
    //                asset = assetDTO;
    //            }
    //
    //            if(asset.getPrevOrderLine() == null) {
    //                //this is a new asset to link
    //                AssetStatusDTO previousStatus = asset.getAssetStatus();
    //                asset.setAssetStatus(orderSavedStatus);
    //
    //                eventsToFire.add(new AssetAddedToOrderEvent(AssetBL.assetEntityIdFor(line, asset),
    //                        asset, line.getPurchaseOrder().getBaseUserByUserId(), executorId));
    //                eventsToFire.add(new AssetUpdatedEvent(AssetBL.assetEntityIdFor(line, asset),
    //                        asset, previousStatus, line.getPurchaseOrder().getBaseUserByUserId(), executorId));
    //            }
    //        }
    //
    //        //replace the asset in this line with the unlinked asset from another line
    //        for(AssetDTO assetDTO : assetsToReplace.keySet()) {
    //            line.removeAsset(assetDTO);
    //            line.addAsset(assetsToReplace.get(assetDTO));
    //        }
    //    }

    /**
     * This method will save order hierarchy, that is saving all orders and
     * order lines top (parent orders) and bottom (child orders) from this order
     * @param entityId entity
     * @param userAgentId current user
     * @param orderDto any order from hierarchy
     * @param appliedChanges an map of applied order change to order lines in this call
     * @return id of persisted passed order
     * @throws SessionInternalError
     */
    public Integer create(Integer entityId, Integer userAgentId,
            OrderDTO orderDto, Map<OrderLineDTO, OrderChangeDTO> appliedChanges)
                    throws SessionInternalError {

        OrderDTO rootOrder = OrderHelper.findRootOrderIfPossible(orderDto);

        // save hierarchy from root order
        Integer parentId = createOrderWithChildOrders(entityId, userAgentId, rootOrder, appliedChanges);
        // we should return initial order id - it was propagated to original dto
        order = orderDas.find(orderDto.getId());
        return order.getId();
    }

    /**
     * This method will create orders from top level to child orders recursively
     * Events and logs will be for each order in hierarchy.
     * Important! All parent orders (and lines) for this orders should be created already
     * @param entityId entity
     * @param userAgentId current user
     * @param orderDto target order
     * @return id of order created
     * @throws SessionInternalError
     */
    private Integer createOrderWithChildOrders(Integer entityId, Integer userAgentId,
            OrderDTO orderDto, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {
        orderDto.setId(null);
        orderDto.setVersionNum(null);
        int orderId = createSingleOrder(entityId, userAgentId, orderDto, appliedChanges);
        // flush and evict order for correct hierarchy propagation
        orderDas.flush();
        orderDas.detach(order);
        // store all child orders recursively
        for (OrderDTO childOrder : orderDto.getChildOrders()) {
            if (childOrder.getId() == null || childOrder.getId() == 0) {
                OrderBL bl = new OrderBL();
                bl.createOrderWithChildOrders(entityId, userAgentId, childOrder, appliedChanges);
            }
        }
        return orderId;
    }


    public void processCustomerPlanSubscription(List<OrderLineDTO> lines, Integer userId, OrderDTO orderDto, Date endDate) {

        for (OrderLineDTO line : lines) {
            if (null != line.getItem() && null != line.getItem().getPlans() &&
                    !line.getItem().getPlans().isEmpty() && line.getDeleted() == 0) {

                CustomerPlanSubscriptionEvent newEvent = new CustomerPlanSubscriptionEvent(new UserBL().getEntityId(userId),
                        null != orderDto ? orderDto : order,
                                line.getItem().getPlans().iterator().next(),
                                line, endDate);
                EventManager.process(newEvent);
            }
        }
    }

    public List<CustomerPlanSubscriptionEvent> generateCustomerPlanSubscriptionEvents(List<OrderLineDTO> lines, Integer userId, OrderDTO orderDto, Date endDate, boolean isPlanSwap, boolean isOrderCreation) {
        List<CustomerPlanSubscriptionEvent> events = new ArrayList<>();
        for (OrderLineDTO line : lines) {
            Date activeSince = line.getPurchaseOrder().getActiveSince();
            Date startDate = TimezoneHelper.companyCurrentDateByUserId(userId);
            if( null!= activeSince ) {
                startDate = activeSince;
            } else if(null != line.getStartDate()) {
                startDate = line.getStartDate();
            }
            if (line.hasItem() && line.getItem().hasPlans() && line.getDeleted() == 0 &&
                    (!PlanBL.isSubscribed(userId, line.getItemId(), startDate) || isPlanSwap || isOrderCreation)) {

                CustomerPlanSubscriptionEvent newEvent = new CustomerPlanSubscriptionEvent(new UserBL().getEntityId(userId),
                        null != orderDto ? orderDto : order,
                                line.getItem().getPlans().iterator().next(),
                                line, endDate);
                events.add(newEvent);
            }
        }
        return events;
    }

    public void processSwapPlanFUPTrasfer(Integer entityId, OrderDTO orderDto, Integer existingPlanItemId, Integer swapPlanItemId, Date effectiveDate) {
        if (null != orderDto && null != orderDto.getLines() &&
                null != existingPlanItemId && null != swapPlanItemId) {
            EventManager.process(new SwapPlanFUPTransferEvent(entityId, orderDto, existingPlanItemId, swapPlanItemId, effectiveDate));
        }
    }

    /**
     * Recalculate order's line price and amount
     * @param entityId
     */
    private void recalculateOrder(Integer entityId) {
        SessionFactory sf = Context.getBean(Name.HIBERNATE_SESSION);
        Session session = sf.getCurrentSession();
        FlushMode defaultFlushMode = session.getFlushMode();
        try {
            // set current session's flush mode to Manual as part of fix for: https://appdirect.jira.com/browse/JBSAPP-55
            session.setFlushMode(FlushMode.MANUAL);
            logger.debug("changed default flush mode {} to {}", defaultFlushMode, session.getFlushMode());
            recalculate(entityId);
        } finally {
            // Once recalculate logic executed set defaultFlushMode to session.
            logger.debug("changing flush mode {} to {}", session.getFlushMode(), defaultFlushMode);
            session.setFlushMode(defaultFlushMode);
            logger.debug("Session flush mode changed to {}", session.getFlushMode());
        }
    }
    /**
     * Create single order, child orders will not be stored in cascade
     * Important! All parent orders (and lines) for this orders should be created already
     * @param entityId entity
     * @param userAgentId current user
     * @param orderDto target order
     * @return id of order created
     */
    public Integer createSingleOrder(
            Integer entityId, Integer userAgentId, OrderDTO orderDto,
            Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {

        try {
            // if the order is a one-timer, force post-paid to avoid any
            // confusion. Everywhere in the rest of the app post-paid is forced.
            if (orderDto.getOrderPeriod().getId() == Constants.ORDER_PERIOD_ONCE) {
                orderDto.setOrderBillingType(orderBillingTypeDas.find(Constants.ORDER_BILLING_POST_PAID));
                // one time orders can not be the main subscription
            }
            UserDAS user = new UserDAS();
            if (userAgentId != null) {
                orderDto.setBaseUserByCreatedBy(user.find(userAgentId));
            }

            // create the record
            orderDto.setBaseUserByUserId(user.find(orderDto.getBaseUserByUserId().getId()));
            orderDto.setOrderPeriod(orderPeriodDAS.find(orderDto.getOrderPeriod().getId()));

            orderDto.setDefaults(entityId);

            List<Integer> subOrderIds = new ArrayList<>();
            List<Integer> bundledOrderIds= new ArrayList<>();

            // subscribe customer to plan items
            if (orderDto.getOrderPeriod().getId() != Constants.ORDER_PERIOD_ONCE) {
                // copy lines to a temp list and populate item from DB so that we can process
                // plans and avoid a LIE exception since we don't know where the DTO has come from.
                List<OrderLineDTO> lines = new ArrayList<OrderLineDTO>(orderDto.getLines());
                for (OrderLineDTO line : lines) {
                    line.setItem(new ItemBL(line.getItemId()).getEntity());
                }

                UserDTO baseUser = orderDto.getBaseUserByUserId();
                addCustomerPlans(lines, baseUser.getId(), orderDto.getActiveSince(), orderDto.getActiveUntil());
                if (addBundleItems) {
                    bundledOrderIds = addBundledItems(orderDto, lines, baseUser, appliedChanges);
                    if (!bundledOrderIds.isEmpty()) {
                        subOrderIds.addAll(bundledOrderIds);
                    }
                }
            }

            // update and validate meta fields
            orderDto.updateMetaFieldsWithValidation(new CompanyDAS().find(entityId).getLanguageId(), entityId, null, orderDto);

            //asset create and update events
            List<AbstractAssetEvent> assetEvents = new ArrayList<>();

            order = orderDto;

            ItemDAS itemDAS = new ItemDAS();

            // link the lines to the new order
            for (OrderLineDTO line : order.getLines()) {
                line.setPurchaseOrder(order);
                // If order line item is a plan, set empty its assets list.
                Integer lineItemID = line.getItemId();
                if (null != lineItemID && itemDAS.findNow(lineItemID).getPlans().size() > 0) {
                    line.setAssets(new HashSet<>());
                }
                if (line.getAssets() != null && !line.getAssets().isEmpty()) {
                    for (AssetDTO lineAsset : line.getAssets()) {
                        lineAsset.setOrderLine(line);
                    }
                }
                validateAssets(line, userAgentId, new HashMap<>(0), assetEvents, appliedChanges);
            }

            recalculateOrder(entityId);
            for (OrderLineDTO line : order.getLines()) {
                if (line.getParentLine() != null && line.getParentLine().getId() > 0) {
                    line.setParentLine(orderLineDAS.find(line.getParentLine().getId()));
                }
                line.getChildLines().clear();
            }

            if (order.getParentOrder() != null && order.getParentOrder().getId() != null) {
                order.setParentOrder(orderDas.find(order.getParentOrder().getId()));
            }

            if (CollectionUtils.isNotEmpty(order.getDiscountLines())) {
                for (DiscountLineDTO lineDTO : order.getDiscountLines()) {
                    lineDTO.setPurchaseOrder(order);
                }
            } else {
                order.setDiscountLines(new LinkedList<>());
            }
            validateDiscountLines();
            checkPlanItemHasTeaserPricing(orderDto);

            order = orderDas.save(orderDto);
            setProvisioningStatus(order);
            orderDas.save(order);
            orderDas.flush();
            orderDas.detach(order);

            List<CustomerPlanSubscriptionEvent> subscriptionEvents = new ArrayList<>();
            subscriptionEvents.addAll(generateCustomerPlanSubscriptionEvents(order.getLines(), order.getUserId(),
                    null, null, false, true));

            // creating customer usage pool and re rating usage order.
            for(CustomerPlanSubscriptionEvent subscriptionEvent: subscriptionEvents) {
                EventManager.process(subscriptionEvent);
            }
            // propagate id for created order and lines
            orderDto.setId(order.getId());
            Iterator<OrderLineDTO> orderLines = order.getLines().iterator();
            Iterator<OrderLineDTO> orderDtoLines = orderDto.getLines().iterator();
            while (orderDtoLines.hasNext() && orderLines.hasNext()) {
                orderDtoLines.next().setId(orderLines.next().getId());
            }
            // New Discounts : If order has discount lines defined, then proceed to create discount suborders or lines.
            if (CollectionUtils.isNotEmpty(orderDto.getDiscountLines())) {
                // Note the bundle order ids are being passed to find out the bundle orders created in case of plan item discounts.
                List<Integer> discountOrderIds = addDiscountItems(orderDto, bundledOrderIds);
                if (CollectionUtils.isNotEmpty(discountOrderIds)) {
                    subOrderIds.addAll(discountOrderIds);
                }
            }
            // set the primary order on all the bundled sub-orders, discount sub-orders and the primary order itself
            setPrimaryOrderForLinkedOrders(subOrderIds);

            //check if order is created with activeSince<= now (or null)
            Date now = TimezoneHelper.companyCurrentDate(order.getUser().getEntity().getId());
            Date activeSince = order.getActiveSince();
            if (activeSince == null || activeSince.before(now)) {
                // generate SubscriptionActiveEvent for order
                SubscriptionActiveEvent newEvent = new SubscriptionActiveEvent(entityId, order);
                EventManager.process(newEvent);
                logger.debug("OrderBL.create(): generated SubscriptionActiveEvent for order: {}", order.getId());
            }

            // add a log row for convenience
            if (userAgentId != null) {
                eLogger.audit(userAgentId, order.getBaseUserByUserId().getId(),
                        Constants.TABLE_PUCHASE_ORDER, order.getId(),
                        EventLogger.MODULE_ORDER_MAINTENANCE, EventLogger.ROW_CREATED, null, null, null);
            } else {
                eLogger.auditBySystem(entityId, order.getBaseUserByUserId().getId(),
                        Constants.TABLE_PUCHASE_ORDER, order.getId(),
                        EventLogger.MODULE_ORDER_MAINTENANCE, EventLogger.ROW_CREATED, null, null, null);
            }
            EventManager.process(new NewOrderEvent(entityId, order));

            //fire the asset events
            for (AbstractAssetEvent event : assetEvents) {
                EventManager.process(event);
            }
        } catch (SessionInternalError e) {
            throw e;
        } catch (Exception e) {
            throw new SessionInternalError("Create exception creating order entity bean", OrderBL.class, e);
        }
        return order.getId();
    }

    private void checkPlanItemHasTeaserPricing(OrderDTO order) {
        Set<PlanDTO> plans = null;
        for(OrderLineDTO line: order.getLines()) {
            if (null != line.getItem() && line.getItem().hasPlanItems()) {
                plans = line.getItem().getPlans();
                continue;
            }
            if (null != plans ) {
                for (PlanDTO plan: plans) {
                    for (PlanItemDTO planItem: plan.getPlanItems()) {
                        PriceModelDTO priceModelDto =  planItem.getPrice(new Date());
                        boolean isTeaserPricing = false;
                        if(null != priceModelDto){
                            isTeaserPricing = priceModelDto.getType().equals(PriceModelStrategy.TEASER_PRICING);
                        }
                        if (isTeaserPricing) {
                            line.setUseItem(Boolean.FALSE);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void setProvisioningStatus(OrderDTO orderDto) {
        // set the provisioning status
        for (OrderLineDTO line : orderDto.getLines()) {
            // set default provisioning status id for order lines
            if (line.getProvisioningStatus() == null) {
                line.setProvisioningStatus(provisioningStatusDas.find(
                        Constants.PROVISIONING_STATUS_INACTIVE));
            } else {
                line.setProvisioningStatus(provisioningStatusDas.find(
                        line.getProvisioningStatus().getId()));
            }
        }
    }

    public void updateActiveUntil(Integer executorId, Date to, OrderDTO newOrder) {
        audit(executorId, order.getActiveUntil());
        // this needs an event
        NewActiveUntilEvent event = new NewActiveUntilEvent(order.getId(), to, order.getActiveUntil(), newOrder.getActiveSince());
        EventManager.process(event);
        // update the period of the latest invoice as well. This is needed
        // because it is the way to extend a subscription when the
        // order status is finished. Then the next_invoice_date is null.
        if (order.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
            updateEndOfOrderProcess(to);
        }

        // update it
        order.setActiveUntil(to);

        Date now = TimezoneHelper.companyCurrentDate(order.getUser().getEntity().getId());
        if (to == null || to.before(now)) {
            // generate SubscriptionInactiveEvent for order
            SubscriptionInactiveEvent newEvent = new SubscriptionInactiveEvent(getEntity().getId(), order);
            EventManager.process(newEvent);
            logger.debug("OrderBL.updateActiveUntil(): generated SubscriptionInactiveEvent for order: {}", order.getId());
        }

        // if the new active until is earlier than the next invoice date, we have a
        // period already invoice being cancelled
        if (isDateInvoiced(to)) {
            // pass the new order, rather than the existing one. Otherwise, the exsiting gets
            // and changes overwritten by the data of the new order.
            EventManager.process(new PeriodCancelledEvent(newOrder,
                    order.getBaseUserByUserId().getCompany().getId(), executorId));
        }
    }

    /**
     * Method checkOrderLineQuantities.
     * Creates a NewQuantityEvent for each order line that has had
     * its quantity modified (including those added or deleted).
     * @return An array with all the events that should be fiered. This
     * prevents events being fired when the order has not be saved and it is
     * still 'mutating'.
     */
    public List<Event> checkOrderLineQuantities(List<OrderLineDTO> oldLines,
            List<OrderLineDTO> newLines, Integer entityId, Integer orderId, boolean sendEvents, boolean isPlanSwap) {

        List<Event> retValue = new ArrayList<>();
        // NewQuantityEvent is generated when an order line and it's quantity
        // has changed, including from >0 to 0 (deleted) and 0 to >0 (added).
        // First, copy and sort new and old order lines by order line id.
        List<OrderLineDTO> oldOrderLines = new ArrayList<>(oldLines);
        List<OrderLineDTO> newOrderLines = new ArrayList<>(newLines);
        Comparator<OrderLineDTO> sortByOrderLineId = (ol1, ol2) -> ol1.getId() - ol2.getId();
        Collections.sort(oldOrderLines, sortByOrderLineId);
        Collections.sort(newOrderLines, sortByOrderLineId);

        // remove any deleted lines
        for (Iterator<OrderLineDTO> it = oldOrderLines.iterator(); it.hasNext(); ) {
            if (it.next().getDeleted() != 0) {
                it.remove();
            }
        }
        for (Iterator<OrderLineDTO> it = newOrderLines.iterator(); it.hasNext(); ) {
            if (it.next().getDeleted() != 0) {
                it.remove();
            }
        }

        Iterator<OrderLineDTO> itOldLines = oldOrderLines.iterator();
        Iterator<OrderLineDTO> itNewLines = newOrderLines.iterator();

        // Step through the sorted order lines, checking if it exists only in
        // one, the other or both. If both, then check if quantity has changed.
        OrderLineDTO currentOldLine = itOldLines.hasNext() ? itOldLines.next() : null;
        OrderLineDTO currentNewLine = itNewLines.hasNext() ? itNewLines.next() : null;
        while (currentOldLine != null && currentNewLine != null) {
            int oldLineId = currentOldLine.getId();
            int newLineId = currentNewLine.getId();
            if (oldLineId < newLineId) {
                // order line has been deleted
                logger.debug("Deleted order line. Order line Id: {}", oldLineId);
                retValue.add(new NewQuantityEvent(entityId, currentOldLine.getQuantity(), BigDecimal.ZERO,
                        orderId, currentOldLine, null, isPlanSwap));
                currentOldLine = itOldLines.hasNext() ? itOldLines.next() : null;
            } else if (oldLineId > newLineId) {
                // order line has been added
                logger.debug("Added order line. Order line Id: {}", newLineId);
                retValue.add(new NewQuantityEvent(entityId, BigDecimal.ZERO, currentNewLine.getQuantity(),
                        orderId, null, currentNewLine, isPlanSwap));
                currentNewLine = itNewLines.hasNext() ? itNewLines.next() : null;
            } else {
                // order line exists in both, so check quantity
                BigDecimal oldLineQuantity = currentOldLine.getQuantity();
                BigDecimal newLineQuantity = currentNewLine.getQuantity();
                if (oldLineQuantity.compareTo(newLineQuantity) != 0) {
                    logger.debug("Order line quantity changed. Order line Id: {}", oldLineId);
                    retValue.add(new NewQuantityEvent(entityId, oldLineQuantity, newLineQuantity, orderId,
                            currentOldLine, currentNewLine, isPlanSwap));
                }
                currentOldLine = itOldLines.hasNext() ? itOldLines.next() : null;
                currentNewLine = itNewLines.hasNext() ? itNewLines.next() : null;
            }
        }
        // check for any remaining item lines that must have been deleted or added
        while (currentOldLine != null) {
            logger.debug("Deleted order line. Order line id: {}", currentOldLine.getId());
            retValue.add(new NewQuantityEvent(entityId, currentOldLine.getQuantity(), BigDecimal.ZERO, orderId,
                    currentOldLine, null, isPlanSwap));
            currentOldLine = itOldLines.hasNext() ? itOldLines.next() : null;
        }
        while (currentNewLine != null) {
            logger.debug("Added order line. Order line id: {}", currentNewLine.getId());
            retValue.add(new NewQuantityEvent(entityId, BigDecimal.ZERO, currentNewLine.getQuantity(), orderId,
                    null, currentNewLine, isPlanSwap));
            currentNewLine = itNewLines.hasNext() ? itNewLines.next() : null;
        }

        if(!retValue.isEmpty()) {
            retValue.add(new OrderUpdatedEvent(entityId, orderId, oldLines, newLines));
        }

        if (sendEvents) {
            for (Event event : retValue) {
                EventManager.process(event);
            }
        }

        return retValue;
    }

    /**
     * Method checkOrderLinePrices.
     * Creates a NewPriceEvent for each order line that has had
     * its price modified ..
     * @return An array with all the events that should be fired.
     */
    public List<NewPriceEvent> checkOrderLinePrices(List<OrderLineDTO> oldLines,
            List<OrderLineDTO> newLines, Integer entityId, Integer orderId, boolean sendEvents) {

        List<NewPriceEvent> retValue = new ArrayList<>();
        for (int newOrderIndex = 0; newOrderIndex < newLines.size(); newOrderIndex++) {
            OrderLineDTO currentNewLine = newLines.get(newOrderIndex);

            // find the corresponding line in oldLines
            for (int oldOrderIndex = 0; oldOrderIndex < oldLines.size(); oldOrderIndex++) {
                OrderLineDTO currentOldLine = oldLines.get(oldOrderIndex);

                if (currentNewLine.getId() == currentOldLine.getId()) {
                    // quantity must be same, only the price should be different
                    if (currentNewLine.getPrice().compareTo(
                            currentOldLine.getPrice()) != 0
                            && (currentNewLine.getQuantity().compareTo(
                                    currentOldLine.getQuantity()) == 0)) {
                        retValue.add(new NewPriceEvent(
                                entityId, currentOldLine.getPrice(), currentNewLine.getPrice(),
                                currentOldLine.getAmount(), currentNewLine.getAmount(), orderId,
                                currentOldLine.getId()));
                        logger.debug("Order line price changed. Order line Id: {}", currentOldLine.getId());
                    }
                }
            }
        }

        if (sendEvents) {
            for (NewPriceEvent event: retValue) {
                EventManager.process(event);
            }
        }
        return retValue;
    }

    /**
     * This method will update persisted orders hierarchy from fields of input orders and from orderChanges collection.
     * Input orderChanges will be updated/created too.
     * Final orders (result of orderChanges application) will be validated for correct assets and hierarchy     *
     * @param updatedRootOrder Input orders hierarchy with fields updated
     * @param orderChanges Order changes to be applied
     * @param deletedChanges Deleted order changes
     * @param entityId Entity Id
     * @param executorId Executor Id
     * @param languageId Language Id
     */
    public void update(OrderDTO updatedRootOrder,
            Collection<OrderChangeDTO> orderChanges,
            Collection<Integer> deletedChanges,
            Integer entityId, Integer executorId,
            Integer languageId) {
        OrderDAS orderDAS = new OrderDAS();
        OrderDTO targetOrder = orderDAS.find(updatedRootOrder.getId());
        // evict orders hierarchy to update as transient entities
        orderDAS.detachOrdersHierarchy(targetOrder);
        Date onDate = com.sapienter.jbilling.common.Util.truncateDate(TimezoneHelper.companyCurrentDate(entityId));
        // update and validate
        targetOrder = OrderBL.updateOrdersFromDto(targetOrder, updatedRootOrder);
        Map<OrderLineDTO, OrderChangeDTO> appliedChanges =
                OrderChangeBL.applyChangesToOrderHierarchy(targetOrder, orderChanges, onDate, true, entityId);

        // validate final hierarchy
        OrderHierarchyValidator validator = new OrderHierarchyValidator();
        validator.buildHierarchy(targetOrder);
        String error = validator.validate();
        if (error != null) {
            throw new SessionInternalError("Incorrect orders hierarchy: " + error, new String[]{error});
        }

        // linked set to preserve hierarchy order in collection, from root to child
        LinkedHashSet<OrderDTO> ordersForUpdate = OrderHelper.findOrdersInHierarchyFromRootToChild(targetOrder);
        List<Integer> bundledOrderIds = new ArrayList<>();
        // update from root order to child orders
        for (OrderDTO updatedOrder : ordersForUpdate) {

            if (bundledOrderIds.contains(updatedOrder.getId())) {
                continue;
            }

            // start by locking the order
            OrderBL oldOrder = new OrderBL();
            oldOrder.setForUpdate(updatedOrder.getId());
            OrderBL orderBL = new OrderBL();
            // see if the related items should provide info
            List<PricingField> pricingFields = updatedOrder.getPricingFields();
            orderBL.processLines(updatedOrder, languageId, entityId, updatedOrder.getBaseUserByUserId().getId(),
                    updatedOrder.getCurrency().getId(),
                    updatedOrder.getPricingFields() != null ? PricingField.setPricingFieldsValue(pricingFields.toArray(new PricingField[pricingFields.size()])) : null);

            // recalculate
            orderBL.set(updatedOrder);
            orderBL.recalculate(entityId);

            // update
            bundledOrderIds.addAll(oldOrder.update(executorId, updatedOrder, appliedChanges));
        }
        OrderChangeBL orderChangeBL = new OrderChangeBL();

        Integer swapPlanItemId = null;
        Integer existingPlanItemId = null;
        Date effectiveDate = null;
        for (OrderChangeDTO orderChangeDTO : orderChanges) {
            if (null == swapPlanItemId && null != orderChangeDTO.getItem() && orderChangeDTO.getItem().isPlan() && orderChangeDTO.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                swapPlanItemId = orderChangeDTO.getItem().getId();
                effectiveDate = orderChangeDTO.getStartDate();
            } else if (null == existingPlanItemId && null != orderChangeDTO.getItem() && orderChangeDTO.getItem().isPlan() && orderChangeDTO.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                existingPlanItemId = orderChangeDTO.getItem().getId();
            }

            if (null != swapPlanItemId && null != existingPlanItemId) {
                break;
            }
        }

        //synchronize order changes with database state
        orderChangeBL.updateOrderChanges(entityId, orderChanges, deletedChanges, onDate);
        logger.debug(" ExistingPlanItemId:{} swapPlanItemId: {}", existingPlanItemId, swapPlanItemId);
        if (existingPlanItemId != null && swapPlanItemId != null && existingPlanItemId.intValue() != swapPlanItemId.intValue()) {
            //Fire swap plan free usage pool transfer event.
            processSwapPlanFUPTrasfer(entityId, targetOrder, existingPlanItemId, swapPlanItemId, effectiveDate);
        }
    }

    public List<Integer> update(Integer executorId, OrderDTO dto, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {

        logger.debug("OrderBL.update");
        logger.debug("OrderDTO {}", dto);
        for (OrderLineDTO orderLineDTO : appliedChanges.keySet()) {
            logger.debug("OrderLineDTO {}", orderLineDTO);
            logger.debug("OrderChangeDTO {}", appliedChanges.get(orderLineDTO));
        }

        List<CustomerPlanUnsubscriptionEvent> customerPlanUnsubscriptionEvents = new ArrayList<>();

        // update first the order own fields
        if (!Util.equal(order.getActiveUntil(), dto.getActiveUntil())) {

            //8922 - update customer prices subscription date possibly
            // activeSince has changed. If order contains plan(s), update their priceSubscriptionDate for the customer
            for (OrderLineDTO line : dto.getLines()) {
                if (null != line.getItem() && line.getItem().hasPlans()) {

                    logger.debug("Active Until updated. Will expire prices for Plans subscriptions");
                    ItemDTO plan = new ItemDAS().findNow(line.getItemId());
                    if (null != plan && plan.getPlans().iterator().hasNext()) {
                        PlanDTO thePlan = plan.getPlans().iterator().next();
                        new CustomerPriceBL(dto.getUserId()).expirePrices(thePlan.getId(), dto.getActiveUntil());
                    }
                }
            }

            updateActiveUntil(executorId, dto.getActiveUntil(), dto);

            // active until has changed. If order contains plan(s), raise CustomerPlanUnsubscriptionEvents
            for (OrderLineDTO line : dto.getLines()) {
                if (null != line.getItem() && line.getItem().hasPlans()
                        && dto.getProrateFlag().equals(Boolean.FALSE)) {
                    customerPlanUnsubscriptionEvents.add(
                            new CustomerPlanUnsubscriptionEvent(
                                    order.getBaseUserByUserId().getEntity().getId(),
                                    order, Constants.CUSTOMER_PLAN_UNSUBSCRIBE_UPDATE_ACTIVE_UNTIL, false)
                            );

                }
            }
        }

        if (!Util.equal(order.getActiveSince(), dto.getActiveSince())) {

            //8922 - update customer prices subscription date possibly
            // activeSince has changed. If order contains plan(s), update their priceSubscriptionDate for the customer
            for (OrderLineDTO line : dto.getLines()) {
                if (null != line.getItem() && line.getItem().hasPlans()) {

                    logger.debug("Active Since updated. Will refresh priceSubscriptionDate for customer's Plans subscriptions");
                    ItemDTO plan = new ItemDAS().findNow(line.getItemId());
                    if (null != plan && plan.getPlans().iterator().hasNext()) {
                        PlanDTO thePlan = plan.getPlans().iterator().next();
                        new CustomerPriceBL(dto.getUserId()).updatePriceSubscriptionDate(thePlan.getId(), dto.getActiveSince());
                    }
                }
            }

            NewActiveSinceEvent event = new NewActiveSinceEvent(order.getId(), dto, order);
            EventManager.process(event);

            audit(executorId, order.getActiveSince());
            order.setActiveSince(dto.getActiveSince());
        }

        // this one needs more to get updated
        updateNextBillableDay(executorId, dto.getNextBillableDay());

        if (order.getOrderPeriod().getId() != dto.getOrderPeriod().getId()) {
            audit(executorId, order.getOrderPeriod().getId());
            order.setOrderPeriod(orderPeriodDAS.find(dto.getOrderPeriod().getId()));
        }

        if (!order.getProrateFlag().equals(dto.getProrateFlag())) {

            UpdateProrateFlagEvent event = new UpdateProrateFlagEvent(dto);
            EventManager.process(event);

            order.setProrateFlag(null != dto.getProrateFlag() ? dto.getProrateFlag() : false);
        }
        Integer entityId = order.getBaseUserByUserId().getCompany().getId();
        Map<String, Object> oldMetaFieldNameAndValueMap = MetaFieldHelper.convertMetaFieldValuesToMap(entityId, null, EntityType.ORDER, order);
        updateOrderOwnFields(order, dto, false);
        // update orderParent link if needed
        order.setParentOrder(dto.getParentOrder() != null ? orderDas.find(dto.getParentOrder().getId()) : null);

        // this should not be necessary any more, since the order is a pojo...
        order.setOrderBillingType(dto.getOrderBillingType());
        order.setNotify(dto.getNotify());
        order.setDueDateUnitId(dto.getDueDateUnitId());
        order.setDueDateValue(dto.getDueDateValue());
        order.setDfFm(dto.getDfFm());
        order.setAnticipatePeriods(dto.getAnticipatePeriods());
        order.setOwnInvoice(dto.getOwnInvoice());
        order.setNotes(dto.getNotes());
        order.setNotesInInvoice(dto.getNotesInInvoice());
        order.setProrateFlag(null != dto.getProrateFlag() ? dto.getProrateFlag() : false);
        // this one needs more to get updated
        updateNextBillableDay(executorId, dto.getNextBillableDay());

        //now process the order lines
        logger.info("Order lines: {}  --> new Order: {}", order.getLines().size(), dto.getLines().size());
        // copy old line, because they can be changed during update
        List<OrderLineDTO> oldLines = OrderHelper.copyOrderLinesToDto(order.getLines());
        //asset create and update events
        List<AbstractAssetEvent> assetEvents = new ArrayList<>();

        Collection<OrderChangeDTO> appliedOrderChanges = appliedChanges.values();
        boolean isPlanSwap = isPlanSwap(appliedOrderChanges);

        List<CustomerPlanSubscriptionEvent> subscriptionEvents = new ArrayList<>();
        if(!isPlanSwap) {
            subscriptionEvents.addAll(generateCustomerPlanSubscriptionEvents(dto.getLines(), order.getBaseUserByUserId().getId(),
                    null, null, false, false));
        }

        List<Integer> bundledOrderIds = updateOrderLinesInOrder(order, oldLines, dto.getLines(), executorId, assetEvents, appliedChanges);
        // creating customer usage pool and re rating usage order.
        for(CustomerPlanSubscriptionEvent subscriptionEvent : subscriptionEvents) {
            EventManager.process(subscriptionEvent);
        }

        List<OrderLineDTO> newLines = new LinkedList<>(order.getLines());

        // get new quantity events as necessary
        List<Event> events = checkOrderLineQuantities(oldLines, newLines,
                order.getBaseUserByUserId().getCompany().getId(),
                order.getId(), false, isPlanSwap); // do not send them now, it will be done later when the order is saved
        // get new price events as necessary
        List<NewPriceEvent> priceEvents = checkOrderLinePrices(oldLines, newLines,
                order.getBaseUserByUserId().getCompany().getId(),
                order.getId(), false);

        // set the free usage quantity used by the order
        order.setFreeUsageQuantity(dto.getFreeUsagePoolsTotalQuantity());

        order = orderDas.save(order);
        fireSwapAssetEvent(order.getId(), order.getBaseUserByUserId().getCompany().getId(), oldLines, dto.getLines());
        fireUpdateMetaFieldEvent(order.getId(), entityId, oldMetaFieldNameAndValueMap,
                MetaFieldHelper.convertMetaFieldValuesToMap(entityId, null, EntityType.ORDER, order));
        // update order status
        setStatus(executorId, dto.getOrderStatus().getId());

        // un subscribe plan when plan item removed from order
        appliedOrderChanges.forEach(change -> {
            OrderLineDTO changeOfLine = change.getOrderLine();
            if(order.hasLinePresent(changeOfLine.getId()) && changeOfLine.getDeleted() == 1 &&
                    changeOfLine.getItem().hasPlans()) {
                customerPlanUnsubscriptionEvents.add(new CustomerPlanUnsubscriptionEvent(order.getUser().getCompany().getId(), order, Constants.CUSTOMER_PLAN_UNSUBSCRIBE_PLAN_REMOVE, isPlanSwap));
                return ;
            }
        });

        newLines = order.getLines();

        // propagate saved lines ids to dtos for hierarchy building
        for (OrderLineDTO line : newLines) {
            if (OrderHelper.findOrderLineWithId(dto.getLines(), line.getId()) != null) {
                continue;
            }
            OrderLineDTO dtoLine = null;
            // find appropriate dto line
            for (OrderLineDTO tmpLine : dto.getLines()) {
                if (tmpLine.getId() == 0 && tmpLine.getItemId().equals(line.getItemId()) && line.getCreateDatetime().equals(tmpLine.getCreateDatetime())) {
                    dtoLine = tmpLine;
                    break;
                }
            }
            if (dtoLine != null && dtoLine.getId() == 0) {
                dtoLine.setId(line.getId());
            }
        }

        auditOrderLinesChange(oldLines, newLines, executorId);

        if (executorId != null) {
            eLogger.audit(executorId, order.getBaseUserByUserId().getId(),
                    Constants.TABLE_PUCHASE_ORDER, order.getId(),
                    EventLogger.MODULE_ORDER_MAINTENANCE,
                    EventLogger.ROW_UPDATED, null,
                    null, null);
        } else {
            eLogger.auditBySystem(order.getBaseUserByUserId().getCompany().getId(),
                    order.getBaseUserByUserId().getId(),
                    Constants.TABLE_PUCHASE_ORDER,
                    order.getId(),
                    EventLogger.MODULE_ORDER_MAINTENANCE,
                    EventLogger.ROW_UPDATED, null,
                    null, null);
        }

        // last, once the order is saved and all done, send out the
        // order modified events
        for (Event event : events) {
            EventManager.process(event);
        }

        for (NewPriceEvent priceEvent : priceEvents) {
            EventManager.process(priceEvent);
        }

        //fire the asset events
        for (AbstractAssetEvent event : assetEvents) {
            EventManager.process(event);
        }

        // Fire the customer plan unsubscription events. Presently there can be only one plan per order,
        // but in future we can have many plans on one order. Fire event for each plan.
        logger.debug("Before firing CustomerPlanUnsubscriptionEvent");
        for (CustomerPlanUnsubscriptionEvent event: customerPlanUnsubscriptionEvents) {
            logger.debug("Firing CustomerPlanUnsubscriptionEvent");
            EventManager.process(event);
        }

        EventManager.process(new EditOrderEvent(order.getBaseUserByUserId().getCompany().getId(), order));
        return bundledOrderIds;
    }

    private boolean isPlanSwap(Collection<OrderChangeDTO> changes) {
        return changes.stream()
                .filter(change -> change.getItem().hasPlans())
                .count() > 1;
    }

    /**
     * fires {@link SwapAssetsEvent} for given order
     * @param orderId
     * @param entityId
     * @param oldLines
     * @param newLines
     */
    private void fireSwapAssetEvent(Integer orderId, Integer entityId, List<OrderLineDTO> oldLines, List<OrderLineDTO> newLines) {
        Map<Integer, List<Integer>> oldOrderLineAssetMap = createOrderLineAssetMap(oldLines);
        Map<Integer, List<Integer>> newOrderLineAssetMap = createOrderLineAssetMap(newLines);
        Map<Integer, Integer> assetDiffMap = new HashMap<>();
        for(Entry<Integer, List<Integer>> assetEntry : oldOrderLineAssetMap.entrySet()) {
            List<Integer> oldAssets = assetEntry.getValue();
            List<Integer> newAssets = newOrderLineAssetMap.get(assetEntry.getKey());
            if(oldAssets.size()!= newAssets.size()) {
                continue;
            }
            for(int i = 0; i <= oldAssets.size()-1; i++) {
                Integer oldId = oldAssets.get(i);
                Integer newId = newAssets.get(i);
                if(!oldId.equals(newId)) {
                    assetDiffMap.put(oldId, newId);
                }
            }
        }
        if(!assetDiffMap.isEmpty()) {
            logger.debug("executing swap asset event for order {}", orderId);
            EventManager.process(new SwapAssetsEvent(orderId, assetDiffMap, entityId));
        }
    }

    private Map<Integer, List<Integer>> createOrderLineAssetMap(List<OrderLineDTO> lines) {
        Map<Integer, List<Integer>> orderLineAssetMap = new HashMap<>();
        for(OrderLineDTO line : lines) {
            orderLineAssetMap.put(line.getId(), line.getAssetIds());
        }
        return orderLineAssetMap;
    }

    private static void fireUpdateMetaFieldEvent(Integer orderId, Integer entityId, Map<String, Object> oldMetaFields, Map<String, Object> newMetaFields) {
        Map<String, ValueDifference<Object>> diffMap = Maps.difference(oldMetaFields, newMetaFields).entriesDiffering();
        logger.debug("calculated meta field value diff {} for order id {}", diffMap, orderId);
        if(!diffMap.isEmpty()) {
            EventManager.process(new OrderMetaFieldUpdateEvent(orderId, entityId, diffMap));
        }
    }

    /**
     * Log changes for order lines in DB
     * @param oldLines Old order lines
     * @param newLines Updated orde lines
     * @param executorId Executor Id
     */
    private void auditOrderLinesChange(List<OrderLineDTO> oldLines, List<OrderLineDTO> newLines, Integer executorId) {
        // log order line change event if a single order line was changed
        OrderLineDTO oldSingleLine = null;
        int nonDeletedLines = 0;
        if (newLines.size() == 1 && oldLines.size() >= 1) {
            // This event needs to logger the old item id and description, so
            // it can only happen when updating orders with only one line.
            for (OrderLineDTO temp : oldLines) {
                // Check which orderLine is not deleted.
                if (temp.getDeleted() == 0) {
                    oldSingleLine = temp;
                    nonDeletedLines++;
                }
            }
        }
        if (oldSingleLine != null && nonDeletedLines == 1) {
            OrderLineDTO newLine = null;
            for (OrderLineDTO temp : newLines) {
                if (temp.getDeleted() == 0) {
                    newLine = temp;
                }
            }
            if (newLine != null && newLine.getItem() != null && oldSingleLine.getItem() != null
                    && !oldSingleLine.getItemId().equals(newLine.getItemId())) {
                if (executorId != null) {
                    eLogger.audit(executorId,
                            order.getBaseUserByUserId().getId(),
                            Constants.TABLE_ORDER_LINE,
                            newLine.getId(), EventLogger.MODULE_ORDER_MAINTENANCE,
                            EventLogger.ORDER_LINE_UPDATED, oldSingleLine.getId(),
                            oldSingleLine.getDescription(),
                            null);
                } else {
                    // it is the mediation process
                    eLogger.auditBySystem(order.getBaseUserByUserId().getCompany().getId(),
                            order.getBaseUserByUserId().getId(),
                            Constants.TABLE_ORDER_LINE,
                            newLine.getId(), EventLogger.MODULE_ORDER_MAINTENANCE,
                            EventLogger.ORDER_LINE_UPDATED, oldSingleLine.getId(),
                            oldSingleLine.getDescription(),
                            null);
                }
            }
        }
    }

    /**
     *
     * @param order
     * @param oldLines
     * @param updatedLineDtos
     * @param executorId
     * @param assetEvents
     * @param appliedChanges
     * @return
     */
    private List<Integer> updateOrderLinesInOrder(
            OrderDTO order, List<OrderLineDTO> oldLines, List<OrderLineDTO> updatedLineDtos, Integer executorId,
            List<AbstractAssetEvent> assetEvents, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {

        // set the provisioning status
        for (OrderLineDTO line : updatedLineDtos) {
            // set default provisioning status id for order lines
            if (line.getProvisioningStatus() == null) {
                line.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_INACTIVE));
            } else {
                line.setProvisioningStatus(provisioningStatusDas.find(line.getProvisioningStatus().getId()));
            }
        }

        List<AbstractAssetEvent> unlinkedAssetEvents = new ArrayList<>();

        // add new customer plan subscriptions
        addOrRemoveCustomerPlans(updatedLineDtos, order.getUserId(), appliedChanges);
        List<Integer> bundledOrderIds = addBundledItems(order, updatedLineDtos, order.getBaseUserByUserId(), appliedChanges);

        if (order.getOrderPeriod().getId() != Constants.ORDER_PERIOD_ONCE) {
            // propagate line to newly created assets from BasicItemManager
            for (OrderLineDTO line : order.getLines()) {
                if (line.getAssets() != null) {
                    for (AssetDTO asset : line.getAssets()) {
                        if (asset.getOrderLine() == null) {
                            asset.setOrderLine(line);
                        } else if (!asset.getOrderLine().equals(line)) {
                            asset.setPrevOrderLine(asset.getOrderLine());
                            asset.setOrderLine(line);
                        }
                    }
                }
            }
        }

        //map asset id to dto
        Map<Integer, AssetDTO> unlinkedAssets = new HashMap<>();

        // now update this order's lines
        for (OrderLineDTO line : order.getLines()) {
            OrderLineDTO updatedLine = OrderHelper.findOrderLineWithId(updatedLineDtos, line.getId());
            if (updatedLine != null) {
                if(PreferenceBL.isTierCreationAllowed(order.getUser().getCompany().getId()) &&
                        (line.hasOrderLineTiers() || line.getItem().isTieredPricingProduct())){
                    if(CollectionUtils.isNotEmpty(line.getOrderLineTiers())) {
                        line.getOrderLineTiers().clear();
                    }
                    line.getOrderLineTiers().addAll(updatedLine.getOrderLineTiers());
                }
                updateOrderLineOwnFields(line, updatedLine);
                // update fields from change
                line.setDeleted(updatedLine.getDeleted());
                line.setPrice(updatedLine.getPrice());
                line.setQuantity(updatedLine.getQuantity());
                line.getAssets().clear();
                for (AssetDTO updatedAsset : updatedLine.getAssets()) {
                    AssetDTO asset = assetDAS.find(updatedAsset.getId());
                    asset.setOrderLine(line);
                    asset.setAssetStatus(updatedAsset.getAssetStatus());
                    asset.setPrevOrderLine(updatedAsset.getPrevOrderLine());
                    line.addAsset(asset);
                }
                // todo: is needed? used in api now
                line.setItem(updatedLine.getItem());
                // set parent line if exists
                line.setParentLine(updatedLine.getParentLine() != null && updatedLine.getParentLine().getId() > 0 ? orderLineDAS.find(updatedLine.getParentLine().getId()) : updatedLine.getParentLine());
                // reset children info in lines - saving only via parent
                line.getChildLines().clear();
                //unlink assets not linked to line anymore
                if (line.getItem() != null && line.getItem().getAssetManagementEnabled() == 1) {
                    Set<AssetDTO> assets = unlinkAssets(oldLines, line, executorId, unlinkedAssetEvents, appliedChanges);
                    for (AssetDTO asset : assets) {
                        unlinkedAssets.put(asset.getId(), asset);
                    }
                }

                line.getOrderLineUsagePools().clear();
                line.getOrderLineUsagePools().addAll(updatedLine.getOrderLineUsagePools());
            }
        }

        //fire the asset events from unlinking
        //this is required here because the asset might change after this point, making
        //the processing of the event difficult to handle
        for (AbstractAssetEvent event : unlinkedAssetEvents) {
            EventManager.process(event);
        }

        // add new lines
        List<OrderLineDTO> newLines = new LinkedList<>();
        for (OrderLineDTO line : updatedLineDtos) {
            if (line.getId() <= 0) {
                // link them all, just in case there's a new one
                line.setPurchaseOrder(order);
                // new lines need createDatetime set
                line.setDefaults();
                // set parent line if exists
                line.setParentLine(line.getParentLine() != null && line.getParentLine().getId() > 0 ? orderLineDAS.find(line.getParentLine().getId()) : line.getParentLine());
                // reset children info in lines - saving only via parent
                line.getChildLines().clear();
                Set<AssetDTO> newAssets = new HashSet<>(line.getAssets());
                line.getAssets().clear();
                for (AssetDTO updatedAsset : newAssets) {
                    AssetDTO asset = unlinkedAssets.get(updatedAsset.getId());
                    if (asset == null) {
                        asset = assetDAS.find(updatedAsset.getId());
                        asset.setAssetStatus(updatedAsset.getAssetStatus());
                        asset.setPrevOrderLine(updatedAsset.getPrevOrderLine());
                    }
                    asset.setOrderLine(line);
                    line.addAsset(asset);
                }
                newLines.add(line);
            }
        }

        setPrimaryOrderForLinkedOrders(bundledOrderIds);

        order.getLines().addAll(newLines);

        //we have to validate the assets in a new loop. It is impossible to unlink assets
        //and relink those same assets to different lines in the same loop
        for (OrderLineDTO line : order.getLines()) {
            //validate assets
            validateAssets(line, executorId, unlinkedAssets, assetEvents, appliedChanges);
        }

        return bundledOrderIds;
    }

    /**
     * Set the line asset status to the default order saved status
     * Checks
     *  - item allows asset mangement
     *  - line quantity match the number of assets
     *  - asset is not linked to another order line
     *  - if asset is linked for the first time check that the old status is available
     * @param line order line to validate
     * @param unlinkedAssets - assets which have been removed from another line in this tx and are eligible for adding
     * @param eventsToFire assetChangeEvents to fire later
     */
    private void validateAssets(
            OrderLineDTO line, Integer executorId, Map<Integer, AssetDTO> unlinkedAssets,
            List<AbstractAssetEvent> eventsToFire, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {

        if (null != line.getPurchaseOrder().getResellerOrder()) {
            logger.debug("We do not validate Assets for the orders in the parent entity for the Reseller Customer.");
            return;
        }
        String errorCode = OrderAssetsValidator.validateAssets(line, unlinkedAssets);
        if (errorCode != null) {
            throw new SessionInternalError("Error during assets validation: " + errorCode, new String[]{errorCode});
        }
        if (line.getItem() == null || line.getItem().getAssetManagementEnabled() == 0) {
            return;
        }
        ItemDAS itemDAS = new ItemDAS();

        ItemDTO itemDto = itemDAS.find(line.getItem().getId());
        AssetStatusDTO orderSavedStatus = itemDto.findItemTypeWithAssetManagement() != null ?
                itemDto.findItemTypeWithAssetManagement().findOrderSavedStatus() : null;

                //map assets from the new order instance (line) to assets in unlinkedAssets
                //if an asset has been unlinked from one line and linked to another we have to use the same
                //object instance that was removed in this line
                Map<AssetDTO, AssetDTO> assetsToReplace = new HashMap<>(2);

                for(AssetDTO asset : line.getAssets()) {
                    //check if this asset was removed from another line
                    if(unlinkedAssets.containsKey(asset.getId())) {
                        AssetDTO assetDTO = unlinkedAssets.get(asset.getId());
                        assetDTO.setOrderLine(line);
                        assetsToReplace.put(asset, assetDTO);
                        asset = assetDTO;
                    }

                    if(asset.getPrevOrderLine() == null) {
                        //this is a new asset to link
                        UserDTO user = line.getPurchaseOrder().getUser();

                        AssetStatusDTO previousStatus = asset.getAssetStatus();
                        addAssetToOrderLine(executorId, line, user, orderSavedStatus, asset, false, appliedChanges);

                        eventsToFire.add(new AssetAddedToOrderEvent(asset.getEntity() != null? asset.getEntity().getId() :null,
                                asset, line.getPurchaseOrder().getBaseUserByUserId(), executorId));
                        eventsToFire.add(new AssetUpdatedEvent(asset.getEntity() != null? asset.getEntity().getId() :null,
                                asset, previousStatus, line.getPurchaseOrder().getBaseUserByUserId(), executorId));
                    }
                }

                //replace the asset in this line with the unlinked asset from another line
                for(AssetDTO assetDTO : assetsToReplace.keySet()) {
                    line.removeAsset(assetDTO);
                    line.addAsset(assetsToReplace.get(assetDTO));
                }
    }

    /**
     * Unlink all assets from a deleted line.
     * Unlink assets not linked to the line anymore.
     *
     * @param oldLines - lines from the order before modification. Can be null.
     * @param line - new line with changed assets
     * @param executorId
     * @param eventsToFire
     */
    private Set<AssetDTO> unlinkAssets(
            Collection<OrderLineDTO> oldLines, OrderLineDTO line, Integer executorId,
            List<AbstractAssetEvent> eventsToFire, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {
        Set<AssetDTO> assetsToRemove = null;

        //unlink assets from deleted lines
        if (line.getId() > 0 && null != line.getItem() && line.getItem().getAssetManagementEnabled() == 1) {
            ItemTypeDTO itemType = line.getItem().findItemTypeWithAssetManagement();
            AssetStatusDTO defaultStatus = itemType.findDefaultAssetStatus();

            //get all assets from the old line not on the new line
            if (oldLines != null) {
                OrderLineDTO oldLine = OrderHelper.findOrderLineWithId(oldLines, line.getId());
                //get all assets from the old order
                assetsToRemove = new CopyOnWriteArraySet<>(oldLine.getAssets());

                //remove assets from the old order which is not in the new order
                for (AssetDTO assetDTO : assetsToRemove) {
                    if (line.containsAsset(assetDTO.getId())) {
                        assetsToRemove.remove(assetDTO);
                    }
                }
            } else {
                assetsToRemove = new HashSet<>() ;
            }

            //if line is deleted, unlink all assets on the new line
            if (line.getDeleted() == 1) {
                assetsToRemove.addAll(line.getAssets());
            }

            //do the actual removal
            for (AssetDTO assetDTO : assetsToRemove) {
                removeAssetFromOrderLine(line, executorId, defaultStatus, assetDTO, eventsToFire, appliedChanges);
            }
        }

        if (assetsToRemove == null) {
            return new HashSet<>(0);
        } else {
            return assetsToRemove;
        }
    }

    /**
     * Unlink the asset from the orderline.
     *
     * @param executorId
     * @param defaultStatus - asset will get this status
     * @param assetDTO
     * @param eventsToFire - events generated will be added to the list
     */
    public void removeAssetFromOrderLine(
            OrderLineDTO line, Integer executorId, AssetStatusDTO defaultStatus,
            AssetDTO assetDTO, List<AbstractAssetEvent> eventsToFire,
            Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {

        AssetStatusDTO previousStatus = assetDTO.getAssetStatus();
        // unlink asset only if it linked to current line
        // see addBundledItems() for example of assets reassign
        if (assetDTO.getOrderLine().equals(line)) {

            OrderChangeDTO appliedChange = findOrderChange(line, appliedChanges);
            Date assetAssignedEndDate = null != appliedChange ? appliedChange.getStartDate() : TimezoneHelper.serverCurrentDate();

            assetDTO.setAssetStatus(defaultStatus);
            assetDTO.setOrderLine(null);
            assetDTO.setPrevOrderLine(null);

            AssetAssignmentDTO assignment = findOpenAssignmentTransition(assetDTO);
            if(null != assignment){assignment.setEndDatetime(assetAssignedEndDate);}

            assetDAS.save(assetDTO);
        }
        //fire unlink event anywhere
        eventsToFire.add(new AssetUpdatedEvent(assetDTO.getEntity() != null ? assetDTO.getEntity().getId() : null,
                assetDTO, previousStatus, null, executorId));
    }

    private AssetAssignmentDTO findOpenAssignmentTransition(AssetDTO asset) {
        for (AssetAssignmentDTO assignment : asset.getAssignments()) {
            if (null == assignment.getEndDatetime()) {
                return assignment;
            }
        }
        return null;
    }

    private OrderChangeDTO findOrderChange(OrderLineDTO line, Map<OrderLineDTO, OrderChangeDTO> changes) {
        /** both arguments can be null when for example an order is deleted **/
        if (null == line || null == changes) {
            return null;
        }

        /** first try to locate the change by object reference **/
        OrderChangeDTO change = changes.get(line);
        if (null != change) {
            return change;
        }

        /** try to find the change by order line id. This is typical for update scenarios **/
        for (OrderLineDTO currentLine : changes.keySet()) {
            if (currentLine.getId() == line.getId()) {
                line = currentLine;
                break;
            }
        }

        return changes.get(line);
    }

    /**
     * Subscribes the given user to any plans held by the given list of order lines,
     * if the user does not already have a subscription.
     *
     * should be called before an order is modified in the persistence context.
     *
     * @param lines lines to process
     * @param userId user id of customer to subscribe
     */
    private void addCustomerPlans(List<OrderLineDTO> lines, Integer userId, Date activeSince, Date activeUntil) {
        logger.debug("Processing {} order line(s), adding plans to user {}", lines.size(), userId);
        for (OrderLineDTO line : lines) {
            // subscribe customer to plan if they haven't already been subscribed.
            if (line.getItem() != null && !line.getItem().getPlans().isEmpty() && line.getDeleted() == 0) {

                Date startDate = ( null != activeSince ? activeSince : (null != line.getStartDate() ? line.getStartDate() : TimezoneHelper.companyCurrentDateByUserId(userId)));
                Date endDate = (null != activeUntil ? activeUntil : line.getEndDate());

                if (!PlanBL.isSubscribed(userId, line.getItemId(), startDate)) {
                    logger.debug("Subscribing user {} to plan item {}", userId, line.getItemId());
                    PlanBL.subscribe(userId, line.getItemId(), activeSince, endDate);
                }
            }
        }
    }

    /**
     * Subscribes the given user to any plans held by the given list of order lines,
     * if the user does not already have a subscription.
     *
     * should be called before an order is modified in the persistence context.
     *
     * @param lines lines to process
     * @param userId user id of customer to subscribe
     */
    private void addOrRemoveCustomerPlans(List<OrderLineDTO> lines, Integer userId, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {
        logger.debug("Processing {} order line(s), adding plans to user {}", lines.size(), userId);
        CustomerPriceBL customerPriceBl = new CustomerPriceBL(userId);
        for (OrderLineDTO line : lines) {
            // subscribe customer to plan if they haven't already been subscribed.
            if (line.getItem() != null && !line.getItem().getPlans().isEmpty()) {

                OrderChangeDTO change = appliedChanges.get(line);
                Date effectiveDate = ((null != change && null != change.getStartDate()) ? change.getStartDate() : line.getPurchaseOrder().getActiveSince());
                Date endDate = ((null != change && null != change.getEndDate()) ? change.getEndDate() : null);

                if (line.getDeleted() == 0) {
                    if (!PlanBL.isSubscribed(userId, line.getItemId(), effectiveDate)) {
                        logger.debug("Subscribing user {} to plan item {}", userId, line.getItemId());
                        PlanBL.subscribe(userId, line.getItemId(), effectiveDate, endDate);

                    } else {
                        //Update price expire date to null as customer is subscribed to same plan
                        for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(line.getItemId())) {
                            customerPriceBl.expirePrices(plan.getId(), null);
                        }
                    }
                } else {
                    Integer itemId = line.getItemId();
                    int count = orderDas.getCountOfOrderLineByPlanItem(
                            line.getPurchaseOrder().getBaseUserByUserId().getEntity().getId(),
                            userId,
                            itemId,
                            line.getPurchaseOrder().getId());
                    if ( 0 == count ) {
                        PlanBL.unsubscribe(userId, itemId, effectiveDate);
                    } else {
                        //Update price expire date to null as customer is subscribed to same plan
                        for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(itemId)) {
                            customerPriceBl.expirePrices(plan.getId(), null);
                        }
                        logger.debug("user {} has subscribed same planItem {} {} times and customer price already "
                                + "overridden during subscription, so do not remove customer price", userId, itemId, count);
                    }
                }
            }
        }
    }

    /**
     * Un-subscribes the given user to any plans held by the list of order lines. The customer
     * is un-subscribed only if there are no other orders subscribing the user to the plan.
     *
     * should be called after an order is modified in the persistence context.
     *
     * @param lines lines to process
     * @param userId user id of customer to subscribe
     */
    private void removeCustomerPlans(List<OrderLineDTO> lines, Integer userId) {
        logger.debug("Processing {} order line(s), removing plans from user {}", lines.size(), userId);
        for (OrderLineDTO line : lines) {
            // make sure the customer is not subscribed to the plan after deleting the order. If the customer
            // is still subscribed it means another order has a subscription item for this plan, so we should
            // leave the prices in place.
            if (line.getItem() != null && !line.getItem().getPlans().isEmpty()) {
                Integer itemId = line.getItemId();
                int count = orderDas.getCountOfOrderByItem(line.getPurchaseOrder().getBaseUserByUserId().getEntity().getId(), userId, itemId);

                if (!PlanBL.isSubscribed(userId, line.getItemId(), TimezoneHelper.companyCurrentDateByUserId(userId))) {
                    if( count == 1 ) {
                        logger.debug("Un-subscribing user {} from plan item {}", userId, line.getItemId());
                        PlanBL planBL= new PlanBL(line.getItem().getPlans().iterator().next());
                        //The below will remove the prices for a deleted plan order
                        planBL.unsubscribe(userId);
                    } else {
                        logger.debug("user {} has subscribed same planItem {} {} times and customer price alreday "
                                + "overridden during subscription, so do not remove customer price", userId, itemId, count);
                    }
                }
            }
        }
    }

    /**
     * This function sets the primary order id for bundled orders and the primary order itself.
     * The purpose is to link all the orders created due to a plan order.
     * The linkage is through purchase_order.primary_order_id at database level.
     * At domain level, the OrderDTO.primaryOrderDTO forms the linkage.
     * @param orderIds
     */
    private void setPrimaryOrderForLinkedOrders(List<Integer> orderIds) {
        if (orderIds != null && !orderIds.isEmpty()) {
            // if any bundled orders are found, also add the main order
            orderIds.add(order.getId());
            OrderDTO[] orderArray = new OrderDTO[orderIds.size()];
            OrderDTO primaryOrder = null;
            int orderIndex = 0;

            // check if Primary Order exists on any pre-existing orders
            for (Integer orderId : orderIds) {
                OrderDTO orderDTO = orderDas.findNow(orderId);
                orderArray[orderIndex++] = orderDTO;
                if (primaryOrder == null && orderDTO.hasPrimaryOrder() && !orderDTO.getNotes().equals(DISCOUNT_NOTE)) {
                    primaryOrder = orderDTO.getParentOrder();
                }
            }
            if (primaryOrder == null) {
                primaryOrder = order;
            }

            for (OrderDTO orderDTO : orderArray) {
                if (orderDTO.getId().intValue() != primaryOrder.getId().intValue()) {
                    orderDTO.setParentOrder(primaryOrder);
                    primaryOrder.getChildOrders().add(orderDTO);
                    orderDas.save(orderDTO);
                }
            }
        }
    }

    /**
     * Gathers bundled plan items by period, creating a new order for each distinct period containing
     * the bundled items of any subscribed plan.
     * <p/>
     * Created "bundled orders" will be based off of the original plan subscription order. Bundled orders
     * will have the same active dates, billing type etc. as the original order.
     *
     * @param order    order holding the plan subscription
     * @param lines    lines containing plan subscriptions
     * @param baseUser user to use when adding lines
     */
    private List<Integer> addBundledItems(OrderDTO order, List<OrderLineDTO> lines,
            UserDTO baseUser, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {
        logger.debug("Processing {} order line(s), updating/creating orders for bundled items.", lines.size());

        // map of orders keyed by user & period
        Map<String, OrderDTO> orders = new HashMap<>();

        /* period Id change entity wise so build currentOrderKey and planLineOrderKey
           using period unit and value to avoid concurrent modification error.*/

        OrderPeriodDTO orderPeriod = new OrderPeriodDAS().find(order.getPeriodId());
        String currentOrderKey = order.getPeriodId().intValue() != Constants.ORDER_PERIOD_ONCE.intValue() ?
                baseUser.getId() + "_" + orderPeriod.getUnitId() + "_" + orderPeriod.getValue() :
                    baseUser.getId() + "_" + order.getPeriodId();
                orders.put(currentOrderKey, order);

                Map<Integer, BigDecimal> oldPlansQuantityMap = new HashMap<>();
                for (OrderLineDTO line : order.getLines()) {
                    if (line.getDeleted() == 0 && line.getItem() != null && line.getItem().isPlan()) {
                        oldPlansQuantityMap.put(line.getItemId(), line.getQuantity());
                    }
                }

                for (OrderLineDTO line : lines) {
                    //List of items that have a period of All Orders. These will be added to all created orders.
                    Set<PlanItemDTO> allOrdersPlanItems = new HashSet<>();
                    if (line.getDeleted() == 0 && line.getItem() != null && line.getItem().isPlan()) {
                        Set<OrderDTO> ordersForCurrentPlan = new HashSet<>();
                        PlanDTO plan = getPlanForItem(line.getItem());
                        String planLineOrderKey = plan.getPeriod().getId() != Constants.ORDER_PERIOD_ONCE ?
                                baseUser.getId() + "_" + plan.getPeriod().getUnitId() + "_" + plan.getPeriod().getValue() :
                                    baseUser.getId() + "_" + plan.getPeriod().getId();
                                if (line.getId() <= 0 && !planLineOrderKey.equals(currentOrderKey)) {  // new order line with incorrect period for current order
                                    // fetch the bundled order, or create a new one as necessary
                                    if (!orders.containsKey(planLineOrderKey)) {
                                        logger.debug("Getting bundle order for user {} and period {}", baseUser.getId(), plan.getPeriod().getId());
                                        OrderDTO bundledOrder = getBundleOrder(baseUser, plan.getPeriod(), line.getItemId(), order, order.getId() != null ? order : null);
                                        orders.put(planLineOrderKey, bundledOrder);
                                    }
                                    // move orderLine to correct order with same period
                                    line.moveToOtherOrder(orders.get(planLineOrderKey));
                                }  else {
                                    Map<PlanItemDTO, BigDecimal> planItemMaps = PlanItemBL.collectPlanItems(Arrays.asList(line));

                                    // Get the quantity of purchased plans in this order.
                                    BigDecimal newPlanQuantity = line.getQuantity();

                                    // Get the old quantity of plans in the order.
                                    BigDecimal oldPlanQuantity = BigDecimal.ZERO;
                                    if (oldPlansQuantityMap.containsKey(line.getItemId())) {
                                        oldPlanQuantity = oldPlansQuantityMap.get(line.getItemId());
                                    }

                                    // Calculate the quantity to add to the order.
                                    // If the order id is null it means we are creating it so we use the new quantity.
                                    // If the order id is not null and the new quantity is greater that the old one then it will be
                                    // a positive number. Otherwise it will be a negative number.
                                    // If the order id is not null and the old quantity is equals to the new one then we use zero.
                                    BigDecimal quantityDifference = BigDecimal.ZERO;
                                    if (order.getId() == null) {
                                        quantityDifference = newPlanQuantity;
                                    } else if (oldPlanQuantity.compareTo(newPlanQuantity) != 0) {
                                        quantityDifference = newPlanQuantity.subtract(oldPlanQuantity);
                                    }

                                    // go through all bundled items and build/update orders as necessary
                                    for (PlanItemDTO planItem : planItemMaps.keySet()) {
                                        if (planItem.getBundle() != null && planItem.getBundle().getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                                            if ( planItem.getBundle().getPeriod().getId() != Constants.ORDER_PERIOD_ALL_ORDERS ) {
                                                if (!quantityDifference.equals(BigDecimal.ZERO)) {
                                                    OrderDTO targetOrder = addPlanItemToAppropriateOrder(planItem, quantityDifference, order, orders, lines, baseUser, appliedChanges);
                                                    ordersForCurrentPlan.add(targetOrder);
                                                } else {
                                                    logger.debug ("No quantity to add/remove, quantityDifference {}", quantityDifference);
                                                }
                                            } else {
                                                allOrdersPlanItems.add(planItem);
                                            }
                                        }
                                    }

                                    // #8931 - The items with "bundle period = all orders" will be added to all orders.
                                    if(allOrdersPlanItems.size()>0) {
                                        ordersForCurrentPlan.add(order);
                                    }

                                    // Add the All Order items to all the created orders.
                                    for (PlanItemDTO planItem : allOrdersPlanItems) {
                                        UserDTO user = PlanItemBundleBL.getTargetUser(planItem.getBundle().getTargetCustomer(), baseUser.getCustomer());
                                        for (OrderDTO bundledOrder : ordersForCurrentPlan) {
                                            logger.debug("Adding All Order item {} to order for user {} and period {}",
                                                    planItem.getItem().getId(),
                                                    user.getId(),
                                                    planItem.getBundle().getPeriod().getId());
                                            //Check if the order change contains the right quantity of the bundled item, in this
                                            //way you can add assets with the API by adding the bundled item with the right asset
                                            //If this is the case it don't add the line because it will add the bundled items with the
                                            //order change where is the asset
                                            if (!bundledItemIsAddedAsOrderChange(lines, planItem.getItem().getId())) {
                                                //Set Plan Item Id to identify given item is bundle item.
                                                bundledOrder.setPlanItemId(planItem.getId());
                                                addItem(bundledOrder,
                                                        planItem.getItem().getId(),
                                                        planItem.getBundle().getQuantity().multiply(newPlanQuantity),
                                                        user.getLanguage().getId(),
                                                        user.getId(),
                                                        user.getEntity().getId(),
                                                        user.getCurrency().getId(),
                                                        null, lines, false, (String)null, null);
                                            }
                                        }
                                    }
                                }
                    }
                }

                // remove the original order, it will be persisted when the transaction ends
                orders.remove(currentOrderKey);

                // get the billing configuration and check the prorating type at company level
                BillingProcessConfigurationDTO billingConfiguration =
                        new ConfigurationBL(baseUser.getEntity().getId()).getDTO();
                ProratingType companyLevelProratingType = billingConfiguration.getProratingType();

                List<Integer> orderIds = new ArrayList<>();
                // save new all bundled orders
                for (OrderDTO bundledOrder : new HashSet<>(orders.values())) {
                    if (bundledOrder.getId() == null) {
                        String pricingFieldStr = null;
                        if (null != bundledOrder.getPricingFields() ) {
                            pricingFieldStr = PricingField.setPricingFieldsValue(bundledOrder.getPricingFields().toArray(new PricingField[0]));
                        }

                        OrderBL orderBL= new OrderBL();
                        orderBL.processLines(bundledOrder,baseUser.getLanguageIdField(),baseUser.getEntity().getId(),baseUser.getId(), baseUser.getCurrencyId(),pricingFieldStr);
                        // setting the bundled order prorate flag looking at the company level configuration,
                        // and comparing bundle and customer billing cycle periods.
                        setBundledOrderProrateFlag(companyLevelProratingType, order, bundledOrder, baseUser);
                        orderIds.add(orderBL.create(baseUser.getEntity().getId(), baseUser.getId(), bundledOrder, appliedChanges));
                    } else {
                        OrderBL orderBL= new OrderBL(bundledOrder.getId());
                        orderBL.update(baseUser.getId(), bundledOrder, appliedChanges);
                        orderIds.add(bundledOrder.getId());
                    }
                }

                return orderIds;
    }

    private void setBundledOrderProrateFlag(ProratingType companyLevelProratingType, OrderDTO planOrder, OrderDTO bundledOrder, UserDTO baseUser) {

        Boolean prorateFlag = Boolean.FALSE;

        OrderPeriodDTO customerBillingCyclePeriod = baseUser.getCustomer().getMainSubscription().getSubscriptionPeriod();
        Integer customerBillingCyclePeriodUnitId = customerBillingCyclePeriod.getUnitId();
        Integer customerBillingCyclePeriodValue = customerBillingCyclePeriod.getValue();

        OrderPeriodDTO bundledOrderPeriod = bundledOrder.getOrderPeriod();
        Integer bundledOrderPeriodUnitId = bundledOrderPeriod.getUnitId();
        Integer bundledOrderPeriodValue = bundledOrderPeriod.getValue();

        if (null != customerBillingCyclePeriodUnitId &&
                null != customerBillingCyclePeriodValue &&
                null != bundledOrderPeriodUnitId &&
                null != bundledOrderPeriodValue) {

            boolean periodsEqual = bundledOrderPeriodUnitId.intValue() == customerBillingCyclePeriodUnitId.intValue() &&
                    bundledOrderPeriodValue.intValue() == customerBillingCyclePeriodValue.intValue();

            if (companyLevelProratingType.isProratingAutoOn()) {
                if (periodsEqual) {
                    prorateFlag = Boolean.TRUE;
                }
            } else if (companyLevelProratingType.isProratingManual()) {
                if (periodsEqual) {
                    prorateFlag = planOrder.getProrateFlag();
                }
            }


        }

        bundledOrder.setProrateFlag(prorateFlag);
    }

    private boolean bundledItemIsAddedAsOrderChange(List<OrderLineDTO> lines, int id) {
        for (OrderLineDTO line : lines) {
            if (line.getItemId() == id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add order line for PlanItem to appropriate order according to user, period and plan item
     * @param planItem plan item for add
     * @param quantityDifference  quantity difference for line
     * @param order order for processing
     * @param orders all orders map
     * @param lines new lines map
     * @param baseUser target user
     * @param appliedChanges
     * @return order in witch line was added
     */
    private OrderDTO addPlanItemToAppropriateOrder(PlanItemDTO planItem, BigDecimal quantityDifference,
            OrderDTO order, Map<String, OrderDTO> orders,
            List<OrderLineDTO> lines, UserDTO baseUser, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {
        PlanItemBundleDTO bundle = planItem.getBundle();

        UserDTO user = PlanItemBundleBL.getTargetUser(bundle.getTargetCustomer(), baseUser.getCustomer());
        OrderPeriodDTO targetPeriod = bundle.getPeriod();
        String mapKey = targetPeriod.getId() != Constants.ORDER_PERIOD_ONCE ?
                user.getId() + "_" + targetPeriod.getPeriodUnit().getId() + "_" + targetPeriod.getValue() :
                    user.getId() + "_" + targetPeriod.getId();
                if (new PlanItemDAS().isPlan(planItem.getId())) {
                    mapKey += "_" + planItem.getItem().getId();
                }

                // fetch the bundled order, or create a new one as necessary
                if (!orders.containsKey(mapKey)) {
                    logger.debug("Getting bundle order for user {} and period {}", user.getId(), bundle.getPeriod().getId());
                    OrderDTO bundledOrder = getBundleOrder(user, targetPeriod, planItem.getItem().getId(), order, order.getId() != null ? order : null);
                    orders.put(mapKey, bundledOrder);
                }

                /*
                   ALWAYS add the item if bundle addIfExists is true
                   if addIfExists is false, check to see if the line already exists before adding
                 */
                OrderDTO bundledOrder = orders.get(mapKey);
                if (bundle.addIfExists()
                        || (!bundle.addIfExists() && bundledOrder.getLine(planItem.getItem().getId()) == null)) {

                    logger.debug("Adding {} units of item {} to order for user {} and period {}",
                            bundle.getQuantity(),
                            planItem.getItem().getId(),
                            user.getId(),
                            bundle.getPeriod().getId());
                    //Get the orderLinePlanItem from order->orderLines->orderLinePlanItems where the itemId matches planitem.getItem.getId()
                    //From orderline plan item get the quantity and price
                    BigDecimal qty = bundle.getQuantity();

                    //Set Plan Item Id to identify given item is bundle item.
                    bundledOrder.setPlanItemId(planItem.getId());

                    addItem(bundledOrder,
                            planItem.getItem().getId(),
                            qty.multiply(quantityDifference),
                            user.getLanguage().getId(),
                            user.getId(),
                            user.getEntity().getId(),
                            user.getCurrency().getId(),
                            null,
                            lines, false, null, null);
                }
                createOrderChangesForBundledLineItems(appliedChanges, bundledOrder);
                return bundledOrder;
    }

    private void createOrderChangesForBundledLineItems(Map<OrderLineDTO, OrderChangeDTO> appliedChanges, OrderDTO bundledOrder) {
        for (OrderLineDTO line : bundledOrder.getLines()) {
            if (!line.getItem().isPlan()) {
                if (line.getOrderChanges() == null) {
                    line.setOrderChanges(new HashSet<>());
                }
                if (line.getOrderChanges().isEmpty() && null!=appliedChanges) {
                    OrderChangeDTO planChange = orderChangeForBundledItem(new ArrayList<>(appliedChanges.values()), line.getItem().getId());
                    if (planChange != null) {
                        OrderChangeDTO changeForItem = new OrderChangeDTO();
                        changeForItem.setApplicationDate(planChange.getApplicationDate());
                        changeForItem.setStartDate(planChange.getStartDate());
                        changeForItem.setItem(line.getItem());
                        changeForItem.setAppliedManually(1);
                        changeForItem.setOrderChangeType(planChange.getOrderChangeType());
                        changeForItem.setAssets(line.getAssets());
                        changeForItem.setUser(bundledOrder.getUser());
                        changeForItem.setUserAssignedStatus(planChange.getUserAssignedStatus());
                        changeForItem.setStatus(planChange.getStatus());
                        changeForItem.setPrice(line.getPrice());
                        changeForItem.setQuantity(line.getQuantity());
                        changeForItem.setOrder(bundledOrder);
                        changeForItem.setOrderLine(line);
                        changeForItem.setNextBillableDate(planChange.getNextBillableDate());
                        appliedChanges.put(line, changeForItem);
                    }
                }
            }
        }
    }

    private OrderChangeDTO orderChangeForBundledItem(List<OrderChangeDTO> appliedChanges, Integer bundleItemId) {
        for (OrderChangeDTO change : appliedChanges) {
            if (change.getItem().isPlan()) {
                List<PlanDTO> plansByAffectedItem = new PlanBL().getPlansBySubscriptionItem(change.getItem().getId());
                for (PlanDTO plan : plansByAffectedItem) {
                    if (plan.getPlanItems().stream().anyMatch(pi -> pi.getItem().getId() == bundleItemId)) {
                        return change;
                    }
                }
            }
        }
        return null;
    }

    /**
     * New Discounts : This method would go through all discount lines defined on an order
     * and create discount suborder with negative discount amount on the line.
     * It first finds out which order should be primary order for the disocunt suborder,
     * as in case of bundle orders, there could be suborders present already.
     * @param bundledOrderIds
     * @return
     */
    private List<Integer> addDiscountItems(OrderDTO originalOrderDTO, List<Integer> bundledOrderIds) throws SessionInternalError {
        List<Integer> discountOrderIds = new ArrayList<>();

        OrderDTO orderDto;
        OrderDTO subOrder = null;
        for (DiscountLineDTO discountLine : originalOrderDTO.getDiscountLines()) {
            discountLine.setPurchaseOrder(originalOrderDTO);
            orderDto = originalOrderDTO;
            subOrder = null;

            if (discountLine.isPlanItemLevelDiscount()) {
                // Its a plan item level discount, so check if there is a bundled order
                if (bundledOrderIds != null && !bundledOrderIds.isEmpty()) {
                    // add logic to check which bundle order id carries the plan item
                    // get the correct bundle order id and retrieve the sub-order
                    // set the order for processing discount as sub-order containing plan item
                    subOrder = getPlanItemDiscountSubOrder(discountLine.getPlanItem(), bundledOrderIds);
                    if (subOrder != null) {
                        orderDto = subOrder;
                    }
                }
            }

            //Always create a discount order for each discount line
            //Ignore the order period checking for one time or recurring order
            // amount or percentage or period based
            Integer discountSubOrderId = createDiscountSubOrder(orderDto, discountLine);
            discountOrderIds.add(discountSubOrderId);
            OrderDTO discountOrder = orderDas.find(discountSubOrderId);
            discountLine.setDiscountOrderLine(discountOrder.getLines().get(0));

            //discountLineDas.save(discountLine)
        }

        return discountOrderIds;
    }

    /**
     * New Discounts : This method returns suborder containing the plan item from the given list of bundleOrderIds
     * Note this logic will need a look into as we test plan item level discounts. ****
     * @param planItem
     * @param bundledOrderIds
     * @return Sub Order containing given planItem
     */
    private OrderDTO getPlanItemDiscountSubOrder(PlanItemDTO planItem, List<Integer> bundledOrderIds) {

        OrderDTO subOrder = null;
        if (planItem.hasBundle() && planItem.getBundle().hasQuantity()) {
            for (Integer bundleOrderId : bundledOrderIds) {
                OrderDTO bundleOrder = orderDas.find(bundleOrderId);
                if (bundleOrder != null &&
                        bundleOrder.getOrderPeriod().getId() == planItem.getBundle().getPeriod().getId()) {
                    for (OrderLineDTO line : bundleOrder.getLines()) {
                        if (line.hasItem()) {
                            if (line.getItemId() == planItem.getItem().getId()) {
                                subOrder = bundleOrder;
                                break;
                            }
                        }
                    }
                }
                if (subOrder != null) {
                    break;
                }
            }
        }

        return subOrder;
    }

    /**
     * New Discounts : Called for every discount line with period based discount,
     * Calls create Order function much the same way it is called from addBundledItems
     */
    private Integer createDiscountSubOrder(OrderDTO orderDto, DiscountLineDTO discountLine) throws SessionInternalError {

        UserDTO baseUser = orderDto.getBaseUserByUserId();

        // New Discounts : Creates new discount order instance and calls OrderBL.create to create/save the order.
        OrderDTO discountSubOrder = constructOrderDTOForDiscount(baseUser, orderDto, discountLine);
        discountSubOrder.setParentOrder(orderDto);
        OrderBL orderBL = new OrderBL();
        orderBL.processLines(discountSubOrder, baseUser.getLanguageIdField(), baseUser.getEntity().getId(), baseUser.getId(), baseUser.getCurrencyId(), null);
        orderBL.set(discountSubOrder);
        orderBL.recalculate(baseUser.getEntity().getId());
        discountSubOrder.setId(orderBL.createSingleOrder(baseUser.getEntity().getId(), baseUser.getId(), discountSubOrder, null));
        return discountSubOrder.getId();
    }

    /**
     * Attempts to find an active order for the given period and userId. If no order found, then a
     * new order will be created using the given order as a template. The new order inherits everything
     * from the original order except the actual order lines.
     *
     * @param user target user for the bundled items to be added to
     * @param period period of new order
     * @param template order to inherit details from
     * @return new order
     */
    private OrderDTO getBundleOrder(UserDTO user, OrderPeriodDTO period, OrderDTO template) {
        // try and find an existing order for this period
        OrderDTO order = new OrderDAS().findByUserAndPeriod(user.getId(), period, template.getActiveSince());

        Integer orderLinetype = null;
        boolean flag = false;
        if (order != null) {
            for (OrderLineDTO orderLine : order.getLines()) {
                orderLinetype = (orderLine.getOrderLineType() != null ? orderLine
                        .getOrderLineType().getId() : null);
                if (Constants.ORDER_LINE_TYPE_DISCOUNT == orderLinetype) {
                    flag = true;
                    break;
                }
            }
        }

        // no existing order found,
        // create a new order using the given order as a template
        if (order == null || flag) {
            logger.debug("No existing order for user {} and period {}, creating new bundle order", user.getId(), period.getId());
            order = new OrderDTO();
            order.setBaseUserByUserId(user);
            order.setOrderStatus(template.getOrderStatus());
            order.setOrderBillingType(template.getOrderBillingType());
            order.setOrderPeriod(period);
            order.setCurrency(template.getCurrency());
            order.setActiveSince(template.getActiveSince());
            order.setActiveUntil(template.getActiveUntil());
            order.setNotesInInvoice(template.getNotesInInvoice());

            // todo: append order notes with plan details
            order.setNotes(template.getNotes());

            //copy mandatory Order Meta Fields
            for (MetaFieldValue<?> mfv : template.getMetaFields()) {
                order.setMetaField(user.getCompany().getId(), null, mfv.getField().getName(), mfv.getValue());
            }

            logger.debug("No existing order for user {} and period {}, creating new bundle order", user.getId(), period.getId());
            order = createNewLinkedOrder(user, period, template);
        }

        return order;
    }

    /**
     * This is a variation of generic getBundleOrder function which takes user, period, template and preferred itemId args.
     * This function takes additional arg for primaryOrder and preferred item. If order has a primary order, then attempts
     * to check if matching order exists in linked orders. If not found then creates a new order
     * based on user, period and template order
     * @param user user of target order
     * @param period period of target order
     * @param template template for new order creation
     * @param primaryOrder parent order for bundle order search
     * @param preferredItemId ID of item that can exists in order for selecting it as bundle (add quantity of item better than create new item)
     * @return OrderDTO order found or created
     */
    private OrderDTO getBundleOrder(UserDTO user, OrderPeriodDTO period, Integer preferredItemId, OrderDTO template, OrderDTO primaryOrder) {

        OrderDTO order = null;

        if (primaryOrder != null) {
            // try and find an existing order for this period in linked orders
            List<OrderDTO> firstLevelChilds = new LinkedList<>();
            // orders is already in heirarchy, use them
            firstLevelChilds.addAll(primaryOrder.getChildOrders());
            Collections.sort(firstLevelChilds, (o1, o2) -> {
                if (o1.getId() == null && o2.getId() == 0) {
                    return 0;
                }
                if (o1.getId() != null && o2.getId() == null) {
                    return 1;
                }
                if (o1.getId() == null && o2.getId() != null) {
                    return -1;
                }
                return o1.getId().compareTo(o2.getId());
            });
            OrderDTO suitableOrder = null;
            // search in hierarchy
            for (OrderDTO childOrder : firstLevelChilds) {
                if (childOrder.getDeleted() == 0 && user.getId() == childOrder.getUserId()
                        && childOrder.getOrderStatus().getOrderStatusFlag().equals(OrderStatusFlag.INVOICE)
                        && childOrder.getPeriodId().equals(period.getId())) {
                    suitableOrder = childOrder;
                    if (childOrder.getLine(preferredItemId) != null) {
                        order = childOrder;
                        break;
                    }
                }
            }
            if (order == null) {
                order = suitableOrder;
            }
        }

        // if no primary order or not found in linked orders, then create new from template
        if (order == null) {
            order = createNewLinkedOrder(user, period, template);
        }

        return order;
    }

    private OrderDTO createNewLinkedOrder(UserDTO user, OrderPeriodDTO period, OrderDTO template) {

        OrderDTO order = new OrderDTO();
        order.setBaseUserByUserId(user);
        order.setOrderStatus(template.getOrderStatus());
        order.setOrderBillingType(template.getOrderBillingType());
        order.setOrderPeriod(period);
        order.setCurrency(template.getCurrency());
        order.setActiveSince(template.getActiveSince());
        order.setActiveUntil(template.getActiveUntil());
        order.setNotesInInvoice(template.getNotesInInvoice());

        // todo: append order notes with plan details
        order.setNotes(template.getNotes());

        //copy mandatory Order Meta Fields
        for (MetaFieldValue<?> mfv : template.getMetaFields()) {
            order.setMetaField(user.getCompany().getId(), null, mfv.getField().getName(), mfv.getValue());
        }

        return order;
    }

    /**
     * New Discounts : Called for every discount line on order.
     * Creates new order, copy everything from given template
     * except the active since, active until which should be from discount (start date, end date)
     * In period based discount, copy the period from discount as well.
     * @param user
     * @param template
     * @param discountLine
     * @return
     */
    private OrderDTO constructOrderDTOForDiscount(UserDTO user, OrderDTO template, DiscountLineDTO discountLine) throws SessionInternalError {
        logger.debug("Call constructOrderDTOForDiscount, discountLine {}", discountLine);
        OrderDTO discountOrder = new OrderDTO();
        discountOrder.setBaseUserByUserId(user);
        discountOrder.setOrderStatus(template.getOrderStatus());
        discountOrder.setOrderBillingType(template.getOrderBillingType());
        //discountOrder.setParentOrder(template);

        DiscountDTO discountDto = discountLine.getDiscount();

        Date discountOrderStartDate = template.getActiveSince();
        discountOrder.setActiveSince(discountOrderStartDate);

        // New Discounts : Calls getDiscountOrderActiveUntil to decide on active until date
        discountOrder.setActiveUntil(
                resolveDiscountOrderActiveUntil(discountDto, discountOrderStartDate, template.getActiveUntil())
                );

        if (discountDto.isPeriodBased()) {
            discountOrder.setOrderPeriod(template.getOrderPeriod());
            // the discount order prorating flag should be set from the main order
            discountOrder.setProrateFlag(template.getProrateFlagValue());
        } else {
            discountOrder.setOrderPeriod(new OrderPeriodDAS().find(Constants.ORDER_PERIOD_ONCE));
            // One time discount - no need to prorate
            discountOrder.setProrateFlag(Boolean.FALSE);
        }
        discountOrder.setCurrency(template.getCurrency());
        discountOrder.setNotesInInvoice(template.getNotesInInvoice());
        discountOrder.setNotes(DISCOUNT_NOTE);

        //copy mandatory Order Meta Fields
        for (MetaFieldValue<?> mfv : template.getMetaFields()) {
            discountOrder.setMetaField(user.getCompany().getId(), null, mfv.getField().getName(), mfv.getValue());
        }

        // New Discounts : Creates the order line corresponding to the discount line and adds it to discount suborder
        List<OrderLineDTO> lines = new ArrayList<OrderLineDTO>();
        lines.add(createNewDiscountOrderLine(discountOrder, template, discountLine));
        discountOrder.setLines(lines);

        return discountOrder;
    }

    /**
     * New Discounts : This method sets the active untile date on discount suborder
     * looking at type of discount. If period based, it looks at period unit, period value
     * and discount end date. In all cases, it looks at primary order active until date
     * and keeps end date whichever is less.
     * ***** Note for periodic discounts, this will undergo change to use order periods instead of period units.
     * @param discountDto
     * @param startDate
     * @param endDate
     * @return
     */
    private Date resolveDiscountOrderActiveUntil(DiscountDTO discountDto, Date startDate, Date endDate) {

        Date discountOrderEndDate = endDate;	// set as template order active until by default

        if (discountDto.isPeriodBased()) {
            if (discountDto.isApplyToAllPeriods()) {
                return order.getActiveUntil();
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            int calendarField = 0;
            OrderPeriodDTO discountPeriod = new OrderPeriodDAS().findNow(discountDto.getPeriodUnit());

            if (discountPeriod != null && discountPeriod.getPeriodUnit() != null && discountDto.getPeriodValue() != null) {
                cal.add(MapPeriodToCalendar.map(discountPeriod.getPeriodUnit().getId()),
                        (discountDto.getPeriodValue() * discountPeriod.getValue()));
            }

            if (discountOrderEndDate == null || (cal.getTime().after(startDate) && cal.getTime().before(discountOrderEndDate))) {
                // subtract one day from cal.getTime because active until date is inclusive.
                cal.add(Calendar.DAY_OF_MONTH, -1);
                discountOrderEndDate = cal.getTime();
            }

            if (discountDto.getEndDate() != null &&
                    discountOrderEndDate.after(discountDto.getEndDate())) {
                discountOrderEndDate = discountDto.getEndDate();
            }
        } else {
            // Amount Based or Percentage Based
            if (discountOrderEndDate == null) {
                discountOrderEndDate = discountDto.getEndDate();
            } else {
                if (discountDto.getEndDate() != null &&
                        discountOrderEndDate.after(discountDto.getEndDate())) {
                    discountOrderEndDate = discountDto.getEndDate();
                }
            }
        }
        return discountOrderEndDate;
    }

    /**
     * New Discounts : Creates a new OrderLineDTO, Sets the Quantity as 1.
     * Calculates the discount amount based on discount strategy.
     * Sets the amount as negative of calculated discount amount on OrderLineDTO
     * @param subOrder
     * @param template
     * @param discountLine
     * @return
     */
    private OrderLineDTO createNewDiscountOrderLine(OrderDTO subOrder,
            OrderDTO template,
            DiscountLineDTO discountLine)
                    throws SessionInternalError {

        OrderLineDTO line = new OrderLineDTO();
        line.setOrderLineType(new OrderLineTypeDAS().find(Constants.ORDER_LINE_TYPE_DISCOUNT));
        line.setQuantity(1);	// Quantity of discount order line will always be 1
        line.setCreateDatetime(TimezoneHelper.serverCurrentDate());
        line.setEditable(Boolean.FALSE);
        line.setDeleted(0);
        line.setUseItem(Boolean.FALSE);

        /* New Discounts :
         ** Step 1: getDiscountableAmount: Gets the amount applicable for discount by
         **         checking whether discount is at line level or order level.
         */
        BigDecimal discountableAmount = null;
        if (discountLine.isPlanItemLevelDiscount()) {
            discountableAmount = getDiscountableAmount(subOrder, discountLine, true);
            line.setParentLine(findLineDiscountIsAppliedTo(subOrder, discountLine, true));
        } else {
            discountableAmount = getDiscountableAmount(template, discountLine, true);
            line.setParentLine(findLineDiscountIsAppliedTo(template, discountLine, true));
        }
        /*
         * Step 2: getDiscountAmount: Based on the type of discount (percentage or amount)
         *			Calculates the discount amount. For example, item price: $50.00, amount type discount rate=$5, discount amount=$5
         *			But if percentage type discount, then discount rate=$5 means 5% discount on $50, so discount amount=$2.5
         * Step 3: Negate the discount amount
         */
        DiscountDTO discountDto = discountLine.getDiscount();
        logger.debug("Call createNewDiscountOrderLine for discount {}", discountDto);
        BigDecimal discountAmount = discountDto.getDiscountAmount(discountableAmount).negate();


        // New Discounts : Imp. method call here to set the exact line description for discount line which will reflect on invoice.
        line.setDescription(getDiscountLineDescription(subOrder, discountLine, discountableAmount));
        line.setPrice(discountAmount);
        line.setAmount(discountAmount);
        line.setPurchaseOrder(subOrder);

        return line;
    }

    /**
     * Find the line in the order that the discount line is applied to.
     *
     * @param orderDto     Order containing the product which received the discount
     * @param discountLine The discount line.
     * @param create
     * @return
     */
    private OrderLineDTO findLineDiscountIsAppliedTo(OrderDTO orderDto, DiscountLineDTO discountLine, boolean create) {
        if (discountLine.isPlanItemLevelDiscount()) {
            if (create && null != orderDto) {
                PlanDTO plan = discountLine.getPlanItem().getPlan();

                OrderLineDTO planSubscriptionLine = order.getLine(plan.getItemId());
                if (null == planSubscriptionLine && null != order.getParentOrder()) {
                    planSubscriptionLine = order.getParentOrder().getLine(plan.getItemId());
                }

                logger.debug("Found planSubscriptionLine for discount line {} as {}", discountLine, planSubscriptionLine);
                return planSubscriptionLine;
            }
        } else if (discountLine.isProductLevelDiscount()) {

            for (OrderLineDTO orderLine : orderDto.getLines()) {
                if (orderLine.getItemId() == discountLine.getItem().getId()) {
                    return orderLine;
                }
            }

        }

        return null;
    }

    /**
     * New Discounts : Gets the discount-applicable amount.
     * The amount on which discount needs to be applied changes based on whether the discount is applied
     * at order level, plan item level or product level (or item level).
     * @param orderDto
     * @param discountLine
     * @return Discount-able amount
     */
    private BigDecimal getDiscountableAmount(OrderDTO orderDto, DiscountLineDTO discountLine, boolean create) {
        BigDecimal discountableAmount = BigDecimal.ZERO;
        Integer itemCurrencyId;
        Date today = orderDto.getActiveSince();

        if (discountLine.isPlanItemLevelDiscount()) {
            if (create && null != orderDto) {
                ItemDTO item = discountLine.getPlanItem().getItem();
                PlanDTO plan = discountLine.getPlanItem().getPlan();

                OrderLineDTO planItemLine = orderDto.getLine(item.getId());

                OrderLineDTO planSubscriptionLine = order.getLine(plan.getItemId());
                if (null == planSubscriptionLine && null != order.getParentOrder()) {
                    planSubscriptionLine = order.getParentOrder().getLine(plan.getItemId());
                }
                logger.debug("Found planSubscriptionLine for discount line {} as {}", discountLine, planSubscriptionLine);

                BigDecimal planQuantity = planSubscriptionLine != null ? planSubscriptionLine.getQuantity() : BigDecimal.ONE;
                BigDecimal planItemLineQuantity = discountLine.getPlanItem().getBundle().getQuantity().multiply(planQuantity);
                logger.debug("Order Line's quantity is {} whereas discountLine.planItem.bundleQty is {} ", ( null != planItemLine ? planItemLine.getQuantity(): null), discountLine.getPlanItem().getBundle().getQuantity());

                if(item.getPrice(TimezoneHelper.companyCurrentDate(item.getEntityId())).getType() == PriceModelStrategy.LINE_PERCENTAGE) {
                    logger.warn("The discount on a percentage Plan Item.");
                    throw new SessionInternalError("Invalid percentage item on a plan.");
                } else {
                    Date planLineStartDate = ( null != planSubscriptionLine && null != planSubscriptionLine.getStartDate() )
                            ? planSubscriptionLine.getStartDate()
                                    : today;

                            // take the discount-able amount as plan item rate X bundle qty
                            PriceModelDTO planItemPrice= discountLine.getPlanItem().getPrice(planLineStartDate);
                            if ( null != planItemPrice ) {
                                logger.debug("Plan Item Rate/Price for {} is {}", today, planItemPrice.getRate());
                                discountableAmount = planItemPrice.getRate().multiply(planItemLineQuantity);
                                itemCurrencyId = planItemPrice.getCurrency().getId();

                                // convert the line or plan item amount to order currency
                                CurrencyBL currencyBl = new CurrencyBL();
                                discountableAmount = currencyBl.convert(
                                        itemCurrencyId, orderDto.getCurrencyId(),
                                        discountableAmount, today,
                                        orderDto.getBaseUserByUserId().getEntity().getId());
                            }
                }
            }
        } else if (discountLine.isProductLevelDiscount()) {

            for (OrderLineDTO orderLine : orderDto.getLines()) {
                if (orderLine.getItemId() == discountLine.getItem().getId()) {
                    // as 1st iteration, we go by taking 1st line with matching item
                    discountableAmount = orderLine.getAmount();
                    break;
                }
            }

        } else if (discountLine.isOrderLevelDiscount()) {
            // Get the total of all lines on the order from getTotal method.
            BigDecimal total = BigDecimal.ZERO;
            for (OrderLineDTO line : discountLine.getPurchaseOrder().getLines()) {
                if (line.isSubscription() || line.isPenalty() || line.isLineTypeItem()) {
                    total = total.add(line.getAmount());
                }
            }
            discountableAmount = total;
        }

        return discountableAmount;
    }

    private ItemDTO getDiscountLineItem(DiscountLineDTO discountLine) {

        if (discountLine.isPlanItemLevelDiscount()) {
            return discountLine.getPlanItem().getItem();
        } else if (discountLine.isProductLevelDiscount()) {
            return discountLine.getItem();
        }

        return null;
    }

    /**
     * New Discounts : Sets the description on discount order lines.
     * The description is overall of the format: Rate % or $Rate Discount On Item Description
     * In case of order level discount, shows order id and total amount.
     * @param orderDto
     * @param discountLine
     * @return discount order line description
     */
    private String getDiscountLineDescription(OrderDTO orderDto, DiscountLineDTO discountLine, BigDecimal discountableAmount) {
        String description = "";
        DiscountDTO discountDto = discountLine.getDiscount();
        BigDecimal discountRate = discountDto.getRate();
        Integer orderBaseUserId = orderDto.getBaseUserByUserId().getId();
        Integer orderCurrencyId = orderDto.getCurrencyId();
        String discountDescription = discountDto.getDescription();

        String formattedDiscountAmount =
                com.sapienter.jbilling.server.util.
                Util.formatMoney(discountRate, orderBaseUserId, orderCurrencyId, false);

        String formattedDiscountPercentage =
                com.sapienter.jbilling.server.util.
                Util.formatPercentage(discountRate, orderBaseUserId);

        String formattedDiscountableAmount =
                com.sapienter.jbilling.server.util.
                Util.formatMoney(discountableAmount, orderBaseUserId, orderCurrencyId, false);

        UserBL user = new UserBL(orderBaseUserId);
        ResourceBundle bundle = ResourceBundle.getBundle("entityNotifications", user.getLocale());

        description = ( discountDto.isPeriodBased()
                ? bundle.getString("discount.line.discount.periodic")
                        : bundle.getString("discount.line.discount.onetime")
                )
                + Constants.SINGLE_SPACE + discountDescription;

        if (discountLine.isPlanItemLevelDiscount() || discountLine.isProductLevelDiscount()) {
            description += Constants.SINGLE_SPACE + bundle.getString("discount.on") + Constants.SINGLE_SPACE;
            description += discountLine.getDescription() +
                    Constants.SINGLE_SPACE +
                    bundle.getString("discount.line.amount") +
                    Constants.SINGLE_SPACE +
                    formattedDiscountableAmount;

        } else if (discountLine.isOrderLevelDiscount()) {

            if (discountDto.isPercentageBased()) {
                description += Constants.SINGLE_SPACE + bundle.getString("discount.at") + Constants.SINGLE_SPACE + formattedDiscountPercentage;
            }

            description += Constants.SINGLE_SPACE + bundle.getString("discount.on") +
                    Constants.SINGLE_SPACE +
                    bundle.getString("discount.line.amount") +
                    Constants.SINGLE_SPACE +
                    formattedDiscountableAmount;
        }

        logger.debug("discount line description : {}", description);
        return description;
    }

    private void updateEndOfOrderProcess(Date newDate) {
        OrderProcessDTO process = null;
        if (newDate == null) {
            logger.debug("Attempting to update an order process end date to null. Skipping");
            return;
        }
        if (order.getActiveUntil() != null) {
            process = orderDas.findProcessByEndDate(order.getId(),
                    order.getActiveUntil());
        }
        if (process != null) {
            logger.debug("Updating process id {}", process.getId());
            process.setPeriodEnd(newDate);

        } else {
            logger.debug("Did not find any process for order {} and date {}", order.getId(), order.getActiveUntil());
        }
    }

    private void updateNextBillableDay(Integer executorId, Date newDate) {
        if (newDate == null) {
            return;
        }
        // only if the new date is in the future
        if (order.getNextBillableDay() == null ||
                newDate.after(order.getNextBillableDay())) {
            // this audit can be added to the order details screen
            // otherwise the user can't account for the lost time
            eLogger.audit(executorId, order.getBaseUserByUserId().getId(),
                    Constants.TABLE_PUCHASE_ORDER, order.getId(),
                    EventLogger.MODULE_ORDER_MAINTENANCE,
                    EventLogger.ORDER_NEXT_BILL_DATE_UPDATED, null,
                    null, order.getNextBillableDay());
            // update the period of the latest invoice as well
            updateEndOfOrderProcess(newDate);
            // do the actual update
            order.setNextBillableDay(newDate);
        } else {
            logger.info("order {} next billable day not updated from {} to {}", order.getId(), order.getNextBillableDay(), newDate);
        }
    }

    public CachedRowSet getList(Integer entityID, Integer userRole,
            Integer userId)
                    throws SQLException, Exception {

        if (userRole.equals(Constants.TYPE_INTERNAL) ||
                userRole.equals(Constants.TYPE_ROOT) ||
                userRole.equals(Constants.TYPE_CLERK)) {
            prepareStatement(OrderSQL.listInternal);
            cachedResults.setInt(1, entityID);
        } else if (userRole.equals(Constants.TYPE_PARTNER)) {
            prepareStatement(OrderSQL.listPartner);
            cachedResults.setInt(1, entityID);
            cachedResults.setInt(2, userId);
        } else if (userRole.equals(Constants.TYPE_CUSTOMER)) {
            prepareStatement(OrderSQL.listCustomer);
            cachedResults.setInt(1, userId);
        } else {
            throw new Exception("The orders list for the type " + userRole + " is not supported");
        }

        execute();
        conn.close();
        return cachedResults;
    }

    public Integer getLatest(Integer userId)
            throws SessionInternalError {
        Integer retValue = null;
        try {
            prepareStatement(OrderSQL.getLatest);
            cachedResults.setInt(1, userId);
            cachedResults.setInt(2, userId);
            execute();
            if (cachedResults.next()) {
                int value = cachedResults.getInt(1);
                if (!cachedResults.wasNull()) {
                    retValue = value;
                }
            }
            cachedResults.close();
            conn.close();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    public Integer getLatestByItemType(Integer userId, Integer itemTypeId)
            throws SessionInternalError {
        Integer retValue = null;
        try {
            prepareStatement(OrderSQL.getLatestByItemType);
            cachedResults.setInt(1, userId);
            cachedResults.setInt(2, itemTypeId);
            execute();
            if (cachedResults.next()) {
                int value = cachedResults.getInt(1);
                if (!cachedResults.wasNull()) {
                    retValue = value;
                }
            }
            cachedResults.close();
            conn.close();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    public CachedRowSet getOrdersByProcessId(Integer processId)
            throws SQLException, Exception {

        prepareStatement(OrderSQL.listByProcess);
        cachedResults.setInt(1, processId);
        execute();
        conn.close();
        return cachedResults;
    }

    public List<Integer> getOrdersByProcess(Integer processId) {
        List<Integer> orders = new ArrayList();
        DataSource dataSource = (DataSource) Context.getBean(Context.Name.DATA_SOURCE);
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(OrderSQL.listByProcess)) {
                stmt.setInt(1, processId.intValue());
                try (ResultSet res = stmt.executeQuery()) {
                    while (res.next()) {
                        orders.add(res.getInt(1));
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new SessionInternalError(e);
        }
        return orders;
    }

    public void setStatus(Integer executorId, Integer statusId) {
        if (statusId == null || statusId.equals(order.getOrderStatus().getId())) {
            return;
        }
        if (executorId != null) {
            eLogger.audit(executorId, order.getBaseUserByUserId().getId(),
                    Constants.TABLE_PUCHASE_ORDER, order.getId(),
                    EventLogger.MODULE_ORDER_MAINTENANCE,
                    EventLogger.ORDER_STATUS_CHANGE,
                    order.getOrderStatus().getId(), null, null);
        } else {
            eLogger.auditBySystem(order.getBaseUserByUserId().getCompany().getId(),
                    order.getBaseUserByUserId().getId(),
                    Constants.TABLE_PUCHASE_ORDER,
                    order.getId(),
                    EventLogger.MODULE_ORDER_MAINTENANCE,
                    EventLogger.ORDER_STATUS_CHANGE,
                    order.getOrderStatus().getId(), null, null);

        }

        Integer oldStatusId = order.getOrderStatus().getId();

        NewStatusEvent event = new NewStatusEvent(
                order.getId(), oldStatusId, statusId, executorId);
        EventManager.process(event);

        OrderStatusDTO newStatus = new OrderStatusDAS().find(statusId);
        order.setOrderStatus(newStatus);

        if (newStatus.getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
            order.setFinishedDate(new Date());
        }

        if (newStatus.getOrderStatusFlag().equals(OrderStatusFlag.SUSPENDED_AGEING) ||
                newStatus.getOrderStatusFlag().equals(OrderStatusFlag.NOT_INVOICE) ||
                newStatus.getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
            SubscriptionInactiveEvent inactiveEvent = new SubscriptionInactiveEvent(executorId, order);
            EventManager.process(inactiveEvent);
        }

        // Expire customer FUP linked to the FINISHED Order with Epoch Date
        if (newStatus.getOrderStatusFlag().equals(OrderStatusFlag.FINISHED)) {
            CustomerPlanUnsubscriptionEvent  customerPlanUnsubscriptionEvent = new CustomerPlanUnsubscriptionEvent(
                    order.getBaseUserByUserId().getEntity().getId(),
                    order, Constants.CUSTOMER_PLAN_UNSUBSCRIBE_ORDER_FINISHED, false);
            EventManager.process(customerPlanUnsubscriptionEvent);
        }

        OrderStatusDTO oldStatus= new OrderStatusDAS().find(oldStatusId);
        if (oldStatus.getOrderStatusFlag().equals(OrderStatusFlag.NOT_INVOICE) && newStatus.getOrderStatusFlag().equals(OrderStatusFlag.INVOICE)) {
            SubscriptionActiveEvent activateEvent = new SubscriptionActiveEvent(executorId, order);
            EventManager.process(activateEvent);
        }
    }

    private void audit(Integer executorId, Date date) {
        eLogger.audit(executorId, order.getBaseUserByUserId().getId(),
                Constants.TABLE_PUCHASE_ORDER, order.getId(),
                EventLogger.MODULE_ORDER_MAINTENANCE,
                EventLogger.ROW_UPDATED, null,
                null, date);
    }

    private void audit(Integer executorId, Integer in) {
        eLogger.audit(executorId, order.getBaseUserByUserId().getId(),
                Constants.TABLE_PUCHASE_ORDER, order.getId(),
                EventLogger.MODULE_ORDER_MAINTENANCE,
                EventLogger.ROW_UPDATED, in,
                null, null);
    }

    public void reviewNotifications(Date today)
            throws NamingException, SQLException, Exception {
        INotificationSessionBean notificationSess = (INotificationSessionBean) Context.getBean(Context.Name.NOTIFICATION_SESSION);

        for (CompanyDTO ent : new CompanyDAS().findEntities()) {
            // find the orders for this entity

            // SQL args
            Object[] sqlArgs = new Object[4];
            sqlArgs[0] = new java.sql.Date(today.getTime());

            // calculate the until date

            // get the this entity preferences for each of the steps
            Integer preferenceDaysOrderNotificationS1 = null;
            int totalSteps = 3;
            int stepDays[] = new int[totalSteps];
            boolean config = false;
            int minStep = -1;
            for (int f = 0; f < totalSteps; f++) {
                try {
                    preferenceDaysOrderNotificationS1 =
                            PreferenceBL.getPreferenceValueAsInteger(ent.getId(), Constants.PREFERENCE_DAYS_ORDER_NOTIFICATION_S1 + f);
                    if (preferenceDaysOrderNotificationS1 == null) {
                        stepDays[f] = -1;
                    } else {
                        stepDays[f] = preferenceDaysOrderNotificationS1;
                        config = true;
                        if (minStep == -1) {
                            minStep = f;
                        }
                    }
                } catch (EmptyResultDataAccessException e) {
                    stepDays[f] = -1;
                }
            }

            if (!config) {
                logger.warn("Preference missing to send a notification for {} entity", ent.getId());
                continue;
            }

            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.setTime(today);
            cal.add(Calendar.DAY_OF_MONTH, stepDays[minStep]);
            sqlArgs[1] = new java.sql.Date(cal.getTime().getTime());

            // the entity
            sqlArgs[2] = ent.getId();
            // the total number of steps
            sqlArgs[3] = totalSteps;

            JdbcTemplate jdbcTemplate = Context.getBean(Context.Name.JDBC_TEMPLATE);
            SqlRowSet results = jdbcTemplate.queryForRowSet(OrderSQL.getAboutToExpire, sqlArgs);
            while (results.next()) {
                int orderId = results.getInt(1);
                Date activeUntil = results.getDate(2);
                int currentStep = results.getInt(3);
                int days = -1;

                // find out how many days apply for this order step
                for (int f = currentStep; f < totalSteps; f++) {
                    if (stepDays[f] >= 0) {
                        days = stepDays[f];
                        currentStep = f + 1;
                        break;
                    }
                }

                if (days == -1) {
                    throw new SessionInternalError("There are no more steps " +
                            "configured, but the order was selected. Order " +
                            " id = " + orderId);
                }

                // check that this order requires a notification
                cal.setTime(today);
                cal.add(Calendar.DAY_OF_MONTH, days);
                if (activeUntil.compareTo(today) >= 0 &&
                        activeUntil.compareTo(cal.getTime()) <= 0) {
                    /*/ ok
                    logger.debug("Selecting order " + orderId + " today = " +
                    today + " active unitl = " + activeUntil +
                    " days = " + days);
                     */
                } else {
                    /*
                    logger.debug("Skipping order " + orderId + " today = " +
                    today + " active unitl = " + activeUntil +
                    " days = " + days);
                     */
                    continue;
                }

                set(orderId);
                UserBL user = new UserBL(order.getBaseUserByUserId().getId());
                try {
                    NotificationBL notification = new NotificationBL();
                    ContactBL contact = new ContactBL();
                    contact.set(user.getEntity().getUserId());
                    MessageDTO message = notification.getOrderNotification(
                            ent.getId(),
                            currentStep,
                            user.getEntity().getLanguageIdField(),
                            order.getActiveSince(),
                            order.getActiveUntil(),
                            user.getEntity().getUserId(),
                            order.getTotal(), order.getCurrencyId());
                    // update the order record only if the message is sent
                    if (notificationSess.notify(user.getEntity(), message)) {
                        // if in the last step, turn the notification off, so
                        // it is skiped in the next process
                        if (currentStep >= totalSteps) {
                            order.setNotify(0);
                        }
                        order.setNotificationStep(currentStep);
                        order.setLastNotified(Calendar.getInstance().getTime());
                    }

                } catch (NotificationNotFoundException e) {
                    logger.warn("Without a message to send, this entity can't notify about orders. Skipping");
                    break;
                }

            }
        }
    }

    public TimePeriod getDueDate() {
        TimePeriod retValue = new TimePeriod();
        if (order.getDueDateValue() == null) {
            // let's go see the customer

            if (order.getBaseUserByUserId().getCustomer().getDueDateValue() == null) {
                // still unset, let's go to the entity
                ConfigurationBL config = new ConfigurationBL(
                        order.getBaseUserByUserId().getCompany().getId());
                retValue.setUnitId(config.getEntity().getDueDateUnitId());
                retValue.setValue(config.getEntity().getDueDateValue());
            } else {
                retValue.setUnitId(order.getBaseUserByUserId().getCustomer().getDueDateUnitId());
                retValue.setValue(order.getBaseUserByUserId().getCustomer().getDueDateValue());
            }
        } else {
            retValue.setUnitId(order.getDueDateUnitId());
            retValue.setValue(order.getDueDateValue());
        }

        // df fm only applies if the entity uses it
        int preferenceUseDfFm = 0;
        try {
            preferenceUseDfFm =
                    PreferenceBL.getPreferenceValueAsIntegerOrZero(
                            order.getUser().getEntity().getId(), Constants.PREFERENCE_USE_DF_FM);
        } catch (EmptyResultDataAccessException e) {
            // no problem go ahead use the defualts
        }
        if (preferenceUseDfFm == 1) {
            // now all over again for the Df Fm
            if (order.getDfFm() == null) {
                // let's go see the customer
                if (order.getUser().getCustomer().getDfFm() == null) {
                    // still unset, let's go to the entity
                    ConfigurationBL config = new ConfigurationBL(
                            order.getUser().getEntity().getId());
                    retValue.setDf_fm(config.getEntity().getDfFm());
                } else {
                    retValue.setDf_fm(order.getUser().getCustomer().getDfFm());
                }
            } else {
                retValue.setDf_fm(order.getDfFm());
            }
        } else {
            retValue.setDf_fm((Boolean) null);
        }

        retValue.setOwn_invoice(order.getOwnInvoice());

        return retValue;
    }

    /**
     * Calculates the target invoicing date for an order. If the order has been billed out to an
     * invoice before, then the "next billable day" will be returned. If the order has not yet
     * been billed, then this method will calculate the target invoice date based off of the
     * billing type (pre-paid or post-paid) and active since dates of the order.
     *
     * @return target invoicing date for the order
     */
    public Date getInvoicingDate() {
        if (order.getNextBillableDay() != null) {
            // next billable day set by billing process, no need to calculate anything
            return order.getNextBillableDay();

        } else {
            // order hasn't been billed out yet so there is no next billable day
            // calculate the target invoice date based on the billing type of the order
            //TODO Cycle Start Date should get priority over Active Since (Release 3.1 or lower)
            Date start = order.getActiveSince() != null ? order.getActiveSince() : order.getCreateDate();

            // pre-paid, customer pays in advance - invoice immediately
            if (order.getOrderBillingType().getId() == Constants.ORDER_BILLING_PRE_PAID) {
                return start;
            }

            // post-paid, customer pays later - invoice after 1 complete order period
            if (order.getOrderBillingType().getId() == Constants.ORDER_BILLING_POST_PAID) {
                //one-time period orders are treated as post-paid, but they
                //do not define period units so no calculation is done
                if (order.getOrderPeriod().getId() == Constants.ORDER_PERIOD_ONCE) {
                    return start;
                } else {
                    Calendar calendar = GregorianCalendar.getInstance();
                    calendar.setTime(start);

                    if (CalendarUtils.isSemiMonthlyPeriod(order.getOrderPeriod().getPeriodUnit())) {
                        calendar.setTime(CalendarUtils.addSemiMonthyPeriod(calendar.getTime()));
                    } else {
                        calendar.add(MapPeriodToCalendar.map(order.getOrderPeriod().getPeriodUnit().getId()),
                                order.getOrderPeriod().getValue());
                    }
                    return calendar.getTime();
                }
            }

            logger.debug("Order uses unknown billing type {}", order.getOrderBillingType().getId());
            return null;
        }
    }

    public boolean isDateInvoiced(Date date) {
        return date != null && order.getNextBillableDay() != null &&
                date.before(order.getNextBillableDay());
    }

    public Integer[] getListIds(Integer userId, Integer number, Integer entityId) {

        List<Integer> result = orderDas.findIdsByUserLatestFirst(userId, number);
        return result.toArray(new Integer[result.size()]);
    }

    public Integer[] getListIds(Integer userId, Integer limit, Integer offset, Integer entityId) {

        List<Integer> result = orderDas.findIdsByUserLatestFirst(userId, limit, offset);
        return result.toArray(new Integer[result.size()]);
    }

    public Integer[] getListIdsByDate(Integer userId, Date since, Date until, Integer entityId) {

        // add a day to include the until date
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(until);
        cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
        until = cal.getTime();

        List<Integer> result = orderDas.findIdsByUserAndDate(userId, since, until);
        return result.toArray(new Integer[result.size()]);
    }

    public List<OrderDTO> filterOrdersByParams(Integer page, Integer size, Date activeSince, Integer productId, Integer orderStatusId, Integer entityId) {
        return orderDas.getOrdersByActiveSinceProductAndStatus(page, size, activeSince, productId, orderStatusId, entityId);
    }

    public Integer[] getListIdsByItemType(Integer userId, Integer itemTypeId, Integer number) {

        List<Integer> result = orderDas.findIdsByUserAndItemTypeLatestFirst(userId, itemTypeId, number);
        return result.toArray(new Integer[result.size()]);
    }

    public Integer[] getByUserAndPeriod(Integer userId, Integer statusId)
            throws SessionInternalError {
        // find the order records first
        try {
            List result = new ArrayList();
            prepareStatement(OrderSQL.getByUserAndPeriod);
            cachedResults.setInt(1, userId);
            cachedResults.setInt(2, statusId);
            execute();
            while (cachedResults.next()) {
                result.add(cachedResults.getInt(1));
            }
            cachedResults.close();
            conn.close();
            // now convert the vector to an int array
            Integer retValue[] = new Integer[result.size()];
            result.toArray(retValue);

            return retValue;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Collection<OrderDTO> getActiveRecurringByUser(Integer userId) {
        return orderDas.findByUserSubscriptions(userId);
    }

    public OrderPeriodDTO[] getPeriods(Integer entityId, Integer languageId) {
        OrderPeriodDTO retValue[] = null;
        CompanyDAS companyDas = new CompanyDAS();
        CompanyDTO company = companyDas.find(entityId);

        Set<OrderPeriodDTO> periods = company.getOrderPeriods();
        if (periods == null || periods.size() == 0) {
            return new OrderPeriodDTO[0];
        }

        retValue = new OrderPeriodDTO[periods.size()];
        int i = 0;
        for (OrderPeriodDTO period : periods) {
            period.setDescription(period.getDescription(languageId));
            retValue[i++] = period;
        }
        return retValue;
    }

    public void updatePeriods(Integer languageId, OrderPeriodDTO periods[]) {
        for (OrderPeriodDTO period : periods) {
            orderPeriodDAS.save(period).setDescription(
                    period.getDescription(), languageId);
            period.getCompany().getOrderPeriods().add(period);
        }
    }

    public void addPeriod(Integer entityId, Integer languageId) {
        OrderPeriodDTO newPeriod = new OrderPeriodDTO();
        CompanyDAS companyDas = new CompanyDAS();
        newPeriod.setCompany(companyDas.find(entityId));
        PeriodUnitDAS periodDas = new PeriodUnitDAS();
        newPeriod.setPeriodUnit(periodDas.find(1));
        newPeriod.setValue(1);
        newPeriod = orderPeriodDAS.save(newPeriod);
        newPeriod.setDescription(" ", languageId);
    }

    public boolean deletePeriod(Integer periodId) {
        OrderPeriodDTO period = orderPeriodDAS.find(
                periodId);
        if (period.getPurchaseOrders().size() > 0) {
            return false;
        } else {
            orderPeriodDAS.delete(period);
            return true;
        }
    }

    public OrderLineWS getOrderLineWS(Integer id) {
        OrderLineDTO line = orderLineDAS.findNow(id);
        if (null == line) {
            logger.warn("Order line {} not found", id);
            return null;
        }
        Map<OrderLineDTO, OrderLineWS> hierarchyLineIdsMap = new HashMap<>();
        return getOrderLineWS(line, hierarchyLineIdsMap);
    }

    private OrderLineUsagePoolWS[] getOrderLineUsagePools(Set<OrderLineUsagePoolDTO> orderLineUsagePoolDtos) {
        OrderLineUsagePoolWS [] orderLineUsagePools = new OrderLineUsagePoolWS[null != orderLineUsagePoolDtos ? orderLineUsagePoolDtos.size() : 0];
        int index = 0;
        for (OrderLineUsagePoolDTO orderLineUsagePoolDto : orderLineUsagePoolDtos) {
            orderLineUsagePools[index++] = new OrderLineUsagePoolWS(orderLineUsagePoolDto.getId(),
                    orderLineUsagePoolDto.getOrderLine().getId(),
                    orderLineUsagePoolDto.getCustomerUsagePool().getId(),
                    null != orderLineUsagePoolDto.getQuantity() ? orderLineUsagePoolDto.getQuantity().toString() : "0");
        }

        return orderLineUsagePools;
    }

    private OrderLineWS getOrderLineWS(OrderLineDTO line, Map<OrderLineDTO, OrderLineWS> linesMapForHierarchy) {
        OrderLineWS retValue = linesMapForHierarchy.get(line);

        if (retValue == null) {
            if (this.order == null) {
                this.order = line.getPurchaseOrder();
            }
            retValue = new OrderLineWS(
                    line.getId(),
                    line.getItem() != null ? line.getItem().getId() : null,
                            line.getDescription(),
                            line.getAmount(),
                            line.getQuantity(),
                            line.getPrice(),
                            line.getCreateDatetime(),
                            line.getDeleted(),
                            ( null != line.getOrderLineType() ? line.getOrderLineType().getId() : null),
                            line.getEditable(),
                            (null != line.getPurchaseOrder() ? line.getPurchaseOrder().getId() : null) ,
                            line.getUseItem(),
                            line.getVersionNum(),
                            line.getCallIdentifier(),
                            line.getProvisioningStatusId(),
                            line.getProvisioningRequestId(),
                            null != line.getOrderLineUsagePools() ? getOrderLineUsagePools(line.getOrderLineUsagePools()) : new OrderLineUsagePoolWS[0],
                                    line.getItem() != null ? line.getItem().getNumber() : null,
                                            line.convertAssetIds(),
                                            MetaFieldHelper.toWSArray(line.getMetaFields()),
                                            line.getSipUri(),
                                            line.isPercentage(),
                            (null != line.getItem() ? line.getItem().isPlan() : false),
                            (null != line.getItem() && null != line.getItem().getPlans() &&
                                line.getItem().getPlans().iterator().hasNext() ?
                                    (null != line.getItem().getPlans().iterator().next() ?
                                            line.getItem().getPlans().iterator().next().getId() :
                                                null)
                                : null ));

            if(null != this.order && PreferenceBL.isTierCreationAllowed(this.order.getUser().getEntity().getId()) &&
                    line.hasOrderLineTiers()) {
                OrderLineTierBL orderLineTierBL = new OrderLineTierBL();
                List<OrderLineTierWS> orderLineTierWSList = new ArrayList<>();
                for(OrderLineTierDTO orderLineTierDTO : line.getOrderLineTiers()){
                    orderLineTierWSList.add(orderLineTierBL.getOrderLineTierWS(orderLineTierDTO));
                }
                retValue.setOrderLineTiers(orderLineTierWSList.toArray(new OrderLineTierWS[orderLineTierWSList.size()]));
            }

            // put currently processed line to map for resolving dependencies with this line in hierarchy
            linesMapForHierarchy.put(line, retValue);
            /*if (line.getParentLine() != null) {
                retValue.setParentLine(getOrderLineWS(line.getParentLine(), linesMapForHierarchy));
            }*/
            ProvisioningCommandBL commandBL = new ProvisioningCommandBL();
            retValue.setProvisioningCommands(commandBL.getCommandsList(line));
            retValue.setAllowedToUpdateOrderChange(PriceModelBL.isAllowedToUpdateOrderChange(line.getItem(),
                    this.order.getUser().getEntity().getId(),
                    line.getStartDate()));

            retValue.setAssetAssignmentIds(line.getAssetAssignmentIds());
            retValue.setChildLines(new OrderLineWS[line.getChildLines().size()]);
            retValue.setFreeCallCounter(line.getFreeCallCounter());
            int index = 0;
            for (OrderLineDTO childLine : line.getChildLines()) {
                retValue.getChildLines()[index] = getOrderLineWS(childLine, linesMapForHierarchy);
                retValue.getChildLines()[index].setParentLine(retValue);
                index++;
            }

        }
        return retValue;
    }

    public OrderLineDTO getOrderLine(Integer id) {
        OrderLineDTO line = orderLineDAS.findNow(id);
        if (line == null) {
            throw new SessionInternalError("Order line " + id + " not found");
        }
        return line;
    }

    public OrderLineDTO getOrderLine(OrderLineWS ws) {
        Map<OrderLineWS, OrderLineDTO> linesMapForHierarchy = new HashMap<>();
        return getOrderLine(ws, linesMapForHierarchy, null);
    }

    private OrderLineDTO getOrderLine(OrderLineWS ws, Map<OrderLineWS, OrderLineDTO> linesMapForHierarchy, Integer entityId) {

        OrderLineDTO dto = linesMapForHierarchy.get(ws);
        if (dto == null) {
            dto = new OrderLineDTO();
            dto.setId(ws.getId());
            dto.setAmount(ws.getAmountAsDecimal());
            dto.setCreateDatetime(ws.getCreateDatetime());
            dto.setDeleted(ws.getDeleted());
            dto.setUseItem(ws.getUseItem());
            dto.setDescription(ws.getDescription());
            dto.setEditable(ws.getEditable());
            dto.setItem(new ItemDAS().find(ws.getItemId()));
            dto.setItemId(ws.getItemId());
            dto.setOrderLineType(new OrderLineTypeDAS().find(ws.getTypeId()));
            dto.setPrice(ws.getPriceAsDecimal());
            dto.setPurchaseOrder(orderDas.find(ws.getOrderId()));
            dto.setQuantity(ws.getQuantityAsDecimal());
            dto.setVersionNum(ws.getVersionNum());
            dto.setProvisioningStatus(provisioningStatusDas.find(ws.getProvisioningStatusId()));
            dto.setProvisioningRequestId(ws.getProvisioningRequestId());
            dto.setCallIdentifier(ws.getCallIdentifier());

            if(entityId != null && PreferenceBL.isTierCreationAllowed(entityId) && ws.getOrderLineTiers() != null){
                OrderLineTierBL orderLineTierBL = new OrderLineTierBL();
                List<OrderLineTierDTO> list = new ArrayList<>();
                for(OrderLineTierWS lineTiers :ws.getOrderLineTiers()){
                    list.add(orderLineTierBL.getOrderLineTierDTO(lineTiers));
                }
                dto.setOrderLineTiers(list);
            }

            if (ws.getOrderLineUsagePools() != null) {
                CustomerUsagePoolDAS fupDas = new CustomerUsagePoolDAS();
                for (OrderLineUsagePoolWS olUsagePoolWs : ws.getOrderLineUsagePools()) {
                    OrderLineUsagePoolBL olUsagePoolBL = new OrderLineUsagePoolBL();
                    OrderLineUsagePoolDTO olUsagePoolDto = olUsagePoolWs.getId() > 0 ?
                            new OrderLineUsagePoolDTO(olUsagePoolBL.find(olUsagePoolWs.getId())) : new OrderLineUsagePoolDTO();
                            if (olUsagePoolDto.getId() == 0) {
                                olUsagePoolDto.setId(0);
                                olUsagePoolDto.setOrderLine(dto);
                                olUsagePoolDto.setQuantity(olUsagePoolWs.getQuantityAsDecimal());
                                olUsagePoolDto.setCustomerUsagePool(fupDas.find(olUsagePoolWs.getCustomerUsagePoolId()));
                            }
                            dto.addOrderLineUsagePool(olUsagePoolDto);
                }
            }

            if (dto.getItem() != null) {
                MetaFieldHelper.fillMetaFieldsFromWS(new HashSet<>(dto.getItem().getOrderLineMetaFields()), dto, ws.getMetaFields());
            } else if (entityId != null) {
                MetaFieldBL.fillMetaFieldsFromWS(entityId, dto, ws.getMetaFields());
            }

            if(ws.getAssetIds() != null) {
                for(Integer assetId : ws.getAssetIds()) {
                    AssetBL assetBL = new AssetBL();
                    AssetDTO assetDto = new AssetDTO(assetBL.find(assetId));
                    assetDto.setPrevOrderLine(assetDto.getOrderLine());
                    dto.addAsset(assetDto);
                }
            }
            // put currently processed line to map for resolving dependencies with this line in hierarchy
            linesMapForHierarchy.put(ws, dto);
            if (ws.getParentLine() != null) {
                dto.setParentLine(getOrderLine(ws.getParentLine(), linesMapForHierarchy, entityId));
            }
            if (ws.getChildLines() != null) {
                for (int i = 0; i < ws.getChildLines().length; i++) {
                    OrderLineWS childLine = ws.getChildLines()[i];
                    dto.getChildLines().add(getOrderLine(childLine, linesMapForHierarchy, entityId));
                }
            }
        }
        return dto;
    }

    public DiscountLineDTO getDiscountLineDTO(DiscountLineWS ws) {
        DiscountLineDTO dto = new DiscountLineDTO();
        dto.setId(ws.getId());
        dto.setDiscount(new DiscountDAS().find(ws.getDiscountId()));
        dto.setItem(new ItemDAS().find(ws.getItemId()));
        dto.setPlanItem(new PlanItemDAS().find(ws.getPlanItemId()));
        dto.setPurchaseOrder( null != ws.getOrderId() ? orderDas.find(ws.getOrderId()) : null );
        dto.setDiscountOrderLine(orderLineDAS.find(ws.getDiscountOrderLineId()));
        if ( !StringUtils.isEmpty(ws.getOrderLineAmount()) ) {
            dto.setOrderLineAmount(new BigDecimal(ws.getOrderLineAmount()));
        }
        dto.setDescription(ws.getDescription());
        return dto;
    }

    public DiscountLineWS getDiscountLineWS(DiscountLineDTO dto) {
        DiscountLineWS ws= new DiscountLineWS();
        ws.setId(dto.getId());
        ws.setDiscountId(dto.getDiscount() != null ? dto.getDiscount().getId() : null);
        ws.setItemId( null != dto.getItem() ? dto.getItem().getId() : null);
        ws.setPlanItemId(null != dto.getPlanItem() ? dto.getPlanItem().getId() : null);
        ws.setOrderId(null != dto.getPurchaseOrder() ? dto.getPurchaseOrder().getId() : null );
        ws.setDiscountOrderLineId( null != dto.getDiscountOrderLine() ? dto.getDiscountOrderLine().getId() : null);
        ws.setOrderLineAmount(dto.getOrderLineAmount() != null ? dto.getOrderLineAmount().setScale(Constants.BIGDECIMAL_QUANTITY_SCALE, Constants.BIGDECIMAL_ROUND).toString() : null);
        ws.setDescription(dto.getDescription());
        ws.setLineLevelDetails(getLineLevelDetails(ws));
        return ws;
    }

    /**
     * This function is used to set the value of the dropdown in case of order edit.
     * The value of the dropdown on screen is of the format:
     * 200|item|17.75|Sail Prod Go Qty=1 Rate=38
     * 200|planItem||Sail Prod Go Qty=2 Percentage=10
     * null|order||
     * @param ws
     */
    private String getLineLevelDetails(DiscountLineWS ws) {
        Integer itemId = null;
        String level = "";
        String lineLevelDetails;
        if (ws.getPlanItemId() != null) {
            itemId = ws.getPlanItemId();
            level="planItem";
            lineLevelDetails = itemId + Constants.PIPE +
                    level + Constants.PIPE +
                    Constants.PIPE +
                    ws.getDescription();
        } else if (ws.getItemId() != null) {
            itemId = ws.getItemId();
            level="item";
            lineLevelDetails = itemId + Constants.PIPE +
                    level + Constants.PIPE +
                    ws.getOrderLineAmount() + Constants.PIPE +
                    ws.getDescription();
        } else {
            level="order";
            lineLevelDetails = itemId + Constants.PIPE +
                    level + Constants.PIPE +
                    Constants.PIPE +
                    "-- Order Level Discount --";
        }

        return lineLevelDetails;
    }

    public List<OrderLineDTO> getRecurringOrderLines(Integer userId) {
        return orderLineDAS.findRecurringByUser(userId);
    }

    public OrderLineDTO getRecurringOrderLine(Integer userId, Integer itemId) {
        return orderLineDAS.findRecurringByUserItem(userId, itemId);
    }

    public List<OrderLineDTO> getOnetimeOrderLines(Integer userId, Integer itemId) {
        return orderLineDAS.findOnetimeByUserItem(userId, itemId);
    }

    public List<OrderLineDTO> getOnetimeOrderLines(Integer userId, Integer itemId, Integer months) {
        return orderLineDAS.findOnetimeByUserItem(userId, itemId, months);
    }

    public List<OrderLineDTO> getOnetimeOrderLinesByParent(Integer parentUserId, Integer itemId, Integer months) {
        return orderLineDAS.findOnetimeByParentUserItem(parentUserId, itemId, months);
    }

    public void updateOrderLine(OrderLineWS dto, Integer executorId) {
        OrderLineDTO line = getOrderLine(dto.getId());
        //asset create and update events
        List<AbstractAssetEvent> assetEvents = new ArrayList<>();

        if (dto.getQuantity() != null && (BigDecimal.ZERO.compareTo(dto.getQuantityAsDecimal()) == 0)) {
            // deletes the order line if the quantity is 0
            line.setDeleted(1);
            unlinkAssets(null, line, executorId, assetEvents, null);

            //	        Before release 4.1 we hard deleted the order line.
            //	        Not sure if that is the desired behaviour now that
            //	        we have asset assignments and order line tiers.

            //          orderLineDAS.delete(line);

        } else {
            line.setAmount(dto.getAmountAsDecimal());
            line.setDeleted(dto.getDeleted());
            line.setDescription(dto.getDescription());
            ItemDAS item = new ItemDAS();
            line.setItem(item.find(dto.getItemId()));
            line.setPrice(dto.getPriceAsDecimal());
            line.setQuantity(dto.getQuantityAsDecimal());
            line.setProvisioningStatus(provisioningStatusDas.find(dto.getProvisioningStatusId()));
            line.setProvisioningRequestId(dto.getProvisioningRequestId());
            line.setUseItem(dto.getUseItem());

            if (line.getItem().getAssetManagementEnabled() == 1) {
                if (line.getDeleted() == 1) {
                    unlinkAssets(null, line, executorId, assetEvents, null);
                } else {
                    Map<Integer, AssetDTO> assetMap = new HashMap<>(line.getAssets().size() * 2);
                    for (AssetDTO assetDTO : line.getAssets()) {
                        assetMap.put(assetDTO.getId(), assetDTO);
                    }

                    UserDTO assignedTo = line.getPurchaseOrder().getBaseUserByUserId();
                    //add new assets to the line
                    AssetStatusDTO orderSavedStatus = line.getItem().findItemTypeWithAssetManagement().findOrderSavedStatus();
                    for (Integer assetId : dto.getAssetIds()) {
                        //if the line wasn't linked to this asset link it
                        if (assetMap.remove(assetId) == null) {
                            AssetDTO assetDTO = assetDAS.find(assetId);

                            if (assetDTO.getAssetStatus().getIsAvailable() == 1) {
                                addAssetToOrderLine(executorId, line, assignedTo, orderSavedStatus, assetDTO, true, null);
                            } else {
                                throw new SessionInternalError("Asset [identifier=" + assetDTO.getIdentifier() + "] is not available");
                            }
                        }
                    }

                    //remove assets not linked to the line anymore
                    AssetStatusDTO defaultStatus = line.getItem().findItemTypeWithAssetManagement().findDefaultAssetStatus();
                    for (AssetDTO assetDTO : assetMap.values()) {
                        removeAssetFromOrderLine(line, executorId, defaultStatus, assetDTO, assetEvents, null);
                    }
                }
            }
        }

        //fire the asset events
        for (AbstractAssetEvent event : assetEvents) {
            EventManager.process(event);
        }
    }

    public void addAssetToOrderLine(
            Integer executorId, OrderLineDTO line, UserDTO assignedTo,
            AssetStatusDTO orderSavedStatus, AssetDTO assetDTO,
            boolean fireEvents, Map<OrderLineDTO, OrderChangeDTO> appliedChanges) {

        /* REQ.: 10256 the revert status param is set to false because the update of the asset status
                        is done by the AssetUpdateEvent fired below */
        new AssetReservationBL().releaseAsset(assetDTO.getId(), null, null);

        AssetStatusDTO previousStatus = assetDTO.getAssetStatus();
        assetDTO.setAssetStatus(orderSavedStatus);
        line.addAsset(assetDTO);

        OrderChangeDTO appliedChange = findOrderChange(line, appliedChanges);
        Date assignStartDate = null != appliedChange ? appliedChange.getStartDate() : TimezoneHelper.serverCurrentDate();

        AssetAssignmentDTO assignment = createAssetAssignment(line, assetDTO, assignStartDate);
        assetDTO.getAssignments().add(assignment);
        line.getAssetAssignments().add(assignment);

        if (fireEvents) {
            EventManager.process(new AssetAddedToOrderEvent(assetDTO.getEntity().getId(),
                    assetDTO, assignedTo, executorId));
            EventManager.process(new AssetUpdatedEvent(assetDTO.getEntity().getId(),
                    assetDTO, previousStatus, assignedTo, executorId));
        }
    }

    /**
     * Returns the current one-time order for this user for the given date.
     */
    public OrderDTO getCurrentOrder(Integer userId, Date date) {
        CurrentOrder co = new CurrentOrder(userId, date, null);
        set(orderDas.findNow(co.getCurrent()));
        return order;
    }

    public void addRelationships(Integer userId, Integer periodId, Integer currencyId) {
        if (periodId != null) {
            OrderPeriodDTO period = orderPeriodDAS.find(periodId);
            order.setOrderPeriod(period);
        }
        if (userId != null) {
            UserDAS das = new UserDAS();
            order.setBaseUserByUserId(das.find(userId));
        }
        if (currencyId != null) {
            CurrencyDAS das = new CurrencyDAS();
            order.setCurrency(das.find(currencyId));
        }
    }

    public OrderDTO getDTO(OrderWS other) {
        Map<OrderWS, OrderDTO> ordersHierarchyMap = new HashMap<>();
        Map<OrderLineWS, OrderLineDTO> orderLinesHierarchyMap = new HashMap<>();
        return getDTO(other, ordersHierarchyMap, orderLinesHierarchyMap);
    }

    public OrderDTO getDTO(OrderWS order, Map<OrderWS, OrderDTO> ordersHierarchyMap, Map<OrderLineWS, OrderLineDTO> orderLinesHierarchyMap) {
        OrderDTO retValue = ordersHierarchyMap.get(order);
        if (retValue == null) {
            retValue = new OrderDTO();
            retValue.setId(order.getId());
            // put order to map for hierarchy building
            ordersHierarchyMap.put(order, retValue);

            retValue.setBaseUserByUserId(new UserDAS().find(order.getUserId()));
            retValue.setBaseUserByCreatedBy(new UserDAS().find(order.getCreatedBy()));
            retValue.setCurrency(new CurrencyDAS().find(order.getCurrencyId()));
            retValue.setOrderStatus( null != order.getOrderStatusWS() ? new OrderStatusDAS().find(order.getOrderStatusWS().getId()) : null );
            retValue.setOrderPeriod(new OrderPeriodDAS().find(order.getPeriod()));
            retValue.setOrderBillingType(new OrderBillingTypeDAS().find(order.getBillingTypeId()));
            retValue.setActiveSince(order.getActiveSince());
            retValue.setActiveUntil(order.getActiveUntil());
            retValue.setCreateDate(order.getCreateDate());
            retValue.setNextBillableDay(order.getNextBillableDay());
            retValue.setDeleted(order.getDeleted());
            retValue.setNotify(order.getNotify());
            retValue.setLastNotified(order.getLastNotified());
            retValue.setNotificationStep(order.getNotificationStep());
            retValue.setDueDateUnitId(order.getDueDateUnitId());
            retValue.setDueDateValue(order.getDueDateValue());
            retValue.setDfFm(order.getDfFm());
            retValue.setAnticipatePeriods(order.getAnticipatePeriods());
            retValue.setOwnInvoice(order.getOwnInvoice());
            retValue.setNotes(order.getNotes());
            retValue.setNotesInInvoice(order.getNotesInInvoice());
            retValue.setCancellationFee(order.getCancellationFee());
            retValue.setCancellationFeePercentage(order.getCancellationFeePercentage());
            retValue.setCancellationFeeType(order.getCancellationFeeType());
            retValue.setCancellationMaximumFee(order.getCancellationMaximumFee());
            retValue.setCancellationMinimumPeriod(order.getCancellationMinimumPeriod());
            retValue.setProrateFlag(null != order.getProrateFlag() ? order.getProrateFlag() : false);
            retValue.setAutoRenew(order.getAutoRenew());
            retValue.setRenewNotification(order.getRenewNotification());
            retValue.setIsMediated(order.getIsMediated());

            for (OrderLineWS line : order.getOrderLines()) {
                if (line != null) {
                    OrderLineDTO lineDto = getOrderLine(line, orderLinesHierarchyMap, retValue.getBaseUserByUserId().getEntity().getId());
                    lineDto.setPurchaseOrder(retValue);
                    retValue.getLines().add(lineDto);
                }
            }

            if (order.hasDiscountLines()) {
                for (DiscountLineWS discountLine : order.getDiscountLines()) {
                    if (discountLine != null) {
                        retValue.getDiscountLines().add(getDiscountLineDTO(discountLine));
                    }
                }
            }

            retValue.setVersionNum(order.getVersionNum());

            if (order.getPricingFields() != null) {
                List<PricingField> pf = new ArrayList<>();
                pf.addAll(Arrays.asList(PricingField.getPricingFieldsValue(order.getPricingFields())));
                retValue.setPricingFields(pf);
            }
            MetaFieldBL.fillMetaFieldsFromWS(
                    retValue.getBaseUserByUserId().getCompany().getId(), retValue, order.getMetaFields());

            if (order.getParentOrder() != null) {
                retValue.setParentOrder(getDTO(order.getParentOrder(), ordersHierarchyMap, orderLinesHierarchyMap));
            }
            if (order.getChildOrders() != null) {
                for (OrderWS childOrder : order.getChildOrders()) {
                    retValue.getChildOrders().add(getDTO(childOrder, ordersHierarchyMap, orderLinesHierarchyMap));
                }
            }

            if (order.getUserCode() != null && order.getUserCode().length() > 0) {
                UserBL userBL = new UserBL();
                UserCodeDTO userCodeDto = userBL.findUserCodeForIdentifier(order.getUserCode(), retValue.getBaseUserByUserId().getCompany().getId());

                if (userCodeDto.hasExpired()) {
                    throw new SessionInternalError("The user code has expired and can not be linked to an order " + order.getUserCode(), new String[]{"OrderWS,userCode,userCode.validation.expired," + order.getUserCode()});
                }
                Set links = retValue.getUserCodeLinks();
                links.add(new UserCodeOrderLinkDTO(userCodeDto, retValue));
            }

            //convert from WS to DTO class
            if (order.getProvisioningCommands() != null) {
                List <OrderProvisioningCommandDTO> commandDTOList = new ArrayList<>();
                for (ProvisioningCommandWS command : order.getProvisioningCommands()) {
                    if (command != null && command.getProvisioningRequests() != null) {
                        List <ProvisioningRequestDTO> requestDTOList = new ArrayList<>();
                        for (ProvisioningRequestWS request : command.getProvisioningRequests()){
                            ProvisioningRequestDTO reqDTO = new ProvisioningRequestDTO();
                            reqDTO.setId(request.getId());
                            reqDTO.setIdentifier(request.getIdentifier());
                            reqDTO.setCreateDate(request.getCreateDate());
                            reqDTO.setProcessor(request.getProcessor());
                            reqDTO.setProvisioningCommand(request.getProvisioningCommandId() != null ?
                                    new ProvisioningCommandDAS().find(request.getProvisioningCommandId()) : null);
                            reqDTO.setRequestStatus(request.getRequestStatus());
                            reqDTO.setResultReceivedDate(request.getResultReceivedDate());
                            reqDTO.setRollbackRequest(request.getRollbackRequest());
                            reqDTO.setExecutionOrder(request.getExecutionOrder());
                            reqDTO.setSubmitDate(request.getSubmitDate());
                            reqDTO.setVersionNum(request.getVersionNum());
                            reqDTO.setResultMap(request.getResultMap());
                            reqDTO.setSubmitRequest(request.getSubmitRequest());
                            requestDTOList.add(reqDTO);
                        }
                        OrderProvisioningCommandDTO cmdDTO = new OrderProvisioningCommandDTO();
                        cmdDTO.setProvisioningRequests(requestDTOList);
                        cmdDTO.setCommandStatus(command.getCommandStatus());
                        cmdDTO.setVersionNum(command.getVersionNum());
                        cmdDTO.setExecutionOrder(command.getExecutionOrder());
                        cmdDTO.setCommandParameters(command.getParameterMap());
                        cmdDTO.setCreateDate(command.getCreateDate());
                        cmdDTO.setEntity(new CompanyDTO(command.getEntityId()));
                        cmdDTO.setLastUpdateDate(command.getLastUpdateDate());
                        cmdDTO.setId(command.getId());
                        cmdDTO.setName(command.getName());
                        commandDTOList.add(cmdDTO);
                    }
                }
                retValue.setProvisioningCommands(commandDTOList);
            }
        }
        return retValue;
    }

    /**
     * This method is used to process Order Lines and set correct line prices
     * based on the configured pricing models. This method is useful when re-rating Orders.
     *
     * @param order
     * @param languageId
     * @param entityId
     * @param userId
     * @param currencyId
     * @param pricingFields
     * @throws SessionInternalError
     */
    public void processLines(OrderDTO order, Integer languageId,
            Integer entityId, Integer userId, Integer currencyId,
            String pricingFields) throws SessionInternalError {

        OrderHelper.synchronizeOrderLines(order);
        String pricingFieldsFromMethod = pricingFields;
        Integer orderId = order.getId();
        for (OrderLineDTO line : order.getLines()) {
            logger.debug("Processing line {}", line);
            String pricingFieldsFromOrderLine = null;
            if (line.getUseItem() && 0 == line.getDeleted()) {
                if(null != orderId && orderId > 0) {
                    MediationService mediationService = Context.getBean(MediationService.BEAN_NAME);
                    List<Filter> filters = new ArrayList<>();
                    filters.add(Filter.integer("orderLineId", FilterConstraint.EQ, line.getId()));
                    filters.add(Filter.integer("orderId", FilterConstraint.EQ, orderId));
                    // get only the first jmr record, sorted by eventDate
                    List<JbillingMediationRecord> events = mediationService.findMediationRecordsByFilters(0, 1, filters);
                    for(JbillingMediationRecord event : events) {
                        if (line.getId() == event.getOrderLineId()) {
                            pricingFieldsFromOrderLine = event.getPricingFields();
                            break;
                        }
                    }
                }
                pricingFields = null != pricingFieldsFromOrderLine ? pricingFieldsFromOrderLine : pricingFieldsFromMethod;
                List<PricingField> fields = pricingFields != null ? Arrays
                        .asList(PricingField
                                .getPricingFieldsValue(pricingFields)) : new ArrayList<>();

                        ItemBL itemBl = new ItemBL(line.getItemId());
                        itemBl.setPricingFields(fields);

                        // get item with calculated price
                        ItemDTO item = itemBl.getDTO(languageId, userId, entityId,
                                currencyId, line.getQuantity(), order, line, false, null);
                        logger.debug("Populating line using item {}", item);

                        // set price or percentage from item
                        if(!line.getItem().isPlan() || (line.getItem().isPlan() && line.getPrice() != null
                                && !item.getPrice().equals(line.getPrice()))) {
                            line.setPrice(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
                        }

                        if (line.getAmount() == null) {
                            line.setAmount(BigDecimal.ZERO);
                        }

                        if (line.getPrice() == null) {
                            line.setPrice(BigDecimal.ZERO);
                        }

                        // set description and line type
                        line.setDescription(item.getDescription(languageId));
                        line.setTypeId(item.getOrderLineTypeId());
            } else if (null != line.getItemId()) {
                ItemBL itemBl = new ItemBL(line.getItemId());
                Integer itemtypeId = itemBl.getOrderLineTypeId();
                logger.debug("item type {}", itemtypeId);

                if (null != itemtypeId && (itemtypeId.equals(Constants.ORDER_LINE_TYPE_ADJUSTMENT) ||
                        itemtypeId.equals(Constants.ORDER_LINE_TYPE_PENALTY))) {
                    line.setTypeId(itemtypeId);
                }
            }
        }

        for(OrderDTO orderDTO : order.getChildOrders()){
            processLines(orderDTO, languageId, entityId, userId, currencyId, pricingFields);
        }

        OrderHelper.desynchronizeOrderLines(order);
    }

    public void processLine(OrderLineDTO line, Integer languageId,
            Integer entityId, Integer userId, Integer currencyId,
            String pricingFields) throws SessionInternalError {

        logger.debug("Processing line {}", line);
        boolean useItemFlag = line.getUseItem();

        if ((!line.getItem().isTieredPricingProduct() && useItemFlag) || line.getItem().isTieredPricingProduct()) {
            List<PricingField> fields = pricingFields != null ? Arrays
                    .asList(PricingField
                            .getPricingFieldsValue(pricingFields)) : null;

                    ItemBL itemBl = new ItemBL(line.getItemId());
                    itemBl.setPricingFields(fields);

                    // get item with calculated price
                    ItemDTO item = itemBl.getDTO(languageId, userId, entityId,
                            currencyId, line.getQuantity(), order, line, false, null);
                    logger.debug("Populating line using item {}", item);

                    // set price or percentage from item
                    if(!line.getItem().isPlan() || (line.getItem().isPlan() && line.getPrice() != null
                            && !item.getPrice().equals(line.getPrice()))) {
                        line.setPrice(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
                    }

                    if (line.getAmount() == null) {
                        line.setAmount(BigDecimal.ZERO);
                    }

                    if (line.getPrice() == null) {
                        line.setPrice(BigDecimal.ZERO);
                    }

                    // set description and line type
                    line.setDescription(item.getDescription(languageId));
                    line.setTypeId(item.getOrderLineTypeId());
        }
    }

    public void setProvisioningStatus(Integer orderLineId, Integer provisioningStatus) {
        OrderLineDTO line = orderLineDAS.findForUpdate(orderLineId);
        Integer oldStatus = line.getProvisioningStatusId();
        line.setProvisioningStatus(provisioningStatusDas.find(provisioningStatus));
        logger.debug("order line {}: updated provisioning status : {}", line, line.getProvisioningStatusId());

        // add a log for provisioning module
        eLogger.auditBySystem(order.getBaseUserByUserId().getCompany().getId(),
                order.getBaseUserByUserId().getId(),
                Constants.TABLE_ORDER_LINE, orderLineId,
                EventLogger.MODULE_PROVISIONING, EventLogger.PROVISIONING_STATUS_CHANGE,
                oldStatus, null, null);
    }

    /**
     * Create or update existed hierarchy from updatedDtos.
     * Method will perform validation for hierarchy and assets during update
     *
     * @param hierarchyRootOrder persisted hierarchy order root, null otherwise
     * @param updatedRootDTO updated orders tree root
     * @return updated orders hierarchy root
     */
    public static OrderDTO updateOrdersFromDto(OrderDTO hierarchyRootOrder, OrderDTO updatedRootDTO) {
        LinkedHashSet<OrderDTO> updatedOrders = OrderHelper.findOrdersInHierarchyFromRootToChild(updatedRootDTO);
        // parent could be updated, save previous parents in separate list
        LinkedHashSet<OrderDTO> previousParents = new LinkedHashSet<>();
        OrderDTO tmpOrder = hierarchyRootOrder != null ? hierarchyRootOrder.getParentOrder() : null;
        while (tmpOrder != null) {
            previousParents.add(tmpOrder);
            tmpOrder = tmpOrder.getParentOrder();
        }
        Map<OrderDTO, OrderDTO> updatedToPersistedOrdersMap = new HashMap<>();
        Map<OrderLineDTO, OrderLineDTO> updatedToPersistedOrderLinesMap = new HashMap<>();
        // first orders creation is possible
        if (hierarchyRootOrder == null) {
            hierarchyRootOrder = updatedRootDTO;
        }
        for (OrderDTO updatedDTO : updatedOrders) {
            if (updatedDTO.getId() == null) {
                updatedDTO.getLines().clear(); //order lines could be created only via orderChange
                updatedDTO.getChildOrders().clear(); // will be set later via parent link
                updatedToPersistedOrdersMap.put(updatedDTO, updatedDTO);
                if (updatedDTO.getParentOrder() != null) {
                    OrderDTO parentOrder = updatedToPersistedOrdersMap.get(updatedDTO.getParentOrder());
                    if (parentOrder != null) {
                        updatedDTO.setParentOrder(parentOrder);
                        parentOrder.getChildOrders().add(updatedDTO);
                    }
                }
            } else {
                OrderDTO targetOrder = OrderHelper.findOrderInHierarchy(hierarchyRootOrder, updatedDTO.getId());
                if (targetOrder == null) {
                    // search in previous parents
                    for (OrderDTO tmp : previousParents) {
                        if (tmp.getId().equals(updatedDTO.getId())) {
                            targetOrder = tmp;
                            break;
                        }
                    }
                    if (targetOrder == null) {
                        continue;
                    }
                }
                updatedToPersistedOrdersMap.put(updatedDTO, targetOrder);
                updateOrderOwnFields(targetOrder, updatedDTO, true);
                // if parent was set to NULL in input order, check that previous parent is in input hierarchy too
                // If previous parent is in input hierarchy - this means, current order parent was reset.
                // Otherwise - input hierarchy was trimmed at this point (part of hierarchy as input).
                // It DOES NOT mean, that parent should be reset to NULL in target order
                if (updatedDTO.getParentOrder() == null && targetOrder.getParentOrder() != null
                        && OrderHelper.findOrderInHierarchy(updatedRootDTO, targetOrder.getParentOrder().getId()) != null) {
                    targetOrder.getParentOrder().getChildOrders().remove(targetOrder);
                    targetOrder.setParentOrder(null);
                } else if (updatedDTO.getParentOrder() != null) {
                    // update parent order for target order if it was updated in input one
                    OrderDTO parentOrder = updatedToPersistedOrdersMap.get(updatedDTO.getParentOrder());
                    if (parentOrder != null) {
                        targetOrder.setParentOrder(parentOrder);
                        parentOrder.getChildOrders().add(targetOrder);
                    }
                }
                // only update lines, deleting and creating via orderChange
                for (OrderLineDTO line : targetOrder.getLines()) {
                    OrderLineDTO updatedLine = OrderHelper.findOrderLineWithId(updatedDTO.getLines(), line.getId());
                    if (updatedLine != null) {
                        updatedToPersistedOrderLinesMap.put(updatedLine, line);
                        updateOrderLineOwnFields(line, updatedLine);
                        // if parent was set to NULL in input line, check that previous parent line ordr  is in input hierarchy too
                        // If previous parent line order is in input hierarchy - this means, current line parent was reset.
                        // Otherwise - input hierarchy was trimmed at this point (part of hierarchy as input).
                        // It DOES NOT mean, that parent should be reset to NULL in target order line
                        if (updatedLine.getParentLine() == null && line.getParentLine() != null &&
                                OrderHelper.findOrderInHierarchy(updatedRootDTO, line.getParentLine().getPurchaseOrder().getId()) != null) {
                            line.getParentLine().getChildLines().remove(line);
                            line.setParentLine(null);
                        } else if (updatedLine.getParentLine() != null && updatedLine.getParentLine().getId() > 0) {
                            // update parent order line for target line if it was updated in input one
                            OrderLineDTO parentLine = updatedToPersistedOrderLinesMap.get(updatedLine.getParentLine());
                            if (parentLine != null) {
                                line.setParentLine(parentLine);
                                parentLine.getChildLines().add(line);
                            }
                        }
                    }
                }
            }
        }
        return hierarchyRootOrder;
    }

    public void setOrderProvisioningStatus(Integer provisioningStatus) {
        Integer oldStatus = order.getProvisioningStatusId();
        order.setProvisioningStatus(provisioningStatusDas.find(provisioningStatus));
        logger.debug("order {}: updated provisioning status : {}", order, order.getProvisioningStatusId());

        // add a log for provisioning module
        eLogger.auditBySystem(order.getBaseUserByUserId().getCompany().getId(),
                order.getBaseUserByUserId().getId(),
                Constants.TABLE_PUCHASE_ORDER, order.getId(),
                EventLogger.MODULE_PROVISIONING, EventLogger.PROVISIONING_STATUS_CHANGE,
                oldStatus, null, null);
    }

    public List<OrderDTO> getListOrdersPaged(Integer entityId, Integer user, Integer limit, Integer offset) {
        return new OrderDAS().findOrdersByUserPaged(user, limit, offset);
    }

    public void setOrderProvisioningStatus(OrderProvisioningCommandDTO orderProvisioningCommand) {

        switch (orderProvisioningCommand.getCommandStatus()) {
        case IN_PROCESS:
            // NOOP
            break;

        case SUCCESSFUL:

            if (order.getProvisioningStatusId().equals(
                    Constants.PROVISIONING_STATUS_PENDING_ACTIVE)) {

                order.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_ACTIVE));

            } else if (order.getProvisioningStatusId().equals(
                    Constants.PROVISIONING_STATUS_PENDING_INACTIVE)) {

                order.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_INACTIVE));

            } else {
                throw new SessionInternalError("Invalid or unexpected " +
                        "provisioning status: " +
                        order.getProvisioningStatusId());
            }

            break;

        case FAILED:
        case CANCELLED:
            order.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_FAILED));
            break;
        case UNAVAILABLE:
            order.setProvisioningStatus(provisioningStatusDas.find(Constants.PROVISIONING_STATUS_UNAVAILABLE));
            break;

        }
    }

    public Map<String, BigDecimal> calculateUpgradePlan(Integer planId, DiscountDTO discount) {
        if (order == null) {
            throw new SessionInternalError("Order cannot be null");
        }

        Integer categoryId = MetaFieldBL.getMetaFieldIntegerValueNullSafety(new EntityBL(order.getBaseUserByUserId().getEntity().getId()).getEntity()
                .getMetaField(com.sapienter.jbilling.client.util.Constants.SETUP_META_FIELD));

        BigDecimal total = CollectionUtils.isEmpty(order.getChildOrders()) ? order.getTotal() :
            order.getChildOrders().stream()
            .filter(orderChild -> orderChild.getLines().stream().noneMatch(OrderLineDTO::isDiscount))
            .map(OrderDTO::getTotal)
            .reduce(order.getTotal(), BigDecimal::add);

        BigDecimal totalAmountWihoutFee = CollectionUtils.isEmpty(order.getChildOrders()) ? getTotalAmountWithoutSetupFee(order, categoryId) :
            order.getChildOrders().stream()
            .map(childOrder -> getTotalAmountWithoutSetupFee(childOrder, categoryId))
            .reduce(getTotalAmountWithoutSetupFee(order, categoryId), BigDecimal::add);
        BigDecimal planAmount = new PlanBL(planId).calculateTotalPlanAmount(categoryId);
        Map<String, BigDecimal> upgradeResult = new HashMap<>();
        upgradeResult.put("Price of the old Plan", total);
        upgradeResult.put("Credit", totalAmountWihoutFee.subtract(prorateTotalAmount(totalAmountWihoutFee, new Date())));
        upgradeResult.put("New Plan Amount", planAmount);
        upgradeResult.put("Discount Amount", (discount == null ? BigDecimal.ZERO : discount.getDiscountAmount(planAmount)));

        return upgradeResult;
    }

    public BigDecimal prorateTotalAmount(BigDecimal totalAmount, Date proratedDate) {
        if (order == null || totalAmount == null) {
            logger.warn("Called with null parameters");
            return BigDecimal.ZERO;
        }

        Date cycleEnds = order.getNextBillableDay() == null ? addPeriodToDate(order.getActiveSince(), 1)
                : order.getNextBillableDay();
        Date cycleStarts = addPeriodToDate(cycleEnds, -1);

        PeriodOfTime fullBillingCycle = new PeriodOfTime(cycleStarts, cycleEnds, 0);
        PeriodOfTime period = new PeriodOfTime(cycleStarts, new Date(proratedDate.getTime() + TimeUnit.DAYS.toMillis(1)), fullBillingCycle.getDaysInPeriod());

        // this is an amount from a one-time order, not a real period of time
        if (period.getDaysInCycle() == 0) {
            return totalAmount;
        }

        // if this is not a fraction of a period, don't bother making any calculations
        if (period.getDaysInCycle() == period.getDaysInPeriod()) {
            return totalAmount;
        }

        BigDecimal oneDayQuantity = totalAmount.divide(new BigDecimal(period.getDaysInCycle()), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);

        return oneDayQuantity.multiply(new BigDecimal(period.getDaysInPeriod()))
                .setScale(Constants.BIGDECIMAL_QUANTITY_SCALE, Constants.BIGDECIMAL_ROUND);

    }

    private Date addPeriodToDate(Date date, int amount) {
        LocalDate localDate = DateConvertUtils.asLocalDate(date);
        return DateConvertUtils.asUtilDate(PeriodUnit.valueOfPeriodUnit(localDate.getDayOfMonth(),
                order.getOrderPeriod().getPeriodUnit().getId())
                .addTo(localDate, amount));
    }

    private BigDecimal getTotalAmountWithoutSetupFee(OrderDTO order, Integer setupCategoryId) {
        return order.getLines()
                .stream()
                .filter(line -> !line.isDiscount() && line.getItem().getItemTypes().stream().noneMatch(itemType -> itemType.getId() == setupCategoryId))
                .map(OrderLineDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validates if order lies within the validity period of product.
     */
    public Boolean isPeriodValid(ItemDTO item, Date startDate, Date endDate) {

        DateMidnight product;
        DateMidnight order;

        if(item.getActiveSince() == null && item.getActiveUntil() == null) {
            return true;
        }

        Boolean since;
        if(item.getActiveSince() == null) {
            since = true;
        } else {
            product = new DateTime(item.getActiveSince()).toDateMidnight();
            order = new DateTime(startDate).toDateMidnight();
            since = !product.isAfter(order);
        }

        Boolean until = false;
        if(item.getActiveUntil() == null) {
            until = true;
        } else {
            product = new DateTime(item.getActiveUntil()).toDateMidnight();
            order = new DateTime(startDate).toDateMidnight();
            until = !order.isAfter(product);
        }

        return since && until;
    }

    /**
     * This method verifies if a product from a given category can be added to order and if it belongs to
     * one per order/customer category then its quantity can not exceed one.
     *
     * @param item	item to be verified
     * @param activeSince	order's active since date
     * @param activeUntil	order's active until date
     * @param usedCategories	a list to reflect categories from which products can not be used
     * @return	true if product can be added to order
     */
    public boolean isCompatible(Integer userId, ItemDTO item, Date activeSince, Date activeUntil, List<Integer> usedCategories, OrderLineWS line) {

        //item can be null for taxes and discounts on order total
        if(item == null) {
            return true;
        }
        ItemTypeDAS itemTypeDAS = new ItemTypeDAS();
        for(ItemTypeDTO itemType : item.getItemTypes()) {
            Boolean onePerCustomer = itemType.isOnePerCustomer();
            Boolean onePerOrder = itemType.isOnePerOrder();

            if(onePerCustomer || onePerOrder) {
                Integer itemTypeId = itemType.getId();

                if(line.getQuantityAsDecimal().intValue() > 1) {
                    throw new SessionInternalError("Oner Per Customer/Order product can not have more than 1 quantity.",
                            new String[]{"validation.error.one.per.category.quantity"});
                }

                if(usedCategories.contains(itemTypeId)) {
                    return false;
                }

                usedCategories.add(itemTypeId);

                if (line.getId() == 0 && onePerCustomer && itemTypeDAS.isAssociatedToActiveOrder(userId, itemTypeId, activeSince, activeUntil) && !line.getIsSwapPlanCondition()) {
                    return false;
                }

            }
        }

        return true;
    }

    public static int countPlan(OrderLineWS[] orderLines) {
        int plans = 0;

        if (!ArrayUtils.isEmpty(orderLines)) {
            ItemBL itemBL = new ItemBL();
            for (OrderLineWS line: orderLines) {
                if(null != line.getItemId()) {
                    itemBL.set(line.getItemId());
                    if (itemBL.getEntity().hasPlans()) {
                        plans++;
                    }
                }
            }
        }

        return plans;
    }

    private PlanDTO getPlanForItem(ItemDTO item) {
        if (item == null || !item.isPlan()) {
            return null;
        }
        return item.getPlans().iterator().next();
    }

    private AssetAssignmentDTO createAssetAssignment(OrderLineDTO line, AssetDTO asset, Date startDatetime) {
        AssetAssignmentDTO assignment = new AssetAssignmentDTO();
        assignment.setOrderLine(line);
        assignment.setAsset(asset);
        assignment.setStartDatetime(startDatetime);
        return assignment;
    }

    /**
     *
     * @param userId
     * @param maxResults
     * @param offset
     * @param sortAttribute
     * @param order
     * @param callerLanguageId
     * @return
     */
    public List<OrderWS> findOrdersByUserPagedSortedByAttribute(Integer userId, int maxResults, int offset, String sortAttribute, ListField.Order order, Integer callerLanguageId) {
        List<Integer> orderIds = new OrderDAS().findOrdersByUserPagedSortedByAttribute(userId, maxResults, offset, sortAttribute, order);
        if (orderIds == null) {
            return null;
        }

        List<OrderWS> ordersWS = new ArrayList<>();
        for (Integer id : orderIds) {
            OrderBL bl = new OrderBL(id);
            ordersWS.add(bl.getWS(callerLanguageId));
        }
        return ordersWS;
    }

    private static List<Integer> getAccessEntities(UserDTO dto) {
        List<Integer> entityIds = new ArrayList<>();
        CompanyDTO company = dto.getEntity();
        while(company != null) {
            entityIds.add(company.getId());
            company = company.getParent();
        }
        return entityIds;
    }

    /**
     * get first active since date of active recurring order
     * @param userId
     * @param itemId
     * @return
     */
    public Date getSubscriptionOrderActiveSinceDateByUsageItem(Integer userId, Integer itemId) {
        return orderDas.getSubscriptionOrderActiveSinceDateByUsageItem(userId, itemId);
    }

    /**
     * get active since date of recurring order based on assetIdentifier
     * @param userId
     * @param eventDate
     * @param assetIdentifier
     * @return
     */
    public Date getSubscriptionOrderActiveSinceDate(Integer userId, Date eventDate, String assetIdentifier) {
        return orderDas.getSubscriptionOrderActiveSinceDate(userId, eventDate, assetIdentifier);
    }

    public List<OrderDTO> getOrdersWithActiveUntil(Integer userId, Date activeUntil){
        return orderDas.getOrdersWithActiveUntil(userId, activeUntil);
    }

    /**
     * Check if the order has free trial plan
     * @param orderId
     * @return returns true if the order has free trial plan else false
     */
    public boolean isFreeTrialOrder(Integer orderId) {
        if (null != orderId && orderId > 0) {
            for (OrderLineDTO lineDTO : orderDas.find(orderId).getLines()) {
                if (null != lineDTO.getItem() &&
                        lineDTO.getItem().isPlan()) {
                    for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(lineDTO.getItemId())) {
                        if (plan.isFreeTrial()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Updates given meta fields values on order
     * @param orderId
     * @param orderMetaFieldValues
     */
    public void updateOrderMetaFields(Integer orderId, MetaFieldValueWS[] orderMetaFieldValues) {
        order = orderDas.findNow(orderId);
        UserDTO user = order.getBaseUserByUserId();
        Integer entityId = user.getCompany().getId();
        Set<MetaField> mFToUpdate = MetaFieldHelper.validateAndGetMetaFields(EntityType.ORDER,
                orderMetaFieldValues, user.getLanguageIdField(), entityId);
        Map<String, Object> oldMetaFieldNameAndValueMap = MetaFieldHelper.convertMetaFieldValuesToMap(entityId, null, EntityType.ORDER, order);
        MetaFieldHelper.fillMetaFieldsFromWS(mFToUpdate, order, orderMetaFieldValues);
        logger.debug("metaField values {} added on order {}", orderMetaFieldValues, orderId);
        fireUpdateMetaFieldEvent(orderId, entityId, oldMetaFieldNameAndValueMap,
                MetaFieldHelper.convertMetaFieldValuesToMap(entityId, null, EntityType.ORDER, order));
    }

    /**
     * Creates {@link OrderMetaFieldValueWS} for given orderId
     * @param orderId
     * @return
     */
    public OrderMetaFieldValueWS getOrderMetaFieldValueWS(Integer orderId) {
        order = orderDas.findNow(orderId);
        Map<String, String> orderMetaFieldMap = new HashMap<>();
        for(MetaFieldValue<?> orderMf : order.getMetaFields()) {
            Object value = orderMf.getValue();
            if(null == value) {
                value = StringUtils.EMPTY;
            }
            if(orderMf.getField().getDataType().equals(DataType.CHAR)) {
                value = new String(char[].class.cast(value));
            }
            orderMetaFieldMap.put(orderMf.getFieldName(), value.toString());
        }
        logger.debug("Order level mf collected {} for order {}", orderMetaFieldMap, orderId);
        return new OrderMetaFieldValueWS(orderId, orderMetaFieldMap);
    }

    /**
     * Method to fetch subsription order based on user and assetIdentifier
     * @param userId
     * @param assetIdentifier
     * @return
     */
    public OrderDTO findOrderByUserAndAssetIdentifier(Integer userId, String assetIdentifier) {
        return orderDas.findOrderByUserAndAssetIdentifier(userId, assetIdentifier);
    }
}
