package com.sapienter.jbilling.server.ediTransaction;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentStatus;
import com.sapienter.jbilling.server.customerEnrollment.CustomerEnrollmentWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.ApiTestCase;
import com.sapienter.jbilling.test.framework.AbstractTestEnvironment;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 * Created by neeraj on 14/3/16.
 */

@Test(groups = {"web-services", "test-sure-address"}, testName = "SureAddressTest")
public class SureAddressTest extends ApiTestCase {

    private static final Logger logger = LoggerFactory.getLogger(SureAddressTest.class);
    private String stateMetaFieldName="STATE";
    private String cityMetaFieldName="CITY";
    private String zipcodeMetaFieldName="ZIP_CODE";
    private String address1MetaFieldName="ADDRESS1";

    private Integer SURE_ADDRESS_TASK_Id;
    private Integer resellerEntityId;

    private AccountTypeWS accountTypeWS;
    private Integer aitId;
    private JbillingAPI resellerApi;
    private Integer pluginId;

    private final String sureAddressUrl="https://testapi.sureaddress.net/SureAddress.asmx/PostRequest";
    private final String sureAddressValidationKey="6c52a7db-6c95-4e9b-9c93-24679a8e93bf";

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @BeforeClass
    public void setup() throws IOException, JbillingAPIException {
        api=JbillingAPIFactory.getAPI();
        resellerApi=JbillingAPIFactory.getAPI(RemoteContext.Name.API_CHILD_CLIENT);
        SURE_ADDRESS_TASK_Id=api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.process.task.SureAddressTask").getId();
        resellerEntityId=resellerApi.getCallerCompanyId();
        //building account type in reseller company
        buildAccountType();
    }

    @AfterClass
    public void cleanUp() {
        resellerApi.deleteAccountType(accountTypeWS.getId());
        if (null != api) {
            api = null;
        }
    }

    private Integer configurePlugin(String clientNumber){
        //code for configure sureAddress plugin
        logger.debug("configuring sure Tax plugin");
        PluggableTaskWS plugin = new PluggableTaskWS();
        Map<String, String> parameters = new Hashtable<String, String>();
        parameters.put("Sure Address Request Url", sureAddressUrl );
        parameters.put("Client Number", clientNumber );
        parameters.put("Validation Key", sureAddressValidationKey);
        plugin.setParameters((Hashtable) parameters);
        plugin.setProcessingOrder(105);
        plugin.setTypeId(SURE_ADDRESS_TASK_Id);

        return api.createPlugin(plugin);

    }

    private MetaFieldValueWS createMetaFieldValue(String value, String metaFieldName){
        MetaFieldValueWS metaFieldValueWS=new MetaFieldValueWS();
        metaFieldValueWS.setValue(value);
        metaFieldValueWS.getMetaField().setDataType(DataType.STRING);
        metaFieldValueWS.setFieldName(metaFieldName);
        metaFieldValueWS.setGroupId(aitId);
        return metaFieldValueWS;
    }

    private CustomerEnrollmentWS createEnrollment( String address1,  String city, String state, String zipcode){
        CustomerEnrollmentWS customerEnrollmentWS=new CustomerEnrollmentWS();
        customerEnrollmentWS.setAccountTypeId(accountTypeWS.getId());
        customerEnrollmentWS.setId(0);
        customerEnrollmentWS.setDeleted(0);
        customerEnrollmentWS.setEntityId(accountTypeWS.getEntityId());
        customerEnrollmentWS.setCreateDatetime(new Date());

        customerEnrollmentWS.setStatus(CustomerEnrollmentStatus.PENDING);

        List<MetaFieldValueWS> metaFieldValueWSList=new ArrayList<MetaFieldValueWS>();

        metaFieldValueWSList.add(createMetaFieldValue(address1, address1MetaFieldName));
        metaFieldValueWSList.add(createMetaFieldValue(state, stateMetaFieldName));
        metaFieldValueWSList.add(createMetaFieldValue(city, cityMetaFieldName));
        metaFieldValueWSList.add(createMetaFieldValue(zipcode, zipcodeMetaFieldName));

        customerEnrollmentWS.setMetaFields(metaFieldValueWSList.toArray(new MetaFieldValueWS[metaFieldValueWSList.size()]));

        return customerEnrollmentWS;
    }

    private String findMetaFieldValue(String name, CustomerEnrollmentWS customerEnrollmentWS){
        MetaFieldValueWS metaFieldValueWS=Arrays.asList(customerEnrollmentWS.getMetaFields()).stream().filter((MetaFieldValueWS metaFieldValue)->metaFieldValue.getFieldName().equals(name)).findFirst().get();
        return (String)metaFieldValueWS.getValue();
    }

