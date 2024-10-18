package com.sapienter.jbilling.server.user;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.JBillingTestUtils;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

@Test(groups = { "web-services", "customer" }, testName = "CustomerMetaFieldAPITest")
public class CustomerMetaFieldAPITest {

	private static final Logger logger = LoggerFactory.getLogger(CustomerMetaFieldAPITest.class);
	JbillingAPI api;
	private static final String CUSTOMER_METAFIELD_ACC_PIN = "Account PIN";
	private String metaFieldValue = null;
	
	@BeforeClass
	protected void setUp() throws Exception {

		api = JbillingAPIFactory.getAPI();
	}
	
	@Test
	public void test001GetUserByCustomerMetaFieldAPI() {
		try {
			JbillingAPI api = JbillingAPIFactory.getAPI();
			metaFieldValue = ""+Calendar.getInstance().getTimeInMillis();
			UserWS user = CustomerMetaFieldAPITest.createUser(true, null, 1, api, metaFieldValue);
			UserWS newUser = api.getUserByCustomerMetaField(metaFieldValue, CUSTOMER_METAFIELD_ACC_PIN);
			assertNotNull("newUser should not be null",newUser);
			assertEquals("UserId should be equal ",newUser.getId(), user.getId());
			
			logger.debug("Checking validation meta field value which should not be null");
			try {
				newUser = api.getUserByCustomerMetaField(null, CUSTOMER_METAFIELD_ACC_PIN);
			} catch (SessionInternalError e) {
				JBillingTestUtils.assertContainsError(e, "UserWS,metaFieldValue,user.validation.metafield.value.or.name.null.or.empty" );
			}
			
			logger.debug("Checking validation meta field name which should not be null");
			try {
				newUser = api.getUserByCustomerMetaField(metaFieldValue, null);
			} catch (SessionInternalError e) {
				JBillingTestUtils.assertContainsError(e, "UserWS,metaFieldValue,user.validation.metafield.value.or.name.null.or.empty" );
			}
			
			logger.debug("Checking validation meta field name and value which should not be null");
			try {
				newUser = api.getUserByCustomerMetaField(null, null);
			} catch (SessionInternalError e) {
				JBillingTestUtils.assertContainsError(e, "UserWS,metaFieldValue,user.validation.metafield.value.or.name.null.or.empty" );
			}
			
			logger.debug("Checking validation meta field name and value which should not be empty");
			try {
				newUser = api.getUserByCustomerMetaField("", "");
			} catch (SessionInternalError e) {
				JBillingTestUtils.assertContainsError(e, "UserWS,metaFieldValue,user.validation.metafield.value.or.name.null.or.empty" );
			}
		}catch(Exception e){
			logger.error("Error during test001GetUserByCustomerMetaFieldAPI", e.getMessage());
		}
	}
	
	@Test
	public void test002GetUserByCustomerMetaFieldAPIValidation() {
		try {
			UserWS user = CustomerMetaFieldAPITest.createUser(true, null, 1, api, metaFieldValue);
			UserWS newUser = api.getUserByCustomerMetaField(metaFieldValue, CUSTOMER_METAFIELD_ACC_PIN);
			assertNotNull("newUser should not be null",newUser);
			assertEquals("UserId should be equal ",newUser.getId(), user.getId());
			
			logger.debug("Checking validation for more than one matching customer with supplied Meta Field Value");
			try {
				newUser = api.getUserByCustomerMetaField(metaFieldValue, CUSTOMER_METAFIELD_ACC_PIN);
			} catch (SessionInternalError e) {
				JBillingTestUtils.assertContainsError(e, "customer.meta.field.value.more.then.one.match" );
			}
		}catch(Exception e){
			logger.error("Error during test002GetUserByCustomerMetaFieldAPIValidation", e.getMessage());
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

		newUser.setUserId(api.createUser(newUser));
		logger.debug("User created with id: {}", newUser.getUserId());
		return newUser;
	}
}
