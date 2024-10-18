package com.sapienter.jbilling.server.metafield;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.MetaFieldGroupWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/11/15.
 */
@Test(testName = "metafield.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final int PRANCING_PONY_META_FIELD_ID = 1;
    private static final int PRANCING_PONY_META_FIELD_GROUP_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testUpdateMetaFieldGroup() {
        MetaFieldGroupWS metaFieldGroupWS = oscorpAdminApi.getMetaFieldGroup(PRANCING_PONY_META_FIELD_GROUP_ID);

        // Cross Company
        try {
            capsuleAdminApi.updateMetaFieldGroup(metaFieldGroupWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_META_FIELD_GROUP_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteMetaFieldGroup() {
        // Cross Company
        try {
            capsuleAdminApi.deleteMetaFieldGroup(PRANCING_PONY_META_FIELD_GROUP_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_META_FIELD_GROUP_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetMetaFieldGroup() {
        // Cross Company
        try {
            capsuleAdminApi.getMetaFieldGroup(PRANCING_PONY_META_FIELD_GROUP_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_META_FIELD_GROUP_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateMetaField() {
        MetaFieldWS metaFieldWS = oscorpAdminApi.getMetaField(PRANCING_PONY_META_FIELD_ID);

        // Cross Company
        try {
            capsuleAdminApi.updateMetaField(metaFieldWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_META_FIELD_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteMetaField() {
        // Cross Company
        try {
            capsuleAdminApi.deleteMetaField(PRANCING_PONY_META_FIELD_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_META_FIELD_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetMetaField() {
        // Cross Company
        try {
            capsuleAdminApi.getMetaField(PRANCING_PONY_META_FIELD_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_META_FIELD_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
