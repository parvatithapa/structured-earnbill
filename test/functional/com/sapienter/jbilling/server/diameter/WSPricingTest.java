package com.sapienter.jbilling.server.diameter;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

@Test(testName = "WSPricingTest")
public class WSPricingTest extends BaseDiameterTest {

	private static final Logger logger = LoggerFactory.getLogger(WSPricingTest.class);

	@BeforeClass
	public void setup() throws Exception {
		API = JbillingAPIFactory.getAPI();
	}

	@Test
	public void testRateCardPrice() throws Exception {
		// Create a user and makes a deposit of $10.00
		UserWS user = createUser(BigDecimal.TEN);

		// Precondition: Verify that the balance is $10.
		assertBigDecimalEquals(BigDecimal.TEN, getDynamicBalanceAsDecimal(user));

		logger.debug("User: {}", user.getUserName());
		// Create new session requesting 60 units
		PricingField[] data = createData(DESTINATION_REALM, user.getUserName(),
				"sip:+15677", "PC1");

		String sessionId = getUniqueSessionId();
		DiameterResultWS result = API.createSession(sessionId, new Date(),
				new BigDecimal("60"), data);

		assertEquals(DiameterResultWS.DIAMETER_CREDIT_CONTROL_NOT_APPLICABLE, result.getResultCode());
		deleteUser(new Integer(user.getUserId()));
	}
}
