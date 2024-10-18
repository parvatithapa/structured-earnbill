package com.sapienter.jbilling.server.spc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.user.UserWS;

@Test(groups = "agl", testName = "agl.BasicSpcAglTest")
public class BasicSpcAglTest extends SPCBaseConfiguration {

    private static final String TEST_CUSTOMER = "Test-1";
    private static final String optusPlan = "SPCMO-01";
    private static final int BILLIING_TYPE_MONTHLY = 1;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeClass
    public void initialize() {
        System.out.println("BasicSpcAglTest.initialize"+testBuilder);
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        System.out.println("BasicSpcAglTest.initialize"+testBuilder);
    }

    @AfterClass
    public void afterTests() {
        System.out.println("BasicSpcAglTest.afterTests");
    }

    @Test(priority = 1)
    public void testPlan() {

        testBuilder.given(envBuilder -> {
            // Creating PLAN: SPCMO-01

                String optusPlanDescription = "Optus Budget - $10";
                String planTypeOptus = "Optus";
                String optusPlanServiceType = "Mobile";
                BigDecimal optusPlanPrice = new BigDecimal("9.0909");
                BigDecimal optusPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024)
                                                                                     // 200
                                                                                     // MB
                BigDecimal optusPlanBoostQuantity = new BigDecimal("1024");
                Integer optusPlanBoostCount = new Integer("3");

                String rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_', '-');

                Map<String, String> optusPlanMetaFieldCodeMap = new HashMap<>();
                optusPlanMetaFieldCodeMap.put("USAGE_POOL_CODE", "410026-150");
                optusPlanMetaFieldCodeMap.put("USAGE_POOL_GL_CODE", "410026-150");
                optusPlanMetaFieldCodeMap.put("COST_GL_CODE", "410026-150");
                optusPlanMetaFieldCodeMap.put("REVENUE_GL_CODE", "410026-150");

                logger.debug("************************ Start creating plan : " + optusPlan + ", " + optusPlanDescription);
                Integer optusPlanId = CreatePlanUtility.createPlan(api, optusPlan, planTypeOptus, optusPlanServiceType,
                        optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, true, optusPlanUsagePoolQuantity,
                        optusPlanBoostCount, optusPlanBoostQuantity, optusPlanMetaFieldCodeMap);
                logger.info("Optus PlanId: {}", optusPlanId);

                // Creating PLAN: SPCMT-02
                String telstraPlan = "SPCMT-02";
                String telstraPlanDescription = "Southern 4G $20";
                String planTypeTelstra = "Telstra";
                String telstraPlanServiceType = "Mobile";
                BigDecimal telstraPlanPrice = new BigDecimal("18.1818");
                BigDecimal telstraPlanUsagePoolQuantity = new BigDecimal("209715200"); // 1024×1024×1024×(200÷1024)
                                                                                       // 200
                                                                                       // MB
                BigDecimal telstraPlanBoostQuantity = new BigDecimal("1024");
                Integer telstraPlanBoostCount = new Integer("3");

                String rate_card_name_2_with_hypen = ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_1.replace('_', '-');

                Map<String, String> telstraPlanMetaFieldCodeMap = new HashMap<>();
                telstraPlanMetaFieldCodeMap.put("USAGE_POOL_CODE", "402101-103");
                telstraPlanMetaFieldCodeMap.put("USAGE_POOL_GL_CODE", "410026-150");
                telstraPlanMetaFieldCodeMap.put("COST_GL_CODE", "602101-206");
                telstraPlanMetaFieldCodeMap.put("REVENUE_GL_CODE", "410026-150");

                logger.debug("************************ Start creating plan : " + telstraPlan + ", " + telstraPlanDescription);
                Integer telstraPlanId = CreatePlanUtility.createPlan(api, telstraPlan, planTypeTelstra, telstraPlanServiceType,
                        telstraPlanDescription, "SPC", rate_card_name_2_with_hypen, "x", telstraPlanPrice, true,
                        telstraPlanUsagePoolQuantity, telstraPlanBoostCount, telstraPlanBoostQuantity, telstraPlanMetaFieldCodeMap);
                logger.info("Telstra PlanId: {}", telstraPlanId);

                // Creating Data PLAN: NBNB-94
                String dataPlan = "NBNB-94";
                String dataPlanDescription = "NBN 100GB 12/1";
                BigDecimal dataPlanPrice = new BigDecimal("40.9091");
                String planTypeInternet = "Internet";
                String dataPlanServiceType = "Data";
                BigDecimal dataPlanUsagePool = new BigDecimal("107374182400");

                Map<String, String> dataMetaFieldCodeMap = new HashMap<>();
                dataMetaFieldCodeMap.put("USAGE_POOL_GL_CODE", "403501-103");
                dataMetaFieldCodeMap.put("COST_GL_CODE", "603521-209");
                dataMetaFieldCodeMap.put("REVENUE_GL_CODE", "");

                logger.debug("************************ Start creating plan : " + dataPlan + ", " + dataPlanDescription);
                CreatePlanUtility.createPlan(api, dataPlan, planTypeInternet, dataPlanServiceType, dataPlanDescription, "SPC", "X", "NBN",
                        dataPlanPrice, false, dataPlanUsagePool, 0, new BigDecimal(0), dataMetaFieldCodeMap);

                // Creating voice PLAN: SPCVV-01
                String voipPlan = "SPCVV-01";
                String voipPlanDescription = "VOIP Bundle Large";
                BigDecimal voipPlanPrice = new BigDecimal("27.2727");
                String planTypeNull = null;
                String voipPlanServiceType = "Voice";
                BigDecimal voipPlanUsagePool = null;

                Map<String, String> voipMetaFieldCodeMap = new HashMap<>();
                voipMetaFieldCodeMap.put("USAGE_POOL_GL_CODE", "410026-150");
                voipMetaFieldCodeMap.put("COST_GL_CODE", "");
                voipMetaFieldCodeMap.put("REVENUE_GL_CODE", "");

                logger.debug("************************ Start creating plan : " + voipPlan + ", " + voipPlanDescription);
                String voipPlanRateCard = ROUTE_RATE_CARD_SPC_TF_PLAN_RATING_1.replace('_', '-');
                CreatePlanUtility.createPlan(api, voipPlan, planTypeNull, voipPlanServiceType, voipPlanDescription, "SPC",
                        voipPlanRateCard, "PSTN", voipPlanPrice, false, voipPlanUsagePool, 0, new BigDecimal(0), voipMetaFieldCodeMap);
                /*
                 * // Creating Inbound PLAN: SPCVV-01 String inboundPlan =
                 * "SPCVTAS-01"; String inboundPlanDescription = "TAS1";
                 * BigDecimal inboundPlanPrice = new BigDecimal("15.0000");
                 * String planTypeInbound = "Inbound"; String
                 * inboundPlanServiceType = "Voice-Inbound"; BigDecimal
                 * inboundPlanUsagePool = new BigDecimal("1258291200");
                 * 
                 * Map<String, String> inboundMetaFieldCodeMap = new
                 * HashMap<>();
                 * inboundMetaFieldCodeMap.put("USAGE_POOL_GL_CODE",
                 * "401101-103"); inboundMetaFieldCodeMap.put("COST_GL_CODE",
                 * ""); inboundMetaFieldCodeMap.put("REVENUE_GL_CODE", "");
                 * 
                 * logger.debug("************************ Start creating plan : "
                 * + inboundPlan +", "+ inboundPlanDescription); String
                 * inboundPlanRateCard =
                 * ROUTE_RATE_CARD_SPC_TAS_PLAN_RATING_1.replace('_','-');
                 * CreatePlanUtility.createPlan(api, inboundPlan,
                 * planTypeInbound, inboundPlanServiceType,
                 * inboundPlanDescription, "SPC", inboundPlanRateCard, "",
                 * inboundPlanPrice, false, inboundPlanUsagePool, 0, new
                 * BigDecimal(0), inboundMetaFieldCodeMap);
                 */
            });
    }

    @Test(priority = 2)
    public void testUserAndOrder() {
        testBuilder.given(envBuilder -> {
            UserWS spcTestUserWS = getSPCTestUserWS(envBuilder, TEST_CUSTOMER, new Date(), "", CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
            // optus
            PlanWS planWS = api.getPlanByInternalNumber(optusPlan, api.getCallerCompanyId());
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
            List<AssetWS> assetWSs = new ArrayList<>();

            Integer asset1 = buildAndPersistAsset(envBuilder, getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY), getItemIdByCode(testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), "1231231233", "asset-01");

            assetWSs.add(api.getAsset(asset1));

            createOrderWithAsset("TestOrder", spcTestUserWS.getId(), new Date(), null, MONTHLY_ORDER_PERIOD, BILLIING_TYPE_MONTHLY, true, productQuantityMap, assetWSs, planWS.getId());
        });
    }

}
