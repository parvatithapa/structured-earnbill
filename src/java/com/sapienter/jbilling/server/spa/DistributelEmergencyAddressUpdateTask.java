package com.sapienter.jbilling.server.spa;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.spa.Distributel911AddressUpdateEvent.RequestType;
import org.apache.log4j.Logger;

/**
 * Created by taimoor on 4/12/17.
 */
public class DistributelEmergencyAddressUpdateTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger LOG = Logger.getLogger(DistributelEmergencyAddressUpdateTask.class);

    private static final Class<Event>[] events = new Class[]{
            Distributel911AddressUpdateEvent.class,
    };

    @Override
    public void process(Event event) throws PluggableTaskException {

        if(event instanceof Distributel911AddressUpdateEvent) {
            Distributel911AddressUpdateEvent emergencyAddressUpdateEvent = ((Distributel911AddressUpdateEvent) event);
            RequestType requestType = emergencyAddressUpdateEvent.getRequestType();

            LOG.debug("Incoming Emergency address update request: " + emergencyAddressUpdateEvent);

            DistributelEmergencyAddressUpdateManager addressUpdateManager = new DistributelEmergencyAddressUpdateManager(null);

            switch (requestType){
                case ADD_PHONE_NUMBER:
                    addressUpdateManager.addNewPhoneNumber(emergencyAddressUpdateEvent.getUserId(), emergencyAddressUpdateEvent.getUpdatedPhoneNumber());
                    break;

                case ASSET_UPDATE:
                    addressUpdateManager.addOrUpdatePhoneNumber(emergencyAddressUpdateEvent.getEntityId(), emergencyAddressUpdateEvent.getUserId(),
                            emergencyAddressUpdateEvent.getUpdatedPhoneNumber(), emergencyAddressUpdateEvent.getExistingPhoneNumber());
                    break;

                case CUSTOMER_UPDATE:
                case CUSTOMER_UPDATE_TASK:
                    boolean isUpdated = addressUpdateManager.updateEmergencyAddress(emergencyAddressUpdateEvent.getUserId(),
                            emergencyAddressUpdateEvent.getUserWS(), emergencyAddressUpdateEvent.getCustomerAssets());

                    if (emergencyAddressUpdateEvent.getUserWS() != null) {
                        addressUpdateManager.updateCustomerServerResponseMetaFields(emergencyAddressUpdateEvent.getUserWS(),
                                emergencyAddressUpdateEvent.getUserDTOEx(), isUpdated);
                    } else {
                        addressUpdateManager.updateCustomerMetaFields(emergencyAddressUpdateEvent.getUserId(),
                                isUpdated, addressUpdateManager.getErrorCodes());
                    }
                    break;

                case ORDER_UPDATE:
                    addressUpdateManager.updateEmergencyAddressOnOrderUpdate(emergencyAddressUpdateEvent.getUserId(), emergencyAddressUpdateEvent.getOrderDTO());
                    break;

                case DELETE_ON_ORDER_UPDATE:
                    addressUpdateManager.deleteEmergencyAddressOnOrderDelete(emergencyAddressUpdateEvent.getOrderWS());
                    break;
            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}
