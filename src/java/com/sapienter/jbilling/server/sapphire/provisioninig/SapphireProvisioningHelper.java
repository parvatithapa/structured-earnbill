package com.sapienter.jbilling.server.sapphire.provisioninig;

import static com.sapienter.jbilling.server.sapphire.provisioninig.SapphireProvisioningRequestType.DEVICE_SWAP;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.AssetDAS;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.sapphire.SapphireSwapAssetEvent;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;

abstract class SapphireProvisioningHelper {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /**
     * private constructor to make class non instantiable
     */
    private SapphireProvisioningHelper() {
        throw new IllegalStateException("Non instantiable class");
    }

    /**
     * Builds the ProvisioningRequestWS object by fetching all the required fields using userId
     *
     * @param userId
     * @param requestType
     * @return
     */
    public static SapphireProvisioningRequestWS mapToProvisioningRequest(Integer userId, String requestType, String aitGroupName) {
        SapphireProvisioningRequestWS provisioningRequest = new SapphireProvisioningRequestWS();
        // populate user details on request.
        setUserDetailsOnSapphireProvisioningRequest(userId, aitGroupName, provisioningRequest);
        List<OrderDTO> orders =  new OrderDAS().findRecurringOrders(userId,
                new OrderStatusFlag[] { OrderStatusFlag.INVOICE, OrderStatusFlag.NOT_INVOICE });
        logger.debug("order fetched {} for user {}", orders, userId);
        Integer orderId = !orders.isEmpty() ? orders.get(0).getId() : 0;
        List<SapphireDeviceWS> deviceList = new ArrayList<>();
        for(OrderDTO order : orders) {
            deviceList.addAll(collectDevicesFromOrder(order));
            if(null == provisioningRequest.getPlanId()) {
                Optional<Integer> planId = getPlanIdFromOrder(order, StringUtils.EMPTY);
                if(planId.isPresent()) {
                    provisioningRequest.setPlanId(planId.get());
                }
            }
        }
        provisioningRequest.setOrderId(orderId);
        provisioningRequest.setRequestType(requestType);
        provisioningRequest.setHardwareId("NONE");
        provisioningRequest.setDevices(deviceList.stream().toArray(SapphireDeviceWS[] :: new));
        return provisioningRequest;
    }

    private static List<SapphireDeviceWS> collectDevicesFromOrder(OrderDTO order) {
        List<SapphireDeviceWS> deviceList = new ArrayList<>();
        for(OrderLineDTO orderLine : order.getLines()) {
            Set<AssetDTO> assets = orderLine.getAssets();
            if(CollectionUtils.isNotEmpty(assets)) {
                for(AssetDTO  asset : assets) {
                    deviceList.add(mapAssetToDevice(asset));
                }
            }
        }
        // collecting assets from child orders.
        for(OrderDTO childOrder : order.getChildOrders()) {
            deviceList.addAll(collectDevicesFromOrder(childOrder));
        }
        return deviceList;
    }

    public static SapphireProvisioningRequestWS createProvisioningRequestFromSapphireSwapEvent(SapphireSwapAssetEvent swapAssetEvent,
            String aitGroupName, String serviceIdMfName) {
        OrderDAS orderDAS = new OrderDAS();
        OrderDTO oldOrder = orderDAS.findNow(swapAssetEvent.getOldOrderId());
        OrderDTO newOrder = orderDAS.findNow(swapAssetEvent.getNewOrderId());
        UserDTO user = newOrder.getBaseUserByUserId();
        SapphireProvisioningRequestWS provisioningRequest = new SapphireProvisioningRequestWS();
        // populate user details on request.
        setUserDetailsOnSapphireProvisioningRequest(user.getId(), aitGroupName, provisioningRequest);

        Optional<Integer> planId = getPlanIdFromOrder(oldOrder, serviceIdMfName);
        if(!planId.isPresent()) {
            List<OrderDTO> orders =  new OrderDAS().findRecurringOrders(user.getId(),
                    new OrderStatusFlag[] { OrderStatusFlag.INVOICE, OrderStatusFlag.NOT_INVOICE });
            logger.debug("order fetched {} for user {}", orders, user.getId());
            for(OrderDTO order : orders) {
                if(null == provisioningRequest.getPlanId()) {
                    planId = getPlanIdFromOrder(order, StringUtils.EMPTY);
                    if(planId.isPresent()) {
                        provisioningRequest.setPlanId(planId.get());
                        break;
                    }
                }
            }
        }
        if(planId.isPresent()) {
            provisioningRequest.setPlanId(planId.get());
        }
        provisioningRequest.setOrderId(newOrder.getId());
        provisioningRequest.setRequestType(DEVICE_SWAP.getRequestType());
        provisioningRequest.setHardwareId("NONE");
        provisioningRequest.setDevices(new SapphireDeviceWS[] {
                mapAssetToDevice(new AssetDAS().findNow(swapAssetEvent.getNewAssetId()))
        });
        return provisioningRequest;
    }

