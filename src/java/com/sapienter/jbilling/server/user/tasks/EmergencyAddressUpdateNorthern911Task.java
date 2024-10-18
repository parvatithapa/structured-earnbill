package com.sapienter.jbilling.server.user.tasks;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.spa.DistributelEmergencyAddressUpdateManager;
import com.sapienter.jbilling.server.spa.Northern911FailureEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.event.EmergencyAddressUpdateEvent;
import com.sapienter.jbilling.server.user.event.EmergencyAddressUpdateEvent.RequestType;
import northern911.api.service.Customer;
import northern911.api.service.Error;
import northern911.api.service.IService;
import northern911.api.service.N911Response;
import northern911.api.service.ObjectFactory;
import northern911.api.service.Service;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wajeeha on 4/9/17.
 */
public class EmergencyAddressUpdateNorthern911Task extends PluggableTask implements IInternalEventsTask{

    private static final Logger LOG = Logger.getLogger(EmergencyAddressUpdateNorthern911Task.class);

    private String vendorCode;
    private String passCode;
    private String soapURL;

    public static final ParameterDescription PARAMETER_VENDOR_CODE =
            new ParameterDescription("vendor_code", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_PASS_CODE =
            new ParameterDescription("pass_code", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_SOAP_URL =
            new ParameterDescription("soap_URL", true, ParameterDescription.Type.STR);


    //initializer for pluggable params
    {
        descriptions.add(PARAMETER_VENDOR_CODE);
        descriptions.add(PARAMETER_PASS_CODE);
        descriptions.add(PARAMETER_SOAP_URL);
    }

    private static final Class<Event>[] events = new Class[]{
            EmergencyAddressUpdateEvent.class,
    };

    public String getVendorCode() throws PluggableTaskException {
        if (vendorCode == null) {
            try {
                vendorCode = (String) parameters.get(PARAMETER_VENDOR_CODE.getName());
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured vendor code must be a String!", e);
            }
        }
        return vendorCode;
    }

    public String getPassCode() throws PluggableTaskException {
        if (passCode == null) {
            try {
                passCode = (String) parameters.get(PARAMETER_PASS_CODE.getName());
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured pass code must be a String!", e);
            }
        }
        return passCode;
    }

    public String getSoapURL() throws PluggableTaskException {
        if (soapURL == null) {
            try {
                soapURL = (String) parameters.get(PARAMETER_SOAP_URL.getName());
            } catch (NumberFormatException e) {
                throw new PluggableTaskException("Configured URL must be a String!", e);
            }
        }
        return soapURL;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof EmergencyAddressUpdateEvent) {
            EmergencyAddressUpdateEvent emergencyAddressUpdateEvent = ((EmergencyAddressUpdateEvent) event);
            RequestType requestType = emergencyAddressUpdateEvent.getRequestType();
            ContactDTO contactDTO = emergencyAddressUpdateEvent.getContactDto();

            LOG.debug("Incoming 911 address update request: " + emergencyAddressUpdateEvent);

            Service service = null;

            try {
                service = new Service(new URL(getSoapURL()));
            } catch (MalformedURLException e) {
                throw new PluggableTaskException("Unable to create service for given URL!", e);
            }

            // Will be used to send requests to Northern911
            IService iservice = service.getBasicHttpBindingIService();


            // Create new HASH code unique for each day (GMT Time)
            String hash = null;
            try {
                hash = createHash(getVendorCode(), getPassCode());
            } catch (NoSuchAlgorithmException e) {
                throw new PluggableTaskException("Unable to generate HASH code", e);
            }

            N911Response response = null;

            switch (requestType){
                case UPDATE:
                    response = iservice.addorUpdateCustomer(createAddUpdateRequest(contactDTO), hash);
                    break;

                case ADD:
                    response = iservice.addorUpdateCustomer(createAddUpdateRequest(contactDTO), hash);
                    break;

                case DELETE:
                    response = iservice.deleteCustomer(getVendorCode(), contactDTO.getPhoneNumber(), hash);
                    break;

                case QUERY:
                    response = iservice.queryCustomer(getVendorCode(), contactDTO.getPhoneNumber(), hash);
                    break;

                case VERIFY:
                    response = iservice.verifyCustomer(createAddUpdateRequest(contactDTO), hash);
                    break;
            }

            //Add/Update Accepted.
            if (response != null && response.isAccepted()) {
                emergencyAddressUpdateEvent.setUpdated(true);
                emergencyAddressUpdateEvent.setErrorResponse("");
                LOG.debug("Add/Update Accepted.");
            } else {
                emergencyAddressUpdateEvent.setUpdated(false);
                LOG.error("Add/Update Not Accepted.");

                String errorResponseCodes = "";
                List<String> errors = new ArrayList<>();

                if (response != null) {
                    for (Error error : response.getErrors().getValue().getError()) {
                        Integer errorCode = error.getErrorCode();
                        String errorMsg = error.getErrorMessage().getValue();

                        LOG.error("Error Code: " + errorCode);
                        LOG.error("Error Message: " + errorMsg);

                        errorResponseCodes += (errorCode + " ");
                        errors.add(errorMsg);
                    }
                } else {
                    LOG.error("Response null.");
                    errorResponseCodes = "Problem obtaining Response. Response null.";
                    errors.add("Problem obtaining Response.");
                }
                emergencyAddressUpdateEvent.setErrorResponse(errorResponseCodes);

                Northern911FailureEvent northern911FailureEvent = new Northern911FailureEvent(emergencyAddressUpdateEvent.getUserId(), this.getEntityId(), errors);
                EventManager.process(northern911FailureEvent);
            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /**
     * Create a new Northern911 Customer to be used in AddOrUpdate request
     * @param contactDTO
     * @return
     * @throws PluggableTaskException
     */
    private Customer createAddUpdateRequest(ContactDTO contactDTO) throws PluggableTaskException {

        ObjectFactory objectFactory = new ObjectFactory();

        Customer customer = new Customer();

        String[] street = contactDTO.getAddress1().split(DistributelEmergencyAddressUpdateManager.STREET_ADDRESS_SEPARATOR);

        String streetNumber="";
        if(street.length > 0){
            streetNumber = street[0];
        }

        String streetName="";
        if(street.length > 1){
            streetName = street[1];
        }

        //Append street type with street name
        if (street.length > 2 && StringUtils.length(street[2])>0) {
            streetName += " " + street[2];
        }

        //Append street direction with street name
        if (street.length > 3 && StringUtils.length(street[3])>0) {
            streetName += " " + street[3];
        }

        String aptSuite="";
        if(street.length > 4){
            aptSuite = street[4];
        }

        customer.setVENDORCODE(objectFactory.createCustomerVENDORCODE(getVendorCode()));
        customer.setPHONENUMBER(objectFactory.createCustomerPHONENUMBER(contactDTO.getPhoneNumber()));
        customer.setLASTNAME(objectFactory.createCustomerLASTNAME(contactDTO.getLastName()));
        customer.setFIRSTNAME(objectFactory.createCustomerFIRSTNAME(contactDTO.getFirstName()));
        customer.setSTREETNUMBER(objectFactory.createCustomerSTREETNUMBER(streetNumber));
        customer.setSTREETNAME(objectFactory.createCustomerSTREETNAME(streetName));
        customer.setSUITEAPT(objectFactory.createCustomerSUITEAPT(aptSuite));
        customer.setCITY(objectFactory.createCustomerCITY(contactDTO.getCity()));
        customer.setPROVINCESTATE(objectFactory.createCustomerPROVINCESTATE(contactDTO.getStateProvince()));
        customer.setPOSTALCODEZIP(objectFactory.createCustomerPOSTALCODEZIP(contactDTO.getPostalCode()));
        customer.setOTHERADDRESSINFO(objectFactory.createCustomerOTHERADDRESSINFO(contactDTO.getAddress2()));
        customer.setENHANCEDCAPABLE(objectFactory.createCustomerENHANCEDCAPABLE("Y"));

        return customer;
    }

    private static DateTimeFormatter GmtDateFormat =
            new DateTimeFormatterBuilder().appendPattern("yyyyMMdd").toFormatter().withZone(ZoneOffset.UTC);

    private String GetGMTDateString() {
        return GmtDateFormat.format(LocalDate.now());
    }

    /**
     * Returns MD5 hash with given parameters and current GMT time
     * @param vendorCode
     * @param passCode
     * @return
     */
    private String createHash(String vendorCode, String passCode ) throws NoSuchAlgorithmException{

        byte[] data = (vendorCode + passCode + GetGMTDateString()).getBytes(StandardCharsets.US_ASCII);

        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(data);
        return javax.xml.bind.DatatypeConverter.printHexBinary(digest.digest());
    }
}
