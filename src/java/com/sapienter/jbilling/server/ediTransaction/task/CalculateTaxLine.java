package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper;
import com.sapienter.jbilling.client.suretax.SureTaxBL;
import com.sapienter.jbilling.client.suretax.request.LineItem;
import com.sapienter.jbilling.client.suretax.request.SuretaxRequest;
import com.sapienter.jbilling.client.suretax.response.IResponseHeader;
import com.sapienter.jbilling.client.suretax.responsev2.SuretaxResponseV2;
import com.sapienter.jbilling.client.suretax.responsev2.TaxItem;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDTO;
import com.sapienter.jbilling.server.customerEnrollment.event.ValidateEnrollmentEvent;
import com.sapienter.jbilling.server.ediTransaction.EDITransactionHelper;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.task.AbstractSureTaxRequestTask;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * This internal event plugin calculate the taxline using the sure tax plugin with dummy data. The calculated tax line
 * is displayed on the enrollment creation time so that we can define the taxes on the enrollment time.
 */

public class CalculateTaxLine extends AbstractSureTaxRequestTask
        implements IInternalEventsTask {



    protected static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private static final Logger LOG = Logger.getLogger(CalculateTaxLine.class);

    private static final Class<Event> events[] = new Class[]{
            ValidateEnrollmentEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        if(!(event instanceof ValidateEnrollmentEvent)){
            return;
        }

        MetaField taxMetaField= MetaFieldBL.getFieldByName(getEntityId(), new EntityType[]{EntityType.ENROLLMENT}, FileConstants.CUSTOMER_TAX_METAFIELD);

        LOG.debug("Tax metafield name : "+taxMetaField);
        if(taxMetaField==null){
            return;
        }

        CustomizedEntity customizedEntity=((ValidateEnrollmentEvent) event).getEnrollmentDTO();
        MetaFieldValue<Boolean> calculateTaxManually= customizedEntity.getMetaField(FileConstants.CUSTOMER_CALCULATE_TAX_MANUALLY);

        if(calculateTaxManually==null || calculateTaxManually.getValue()==null || !calculateTaxManually.getValue()){
            return;
        }

        MetaFieldValue<String> taxMetaFieldValue= customizedEntity.getMetaField(taxMetaField.getName());
        // if tax is already calculated then did not calculate tax again.
        LOG.debug(" taxMetaFieldValue "+taxMetaFieldValue);
        if(taxMetaFieldValue!=null && taxMetaFieldValue.getValue()!=null){
            Map<String,String> jsonMap= MetaFieldBindHelper.convertJSONStringToMap(taxMetaFieldValue.getValue());
            if(jsonMap.keySet().size()>0){
                return;
            }
        }

        SuretaxRequest suretaxRequest=buildRequest(customizedEntity);
        String suretaxRequestUrl = getParameter(SURETAX_REQUEST_URL, "");
        SureTaxBL sureTaxBL = new SureTaxBL(suretaxRequestUrl);

        SuretaxResponseV2 response = sureTaxBL.calculateLineItemTax(suretaxRequest);
        LOG.debug("Response : "+response);

        if(response==null)
            throw new SessionInternalError("Issue on connection with suretax");

        String jsonResult= convertResponseToJSONString(response);
        customizedEntity.setMetaField(taxMetaField, jsonResult);
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    /* This method create line item which will be added the sure tax request to calculate the taxes.
        We find the zicode and plan from the entity's metafield and create Line item.
      * @params customizedEntity will be the customer or enrollment
       *
       * @return LineItem
       * @thwow SessionInternalError
       * */
    private LineItem constructBaseLineItem(CustomizedEntity entity) {

        LineItem lineItem = new LineItem();
        lineItem.setLineNumber("0");
        lineItem.setInvoiceNumber("0");
        lineItem.setCustomerNumber("0");

        String taxExemptionCodeFieldname = getParameter(
                TAX_EXEMPTION_CODE_FIELDNAME, "Tax exemption code");

        CustomerEnrollmentDTO customerEnrollmentDTO = (CustomerEnrollmentDTO) entity;
        Map<String, MetaFieldValue> metaFieldValues = EDITransactionHelper.getMetaFieldValues(customerEnrollmentDTO, new LinkedList<String>() {{
            add(FileConstants.ZIP_CODE);
            add(taxExemptionCodeFieldname);
        }});

        MetaFieldValue<String> zipcodeValue=metaFieldValues.get(FileConstants.ZIP_CODE);
        LOG.debug("Zipcode : "+zipcodeValue);
        if(zipcodeValue==null || zipcodeValue.getValue()==null){
            throw new SessionInternalError("Zipcode cannot be blank", new String[]{"Zipcode cannot be blank"});
        }

        String zipCode=(String) zipcodeValue.getValue();

        lineItem.setZipcode(zipCode.substring(0, 5));
        lineItem.setPlus4(zipCode.substring(5));
        lineItem.setTransDate(dateFormatter.format(TimezoneHelper.companyCurrentLDT(customerEnrollmentDTO.getCompany().getId())));
        lineItem.setRevenue(100);
        lineItem.setUnits(100);
        lineItem.setSeconds(100);
        lineItem.setSeconds(100);
        lineItem.setTaxIncludedCode("0");
        lineItem.setTaxSitusRule("05");

        MetaFieldValue<String> planMetaField=entity.getMetaField(FileConstants.PLAN);
        LOG.debug("Plan : "+ planMetaField);
        if(planMetaField==null || planMetaField.getValue()==null){
            throw new SessionInternalError("Please select a plan", new String[]{"Please select a plan"});
        }

        String planCode=planMetaField.getValue();
        LOG.debug("Plan Code : "+planCode);

        ItemDTO planItem=getPlanItem(planCode);
        LOG.debug("Plan : "+planItem);
        if (planItem==null){
            throw new SessionInternalError("No plan exit for plan code "+planCode, new String[]{"No plan exit for plan code "+planCode});
        }

        String transTypeCode=(String) planItem.getMetaField(getParameter(
                TRANSACTION_TYPE_CODE_FIELDNAME, "Transaction Type Code")).getValue();
        String regulateryCode=(String) planItem.getMetaField(getParameter(
                REGULATORY_CODE_FIELDNAME, "Regulatory Code")).getValue();

        String unitTypeFieldname = getParameter(
                UNIT_TYPE_CODE_FIELDNAME, "Unit Type");

        MetaFieldValue<String> unitType = planItem.getMetaField(unitTypeFieldname);
        if (unitType == null || unitType.getValue() == null
                || unitType.getValue().isEmpty()) {
            lineItem.setUnitType("00");
        }else{
            lineItem.setUnitType(unitType.getValue());
        }

        MetaFieldValue<String> taxExamptionCodeMetaFieldValue=metaFieldValues.get(taxExemptionCodeFieldname);
        LOG.debug("Customer tax examption "+taxExamptionCodeMetaFieldValue);
        if(taxExamptionCodeMetaFieldValue==null || taxExamptionCodeMetaFieldValue.getValue()==null){
            taxExamptionCodeMetaFieldValue  = planItem.getMetaField(taxExemptionCodeFieldname);
        }
        List<String> taxExemptionCodeList=new ArrayList<>();

        if(taxExamptionCodeMetaFieldValue==null || taxExamptionCodeMetaFieldValue.getValue()==null){
            taxExemptionCodeList.add("00");
        }else {
            taxExemptionCodeList.add(taxExamptionCodeMetaFieldValue.getValue());
        }

        lineItem.setTaxExemptionCodeList(taxExemptionCodeList);

        lineItem.setTransTypeCode(transTypeCode);
        lineItem.setSalesTypeCode("R");
        lineItem.setRegulatoryCode(regulateryCode);
        lineItem.setBillToNumber("");
        lineItem.setOrigNumber("");
        lineItem.setTermNumber("");
        lineItem.setP2PPlus4("");
        lineItem.setP2PZipcode("");
        lineItem.setTaxExemptionCodeList(new ArrayList<>());
        return lineItem;
    }

    /* This method create Sure tax request by using the plugin parameter and entity meta-field value
      * @params customizedEntity will be the customer or enrollment
       *
       * @return sureTaxRequest
       * @throw SessionInternalError
       * */
    private SuretaxRequest buildRequest(CustomizedEntity customizedEntity) throws SessionInternalError{

        LineItem lineItem=constructBaseLineItem(customizedEntity);
        LOG.debug("Line Item : "+lineItem);

        if(lineItem==null){
            return null;
        }

        SuretaxRequest suretaxRequest=new SuretaxRequest();

        List<LineItem> itemList = new ArrayList<>();
        itemList.add(lineItem);
        suretaxRequest.setItemList(itemList);

        String clientNumber = getParameter(CLIENT_NUMBER, "");
        String validationKey = getParameter(VALIDATION_KEY, "");
        String responseGroup = getParameter(RESPONSE_GROUP, "03");
        /* SureTax takes this as a Quote and doesn’t store anything on their side. */
        String responseType = getParameter(RESPONSE_TYPE, "D");
        String numberOfDecimals = getParameter(NUMBER_OF_DECIMAL, "2");

        Integer dataYear = null;
        Integer dataMonth = null;
        try {
            dataYear = getParameter(DATA_YEAR, Calendar.getInstance().get(Calendar.YEAR));
            dataMonth = getParameter(DATA_MONTH, Calendar.getInstance().get(Calendar.MONTH) + 1);
        } catch (PluggableTaskException e) {
            LOG.debug("Exception while retrieving Data Year or Data Month");
        }

        suretaxRequest.setClientNumber(clientNumber);
        suretaxRequest.setValidationKey(validationKey);
        String uniqueTrackingCode = SureTaxBL.nextTransactionId();
        suretaxRequest.setClientTracking(uniqueTrackingCode);
        suretaxRequest.setDataMonth(dataMonth.toString());
        suretaxRequest.setDataYear(dataYear.toString());
        suretaxRequest.setIndustryExemption("");
        suretaxRequest.setBusinessUnit("");
        suretaxRequest.setResponseGroup(responseGroup);
        suretaxRequest.setResponseType(responseType + numberOfDecimals);
        suretaxRequest.setReturnFileCode("0");
        suretaxRequest.setTotalRevenue(100);
        return suretaxRequest;
    }


    /*This method return the plan's  plan-item */
    private ItemDTO getPlanItem(String planCode){

        ItemDTO item=new ItemDAS().findItemByInternalNumber(planCode, getEntityId());
        if (item==null){
            return null;
        }

        PlanDTO planDTO = item.getPlans().iterator().next();
        if (planDTO==null){
            return null;
        }
        return planDTO.getPlanItems().iterator().next().getItem();
    }

    /* Converting sure tax response to JSON String.  */
    public static String convertResponseToJSONString(SuretaxResponseV2 response){
        HashMap<String, String> taxesMap=new HashMap<>();

        if(response.successful.equals("Y")){
            for (com.sapienter.jbilling.client.suretax.responsev2.Group group : response.groupList) {
                group.taxList.stream().forEach((TaxItem taxItem)->{
                    taxesMap.put(taxItem.taxTypeDesc, taxItem.taxAmount);
                });
            }
        }
        return new JSONObject(taxesMap).toString();
    }

    public void handleResponse(SuretaxRequest request, IResponseHeader response){}
}
