package com.sapienter.jbilling.server.asset;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/4/15.
 */
@Test(testName = "asset.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private final static int PRODUCT_WITH_ASSETS_ID = 1250;
    private final static int ASSET3_ID = 3;
    private final static int ASSET3_CATEGORY_ID = 32;
    private final static int ORDER_ID = 100;
    private final static int ACCOUNT_TYPE_PRANCING_PONY_ID = 3;
    private final static int PRANCING_PONY_PLAN_ID = 3;
    private final static int GANDALF_USER_ID = 2;
    private final static int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testUpdateAsset() {
        AssetWS asset = oscorpAdminApi.getAsset(ASSET3_ID);
        // Cross Company
        try {
            capsuleAdminApi.updateAsset(asset);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAsset() {
        AssetWS asset = oscorpAdminApi.getAsset(ASSET3_ID);
        // Cross Company
        try {
            capsuleAdminApi.getAsset(ASSET3_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteAsset() {
        // Cross Company
        try {
            capsuleAdminApi.deleteAsset(ASSET3_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAssetsForCategory() {
        // Cross Company
        try {
            capsuleAdminApi.getAssetsForCategory(ASSET3_CATEGORY_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_CATEGORY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAssetsForItem() {
        // Cross Company
        try {
            capsuleAdminApi.getAssetsForItem(PRODUCT_WITH_ASSETS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_CATEGORY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAssetTransitions() {
        // Cross Company
        try {
            capsuleAdminApi.getAssetTransitions(ASSET3_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {

            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testFindAssets() {

        SearchCriteria criteria = new SearchCriteria();
        criteria.setFilters(new BasicFilter[]{ new BasicFilter("id", Filter.FilterConstraint.EQ, ASSET3_ID)});
        AssetSearchResult result = oscorpAdminApi.findAssets(PRODUCT_WITH_ASSETS_ID, criteria);
        assertNotNull("Assets found: " + result);

        // Cross Company
        try {
            capsuleAdminApi.findAssets(PRODUCT_WITH_ASSETS_ID, criteria);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAssetAssignmentsForAsset() {
        // Cross Company
        try {
            capsuleAdminApi.getAssetAssignmentsForAsset(ASSET3_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAssetAssignmentsForOrder() {
        // Cross Company
        try {
            capsuleAdminApi.getAssetAssignmentsForOrder(ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testFindOrderForAsset() {
        // Cross Company
        try {
            capsuleAdminApi.findOrderForAsset(ASSET3_ID, new Date());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testFindOrdersForAssetAndDateRange() {
        // Cross Company
        try {
            capsuleAdminApi.findOrdersForAssetAndDateRange(ASSET3_ID, new Date(), new Date());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testCreateAccountTypePrice() {
        // Cross Company
        try {
            capsuleAdminApi.createAccountTypePrice(ACCOUNT_TYPE_PRANCING_PONY_ID, new PlanItemWS(), new Date());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateAccountTypePrice() {
        // Cross Company
        try {
            capsuleAdminApi.updateAccountTypePrice(ACCOUNT_TYPE_PRANCING_PONY_ID, new PlanItemWS(), new Date());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteAccountTypePrice() {
        PlanWS plan = oscorpAdminApi.getPlanWS(PRANCING_PONY_PLAN_ID);
        // Cross Company
        try {
            capsuleAdminApi.deleteAccountTypePrice(ACCOUNT_TYPE_PRANCING_PONY_ID, plan.getPlanItems().get(0).getId());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ACCOUNT_TYPE_PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAccountTypePrices() {
        // Cross Company
        try {
            capsuleAdminApi.getAccountTypePrices(ACCOUNT_TYPE_PRANCING_PONY_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ACCOUNT_TYPE_PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAccountTypePrice() {
        // Cross Company
        try {
            capsuleAdminApi.getAccountTypePrice(ACCOUNT_TYPE_PRANCING_PONY_ID,PRODUCT_WITH_ASSETS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ACCOUNT_TYPE_PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testReserveAsset() {
        // Cross Company
        try {
            capsuleAdminApi.reserveAsset(ASSET3_ID,GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testReleaseAsset() {
        // Cross Company
        try {
            capsuleAdminApi.releaseAsset(ASSET3_ID,GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testStartImportAssetJob() {
        // Cross Company
        try {
            capsuleAdminApi.startImportAssetJob(PRODUCT_WITH_ASSETS_ID,"1","notes-column","global-column","entities-column","file-path","error-file-path");
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, ASSET3_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

}
