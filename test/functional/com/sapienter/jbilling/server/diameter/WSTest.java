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
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.*;

@Test(groups = {"diameter"}, testName = "diameter.WSTest")
public class WSTest extends BaseDiameterTest {

    private static final Logger logger = LoggerFactory.getLogger(WSTest.class);

    private static final int THRESHOLD_PREFERENCE_ID = 65;
    private static final int REALM_PREFERENCE_ID = 64;
    private static final int REAPER_THRESHOLD_PREF_ID = 66;

    private String originalRealm;
    private String originalThreshold;

    @BeforeClass
    public void setup() throws Exception {
        API = JbillingAPIFactory.getAPI();

        // Saves original preference values for realm code and threshold
        PreferenceWS realmPref = API.getPreference(REALM_PREFERENCE_ID);
        PreferenceWS thresholdPref = API.getPreference(THRESHOLD_PREFERENCE_ID);
        originalRealm = realmPref.getValue();
        originalThreshold = thresholdPref.getValue();

        // Sets test preferences for realm and threshold
        realmPref.setValue(DESTINATION_REALM);
        API.updatePreference(realmPref);
        thresholdPref.setValue(QUOTA_THRESHOLD.toString());
        API.updatePreference(thresholdPref);
    }

    @AfterClass
    public void tearOff() throws Exception {

        // Restores original values for realm and threshold
        PreferenceWS realmPref = API.getPreference(REALM_PREFERENCE_ID);
        PreferenceWS thresholdPref = API.getPreference(THRESHOLD_PREFERENCE_ID);
        realmPref.setValue(originalRealm);
        API.updatePreference(realmPref);
        thresholdPref.setValue(originalThreshold);
        API.updatePreference(thresholdPref);
    }

    @Test
    public void invalidUser() {
        PricingField[] data = createData(DESTINATION_REALM,
                SUBSCRIPTION_DATA_INVALID_USER, null, null);
        DiameterResultWS result = API.createSession(getUniqueSessionId(),
                new Date(), new BigDecimal("60"), data);
        assertEquals(DiameterResultWS.DIAMETER_USER_UNKNOWN,
                result.getResultCode());
    }

