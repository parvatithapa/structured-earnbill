package com.sapienter.jbilling.fc;

import static org.testng.AssertJUnit.assertEquals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

@Test(groups = { "fullcreative" }, testName = "FullCreativeAccountPinMetaFieldTest")
public class FullCreativeAccountPinMetaFieldTest {

	private static final Logger logger = LoggerFactory.getLogger(FullCreativeAccountPinMetaFieldTest.class);
	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	private static final String CUSTOMER_METAFIELD_ACC_PIN = "Account PIN";
	
	@Test
	public void test001MetaField() {
		try {
			JbillingAPI api = JbillingAPIFactory.getAPI();
			String pin = ""+Calendar.getInstance().getTimeInMillis();
			UserWS user = FullCreativeAccountPinMetaFieldTest.createUser(true, null, 1, api, pin);
			UserWS newUser = api.getUserByCustomerMetaField(pin, CUSTOMER_METAFIELD_ACC_PIN);
			assertEquals(newUser.getId(), user.getId());
		}catch(Exception e){
			logger.error("Error retrieving user by customer metafield", e);
		}
	}
	
	public static UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId,JbillingAPI api,String pin){

		// Create - This passes the password validation routine.
		 
		UserWS newUser = new UserWS();
		newUser.setUserId(0); // it is validated
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("123qweASD$%^");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(Integer.valueOf(60103));
		newUser.setParentId(parentId); // this parent exists
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(currencyId);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));
		
		MetaFieldValueWS metaField = new MetaFieldValueWS();
		metaField.setFieldName("Email Address");
		metaField.setValue(newUser.getUserName()+"test@jbilling.com");
		
		MetaFieldValueWS metaField1 = new MetaFieldValueWS();
		metaField1.setFieldName("Last Name");
		metaField1.setValue("Gandaf");
		
		MetaFieldValueWS metaField2 = new MetaFieldValueWS();
		metaField2.setFieldName("State/Province");
		metaField2.setValue("Test- Province");
		
		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("Country");
		metaField3.setValue("USA");
		
		MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("First Name");
		metaField4.setValue("Foo");
		
		MetaFieldValueWS metaField5 = new MetaFieldValueWS();
		metaField5.setFieldName("Address 1");
		metaField5.setValue("Test - Address");
		
		MetaFieldValueWS metaField6 = new MetaFieldValueWS();
		metaField6.setFieldName("Account PIN");
		metaField6.setValue(pin);
		
		MetaFieldValueWS metaField7 = new MetaFieldValueWS();
		metaField7.setFieldName("Postal Code");
		metaField7.setValue("1234");
		
		MetaFieldValueWS metaField8 = new MetaFieldValueWS();
		metaField8.setFieldName("City");
		metaField8.setValue("N-Y");

        MetaFieldValueWS metaField9 = new MetaFieldValueWS();
        metaField9.setFieldName("COUNTRY_CODE");
        metaField9.getMetaField().setDataType(DataType.STRING);
        metaField9.setValue("CA");

        MetaFieldValueWS metaField10 = new MetaFieldValueWS();
        metaField10.setFieldName("STATE_PROVINCE");
        metaField10.getMetaField().setDataType(DataType.STRING);
        metaField10.setValue("OR");


        MetaFieldValueWS metaField11 = new MetaFieldValueWS();
        metaField11.setFieldName("ORGANIZATION");
        metaField11.getMetaField().setDataType(DataType.STRING);
        metaField11.setValue("Frodo");

        MetaFieldValueWS metaField12 = new MetaFieldValueWS();
        metaField12.setFieldName("LAST_NAME");
        metaField12.getMetaField().setDataType(DataType.STRING);
        metaField12.setValue("Baggins");

        MetaFieldValueWS metaField13 = new MetaFieldValueWS();
        metaField13.setFieldName("ADDRESS1");
        metaField13.setValue("Baggins");
        metaField13.getMetaField().setDataType(DataType.STRING);

        MetaFieldValueWS metaField14 = new MetaFieldValueWS();
        metaField14.setFieldName("CITY");
        metaField14.setValue("Baggins");
        metaField14.getMetaField().setDataType(DataType.STRING);

        MetaFieldValueWS metaField15 = new MetaFieldValueWS();
        metaField15.setFieldName("BILLING_EMAIL");
        metaField15.setValue(newUser.getUserName() + "@shire.com");
        metaField15.getMetaField().setDataType(DataType.STRING);

        MetaFieldValueWS metaField16 = new MetaFieldValueWS();
        metaField16.setFieldName("POSTAL_CODE");
        metaField16.setValue("K0");
        metaField16.getMetaField().setDataType(DataType.STRING);

		newUser.setMetaFields(new MetaFieldValueWS[] {  metaField1, metaField2, metaField3, metaField4, metaField5, metaField6, metaField7, metaField8,
                metaField9, metaField10, metaField11, metaField12, metaField13, metaField14, metaField15, metaField16 });


		logger.debug("Meta field values set");

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		PaymentInformationWS cc = createCreditCard("Frodo Baggins",
				goodCC ? "4111111111111152" : "4111111111111111",
				expiry.getTime());

		newUser.getPaymentInstruments().add(cc);
		logger.debug("Creating user ...");
		newUser.setUserId(api.createUser(newUser));
		logger.debug("User created with id : {}", newUser.getUserId());
		return newUser;
	}
	
	public static PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(1);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
		
		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, new SimpleDateFormat(Constants.CC_DATE_FORMAT).format(date).toCharArray());
		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, true, false,
				DataType.STRING, 4, CreditCardType.VISA);
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
}
