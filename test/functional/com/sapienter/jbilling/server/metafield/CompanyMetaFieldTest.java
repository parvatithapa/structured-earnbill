package com.sapienter.jbilling.server.metafield;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.ApiTestCase;
import org.junit.Rule;
import org.junit.matchers.JUnitMatchers;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.regex.Matcher;

import static org.testng.AssertJUnit.*;

/**
 * Created by hitesh on 10/3/16.
 */

@Test(groups = {"web-services", "company-metafield"}, sequential = true, testName = "CompanyMetaFieldTest")
public class CompanyMetaFieldTest extends ApiTestCase {
    private static final Logger logger = LoggerFactory.getLogger(CompanyMetaFieldTest.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public void setup() throws IOException, JbillingAPIException {
        if (null == api) {
            logger.debug("Inside setup api value null");
            api = JbillingAPIFactory.getAPI();
        }
    }

    @AfterClass
    public void cleanUp() {
        if (null != api) {
            api = null;
        }
    }

    @Test
    public void test001CreateCompanyLevelMetaField() {
        MetaFieldWS metafieldWS = createMetaField();
        Integer result = api.createMetaField(metafieldWS);
        assertNotNull("Company Level Metafield has not been created", result);
        api.deleteMetaField(result);
    }

    @Test
    public void test002FindCompanyLevelMetaField() {
        MetaFieldWS metafieldWS = createMetaField();
        Integer result = api.createMetaField(metafieldWS);
        assertNotNull("Company Level Metafield has not been created", result);
        MetaFieldWS newMetaFieldWS = api.getMetaField(result);
        assertNotNull("Company Level Metafield not found at: " + result, newMetaFieldWS);
        api.deleteMetaField(result);

    }

    @Test
    public void test003UpdateCompanyLevelMetaField() {

        MetaFieldWS metafieldWS = createMetaField();
        Integer result = api.createMetaField(metafieldWS);
        assertNotNull("Company Level Metafield has not been created", result);

        MetaFieldWS newMetaFieldWS = api.getMetaField(result);
        assertNotNull("Company Level Metafield not found at: " + result, newMetaFieldWS);

        newMetaFieldWS.setName("CompanyLevel-2");
        newMetaFieldWS.setDataType(DataType.STRING);
        newMetaFieldWS.setMandatory(true);
        newMetaFieldWS.setDisabled(false);
        newMetaFieldWS.setDisplayOrder(5);

        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
        metaFieldValueWS.setStringValue("ESCO");
        metaFieldValueWS.getMetaField().setDataType(metafieldWS.getDataType());
        metaFieldValueWS.setFieldName(metafieldWS.getName());

        newMetaFieldWS.setDefaultValue(metaFieldValueWS);
        newMetaFieldWS.setEntityType(EntityType.COMPANY);
        newMetaFieldWS.setPrimary(true);
        api.updateMetaField(newMetaFieldWS);

        MetaFieldWS updatedMetafieldWS = api.getMetaField(newMetaFieldWS.getId());
        assertNotSame("Company Level MetaField has not been updated", updatedMetafieldWS.getName(), newMetaFieldWS.getName());
        api.deleteMetaField(updatedMetafieldWS.getId());
    }

    @Test(expectedExceptions = SessionInternalError.class)
    public void test004DeleteCompanyLevelMetaField() throws SessionInternalError {

        MetaFieldWS metafieldWS = createMetaField();
        Integer result = api.createMetaField(metafieldWS);
        assertNotNull("Company Level Metafield has not been created", result);
        api.deleteMetaField(result);

        MetaFieldWS metaFieldWS1 = api.getMetaField(result);
        assertTrue("Company Level MetaField has not been deleted", metaFieldWS1 == null);

    }

    @Test(expectedExceptions = SessionInternalError.class)
    public void test005CreateEmptyMetaField() {
        MetaFieldWS metafieldWS = new MetaFieldWS();
        Integer result = api.createMetaField(metafieldWS);
    }

    private MetaFieldWS createMetaField() {
        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setName("CompanyLevel-1");
        metafieldWS.setDataType(DataType.STRING);
        metafieldWS.setMandatory(true);
        metafieldWS.setDisabled(false);
        metafieldWS.setDisplayOrder(5);
        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
        metaFieldValueWS.setFieldName("CompanyLevel-1");
        metaFieldValueWS.setStringValue("LDC");
        metaFieldValueWS.getMetaField().setDataType(metafieldWS.getDataType());
        metaFieldValueWS.setFieldName(metafieldWS.getName());
        metafieldWS.setDefaultValue(metaFieldValueWS);
        metafieldWS.setEntityType(EntityType.COMPANY);
        metafieldWS.setPrimary(true);
        return metafieldWS;
    }

}