    @Test
    public void validUserAndInvalidSession() {
        // Create a user with balance $0
        UserWS user = createUser(BigDecimal.TEN);

        // Precondition: Verify that the balance is $10.
        assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));
        DiameterResultWS result = API.extendSession(getUniqueSessionId(),
                new Date(), new BigDecimal("30"), new BigDecimal("60"));
        assertEquals(DiameterResultWS.DIAMETER_UNKNOWN_SESSION_ID,
                result.getResultCode());
        deleteUser(new Integer(user.getUserId()));
    }

    @Test
    public void creditLimitReached() {
        // Create a user with balance $0
        UserWS user = createUser(BigDecimal.ZERO);

        // Precondition: Verify that the balance is $0.
        assertBigDecimalEquals(BigDecimal.ZERO,
                getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                null, item.getNumber());
        String sessionId = getUniqueSessionId();

        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("60"), data);

        assertEquals(DiameterResultWS.DIAMETER_CREDIT_LIMIT_REACHED,
                result.getResultCode());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getGrantedUnits());
        assertFalse(result.isTerminateWhenConsumed());
        assertEquals(Integer.valueOf(0),
                Integer.valueOf(result.getQuotaThreshold()));

        // Verify that the balance is still $0.
        assertBigDecimalEquals(BigDecimal.ZERO,
                getDynamicBalanceAsDecimal(user));
        deleteItem(item.getId());
        deleteUser(new Integer(user.getUserId()));
    }

    @Test
    public void invalidRealm() {
        // Create a user with balance $0
        UserWS user = createUser(BigDecimal.TEN);

        // Precondition: Verify that the balance is $10.
        assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));

        PricingField[] data = createData(INVALID_DESTINATION_REALM,
                user.getUserName(), null, null);
        DiameterResultWS result = API.createSession(getUniqueSessionId(),
                new Date(), new BigDecimal("60"), data);

        assertEquals(DiameterResultWS.DIAMETER_REALM_NOT_SERVED,
                result.getResultCode());

        deleteUser(new Integer(user.getUserId()));
    }

    @Test
    public void simpleShortCall() {
        // Create a user and makes a deposit of $10.00
        UserWS user = createUser(BigDecimal.TEN);

        // Precondition: Verify that the balance is $10.
        assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        logger.debug("User: {}", user.getUserName());
        // Create new session requesting 60 units
        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                null, item.getNumber());

        String sessionId = getUniqueSessionId();
        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("60"), data);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("60"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));

        // Verify that the balance drops to 10 - 60x0.01 = 10 - 0.6 = $9.4.
        assertBigDecimalEquals(new BigDecimal("9.4"),
                getDynamicBalanceAsDecimal(user));

        // Terminate the session using 30 units
        API.endSession(sessionId, new Date(), new BigDecimal("30"), 0);

        // Verify that the balance returns to 10 - 30x0.01 = 10 - 0.3 = $9.7
        assertBigDecimalEquals(new BigDecimal("9.7"),
                getDynamicBalanceAsDecimal(user));
        // assert that current order now has a line with 30 units and priced
        // 0.30$
        assertBigDecimalEquals(new BigDecimal("0.30"), getTotalOrder(user));

        deleteItem(item.getId());
        deleteUser(new Integer(user.getUserId()));
    }

    @Test
    public void simpleExtendedCall() {
        // Create a user and makes a deposit of $10.00
        UserWS user = createUser(BigDecimal.TEN);

        // Precondition: Verify that the balance is $10.
        assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        // Create new session requesting 60 units
        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                null, item.getNumber());
        String sessionId = getUniqueSessionId();

        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("60"), data);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("60"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Verify that the balance drops to 10 - 60x0.01 = 10 - 0.6 = $9.4.
        assertBigDecimalEquals(new BigDecimal("9.4"),
                getDynamicBalanceAsDecimal(user));

        result = API.extendSession(sessionId, new Date(), new BigDecimal("60"),
                new BigDecimal("60"));

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("60"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Verify that the balance drops to 9.4 - 60x0.01 = 9.4 - 0.6 = $8.8.
        assertBigDecimalEquals(new BigDecimal("8.8"),
                getDynamicBalanceAsDecimal(user));

        // Terminate the session using 40 units
        API.endSession(sessionId, new Date(), new BigDecimal("40"), 0);

        // Verify that the balance drops to 9.4 - 40x0.01 = 9.4 - 0.4 = $9
        assertBigDecimalEquals(new BigDecimal("9"),
                getDynamicBalanceAsDecimal(user));
        // assert that the current order now has a line with 100 units and
        // priced 1$
        assertTrue(assertLineQuantityPrice(user, new BigDecimal("100"),
                new BigDecimal("1")));

        deleteItem(item.getId());
        deleteUser(new Integer(user.getUserId()));
    }

    @Test
    public void simpleExtendedCallWithDecimalQuantity() {
        // Create a user and makes a deposit of $10.00
        UserWS user = createUser(BigDecimal.TEN);

        // Precondition: Verify that the balance is $10.
        assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.50"));

        // Create new session requesting 60 units
        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                null, item.getNumber());
        String sessionId = getUniqueSessionId();

        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("1"), data);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("1"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Verify that the balance drops to 10 - 1x0.50 = 10 - 0.50 = $9.50.
        assertBigDecimalEquals(new BigDecimal("9.50"),
                getDynamicBalanceAsDecimal(user));

        // Extend the session a couple of times.
        // First extension
        result = API.extendSession(sessionId, new Date(), new BigDecimal("0.85"),
                new BigDecimal("1"));

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("1"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Verify that the balance drops to 9.4 - 0.85x0.50 = 9.50 - 0.425 = $9.075.
        assertBigDecimalEquals(new BigDecimal("9.075"),
                getDynamicBalanceAsDecimal(user));

        // Second extension
        result = API.extendSession(sessionId, new Date(), new BigDecimal("0.85"),
                new BigDecimal("1"));

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("1"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Verify that the balance drops to 9.075 - 0.85x0.50 = 9.075 - 0.425 = $8.65.
        assertBigDecimalEquals(new BigDecimal("8.65"),
                getDynamicBalanceAsDecimal(user));

        // Third extension
        result = API.extendSession(sessionId, new Date(), new BigDecimal("0.85"),
                new BigDecimal("1"));

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("1"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Verify that the balance drops to 8.65 - 0.85x0.50 = 8.65 - 0.425 = $8.225.
        assertBigDecimalEquals(new BigDecimal("8.225"),
                getDynamicBalanceAsDecimal(user));

        // Terminate the session using 0.85
        API.endSession(sessionId, new Date(), new BigDecimal("0.85"), 0);

        // Verify that the balance drops to 8
        assertBigDecimalEquals(new BigDecimal("8"),
                getDynamicBalanceAsDecimal(user));
        // assert that the current order now has a line with 4 units and
        // priced 2$
        assertTrue(assertLineQuantityPrice(user, new BigDecimal("4"),
                new BigDecimal("2")));

        deleteItem(item.getId());
        deleteUser(new Integer(user.getUserId()));
    }

    @Test
    public void simpleUpdatedCall() {
        // Create a user and makes a deposit of $10.00
        UserWS user = createUser(BigDecimal.TEN);

        // Precondition: Verify that the balance is $10.
        assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit. RG1
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        // Create new item priced $ 0.05 per unit. RG2
        ItemDTOEx item2 = createItem(new BigDecimal("0.05"));

        // Create new session requesting 60 units, RG1
        PricingField[] data1 = createData(DESTINATION_REALM,
                user.getUserName(), null, item.getNumber());
        String sessionId = getUniqueSessionId();
        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("60"), data1);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("60"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));

        // Verify that the balance drops to 10 - 60x0.01 = 10 - 0.6 = $9.4.
        assertBigDecimalEquals(new BigDecimal("9.4"),
                getDynamicBalanceAsDecimal(user));

        PricingField[] data2 = createData(DESTINATION_REALM,
                user.getUserName(), null, item2.getNumber());
        result = API.updateSession(sessionId, new Date(), BigDecimal.ZERO,
                new BigDecimal("60"), data2);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("60"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));

        // assert that balance is now 10$ - 0.6$ - 0.05x60 = 6.4$
        assertBigDecimalEquals(new BigDecimal("6.4"),
                getDynamicBalanceAsDecimal(user));

        result = API.updateSession(sessionId, new Date(), BigDecimal.ZERO,
                BigDecimal.ZERO, data1);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));

        // assert that balance is now 10$ - 0.05x60 = 7$
        assertBigDecimalEquals(new BigDecimal("7"),
                getDynamicBalanceAsDecimal(user));

        // Terminate the session using 30 units
        API.endSession(sessionId, new Date(), new BigDecimal("30"), 0);

        // assert that balance is now 10$ - 0.05x30 = 8.5$
        assertBigDecimalEquals(new BigDecimal("8.5"),
                getDynamicBalanceAsDecimal(user));

        deleteItem(item.getId());
        deleteItem(item2.getId());
        deleteUser(new Integer(user.getUserId()));
    }

    @Test
    public void initialCreditLimitReached() {
        // Create a user with balance $0.50
        UserWS user = createUser(new BigDecimal("0.50"));

        // Precondition: Verify that the balance is $0.50.
        assertBigDecimalEquals(new BigDecimal("0.50"),
                getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        // Create new session requesting 60 units
        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                null, item.getNumber());
        DiameterResultWS result = API.createSession(getUniqueSessionId(),
                new Date(), new BigDecimal("60"), data);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("50"), result.getGrantedUnits());
        assertTrue(result.isTerminateWhenConsumed());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));

        // Verify that the balance is $0.
        assertBigDecimalEquals(BigDecimal.ZERO,
                getDynamicBalanceAsDecimal(user));

        deleteUser(new Integer(user.getUserId()));
        deleteItem(item.getId());
    }

    @Test
    public void creditLimitReachedOnExtension() {
        // Create a user with balance $1.00
        UserWS user = createUser(new BigDecimal("1.00"));

        // Precondition: Verify that the balance is $1.00.
        assertBigDecimalEquals(new BigDecimal("1.00"),
                getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        // Create new session requesting 60 units
        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                null, item.getNumber());
        String sessionId = getUniqueSessionId();

        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("60"), data);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("60"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Verify that the balance is $0.40.
        assertBigDecimalEquals(new BigDecimal("0.40"),
                getDynamicBalanceAsDecimal(user));

        // Request another 60 units
        result = API.extendSession(sessionId, new Date(), new BigDecimal("60"),
                new BigDecimal("60"));

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("40"), result.getGrantedUnits());
        assertTrue(result.isTerminateWhenConsumed());

        deleteUser(new Integer(user.getUserId()));
        deleteItem(item.getId());
    }

    @Test
    public void creditLimitReachedOnExtensionPrecise() {
        // Create a user with balance $1.00
        UserWS user = createUser(new BigDecimal("1.00"));

        // Precondition: Verify that the balance is $1.00.
        assertBigDecimalEquals(new BigDecimal("1.00"),
                getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        // Create new session requesting 100 units
        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                null, item.getNumber());
        String sessionId = getUniqueSessionId();

        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("100"), data);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("100"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Verify that the balance now dropped to $0.
        assertBigDecimalEquals(BigDecimal.ZERO,
                getDynamicBalanceAsDecimal(user));

        // Request another 60 units
        result = API.extendSession(sessionId, new Date(),
                new BigDecimal("100"), new BigDecimal("60"));

        assertEquals(DiameterResultWS.DIAMETER_CREDIT_LIMIT_REACHED,
                result.getResultCode());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getGrantedUnits());
        assertFalse(result.isTerminateWhenConsumed());

        deleteUser(new Integer(user.getUserId()));
        deleteItem(item.getId());
    }

    @Test
    public void sessionExpiration() throws Exception {
        // Set session expiration threshold to 2 seconds
        PreferenceWS expiryPref = API.getPreference(REAPER_THRESHOLD_PREF_ID);
        String originalExpiry = expiryPref.getValue();
        expiryPref.setValue(Integer.valueOf(2).toString());
        API.updatePreference(expiryPref);

        // Create a user with balance $1.00
        UserWS user = createUser(new BigDecimal("10.00"));

        // Precondition: Verify that the balance is $1.00.
        assertBigDecimalEquals(new BigDecimal("10.00"),
                getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        // Create new session requesting 60 units
        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                null, item.getNumber());
        String sessionId = getUniqueSessionId();

        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("60"), data);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertBigDecimalEquals(new BigDecimal("60"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));
        assertFalse(result.isTerminateWhenConsumed());

        // Wait 2.5 seconds, the session should expire as the threshold has been
        // set to 2 sec
        Thread.sleep(2500);

        // Request another 60 units, should fail due to session expiration
        result = API.extendSession(sessionId, new Date(), new BigDecimal("60"),
                new BigDecimal("60"));

        assertEquals(DiameterResultWS.DIAMETER_UNKNOWN_SESSION_ID,
                result.getResultCode());
        assertBigDecimalEquals(BigDecimal.ZERO, result.getGrantedUnits());
        assertFalse(result.isTerminateWhenConsumed());

        deleteUser(new Integer(user.getUserId()));
        deleteItem(item.getId());
        expiryPref = API.getPreference(REAPER_THRESHOLD_PREF_ID);
        expiryPref.setValue(originalExpiry);
        API.updatePreference(expiryPref);
    }

    @Test
    public void testSipUriBeingStored() {
        // Create a user and makes a deposit of $10.00
        UserWS user = createUser(BigDecimal.TEN);

        // Precondition: Verify that the balance is $10.
        assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = createItem(new BigDecimal("0.01"));

        String sipUri = "Test sipUri";

        logger.debug("User: {}", user.getUserName());
        // Create new session requesting 60 units
        PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
                sipUri, item.getNumber());

        String sessionId = getUniqueSessionId();
        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("60"), data);

        assertEquals(DiameterResultWS.DIAMETER_SUCCESS, result.getResultCode());
        assertEquals(new BigDecimal("60"), result.getGrantedUnits());
        assertEquals(QUOTA_THRESHOLD,
                Integer.valueOf(result.getQuotaThreshold()));

        // Verify that the balance drops to 10 - 60x0.01 = 10 - 0.6 = $9.4.
        assertBigDecimalEquals(new BigDecimal("9.4"),
                getDynamicBalanceAsDecimal(user));

        // Terminate the session using 30 units
        API.endSession(sessionId, new Date(), new BigDecimal("30"), 0);

        // Verify that the balance returns to 10 - 30x0.01 = 10 - 0.3 = $9.7
        assertBigDecimalEquals(new BigDecimal("9.7"),
                getDynamicBalanceAsDecimal(user));
        // assert that current order now has a line with 30 units and priced
        // 0.30$
        assertBigDecimalEquals(new BigDecimal("0.30"), getTotalOrder(user));

        OrderWS orderWS = API.getCurrentOrder(user.getUserId(), new Date());

        assertEquals(sipUri, orderWS.getOrderLines()[0].getSipUri());

        deleteItem(item.getId());
        deleteUser(new Integer(user.getUserId()));
    }

    @Test
    public void testRateCardPrice() throws Exception {
        // Create a user and makes a deposit of $10.00
        UserWS user = createUser(BigDecimal.TEN);

        // Precondition: Verify that the balance is $10.
        assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));

        logger.debug("User: {}", user.getUserName());
        // Create new session requesting 60 units
        PricingField[] priceData = createData(DESTINATION_REALM, user.getUserName(),
                "sip:+15677", "PC1");

        String sessionId = getUniqueSessionId();
        DiameterResultWS result = API.createSession(sessionId, new Date(),
                new BigDecimal("60"), priceData);
        // Reverted assert condition, after the item entity mapping in changeset.
        assertEquals(DiameterResultWS.DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE, result.getResultCode());
        deleteUser(new Integer(user.getUserId()));
    }

    @Test(groups = {"test-single"})
    public void testConcurrentCall() throws Exception{
        setup();
        // Create a user and makes a deposit of $10.00
        UserWS user = createUser(new BigDecimal("1000"));

        // Create new item priced $ 0.01 per unit.
        ItemDTOEx item = API.getItem(3200, user.getUserId(), null);

        ConcurrentCall concurrentCall = new ConcurrentCall();

        ConcurrentCall.Runner runner = new ConcurrentCall.Runner(API, user, item);

        for(int i=0; i<8; i++){
            concurrentCall.invokeApi(runner);
            //Thread.sleep(1000);
        }

        concurrentCall.waitToFinish();
    }
}
