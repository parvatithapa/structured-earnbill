package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.api.SpringAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Vladimir Carevski
 * @since 28-JAN-2014
 */
@Test(groups = {"web-services", "mediation"}, testName = "GlobalMediationTest", priority = 16)
public class GlobalMediationTest {

    private JbillingAPI parentApi;
    private JbillingAPI childApi;

    private static final Logger logger = LoggerFactory.getLogger(GlobalMediationTest.class);
    private static final Integer MEDIATION_CHILD_ID = Integer.valueOf(10810);
    private static final Integer MEDIATION_PARENT_ID = Integer.valueOf(10813);
    private static final Integer ONE_TIME = Integer.valueOf(1);

    @BeforeClass
    protected void setUp() throws Exception {
        parentApi = JbillingAPIFactory.getAPI();
        childApi = new SpringAPI(RemoteContext.Name.API_CHILD_CLIENT);
    }

    @Test
    public void test001ConfigurationCrud() throws Exception {
        MediationConfigurationWS config = new MediationConfigurationWS();
        config.setName("WS - Test Mediation Job");
        config.setMediationJobLauncher("sampleMediationJob");
        config.setGlobal(Boolean.FALSE);
        config.setOrderValue("1001");
        config.setPluggableTaskId(421);

        logger.debug("001.1 creating mediation configuration for parent");
        Integer configId = parentApi.createMediationConfiguration(config);

        try {
            //child tries to delete mediation configuration from the root
            logger.debug("001.2 delete parent mediation config with child API call");
            childApi.deleteMediationConfiguration(configId);
            fail("child should not be able to delete parent mediation configurations");
        } catch (SecurityException | SessionInternalError se) {
        }

        logger.debug("001.3 delete parent mediation config with parent API call");
        parentApi.deleteMediationConfiguration(configId);

        //a global mediation configuration
        config.setGlobal(Boolean.TRUE);
        try {
            //try registering a global mediation config for child
            logger.debug("001.4 create global mediation configuration with child API call");
            childApi.createMediationConfiguration(config);
            fail("child should not be able to register global mediation configurations");
        } catch (SessionInternalError sie) {
        }

        //try registering the global mediation process to the parent
        logger.debug("001.5 create global mediation configuration with parent API call");
        configId = parentApi.createMediationConfiguration(config);

        try {
            logger.debug("001.6 delete global mediation configuration with child API call");
            childApi.deleteMediationConfiguration(configId);
            fail("Child company should not be able to delete parent mediation configurations");
        } catch (SecurityException | SessionInternalError sie) {
        }

        //cleanup, remove the mediation configuration
        logger.debug("001.7 delete global mediation configuration with parent API call");
        parentApi.deleteMediationConfiguration(configId);
    }

    @Test
    public void test002ConfigurationCrud() throws Exception {
        MediationConfigurationWS config = new MediationConfigurationWS();
        config.setName("WS - Test Mediation Job");
        config.setMediationJobLauncher("sampleMediationJob");
        config.setGlobal(Boolean.FALSE);
        config.setOrderValue("1001");
        config.setPluggableTaskId(421);

        logger.debug("002.1 creating non global mediation configuration for parent company");
        Integer configId = parentApi.createMediationConfiguration(config);

        logger.debug("002.2 retrieve mediation configuration available to child company");
        MediationConfigurationWS[] mediationConfigurations =
                childApi.getAllMediationConfigurations();

        boolean found = false;
        for (MediationConfigurationWS cfg : mediationConfigurations) {
            if (cfg.getId().equals(configId)) {
                found = true;
            }
        }

        assertFalse("Non Global parent mediation configurations should not be visible " +
                "to the child", found);

        logger.debug("002.3 delete non global mediation configuration for parent company");
        parentApi.deleteMediationConfiguration(configId);

        config.setGlobal(Boolean.TRUE);
        logger.debug("002.4 create global mediation configuration for parent company");
        configId = parentApi.createMediationConfiguration(config);

        logger.debug("002.5 retrieve mediation configuration available to child company");
        mediationConfigurations = childApi.getAllMediationConfigurations();

        found = false;
        MediationConfigurationWS globalMediation = null;
        for (MediationConfigurationWS cfg : mediationConfigurations) {
            if (cfg.getId().equals(configId)) {
                globalMediation = cfg;
                found = true;
            }
        }

        assertTrue("Child company should be able to see the global mediation configurations", found);

        globalMediation.setGlobal(Boolean.FALSE);
        globalMediation.setName("Child tries to change existing global mediation config");

        try {
            logger.debug("002.6 update global mediation configuration via child company");
            childApi.updateAllMediationConfigurations(Arrays.asList(globalMediation));
            fail("Child should not be able update global mediation configs. Should throw security exception");
        } catch (SecurityException | SessionInternalError sie) {
        }

        //do cleanup and remove create mediation configurations
        logger.debug("002.7 delete create mediation configuration for parent");
        parentApi.deleteMediationConfiguration(configId);
    }

}
