/*
* JBILLING CONFIDENTIAL
* _____________________
*
* [2003] - [2012] Enterprise jBilling Software Ltd.
* All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Enterprise jBilling Software.
* The intellectual and technical concepts contained
* herein are proprietary to Enterprise jBilling Software
* and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden.
*/
package com.sapienter.jbilling.server.diameter;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import org.apache.commons.lang.RandomStringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class BaseDiameterTest {
    public static final int ITEM_TYPE_CALLS = 2201;

    public static final String SUBSCRIPTION_DATA_INVALID_USER = "INVALID USER";
    public static final String INVALID_DESTINATION_REALM = "DEF";
    public static final String DESTINATION_REALM = "realm";
    public static final String USERNAME = "diameterTest";
    public static final Integer QUOTA_THRESHOLD = new Integer(2);

    protected JbillingAPI API = null;

    public PricingField[] createData(String destinationRealm,
    		String subscriptionIdData, String calledPartyAddress,
    		String ratingGroup) {

        List<PricingField> result = new ArrayList<PricingField>();

        if (destinationRealm != null) {
            result.add(new PricingField(Input.DESTINATION_REALM, destinationRealm));
        }

        if (subscriptionIdData != null) {
        	result.add(new PricingField(Input.SUBSCRIPTION_ID_DATA, subscriptionIdData));
        }

        if (calledPartyAddress != null) {
        	result.add(new PricingField(Input.CALLED_PARTY_ADDRESS, calledPartyAddress));
        }

        if (ratingGroup != null) {
        	result.add(new PricingField(Input.RATING_GROUP, ratingGroup));
        }

        return result.toArray(new PricingField[0]);
    }

    public UserWS createUser (BigDecimal balance) {
        String username = USERNAME + "-" + new Date().getTime();

        //Create a user
        UserWS user = CreateObjectUtil.createCustomer(new Integer(1), username, "0fu3js8wl1a$e2wxRQ", new Integer(1), new Integer(5), false, UserDTOEx.STATUS_ACTIVE, null, null, null);
        user.setAccountTypeId(Integer.valueOf(1));

        MetaFieldValueWS meta = new MetaFieldValueWS();
        meta.setFieldName("Subscriber URI");
        meta.setStringValue(user.getUserName());

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(username + "@shire.com");
        metaField3.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[]{meta, metaField3});



        user.setUserId(API.createUser(user));

        if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
            //The user makes a deposit
        	try(PaymentInformationWS cheque = com.sapienter.jbilling.server.user.WSTest.
        			createCheque("ws bank", "2232-2323-2323", Calendar.getInstance().getTime())) {


                PaymentWS payment = CreateObjectUtil.createPaymentObject(user.getUserId(), balance, new Integer(1), false, Constants.PAYMENT_METHOD_CHEQUE, new Date(), "Notes", cheque);
                API.applyPayment(payment, new Integer(35));
            }catch (Exception exception){
                // log exception
            }

        }

        return user;
    }

    public ItemDTOEx createItem(BigDecimal price) {
        ItemDTOEx newItem = new ItemDTOEx();

        newItem.setDescription("an item from ws");
        newItem.setPrice(price);
        newItem.setCurrencyId(1);
        newItem.setHasDecimals(1);
        String code = "DIAMETER-TEST-" + RandomStringUtils.randomAlphabetic(15);
        newItem.setNumber(code);

        Integer types[] = new Integer[1];
        types[0] = new Integer(ITEM_TYPE_CALLS);
        newItem.setTypes(types);

        newItem.setId(API.createItem(newItem));

        return newItem;
    }

    public void deleteItem(Integer itemId){
        API.deleteItem(itemId);
    }

    public void deleteUser(Integer userId) {
        API.deleteUser(userId);
    }

    public String getUniqueSessionId() {
    	return RandomStringUtils.randomAlphanumeric(10);
    }

    public boolean assertLineQuantityPrice(UserWS user, BigDecimal quantity, BigDecimal price) {
        OrderWS order = API.getCurrentOrder(new Integer(user.getUserId()), new Date());
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;
        OrderLineWS[] lines = order.getOrderLines();

        for (int i = 0; i < lines.length; i++) {
            OrderLineWS line = lines[i];

            totalPrice = totalPrice.add(line.getAmountAsDecimal());
            totalQuantity = totalQuantity.add(line.getQuantityAsDecimal());
        }

        return (quantity.compareTo(totalQuantity) == 0 && price.compareTo(totalPrice) == 0);
    }

    public BigDecimal getTotalOrder(UserWS user) {
        return API.getCurrentOrder(new Integer(user.getUserId()), new Date()).getTotalAsDecimal();
    }

    public BigDecimal getDynamicBalanceAsDecimal(UserWS user) {
        return (user != null) ? getDynamicBalanceAsDecimal(new Integer(user.getUserId())) : BigDecimal.ZERO;
    }

    public BigDecimal getDynamicBalanceAsDecimal(Integer userId) {
        return API.getUserWS(userId).getDynamicBalanceAsDecimal();
    }

    public interface Input {
        public static final String DESTINATION_REALM = "Destination-Realm";
        public static final String SUBSCRIPTION_ID_DATA = "Subscription-Id-Data";
        public static final String CALLED_PARTY_ADDRESS = "Called-Party-Address";
        public static final String RATING_GROUP = "Rating-Group";
    }

    public static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
    	assertTrue("expected: <" + expected.toPlainString() + "> but was: <" + actual.toPlainString() + ">",
    			expected.compareTo(actual) == 0);
    }
}
