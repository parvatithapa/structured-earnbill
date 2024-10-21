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
import com.sapienter.jbilling.server.adennet.ws.FeeWS;
import com.sapienter.jbilling.server.adennet.ws.PlanChangeRequestWS;
import com.sapienter.jbilling.server.adennet.ws.PlanDescriptionWS;
import com.sapienter.jbilling.server.adennet.ws.PrimaryPlanWS;
import com.sapienter.jbilling.server.adennet.ws.RechargeRequestWS;
import com.sapienter.jbilling.server.adennet.ws.SubscriptionWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Onkar Pawar, 30 June 2022
 * https://docs.google.com/spreadsheets/d/1lzZKaZ6ZovZ6UaZVACpw9r2pzn5arGuOx03jMZhF0YY/edit?usp=sharing
 */

@Test(groups = "adennet", testName = "adennet.AdennetPlanTest")
public class AdennetPlanTest extends AdennetBaseConfiguration {

    private static final String TEST_USER_001 = "Test-Plan-User" + System.currentTimeMillis();
    UserWS adennetUserWS;
    Integer adennetSimFeeId;
    Integer adennetModemFeeId;
    private Integer assetId1;
    private Integer assetId2;
    private Integer assetId3;

    @BeforeClass
    public void beforeClass () {
        logger.debug("AdennetPlanTest.beforeClass"+testBuilder);
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        testBuilder.given(envBuilder -> {
            adennetSimFeeId = getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_SIM_CARD_FEES);
            adennetModemFeeId = getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_MODEM_FEES);

            assetId1 = buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode" + getTwoDigitNumber(),
                    getTwoDigitNumber() + "-id");

