package com.sapienter.jbilling.server.company;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import static junit.framework.Assert.assertNotNull;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import java.util.List;
import static org.junit.Assert.assertEquals;

@Test(groups = {"web-services", "company"}, testName = "CopyCompanyTest")
public class CopyCompanyTest {

    private static final Logger logger = LoggerFactory.getLogger(CopyCompanyTest.class);
    private static JbillingAPI api = null;
    private TestBuilder testBuilder;
    private EnvironmentHelper envHelper;
    private Integer companyId = null;
    private String childCompanyTemplateName = null;
    private List<String> importEntities = null;
    private boolean isCompanyChild = false;
    private boolean copyProducts = false;
    private boolean copyPlans = false;
    private String adminEmail = "raushankumar.raj@sarathisoftech.com";
    private String systemAdminLoginname = "raushankumar.raj@sarathisoftech.com";

    @BeforeClass
    public void setup() {
        testBuilder = getTestEnvironment();
        api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        companyId = api.getCompany().getId();

    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
    }

    @Test
    public void copyCompanyTestUniqueLoginNameTrue() {
        logger.debug("Creating new company");
        UserWS userWS = api.copyCompanyInSaas(childCompanyTemplateName,
                companyId,
                importEntities,
                isCompanyChild,
                copyProducts,
                copyPlans,
                adminEmail,
                systemAdminLoginname
        );
        assertNotNull(userWS);
        logger.debug("Created new company with company name " + userWS.getCompanyName());
    }

    @Test
    public void copyCompanyTestEmptyEmailField() {
        logger.debug("Creating company with passing whitespace");
        String adminEmail = " ";
        try {
            UserWS userWS = api.copyCompanyInSaas(childCompanyTemplateName,
                    companyId,
                    importEntities,
                    isCompanyChild,
                    copyProducts,
                    copyPlans,
                    adminEmail,
                    systemAdminLoginname
            );
        } catch (SessionInternalError sessionInternalError) {
            String expected = "copy.company.admin.email.not.blank";
            String actual = sessionInternalError.getErrorMessages()[0].toString().trim();
            logger.info(actual);
            assertEquals("The exception message it not same", expected, actual);
        }
    }

    @Test
    public void copyCompanyTestInvalidEmailFormat() {
        logger.debug("Creating company with passing invalid email format");
        String adminEmail = "Raushan#sarathisoftech.com";
        try {
            UserWS userWS = api.copyCompanyInSaas(childCompanyTemplateName,
                    companyId,
                    importEntities,
                    isCompanyChild,
                    copyProducts,
                    copyPlans,
                    adminEmail,
                    systemAdminLoginname
            );
        } catch (SessionInternalError sessionInternalError) {
            String expected = "copy.company.admin.email.not.valid";
            String actual = sessionInternalError.getErrorMessages()[0].toString().trim();
            logger.info(actual);
            assertEquals("The exception message it not same", expected, actual);
        }
    }
}



