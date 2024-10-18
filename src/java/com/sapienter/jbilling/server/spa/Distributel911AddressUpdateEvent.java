package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Created by taimoor on 4/12/17.
 */
public class Distributel911AddressUpdateEvent implements Event {


    public RequestType getRequestType() {
        return requestType;
    }

    public Integer getUserId() {
        return userId;
    }

    public enum RequestType{
        ADD_PHONE_NUMBER,
        UPDATE_PHONE_NUMBER,
        CUSTOMER_UPDATE,
        ASSET_UPDATE,
        ORDER_UPDATE,
        DELETE_ON_ORDER_UPDATE,
        DELETE_ON_ASSET_UPDATE,
        CUSTOMER_UPDATE_TASK
    }

    private RequestType requestType;
    private Integer entityId;
    private Integer userId;
    private OrderWS orderWS;

    private String existingPhoneNumber;
    private String updatedPhoneNumber;
    private OrderDTO orderDTO;
    private UserWS userWS;
    private UserDTOEx userDTOEx;
    private List<AssetWS> customerAssets;

    public Distributel911AddressUpdateEvent(Integer entityId) {
        this.entityId = entityId;
    }

    public static Distributel911AddressUpdateEvent createEventForAddingNewPhoneNumber(Integer entityId, Integer userId, String phoneNumber){
        Distributel911AddressUpdateEvent updateEvent = null;
        if(userId != null && !StringUtils.isEmpty(phoneNumber)) {

            updateEvent = new Distributel911AddressUpdateEvent(entityId);
            updateEvent.setEntityId(entityId);
            updateEvent.setUserId(userId);
            updateEvent.setUpdatedPhoneNumber(phoneNumber);
            updateEvent.setRequestType(RequestType.ADD_PHONE_NUMBER);

        }
        return updateEvent;
    }

    public static Distributel911AddressUpdateEvent createEventForAssetUpdateOnOrder(Integer entityId, Integer userId, OrderDTO orderDTO){
        Distributel911AddressUpdateEvent updateEvent = null;
        if(userId != null & orderDTO != null) {

            updateEvent = new Distributel911AddressUpdateEvent(entityId);
            updateEvent.setEntityId(entityId);
            updateEvent.setUserId(userId);
            updateEvent.setOrderDTO(orderDTO);
            updateEvent.setRequestType(RequestType.ORDER_UPDATE);

        }
        return updateEvent;
    }

    public static Distributel911AddressUpdateEvent createEventForAssetUpdate(Integer entityId, Integer userId, String existingPhoneNumber, String updatedPhoneNumber){
        Distributel911AddressUpdateEvent updateEvent = null;

        if(userId != null && (!StringUtils.isEmpty(existingPhoneNumber) || !StringUtils.isEmpty(updatedPhoneNumber))) {
            updateEvent = new Distributel911AddressUpdateEvent(entityId);
            updateEvent.setEntityId(entityId);
            updateEvent.setUserId(userId);
            updateEvent.setExistingPhoneNumber(existingPhoneNumber);
            updateEvent.setUpdatedPhoneNumber(updatedPhoneNumber);
            updateEvent.setRequestType(RequestType.ASSET_UPDATE);
        }
        return updateEvent;
    }

    public static Distributel911AddressUpdateEvent createEventForCustomerUpdate(Integer entityId, Integer userId, UserWS userWS, UserDTOEx userDTOEx, List<AssetWS> userAssets){
        Distributel911AddressUpdateEvent updateEvent = null;

        if(userId != null && userWS !=null & userDTOEx != null) {
            updateEvent = new Distributel911AddressUpdateEvent(entityId);
            updateEvent.setEntityId(entityId);
            updateEvent.setUserId(userId);
            updateEvent.setUserWS(userWS);
            updateEvent.setUserDTOEx(userDTOEx);
            updateEvent.setCustomerAssets(userAssets);
            updateEvent.setRequestType(RequestType.CUSTOMER_UPDATE);
        }
        return updateEvent;
    }

    public static Distributel911AddressUpdateEvent createEventForDeletePhoneNumberOnOrderDelete(Integer entityId, OrderWS orderWS){

        Distributel911AddressUpdateEvent updateEvent = null;
        if(entityId != null & orderWS != null) {

            updateEvent = new Distributel911AddressUpdateEvent(entityId);
            updateEvent.setEntityId(entityId);
            updateEvent.setOrderWS(orderWS);
            updateEvent.setRequestType(RequestType.DELETE_ON_ORDER_UPDATE);

        }
        return updateEvent;
    }

    /**
     * Event to update a customer through the EmergencyAddressUpdateCurrent task 
     * @param entityId entity id
     * @param userId user id
     * @param userAssets assets
     * @return Distributel911AddressUpdateEvent
     */
    public static Distributel911AddressUpdateEvent createEventForCustomerUpdateTask(Integer entityId, Integer userId, List<AssetWS> userAssets){
        Distributel911AddressUpdateEvent updateEvent = null;

        if(userId != null) {
            updateEvent = new Distributel911AddressUpdateEvent(entityId);
            updateEvent.setEntityId(entityId);
            updateEvent.setUserId(userId);
            updateEvent.setCustomerAssets(userAssets);
            updateEvent.setRequestType(RequestType.CUSTOMER_UPDATE_TASK);
        }
        return updateEvent;
    }

    @Override
    public String getName() {
        return "Distributel 911 Address UpdateEvent";
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setExistingPhoneNumber(String existingPhoneNumber) {
        this.existingPhoneNumber = existingPhoneNumber;
    }

    public void setUpdatedPhoneNumber(String updatedPhoneNumber) {
        this.updatedPhoneNumber = updatedPhoneNumber;
    }

    public void setOrderDTO(OrderDTO orderDTO) {
        this.orderDTO = orderDTO;
    }

    public void setUserWS(UserWS userWS) {
        this.userWS = userWS;
    }

    public void setUserDTOEx(UserDTOEx userDTOEx) {
        this.userDTOEx = userDTOEx;
    }

    public void setCustomerAssets(List<AssetWS> customerAssets) {
        this.customerAssets = customerAssets;
    }

    public void setOrderWS(OrderWS orderWS) {
        this.orderWS = orderWS;
    }

    public String getExistingPhoneNumber() {
        return existingPhoneNumber;
    }

    public String getUpdatedPhoneNumber() {
        return updatedPhoneNumber;
    }

    public OrderDTO getOrderDTO() {
        return orderDTO;
    }

    public UserWS getUserWS() {
        return userWS;
    }

    public UserDTOEx getUserDTOEx() {
        return userDTOEx;
    }

    public List<AssetWS> getCustomerAssets() {
        return customerAssets;
    }

    public OrderWS getOrderWS() {
        return orderWS;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Distributel911AddressUpdateEvent{");
        strBuilder.append("requestType=").append(requestType);
        strBuilder.append(", entityId=").append(entityId);
        strBuilder.append(", userId=").append(userId);
        strBuilder.append(", existingPhoneNumber='").append(existingPhoneNumber).append("'");
        strBuilder.append(", updatedPhoneNumber='").append(updatedPhoneNumber).append("'");
        strBuilder.append(", orderDTO=").append(orderDTO);
        strBuilder.append(", userWS=").append(userWS);
        strBuilder.append(", userDTOEx=").append(userDTOEx).append('}');
        return strBuilder.toString();
    }
}
