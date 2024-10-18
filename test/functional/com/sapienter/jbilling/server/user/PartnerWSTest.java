/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.partner.PartnerType;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test of Partner web-service API
 *
 * @author Brian Cowdery
 * @since 31-Oct-2011
 */
@Test(groups = { "web-services", "partner" }, testName = "PartnerWSTest")
public class PartnerWSTest {

    private static final Logger logger = LoggerFactory.getLogger(PartnerWSTest.class);
    private static final Integer PARTNER_ROLE_ID = 4;
    private static Integer MORDOR_CUSTOMER_ROLE_ID = 5;
    private static Integer MORDOR_PARTNER_ID = 20;
    private static Integer PRANCING_PONY_PARTNER_ID = 10;

    private static final String RESELLER_API_BEAN = "apiClientO1Admin";
    private static final String MORDOR_API_BEAN = "apiClientSysAdmin";

    private final List<Integer> partnerIdsToClean = new LinkedList<Integer>();
    private final List<Integer> otherPartnerIdsToClean = new LinkedList<Integer>();

    public void cleanAfterTest() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        for (Integer partnerId: partnerIdsToClean) {
            if (partnerId != null) api.deletePartner(partnerId);
        }
        partnerIdsToClean.clear();

        JbillingAPI secondApi = JbillingAPIFactory.getAPI(RESELLER_API_BEAN);
        for (Integer partnerId: otherPartnerIdsToClean) {
            if (partnerId != null) secondApi.deletePartner(partnerId);
        }
        otherPartnerIdsToClean.clear();
    }

    @Test
    public void testCreatePartner() throws Exception {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            JbillingAPI resellerApi = JbillingAPIFactory.getAPI(RESELLER_API_BEAN);

            try {
                createPartnerForTestOnDatabase(1, "create new", MORDOR_PARTNER_ID, api);
                fail("Parent is in a different hierarchy");
            } catch (SessionInternalError e) { }

            //create reseller with link to parent in prancing pony (higher in hierarchy)
            Integer resellerPartnerId = createPartnerForTestOnDatabase(1, "createNewReseller", PRANCING_PONY_PARTNER_ID, resellerApi);
            otherPartnerIdsToClean.add(resellerPartnerId);

            //create partner with link to parent in child company
            Integer partnerId = createPartnerForTestOnDatabase(1, "create new", resellerPartnerId, api);
            partnerIdsToClean.add(partnerId);

            //create partner with link to parent in same company
            partnerId = createPartnerForTestOnDatabase(1, "create new", PRANCING_PONY_PARTNER_ID, api);
            partnerIdsToClean.add(partnerId);

            PartnerWS partner = api.getPartner(partnerId);

            assertNotNull("partner created", partner);
            assertNotNull("partner has an id", partner.getId());
        } finally { cleanAfterTest(); }
    }

    @Test
    public void testUpdatePartner() throws Exception {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            JbillingAPI resellerApi = JbillingAPIFactory.getAPI(RESELLER_API_BEAN);

            //create reseller partner
            logger.debug("Create reseller");
            Integer resellerPartnerId = createPartnerForTestOnDatabase(1, "updateReseller", null, resellerApi);
            otherPartnerIdsToClean.add(resellerPartnerId);

            //set parent to reseller in parent company
            PartnerWS partner = resellerApi.getPartner(resellerPartnerId);
            partner.setParentId(PRANCING_PONY_PARTNER_ID);
            logger.debug("Update reseller. Set parent to PP");
            resellerApi.updatePartner(null, partner);
            partner = resellerApi.getPartner(resellerPartnerId);
            assertEquals("Partner parent should be "+PRANCING_PONY_PARTNER_ID, PRANCING_PONY_PARTNER_ID, partner.getParentId());

            //clear parent
            partner.setParentId(null);
            logger.debug("Update reseller. Clear parent.");
            resellerApi.updatePartner(null, partner);

            // create partner in parent
            logger.debug("Update prancing pony. Clear parent.");
            Integer partnerId = createPartnerForTestOnDatabase(2, "update", null, api);
            partnerIdsToClean.add(partnerId);

            partner = api.getPartner(partnerId);
            assertNotNull("partner created", partner);

            //set parent to be child
            partner.setParentId(resellerPartnerId);
            logger.debug("Update prancing pony. Set parent to reseller.");
            api.updatePartner(null, partner);
            partner = api.getPartner(partnerId);
            assertEquals("Partner parent should be "+resellerPartnerId, resellerPartnerId, partner.getParentId());

            try {
                partner.setParentId(MORDOR_PARTNER_ID);
                logger.debug("Update prancing pony. Parent to mordor.");
                api.updatePartner(null, partner);
                fail("Parent is in a different hierarchy");
            } catch (SessionInternalError e) { }

        } finally { cleanAfterTest(); }
    }

    @Test
    public void testDeletePartner() throws Exception {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            JbillingAPI mordorApi = JbillingAPIFactory.getAPI(MORDOR_API_BEAN);

            // create partner
            Integer partnerId = createPartnerForTestOnDatabase(3, "delete", null, api);
            PartnerWS partner = api.getPartner(partnerId);
            assertNotNull("partner created", partner);
            assertEquals("Partner must not be deleted", 0, partner.getDeleted());

            //delete should fail
            try {
                mordorApi.deletePartner(partnerId);
                fail("Mordor can not delete partner from prancing pony");
            } catch (Exception e) {}

            // delete partner
            api.deletePartner(partner.getId());
            partner = api.getPartner(partnerId);
            assertEquals("Partner must be deleted", 1, partner.getDeleted());

            // verify that the base user was deleted with the partner
            UserWS deletedUser = api.getUserWS(partner.getUserId());
            assertEquals(1, deletedUser.getDeleted());
        } finally { cleanAfterTest(); }
    }

    private Integer createPartnerForTestOnDatabase(int testNumber, String contactLastName, Integer parentId, JbillingAPI api) throws Exception {
        // new partner
        UserWS user = createUserForTest(testNumber, contactLastName);

        PartnerWS partner = new PartnerWS();
        partner.setType(PartnerType.STANDARD.name());
        partner.setParentId(parentId);

        // create partner
        return api.createPartner(user, partner);
    }

    private UserWS createUserForTest(Integer testNumber, String contactLastName) {
        UserWS user = new UserWS();
        user.setUserName("partner-0" + testNumber + "-" + new Date().getTime());
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(PARTNER_ROLE_ID);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);
        setContactTestOnUser(user, contactLastName);
        return user;
    }

    private void setContactTestOnUser(UserWS user, String lastName) {
        ContactWS contact= CreateObjectUtil.createCustomerContact(user.getUserName() + "@test.com");
        contact.setFirstName("Partner Test");
        contact.setLastName(lastName);
        user.setContact(contact);
    }

    @Test
    public void testGetPartner() throws Exception {
        JbillingAPI prancingPonyEntity = JbillingAPIFactory.getAPI();
        JbillingAPI mordorEntity= JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_MORDOR.getName());

        // partner that does not exist throws exception
        try {
            prancingPonyEntity.getPartner(999);
            fail("non-existent partner should throw exception");
        } catch (SessionInternalError e) {
            assertTrue(e.getMessage().contains("Error calling jBilling API, method=getPartner"));
        }
        Integer partnerOnOtherEntity = null;
        try {
            UserWS userForTest = createUserForTest(4, "partner on other entity");
            userForTest.setMainRoleId(MORDOR_CUSTOMER_ROLE_ID);
            PartnerWS partner = new PartnerWS();
            partner.setType(PartnerType.STANDARD.name());
            // create partner on other entity
            partnerOnOtherEntity = mordorEntity.createPartner(userForTest, partner);

            // partner belonging to a different entity throws a security exception
            try {
                prancingPonyEntity.getPartner(20); // belongs to mordor entity
                fail("partner does not belong to entity 1, should throw security exception.");
            } catch (SessionInternalError | SecurityException e) {
                assertTrue(e.getMessage().contains("Unauthorized access to entity 2"));
            }
        } finally {
            try{
                if (partnerOnOtherEntity != null) mordorEntity.deletePartner(partnerOnOtherEntity);
            } catch (SessionInternalError e) {
                assertTrue(e.getMessage().contains("Notification not found for sending deleted user notification"));
            }
        }

    }
}
