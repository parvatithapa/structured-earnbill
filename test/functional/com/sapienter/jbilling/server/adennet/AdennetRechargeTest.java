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

import com.sapienter.jbilling.server.adennet.ws.AddOnProductWS;
import com.sapienter.jbilling.server.adennet.ws.AdennetPlanWS;
import com.sapienter.jbilling.server.adennet.ws.BalanceResponseWS;
import com.sapienter.jbilling.server.adennet.ws.FeeWS;
import com.sapienter.jbilling.server.adennet.ws.PlanDescriptionWS;
import com.sapienter.jbilling.server.adennet.ws.PrimaryPlanWS;
import com.sapienter.jbilling.server.adennet.ws.RechargeRequestWS;
import com.sapienter.jbilling.server.adennet.ws.ums.TransactionResponseWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Vipul Yadav
 * Source of Test cases : https://docs.google.com/spreadsheets/d/1GJRV-adIIv3Le7yjCyFy1CwZs5y4cbsaDRHm5meOIiM/edit?pli=1#gid=1571945981
 */

@Test(groups = "adennet", testName = "adennet.AdennetRechargeTest")
public class AdennetRechargeTest extends AdennetBaseConfiguration{

    private static final String TEST_USER_001 = "test-001" + System.currentTimeMillis();
    private static final String TEST_USER_004 = "test-004" + System.currentTimeMillis();
    private static final String TEST_USER_005 = "test-005" + System.currentTimeMillis();
    private static final String TEST_USER_006 = "test-006" + System.currentTimeMillis();
    private static final String TEST_USER_013 = "test-013" + System.currentTimeMillis();
    private static final String TEST_USER_021 = "test-021" + System.currentTimeMillis();
    private static final String TEST_USER_012 = "test-012" + System.currentTimeMillis();
    private static final String TEST_USER_009 = "test-009" + System.currentTimeMillis();
    private static final String TEST_USER_011 = "test-011" + System.currentTimeMillis();
    private static final String TEST_USER_022 = "test-022" + System.currentTimeMillis();
    private static final String TEST_USER_023 = "test-023" + System.currentTimeMillis();
    private static final String TEST_USER_024 = "test-024" + System.currentTimeMillis();
    private static final String TEST_USER_036 = "test-036" + System.currentTimeMillis();
    private static final String TEST_USER_039 = "test-039" + System.currentTimeMillis();
    private static final String TEST_USER_060 = "test-040" + System.currentTimeMillis();

    private static final String DONE_BY = "adennet";
    private static final String TEST_CASE = "test case";
    private static final String NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL = "New recharge request should not be null";
    private static final String USER_SHOULD_NOT_BE_NULL = "User should not be null";
    private static final String TRANSACTION_ID_SHOULD_NOT_BE_NULL = "Transaction id should not be null ";
    private static final String TRANSACTION_ID = "Transaction Id : {}";
    public static final String UMSRESPONSE_WS_SHOULD_NOT_BE_NULL = "UMSResponseWS  should not be null.";

    private UserWS adennetUser001;
    private UserWS adennetUser004;
    private UserWS adennetUser005;
    private UserWS adennetUser006;
    private UserWS adennetUser013;
    private UserWS adennetUser021;
    private UserWS adennetUser012;
    private UserWS adennetUser009;
    private UserWS adennetUser011;
    private UserWS adennetUser022;
    private UserWS adennetUser023;
    private UserWS adennetUser024;
    private UserWS adennetUser036;
    private UserWS adennetUser039;
    private UserWS adennetUser060;

    private Integer assetId1;
    private Integer assetId2;
    private Integer assetId3;
    private Integer assetId4;
    private Integer assetId5;
    private Integer assetId6;
    private Integer assetId7;
    private Integer assetId8;
    private Integer assetId9;
    private Integer assetId10;
    private Integer assetId11;
    private Integer assetId12;
    private Integer assetId13;
    private Integer assetId21;
    private Integer assetId22;
    private Integer assetId23;
    private Integer assetId24;
    private Integer assetId36;
    private Integer assetId39;
    private Integer assetId60;

    private Integer adennetSimFeeId;
    private Integer adennetModemFeeId;
    private Integer adennetDowngradeFeeId;


