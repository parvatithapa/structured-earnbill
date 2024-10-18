package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.client.suretax.SuretaxClient;
import com.sapienter.jbilling.client.suretax.request.SureAddressRequest;
import com.sapienter.jbilling.client.suretax.response.SureAddressResponse;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO;
import com.sapienter.jbilling.server.customerEnrollment.helper.CustomerEnrollmentFileGenerationHelper;
import com.sapienter.jbilling.server.invoice.db.SuretaxTransactionLogDAS;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.SureAddressEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.AccountTypeDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;


/**
 * Created by neeraj on 28/10/15.
 */
public class SureAddressTask extends PluggableTask implements IInternalEventsTask {

    private static final ParameterDescription SURETAX_REQUEST_URL =
            new ParameterDescription("Sure Address Request Url", true, ParameterDescription.Type.STR);
    private static final ParameterDescription CLIENT_NUMBER =
            new ParameterDescription("Client Number", true, ParameterDescription.Type.STR);
    private static final ParameterDescription VALIDATION_KEY =
            new ParameterDescription("Validation Key", true, ParameterDescription.Type.STR);

    {
        descriptions.add(SURETAX_REQUEST_URL);
        descriptions.add(CLIENT_NUMBER);
        descriptions.add(VALIDATION_KEY);

    }

    private static final Class<Event> events[] = new Class[]{
            SureAddressEvent.class
    };

    private static final FormatLogger LOG = new FormatLogger(
            Logger.getLogger(SureTaxCompositionTask.class));

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    private IWebServicesSessionBean webServicesSessionSpringBean;
    private SuretaxTransactionLogDAS suretaxTransactionLogDAS = null;
    private List<MetaFieldValue> zipMetaFieldValues=new ArrayList<>();
    private CustomizedEntity customizedEntity;
    private List<AccountInformationTypeDTO> accountInformationTypes=new ArrayList<>();

    @Override
    public void process(Event event) throws PluggableTaskException {


        if (!(event instanceof SureAddressEvent)) {
            return;
        }

        webServicesSessionSpringBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        suretaxTransactionLogDAS = new SuretaxTransactionLogDAS();


        String clientNumber = getParameter(CLIENT_NUMBER.getName(), "");
        String validationKey = getParameter(VALIDATION_KEY.getName(), "");
        String url = getParameter(SURETAX_REQUEST_URL.getName(), "");


        /* code for calculating the sure address */
        SureAddressEvent sureAddressEvent = (SureAddressEvent) event;

        customizedEntity = sureAddressEvent.getEntity();
        if(!(customizedEntity instanceof CustomerEnrollmentDTO)){
            return;
        }

        CustomerEnrollmentDTO customerEnrollmentDTO=(CustomerEnrollmentDTO)customizedEntity;
        AccountTypeDTO accountTypeDTO=customerEnrollmentDTO.getAccountType();

        LOG.debug("Account type : "+accountTypeDTO.getDescription());
        List<String> errorMessages=new ArrayList<>();
        List<SureAddressRequest> sureAddressRequests=new ArrayList<>();
        for(AccountInformationTypeDTO accountInformationTypeDTO:accountTypeDTO.getInformationTypes()){

            // finding is AIT have address information. If it has address information then validate and create sureAddressRequst
            Boolean aitHaveAddressInfo=accountInformationTypeDTO.getMetaFields().stream().filter((MetaField mf) -> mf.getFieldUsage()==MetaFieldType.ADDRESS1 ).count()==1;
            if(!aitHaveAddressInfo){
                continue;
            }

            accountInformationTypes.add(accountInformationTypeDTO);
            SureAddressRequest sureAddressRequest=new SureAddressRequest();
            sureAddressRequest.clientNumber=clientNumber;
            sureAddressRequest.validationKey=validationKey;

            try{
                //validating address information of each AIT and creating Sure Address request.
                validateAddress(accountInformationTypeDTO, sureAddressRequest);
                sureAddressRequests.add(sureAddressRequest);
            }catch (SessionInternalError e){
                errorMessages.addAll(Arrays.asList(e.getErrorMessages()));
            }
        }

        //throwing address validation error
        if(errorMessages.size()>0){
            throw new SessionInternalError(errorMessages.toArray(new String[errorMessages.size()]));
        }

        int i=0;

        LOG.debug("Total request : "+sureAddressRequests.size());

        for(SureAddressRequest sureAddressRequest:sureAddressRequests){
            //sending request to SureAddress API
            SureAddressResponse sureAddressResponse = new SuretaxClient().getAddressResponse(sureAddressRequest,
                    url);

            LOG.debug("SureAddress request : "+sureAddressRequest+" and SureAddress response : "+sureAddressResponse);
            try{
                //validating sure address response
                validatingSureAddressResponse(sureAddressResponse, sureAddressRequest, accountInformationTypes.get(i));
            }catch (SessionInternalError e){
                errorMessages.add(e.getErrorMessages()[0]);
            }
            i++;
        }
        // throwing validation responce error message
        if(errorMessages.size()>0){
            throw new SessionInternalError(errorMessages.toArray(new String[errorMessages.size()]));
        }
    }

