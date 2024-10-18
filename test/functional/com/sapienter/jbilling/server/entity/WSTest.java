package com.sapienter.jbilling.server.entity;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafield.builder.ValidationRuleBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.validation.ScriptValidationRuleModel;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.RemoteContext;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.ApiTestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.testng.AssertJUnit.*;

@Test(groups = {"web-services", "entity"}, testName = "entity.WSTest")
public class WSTest extends ApiTestCase {

    private static JbillingAPI apiSysAdmin;
    private static JbillingAPI apiSuperUser;
    public static final int COMPANIES_QUANTITY = 6;

    @BeforeClass
    public void setUp() throws Exception {
        apiSysAdmin = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_SYSADMIN.getName());
        apiSuperUser = JbillingAPIFactory.getAPI(RemoteContext.Name.API_CLIENT_MORDOR.getName());
    }

    @Test
    public void test001GetCompaniesAsSystemAdmin() {
        //We retrieve the companies as System Admin and we should get an answer with all of them.
        //Prancing Pony, Mordor Inc., Reseller Organization and Child Company, Second Child Reseller, Mordor Child Company
        CompanyWS[] companies = apiSysAdmin.getCompanies();

        Assert.assertEquals(COMPANIES_QUANTITY, companies.length);
    }


    @Test(expectedExceptions = SessionInternalError.class)
    public void test002GetCompaniesAsOtherThanSystemAdmin() {
        //We try to retrieve the companies as other than a System Admin and we should get an exception.
        apiSuperUser.getCompanies();
    }
}