    public static SapphireProvisioningRequestWS createProvisioningRequestFromOrder(Integer orderId, Integer planOrderId,
            String aitGroupName, SapphireProvisioningRequestType requestType) {
        OrderDAS orderDAS = new OrderDAS();
        OrderDTO order = orderDAS.findNow(orderId);
        UserDTO user = order.getBaseUserByUserId();
        SapphireProvisioningRequestWS provisioningRequest = new SapphireProvisioningRequestWS();
        // populate user details on request.
        setUserDetailsOnSapphireProvisioningRequest(user.getId(), aitGroupName, provisioningRequest);

        // set subscription plan order id.
        if(null == planOrderId) {
            Optional<Integer> planId = getPlanIdFromOrder(order, StringUtils.EMPTY);
            if(planId.isPresent()) {
                planOrderId = planId.get();
            } else {
                List<OrderDTO> orders =  orderDAS.findRecurringOrders(user.getId(),
                        new OrderStatusFlag[] { OrderStatusFlag.INVOICE, OrderStatusFlag.NOT_INVOICE });
                for(OrderDTO subscriptionOrder : orders) {
                    planId = getPlanIdFromOrder(subscriptionOrder, StringUtils.EMPTY);
                    if(planId.isPresent()) {
                        planOrderId = planId.get();
                    }
                }

            }
        }
        provisioningRequest.setPlanId(planOrderId);
        provisioningRequest.setOrderId(orderId);
        provisioningRequest.setRequestType(requestType.getRequestType());
        provisioningRequest.setHardwareId("NONE");
        List<SapphireDeviceWS> assets = new ArrayList<>();
        for(OrderLineDTO orderLine : order.getLines()) {
            Set<AssetDTO> olAssets = orderLine.getAssets();
            if(CollectionUtils.isEmpty(olAssets)) {
                for(AssetDTO asset : olAssets) {
                    assets.add(mapAssetToDevice(asset));
                }
            }
        }
        provisioningRequest.setDevices(assets.toArray(new SapphireDeviceWS[0]));
        return provisioningRequest;
    }

    private static Optional<Integer> getPlanIdFromOrder(OrderDTO order, String serviceIdMfName) {
        if(null == order) {
            return Optional.empty();
        }
        // try to find plan id from given order.
        for(OrderLineDTO line : order.getLines()) {
            if(line.hasItem() && line.getItem().isPlan()) {
                logger.debug("plan found from given order {}", order.getId());
                return Optional.of(line.getItem().getPlan().getId());
            }
        }
        OrderDTO parentOrder = order.getParentOrder();
        // try to plan plan id from parent order.
        Optional<Integer> planId = getPlanIdFromOrder(parentOrder, serviceIdMfName);
        if(planId.isPresent()) {
            logger.debug("plan {} found from parent order {}", planId, parentOrder.getId());
            return planId;
        }

        if(StringUtils.isNotEmpty(serviceIdMfName)) {
            // try to find plan id from order level meta field.
            @SuppressWarnings("unchecked")
            MetaFieldValue<Integer> serviceIdMetaField = order.getMetaField(serviceIdMfName);
            if(null == serviceIdMetaField || serviceIdMetaField.isEmpty()) {
                return Optional.empty();
            }
            logger.debug("fetching plan from order level meta field {}", serviceIdMfName);
            OrderDTO serviceOrder = new OrderDAS().findNow(serviceIdMetaField.getValue());
            if(null != serviceOrder) {
                logger.debug("fetching plan from service order {}", serviceOrder.getId());
                planId = getPlanIdFromOrder(serviceOrder, serviceIdMfName);
                if(planId.isPresent()) {
                    logger.debug("plan {} found from service order {}", planId, serviceOrder.getId());
                    return planId;
                }
            }
        }
        return Optional.empty();
    }

