package com.sapienter.jbilling.server.company.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.event.ItemUpdatedEvent;
import com.sapienter.jbilling.server.item.event.NewItemEvent;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderStatusDTO;
import com.sapienter.jbilling.server.order.event.IgnitionOrderStatusEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.commons.lang.StringUtils;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;

/**
 * Created by Taimoor Choudhary on 9/18/17.
 */
public class IgnitionWebhooksCommunicationTask extends PluggableTask implements IInternalEventsTask {

    public enum StatusCode{

        SUCCESSFUL("23"),
        FAILED("1000"),
        SUSPENDED("19"),
        LAPSED("20");

        private final String value;

        private StatusCode(String value) {
            this.value = value;
        }

        public boolean equals(String otherValue) {
            return (otherValue == null) ? false : value.equals(otherValue);
        }

        public String toString() {
            return this.value;
        }
    }

    private static final FormatLogger logger = new FormatLogger(IgnitionWebhooksCommunicationTask.class);

    // Subscribed Events
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            NewItemEvent.class,
            ItemUpdatedEvent.class,
            IgnitionPaymentSuccessfulEvent.class,
            IgnitionPaymentFailedEvent.class,
            NewUserStatusEvent.class,
            IgnitionOrderStatusEvent.class

    };

    // region Plugin Parameters

    /* Plugin parameters */
    public static final ParameterDescription PARAMETER_SAS_TOKEN =
            new ParameterDescription("SasToken", true, ParameterDescription.Type.STR, false);

    public static final ParameterDescription PARAMETER_ACCOUNT_NAME =
            new ParameterDescription("AccountName", true, ParameterDescription.Type.STR, false);

    public static final ParameterDescription PARAMETER_ITEM_EVENT_QUEUE =
            new ParameterDescription("ItemEventQueue", false, ParameterDescription.Type.STR, false);

    public static final ParameterDescription PARAMETER_PAYMENT_EVENT_QUEUE =
            new ParameterDescription("PaymentEventQueue", false, ParameterDescription.Type.STR, false);

    public static final ParameterDescription PARAMETER_ORDER_EVENT_QUEUE =
            new ParameterDescription("OrderEventQueue", false, ParameterDescription.Type.STR, false);

    public static final ParameterDescription PARAMETER_CUSTOMER_EVENT_QUEUE =
            new ParameterDescription("CustomerEventQueue", false, ParameterDescription.Type.STR, false);

    //initializer for pluggable parameters
    {
        descriptions.add(PARAMETER_SAS_TOKEN);
        descriptions.add(PARAMETER_ACCOUNT_NAME);
        descriptions.add(PARAMETER_ITEM_EVENT_QUEUE);
        descriptions.add(PARAMETER_PAYMENT_EVENT_QUEUE);
        descriptions.add(PARAMETER_ORDER_EVENT_QUEUE);
        descriptions.add(PARAMETER_CUSTOMER_EVENT_QUEUE);
    }


    public  String getSasToken() {
        return  parameters.get(PARAMETER_SAS_TOKEN.getName());
    }

    public  String getAccountName() {
        return  parameters.get(PARAMETER_ACCOUNT_NAME.getName());
    }

    public  String getItemEventQueueName() {
        return  parameters.get(PARAMETER_ITEM_EVENT_QUEUE.getName());
    }

    public  String getPaymentEventQueueName() {
        return  parameters.get(PARAMETER_PAYMENT_EVENT_QUEUE.getName());
    }

    public  String getOrderEventQueueName() {
        return  parameters.get(PARAMETER_ORDER_EVENT_QUEUE.getName());
    }

    public  String getCustomerEventQueueName() {
        return  parameters.get(PARAMETER_CUSTOMER_EVENT_QUEUE.getName());
    }

    // endregion

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        try{

            if(StringUtils.isEmpty(getSasToken()) && StringUtils.isEmpty(getAccountName())){

                logger.debug("Both SaS Token and Account Name are requried for connectivity.");
                return;
            }

            CloudStorageAccount storageAccount = null;

            // Create storage credentials from SaS Token
            StorageCredentials storageCredentials = new StorageCredentialsSharedAccessSignature(getSasToken());

            // Retrieve cloud storage account from credentials
            storageAccount = new CloudStorageAccount(storageCredentials, true, (String)null, getAccountName());

            // Create the queue client.
            CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

            // Get queue name based on the incoming event
            String queueName = getQueueName(event);

            if(!StringUtils.isEmpty(queueName)) {

                // Retrieve a reference to a queue.
                CloudQueue queue = queueClient.getQueueReference(queueName);

                logger.debug(String.format("Queue: %s, exists: %s.", queueName, queue.exists()));

                // Create the queue if it doesn't already exist.
                if(!queue.exists()) {

                    logger.debug(String.format("Creating Queue: %s.", queue.getName()));

                    queue.createIfNotExists();

                    logger.debug(String.format("Queue: %s, created.", queue.getName()));
                }

                // Get Notification message to be sent
                String notification = getNotificationMessage(event);

                if(!StringUtils.isEmpty(notification)){

                    logger.debug("Generated notification message is: " +  notification);

                    // Create a message and add it to the queue.
                    CloudQueueMessage message = new CloudQueueMessage(notification);
                    queue.addMessage(message);

                    logger.debug("Message added to the Queue: " + queueName);
                }
            }

        }catch (Exception exception){
            logger.error("Exception occurred while trying to send notification events to Ignition Azure Webhooks");
            logger.error(exception);
        }
    }

    // Returns the name of the Queue set in the Plugin Parameters based on incoming Event Type
    private String getQueueName(Event event){

        if (event instanceof NewItemEvent || event instanceof ItemUpdatedEvent){
            return getItemEventQueueName();

        }else if (event instanceof IgnitionPaymentSuccessfulEvent || event instanceof IgnitionPaymentFailedEvent){
            return getPaymentEventQueueName();

        }else if (event instanceof NewUserStatusEvent){
            return getCustomerEventQueueName();

        }else if (event instanceof IgnitionOrderStatusEvent){
            return getOrderEventQueueName();

        }

        return StringUtils.EMPTY;
    }

    /**
     * Return the JSON message as per the incoming Notification Event
     * @param event
     * @return
     */
    private String getNotificationMessage(Event event){

        if (event instanceof NewItemEvent){

            NewItemEvent newItemEvent = (NewItemEvent) event;

            ItemDTOEx itemDTOEx = ItemBL.getItemDTOEx(newItemEvent.getItem());

            return getItemNotificationString(itemDTOEx);

        }else if (event instanceof ItemUpdatedEvent){

            ItemUpdatedEvent updateItemEvent = (ItemUpdatedEvent) event;

            ItemDTOEx itemDTOEx = ItemBL.getItemDTOEx(updateItemEvent.getItem());

            return getItemNotificationString(itemDTOEx);

        }else if (event instanceof IgnitionPaymentSuccessfulEvent){
            IgnitionPaymentSuccessfulEvent paymentSuccessfulEvent = (IgnitionPaymentSuccessfulEvent) event;

            UserDTO userDTO = new UserDAS().findByUserId(paymentSuccessfulEvent.getPayment().getUserId(), this.getEntityId());

            if(userDTO == null){
                logger.debug("Unable to find User for ID: " + paymentSuccessfulEvent.getPayment().getUserId());
                return StringUtils.EMPTY;
            }

            return createNotificationString(this.getEntityId(), StatusCode.SUCCESSFUL.toString(), "OrderItemId", userDTO.getUserName());

        }else if (event instanceof IgnitionPaymentFailedEvent){
            IgnitionPaymentFailedEvent paymentFailedEvent = (IgnitionPaymentFailedEvent) event;

            UserDTO userDTO = new UserDAS().findByUserId(paymentFailedEvent.getPayment().getUserId(), this.getEntityId());

            if(userDTO == null){
                logger.debug("Unable to find User for ID: " + paymentFailedEvent.getPayment().getUserId());
                return StringUtils.EMPTY;
            }

            return createNotificationString(this.getEntityId(), StatusCode.FAILED.toString(), "OrderItemId", userDTO.getUserName());

        }else if (event instanceof NewUserStatusEvent){
            NewUserStatusEvent userStatusEvent = (NewUserStatusEvent) event;

            UserStatusDTO userStatusDTO = new UserStatusDAS().find(userStatusEvent.getNewStatusId());

            if(userStatusDTO.getDescription(Constants.LANGUAGE_ENGLISH_ID).equals(IgnitionConstants.CUSTOMER_ACCOUNT_STATUS_SUSPEND)) {

                return createNotificationString(this.getEntityId(), StatusCode.SUSPENDED.toString(), "OrderItemId", userStatusEvent.getUser().getUserName());
            }

            logger.debug(String.format("Ignoring User Status update event as the customer: %s is not suspended with status id: %s.",
                    userStatusEvent.getUserId(), userStatusEvent.getNewStatusId()));

            return StringUtils.EMPTY;

        }else if (event instanceof IgnitionOrderStatusEvent){
            IgnitionOrderStatusEvent orderStatusEvent = (IgnitionOrderStatusEvent) event;

            OrderStatusDTO orderStatusDTO = new OrderStatusDAS().find(orderStatusEvent.getStatusId());

            if(orderStatusDTO == null){
                logger.debug(String.format("Ignoring Order Status update event as the order: %s with the order status with id: %s not found",
                        orderStatusEvent.getUserId(), orderStatusEvent.getStatusId()));

                return StringUtils.EMPTY;

            }

            if(orderStatusDTO.getDescription(Constants.LANGUAGE_ENGLISH_ID).equals(IgnitionConstants.ORDER_STATUS_LAPSED)) {

                UserDTO userDTO = new UserDAS().findByUserId(orderStatusEvent.getUserId(), this.getEntityId());

                if(userDTO == null){
                    logger.debug("Unable to find User for ID: " + orderStatusEvent.getUserId());
                    return StringUtils.EMPTY;
                }

                return createNotificationString(this.getEntityId(), StatusCode.LAPSED.toString(), "OrderItemId", userDTO.getUserName());
            }

            logger.debug(String.format("Ignoring Order Status update event as the order: %s is not lapsed with status id: %s.",
                    orderStatusEvent.getUserId(), orderStatusEvent.getStatusId()));

            return StringUtils.EMPTY;

        }

        return StringUtils.EMPTY;
    }

    private String createNotificationString(Integer entityId, String StatusCode, String propertyName, String propertyValue){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("{\"Company_Id\"=\"");
        stringBuilder.append(entityId);
        stringBuilder.append("\"");

        stringBuilder.append(",\"");
        stringBuilder.append(propertyName);
        stringBuilder.append("\"=\"");
        stringBuilder.append(propertyValue);
        stringBuilder.append("\"");

        stringBuilder.append(",\"ExtractStatus\"=\"");
        stringBuilder.append(StatusCode);
        stringBuilder.append("\"}");
        return stringBuilder.toString();
    }

    private String getItemNotificationString(ItemDTOEx itemDTOEx){

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(itemDTOEx);
        } catch (JsonProcessingException processingException) {
            logger.error("Exception occured while trying to create a JSON object for: " + itemDTOEx.getId());
            logger.error(processingException);
        }

        return StringUtils.EMPTY;
    }
}
