package com.sapienter.jbilling.server.company.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.company.CopyCompanyUtils;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.item.db.*;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.*;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vivek on 19/11/14.
 */
public class OrderCopyTask extends AbstractCopyTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderCopyTask.class));

    OrderDAS orderDAS = null;
    UserDAS userDAS = null;
    OrderBL orderBL = null;
    CompanyDAS companyDAS = null;
    ItemTypeDAS itemTypeDAS = null;
    AssetStatusDAS assetStatusDAS = null;

    private static final Class dependencies[] = new Class[]{
            ProductCopyTask.class,
            PlanCopyTask.class,
            OrderChangeStatusCopyTask.class
    };

    public Class[] getDependencies() {
        return dependencies;
    }

    public Boolean isTaskCopied(Integer entityId, Integer targetEntityId) {
        Long orderCount = orderDAS.findOrderCountByEntity(targetEntityId);
        return !(orderCount == 0);
    }

    public OrderCopyTask() {
        init();
    }

    public void init() {
        orderDAS = new OrderDAS();
        userDAS = new UserDAS();
        companyDAS = new CompanyDAS();
        itemTypeDAS = new ItemTypeDAS();
        assetStatusDAS = new AssetStatusDAS();
    }

    public void create(Integer entityId, Integer targetEntityId) {
        initialise(entityId, targetEntityId);  // This will create all the entities on which the current entity is dependent.
        CompanyDTO targetEntity = companyDAS.find(targetEntityId);
        CompanyDTO entity = companyDAS.find(entityId);
        LOG.debug("Create OrderCopyTask");
        Map<Integer, Integer> oldNewUserMap = CopyCompanyUtils.oldNewUserMap;
        for (Integer oldUserId : oldNewUserMap.keySet()) {

            UserDTO newUser = userDAS.find(oldNewUserMap.get(oldUserId));

            List<OrderDTO> orderDTOs = orderDAS.findAllUserByUserId(oldUserId);
            for (OrderDTO orderDTO : orderDTOs) {
                OrderDTO rootOrderDTO = OrderHelper.findRootOrderIfPossible(orderDTO);

                if (!CopyCompanyUtils.oldNewOrderMap.keySet().contains(rootOrderDTO.getId())) {
                    OrderWS orderWS = new OrderBL(orderDTO).getWS(entity.getLanguageId());
                    OrderWS rootOrder = OrderHelper.findRootOrderIfPossible(orderWS);

                    orderWS = rootOrder;
                    Integer rootOrderId = rootOrder.getId();

                    OrderChangeStatusDTO orderChangeStatusDTO = new OrderChangeStatusDAS().findApplyStatus(entityId);
                    OrderChangeWS[] orderChangeWSes = OrderChangeBL.buildFromOrder(rootOrder, orderChangeStatusDTO.getId());
                    for (OrderChangeWS orderChangeWS : orderChangeWSes) {
                        setOrderChange(orderChangeWS);
                    }

                    orderWS = copyOrder(orderWS, newUser, entity, targetEntity);
                    IWebServicesSessionBean local = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
                    Integer copyOrderId = local.copyCreateUpdateOrder(orderWS, orderChangeWSes, targetEntity.getId(), 
                            targetEntity.getLanguageId(), newUser.getId());
                    CopyCompanyUtils.oldNewOrderMap.put(rootOrderId, copyOrderId);
                }
            }
        }
        LOG.debug("Order copy has been completed.");
    }

    private OrderWS copyOrder(OrderWS rootOrder, UserDTO newUser, CompanyDTO entity, CompanyDTO targetEntity) {

//        OrderWS orderWS = new OrderBL(orderDTO).getWS(entity.getLanguageId());
        OrderWS orderWS = rootOrder;

        orderWS.setId(null);
        orderWS.setStatusStr(null);
        orderWS.setPeriodStr(null);
        orderWS.setBillingTypeStr(null);
        orderWS.setStatusStr(null);
        orderWS.setProratingOption("PRORATING_AUTO_OFF");
        orderWS.setFreeUsageQuantity(null);

        if (CopyCompanyUtils.oldNewOrderPeriodMap.get(orderWS.getPeriod()) != null) {
            orderWS.setPeriod(CopyCompanyUtils.oldNewOrderPeriodMap.get(orderWS.getPeriod()));
        }
        orderWS.setUserId(newUser.getId());
        if (orderWS.getOrderLines().length > 0) {
            orderWS.setOrderLines(new OrderLineWS[0]);
        }
        if (orderWS.getOrderStatusWS() != null) {
            OrderStatusDTO copyOrderStatusDTO = new OrderStatusDAS().find(CopyCompanyUtils.oldNewOrderStatusMap.get(orderWS.getOrderStatusWS().getId()));
            orderWS.setOrderStatusWS(OrderStatusBL.getOrderStatusWS(copyOrderStatusDTO));
        }
        orderWS.setChildOrders(new OrderWS[0]);
        if (orderWS.getProvisioningCommands().length > 0) {
            for (ProvisioningCommandWS provisioningCommandWS : orderWS.getProvisioningCommands()) {
                provisioningCommandWS.setId(0);
            }
        }
        copyDiscountLine(orderWS.getDiscountLines());

                /*We are creating order for now.*/
        orderWS.setGeneratedInvoices(null);

        if (orderWS.getMetaFields().length > 0) {
            for (MetaFieldValueWS metaFieldValueWS : orderWS.getMetaFields()) {
                metaFieldValueWS.setId(0);
                if (metaFieldValueWS.getGroupId() != null) {
                    metaFieldValueWS.setGroupId(CopyCompanyUtils.oldNewAccountInformationTypeMap.get(metaFieldValueWS.getGroupId()));
                }
            }
        }
        return orderWS;
    }

    public OrderChangeWS setOrderChange(OrderChangeWS orderChangeWS) {
        if (orderChangeWS != null) {

            orderChangeWS.setId(null);

            if (orderChangeWS.getParentOrderChange() != null) {
                setOrderChange(orderChangeWS.getParentOrderChange());
            }
            if (orderChangeWS.getAssetIds().length > 0) {
                List<Integer> copyAssetIds = new ArrayList<Integer>();
                for (Integer assetId : orderChangeWS.getAssetIds()) {
                    copyAssetIds.add(CopyCompanyUtils.oldNewAssetMap.get(assetId));

                }
                orderChangeWS.setAssetIds(copyAssetIds.toArray(new Integer[copyAssetIds.size()]));
            }
            if (CopyCompanyUtils.oldNewOrderChangeStatusMap.get(orderChangeWS.getUserAssignedStatusId()) != null) {
                orderChangeWS.setUserAssignedStatusId(CopyCompanyUtils.oldNewOrderChangeStatusMap.get(orderChangeWS.getUserAssignedStatusId()));

            }
            if (orderChangeWS.getMetaFields().length > 0) {
                for (MetaFieldValueWS metaFieldValueWS : orderChangeWS.getMetaFields()) {
                    metaFieldValueWS.setId(null);
                    if (metaFieldValueWS.getGroupId() != null) {
                        metaFieldValueWS.setGroupId(CopyCompanyUtils.oldNewAccountInformationTypeMap.get(metaFieldValueWS.getGroupId()));
                    }
                }
            }


            Integer itemId;
            if (CopyCompanyUtils.oldNewItemMap.get(orderChangeWS.getItemId()) == null) {
                itemId = new ItemDAS().find(orderChangeWS.getItemId()).getId();
            } else {
                itemId = CopyCompanyUtils.oldNewItemMap.get(orderChangeWS.getItemId());
            }
            ItemDTO itemDTO = new ItemDAS().find(itemId);
            if (itemDTO.isPlan()) {
                if (itemDTO.getPlans().size() > 0) {

                    PlanDTO planDTO = itemDTO.getPlans().iterator().next();
                    List<OrderChangePlanItemWS> orderChangePlanItemWSes = new ArrayList<OrderChangePlanItemWS>();
                    for (PlanItemDTO planItemDTO : planDTO.getPlanItems()) {
                        OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();

                        orderChangePlanItem.setItemId(planItemDTO.getItem().getId());
                        orderChangePlanItem.setId(0);
                        orderChangePlanItem.setOptlock(0);
                        BigDecimal bundleQuantity = planItemDTO.getBundle() != null ? planItemDTO.getBundle().getQuantity() : BigDecimal.ZERO;
                        orderChangePlanItem.setBundledQuantity(bundleQuantity.intValueExact());
                        orderChangePlanItem.setDescription(planItemDTO.getItem().getDescription());
                        orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);
                        orderChangePlanItemWSes.add(orderChangePlanItem);
                    }

                    orderChangeWS.setOrderChangePlanItems(orderChangePlanItemWSes.toArray(new OrderChangePlanItemWS[orderChangePlanItemWSes.size()]));
                }
            }
            orderChangeWS.setItemId(itemId);
            orderChangeWS.setOrderId(null);
            orderChangeWS.setOrderLineId(null);
            orderChangeWS.setParentOrderLineId(null);
        }
        return orderChangeWS;
    }

    private void copyDiscountLine(DiscountLineWS[] discountLineWSes) {
        for (DiscountLineWS discountLineWS : discountLineWSes) {
            discountLineWS.setId(0);
        }
    }
}
