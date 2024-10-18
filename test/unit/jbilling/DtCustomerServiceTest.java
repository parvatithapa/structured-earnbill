package jbilling;



import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapienter.jbilling.appdirect.subscription.PayloadWS;
import com.sapienter.jbilling.appdirect.userCompany.CompanyPayload;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.WebServicesSessionSpringBean;


@RunWith(MockitoJUnitRunner.class)
public class DtCustomerServiceTest {


    @Autowired
    @InjectMocks
    private DtCustomerService dtCustomerService;

    @Mock
    WebServicesSessionSpringBean webServicesSession;


    ObjectMapper mapper = new ObjectMapper();
    PayloadWS payload;
    UriInfo uriInfo;

    private final String SESSIONINTERNALERROR_CUSTOMERNOTFOUNDMESSAGE="Customer not found for Id";
    private final String SESSIONINTERNALERROR_COMPANYNOTFOUND="Company not found with entityId";
    private final String testEntityIdentifier="e2af3a99-c85b-4dc9-b6db-a583b4199795";

    @Rule
    public ExpectedException thrown = ExpectedException.none();




    @Test
    public void testUpdateCompany_ParentAndChildCustomersUpdated() throws IOException{


        when(webServicesSession.getCompanyByMetaFieldValue(Mockito.anyString())).thenReturn(getCompanyWS());
        when(webServicesSession.getUserByCustomerMetaFieldAndCompanyId(Mockito.anyString(),Mockito.anyString(), Mockito.anyInt())).thenReturn(getExistingCustomer());
        Mockito.doNothing().when(webServicesSession).updateUserWithCompanyId(Mockito.any(UserWS.class),Mockito.anyInt());
        when(webServicesSession.getUsersByParentId(Mockito.anyInt())).thenReturn(getChildCustomerList());

        dtCustomerService.updateCompany(testEntityIdentifier,getPayLoad());

        Mockito.verify(webServicesSession, times(4)).updateUserWithCompanyId(Mockito.any(UserWS.class),Mockito.anyInt());


    }

    @Test
    public void testUpdateCompany_WhenParentCustomerDoesNotExist()throws SessionInternalError {


        when(webServicesSession.getCompanyByMetaFieldValue(Mockito.anyString())).thenReturn(getCompanyWS());
        when(webServicesSession.getUserByCustomerMetaFieldAndCompanyId(Mockito.anyString(),Mockito.anyString(), Mockito.anyInt())).thenReturn(null);
        Mockito.doNothing().when(webServicesSession).updateUserWithCompanyId(Mockito.any(UserWS.class),Mockito.anyInt());
        when(webServicesSession.getUsersByParentId(Mockito.anyInt())).thenReturn(getChildCustomerList());

        thrown.expect(SessionInternalError.class);
        thrown.expectMessage(startsWith(SESSIONINTERNALERROR_CUSTOMERNOTFOUNDMESSAGE));

        dtCustomerService.updateCompany(testEntityIdentifier,getPayLoad());

    }


    @Test
    public void testUpdateCompany_CompanyNotFound() throws IOException{


        when(webServicesSession.getCompanyByMetaFieldValue(Mockito.anyString())).thenReturn(null);
        when(webServicesSession.getUserByCustomerMetaFieldAndCompanyId(Mockito.anyString(),Mockito.anyString(), Mockito.anyInt())).thenReturn(null);
        Mockito.doNothing().when(webServicesSession).updateUserWithCompanyId(Mockito.any(UserWS.class),Mockito.anyInt());
        when(webServicesSession.getUsersByParentId(Mockito.anyInt())).thenReturn(getChildCustomerList());

        thrown.expect(SessionInternalError.class);
        thrown.expectMessage(startsWith(SESSIONINTERNALERROR_COMPANYNOTFOUND));

        dtCustomerService.updateCompany(testEntityIdentifier,getPayLoad());

    }


    @Test
    public void testRemoveUserSingleCall() throws IOException {

        String json = "{\"uuid\":\"0b326b16-78fe-4fca-83f3-314ad567b4b8\",\"timestamp\":1517825557145,\"resource\":{\"type" +
                "\":\"SUBSCRIPTION\",\"uuid\":\"a4ace961-2423-4f28-b09d-af98a7a80e99\"},\"resourceAction\":\"REMOVED\"}\n";
        CompanyWS companyWS = new CompanyWS();
        companyWS.setId(1);
        UserWS existingCustomer = new UserWS();
        existingCustomer.setUserId(10);
        payload = mapper.readValue(json,PayloadWS.class);
        String subscriptionId = payload.getResource().getUuid();

        Mockito.when(webServicesSession.getCompanyByMetaFieldValue(Mockito.anyString())).thenReturn(companyWS);
        Mockito.when(webServicesSession.getUserByCustomerMetaFieldAndCompanyId(subscriptionId,
                "appdirectSubscriptionIdentifier",companyWS.getId())).thenReturn(existingCustomer);
        Mockito.doNothing().when(webServicesSession).removeUser(Mockito.anyInt(),Mockito.anyInt());

        dtCustomerService.deleteDTCustomer("dummy",payload);

        Mockito.verify(webServicesSession, Mockito.times(1)).removeUser(Mockito.anyInt(),Mockito.anyInt());
    }

