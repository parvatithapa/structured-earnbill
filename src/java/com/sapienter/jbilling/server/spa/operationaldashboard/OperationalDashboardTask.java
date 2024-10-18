package com.sapienter.jbilling.server.spa.operationaldashboard;

import com.sapienter.jbilling.CustomerNoteDAS;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.event.AssetAddedToOrderEvent;
import com.sapienter.jbilling.server.item.event.AssetUpdatedEvent;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.CustomerNoteDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Matías Cabezas on 04/10/17.
 */
public class OperationalDashboardTask extends PluggableTask implements IInternalEventsTask {

    private static final ParameterDescription OPERATIONAL_DASHBOARD_USERNAME = new ParameterDescription("Acanac Biller User", true, ParameterDescription.Type.STR, false);
    private static final ParameterDescription OPERATIONAL_DASHBOARD_PASSWORD = new ParameterDescription("Acanac Biller Password", true, ParameterDescription.Type.STR, true);
    private static final ParameterDescription OPERATIONAL_DASHBOARD_NOTIFICATION_EMAIL = new ParameterDescription("Email Address For Exceptions", true, ParameterDescription.Type.STR, false);
    private static final ParameterDescription OPERATIONAL_DASHBOARD_NOTIFICATION_ID = new ParameterDescription("Exeception Notification Id", true, ParameterDescription.Type.STR, false);
    private static final String NOTE_TITLE = "OPERATIONAL DASHBOARD RESPONSE CODE";
    private static final String SUCCESS_OPERATION = "SUCCESS";
    private static final FormatLogger LOG = new FormatLogger(OperationalDashboardTask.class);
    private static final String DASHBOARD_REQUEST = "dashboard_request";
    private static final String DASHBOARD_RESPONSE = "dashboard_response";
    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
            AssetUpdatedEvent.class,
            AssetAddedToOrderEvent.class
    };

    {
        descriptions.add(OPERATIONAL_DASHBOARD_USERNAME);
        descriptions.add(OPERATIONAL_DASHBOARD_PASSWORD);
        descriptions.add(OPERATIONAL_DASHBOARD_NOTIFICATION_EMAIL);
        descriptions.add(OPERATIONAL_DASHBOARD_NOTIFICATION_ID);
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof AssetUpdatedEvent) {
            /* Update asset*/
            AssetUpdatedEvent assetUpdatedEvent = (AssetUpdatedEvent) event;
            if (assetUpdatedEvent.getOldMetaFieldValues() != null && assetUpdatedEvent.getAsset().getOrderLine() != null) {
                OperationalDashboardRequestGenerator requestGenerator = new OperationalDashboardRequestGenerator();
                OrderDTO orderDTO = assetUpdatedEvent.getAsset().getOrderLine().getPurchaseOrder();
                String assetType = assetUpdatedEvent.getAsset().getItem().findItemTypeWithAssetManagement().getDescription();
                String requestURL = requestGenerator.getAssetTypeURL(assetType, orderDTO.getUser());

                Map<String, String> parameters = new HashMap<>();
            /* Prefix NEW*/
                requestGenerator.generateCustomerParameters(OperationalDashboardPrefix.NEW.getValue(), parameters, orderDTO.getUser(), OperationalDashboardMode.UPDATE, assetUpdatedEvent.getAsset());
                requestGenerator.generateAssetMetafieldParameters(parameters, assetUpdatedEvent.getAsset(), OperationalDashboardPrefix.NEW.getValue());
            /* Prefix OLD*/
                requestGenerator.generateCustomerParameters(OperationalDashboardPrefix.OLD.getValue(), parameters, orderDTO.getUser(), OperationalDashboardMode.UPDATE, assetUpdatedEvent.getAsset());
                requestGenerator.generateAssetMetafieldOldParameters(parameters, assetUpdatedEvent.getOldMetaFieldValues(), assetUpdatedEvent.getAsset().getItem().findItemTypeWithAssetManagement().getAssetMetaFields());
            
            /* Send the requestURL to the OperationalDashboardClient*/
                sendClientRequest(requestURL, parameters, orderDTO);
            }

        } else if (event instanceof AssetAddedToOrderEvent) {
            /* Create asset */
            AssetAddedToOrderEvent assetAddedToOrderEvent = (AssetAddedToOrderEvent) event;
            OperationalDashboardRequestGenerator requestGenerator = new OperationalDashboardRequestGenerator();
            OrderDTO orderDTO = assetAddedToOrderEvent.getAsset().getOrderLine().getPurchaseOrder();
            String assetType = assetAddedToOrderEvent.getAsset().getItem().findItemTypeWithAssetManagement().getDescription();
            String requestURL = requestGenerator.getAssetTypeURL(assetType, assetAddedToOrderEvent.getAssignedTo());

            Map<String, String> parameters = new HashMap<>();

            requestGenerator.generateCustomerParameters(OperationalDashboardPrefix.NONE.getValue(), parameters, assetAddedToOrderEvent.getAssignedTo(), OperationalDashboardMode.CREATE, assetAddedToOrderEvent.getAsset());
            requestGenerator.generateAssetMetafieldParameters(parameters, assetAddedToOrderEvent.getAsset().getId(), OperationalDashboardPrefix.NONE.getValue());

        /* Send the requestURL to the OperationalDashboardClient*/
            sendClientRequest(requestURL, parameters, orderDTO);
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private void sendClientRequest(String requestURL, Map<String, String> requestParameters, OrderDTO orderDTO) {
        OperationalDashboardClient client = new OperationalDashboardClient(requestURL, requestParameters, parameters.get(OPERATIONAL_DASHBOARD_USERNAME.getName()), parameters.get(OPERATIONAL_DASHBOARD_PASSWORD.getName()));
        String response = client.sendRequest();
        CustomerNoteDTO customerNoteDTO = new CustomerNoteDTO();
        customerNoteDTO.setNoteTitle(NOTE_TITLE);
        customerNoteDTO.setUser(new UserDAS().find(orderDTO.getUserId()));
        customerNoteDTO.setCompany(orderDTO.getUser().getCompany());
        customerNoteDTO.setCustomer(orderDTO.getUser().getCustomer());
        customerNoteDTO.setNoteContent(response);
        customerNoteDTO.setCreationTime(TimezoneHelper.serverCurrentDate());
        CustomerNoteDAS customerNoteDAS = new CustomerNoteDAS();
        customerNoteDAS.save(customerNoteDTO);
        checkResponseStatus(response, client.getRequest(), orderDTO.getUser());
    }

    private void checkResponseStatus(String response, String request, UserDTO userDTO) {
        if (!response.contains(SUCCESS_OPERATION)) {
            /* Send the notification email if the operation is not succesful*/
            MessageDTO message = null;
            if (StringUtils.isNotEmpty(parameters.get(OPERATIONAL_DASHBOARD_NOTIFICATION_ID.getName()))) {
                try {
                    message = new NotificationBL().getCustomNotificationMessage(
                            Integer.valueOf(parameters.get(OPERATIONAL_DASHBOARD_NOTIFICATION_ID.getName())),
                            userDTO.getEntity().getId(),
                            userDTO.getUserId(),
                            userDTO.getLanguageIdField()
                    );
                } catch (NotificationNotFoundException e) {
                    LOG.info(String.format("Custom notification id: %s does not exist for the user id %s ",
                            parameters.get(OPERATIONAL_DASHBOARD_NOTIFICATION_ID.getName()), userDTO.getUserId()));
                }
            }
            if (message == null) {
                return;
            }
            LOG.info("A notification e-mail was sent to %s in order to notify a non succesful operation", parameters.get(OPERATIONAL_DASHBOARD_NOTIFICATION_EMAIL.getName()));
            INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            message.addParameter(MessageDTO.PARAMETER_SPECIFIC_EMAIL_ADDRESS, parameters.get(OPERATIONAL_DASHBOARD_NOTIFICATION_EMAIL.getName()));
            message.addParameter(DASHBOARD_REQUEST, request);
            message.addParameter(DASHBOARD_RESPONSE, response);
            notificationSession.notify(userDTO.getUserId(), message);
        }
    }
}