    /* This method validate addresses for each AIT */
    private void validateAddress(AccountInformationTypeDTO accountInformationTypeDTO, SureAddressRequest sureAddressRequest){

        List<String> errorMessages=new ArrayList<>();
        Map<MetaFieldType, Field> addressFieldMap = new HashMap();
        try {
            addressFieldMap.put(MetaFieldType.ADDRESS1, sureAddressRequest.getClass().getField("address1"));
            addressFieldMap.put(MetaFieldType.CITY, sureAddressRequest.getClass().getField("city"));
            addressFieldMap.put(MetaFieldType.STATE_PROVINCE, sureAddressRequest.getClass().getField("state"));
            addressFieldMap.put(MetaFieldType.POSTAL_CODE, sureAddressRequest.getClass().getField("zipcode"));

            assignValueToAddressField(accountInformationTypeDTO, sureAddressRequest, addressFieldMap);
            addressFieldMap.remove(MetaFieldType.POSTAL_CODE);
            for (Field field : addressFieldMap.values()) {
                if (StringUtils.isBlank((String) field.get(sureAddressRequest))) {
                    errorMessages.add("MetaFieldValue,value,sure.address.value.cannot.be.null," + accountInformationTypeDTO.getName() + ", " + field.getName().toUpperCase());
                }
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        if(errorMessages.size()>0){
            throw new SessionInternalError(errorMessages.toArray(new String[errorMessages.size()]));
        }

        if(StringUtils.isNotBlank(sureAddressRequest.state)){
            sureAddressRequest.state=CustomerEnrollmentFileGenerationHelper.USState.getAbbreviationForState(sureAddressRequest.state);;
        }
    }

    private void assignValueToAddressField(AccountInformationTypeDTO accountInformationTypeDTO, SureAddressRequest sureAddressRequest, Map<MetaFieldType, Field> addressFieldMap) {
        for (MetaFieldType fieldType : addressFieldMap.keySet()) {
            accountInformationTypeDTO.getMetaFields().stream().filter((MetaField mf) -> (mf.getFieldUsage() == fieldType)).forEach((MetaField mf) -> {
                customizedEntity.getMetaFields().stream().filter((MetaFieldValue mfv) -> mfv.getField().getId() == mf.getId()).forEach((MetaFieldValue mfv) -> {
                    try {
                        addressFieldMap.get(fieldType).set(sureAddressRequest, mfv.getValue() != null ? (String) mfv.getValue() : "");
                    } catch (IllegalAccessException e) {
                        throw new SessionInternalError(e);
                    }
                    if (fieldType == MetaFieldType.POSTAL_CODE) zipMetaFieldValues.add(mfv);
                });
            });
        }
    }

    private void validatingSureAddressResponse(SureAddressResponse sureAddressResponse, SureAddressRequest sureAddressRequest, AccountInformationTypeDTO ait){

        if(sureAddressResponse==null){
            throw new SessionInternalError("Not able to connect with SureAddress API", new String[]{"Not able to connect with SureAddress API"});
        }

        if (sureAddressResponse.getMessage() != null) {
            throw new SessionInternalError(sureAddressResponse.getMessage(), new String[] { ait.getName()+" :No search result found for given address '"+sureAddressRequest.address1+"'and city '"+sureAddressRequest.city+"' and state'"+sureAddressRequest.state+"'. Please provide valid address"});
        }

        if(!sureAddressResponse.getAddress1().toUpperCase().equals(sureAddressRequest.address1.toUpperCase())){
            throw new SessionInternalError("Provided address '"+sureAddressRequest.address1+"' is not equal to the searched address'"+sureAddressResponse.getAddress1()+"'", new String[]{ait.getName()+" :Address found '"+sureAddressResponse.getAddress1()+"' instead of '"+sureAddressRequest.address1+"'"});
        }

        if(!sureAddressResponse.getCity().toUpperCase().equals(sureAddressRequest.city.toUpperCase())){
            throw new SessionInternalError("Provided city ("+sureAddressRequest.city+") is not equal to the searched city("+sureAddressResponse.getCity()+")", new String[]{ait.getName()+" :City found ("+sureAddressResponse.getCity()+") instead of("+sureAddressRequest.city+")"});
        }

        if(!sureAddressResponse.getState().toUpperCase().equals(sureAddressRequest.state.toUpperCase())){
            throw new SessionInternalError("Provided state ("+sureAddressRequest.state+") is not equal to the searched state ("+sureAddressResponse.getState()+")", new String[]{ait.getName()+" :State found '"+sureAddressResponse.getState() +"' instead of '"+sureAddressRequest.state+"'"});
        }

        String zipPlusFourCode=sureAddressResponse.getZipCode() + "" + sureAddressResponse.getZIPPlus4();

        Optional zipcodeMetafieldOptional =ait.getMetaFields().stream().filter((MetaField mf)-> (mf.getFieldUsage()==MetaFieldType.POSTAL_CODE)).findFirst();
        MetaField zipcodeMetafield = zipcodeMetafieldOptional.isPresent() ? (MetaField) zipcodeMetafieldOptional.get() : null;
        LOG.debug("Zipcode Metafield : "+zipcodeMetafield);
        if(zipcodeMetafield==null){
            throw new SessionInternalError("AIT should have zipcode metafield", new String[]{ait.getName()+" should have zipcode field"});
        }

        Optional zipMetaFieldValueOptional=zipMetaFieldValues.stream().filter((MetaFieldValue mfv) ->mfv.getField().getId()==zipcodeMetafield.getId()).findFirst();
        MetaFieldValue zipMetaFieldValue = zipMetaFieldValueOptional.isPresent() ? (MetaFieldValue) zipMetaFieldValueOptional.get() : null;
        LOG.debug("Zipcode MetafieldValue : "+zipMetaFieldValue);
        if(zipMetaFieldValue==null){
            throw new SessionInternalError("AIT should have zipcode metafield", new String[]{ait.getName()+" should have zipcode field"});
        }

        zipMetaFieldValue.setValue(zipPlusFourCode);
        customizedEntity.setMetaField(zipMetaFieldValue, null);

        String message="";
        if(StringUtils.isBlank(sureAddressRequest.zipcode)){
            message += ait.getName()+" :The valid zip code for provided address is "+zipPlusFourCode +".;";
        }

        if((StringUtils.isNotBlank(sureAddressRequest.zipcode) && !zipPlusFourCode.equals(sureAddressRequest.zipcode))){
            message += ait.getName()+" :The valid zip code for provided address is "+zipPlusFourCode+" instead of "+sureAddressRequest.zipcode+". Please confirm.;";
        }

        if(customizedEntity instanceof CustomerEnrollmentDTO && !message.equals("")){
            CustomerEnrollmentDTO customerEnrollmentDTO=(CustomerEnrollmentDTO)customizedEntity;
            customerEnrollmentDTO.setMessage(customerEnrollmentDTO.getMessage()==null?message:customerEnrollmentDTO.getMessage().concat(message));;
        }
    }
}