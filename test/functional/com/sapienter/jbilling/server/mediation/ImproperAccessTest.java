package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/7/15.
 */
@Test(testName = "mediation.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private UUID mediation_process_id = null;
    private static int MEDIATION_CONFIGURATION_P_PONY;
    private final static int PRANCING_PONY_ORDER_ID = 100;
    private final static int PRANCING_PONY_INVOICE_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @BeforeClass
        public void initializeTests() {
                MediationConfigurationWS config = new MediationConfigurationWS();
                config.setName("WS - Oscorp Mediation Job");
                config.setMediationJobLauncher("sampleMediationJob");
                config.setGlobal(Boolean.FALSE);
                config.setOrderValue("1001");
                config.setPluggableTaskId(421);
                MEDIATION_CONFIGURATION_P_PONY = oscorpAdminApi.createMediationConfiguration(config);
        }

    @Test
    public void testUndoMediation() {
        this.triggerMediationProcess();
        // Cross Company
        try {
            capsuleAdminApi.undoMediation(this.mediation_process_id);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetMediationEventsForOrder() {
        // Cross Company
        try {
            capsuleAdminApi.getMediationEventsForOrder(PRANCING_PONY_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getMediationEventsForOrder(PRANCING_PONY_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getMediationEventsForOrder(PRANCING_PONY_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }
    }

    @Test
    public void testGetMediationEventsForOrderDateRange() {
        // Cross Company
        try {
            capsuleAdminApi.getMediationEventsForOrderDateRange(PRANCING_PONY_ORDER_ID, null, null, 0, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getMediationEventsForOrderDateRange(PRANCING_PONY_ORDER_ID, null, null, 0, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getMediationEventsForOrderDateRange(PRANCING_PONY_ORDER_ID, null, null, 0, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }
    }

    @Test
    public void testGetMediationEventsForInvoice() {
        // Cross Company
        try {
            capsuleAdminApi.getMediationEventsForInvoice(PRANCING_PONY_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getMediationEventsForInvoice(PRANCING_PONY_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getMediationEventsForInvoice(PRANCING_PONY_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString("Unauthorized access"));
        }
    }

    @Test
    public void testGetMediationRecordsByMediationProcess() {
        this.triggerMediationProcess();

        // Cross Company
        try {
            capsuleAdminApi.getMediationRecordsByMediationProcess(this.mediation_process_id, null, 1, null, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteMediationConfiguration() {
        // Cross Company
        try {
            capsuleAdminApi.deleteMediationConfiguration(MEDIATION_CONFIGURATION_P_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetMediationErrorRecordsCount() {
        // Cross Company
        try {
            capsuleAdminApi.getMediationErrorRecordsCount(MEDIATION_CONFIGURATION_P_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    private void triggerMediationProcess() {
        if(this.mediation_process_id == null) {
            this.mediation_process_id = oscorpAdminApi.triggerMediationByConfiguration(MEDIATION_CONFIGURATION_P_PONY);
            try{
                while(oscorpAdminApi.isMediationProcessRunning()){
                    Thread.sleep(3000);
                }
            } catch (InterruptedException ie ){}
        }
    }
}