    @BeforeClass
    public void beforeClass (     ) {
         logger.debug("AdennetBasicTest.beforeClass : "+testBuilder);
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        // asset creation
        testBuilder.given(envBuilder -> {
            logger.debug("api.getCallerCompanyId() :  " + api.getCallerCompanyId());

            adennetSimFeeId = getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_SIM_CARD_FEES);
            logger.debug("adennetSimFeeId : {}", adennetSimFeeId);
            adennetModemFeeId = getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_MODEM_FEES);
            logger.debug("adennetModemFeeId : {}", adennetModemFeeId);
            adennetDowngradeFeeId = getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_DOWNGRADE_FEES);
            logger.debug("adennetDowngradeFeeId : {} " , adennetDowngradeFeeId);


            assetId1 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId1);

            assetId2 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId2);
            assetId3 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId3);
            assetId4 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId4);
            assetId5 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId5);
            assetId6 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId6);
            assetId7 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId7);
            assetId8 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId8);
            assetId9 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId9);
            assetId10 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId10);
            assetId11 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId11);
            assetId12 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId12);
            assetId13 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId13);
            assetId21 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId21);
            assetId22 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId22);
            assetId23 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId23);
            assetId24 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId24);
            assetId36 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId36);
            assetId39 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId39);
            assetId60 =  buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(),PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode"+getTwoDigitNumber(),
                    getTwoDigitNumber()+"-id");
            logger.debug("assetId = : ", assetId60);
        });
    }

    /**
     * Coverd Test Cases ( TC_01 , TC_02 , TC_03)
     * TC_01
     * Verify that a new customer is on boarded successfully along with a plan fee as well as SIM chargers
     *
     * TC_02
     * Verify that a new customer is on boarded successfully along with a plan fee , SIM chargers and Add on service
     *
     * TC_03
     * Verify that when a new customer is on-broarded and "recharge amount" is given as "0"
     * then wallet balance is consumed for recharge new plan
     *
     *  planFee = 9000
     *  simFee = 100    (sim price is under add on category)
     *  addOnProductFee = 200 (modem fee)
     *  recharge amount = 0
     *  wallet balance : greater or equal to ( planFee + simFee + addOnProductFee ) =  9300
     */
    @Test(priority = 1, enabled = true)
    public void testRecharge001() {
        try {
            testBuilder.given(envBuilder -> {

            adennetUser001 = getAdennetTestUserWS(envBuilder, TEST_USER_001);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser001);
                validateBalanceResponse(adennetUser001.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

                /**
                 * Topup
                 * call Topup API
                 * getBalance
                 * Validate GetBalance Response
                 */
                //do a top up
                BigDecimal topAmount= new BigDecimal(PLAN_FEE_60_GB).add(new BigDecimal(1000));
                RechargeRequestWS rechargeRequestWS = getRechargeRequestWSForTopUp(adennetUser001.getUserId(),
                        topAmount,OffsetDateTime.now(),"Adennet","Test Case");
                TransactionResponseWS rechargeResponseWS = rechargeTopUp(rechargeRequestWS);
                logger.debug(TRANSACTION_ID, rechargeResponseWS.getTransactionId());
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,rechargeResponseWS.getTransactionId());

                validateBalanceResponse(adennetUser001.getUserId(),topAmount,BigDecimal.ZERO ,topAmount);

                /**
                 * Call Recharge
                 * Validate Recharge Responce
                 */

                AssetWS assetWS = api.getAsset(assetId1);
                String subscriberNumber= assetWS.getIdentifier();
                logger.debug("subscriberNumber : {}", assetWS.getIdentifier());
                boolean activeNow = false;
                BigDecimal rechargeAmount = BigDecimal.ZERO;
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                BigDecimal primaryPlanPrice = primaryPlanWS.getPrice();

                // fees product sim price
                Integer feesProductIds[] = {};
                List<FeeWS> feesWSList =  getFeesWSList(feesProductIds);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId, adennetModemFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser001.getId(),subscriberNumber,primaryPlanWS,
                        feesWSList,addOnProductList, activeNow,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String transactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                validateBalanceResponse(adennetUser001.getUserId(),new BigDecimal(700),BigDecimal.ZERO ,new BigDecimal(700));
            });
        } catch (Exception exception) {
                    exception.printStackTrace();
        }
    }

    /**
     * TC_04
     * Verify that when a new customer is on-broarded and "recharge amount" is given as full amount then
     * wallet balance is should not be consumed for recharge new plan
     *
     * "Wallet Balance : 6000
     * Plan fee : 9000
     * SIM Fee : 100
     * Recharge Amount : 9100
     * Don't Use Wallet balance for recharge
     * New Wallet balance : 6000"
     */

    @Test(priority = 2, enabled = true)
    public void testRecharge004() {
        try {
            testBuilder.given(envBuilder -> {
                adennetUser004 = getAdennetTestUserWS(envBuilder, TEST_USER_004);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser004);

                validateBalanceResponse(adennetUser004.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

                /**
                 * Topup
                 * call Topup API
                 * getBalance
                 * Validate GetBalance Response
                 */
                //do a top up
                BigDecimal topAmount= new BigDecimal(6000);
                RechargeRequestWS rechargeRequestWS = getRechargeRequestWSForTopUp(adennetUser004.getUserId(),
                        topAmount,OffsetDateTime.now(),"Adennet","Test Case");
                TransactionResponseWS rechargeResponseWS = rechargeTopUp(rechargeRequestWS);
                logger.debug(TRANSACTION_ID, rechargeResponseWS.getTransactionId());
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,rechargeResponseWS.getTransactionId());
                validateBalanceResponse(adennetUser004.getUserId(),topAmount,BigDecimal.ZERO ,topAmount);

                /**
                 * Call Recharge
                 * Validate Recharge Responce
                 *
                 */

                AssetWS assetWS = api.getAsset(assetId4);
                String subscriberNumber= assetWS.getIdentifier();
                boolean activeNow = false;
                BigDecimal rechargeAmount = new BigDecimal(9100);
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                // fees product sim price
                Integer feesProductIds[] = {};
                List<FeeWS> feesWSList =  getFeesWSList(feesProductIds);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser004.getId(),subscriberNumber,primaryPlanWS,
                        feesWSList,addOnProductList, activeNow,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String transactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                validateBalanceResponse(adennetUser004.getUserId(),topAmount,BigDecimal.ZERO ,topAmount);
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * TC_05
     * Verify that when a new customer is on-broarded and "recharge amount" is given as partial amount then
     * wallet balance is should be consumed for recharge new plan
     * Wallet Balance : 6000
     * Plan fee : 9000
     * SIM Fee : 100
     * Recharge Amount :7000
     * New Wallet balance : 3900"
     */

    @Test(priority = 3, enabled = true)
    public void testRecharge005() {
        try {
            testBuilder.given(envBuilder -> {
                adennetUser005 = getAdennetTestUserWS(envBuilder, TEST_USER_005);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser005);
                validateBalanceResponse(adennetUser005.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

                /**
                 * Topup
                 * call Topup API
                 * getBalance
                 * Validate GetBalance Response
                 */
                //do a top up
                BigDecimal topAmount= new BigDecimal(6000);
                RechargeRequestWS rechargeRequestWS = getRechargeRequestWSForTopUp(adennetUser005.getUserId(),
                        topAmount,OffsetDateTime.now(),"Adennet","Test Case");
                TransactionResponseWS rechargeResponseWS = rechargeTopUp(rechargeRequestWS);
                logger.debug(TRANSACTION_ID, rechargeResponseWS.getTransactionId());
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,rechargeResponseWS.getTransactionId());
                validateBalanceResponse(adennetUser005.getUserId(),topAmount,BigDecimal.ZERO ,topAmount);

                /**
                 * Call Recharge
                 * Validate Recharge Responce
                 *
                 */

                AssetWS assetWS = api.getAsset(assetId5);
                String subscriberNumber= assetWS.getIdentifier();
                boolean activeNow = false;
                BigDecimal rechargeAmount = new BigDecimal(7000);
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                // fees product sim price
                Integer feesProductIds[] = {};
                List<FeeWS> feesWSList =  getFeesWSList(feesProductIds);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser005.getId(),subscriberNumber,primaryPlanWS,
                        feesWSList,addOnProductList, activeNow,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String transactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                validateBalanceResponse(adennetUser005.getUserId(),new BigDecimal(3900),BigDecimal.ZERO ,new BigDecimal(3900));
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * TC_06
     * Verify that when excess amount on top of the recharge amount will be considered as
     * wallet top-up and should reflect as available balance in the wallet
     *
     * Wallet Balance : 6000
     * Plan fee : 9000
     * SIM Fee : 100
     * Recharge Amount :14100
     * New Wallet balance : 11000"
     */

    @Test(priority = 4, enabled = true)
    public void testRecharge006() {
        try {
            testBuilder.given(envBuilder -> {
                adennetUser006 = getAdennetTestUserWS(envBuilder, TEST_USER_006);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser006);
                validateBalanceResponse(adennetUser006.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

                /**
                 * Topup
                 * call Topup API
                 * getBalance
                 * Validate GetBalance Response
                 */
                //do a top up
                BigDecimal topAmount= new BigDecimal(6000);
                RechargeRequestWS rechargeRequestWS = getRechargeRequestWSForTopUp(adennetUser006.getUserId(),
                        topAmount,OffsetDateTime.now(),"Adennet","Test Case");
                TransactionResponseWS rechargeResponseWS = rechargeTopUp(rechargeRequestWS);
                logger.debug(TRANSACTION_ID, rechargeResponseWS.getTransactionId());
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,rechargeResponseWS.getTransactionId());

                validateBalanceResponse(adennetUser006.getUserId(),topAmount,BigDecimal.ZERO ,topAmount);

                /**
                 * Call Recharge
                 * Validate Recharge Responce
                 *
                 */

                AssetWS assetWS = api.getAsset(assetId6);
                String subscriberNumber= assetWS.getIdentifier();
                logger.debug("subscriberNumber : {}", assetWS.getIdentifier());
                boolean activeNow = false;
                BigDecimal rechargeAmount = new BigDecimal(14100);
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                // fees product sim price
                Integer feesProductIds[] = {};
                List<FeeWS> feesWSList =  getFeesWSList(feesProductIds);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser006.getId(),subscriberNumber,primaryPlanWS,
                        feesWSList,addOnProductList, activeNow,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String transactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                BigDecimal walletBalance = rechargeAmount.subtract(new BigDecimal(PLAN_FEE_60_GB).add(new BigDecimal(100)));
                validateBalanceResponse(adennetUser006.getUserId(),walletBalance.add(topAmount),BigDecimal.ZERO ,walletBalance.add(topAmount));
            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * TC_21
     * Verify that when plan is renewed on immediate basis then the current plan will get cancelled
     * and renewed plan will get activated at the same time.
     */

    @Test(priority = 9, enabled = true)
    public void testRecharge021() {
        try {
            AssetWS assetWS = api.getAsset(assetId21);
            testBuilder.given(envBuilder -> {
                adennetUser021 = getAdennetTestUserWS(envBuilder, TEST_USER_021);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser021);
                validateBalanceResponse(adennetUser021.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

               // Call Recharge and Validate Recharge Responce
                String subscriberNumber= assetWS.getIdentifier();
                logger.debug("subscriberNumber = {}", subscriberNumber);

                BigDecimal rechargeAmount = new BigDecimal(PLAN_FEE_60_GB).add(new BigDecimal(100));
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                //activeNow = false;
                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser021.getId(),subscriberNumber,primaryPlanWS,
                        Collections.emptyList(),addOnProductList, Boolean.FALSE,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String transactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                validateBalanceResponse(adennetUser021.getUserId(),BigDecimal.ZERO,
                        BigDecimal.ZERO,BigDecimal.ZERO );

                // check plan
                AdennetPlanWS currentPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser021.getId());
                PlanDescriptionWS currentPlanDescriptionWS = currentPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,currentPlanDescriptionWS.getId(), currentPlanDescriptionWS.getDescription());

                // recharge request for future
                BigDecimal futureRechargeAmount = new BigDecimal(PLAN_FEE_80_GB);
                PlanWS  futurePlanByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_80, api.getCallerCompanyId());
                PrimaryPlanWS futurePrimaryPlanWS = getPrimaryPlanWS(futurePlanByInternalNumber.getId(),PLAN_NAME_80, USAGE_QUATA_80_GB,30, PLAN_FEE_80_GB);

                // active now = true
                RechargeRequestWS futureRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser021.getId(),subscriberNumber,futurePrimaryPlanWS,
                        Collections.emptyList(),Collections.emptyList(), Boolean.TRUE,futureRechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,futureRechargeRequest);

                String futureTransactionId = doRecharge(futureRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,futureTransactionId);
                logger.debug(TRANSACTION_ID, futureTransactionId);

                AdennetPlanWS newPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser021.getId());
                PlanDescriptionWS newPlanDescriptionWS = newPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,newPlanDescriptionWS.getId(), newPlanDescriptionWS.getDescription());

            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * TC_22
     * Verify that while renewal of plan on immediate basis when current plan is active
     * and "recharge amount" is given as "0" then wallet balance is consumed for recharge
     */


    @Test(priority = 10, enabled = true)
    public void testRecharge022() {
        try {
            AssetWS assetWS = api.getAsset(assetId22);
            testBuilder.given(envBuilder -> {
                adennetUser022 = getAdennetTestUserWS(envBuilder, TEST_USER_022);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser022);
                validateBalanceResponse(adennetUser022.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

                // Call Recharge and Validate Recharge Responce
                String subscriberNumber= assetWS.getIdentifier();
                logger.debug("subscriberNumber = {}", subscriberNumber);

                BigDecimal rechargeAmount = new BigDecimal(PLAN_FEE_60_GB).add(new BigDecimal(100));
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                //activeNow = false;
                RechargeRequestWS rechargeRequest =  getRechargeRequestWSForNewUser(adennetUser022.getId(),subscriberNumber,primaryPlanWS,
                        Collections.emptyList(),addOnProductList, Boolean.FALSE,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,rechargeRequest);

                String transactionId = doRecharge(rechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                validateBalanceResponse(adennetUser022.getUserId(),BigDecimal.ZERO,
                        BigDecimal.ZERO,BigDecimal.ZERO );

                // top up
                BigDecimal topAmount= new BigDecimal(PLAN_FEE_80_GB).add(new BigDecimal(500));
                RechargeRequestWS rechargeRequestWS = getRechargeRequestWSForTopUp(adennetUser022.getUserId(),
                        topAmount,OffsetDateTime.now(),"Adennet","Test Case");
                TransactionResponseWS rechargeResponseWS = rechargeTopUp(rechargeRequestWS);
                logger.debug(TRANSACTION_ID, rechargeResponseWS.getTransactionId());
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,rechargeResponseWS.getTransactionId());

                validateBalanceResponse(adennetUser022.getUserId(),topAmount, BigDecimal.ZERO,topAmount);

                // check plan
                AdennetPlanWS currentPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser022.getId());
                PlanDescriptionWS currentPlanDescriptionWS = currentPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,currentPlanDescriptionWS.getId(), currentPlanDescriptionWS.getDescription());


                // recharge request for future
                BigDecimal futureRechargeAmount = BigDecimal.ZERO;
                PlanWS  futurePlanByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_80, api.getCallerCompanyId());
                PrimaryPlanWS futurePrimaryPlanWS = getPrimaryPlanWS(futurePlanByInternalNumber.getId(),PLAN_NAME_80, USAGE_QUATA_80_GB,30, PLAN_FEE_80_GB);

                // active now = true
                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser022.getId(),subscriberNumber,futurePrimaryPlanWS,
                        Collections.emptyList(),Collections.emptyList(), Boolean.TRUE,futureRechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String newTransactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,newTransactionId);
                logger.debug(TRANSACTION_ID, newTransactionId);

                validateBalanceResponse(adennetUser022.getUserId(),new BigDecimal(500), BigDecimal.ZERO,new BigDecimal(500));

                AdennetPlanWS newPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser022.getId());
                PlanDescriptionWS newPlanDescriptionWS = newPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,newPlanDescriptionWS.getId(), newPlanDescriptionWS.getDescription());

            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * TC_23
     * Verify that while renewal of plan on immediate basis when current plan is active and
     * "recharge amount" is given as full amount then wallet balance is should not be consumed for recharge
     */

    @Test(priority = 11, enabled = true)
    public void testRecharge023() {
        try {
            AssetWS assetWS = api.getAsset(assetId23);
            testBuilder.given(envBuilder -> {
                adennetUser023 = getAdennetTestUserWS(envBuilder, TEST_USER_023);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser023);
                validateBalanceResponse(adennetUser023.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

                // Call Recharge and Validate Recharge Responce
                String subscriberNumber= assetWS.getIdentifier();
                logger.debug("subscriberNumber = {}", subscriberNumber);

                BigDecimal rechargeAmount = new BigDecimal(PLAN_FEE_60_GB).add(new BigDecimal(600));
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                //activeNow = false;
                RechargeRequestWS rechargeRequest =  getRechargeRequestWSForNewUser(adennetUser023.getId(),subscriberNumber,primaryPlanWS,
                        Collections.emptyList(),addOnProductList, Boolean.FALSE,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,rechargeRequest);

                String transactionId = doRecharge(rechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                validateBalanceResponse(adennetUser023.getUserId(),new BigDecimal(500),
                        BigDecimal.ZERO,new BigDecimal(500) );

                // check plan
                AdennetPlanWS currentPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser023.getId());
                PlanDescriptionWS currentPlanDescriptionWS = currentPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,currentPlanDescriptionWS.getId(), currentPlanDescriptionWS.getDescription());

                // recharge request for future
                BigDecimal futureRechargeAmount = new BigDecimal(PLAN_FEE_80_GB);
                PlanWS  futurePlanByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_80, api.getCallerCompanyId());
                PrimaryPlanWS futurePrimaryPlanWS = getPrimaryPlanWS(futurePlanByInternalNumber.getId(),PLAN_NAME_80, USAGE_QUATA_80_GB,30, PLAN_FEE_80_GB);

                // active now = true
                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser023.getId(),subscriberNumber,futurePrimaryPlanWS,
                        Collections.emptyList(),Collections.emptyList(), Boolean.TRUE,futureRechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String newTransactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,newTransactionId);
                logger.debug(TRANSACTION_ID, newTransactionId);

                validateBalanceResponse(adennetUser023.getUserId(),new BigDecimal(500), BigDecimal.ZERO,new BigDecimal(500));

                AdennetPlanWS newPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser023.getId());
                PlanDescriptionWS newPlanDescriptionWS = newPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,newPlanDescriptionWS.getId(), newPlanDescriptionWS.getDescription());

            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     *  TC_24
     *  Verify that while renewal of plan on immediate basis when current plan is active and  "recharge amount" is given as
     *  partial amount then wallet balance is should be consumed for recharge
     */

    @Test(priority = 12, enabled = true)
    public void testRecharge024() {
        try {
            AssetWS assetWS = api.getAsset(assetId24);
            testBuilder.given(envBuilder -> {
                adennetUser024 = getAdennetTestUserWS(envBuilder, TEST_USER_024);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser024);
                validateBalanceResponse(adennetUser024.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

                // Call Recharge and Validate Recharge Responce
                String subscriberNumber= assetWS.getIdentifier();
                logger.debug("subscriberNumber = {}", subscriberNumber);

                BigDecimal rechargeAmount = new BigDecimal(PLAN_FEE_60_GB).add(new BigDecimal(2100));
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                //activeNow = false;
                RechargeRequestWS rechargeRequest =  getRechargeRequestWSForNewUser(adennetUser024.getId(),subscriberNumber,primaryPlanWS,
                        Collections.emptyList(),addOnProductList, Boolean.FALSE,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,rechargeRequest);

                String transactionId = doRecharge(rechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                validateBalanceResponse(adennetUser024.getUserId(),new BigDecimal(2000),
                        BigDecimal.ZERO,new BigDecimal(2000) );

                // check plan
                AdennetPlanWS currentPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser024.getId());
                PlanDescriptionWS currentPlanDescriptionWS = currentPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,currentPlanDescriptionWS.getId(), currentPlanDescriptionWS.getDescription());

                // recharge request for future
                BigDecimal futureRechargeAmount = new BigDecimal(PLAN_FEE_80_GB).subtract(new BigDecimal(1000));
                PlanWS  futurePlanByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_80, api.getCallerCompanyId());
                PrimaryPlanWS futurePrimaryPlanWS = getPrimaryPlanWS(futurePlanByInternalNumber.getId(),PLAN_NAME_80, USAGE_QUATA_80_GB,30, PLAN_FEE_80_GB);

                // active now = true
                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser024.getId(),subscriberNumber,futurePrimaryPlanWS,
                        Collections.emptyList(),Collections.emptyList(), Boolean.TRUE,futureRechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String newTransactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,newTransactionId);
                logger.debug(TRANSACTION_ID, newTransactionId);

                validateBalanceResponse(adennetUser024.getUserId(),new BigDecimal(1000), BigDecimal.ZERO,new BigDecimal(1000));

                AdennetPlanWS newPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser024.getId());
                PlanDescriptionWS newPlanDescriptionWS = newPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,newPlanDescriptionWS.getId(), newPlanDescriptionWS.getDescription());

            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * TC_36
     * Verify that when plan is downgraded  on immediate basis then the current plan will get cancelled ,
     * renewed plan will get activated at the same time and downgrade fee is also applicable
     */
    @Test(priority = 13, enabled = true)
    public void testRecharge036() {
        try {
            AssetWS assetWS = api.getAsset(assetId36);
            testBuilder.given(envBuilder -> {
                adennetUser036 = getAdennetTestUserWS(envBuilder, TEST_USER_036);
                assertNotNull(USER_SHOULD_NOT_BE_NULL, adennetUser036);
                validateBalanceResponse(adennetUser036.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);

                // Call Recharge and Validate Recharge Responce
                String subscriberNumber= assetWS.getIdentifier();
                logger.debug("subscriberNumber = {}", subscriberNumber);

                BigDecimal rechargeAmount = new BigDecimal(PLAN_FEE_60_GB).add(new BigDecimal(100));
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(),PLAN_NAME_60, USAGE_QUATA_60_GB,30, PLAN_FEE_60_GB);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                //activeNow = false;
                RechargeRequestWS rechargeRequest =  getRechargeRequestWSForNewUser(adennetUser036.getId(),subscriberNumber,primaryPlanWS,
                        Collections.emptyList(),addOnProductList, Boolean.FALSE,rechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,rechargeRequest);

                String transactionId = doRecharge(rechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                validateBalanceResponse(adennetUser036.getUserId(),BigDecimal.ZERO,BigDecimal.ZERO, BigDecimal.ZERO);

                // check plan
                AdennetPlanWS currentPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser036.getId());
                PlanDescriptionWS currentPlanDescriptionWS = currentPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,currentPlanDescriptionWS.getId(), currentPlanDescriptionWS.getDescription());

                // recharge request for downgrade plan
                BigDecimal futureRechargeAmount = new BigDecimal(PLAN_FEE_40_GB).add(new BigDecimal(1100));
                PlanWS  futurePlanByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_40, api.getCallerCompanyId());
                PrimaryPlanWS futurePrimaryPlanWS = getPrimaryPlanWS(futurePlanByInternalNumber.getId(),PLAN_NAME_40, USAGE_QUATA_40_GB,30, PLAN_FEE_40_GB);

                // active now = true and downgrade fee
                Integer fees[] = {adennetDowngradeFeeId};
                List<FeeWS> feeWSList = getFeesWSList(fees);

                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUser036.getId(),subscriberNumber,futurePrimaryPlanWS,
                        feeWSList,Collections.emptyList(), Boolean.TRUE,futureRechargeAmount, OffsetDateTime.now(), DONE_BY, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);

                String newTransactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,newTransactionId);
                logger.debug(TRANSACTION_ID, newTransactionId);

                validateBalanceResponse(adennetUser036.getUserId(),new BigDecimal(1000), BigDecimal.ZERO,new BigDecimal(1000));

                AdennetPlanWS newPlanDetailsByUserId = getPlanDetailsByUserId(adennetUser036.getId());
                PlanDescriptionWS newPlanDescriptionWS = newPlanDetailsByUserId.getSubscriptions().get(0).getPlan();
                logger.debug("Current Plan Id : {} and Description : {}" ,newPlanDescriptionWS.getId(), newPlanDescriptionWS.getDescription());

            });
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void validateBalanceResponse( Integer userId, BigDecimal totalBalance,  BigDecimal holdAmount, BigDecimal availableBalance ) {

        logger.debug("Validating Balance Response for user {}",userId);
        BalanceResponseWS balanceResponseWS = getWalletBalance(userId);
        logger.debug("balanceResponseWS.getTotalBalance() = {}", balanceResponseWS.getTotalBalance());
        logger.debug("balanceResponseWS.getHoldAmount() = {}", balanceResponseWS.getHoldAmount());
        logger.debug("balanceResponseWS.getAvailableBalance() = {}", balanceResponseWS.getAvailableBalance());

        assertEquals(totalBalance.setScale(SCALE_TWO, RoundingMode.HALF_UP),balanceResponseWS.getTotalBalance());
        assertEquals(holdAmount.setScale(SCALE_TWO, RoundingMode.HALF_UP),balanceResponseWS.getHoldAmount());
        assertEquals(availableBalance.setScale(SCALE_TWO, RoundingMode.HALF_UP),balanceResponseWS.getAvailableBalance());

    }
}