    @Test(expected = SessionInternalError.class)
    public void testWithSubscriptionIdNotFound() throws IOException{
        String json = "{\"uuid\":\"0b326b16-78fe-4fca-83f3-314ad567b4b8\",\"timestamp\":1517825557145,\"resource\":{\"type\"" +
                ":\"SUBSCRIPTION\",\"uuid\":\"a4ace961-2423-4f28-b09d-af98a7a80e9\"},\"resourceAction\":\"REMOVED\"}\n";
        CompanyWS companyWS = new CompanyWS();
        companyWS.setId(1);
        UserWS existingCustomer = new UserWS();
        existingCustomer.setUserId(10);
        payload = mapper.readValue(json,PayloadWS.class);
        String subscriptionId = payload.getResource().getUuid();

        Mockito.when(webServicesSession.getCompanyByMetaFieldValue(Mockito.anyString())).thenReturn(companyWS);
        Mockito.when(webServicesSession.getUserByCustomerMetaFieldAndCompanyId("a4ace961-2423-4f28-b09d-af98a7a80e99",
                "appdirectSubscriptionIdentifier",companyWS.getId())).thenReturn(existingCustomer);
        Mockito.doNothing().when(webServicesSession).removeUser(Mockito.anyInt(),Mockito.anyInt());

        dtCustomerService.deleteDTCustomer("dummy",payload);

    }

    @Test(expected = SessionInternalError.class)
    public void testWithCompanyIdNotFound() throws IOException{
        String json = "{\"uuid\":\"0b326b16-78fe-4fca-83f3-314ad567b4b8\",\"timestamp\":1517825557145,\"resource\":" +
                "{\"type\":\"SUBSCRIPTION\",\"uuid\":\"a4ace961-2423-4f28-b09d-af98a7a80e99\"},\"resourceAction\":\"REMOVED\"}\n";
        CompanyWS companyWS = new CompanyWS();
        companyWS.setId(1);
        UserWS existingCustomer = new UserWS();
        existingCustomer.setUserId(10);
        payload = mapper.readValue(json,PayloadWS.class);
        String subscriptionId = payload.getResource().getUuid();

        Mockito.when(webServicesSession.getCompanyByMetaFieldValue(Mockito.anyString())).thenReturn(null);
        Mockito.when(webServicesSession.getUserByCustomerMetaFieldAndCompanyId(subscriptionId,
                "appdirectSubscriptionIdentifier",companyWS.getId())).thenReturn(existingCustomer);
        Mockito.doNothing().when(webServicesSession).removeUser(Mockito.anyInt(),Mockito.anyInt());

        dtCustomerService.deleteDTCustomer("dummy",payload);
    }

    @Test(expected = SessionInternalError.class)
    public void testExceptionRemoveUserCall() throws IOException{

        String json = "{\"uuid\":\"0b326b16-78fe-4fca-83f3-314ad567b4b8\",\"timestamp\":1517825557145,\"resource\":{\"type" +
                "\":\"SUBSCRIPTION\",\"uuid\":\"a4ace961-2423-4f28-b09d-af98a7a80e99\"},\"resourceAction\":\"REMOVED\"}\n";
        CompanyWS companyWS = new CompanyWS();
        companyWS.setId(1);
        UserWS existingCustomer = new UserWS();
        existingCustomer.setUserId(10);
        payload = mapper.readValue(json,PayloadWS.class);
        String subscriptionId =  payload.getResource().getUuid();

        Mockito.when(webServicesSession.getCompanyByMetaFieldValue(Mockito.anyString())).thenReturn(companyWS);
        Mockito.when(webServicesSession.getUserByCustomerMetaFieldAndCompanyId(subscriptionId,
                "appdirectSubscriptionIdentifier",companyWS.getId())).thenReturn(existingCustomer);
        Mockito.doThrow(new SessionInternalError()).when(webServicesSession).removeUser(Mockito.anyInt(),Mockito.anyInt());

        dtCustomerService.deleteDTCustomer("dummy",payload);
    }



    /*Utility methods*/
      private UserWS getExistingCustomer()
    {
        UserWS customer=new UserWS();
        customer.setUserId(1);
        return customer;

    }
    private CompanyWS getCompanyWS()
    {
        return new CompanyWS(1);
    }

    private List<UserWS> getChildCustomerList()
    {
        List childCustomers=new ArrayList<UserWS>();

        childCustomers.add(new UserWS());
        childCustomers.add(new UserWS());
        childCustomers.add(new UserWS());
        return childCustomers;
    }

    private CompanyPayload getPayLoad()
    {
        CompanyPayload payloadObject=null;
        try {
            String payload = "{\"uuid\":\"894d78c0-df33-4c7a-9aa1-40ac8664bffb\",\"timestamp\":1517828626291,\"resource\":{\"type\":\"COMPANY\",\"" +
                    "uuid\":\"dad75d7a-8aba-4482-873f-fb349bcb6c0b\",\"url\":\"https://od-fkf7rwt6stelekom.od4.appdirectondemand.com/api/account/v1/companies" +
                    "/dad75d7a-8aba-4482-873f-fb349bcb6c0b?isExternalId=false\",\"content\":{\"id\":\"dad75d7a-8aba-4482-873f-fb349bcb6c0b\"," +
                    "\"name\":\"JETe\",\"enabled\":true,\"contact\":null,\"size\":null,\"status\":\"ACTIVE\",\"attributes\":null," +
                    "\"creationDate\":1517815566000,\"industry\":null,\"salesAgent\":null,\"website\":\"yopmail.com\",\"emailAddress\":null," +
                    "\"dealer\":null,\"uuid\":\"dad75d7a-8aba-4482-873f-fb349bcb6c0b\",\"externalId\":null,\"countryCode\":\"US\",\"permissions\":[]," +
                    "\"defaultIdpUuid\":null,\"domains\":null}},\"resourceAction\":\"CHANGED\"}\n";
            ObjectMapper mapper = new ObjectMapper();
            payloadObject = mapper.readValue(payload, CompanyPayload.class);

        }
        finally {
            return payloadObject;
        }
    }
}
