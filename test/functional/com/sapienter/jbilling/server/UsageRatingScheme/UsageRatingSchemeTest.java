package com.sapienter.jbilling.server.UsageRatingScheme;


import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.usageratingscheme.domain.entity.UsageRatingSchemeTypeDTO;
import com.sapienter.jbilling.server.usageratingscheme.domain.repository.UsageRatingSchemeTypeDAS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

@Test(groups = {"web-services"}, testName = "UsageRatingScheme.UsageRatingSchemeTest")
public class UsageRatingSchemeTest {

    JbillingAPI api;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final UsageRatingSchemeTypeDTO[] types = new UsageRatingSchemeTypeDTO[2];

    @BeforeClass
    public void init() throws IOException, JbillingAPIException {
        api = JbillingAPIFactory.getAPI();
    }

    @AfterClass
    public void cleanUp() {
    }

    @Test
    public void testCreateRatingScheme() {
//        try {
//            logger.debug("Creating valid Rating Scheme.");
//            Integer ratingScheme = api.createUsageRatingScheme(getUsageRatingScheme("Test Create Rating Scheme"));
//            assertNotNull(ratingScheme);
//            api.deleteUsageRatingScheme(ratingScheme);
//        } catch (Exception e) {
//            fail("Usage rating Scheme creation failed."+e);
//        }
    }

    private UsageRatingSchemeWS getUsageRatingScheme(String name) {
        Map<String, String> fixed = new HashMap<>();
        fixed.put("Start","200");
        fixed.put("Size","5");
        fixed.put("Increment", "1");
        UsageRatingSchemeWS usageRatingSchemeWS = new UsageRatingSchemeWS();
        usageRatingSchemeWS.setRatingSchemeCode(name);
        usageRatingSchemeWS.setRatingSchemeType("Tiered Linear Test");
        usageRatingSchemeWS.setFixedAttributes(fixed);
        usageRatingSchemeWS.setUsesDynamicAttributes(false);
        usageRatingSchemeWS.setDynamicAttributes(Collections.emptySortedSet());
        usageRatingSchemeWS.setDynamicAttributeName(null);
        return usageRatingSchemeWS;
    }

    @Test
    public void testDeleteRatingScheme() {
//        try {
//            logger.debug("Creating valid usage Rating Scheme.");
//            Integer ratingSchemeId = api.createUsageRatingScheme(getUsageRatingScheme("Test Delete usage Rating Scheme"));
//            logger.debug("Deleting persisted usage rating scheme.");
//            Boolean deleted = api.deleteUsageRatingScheme(ratingSchemeId);
//            assertTrue(deleted);
//        } catch (Exception e) {
//            fail("Usage Rating Scheme deletion failed.");
//        }
    }

    @Test
    public void testGetRatingScheme() {
//        try {
//            logger.debug("Creating valid usage Rating Scheme.");
//            UsageRatingSchemeWS usageRatingSchemeWS = new UsageRatingSchemeWS();
//            usageRatingSchemeWS.setRatingSchemeCode("Test Get usage Rating Scheme");
//            usageRatingSchemeWS.setRatingSchemeType("Average Derived Test");
//            usageRatingSchemeWS.setUsesDynamicAttributes(false);
//
//            Integer ratingSchemeId = api.createUsageRatingScheme(getUsageRatingScheme("Test Get usage Rating Scheme"));
//            logger.debug("Getting usage Rating Scheme {}", ratingSchemeId);
//            UsageRatingSchemeWS ratingSchemeWS = api.getUsageRatingScheme(ratingSchemeId);
//            assertNotNull(ratingSchemeWS);
//            api.deleteUsageRatingScheme(ratingSchemeId);
//
//        } catch (Exception e) {
//            fail("Cannot retrieve usage rating scheme.");
//        }
    }
}

