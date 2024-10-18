package com.sapienter.jbilling.server.ediTransaction.task;

import com.sapienter.jbilling.client.metafield.MetaFieldBindHelper;
import com.sapienter.jbilling.client.suretax.SureTaxBL;
import com.sapienter.jbilling.client.suretax.request.LineItem;
import com.sapienter.jbilling.client.suretax.request.SuretaxRequest;
import com.sapienter.jbilling.client.suretax.responsev2.SuretaxResponseV2;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.BooleanType;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by neeraj on 7/6/16.
 * This plugin finds various types of taxes link to the customer zipcode via suretax and updates the customer tax rate metafield.
 * According to the number of taxes on the customer level, NGESSureTaxWithExemptionCompositionTask plugin calculates the taxes on the invoices.
 *
 * To calculate the available Taxes on the zipcode it creates a dummy item lines and send request to the sure-tax.
 */
public class CalculateCustomerTaxLine  extends AbstractCronTask {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CalculateCustomerTaxLine.class));
    private static final String LINE_ITEM_DISCOUNT_AS_NEW_LINE = "Send line item discount as separate line";
    private static final String SURETAX_REQUEST_URL = "Suretax Request Url";
    private static final String CLIENT_NUMBER = "Client Number";
    private static final String LINE_ITEM_TAX = "Line Item Tax";
    private static final String VALIDATION_KEY = "Validation Key";
    private static final String DATA_YEAR = "Data Year";
    private static final String DATA_MONTH = "Data Month";
    private static final String RESPONSE_GROUP = "Response Group";
    private static final String RESPONSE_TYPE = "Response Type";
    private static final String NUMBER_OF_DECIMAL = "Number of Decimals";
    private static final String ROLLBACK_INVOICE_ON_ERROR = "Rollback Invoice on Error";
    private static final String SECONDARY_ZIP_CODE_EXTN_FIELDNAME = "Secondary Zip Code Extension Field Name";
    private static final String SECONDARY_ZIP_CODE_FIELDNAME = "Secondary Zip Code Field Name";
    private static final String BILLING_ZIP_CODE_FIELDNAME = "Billing Zip Code Field Name";
    private static final String BILLING_ZIP_CODE_Extension_FIELDNAME = "Billing Zip Code Extension Field Name";
    private static final String REGULATORY_CODE_FIELDNAME = "Regulatory Code Field Name";
    private static final String SALES_TYPE_CODE_FIELDNAME = "Sales Type Code Field Name";
    private static final String TAX_EXEMPTION_CODE_FIELDNAME = "Tax Exemption Code Field Name";
    private static final String TRANSACTION_TYPE_CODE_FIELDNAME = "Transaction Type Code Field Name";
    private static final String UNIT_TYPE_CODE_FIELDNAME = "UnitType Field Name";

    protected static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    PlanDAS planDas=new PlanDAS();
    IWebServicesSessionBean webServicesSessionBean= Context.getBean(Context.Name.WEB_SERVICES_SESSION);;

    // Initializer for pluggable params
    {
        descriptions.add(new ParameterDescription(SURETAX_REQUEST_URL, true, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(CLIENT_NUMBER, true, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(LINE_ITEM_TAX, false, ParameterDescription.Type.BOOLEAN));
        descriptions.add(new ParameterDescription(VALIDATION_KEY, true, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(DATA_YEAR, false, ParameterDescription.Type.INT));
        descriptions.add(new ParameterDescription(DATA_MONTH, false, ParameterDescription.Type.INT));
        descriptions.add(new ParameterDescription(RESPONSE_GROUP, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(RESPONSE_TYPE, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(NUMBER_OF_DECIMAL, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(ROLLBACK_INVOICE_ON_ERROR, false, ParameterDescription.Type.BOOLEAN));
        descriptions.add(new ParameterDescription(SECONDARY_ZIP_CODE_EXTN_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(SECONDARY_ZIP_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(BILLING_ZIP_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(BILLING_ZIP_CODE_Extension_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(REGULATORY_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(SALES_TYPE_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(TAX_EXEMPTION_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(TRANSACTION_TYPE_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(UNIT_TYPE_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(LINE_ITEM_DISCOUNT_AS_NEW_LINE, false, ParameterDescription.Type.BOOLEAN));
    }

    public CalculateCustomerTaxLine() {
        setUseTransaction(true);
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        _init(context);

        MetaField taxMetaField= MetaFieldBL.getFieldByName(getEntityId(), new EntityType[]{EntityType.CUSTOMER}, FileConstants.CUSTOMER_TAX_METAFIELD);

        LOG.debug("Tax metafield name : "+taxMetaField);
        if(taxMetaField==null){
            return;
        }

        // finding all customers having "Calculate Tax Manually" true
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.sqlRestriction("boolean_value =  ?", true, BooleanType.INSTANCE));
        conjunction.add(Restrictions.eq("metaField.name", FileConstants.CUSTOMER_CALCULATE_TAX_MANUALLY));
        UserDAS userDAS=new UserDAS();
        List<UserDTO> users=userDAS.findUsers(conjunction, getEntityId());

        for(UserDTO user:users){
            LOG.debug("User : "+user );

            //if customer spacific tax is defined then did not override it.
            //TODO waiting for information
            MetaFieldValue<String> taxMetafield = user.getCustomer().getMetaField(FileConstants.CUSTOMER_TAX_METAFIELD);
            if(taxMetafield!=null &&  StringUtils.isNotBlank(taxMetafield.getValue())){
                Map<String, String> taxes=MetaFieldBindHelper.convertJSONStringToMap(taxMetafield.getValue());
                if(taxes.keySet().size()>0){
                    continue;
                }

            }

            SuretaxRequest suretaxRequest=buildRequest(user);
            if (suretaxRequest==null){
                continue;
            }

            String suretaxRequestUrl = getParameter(SURETAX_REQUEST_URL, "");
            SureTaxBL sureTaxBL = new SureTaxBL(suretaxRequestUrl);

            SuretaxResponseV2 response = sureTaxBL.calculateLineItemTax(suretaxRequest);
            LOG.debug("Responce : "+response);
            //if there is connection issue with sure tax than try after sometime.
            if(response==null)
                throw new SessionInternalError("Issue on connection with suretax");

            String jsonString=CalculateTaxLine.convertResponseToJSONString(response);

            user.getCustomer().setMetaField(taxMetaField, jsonString);
            UserBL userBL=new UserBL(user);
            UserWS userWS=userBL.getUserWS();
            LOG.debug("updating customer tax : "+userWS);
            webServicesSessionBean.updateUserWithCompanyId(userWS, getEntityId());
        }
    }

    private LineItem constructBaseLineItem(UserDTO user) {

        LineItem lineItem = new LineItem();
        lineItem.setLineNumber("0");
        lineItem.setInvoiceNumber("0");
        lineItem.setCustomerNumber("0");

       Map<String, MetaFieldValue> customerMetaFieldValues = getMetaFieldValues(user, new LinkedList<String>() {{
            add(FileConstants.ZIP_CODE);
            add(FileConstants.PLAN);
        }});

        MetaFieldValue zipcodeValue=customerMetaFieldValues.get(FileConstants.ZIP_CODE);
        LOG.debug("Zipcode : "+zipcodeValue);
        if(zipcodeValue==null || zipcodeValue.getValue()==null){
            return null;
        }

        String zipCode=(String) zipcodeValue.getValue();

        lineItem.setZipcode(zipCode.substring(0, 5));
        lineItem.setPlus4(zipCode.substring(5));
        lineItem.setTransDate(dateFormatter.format(TimezoneHelper.companyCurrentLDT(user.getCompany().getId())));
        lineItem.setRevenue(100);
        lineItem.setUnits(100);
        lineItem.setUnitType("03");
        lineItem.setSeconds(100);
        lineItem.setSeconds(100);
        lineItem.setTaxIncludedCode("0");
        lineItem.setTaxSitusRule("05");


        MetaFieldValue<String> planMetaField=customerMetaFieldValues.get(FileConstants.PLAN);
        LOG.debug("Plan : "+ planMetaField);
        if(planMetaField==null || planMetaField.getValue()==null){
            return null;
        }

        String planCode=planMetaField.getValue();
        LOG.debug("Plan Code : "+planCode);

        ItemDTO planItem=getPlanItem(planCode);
        LOG.debug("Plan : "+planItem);
        if (planItem==null){
            return null;
        }

        String transTypeCode=(String) planItem.getMetaField(getParameter(
                TRANSACTION_TYPE_CODE_FIELDNAME, "Transaction Type Code")).getValue();
        String regulateryCode=(String) planItem.getMetaField(getParameter(
                REGULATORY_CODE_FIELDNAME, "Regulatory Code")).getValue();


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

    @Override
    public String getTaskName() {
        return "Calculate customer tax line, entity Id: " + this.getEntityId() + ", task Id:" + getTaskId();

    }

    private SuretaxRequest buildRequest(UserDTO user){

        LineItem lineItem=constructBaseLineItem(user);
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



    protected Map<String, MetaFieldValue> getMetaFieldValues(UserDTO user, List<String> metaFieldNames) {
        Map<String, MetaFieldValue> metaFieldValues = new HashMap<String, MetaFieldValue>();
        Set<CustomerAccountInfoTypeMetaField> list = user.getCustomer().getCustomerAccountInfoTypeMetaFields();
        for (CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField : list) {
            String aitName=customerAccountInfoTypeMetaField.getAccountInfoType().getName();
            //we have two aits having zipcode metafield. So for calculate tax use  "Customer Information" or "Business Information" ait's zipcode value.
            List<String> aits=Arrays.asList(FileConstants.CUSTOMER_INFORMATION_AIT, FileConstants.BUSINESS_INFORMATION_AIT, FileConstants.ACCOUNT_INFORMATION_AIT);
            if(aits.contains(aitName)){
                MetaFieldValue metaField = customerAccountInfoTypeMetaField.getMetaFieldValue();
                if (metaFieldNames.contains(metaField.getField().getName())) {
                    if (metaFieldNames.remove(metaField.getField().getName()))
                        metaFieldValues.put(metaField.getField().getName(), metaField);
                }
            }
        }
        return metaFieldValues;
    }

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
}
