package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationRatingSchemeWS;
import com.sapienter.jbilling.server.mediation.RatingSchemeAssociationWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Andres Canevaro on 17/08/15.
 */
@Test(groups = {"web-services", "mediation"}, testName = "RatingSchemeTest")
public class RatingSchemeTest {

    private static final Logger logger = LoggerFactory.getLogger(GlobalMediationTest.class);
    JbillingAPI api;

    @BeforeClass
    public void init() throws IOException, JbillingAPIException {
        api = JbillingAPIFactory.getAPI();
    }

    @AfterTest
    public void clean() {
        List<MediationRatingSchemeWS> ratingSchemeList = Arrays.asList(api.getRatingSchemesForEntity());
        for(MediationRatingSchemeWS ratingScheme: ratingSchemeList) {
            api.deleteRatingScheme(ratingScheme.getId());
        }
    }

    @Test
    public void testCreateRatingScheme() {
        try {
            logger.debug("Creating valid Rating Scheme.");
            Integer ratingScheme = api.createRatingScheme(getRatingScheme("Test Create Rating Scheme"));
            assertNotNull(ratingScheme);
        } catch (Exception e) {
            fail("Rating Scheme creation failed.");
        }
    }

    @Test
    public void testCreateRatingSchemeInvalid() {
        MediationRatingSchemeWS ratingScheme = getRatingScheme("Test Rating Scheme");
        ratingScheme.setInitialIncrement(0);
        ratingScheme.setMainIncrement(0);
        try {
            logger.debug("Creating Rating Scheme with initial and main increment set to 0.");
            api.createRatingScheme(ratingScheme);
            fail("Rating Scheme cannot have both initial and main increment set to 0.");
        } catch (Exception e) { }

        ratingScheme = getRatingScheme("Test Rating Scheme");
        ratingScheme.setInitialIncrement(null);
        try {
            logger.debug("Creating Rating Scheme without initial increment.");
            api.createRatingScheme(ratingScheme);
            fail("Initial Increment is requiered");
        } catch (Exception e) { }

        ratingScheme = getRatingScheme("Test Rating Scheme");
        ratingScheme.setMainIncrement(null);
        try {
            logger.debug("Creating Rating Scheme without main increment.");
            api.createRatingScheme(ratingScheme);
            fail("Main Increment is requiered");
        } catch (Exception e) { }

        ratingScheme = getRatingScheme("Test Rating Scheme");
        try {
            logger.debug("Creating Rating Scheme with duplicated name.");
            api.createRatingScheme(ratingScheme);
            api.createRatingScheme(ratingScheme);
            fail("Main rating scheme can not have dupicated name.");
        } catch (Exception e) { }

        ratingScheme = getRatingScheme("Test Rating Scheme");
        addAssociation(ratingScheme);
        try {
            logger.debug("Associating two rating scheme for a mediation/company combination.");
            api.createRatingScheme(ratingScheme);

            ratingScheme = getRatingScheme("Test Rating Scheme");
            ratingScheme.setName("New Rating Scheme");
            addAssociation(ratingScheme);
            api.createRatingScheme(ratingScheme);
            fail("Mediation 4 for company 60 is already associated to a rating scheme.");
        } catch (Exception e) { }

    }

    @Test
    public void testGetRatingScheme() {
        try {
            logger.debug("Creating Rating Scheme to be retrieved.");
            Integer ratingScheme = api.createRatingScheme(getRatingScheme("Test Get Rating Scheme"));
            logger.debug("Getting Rating Scheme {}", ratingScheme);
            MediationRatingSchemeWS ratingSchemeWS = api.getRatingScheme(ratingScheme);
            assertNotNull(ratingSchemeWS);
        } catch (Exception e) {
            fail("Cannot retireve rating scheme.");
        }
    }

    @Test
    public void testDeleteRatingScheme() {
        try {
            logger.debug("Creating valid Rating Scheme.");
            Integer ratingSchemeId = api.createRatingScheme(getRatingScheme("Test Delete Rating Scheme"));
            logger.debug("Deleting persisted rating scheme.");
            Boolean deleted = api.deleteRatingScheme(ratingSchemeId);
            assertTrue(deleted);
        } catch (Exception e) {
            fail("Rating Scheme deletion failed.");
        }
    }

    private MediationRatingSchemeWS getRatingScheme(String name) {
        MediationRatingSchemeWS ratingSchemeWS = new MediationRatingSchemeWS();
        ratingSchemeWS.setName(name);
        ratingSchemeWS.setInitialIncrement(30);
        ratingSchemeWS.setInitialRoundingMode(BigDecimal.ROUND_UP);
        ratingSchemeWS.setMainIncrement(30);
        ratingSchemeWS.setMainRoundingMode(BigDecimal.ROUND_UP);
        ratingSchemeWS.setGlobal(false);
        ratingSchemeWS.setAssociations(new ArrayList<>());
        return ratingSchemeWS;
    }

    private void addAssociation(MediationRatingSchemeWS ratingScheme) {
        RatingSchemeAssociationWS associationWS = new RatingSchemeAssociationWS();
        associationWS.setCompany(new CompanyWS(60));
        MediationConfigurationWS mediation = new MediationConfigurationWS();
        mediation.setId(4);
        associationWS.setMediation(mediation);
        List<RatingSchemeAssociationWS> associationWSList = new ArrayList<>();
        associationWSList.add(associationWS);
        ratingScheme.setAssociations(associationWSList);
    }

}
