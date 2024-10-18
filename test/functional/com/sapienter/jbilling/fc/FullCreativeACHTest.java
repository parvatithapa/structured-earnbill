/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.fc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

@Test(groups = "external-system", testName = "FullCreativeACHTest")
public class FullCreativeACHTest {

    private static final Logger logger = LoggerFactory.getLogger(FullCreativeACHTest.class);
    private JbillingAPI api;
    private final static int ACH_PM_ID = 14;
    private final static String ACH_MF_ROUTING_NUMBER = "ach.routing.number";
    private final static String ACH_MF_BANK_NAME = "ach.bank.name";
    private final static String ACH_MF_CUSTOMER_NAME = "ach.customer.name";
    private final static String ACH_MF_ACCOUNT_NUMBER = "ach.account.number";
    private final static String ACH_MF_ACCOUNT_TYPE = "ach.account.type";
    public final static int ORDER_PERIOD = 2;
    public final static Integer ACCOUNT_TYPE = 60103;
    private final static String ACH_ROUTING_NUMBER = "111111118";
    private final static String ACH_ACCOUNT_NUMBER = "1234567801";
    private static Integer payflowExternalACHTaskPluginId;
    private static Integer saveACHPluginId;
    private static Integer basicItemManagerPlugInId;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FAKE_TASK_ID1);
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FAKE_TASK_ID2);
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_FILTER_TASK_ID);
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_ROUTER_CCF_TASK_ID);
        updateProcessingOrderOfPlugin(api, FullCreativeTestConstants.PAYMENT_ROUTER_CUREENCY_TASK_ID);
        
        PluggableTaskWS payflowExternalACHTaskPlugin = new PluggableTaskWS();
        payflowExternalACHTaskPlugin.setProcessingOrder(30);
        payflowExternalACHTaskPlugin.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.PAY_FLOW_EXTERNAL_ACH_TASK_CLASS_NAME).getId());
        Hashtable<String, String> parameters = new Hashtable<String, String>();
        parameters.put("PayflowEnvironment", "sandbox");
        parameters.put("PayflowVendor", "anctpaypal");
        parameters.put("PayflowUserId", "clientservices");
        parameters.put("PayflowPassword", "jimmy123");
        payflowExternalACHTaskPlugin.setParameters(parameters);
        payflowExternalACHTaskPlugin.setOwningEntityId(api.getCallerCompanyId());
        payflowExternalACHTaskPluginId = api.createPlugin(payflowExternalACHTaskPlugin);
        
        logger.debug("payflowExternalACHTaskPluginId : {}", payflowExternalACHTaskPluginId);
        PluggableTaskWS saveACHExternalPlugin = new PluggableTaskWS();
        
        saveACHExternalPlugin.setOwningEntityId(api.getCallerCompanyId());
        saveACHExternalPlugin.setProcessingOrder(340);
        saveACHExternalPlugin.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.SAVE_ACH_EXTERNALLY_TASK_CLASS_NAME).getId());
        parameters.clear();
        
        parameters = new Hashtable<String, String>();
        parameters.put("externalSavingPluginId",payflowExternalACHTaskPluginId.toString());
        parameters.put("obscureOnFail", "1");
        parameters.put("contactType", "14");
        
        saveACHExternalPlugin.setParameters(parameters);
        saveACHPluginId = api.createPlugin(saveACHExternalPlugin);
        logger.debug("Save ACH plugin id : {}", saveACHPluginId);

        basicItemManagerPlugInId = FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID;
        FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);

        AccountTypeWS accountTypeWS = api.getAccountType(ACCOUNT_TYPE);
        Integer[] paymentMethodIds = {1,2,14};
        accountTypeWS.setPaymentMethodTypeIds(paymentMethodIds);
        api.updateAccountType(accountTypeWS);
    }

    @Test
    public void test01paymentTest() throws Exception {

        Calendar calendar = Calendar.getInstance();
        UserWS user = createUser(calendar.getTime());
        UserWS newUser=api.getUserWS(user.getId());

        PaymentInformationWS ach = null;
        for(PaymentInformationWS instrument : newUser.getPaymentInstruments()) {
            if(instrument.getPaymentMethodTypeId() == ACH_PM_ID) {
                ach = instrument;
            }
        }
        logger.debug("Ach is: {}", ach);

                MetaFieldValueWS[] metaFields = ach.getMetaFields();
                char[] achAccountNumber = null;
                char[] achGatewayKey=null;
                for(MetaFieldValueWS metafield : metaFields){
                        if( (metafield.getFieldName()).equals("ach.account.number") ){
                                achAccountNumber = metafield.getCharValue();
                    }else if((metafield.getFieldName()).equals("ach.gateway.key")){
                                achGatewayKey = metafield.getCharValue();
                    }
        }

        logger.debug("achAccountNumber is: {}", achAccountNumber != null ? new String(achAccountNumber) : null);
        logger.debug("achGatewayKey  is: {}", achGatewayKey != null ? new String(achGatewayKey) : null);
        assertTrue("ACH Account Number should be obscure",( achAccountNumber != null && new String(achAccountNumber).startsWith("******")));
        assertNotNull("ACH gateway Key meta field shold be saved  after update.", achGatewayKey);

        logger.debug("Testing ACH payment");
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal("1.00"));
        payment.setIsRefund(new Integer(0));
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setResultId(Constants.RESULT_ENTERED);
        payment.setCurrencyId(new Integer(1));
        payment.setUserId(newUser.getUserId());
        payment.setPaymentNotes("Notes");
        payment.setPaymentPeriod(new Integer(1));

        PaymentAuthorizationDTOEx result = api.processPayment(payment, null);
        assertEquals("ACH payment should pass",
                Constants.RESULT_OK, api.getPayment(result.getPaymentId()).getResultId());

        /**
         * Commented refund process here. ACH refund process require User permission from Gateway account.
         */
       /* PaymentWS paymentRefund = new PaymentWS();
        paymentRefund.setAmount(new BigDecimal("1.00"));
        paymentRefund.setIsRefund(new Integer(1));
        paymentRefund.setMethodId(14);
        paymentRefund.setPaymentDate(Calendar.getInstance().getTime());
        paymentRefund.setResultId(Constants.RESULT_ENTERED);
        paymentRefund.setCurrencyId(new Integer(1));
        paymentRefund.setUserId(newUser.getUserId());
        paymentRefund.setPaymentNotes("Notes");
        paymentRefund.setPaymentPeriod(new Integer(1));
        paymentRefund.getPaymentInstruments().add(ach);
        paymentRefund.setPaymentId(api.getLatestPayment(newUser.getId()).getId());

        PaymentAuthorizationDTOEx resultRefund = api.processPayment(paymentRefund, null);
        assertEquals("ACH payment refund should pass",
                Constants.RESULT_OK, api.getPayment(resultRefund.getPaymentId()).getResultId());

        api.updateUser(newUser);

        ach = null;
        for(PaymentInformationWS instrument : newUser.getPaymentInstruments()) {
            if(instrument.getPaymentMethodTypeId() == ACH_PM_ID) {
                ach = instrument;
            }
        }
        logger.debug("Updated user Ach is: {}", ach);

        metaFields = ach.getMetaFields();
        achAccountNumber = null;
        achGatewayKey=null;
        for(MetaFieldValueWS metafield : metaFields){
            if( (metafield.getFieldName()).equals("ach.account.number") ){
                achAccountNumber = metafield.getCharValue();
            }else  if( (metafield.getFieldName()).equals("ach.gateway.key") ){
                achGatewayKey = metafield.getCharValue();
            }
        }*/

        logger.debug("Updated User achAccountNumber is: {}", achAccountNumber != null ? new String(achAccountNumber) : null);
        logger.debug("Updated User achGatewayKey is: {}", achGatewayKey != null ? new String(achGatewayKey) : null);
        assertTrue(" Updated user ACH Account Number should be obscure",( achAccountNumber != null && new String(achAccountNumber).startsWith("******")));
        assertNotNull("ACH gateway Key meta field shold be saved  after update also.", achGatewayKey);

        /**
         * test coverage for bugfix/JB-2273
         */
        newUser.getPaymentInstruments().clear();
        api.updateUser(newUser);
        newUser = api.getUserWS(newUser.getId());
        logger.debug("newUser.getPaymentInstruments() : {}", newUser.getPaymentInstruments().isEmpty());
        assertTrue("Payment Information of user Should be Empty ", newUser.getPaymentInstruments().isEmpty());

    }

    public static UserWS createUser(Date nextInvoiceDate) throws JbillingAPIException,
	IOException {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS newUser = new UserWS();
        List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
        newUser.setUserId(0);
        newUser.setUserName("testUserName-"
        		+ Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("P@ssword12");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(ACCOUNT_TYPE);
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(null);
        newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
        newUser.setInvoiceChild(new Boolean(false));
        
        logger.debug("User properties set");
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("Country");
        metaField1.getMetaField().setDataType(DataType.STRING);
        metaField1.setValue("CA");
        metaFieldValues.add(metaField1);
        
        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("State/Province");
        metaField2.getMetaField().setDataType(DataType.STRING);
        metaField2.setValue("OR");
        metaFieldValues.add(metaField2);
        
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("First Name");
        metaField3.getMetaField().setDataType(DataType.STRING);
        metaField3.setValue("Frodo");
        metaFieldValues.add(metaField3);
        
        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("Last Name");
        metaField4.getMetaField().setDataType(DataType.STRING);
        metaField4.setValue("Baggins");
        metaFieldValues.add(metaField4);
        
        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("Address 1");
        metaField5.getMetaField().setDataType(DataType.STRING);
        metaField5.setValue("Baggins");
        metaFieldValues.add(metaField5);
        
        MetaFieldValueWS metaField6 = new MetaFieldValueWS();
        metaField6.setFieldName("City");
        metaField6.getMetaField().setDataType(DataType.STRING);
        metaField6.setValue("Baggins");
        metaFieldValues.add(metaField6);
        
        MetaFieldValueWS metaField7 = new MetaFieldValueWS();
        metaField7.setFieldName("Email Address");
        metaField7.getMetaField().setDataType(DataType.STRING);
        metaField7.setValue(newUser.getUserName() + "@shire.com");
        metaFieldValues.add(metaField7);
        
        MetaFieldValueWS metaField8 = new MetaFieldValueWS();
        metaField8.setFieldName("Postal Code");
        metaField8.getMetaField().setDataType(DataType.STRING);
        metaField8.setValue("K0");
        metaFieldValues.add(metaField8);
        
        MetaFieldValueWS metaField9 = new MetaFieldValueWS();
        metaField9.setFieldName("COUNTRY_CODE");
        metaField9.getMetaField().setDataType(DataType.STRING);
        metaField9.setValue("CA");
        metaField9.setGroupId(14);
        metaFieldValues.add(metaField9);
        
        MetaFieldValueWS metaField10 = new MetaFieldValueWS();
        metaField10.setFieldName("STATE_PROVINCE");
        metaField10.getMetaField().setDataType(DataType.STRING);
        metaField10.setValue("OR");
        metaField10.setGroupId(14);
        metaFieldValues.add(metaField10);
        
        MetaFieldValueWS metaField11 = new MetaFieldValueWS();
        metaField11.setFieldName("ORGANIZATION");
        metaField11.getMetaField().setDataType(DataType.STRING);
        metaField11.setValue("Frodo");
        metaField11.setGroupId(14);
        metaFieldValues.add(metaField11);
        
        MetaFieldValueWS metaField12 = new MetaFieldValueWS();
        metaField12.setFieldName("LAST_NAME");
        metaField12.getMetaField().setDataType(DataType.STRING);
        metaField12.setValue("Baggins");
        metaField12.setGroupId(14);
        metaFieldValues.add(metaField12);
        
        MetaFieldValueWS metaField13 = new MetaFieldValueWS();
        metaField13.setFieldName("ADDRESS1");
        metaField13.getMetaField().setDataType(DataType.STRING);
        metaField13.setValue("Baggins");
        metaField13.setGroupId(14);
        metaFieldValues.add(metaField13);
        
        MetaFieldValueWS metaField14 = new MetaFieldValueWS();
        metaField14.setFieldName("CITY");
        metaField14.getMetaField().setDataType(DataType.STRING);
        metaField14.setValue("Baggins");
        metaField14.setGroupId(14);
        metaFieldValues.add(metaField14);
        
        MetaFieldValueWS metaField15 = new MetaFieldValueWS();
        metaField15.setFieldName("BILLING_EMAIL");
        metaField15.getMetaField().setDataType(DataType.STRING);
        metaField15.setValue(newUser.getUserName() + "@shire.com");
        metaField15.setGroupId(14);
        metaFieldValues.add(metaField15);
        
        MetaFieldValueWS metaField16 = new MetaFieldValueWS();
        metaField16.setFieldName("POSTAL_CODE");
        metaField16.getMetaField().setDataType(DataType.STRING);
        metaField16.setValue("K0");
        metaField16.setGroupId(14);
        metaFieldValues.add(metaField16);
        
        newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
        
        logger.debug("Meta field values set");
        
        // add a ACH 
        newUser.getPaymentInstruments().add(createACH("jBiller","Shire Financial Bank", ACH_ROUTING_NUMBER, ACH_ACCOUNT_NUMBER, Integer.valueOf(1)));
        
        logger.debug("Creating user ...");
        MainSubscriptionWS billing = new MainSubscriptionWS();
        billing.setPeriodId(2);
        billing.setNextInvoiceDayOfPeriod(1);
        newUser.setMainSubscription(billing);
        newUser.setNextInvoiceDate(nextInvoiceDate);
        newUser.setUserId(api.createUser(newUser));
        logger.debug("User created with id: {}", newUser.getUserId());
        
        return newUser;
    }
    
    public static PaymentInformationWS createACH(String customerName,
                                                 String bankName, String routingNumber, String accountNumber, Integer accountType) {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(ACH_PM_ID);
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_ACH);
        cc.setProcessingOrder(new Integer(2));

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, ACH_MF_ROUTING_NUMBER, false, true,
                DataType.CHAR, 1, routingNumber.toCharArray());
        addMetaField(metaFields, ACH_MF_CUSTOMER_NAME, false, true,
                DataType.STRING, 2, customerName);
        addMetaField(metaFields, ACH_MF_ACCOUNT_NUMBER, false, true,
                DataType.CHAR, 3, accountNumber.toCharArray());
        addMetaField(metaFields, ACH_MF_BANK_NAME, false, true,
                DataType.STRING, 4, bankName);
        addMetaField(metaFields, ACH_MF_ACCOUNT_TYPE, false, true,
                DataType.ENUMERATION, 5, accountType == 1 ? Constants.ACH_CHECKING : Constants.ACH_SAVING);

        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }
    private static void addMetaField(List<MetaFieldValueWS> metaFields,
                                     String fieldName, boolean disabled, boolean mandatory,
                                     DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.getMetaField().setDisabled(disabled);
        ws.getMetaField().setMandatory(mandatory);
        ws.getMetaField().setDataType(dataType);
        ws.getMetaField().setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws);
    }

    private void updateProcessingOrderOfPlugin(JbillingAPI api, Integer pluginId) {
   	PluggableTaskWS plugIn = api.getPluginWS(pluginId);
   	logger.debug("Plugin id : {}", pluginId);
   	plugIn.setProcessingOrder(plugIn.getProcessingOrder()+30);
   	plugIn.setParameters(new Hashtable<String, String>(plugIn.getParameters()));
   	api.updatePlugin(plugIn);
   	logger.debug("Updated Plugin id : {}", pluginId);
       }
    
    @AfterClass
    public void cleanUp(){
	api.deletePlugin(payflowExternalACHTaskPluginId);
	api.deletePlugin(saveACHPluginId);
	FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);

    }
}