    private Integer buildAccountType() {

        accountTypeWS = new AccountTypeBuilder().entityId(resellerEntityId).create(resellerApi);
        //create a valid meta field
        MetaFieldWS stateMetaField = new MetaFieldBuilder()
                .dataType(DataType.STRING)
                .entityType(EntityType.ACCOUNT_TYPE)
                .entityId(resellerEntityId)
                .name(stateMetaFieldName)
                .fieldUsage(MetaFieldType.STATE_PROVINCE)
                .build();

        MetaFieldWS cityMetaField = new MetaFieldBuilder()
                .dataType(DataType.STRING)
                .entityType(EntityType.ACCOUNT_TYPE)
                .entityId(resellerEntityId)
                .name(cityMetaFieldName)
                .fieldUsage(MetaFieldType.CITY)
                .build();

        MetaFieldWS addressMetaField = new MetaFieldBuilder()
                .dataType(DataType.STRING)
                .entityType(EntityType.ACCOUNT_TYPE)
                .entityId(resellerEntityId)
                .name(address1MetaFieldName)
                .fieldUsage(MetaFieldType.ADDRESS1)
                .build();

        MetaFieldWS zipcodeMetaField = new MetaFieldBuilder()
                .dataType(DataType.STRING)
                .entityId(resellerEntityId)
                .entityType(EntityType.ACCOUNT_TYPE)
                .name(zipcodeMetaFieldName)
                .fieldUsage(MetaFieldType.POSTAL_CODE)
                .build();


        AccountInformationTypeWS ait = new AccountInformationTypeBuilder(accountTypeWS)
                .name("Service Information")
                .addMetaField(stateMetaField)
                .addMetaField(cityMetaField)
                .addMetaField(addressMetaField)
                .addMetaField(zipcodeMetaField)
                .entityId(resellerEntityId)
                .build();


        ait.setId(resellerApi.createAccountInformationType(ait));
        aitId=ait.getId();
        return accountTypeWS.getId();
    }


    @Test
    public void test001PluginNotConfigured() {
        String city="ALBANY", state="New York", zipCode="201301", address="42 EAGLE ST";

        CustomerEnrollmentWS customerEnrollmentWS=createEnrollment(address, city, state, zipCode);
        customerEnrollmentWS=resellerApi.validateCustomerEnrollment(customerEnrollmentWS);

        assertEquals(state, findMetaFieldValue(stateMetaFieldName, customerEnrollmentWS));
        assertEquals( zipCode, findMetaFieldValue(zipcodeMetaFieldName, customerEnrollmentWS));
        assertEquals( city, findMetaFieldValue(cityMetaFieldName, customerEnrollmentWS));
        assertEquals(address, findMetaFieldValue(address1MetaFieldName, customerEnrollmentWS));
    }


    @Test
    public void test002PluginConfiguredWithInvalidCredential() {

        String invalidClientNumber="00000035012";
        pluginId=configurePlugin(invalidClientNumber);
        String city="ALBANY", state="New York", zipCode="201301", address="42 EAGLE ST";
        CustomerEnrollmentWS customerEnrollmentWS=createEnrollment(address, city, state, zipCode);
        try {
            resellerApi.validateCustomerEnrollment(customerEnrollmentWS);
            fail("With invalid client-number exception should be thrown");
        } catch (SessionInternalError sei){}
        catch (Exception e) {
            e.printStackTrace();
            fail("SessionInternalError should be a thrown");
        }
    }

    @Test
    public void test003ValidCredentialButInvalidZipcode() {
        String validClientNumber="000000350";
        pluginId=configurePlugin(validClientNumber);
        String city="ALBANY", state="New York", zipCode="201301", address="42 EAGLE ST";
        try{
            CustomerEnrollmentWS customerEnrollmentWS=createEnrollment(address, city, state, zipCode);
            customerEnrollmentWS=resellerApi.validateCustomerEnrollment(customerEnrollmentWS);
            assertEquals( city, findMetaFieldValue(cityMetaFieldName, customerEnrollmentWS));
            assertEquals(state, findMetaFieldValue(stateMetaFieldName, customerEnrollmentWS));
            assertEquals(address, findMetaFieldValue(address1MetaFieldName, customerEnrollmentWS));
            assertEquals("122071619", findMetaFieldValue(zipcodeMetaFieldName, customerEnrollmentWS));

        }catch (Exception e){
            e.printStackTrace();
            fail("Zip+4 code should be resolved for the address("+address+", "+city+", "+state+")");
        }

    }


    @Test
    public void test004ValidCredentialValidZipCode() {
        String validClientNumber="000000350";
         pluginId=configurePlugin(validClientNumber);

        String city="ALBANY", state="New York", zipCode="122071619", address="42 EAGLE ST";
        CustomerEnrollmentWS customerEnrollmentWS=createEnrollment(address, city, state, zipCode);
        try {
            customerEnrollmentWS=resellerApi.validateCustomerEnrollment(customerEnrollmentWS);
            assertEquals(city, findMetaFieldValue(cityMetaFieldName, customerEnrollmentWS));
            assertEquals(state, findMetaFieldValue(stateMetaFieldName, customerEnrollmentWS));
            assertEquals(address, findMetaFieldValue(address1MetaFieldName, customerEnrollmentWS));
            assertEquals(zipCode, findMetaFieldValue(zipcodeMetaFieldName, customerEnrollmentWS));
        } catch (Exception e) {
            fail("Zip+4 code should be resolved for the address("+address+", "+city+", "+state+")");
        }
    }

    @Test
    public void test005ValidCredentialInvalidAddress() {
        String validClientNumber="000000350";
         pluginId=configurePlugin(validClientNumber);

        try {
            String city="Delhi", state="New York", zipCode="201301", address="Noida";
            CustomerEnrollmentWS customerEnrollmentWS=createEnrollment(city, state, address, zipCode);
            resellerApi.validateCustomerEnrollment(customerEnrollmentWS);
            fail("No zipcode should be found for the address("+address+", "+city+", "+state+")");

        }catch (SessionInternalError sei){}
        catch (Exception e) {
            fail("SessionInternalError should be a thrown");
        }
    }

    @AfterMethod
    public void afterTest() throws Exception {
      if(pluginId!=null){
          api.deletePlugin(pluginId);
      }
    }

}
