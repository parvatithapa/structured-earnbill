package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/5/15.
 */
@Test(testName = "item.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private final static int ITEM_TYPE_PRANCING_PONY = 2201;
    private final static int ITEM_PRANCING_PONY = 2900;
    private final static int PRANCING_PONY_ID = 1;
    private final static int GANDALF_USER_ID = 2;
    private final static String ITEM_PRANCING_PONY_CODE = "CALL-LD-GEN";


    @Test(enabled = false)//TODO: string param, not secured yet
    public void testGetItemCategoriesByPartner() {
        // Cross Company
        try {
            pendunsus1Api.getItemCategoriesByPartner("test-partner",true);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access to entity 2 by caller 'pendunsus1;1' (id 1)"));
        }
    }

    @Test
    public void testGetChildItemCategories() {
        // Cross Company
        try {
            capsuleAdminApi.getChildItemCategories(ITEM_TYPE_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetItem() {
        // Cross Company
        try {
            capsuleAdminApi.getItem(1, 1, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateItem() {
        ItemDTOEx[] itemList = oscorpAdminApi.getItemByCategory(1);
         // Cross Company
        try {
            capsuleAdminApi.updateItem(itemList[0]);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteItem() {
        // Cross Company
        try {
            capsuleAdminApi.deleteItem(ITEM_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAddonItems() {
        // Cross Company
        try {
            capsuleAdminApi.getAddonItems(ITEM_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetItemByCategory() {
        // Cross Company
        try {
            capsuleAdminApi.getItemByCategory(1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetUserItemsByCategory() {
        // Cross Company
        try {
            capsuleAdminApi.getUserItemsByCategory(1,ITEM_TYPE_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateItemCategory() {
        ItemTypeWS itemType = oscorpAdminApi.getItemCategoryById(ITEM_TYPE_PRANCING_PONY);
        // Cross Company
        try {
            capsuleAdminApi.updateItemCategory(itemType);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test(enabled = false) //TODO: review @Validator(type = Validator.Type.EDIT)
    public void testDeleteItemCategory() {
        // Cross Company
        try {
            capsuleAdminApi.deleteItemCategory(ITEM_TYPE_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access to entity 2"));
        }
    }

    @Test
    public void testGetAllItemCategoriesByEntityId() {
        // Cross Company
        try {
            capsuleAdminApi.getAllItemCategoriesByEntityId(PRANCING_PONY_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAllItemsByEntityId() {
        // Cross Company
        try {
            capsuleAdminApi.getAllItemsByEntityId(PRANCING_PONY_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testIsUserSubscribedTo() {
        // Cross Company
        try {
            capsuleAdminApi.isUserSubscribedTo(GANDALF_USER_ID,ITEM_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetLatestInvoiceByItemType() {
        // Cross Company
        try {
            capsuleAdminApi.getLatestInvoiceByItemType(GANDALF_USER_ID,ITEM_TYPE_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetLastInvoicesByItemType() {
        // Cross Company
        try {
            capsuleAdminApi.getLastInvoicesByItemType(GANDALF_USER_ID,ITEM_TYPE_PRANCING_PONY,1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetLatestOrderByItemType() {
        // Cross Company
        try {
            capsuleAdminApi.getLatestOrderByItemType(GANDALF_USER_ID,ITEM_TYPE_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetLastOrdersByItemType() {
        // Cross Company
        try {
            capsuleAdminApi.getLastOrdersByItemType(GANDALF_USER_ID,ITEM_TYPE_PRANCING_PONY,1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testValidatePurchase() {
        // Cross Company
        try {
            capsuleAdminApi.validatePurchase(GANDALF_USER_ID,ITEM_PRANCING_PONY,null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testValidateMultiPurchase() {
        // Cross Company
        try {
            capsuleAdminApi.validateMultiPurchase(GANDALF_USER_ID,null,null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetItemCategoryById() {
        // Cross Company
        try {
            capsuleAdminApi.getItemCategoryById(ITEM_TYPE_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

}
