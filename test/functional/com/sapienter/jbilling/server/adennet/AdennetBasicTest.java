/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.adennet;

import com.sapienter.jbilling.server.adennet.ws.BalanceResponseWS;
import com.sapienter.jbilling.server.adennet.ws.RechargeRequestWS;
import com.sapienter.jbilling.server.adennet.ws.ums.TransactionResponseWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.testng.AssertJUnit.assertNotNull;

@Test(groups = "adennet", testName = "adennet.AdennetBasicTest")
public class AdennetBasicTest extends AdennetBaseConfiguration {

    private static final String TEST_CUSTOMER = "Test-Customer"+System.currentTimeMillis();
    UserWS adennetUserWS;

    @BeforeClass
    public void beforeClass () {

        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
            testBuilder.given(envBuilder -> {
                logger.debug("AdennetBasicTest.beforeClass : {} ",testBuilder);
            });
        }
        testBuilder.given(envBuilder -> {});
    }

    @Test(priority = 1,enabled = true)
    public void testCreateAdennetUser(){
        try{
            testBuilder.given(envBuilder -> {

                adennetUserWS = getAdennetTestUserWS(envBuilder, TEST_CUSTOMER);
                logger.debug("AdennetTestUserWS created {}", adennetUserWS.getId());
                assertNotNull("Failed to Create Adennet test customer ", adennetUserWS);

                BalanceResponseWS balanceResponseWS = getWalletBalance(adennetUserWS.getId());
                logger.debug("BalanceResponseWS = ", balanceResponseWS);
                assertNotNull("Expected balance response", balanceResponseWS);

                //do a top up
                RechargeRequestWS rechargeRequestWS = getRechargeRequestWSForTopUp(adennetUserWS.getUserId(),
                                                        BigDecimal.TEN,OffsetDateTime.now(), ADENNET, TEST_CASE);
                TransactionResponseWS rechargeResponseWS = rechargeTopUp(rechargeRequestWS);
                logger.debug("transactionId = ", rechargeResponseWS);

                // get balance (check with assert equals)
                BigDecimal walletBalance1 =getWalletBalance(adennetUserWS.getId()).getAvailableBalance();
                logger.debug("walletBalance1 = ", walletBalance1);
                assertNotNull("Expected walletBalance but found 0.", walletBalance1);
            });

        }finally {
          clearTestDataForUser(adennetUserWS.getId());
        }
    }

    @AfterClass
    private void teardown(){
        
    }
}
