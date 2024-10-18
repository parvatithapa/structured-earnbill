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

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.customerEnrollment;

import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertTrue;

@Test(groups = {  "web-services", "customer-enrollment" }, testName = "customerEnrollment.WSTest")
public class WSTest {

    private static JbillingAPI api;

    private AccountTypeWS refAccountType;
    private AccountInformationTypeWS ait;

    @BeforeClass
    public void initializeTests() throws IOException, JbillingAPIException {
        api=JbillingAPIFactory.getAPI();
        refAccountType=api.getAccountType(1);
        ait=api.getAccountInformationType(1);
    }

    @Test
    public void saveAndDeleteCustomerEnrollment() throws Exception {

        CustomerEnrollmentWS customerEnrollmentWS = new CustomerEnrollmentWS();
        customerEnrollmentWS.setAccountTypeId(refAccountType.getId());
        customerEnrollmentWS.setId(0);
        customerEnrollmentWS.setDeleted(0);
        customerEnrollmentWS.setEntityId(refAccountType.getEntityId());
        customerEnrollmentWS.setCreateDatetime(new Date());
        customerEnrollmentWS.setStatus(CustomerEnrollmentStatus.PENDING);
        customerEnrollmentWS.setComment("test comment");

        List<MetaFieldValueWS> metaFieldValueWSList=new ArrayList<MetaFieldValueWS>();
        MetaFieldValueWS fnMetaFieldValueWS=new MetaFieldValueWS();
        fnMetaFieldValueWS.setValue("Neeraj");
        fnMetaFieldValueWS.getMetaField().setDataType(DataType.STRING);
        fnMetaFieldValueWS.setFieldName("contact.first.name");
        fnMetaFieldValueWS.setGroupId(ait.getId());
        metaFieldValueWSList.add(fnMetaFieldValueWS);

        MetaFieldValueWS ageMetaFieldValueWS=new MetaFieldValueWS();
        ageMetaFieldValueWS.getMetaField().setDataType(DataType.INTEGER);
        ageMetaFieldValueWS.setFieldName("contact.fax.area.code");
        ageMetaFieldValueWS.setGroupId(ait.getId());
        metaFieldValueWSList.add(ageMetaFieldValueWS);

        MetaFieldValueWS emailMetaFieldValueWS=new MetaFieldValueWS();
        emailMetaFieldValueWS.getMetaField().setDataType(DataType.STRING);
        emailMetaFieldValueWS.setFieldName("contact.email");
        emailMetaFieldValueWS.setGroupId(ait.getId());
        emailMetaFieldValueWS.setValue("neeraj");
        metaFieldValueWSList.add(emailMetaFieldValueWS);

        customerEnrollmentWS.setMetaFields(metaFieldValueWSList.toArray(new MetaFieldValueWS[metaFieldValueWSList.size()]));

        Integer customerEnrollentId=api.createUpdateEnrollment(customerEnrollmentWS);

        CustomerEnrollmentWS customerEnrollment=api.getCustomerEnrollment(customerEnrollentId);
        assertNotNull("Customer Enrollment saved successfully", customerEnrollentId);
        assertEquals(ait.getMetaFields().length, customerEnrollment.getMetaFields().length);
        api.deleteEnrollment(customerEnrollentId);

    }

    @Test
    public void validateCustomerEnrollment() throws Exception {
        CustomerEnrollmentWS customerEnrollmentWS = new CustomerEnrollmentWS();
        customerEnrollmentWS.setAccountTypeId(refAccountType.getId());
        customerEnrollmentWS.setId(0);
        customerEnrollmentWS.setDeleted(0);
        customerEnrollmentWS.setEntityId(refAccountType.getEntityId());
        customerEnrollmentWS.setCreateDatetime(new Date());
        customerEnrollmentWS.setStatus(CustomerEnrollmentStatus.PENDING);
        customerEnrollmentWS.setComment("test comment");


        List<MetaFieldValueWS> metaFieldValueWSList=new ArrayList<MetaFieldValueWS>();
        MetaFieldValueWS fnMetaFieldValueWS=new MetaFieldValueWS();
        fnMetaFieldValueWS.setValue("Neeraj");
        fnMetaFieldValueWS.getMetaField().setDataType(DataType.STRING);
        fnMetaFieldValueWS.setFieldName("contact.first.name");
        fnMetaFieldValueWS.setGroupId(ait.getId());
        metaFieldValueWSList.add(fnMetaFieldValueWS);

        MetaFieldValueWS ageMetaFieldValueWS=new MetaFieldValueWS();
        ageMetaFieldValueWS.getMetaField().setDataType(DataType.INTEGER);
        ageMetaFieldValueWS.setFieldName("contact.fax.area.code");
        ageMetaFieldValueWS.setGroupId(ait.getId());
        metaFieldValueWSList.add(ageMetaFieldValueWS);

        MetaFieldValueWS emailMetaFieldValueWS=new MetaFieldValueWS();
        emailMetaFieldValueWS.getMetaField().setDataType(DataType.STRING);
        emailMetaFieldValueWS.setFieldName("contact.email");
        emailMetaFieldValueWS.setGroupId(ait.getId());
        metaFieldValueWSList.add(emailMetaFieldValueWS);

        customerEnrollmentWS.setMetaFields(metaFieldValueWSList.toArray(new MetaFieldValueWS[metaFieldValueWSList.size()]));

        try{
            api.validateCustomerEnrollment(customerEnrollmentWS);
            fail();
        }catch (Exception e){
            assertTrue("Validation failed", true);
        }
        emailMetaFieldValueWS.setValue("abc.xyz@jbilling.com");
        customerEnrollmentWS.setMetaFields(null);
        customerEnrollmentWS.setMetaFields(metaFieldValueWSList.toArray(new MetaFieldValueWS[metaFieldValueWSList.size()]));

        api.validateCustomerEnrollment(customerEnrollmentWS);

        Integer customerEnrollmentId=api.createUpdateEnrollment(customerEnrollmentWS);
        assertNotNull("Enrollment save successfully", customerEnrollmentId);
        api.deleteEnrollment(customerEnrollmentId);
    }
}