            assetId2 = buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode" + getTwoDigitNumber(),
                    getTwoDigitNumber() + "-id");

            assetId3 = buildAndPersistAssetWithIMSINumber(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), CATEGORY_SUBSCRIBER_NUMBERS),
                    getItemIdByCode(testBuilder.getTestEnvironment(), PRODUCT_CODE_SERVICE_NUMBER), getTenDigitNumber(), "TestAssetCode" + getTwoDigitNumber(),
                    getTwoDigitNumber() + "-id");
        });
    }

    /**
     * Verify that getPlanDetails api should return 404
     * if userId or subscribeNumber is invalid
     */
    @Test(priority = 1,enabled = true)
    public void testGetPlanDetailsApi404(){
        try{
            testBuilder.given(envBuilder -> {

                /*  This test case expect all prerequisite data like user, asset/subscriber number, and plan should be created first
                *   1. Create a user
                *   2. Create a plan
                *   3. Create an asset
                *   4. Check if there is no plan assigned for a combination of userId and subscriberNumber
                */

                adennetUserWS = getAdennetTestUserWS(envBuilder, TEST_USER_001);
                assertNotNull(EXPECTED_NOT_NULL_USER_WS_BUT_FOUND_NULL,adennetUserWS);
                logger.debug(ADENNET_USER_WS_ID, adennetUserWS.getId());

                AssetWS assetWS = api.getAsset(assetId1);
                assertNotNull(EXPECTED_NOT_NULL_ASSET_WS_BUT_FOUND_NULL,assetWS);
                String subscriberNumber = assetWS.getIdentifier();
                logger.debug(ASSET_WS,assetWS);

                //4. Check if there is no plan assigned for a combination of userId and subscriberNumber
                // Call /api/adennet/users/{userId}/plan/{subscriber-number} and it should return 404, no content

                AdennetPlanWS adennetPlanWS = getPlanDetailsByUserIdAndSubscriberNumber(adennetUserWS.getUserId(), subscriberNumber);
                assertEquals(String.format(EXPECTED_NO_PLAN_BUT_FOUND_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, adennetUserWS.getUserId(), subscriberNumber),null, adennetPlanWS);
                logger.info(String.format(NO_ACTIVE_PLAN_FOUND_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, adennetUserWS.getUserId(), subscriberNumber));

            });

        }finally {
            clearTestDataForUser(adennetUserWS.getId());
        }
    }

    /**
     * Verify that getPlanDetails should successfully return the plan details
     * for given userId
     */
    @Test(priority = 2,enabled = true)
    public void testGetPlanDetailsApi200(){
        try{
            testBuilder.given(envBuilder -> {

                 /* This test case expect all prerequisite data like user, asset/subscriber number, and plan should be created first
                 *   1. Create a user
                 *   2. Create a plan
                 *   3. Create an asset
                 *   4. Check if there is no plan assigned for a combination of userId and subscriberNumber
                 *   5. Call recharge
                 *   6. Validate that there is a plan assigned for a combination of userId and subscriberNumber                *
                 */

                adennetUserWS = getAdennetTestUserWS(envBuilder, TEST_USER_001);
                assertNotNull(EXPECTED_NOT_NULL_USER_WS_BUT_FOUND_NULL,adennetUserWS);
                logger.debug(ADENNET_USER_WS_ID, adennetUserWS.getId());

                AssetWS assetWS = api.getAsset(assetId1);
                assertNotNull(EXPECTED_NOT_NULL_ASSET_WS_BUT_FOUND_NULL,assetWS);
                String subscriberNumber = assetWS.getIdentifier();
                logger.debug(ASSET_WS,assetWS);

                //4. Check if there is no plan assigned for a combination of userId and subscriberNumber
                // Call /api/adennet/users/{userId}/plan/{subscriber-number} and it should return 404, no content

                AdennetPlanWS adennetPlanWS = getPlanDetailsByUserIdAndSubscriberNumber(adennetUserWS.getUserId(), subscriberNumber);
                assertEquals(String.format(EXPECTED_NO_PLAN_BUT_FOUND_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, adennetUserWS.getUserId(), subscriberNumber),null, adennetPlanWS);
                logger.info(String.format(NO_ACTIVE_PLAN_FOUND_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, adennetUserWS.getUserId(), subscriberNumber));

                BigDecimal rechargeAmount = BigDecimal.valueOf(50000);
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(), PLAN_NAME_60, "122880", 30, "30000");

                // fees product sim price
                Integer feesProductIds[] = {};
                List<FeeWS> feesWSList =  getFeesWSList(feesProductIds);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId, adennetModemFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                //activeNow = false;
                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUserWS.getId(),subscriberNumber, primaryPlanWS,
                        feesWSList, addOnProductList, Boolean.FALSE, rechargeAmount, OffsetDateTime.now(), ADENNET, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);
                logger.debug(RECHARGE_REQUEST_WS, newRechargeRequest);

                String transactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                AdennetPlanWS adennetPlanWSByUserId = getPlanDetailsByUserId(adennetUserWS.getId());
                logger.debug("AdennetPlanWS : ",adennetPlanWSByUserId);
                assertNotNull(String.format(EXPECTED_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, adennetUserWS.getUserId(), subscriberNumber), adennetPlanWSByUserId);
            });

        }finally {
            clearTestDataForUser(adennetUserWS.getId());
        }
    }

    /**
     * Verify that getPlanDetails api should return 405
     * if request method is other than GET
     */
    @Test(priority = 3,enabled = true)
    public void testGetPlanDetailsApi405(){
        try{
            testBuilder.given(envBuilder -> {

                 /* This test case expect all prerequisite data like user, asset/subscriber number, and plan should be created first
                 *   1. Create a user
                 *   3. Create an asset
                 *   4. Check if API returns method not available exception if POST method is passed instead of GET
                 */

                adennetUserWS = getAdennetTestUserWS(envBuilder, TEST_USER_001);
                assertNotNull(EXPECTED_NOT_NULL_USER_WS_BUT_FOUND_NULL,adennetUserWS);
                logger.debug(ADENNET_USER_WS_ID, adennetUserWS.getId());

                AssetWS assetWS = api.getAsset(assetId1);
                assertNotNull(EXPECTED_NOT_NULL_ASSET_WS_BUT_FOUND_NULL,assetWS);
                String subscriberNumber = assetWS.getIdentifier();
                logger.debug(ASSET_WS,assetWS);

                //4. Check if there is no plan assigned for a combination of userId and subscriberNumber
                // Call /api/adennet/users/{userId}/plan/{subscriber-number} and it should return 405, no content

                AdennetPlanWS adennetPlanWS = checkMethodNotAllowedResponseForGetPlanDetailsApi(adennetUserWS.getUserId(), subscriberNumber);
                assertEquals(String.format(EXPECTED_METHOD_NOT_ALLOWED_BUT_FOUND_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, adennetUserWS.getUserId(), subscriberNumber),null, adennetPlanWS);
            });
          }finally {
            clearTestDataForUser(adennetUserWS.getId());
        }
    }

    /**
     * Verify that changePlan api should return 404
     * if userId or subscribeNumber is invalid
     */
    @Test(priority = 4, enabled = true)
    public void testChangePlanApi404() {
        try{
            testBuilder.given(envBuilder -> {

                PlanDescriptionWS planDescriptionWS = getPlanDescriptionWS(PLAN_ID_INVALID,"12 GB",30,1024,BigDecimal.valueOf(1000));
                SubscriptionWS subscriptionWS = new SubscriptionWS(SUBSCRIBER_NUMBER_INVALID,planDescriptionWS);
                PlanChangeRequestWS planChangeRequestWS = getPlanChangeRequestWS(USER_ID_INVALID,"2022-06-28 16:24:00","2022-07-28 16:24:00",subscriptionWS);
                Integer changePlanResponse = changePlan(planChangeRequestWS);
                assertEquals(String.format("Expected invalid userId=%s and subscriberNumber=%s but plan successfully changed.", USER_ID_INVALID, SUBSCRIBER_NUMBER_INVALID),null, changePlanResponse);
                logger.debug(CHANGE_PLAN_RESPONSE,changePlanResponse);
            });
        } finally {
            clearTestDataForUser(adennetUserWS.getId());
        }
    }

    /**
     * Verify that changePlan api checks if there is already a plan for user
     * and successfully change the plan for given user
     */
    @Test(priority = 5, enabled = true)
    public void testChangePlanApi200(){
        try{
            testBuilder.given(envBuilder ->{

                adennetUserWS = getAdennetTestUserWS(envBuilder, TEST_USER_001);
                assertNotNull(EXPECTED_NOT_NULL_USER_WS_BUT_FOUND_NULL,adennetUserWS);
                logger.debug(ADENNET_USER_WS_ID, adennetUserWS.getId());

                AssetWS assetWS = api.getAsset(assetId1);
                assertNotNull(EXPECTED_NOT_NULL_ASSET_WS_BUT_FOUND_NULL,assetWS);
                String subscriberNumber = assetWS.getIdentifier();
                logger.debug(ASSET_WS,assetWS);

                //4. Check if there is no plan assigned for a combination of userId and subscriberNumber
                // Call /api/adennet/users/{userId}/plan/{subscriber-number} and it should return 404, no content

                AdennetPlanWS adennetPlanWS = getPlanDetailsByUserIdAndSubscriberNumber(adennetUserWS.getUserId(), subscriberNumber);
                assertEquals(String.format(EXPECTED_NO_PLAN_BUT_FOUND_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, adennetUserWS.getUserId(), subscriberNumber),null, adennetPlanWS);
                logger.info(String.format(NO_ACTIVE_PLAN_FOUND_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, adennetUserWS.getUserId(), subscriberNumber));

                BigDecimal rechargeAmount = BigDecimal.valueOf(50000);
                PlanWS planByInternalNumber = api.getPlanByInternalNumber(PLAN_NUMBER_60, api.getCallerCompanyId());
                PrimaryPlanWS primaryPlanWS = getPrimaryPlanWS(planByInternalNumber.getId(), PLAN_NAME_60, "122880", 30, "30000");

                // fees product sim price
                Integer feesProductIds[] = {};
                List<FeeWS> feesWSList =  getFeesWSList(feesProductIds);

                // add on product sim price
                Integer addOnProductIds[] = {adennetSimFeeId, adennetModemFeeId};
                List<AddOnProductWS> addOnProductList = getAddOnProductWSList(addOnProductIds);

                //activeNow = false;
                RechargeRequestWS newRechargeRequest =  getRechargeRequestWSForNewUser(adennetUserWS.getId(),subscriberNumber, primaryPlanWS,
                        feesWSList, addOnProductList, Boolean.FALSE, rechargeAmount, OffsetDateTime.now(), ADENNET, TEST_CASE);
                assertNotNull(NEW_RECHARGE_REQUEST_SHOULD_NOT_BE_NULL,newRechargeRequest);
                logger.debug(RECHARGE_REQUEST_WS, newRechargeRequest);

                String transactionId = doRecharge(newRechargeRequest);
                assertNotNull(TRANSACTION_ID_SHOULD_NOT_BE_NULL,transactionId);
                logger.debug(TRANSACTION_ID, transactionId);

                PlanWS newPlan = api.getPlanByInternalNumber(PLAN_NUMBER_80, api.getCallerCompanyId());
                PlanDescriptionWS planDescriptionWS = getPlanDescriptionWS( newPlan.getId(), PLAN_NAME_80,30,81920, BigDecimal.valueOf(12000));
                SubscriptionWS subscriptionWS = new SubscriptionWS(subscriberNumber, planDescriptionWS);
                PlanChangeRequestWS planChangeRequestWS = getPlanChangeRequestWS(adennetUserWS.getUserId(),"2022-06-27 16:24:00","2022-07-27 16:24:00",subscriptionWS);
                logger.debug("PlanChangeRequestWS : {}",planChangeRequestWS);

                Integer changePlanResponse = changePlan(planChangeRequestWS);
                assertNotNull(String.format("Expected change plan for userId=%s and subscriberNumber=%s", adennetUserWS.getUserId(), subscriberNumber), changePlanResponse);
                logger.debug(CHANGE_PLAN_RESPONSE,changePlanResponse);
            });
        } finally {
            clearTestDataForUser(adennetUserWS.getId());
        }
    }

    /**
     * Verify that changePlan api should return 405
     * if request method is other than GET
     */
    @Test(priority = 6, enabled = true)
    public void testChangePlanApi405() {
        try{
            testBuilder.given(envBuilder -> {

                PlanDescriptionWS planDescriptionWS = getPlanDescriptionWS(PLAN_ID_INVALID,"12 GB",30,1024,BigDecimal.valueOf(1000));
                SubscriptionWS subscriptionWS = new SubscriptionWS(SUBSCRIBER_NUMBER_INVALID,planDescriptionWS);
                PlanChangeRequestWS planChangeRequestWS = getPlanChangeRequestWS(USER_ID_INVALID,"2022-06-28 16:24:00","2022-07-28 16:24:00",subscriptionWS);

                Integer changePlanResponse = checkMethodNotAllowedResponseForChangePlanApi (planChangeRequestWS);
                assertEquals(String.format(EXPECTED_METHOD_NOT_ALLOWED_BUT_FOUND_PLAN_FOR_USER_ID_AND_SUBSCRIBER_NUMBER, USER_ID_INVALID, SUBSCRIBER_NUMBER_INVALID),null, changePlanResponse);
                logger.debug(CHANGE_PLAN_RESPONSE,changePlanResponse);
            });
        } catch (Exception exception) {
            //TODO
        }
    }

    @AfterClass
    private void teardown(){

    }

}


