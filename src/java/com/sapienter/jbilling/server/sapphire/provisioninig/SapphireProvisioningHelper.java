package com.sapienter.jbilling.server.sapphire.provisioninig;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
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

        List<OrderDTO> orders =  new OrderDAS().findRecurringOrders(userDTO.getId(),
                new OrderStatusFlag[] { OrderStatusFlag.INVOICE, OrderStatusFlag.NOT_INVOICE });
        logger.debug("order fetched {} for user {}", orders, userDTO.getId());
        Integer orderId = !orders.isEmpty() ? orders.get(0).getId() : 0;
        List<SapphireDeviceWS> deviceList = new ArrayList<>();
        orders.stream()
        .forEach(order ->
        order.getLines().stream()
        .filter(Objects::nonNull)
        .forEach(line -> {
            if(line.getItem().isPlan()) {
                provisioningRequest.setPlanId(line.getItem().getPlans().iterator().next().getId());
            }
            line.getAssets().stream().forEach(asset -> deviceList.add(mapAssetToDevice(asset)));
        }));

        provisioningRequest.setId(System.currentTimeMillis());
        provisioningRequest.setOfficeId(Integer.toString(accountTypeId));
        provisioningRequest.setClientId(userDTO.getId());
        provisioningRequest.setFirstName(firstName);
        provisioningRequest.setLastName(lastName);
        provisioningRequest.setEmail(email);
        provisioningRequest.setOrderId(orderId);
        provisioningRequest.setRequestType(requestType);
        provisioningRequest.setHardwareId("NONE");
        provisioningRequest.setDevices(deviceList.stream().toArray(SapphireDeviceWS[] :: new));
        return provisioningRequest;
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
