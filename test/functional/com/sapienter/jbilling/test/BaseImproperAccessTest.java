package com.sapienter.jbilling.test;

import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = {"improper-access", "web-services"})
public class BaseImproperAccessTest {

    protected JbillingAPI oscorpAdminApi;
    protected JbillingAPI oscorpCustomerApi;
    protected JbillingAPI frenchSpeakerApi;
    protected JbillingAPI pendunsus1Api;

    protected JbillingAPI o1AdminApi;
    protected JbillingAPI o1CustomerApi;

    protected JbillingAPI o2AdminApi;
    protected JbillingAPI o2CustomerApi;

    protected JbillingAPI capsuleAdminApi;
    protected JbillingAPI capsuleCustomerApi;

    protected JbillingAPI c1AdminApi;
    protected JbillingAPI c1CustomerApi;

    public static final String UNAUTHORIZED_ACCESS_TO_ID = "Unauthorized access to ID %d";
    public static final String INVALID_ERROR_MESSAGE = "Invalid error message!";
    protected static final String CROSS_CUSTOMER_ERROR_MSG = "Unauthorized access to entity %d for customer %d data by caller '%s'";
    protected static final String CROSS_COMPANY_ERROR_MSG = "Unauthorized access to entity %d by caller '%s'";
    protected static final String FRENCH_SPEAKER_LOGIN = "french-speaker;1";
    protected static final String PENDUNSUS_LOGIN = "pendunsus1;1";
    protected static final String MORDOR_LOGIN = "mordor;2";
    protected static final String ADMIN_LOGIN = "admin;1";


    @BeforeClass
    protected void initialize() throws Exception {
        oscorpAdminApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_OSCORP_ADMIN);
        oscorpCustomerApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_OSCORP_CUSTOMER);
        frenchSpeakerApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_FRENCH_SPEAKER);
        pendunsus1Api = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_PENDUNSUS1);

        o1AdminApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_O1_ADMIN);
        o1CustomerApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_O1_CUSTOMER);

        o2AdminApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_O2_ADMIN);
        o2CustomerApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_O2_CUSTOMER);

        capsuleAdminApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_CAPSULE_ADMIN);
        capsuleCustomerApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_CAPSULE_CUSTOMER);

        c1AdminApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_C1_ADMIN);
        c1CustomerApi = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_C1_CUSTOMER);
    }

    @AfterClass
    protected void cleanup() {
        oscorpAdminApi = null;
        oscorpCustomerApi = null;
        frenchSpeakerApi = null;
        pendunsus1Api = null;

        o1AdminApi = null;
        o1CustomerApi = null;

        o2AdminApi = null;
        o2CustomerApi = null;

        capsuleAdminApi = null;
        capsuleCustomerApi = null;

        c1AdminApi = null;
        c1CustomerApi = null;
    }
}