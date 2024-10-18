package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.tools.UploadOrders;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Unit testing the tool that is used for bulk creation
 * of orders.
 */
@Test(groups = { "upload", "order" }, testName = "UploadOrdersTest")
public class UploadOrdersTest {

    @Test
    public void testOrderUpload() throws Exception {

        //create a user with username uploadOrdersTestUser
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = createUser();
        Integer userId = api.createUser(user);

        //run the upload procedure with the given
        //properties file and a data file
        String uploadPropertiesFile = "upload.properties";
        String uploadFileName = "upload_orders.csv";

        URL propertiesFile = Thread.currentThread()
                .getContextClassLoader().getResource(uploadPropertiesFile);
        URL uploadFile = Thread.currentThread()
                .getContextClassLoader().getResource(uploadFileName);

        String [] args = new String [] {
                propertiesFile.getPath(),  uploadFile.getPath()};

        UploadOrders uploadOrders = new UploadOrders();
        uploadOrders.upload(args);

        // check if the order data were correctly imported
        List<Integer> orderIds = Arrays.asList(api.getLastOrders(userId, 10));
        Collections.sort(orderIds);

        assertTrue("there should be 3 orders uploaded for this user", 3 == orderIds.size());

        OrderWS order = api.getOrder(orderIds.get(0));

        assertEquals("the item id of the first order line is not correct",
                Integer.valueOf(2800), order.getOrderLines()[0].getItemId());
        assertEquals("quantity for first order is not correct",
                BigDecimal.valueOf(2), order.getOrderLines()[0].getQuantityAsDecimal());
        assertEquals("the total order amount is not correct",
                BigDecimal.ZERO, order.getOrderLines()[0].getAmountAsDecimal());

        api.deleteOrder(orderIds.get(0));


        order = api.getOrder(orderIds.get(1));

        assertEquals("the item id should be 3",
                Integer.valueOf(2801), order.getOrderLines()[0].getItemId());
        assertEquals("quantity for first order is not correct",
                BigDecimal.valueOf(2), order.getOrderLines()[0].getQuantityAsDecimal());
        assertEquals("the total order amount is not correct",
                BigDecimal.ZERO, order.getOrderLines()[0].getAmountAsDecimal());

        api.deleteOrder(orderIds.get(1));

        order = api.getOrder(orderIds.get(2));

        assertEquals("the item id should be 4",
                Integer.valueOf(2801), order.getOrderLines()[0].getItemId());
        assertEquals("quantity for first order is not correct",
                BigDecimal.valueOf(2), order.getOrderLines()[0].getQuantityAsDecimal());
        assertEquals("the total order amount is not correct",
                new BigDecimal(2.2), order.getOrderLines()[0].getAmountAsDecimal());

        api.deleteOrder(orderIds.get(2));
        api.deleteUser(userId);

    }

    private UserWS createUser() {

        UserWS newUser = new UserWS();
        newUser.setUserName("uploadOrdersTestUser");
        newUser.setPassword("As$fasdf1");
        newUser.setLanguageId(new Integer(1));
        newUser.setMainRoleId(new Integer(5));
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(null);

        // add a contact
        ContactWS contact = new ContactWS();
        contact.setEmail(newUser.getUserName() + "@jbilling.com");
        contact.setFirstName("Uploader");
        contact.setLastName("OfOrders");
        newUser.setContact(contact);

        return newUser;
    }

}