    private static void setUserDetailsOnSapphireProvisioningRequest(Integer userId, String aitGroupName,
            SapphireProvisioningRequestWS provisioningRequest) {
        UserDTO userDTO = UserBL.getUserEntity(userId);
        Integer accountTypeId = new UserBL(userDTO.getId()).getAccountType().getId();
        AccountInformationTypeDTO contactInformationAIT = new AccountInformationTypeDAS().findByName(aitGroupName,
                userDTO.getEntity().getId(), accountTypeId);
        Integer groupId = contactInformationAIT.getId();
        logger.debug("group id {} found for account Type {}", groupId, accountTypeId);
        Date currentDate = userDTO.getCustomer().getCurrentEffectiveDateByGroupId(groupId);
        CustomerDTO customer = userDTO.getCustomer();
        CustomerAccountInfoTypeMetaField firstNameCAITMF = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.FIRST_NAME, groupId, currentDate);
        CustomerAccountInfoTypeMetaField lastNameCAITMF = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.LAST_NAME,
                groupId, currentDate);
        CustomerAccountInfoTypeMetaField emailCAITMF = customer.getCustomerAccountInfoTypeMetaFieldByUsageType(MetaFieldType.EMAIL,
                groupId, currentDate);
        String firstName = validateMetaField(firstNameCAITMF) ? (String) firstNameCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY;
        String lastName = validateMetaField(lastNameCAITMF) ? (String) lastNameCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY;
        String email = validateMetaField(emailCAITMF) ? (String) emailCAITMF.getMetaFieldValue().getValue() : StringUtils.EMPTY;
        provisioningRequest.setId(System.currentTimeMillis());
        provisioningRequest.setOfficeId(Integer.toString(accountTypeId));
        provisioningRequest.setClientId(userDTO.getId());
        provisioningRequest.setFirstName(firstName);
        provisioningRequest.setLastName(lastName);
        provisioningRequest.setEmail(email);
    }

    /**
     * validates the metafield and returns true or false
     *
     * @param metaField
     * @return
     */
    private static boolean validateMetaField(CustomerAccountInfoTypeMetaField metaField) {
        return metaField != null &&
                StringUtils.isNotEmpty(((StringMetaFieldValue) metaField.getMetaFieldValue()).getValue());
    }

    /**
     * Maps the asset object into the Device and returns the Device object
     *
     * @param asset
     * @return
     */
    private static SapphireDeviceWS mapAssetToDevice(AssetDTO asset) {
        SapphireDeviceWS device = new SapphireDeviceWS();
        device.setId(Integer.toString(asset.getId()));
        device.setItemCode(asset.getItem().getInternalNumber());
        device.setProvserialnumber(asset.getIdentifier());
        device.setSerialNo(asset.getIdentifier());
        device.setItemType(asset.getItem().getItemTypes().iterator().next().getDescription());
        return device;
    }

    /**
     * Updates customer provisioning meta field value
     * @param customerProvisioningMfName
     * @param userId
     * @param status
     */
    public static void updateCustomerProvisioningStatusMf(String customerProvisioningMfName, Integer userId, UserProvisioninigStatus status) {
        UserDAS userDAS = new UserDAS();
        UserDTO user = userDAS.findNow(userId);
        CustomerDTO customer = user.getCustomer();
        Integer entityId = user.getCompany().getId();
        MetaField provisioningMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.CUSTOMER }, customerProvisioningMfName);
        if(null == provisioningMf) {
            logger.error("{} not present on customer level metafield for entity {}", customerProvisioningMfName, entityId);
            throw new SessionInternalError(customerProvisioningMfName + " not found on customer level for entity "+ entityId);
        }
        @SuppressWarnings("unchecked")
        MetaFieldValue<String> provisioningMfValue = customer.getMetaField(customerProvisioningMfName);
        if(null == provisioningMfValue) {
            logger.debug("Metafield {} not found on user {}", customerProvisioningMfName, userId);
            provisioningMfValue = new StringMetaFieldValue(provisioningMf);
            logger.debug("created metaField {} for user {}", provisioningMfValue, userId);
            customer.getMetaFields().add(provisioningMfValue);
        }
        logger.debug("change customer provisioing status to [{}] from [{}] for user {}", status, provisioningMfValue.getValue(), userId);
        provisioningMfValue.setValue(status.getStatus());
        userDAS.save(user);
    }

    /**
     * Updates order provisioning meta field value
     * @param orderProvisioningMfName
     * @param orderId
     * @param status
     */
    public static void updateOrderProvisioningStatusMf(String orderProvisioningMfName, Integer orderId, OrderProvisioninigStatus status) {
        OrderDAS orderDAS = new OrderDAS();
        OrderDTO order = orderDAS.findNow(orderId);
        Integer entityId = order.getBaseUserByUserId().getCompany().getId();
        MetaField provisioningMf = MetaFieldBL.getFieldByName(entityId, new EntityType[] { EntityType.ORDER }, orderProvisioningMfName);
        if(null == provisioningMf) {
            logger.error("{} not present on order level metafield for entity {}", orderProvisioningMfName, entityId);
            throw new SessionInternalError(orderProvisioningMfName + " not found on order level for entity "+ entityId);
        }
        @SuppressWarnings("unchecked")
        MetaFieldValue<String> provisioningMfValue = order.getMetaField(orderProvisioningMfName);
        if(null == provisioningMfValue) {
            logger.debug("Metafield {} not found on order {}", orderProvisioningMfName, orderId);
            provisioningMfValue = new StringMetaFieldValue(provisioningMf);
            logger.debug("created metaField {} for order {}", provisioningMfValue, orderId);
            order.getMetaFields().add(provisioningMfValue);
        }
        logger.debug("change order provisioing status to [{}] from [{}] for order {}", status, provisioningMfValue.getValue(), orderId);
        provisioningMfValue.setValue(status.getStatus());
        orderDAS.save(order);
    }
}
