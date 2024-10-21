package com.sapienter.jbilling.server.spc.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolResetValueEnum;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResult;

//JBSPC-608,609,611,618,619,515,626,666
public class CreatePlanUtility {

    private static final String FILE_PATH = "/home/jbilling/MigrationData/JBSPC-775/";
    private static final String CONSUMED_50_PERCENT = "50";
    private static final String CONSUMED_85_PERCENT = "85";
    private static final String CONSUMED_100_PERCENT = "100";
    private static final String START_NOTIFICATION_ID = "1021";
    private static final String END_NOTIFICATION_ID = "1022";
    private static final String EXCEEDED_NOTIFICATION_ID = "1023";
    private static Integer USAGE_FEE_PRODUCT_ID = null;
    private static final String USAGE_POOL_GL_CODE = "Usage Pool GL Code";
    private static final String USAGE_POOL_COSTS_GL_CODE = "Usage Pool Costs GL Code";
    private static final int CURRENCY_ID = 11;
    private static final String TARIFF_CODE = "tariff_code";
    private static JbillingAPI api = null;
    private static Logger logger = LoggerFactory.getLogger(CreatePlanUtility.class);
    public static final Date EPOCH_DATE = new DateTime(1970, 1, 1, 0, 0, 0, 0).toDate();
    private static final String CDR_DURATION_FIELD_NAME_VALUE = "DURATION";
    private static final String ROUTE_RATE_CARD_ID = "route_rate_card_id";
    private static final String CDR_DURATION_FIELD_NAME = "cdr_duration_field_name";
    private static final String COMMA_DELIMITER = ",";
    private static final String HYPHEN = "-";
    private static final String FIELD_NAME_CALL_CHARGE = "cdr_call_charge_field_name";
    private static final String CALL_CHARGE_FIELD_NAME_VALUE = "CALL_CHARGE";
    private static Map<String, Integer> categoryMapStatic = new HashMap<>();
    private static Map<String, Integer> productMapStatic = new HashMap<>();
    private static Map<String, Integer> usageMapStatic = new HashMap<>();
    private static Map<Integer, String> routeRateCardMapStatic = new HashMap<>();
    private static String[] productCodes = { "SE-INT-TOTAL", "AAPT-INT-TOTAL", "Satellite-INT-TOTAL", "SCONNECT-INT-TOTAL",
            "internet_user_names", "voip_number_exclude_zero_price_asset", "SCONNECT-INT-DOWNLOAD", "mobile_numbers", "DBC-10 (Optus)",
            "DBC-10 (Telstra)", "voice_numbers" };
    private static Integer monthlyPeriodId = 0;

    private static Set<String> rateCardsList = new HashSet<>();
    private static Map<String, String> rateCardmap = new HashMap<>();

    public static Integer createPlan(JbillingAPI jbillingApi, String planCode, String planType, String category, String planDescription,
            String planOrigin, String rateCardName, String internetTechnology, BigDecimal price, boolean havingBoost,
            BigDecimal mainPoolQuantity, Integer boostCount, BigDecimal boostQuantity) {
        boolean OPTUS = false;
        boolean TELSTRA = false;
        boolean IS_INTERNET_PLAN = false;
        boolean IS_SET_USAGE_FEE_PRODUCT = false;
        api = jbillingApi;
        // plan Type: Internet (NBN), Optus(), Telstra
        // Plan service type: Mobile, voice, Inbound
        // technology: PSTN

        // String sConnect, optus, aapt,

        logger.info("Plan creation started for planType: {}", planType);
        if (!"INTERNET".equalsIgnoreCase(planType)) {
            routeRateCardMapStatic = getRouteRateCardNames();
        }
        if ("Mobile".equalsIgnoreCase(category) && "OPTUS".equalsIgnoreCase(planType)) {
            OPTUS = true;
        }
        if ("Mobile".equalsIgnoreCase(category) && "TELSTRA".equalsIgnoreCase(planType)) {
            TELSTRA = true;
        }
        if ("INTERNET".equalsIgnoreCase(planType)) {
            IS_INTERNET_PLAN = true;
        }

        monthlyPeriodId = getMonthlyOrderPeriod();
        categoryMapStatic = getAllCategory();
        for (String productCode : productCodes) {
            productMapStatic.put(productCode, api.getItemID(productCode));
        }

        fetchItemByCategory(category);

        if ("OPTUS".equalsIgnoreCase(planType)) {
            IS_INTERNET_PLAN = false;
            USAGE_FEE_PRODUCT_ID = productMapStatic.get("DBC-10 (Optus)");
        } else if ("TELSTRA".equalsIgnoreCase(planType)) {
            IS_INTERNET_PLAN = false;
            USAGE_FEE_PRODUCT_ID = productMapStatic.get("DBC-10 (Telstra)");
        }

        usageMapStatic = getAllUsagePool();

        Plan plan = configurePlan(planCode, planType, category, planDescription, planOrigin, rateCardName, internetTechnology, price,
                havingBoost, mainPoolQuantity, boostCount, boostQuantity, OPTUS, TELSTRA, IS_INTERNET_PLAN);
        Integer planId = createPlan(plan, IS_INTERNET_PLAN);
        logger.info("Plan: {} created", planId);

        return planId;
    }

    public static Integer createPlan(JbillingAPI jbillingApi, String planCode, String planType, String category, String planDescription,
            String planOrigin, String rateCardName, String internetTechnology, BigDecimal price, boolean havingBoost,
            BigDecimal mainPoolQuantity, Integer boostCount, BigDecimal boostQuantity, Map<String, String> planMetaFieldCodeMap) {
        boolean OPTUS = false;
        boolean TELSTRA = false;
        boolean IS_INTERNET_PLAN = false;
        boolean IS_SET_USAGE_FEE_PRODUCT = false;
        api = jbillingApi;
        // plan Type: Internet (NBN), Optus(), Telstra
        // Plan service type: Mobile, voice, Inbound
        // technology: PSTN

        // String sConnect, optus, aapt,

        logger.info("Plan creation started for planType: {}", planType);
        if (!"INTERNET".equalsIgnoreCase(planType)) {
            routeRateCardMapStatic = getRouteRateCardNames();
        }
        if ("Mobile".equalsIgnoreCase(category) && "OPTUS".equalsIgnoreCase(planType)) {
            OPTUS = true;
        }
        if ("Mobile".equalsIgnoreCase(category) && "TELSTRA".equalsIgnoreCase(planType)) {
            TELSTRA = true;
        }
        if ("INTERNET".equalsIgnoreCase(planType)) {
            IS_INTERNET_PLAN = true;
        }

        monthlyPeriodId = getMonthlyOrderPeriod();
        categoryMapStatic = getAllCategory();
        for (String productCode : productCodes) {
            productMapStatic.put(productCode, api.getItemID(productCode));
        }

        fetchItemByCategory(category);

        if ("OPTUS".equalsIgnoreCase(planType)) {
            IS_INTERNET_PLAN = false;
            USAGE_FEE_PRODUCT_ID = productMapStatic.get("DBC-10 (Optus)");
        } else if ("TELSTRA".equalsIgnoreCase(planType)) {
            IS_INTERNET_PLAN = false;
            USAGE_FEE_PRODUCT_ID = productMapStatic.get("DBC-10 (Telstra)");
        }

        usageMapStatic = getAllUsagePool();

        Plan plan = configurePlan(planCode, planType, category, planDescription, planOrigin, rateCardName, internetTechnology, price,
                havingBoost, mainPoolQuantity, boostCount, boostQuantity, planMetaFieldCodeMap, OPTUS, TELSTRA, IS_INTERNET_PLAN);
        Integer planId = createPlan(plan, IS_INTERNET_PLAN);
        logger.info("Plan: {} created", planId);

        return planId;
    }

    /*
     * planCode: Plan Number planType: Internet (NBN), Optus, Telstra category:
     * Plan service type like Mobile, voice, Inbound planDescription: plan
     * description planOrigin: SPC rateCardName: Rate card name
     * internetTechnology: technology used for service like PSTN price: Plan
     * price havingBoost: Is usage pool boost required mainPoolQuantity: Main
     * usage pool quantity boostCount: Number of usage pool boost boostQuantity:
     * Boost pool quantity
     */
    private static Plan configurePlan(String planCode, String planType, String category, String planDescription, String planOrigin,
                                      String rateCardName, String internetTechnology, BigDecimal price, boolean havingBoost, 
                                      BigDecimal mainPoolQuantity, Integer boostCount, BigDecimal boostQuantity, 
                                      boolean OPTUS, boolean TELSTRA, boolean IS_INTERNET_PLAN) {

        // String usagePoolCode;
        String usagePoolGLCode;
        String costGLCode;
        String revenueGLCode;

        if ("Optus".equalsIgnoreCase(planType)) {
            usagePoolGLCode = "402201-103";
            costGLCode = "602201-207";
            revenueGLCode = "";
        } else if ("Telstra".equalsIgnoreCase(planType)) {
            usagePoolGLCode = "402101-103";
            costGLCode = "602101-206";
            revenueGLCode = "";
        } else if ("Internet".equalsIgnoreCase(planType)) {
            usagePoolGLCode = "403501-103";
            costGLCode = "603521-209";
            revenueGLCode = "";
        } else {
            // usagePoolCode = "410026-150";
            usagePoolGLCode = "410026-150";
            costGLCode = "410026-150";
            revenueGLCode = "410026-150";
        }

        PlanWS planByInternalNumber = null;
        try {
            planByInternalNumber = api.getPlanByInternalNumber(planCode, api.getCallerCompanyId());
            logger.info("Plan found with plan number: {} and id: {}", planCode, planByInternalNumber.getId());
        } catch (Exception e) {
            planByInternalNumber = null;
            logger.error("Plan not found with plan number: {}", planCode);
        }
        Plan plan = null;
        if (planByInternalNumber == null) {
            plan = new Plan();
            String aapt = "Yes"; // "YES";
            String vocusSpc = "NO"; // "YES";
            String sConnect = "Yes"; // "YES";
            String lbnco = "NO"; // "YES";
            String engine = "Yes"; // "YES";
            String telstra = "NO"; // "YES";
            String optus = "NO"; // "YES";

            if ("Optus".equalsIgnoreCase(planType)) {
                optus = "Yes";
            }
            if ("Telstra".equalsIgnoreCase(planType)) {
                telstra = "Yes";
            }
            if ("Internet".equalsIgnoreCase(planType)) {
                aapt = "Yes";
                sConnect = "Yes";
            }
            if ("Inbound".equalsIgnoreCase(planType) && "Voice-Inbound".equalsIgnoreCase(category)) {
                optus = "Yes";
            }

            usagePoolGLCode = usagePoolGLCode;
            if (!IS_INTERNET_PLAN && rateCardName.contains("|")) {
                rateCardName = rateCardName.replace("|", ",");
            }
            rateCardsList.add(rateCardName);

            if (internetTechnology.equalsIgnoreCase("Satellite")) {
                internetTechnology = "NBN";
            } else if (internetTechnology.equalsIgnoreCase("DSL")) {
                internetTechnology = "ADSL";
            } else if (internetTechnology.equalsIgnoreCase("mobile")) {
                internetTechnology = "Mobile";
            }

            List<Integer> categoryList = new ArrayList<>();
            List<Integer> itemList = new ArrayList<>();
            boolean IS_SET_USAGE_FEE_PRODUCT = true;
            if (category.contains("Inbound")) {
                categoryList.add(categoryMapStatic.get("Inbound Services"));
                if (!itemList.contains(productMapStatic.get("voice_numbers"))) {
                    itemList.add(productMapStatic.get("voice_numbers"));
                }

                if (aapt.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("aa_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (sConnect.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("sc_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (optus.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("op_inbound_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (engine.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("en_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (telstra.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("tf_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

            }

            if (category.contains("Voice") && !category.contains("Inbound")) {
                categoryList.add(categoryMapStatic.get("Voice Services"));
                if ((internetTechnology.equalsIgnoreCase("voip") || internetTechnology.equalsIgnoreCase("NBN"))
                        && !itemList.contains(productMapStatic.get("voip_number_exclude_zero_price_asset"))) {
                    itemList.add(productMapStatic.get("voip_number_exclude_zero_price_asset"));
                }

                if (internetTechnology.equalsIgnoreCase("PSTN") && !itemList.contains(productMapStatic.get("voice_numbers"))) {
                    itemList.add(productMapStatic.get("voice_numbers"));
                }

                if (aapt.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("aa_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (sConnect.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("sc_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (optus.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("op_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (engine.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("en_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (telstra.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("tf_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

            }

            if (category.contains("Mobile")) {
                categoryList.add(categoryMapStatic.get("Mobile Services"));
                if (!itemList.contains(productMapStatic.get("mobile_numbers"))) {
                    itemList.add(productMapStatic.get("mobile_numbers"));
                }

                if (aapt.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("aa_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (sConnect.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("sc_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (optus.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("om_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (engine.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("en_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (telstra.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("tm_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

            }

            if (category.contains("Data")) {
                categoryList.add(categoryMapStatic.get("Internet Services"));
                if (!itemList.contains(productMapStatic.get("internet_user_names"))) {
                    itemList.add(productMapStatic.get("internet_user_names"));
                }

                if (aapt.equalsIgnoreCase("YES")) {
                    itemList.add(productMapStatic.get("AAPT-INT-TOTAL"));
                }

                if (sConnect.equalsIgnoreCase("YES") || vocusSpc.equalsIgnoreCase("YES")) {
                    itemList.add(productMapStatic.get("SCONNECT-INT-TOTAL"));
                }

                if (optus.equalsIgnoreCase("YES")) {
                    itemList.add(productMapStatic.get("Satellite-INT-TOTAL"));
                }
            }

            String usageTotal = (null != mainPoolQuantity ? mainPoolQuantity.toString() : "");
            if (IS_INTERNET_PLAN) {
                plan.setUsagePoolCostsGLCode(costGLCode);
            }

            String tariffCodes = "";
            List<Integer> usagePoolList = new ArrayList<>();
            String usagePoolName = planDescription;
            if (!planDescription.startsWith("$")) {
                usagePoolName = planDescription.split("\\$")[0];
            }
            String boostUsagePoolName = usagePoolName;
            if (!category.equalsIgnoreCase("Inbound") && null != mainPoolQuantity) {
                Double usageQuantity = ((Double.parseDouble(usageTotal) / 1024) / 1024) / 1024;
                if (usageQuantity >= 1) {
                    usagePoolName = usagePoolName.concat("-").concat(usageQuantity + " GB");
                } else {
                    usageQuantity = (Double.parseDouble(usageTotal) / 1024) / 1024;
                    usagePoolName = usagePoolName.concat("-").concat(usageQuantity + " MB");
                }
            }

            if ("Inbound".equalsIgnoreCase(category) || ("Inbound".equalsIgnoreCase(planType))) {
                /*
                 * tariffCodes =
                 * dataFormatter.formatCellValue(currentRow.getCell(98)); String
                 * numberOfCall =
                 * dataFormatter.formatCellValue(currentRow.getCell(99));
                 */
                tariffCodes = "OP:13LOC";
                String numberOfCall = "1200";
                if (null != tariffCodes && tariffCodes.contains("|")) {
                    String[] tariffCodeStr = tariffCodes.split("\\|");
                    String[] callQuantityStr = numberOfCall.split("\\|");
                    for (int i = 0; i < tariffCodeStr.length; i++) {
                        usagePoolList.addAll(createUsagePool(usagePoolName + "-" + (i + 1), boostUsagePoolName, tariffCodeStr[i],
                                callQuantityStr[i], rateCardName, category, havingBoost, boostCount.toString(), boostQuantity.intValue(),
                                OPTUS, TELSTRA, IS_INTERNET_PLAN, IS_SET_USAGE_FEE_PRODUCT));
                    }
                } else {
                    usagePoolList.addAll(createUsagePool(usagePoolName, boostUsagePoolName, tariffCodes, numberOfCall, rateCardName,
                            category, havingBoost, boostCount.toString(), boostQuantity.intValue(), OPTUS, TELSTRA, IS_INTERNET_PLAN,
                            IS_SET_USAGE_FEE_PRODUCT));
                }
            } else if (null != mainPoolQuantity) {
                usagePoolList = createUsagePool(usagePoolName, boostUsagePoolName, "", mainPoolQuantity.toString(), // usagePoolName+"-"+(i+1),
                    // tariffCodeStr[i],
                    // callQuantityStr[i],
                        rateCardName, category, havingBoost, boostCount.toString(), boostQuantity.intValue(), OPTUS, TELSTRA,
                        IS_INTERNET_PLAN, IS_SET_USAGE_FEE_PRODUCT);
            }

            plan.setPeriod(monthlyPeriodId);
            plan.setPlanNumber(planCode);
            plan.setPlanName(planDescription.trim());
            // plan.setUsagePoolName(usagePoolName);
            plan.setUsagePoolIds(usagePoolList.toArray(new Integer[0]));

            /*
             * plan.setStatus(dataFormatter.formatCellValue(currentRow
             * .getCell(13)));
             */
            Integer routeRateCardId = null;
            if (rateCardmap.get(rateCardName) != null) {
                routeRateCardId = getRouteRateCardIdByValue(rateCardmap.get(rateCardName));
            } else {
                routeRateCardId = getRouteRateCardIdByValue(rateCardName);
            }
            if (null != rateCardName) {
                if (rateCardmap.get(rateCardName) != null) {
                    plan.setRouteRateCard(rateCardmap.get(rateCardName).replaceAll("-", "_"));
                } else {
                    plan.setRouteRateCard(rateCardName.replaceAll("-", "_"));
                }
            }
            if (null != routeRateCardId) {
                plan.setRouteRateCardId(routeRateCardId.toString());
            }

            plan.setTechnology(internetTechnology);
            plan.setExGST(price.doubleValue());
            plan.setGst(price.multiply(new BigDecimal("0.1")).doubleValue());
            plan.setTotal(price.multiply(new BigDecimal("1.1")).doubleValue());
            plan.setRevenueGLCode(revenueGLCode);
            plan.setCostGLCode(costGLCode);
            plan.setUsagePoolGLCode(usagePoolGLCode);

            String[] unbilledUnearned = new String[] { "Unbilled", "Unearned" };

            List<String> category1 = Arrays.asList(category.split(","));
            for (String cat : category1) {
                if (categoryMapStatic.containsKey(category)) {
                    categoryList.add(categoryMapStatic.get(cat));
                }
            }

            for (java.util.Map.Entry<String, Integer> entry : categoryMapStatic.entrySet()) {
                if ((entry.getKey().contains(unbilledUnearned[0]) || entry.getKey().contains(unbilledUnearned[1]))
                        && entry.getKey().contains("Included")) {
                    categoryList.add(entry.getValue());
                }
            }
            plan.setCategories(categoryList.toArray(new Integer[0]));
            plan.setItems(itemList.toArray(new Integer[0]));

        }
        return plan;

    }

    /*
     * planCode: Plan Number planType: Internet (NBN), Optus, Telstra category:
     * Plan service type like Mobile, voice, Inbound planDescription: plan
     * description planOrigin: SPC rateCardName: Rate card name
     * internetTechnology: technology used for service like PSTN price: Plan
     * price havingBoost: Is usage pool boost required mainPoolQuantity: Main
     * usage pool quantity boostCount: Number of usage pool boost boostQuantity:
     * Boost pool quantity
     */
    private static Plan configurePlan(String planCode, String planType, String category, String planDescription, String planOrigin,
                                      String rateCardName, String internetTechnology, BigDecimal price, boolean havingBoost, BigDecimal mainPoolQuantity,
                                      Integer boostCount, BigDecimal boostQuantity, Map<String, String> planMetaFieldCodeMap,
                                      boolean OPTUS, boolean TELSTRA, boolean IS_INTERNET_PLAN) {

        // String usagePoolCode;
        String usagePoolGLCode;
        String costGLCode;
        String revenueGLCode;

        if (null != planMetaFieldCodeMap && planMetaFieldCodeMap.size() > 0) {
            // usagePoolCode = planMetaFieldCodeMap.get("USAGE_POOL_CODE");
            usagePoolGLCode = planMetaFieldCodeMap.get("USAGE_POOL_GL_CODE");
            costGLCode = planMetaFieldCodeMap.get("COST_GL_CODE");
            revenueGLCode = planMetaFieldCodeMap.get("REVENUE_GL_CODE");
        } else {
            // usagePoolCode = "410026-150";
            usagePoolGLCode = "410026-150";
            costGLCode = "410026-150";
            revenueGLCode = "410026-150";
        }

        PlanWS planByInternalNumber = null;
        try {
            planByInternalNumber = api.getPlanByInternalNumber(planCode, api.getCallerCompanyId());
            logger.info("Plan found with plan number: {} and id: {}", planCode, planByInternalNumber.getId());
        } catch (Exception e) {
            planByInternalNumber = null;
            logger.error("Plan not found with plan number: {}", planCode);
        }
        Plan plan = null;
        if (planByInternalNumber == null) {
            plan = new Plan();
            String aapt = "Yes"; // "YES";
            String vocusSpc = "NO"; // "YES";
            String sConnect = "Yes"; // "YES";
            String lbnco = "NO"; // "YES";
            String engine = "Yes"; // "YES";
            String telstra = "NO"; // "YES";
            String optus = "NO"; // "YES";

            if ("Optus".equalsIgnoreCase(planType)) {
                optus = "Yes";
            }
            if ("Telstra".equalsIgnoreCase(planType)) {
                telstra = "Yes";
            }
            if ("Internet".equalsIgnoreCase(planType)) {
                aapt = "Yes";
                sConnect = "Yes";
            }
            if ("Inbound".equalsIgnoreCase(planType) && "Voice-Inbound".equalsIgnoreCase(category)) {
                optus = "Yes";
            }

            usagePoolGLCode = usagePoolGLCode;
            if (!IS_INTERNET_PLAN && rateCardName.contains("|")) {
                rateCardName = rateCardName.replace("|", ",");
            }
            rateCardsList.add(rateCardName);

            if (internetTechnology.equalsIgnoreCase("Satellite")) {
                internetTechnology = "NBN";
            } else if (internetTechnology.equalsIgnoreCase("DSL")) {
                internetTechnology = "ADSL";
            } else if (internetTechnology.equalsIgnoreCase("mobile")) {
                internetTechnology = "Mobile";
            }

            List<Integer> categoryList = new ArrayList<>();
            List<Integer> itemList = new ArrayList<>();
            boolean IS_SET_USAGE_FEE_PRODUCT = true;
            if (category.contains("Inbound")) {
                categoryList.add(categoryMapStatic.get("Inbound Services"));
                if (!itemList.contains(productMapStatic.get("voice_numbers"))) {
                    itemList.add(productMapStatic.get("voice_numbers"));
                }

                if (aapt.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("aa_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (sConnect.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("sc_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (optus.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("op_inbound_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (engine.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("en_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (telstra.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("tf_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

            }

            if (category.contains("Voice") && !category.contains("Inbound")) {
                categoryList.add(categoryMapStatic.get("Voice Services"));
                if ((internetTechnology.equalsIgnoreCase("voip") || internetTechnology.equalsIgnoreCase("NBN"))
                        && !itemList.contains(productMapStatic.get("voip_number_exclude_zero_price_asset"))) {
                    itemList.add(productMapStatic.get("voip_number_exclude_zero_price_asset"));
                }

                if (internetTechnology.equalsIgnoreCase("PSTN") && !itemList.contains(productMapStatic.get("voice_numbers"))) {
                    itemList.add(productMapStatic.get("voice_numbers"));
                }

                if (aapt.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("aa_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (sConnect.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("sc_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (optus.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("op_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (engine.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("en_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (telstra.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("tf_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

            }

            if (category.contains("Mobile")) {
                categoryList.add(categoryMapStatic.get("Mobile Services"));
                if (!itemList.contains(productMapStatic.get("mobile_numbers"))) {
                    itemList.add(productMapStatic.get("mobile_numbers"));
                }

                if (aapt.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("aa_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (sConnect.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("sc_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (optus.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("om_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (engine.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("en_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

                if (telstra.equalsIgnoreCase("YES")) {
                    for (String itemCode : productMapStatic.keySet()) {
                        if (null != itemCode && itemCode.startsWith("tm_")) {
                            itemList.add(productMapStatic.get(itemCode));
                        }
                    }
                }

            }

            if (category.contains("Data")) {
                categoryList.add(categoryMapStatic.get("Internet Services"));
                if (!itemList.contains(productMapStatic.get("internet_user_names"))) {
                    itemList.add(productMapStatic.get("internet_user_names"));
                }

                if (aapt.equalsIgnoreCase("YES")) {
                    itemList.add(productMapStatic.get("AAPT-INT-TOTAL"));
                }

                if (sConnect.equalsIgnoreCase("YES") || vocusSpc.equalsIgnoreCase("YES")) {
                    itemList.add(productMapStatic.get("SCONNECT-INT-TOTAL"));
                }

                if (optus.equalsIgnoreCase("YES")) {
                    itemList.add(productMapStatic.get("Satellite-INT-TOTAL"));
                }
            }

            String usageTotal = (null != mainPoolQuantity ? mainPoolQuantity.toString() : "");
            if (IS_INTERNET_PLAN) {
                plan.setUsagePoolCostsGLCode(costGLCode);
            }

            String tariffCodes = "";
            List<Integer> usagePoolList = new ArrayList<>();
            String usagePoolName = planDescription;
            if (!planDescription.startsWith("$")) {
                usagePoolName = planDescription.split("\\$")[0];
            }
            String boostUsagePoolName = usagePoolName;
            if (!category.equalsIgnoreCase("Inbound") && null != mainPoolQuantity) {
                Double usageQuantity = ((Double.parseDouble(usageTotal) / 1024) / 1024) / 1024;
                if (usageQuantity >= 1) {
                    usagePoolName = usagePoolName.concat("-").concat(usageQuantity + " GB");
                } else {
                    usageQuantity = (Double.parseDouble(usageTotal) / 1024) / 1024;
                    usagePoolName = usagePoolName.concat("-").concat(usageQuantity + " MB");
                }
            }

            if ("Inbound".equalsIgnoreCase(category) || ("Inbound".equalsIgnoreCase(planType))) {
                /*
                 * tariffCodes =
                 * dataFormatter.formatCellValue(currentRow.getCell(98)); String
                 * numberOfCall =
                 * dataFormatter.formatCellValue(currentRow.getCell(99));
                 */
                tariffCodes = "OP:13LOC";
                String numberOfCall = "1200";
                if (null != tariffCodes && tariffCodes.contains("|")) {
                    String[] tariffCodeStr = tariffCodes.split("\\|");
                    String[] callQuantityStr = numberOfCall.split("\\|");
                    for (int i = 0; i < tariffCodeStr.length; i++) {
                        usagePoolList.addAll(createUsagePool(usagePoolName + "-" + (i + 1), boostUsagePoolName, tariffCodeStr[i],
                                callQuantityStr[i], rateCardName, category, havingBoost, boostCount.toString(), boostQuantity.intValue(),
                                OPTUS, TELSTRA, IS_INTERNET_PLAN, IS_SET_USAGE_FEE_PRODUCT));
                    }
                } else {
                    usagePoolList.addAll(createUsagePool(usagePoolName, boostUsagePoolName, tariffCodes, numberOfCall, rateCardName,
                            category, havingBoost, boostCount.toString(), boostQuantity.intValue(), OPTUS, TELSTRA, IS_INTERNET_PLAN,
                            IS_SET_USAGE_FEE_PRODUCT));
                }
            } else if (null != mainPoolQuantity) {
                usagePoolList = createUsagePool(usagePoolName, boostUsagePoolName, "", mainPoolQuantity.toString(), // usagePoolName+"-"+(i+1),
                                                                                                                    // tariffCodeStr[i],
                                                                                                                    // callQuantityStr[i],
                        rateCardName, category, havingBoost, boostCount.toString(), boostQuantity.intValue(), OPTUS, TELSTRA,
                        IS_INTERNET_PLAN, IS_SET_USAGE_FEE_PRODUCT);
            }

            plan.setPeriod(monthlyPeriodId);
            plan.setPlanNumber(planCode);
            plan.setPlanName(planDescription.trim());
            // plan.setUsagePoolName(usagePoolName);
            plan.setUsagePoolIds(usagePoolList.toArray(new Integer[0]));

            /*
             * plan.setStatus(dataFormatter.formatCellValue(currentRow
             * .getCell(13)));
             */
            Integer routeRateCardId = null;
            if (rateCardmap.get(rateCardName) != null) {
                routeRateCardId = getRouteRateCardIdByValue(rateCardmap.get(rateCardName));
            } else {
                routeRateCardId = getRouteRateCardIdByValue(rateCardName);
            }
            if (null != rateCardName) {
                if (rateCardmap.get(rateCardName) != null) {
                    plan.setRouteRateCard(rateCardmap.get(rateCardName).replaceAll("-", "_"));
                } else {
                    plan.setRouteRateCard(rateCardName.replaceAll("-", "_"));
                }
            }
            if (null != routeRateCardId) {
                plan.setRouteRateCardId(routeRateCardId.toString());
            }

            plan.setTechnology(internetTechnology);
            plan.setExGST(price.doubleValue());
            plan.setGst(price.multiply(new BigDecimal("0.1")).doubleValue());
            plan.setTotal(price.multiply(new BigDecimal("1.1")).doubleValue());
            plan.setRevenueGLCode(revenueGLCode);
            plan.setCostGLCode(costGLCode);
            plan.setUsagePoolGLCode(usagePoolGLCode);

            String[] unbilledUnearned = new String[] { "Unbilled", "Unearned" };

            List<String> category1 = Arrays.asList(category.split(","));
            for (String cat : category1) {
                if (categoryMapStatic.containsKey(category)) {
                    categoryList.add(categoryMapStatic.get(cat));
                }
            }

            for (java.util.Map.Entry<String, Integer> entry : categoryMapStatic.entrySet()) {
                if ((entry.getKey().contains(unbilledUnearned[0]) || entry.getKey().contains(unbilledUnearned[1]))
                        && entry.getKey().contains("Included")) {
                    categoryList.add(entry.getValue());
                }
            }
            plan.setCategories(categoryList.toArray(new Integer[0]));
            plan.setItems(itemList.toArray(new Integer[0]));

        }
        return plan;

    }

    private static Integer createPlan(Plan plan, boolean IS_INTERNET_PLAN) {
        Integer planItemId1 = api.getItemID(plan.getPlanNumber());
        if (planItemId1 != null) {
            logger.info("Plan item found with item id: {} and code: {}", planItemId1, plan.getPlanNumber());
            PlanWS planWS = api.getPlanByInternalNumber(plan.getPlanNumber(), api.getCallerCompanyId());
            return planWS.getId();
        } else {
            ItemDTOEx planItem = new ItemDTOEx();
            Integer callerCompanyId = api.getCallerCompanyId();
            planItem.setEntityId(callerCompanyId);
            planItem.setDescription(plan.getPlanName());
            planItem.setTypes(plan.getCategories());
            String price = null;
            try {
                price = new DecimalFormat("##.#####").format(plan.getExGST());
            } catch (Exception e) {
                logger.error("Error while parsing price: {} for plan number: {}", plan.getExGST(), plan.getPlanNumber());
            }
            planItem.setPrice(price);
            planItem.setNumber(plan.getPlanNumber());
            planItem.setCurrencyId(CURRENCY_ID);
            planItem.setPriceModelCompanyId(callerCompanyId);
            planItem.setEntityId(callerCompanyId);
            planItem.getEntities().add(callerCompanyId);

            List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
            MetaFieldValueWS taxSchemeMetaField = new MetaFieldValueWS();
            taxSchemeMetaField.setFieldName("Tax Scheme");
            taxSchemeMetaField.setDataType(DataType.ENUMERATION);
            taxSchemeMetaField.setValue("Regular GST");
            taxSchemeMetaField.setEntityId(callerCompanyId);
            metaFieldValues.add(taxSchemeMetaField);

            TreeMap<Integer, MetaFieldValueWS[]> itemMetafieldMap = new TreeMap();
            itemMetafieldMap.put(callerCompanyId, metaFieldValues.toArray(new MetaFieldValueWS[0]));
            planItem.setMetaFieldsMap(itemMetafieldMap);

            Integer planItemId = null;
            try {
                planItemId = api.createItem(planItem);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error while creating plan item with plan description: {}, {}", planItem.getDescription(), e);
            }

            PriceModelWS priceModel = new PriceModelWS("FLAT", BigDecimal.ZERO, CURRENCY_ID);
            SortedMap<Date, PriceModelWS> flatModels = new TreeMap();
            flatModels.put(EPOCH_DATE, priceModel);

            SortedMap<Date, PriceModelWS> models = new TreeMap();
            PriceModelWS priceModelWS = new PriceModelWS();
            priceModelWS.setId(null);
            priceModelWS.setRate(BigDecimal.ZERO);
            priceModelWS.setType("ROUTE_BASED_RATE_CARD");
            priceModelWS.setCurrencyId(CURRENCY_ID);
            Map<String, String> attributes = new HashMap();
            attributes.put(CDR_DURATION_FIELD_NAME, CDR_DURATION_FIELD_NAME_VALUE);
            attributes.put(ROUTE_RATE_CARD_ID, plan.getRouteRateCardId());
            attributes.put(FIELD_NAME_CALL_CHARGE, CALL_CHARGE_FIELD_NAME_VALUE);
            priceModelWS.setAttributes(attributes);
            models.put(CommonConstants.EPOCH_DATE, priceModelWS);

            PlanWS planWs = new PlanWS();
            planWs.setItemId(planItemId);
            planWs.setDescription(plan.getPlanName());
            planWs.setPeriodId(plan.getPeriod());

            for (Integer itemId : plan.getItems()) {
                PlanItemBundleWS bundle = new PlanItemBundleWS();
                bundle.setPeriodId(plan.getPeriod());
                bundle.setQuantity(BigDecimal.ZERO);
                PlanItemWS planItemWs = new PlanItemWS();

                if (productMapStatic.get("internet_user_names").equals(itemId)
                        || productMapStatic.get("voip_number_exclude_zero_price_asset").equals(itemId)
                        || productMapStatic.get("mobile_numbers").equals(itemId) || productMapStatic.get("voice_numbers").equals(itemId)) {
                    bundle.setQuantity(BigDecimal.ONE);
                    planItemWs.setModels(flatModels);
                } else if (IS_INTERNET_PLAN) {
                    planItemWs.setModels(flatModels);
                } else {
                    planItemWs.setModels(models);
                }
                planItemWs.setItemId(itemId);
                planItemWs.setPrecedence(-1);
                planItemWs.setBundle(bundle);
                planWs.addPlanItem(planItemWs);
            }
            List<MetaFieldValueWS> planMetaFieldValues = new ArrayList<>();
            boolean isVoiceCatPresent = Arrays.stream(plan.getCategories()).anyMatch(categoryMapStatic.get("Voice Services")::equals);
            if (!isVoiceCatPresent) {
                MetaFieldValueWS quantityResolutionUnitsMetafieldField = new MetaFieldValueWS();
                quantityResolutionUnitsMetafieldField.setFieldName("Quantity Resolution Unit");
                quantityResolutionUnitsMetafieldField.setDataType(DataType.ENUMERATION);
                quantityResolutionUnitsMetafieldField.setValue("Total");
                quantityResolutionUnitsMetafieldField.setEntityId(callerCompanyId);
                planMetaFieldValues.add(quantityResolutionUnitsMetafieldField);
            }

            MetaFieldValueWS planTaxSchemeMetaField = new MetaFieldValueWS();
            planTaxSchemeMetaField.setFieldName("Tax Scheme");
            planTaxSchemeMetaField.setDataType(DataType.ENUMERATION);
            planTaxSchemeMetaField.setValue("Regular GST");
            planTaxSchemeMetaField.setEntityId(callerCompanyId);
            planMetaFieldValues.add(planTaxSchemeMetaField);

            MetaFieldValueWS planGLMetaField = new MetaFieldValueWS();
            planGLMetaField.setFieldName("Plan GL");
            planGLMetaField.setDataType(DataType.STRING);
            planGLMetaField.setValue(plan.getRevenueGLCode());
            planGLMetaField.setEntityId(callerCompanyId);
            planMetaFieldValues.add(planGLMetaField);

            if (!plan.getRouteRateCard().equals("X")) {
                MetaFieldValueWS planRatingMetaField = new MetaFieldValueWS();
                planRatingMetaField.setFieldName("Plan Rating");
                planRatingMetaField.setDataType(DataType.ENUMERATION);
                planRatingMetaField.setValue(plan.getRouteRateCard());
                planRatingMetaField.setEntityId(callerCompanyId);
                planMetaFieldValues.add(planRatingMetaField);
            }

            MetaFieldValueWS costsGLCodeMetaField = new MetaFieldValueWS();
            costsGLCodeMetaField.setFieldName("Costs GL Code");
            costsGLCodeMetaField.setDataType(DataType.STRING);
            costsGLCodeMetaField.setValue(plan.getCostGLCode());
            costsGLCodeMetaField.setEntityId(callerCompanyId);
            planMetaFieldValues.add(costsGLCodeMetaField);

            MetaFieldValueWS usagePoolGLCodeMetaField = new MetaFieldValueWS();
            usagePoolGLCodeMetaField.setFieldName(USAGE_POOL_GL_CODE);
            usagePoolGLCodeMetaField.setDataType(DataType.STRING);
            usagePoolGLCodeMetaField.setValue(plan.getUsagePoolGLCode());
            usagePoolGLCodeMetaField.setEntityId(callerCompanyId);
            planMetaFieldValues.add(usagePoolGLCodeMetaField);

            if (IS_INTERNET_PLAN) {
                MetaFieldValueWS usagePoolCostsGLCodeMetaField1 = new MetaFieldValueWS();
                usagePoolCostsGLCodeMetaField1.setFieldName(USAGE_POOL_COSTS_GL_CODE);
                usagePoolCostsGLCodeMetaField1.setDataType(DataType.STRING);
                usagePoolCostsGLCodeMetaField1.setValue(plan.getUsagePoolCostsGLCode());
                usagePoolCostsGLCodeMetaField1.setEntityId(callerCompanyId);
                planMetaFieldValues.add(usagePoolCostsGLCodeMetaField1);
            }

            String value = plan.getTechnology();
            if (null != value && !value.trim().equals("") && !isVoiceCatPresent && !value.equalsIgnoreCase("X")) {
                MetaFieldValueWS internetTechnologiesMetaField = new MetaFieldValueWS();
                internetTechnologiesMetaField.setFieldName("Internet Technology");
                internetTechnologiesMetaField.setDataType(DataType.ENUMERATION);
                internetTechnologiesMetaField.setValue(value);
                internetTechnologiesMetaField.setEntityId(callerCompanyId);
                planMetaFieldValues.add(internetTechnologiesMetaField);
            }

            TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap();
            metafieldMap.put(callerCompanyId, planMetaFieldValues.toArray(new MetaFieldValueWS[0]));
            planWs.setMetaFieldsMap(metafieldMap);

            planWs.setUsagePoolIds(plan.getUsagePoolIds());
            Integer planId = null;
            try {
                planId = api.createPlan(planWs);
                logger.debug("Created planId : {} for plan name: {}", planId, plan.getPlanName());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error while creating plan for plan name:{} and plan code: {}, {}", plan.getPlanName(), plan.getPlanNumber(),
                        e);
            }
            return planId;
        }
    }

    private static List<Integer> createUsagePool(String planName, String boostUPName, String tariffCodes, String usageTotal,
            String ratingName, String category, Boolean havingBoost, String creditUsageBoostCount, Integer boostQuantity,
            boolean OPTUS, boolean TELSTRA, boolean IS_INTERNET_PLAN, boolean IS_SET_USAGE_FEE_PRODUCT) {
        int thousand = 1000;
        List<Integer> usagePoolList = new ArrayList<>();
        Double usageQuantity = Double.parseDouble(usageTotal);
        if (!category.equalsIgnoreCase("Inbound")) {
            usageQuantity = (Double.parseDouble(usageTotal) / 1024) / 1024;
        }
        String boostTrimName = trimCharacter(boostUPName);
        String trimmedPlanName = trimCharacter(planName);
        if (OPTUS) {
            trimmedPlanName = "OM_" + trimmedPlanName;
            boostTrimName = "OM_" + boostTrimName;
        }
        if (TELSTRA) {
            trimmedPlanName = "TM_" + trimmedPlanName;
            boostTrimName = "TM_" + boostTrimName;
        }
        usagePoolList.add(createPool(trimmedPlanName, tariffCodes, usageQuantity.toString(), category, -1,
                creditUsageBoostCount, OPTUS, TELSTRA, IS_INTERNET_PLAN,IS_SET_USAGE_FEE_PRODUCT));
        // Boost credit pool
        if (havingBoost && null != creditUsageBoostCount && Integer.valueOf(creditUsageBoostCount) > 0) {
            IS_SET_USAGE_FEE_PRODUCT = true;
            Integer poolCount = Integer.valueOf(creditUsageBoostCount);
            for (int i = 0; i < poolCount; i++) {

                if (poolCount.equals(i + 1)) {
                    creditUsageBoostCount = "0";
                    IS_SET_USAGE_FEE_PRODUCT = false;
                }
                usagePoolList.add(createPool(boostTrimName.concat("-" + (boostQuantity / 1024) + "GB-Boost-" + (i + 1)), tariffCodes,
                        boostQuantity.toString(), category, (thousand - (poolCount - i)), creditUsageBoostCount,
                        OPTUS, TELSTRA, IS_INTERNET_PLAN, IS_SET_USAGE_FEE_PRODUCT));
            }
        }

        return usagePoolList;
    }

    private static String trimCharacter(String planName) {
        String trimmedPlanName = planName.trim();
        char last = trimmedPlanName.charAt(trimmedPlanName.length() - 1);
        if ("-".equals(last)) {
            trimmedPlanName = StringUtils.chop(trimmedPlanName);
        }
        return trimmedPlanName;
    }

    private static Integer createPool(String planName, String tariffCodes, String usageTotal, String category, Integer precedence,
        String creditUsageBoostCount, boolean OPTUS, boolean TELSTRA, boolean IS_INTERNET_PLAN, boolean IS_SET_USAGE_FEE_PRODUCT) {
        Integer usagePoolId = null;
        if (usageMapStatic.containsKey(planName.trim())) {
            logger.info("Usage pool already exists with name: {} and id: {}", planName, usageMapStatic.get(planName));
            usagePoolId = usageMapStatic.get(planName.trim());
        } else {
            UsagePoolWS usagePool = new UsagePoolWS();
            usagePool.setName(planName);
            usagePool.setQuantity(usageTotal);
            usagePool.setPrecedence(precedence);
            usagePool.setCyclePeriodUnit("Billing Periods");
            usagePool.setCyclePeriodValue(1);
            usagePool.setEntityId(api.getCallerCompanyId());
            usagePool.setUsagePoolResetValue(UsagePoolResetValueEnum.RESET_TO_INITIAL_VALUE.toString());
            /*
             * if(FILE_PATH.contains("JBSPC-619")){ Integer routeRateCardId =
             * getRouteRateCardIdByValue(ratingName); if (null !=
             * routeRateCardId) { List<Integer> items = getItems(tariffCodes,
             * routeRateCardId); usagePool.setItems(items.toArray(new
             * Integer[0])); } else {
             * System.err.println("Route rate card not found: " +
             * ratingName+" for plan name: "+planName);
             * logger.error("Route rate card not found: {} for plan name: {}",
             * ratingName, planName); } }
             */
            if (OPTUS) {
                usagePool.setItems(new Integer[] { api.getItemID("om_mobile_data"), api.getItemID("om_mur_mobile_data") });
            }
            if (TELSTRA) {
                usagePool.setItems(new Integer[] { api.getItemID("tm_mobile_data") });
            }

            if (IS_INTERNET_PLAN) {
                usagePool.setItemTypes(new Integer[] { categoryMapStatic.get("Internet Usage") });
                IS_SET_USAGE_FEE_PRODUCT = false;
            }

            // "Inbound".equalsIgnoreCase(planType) &&
            if ("Voice-Inbound".equalsIgnoreCase(category)) {
                usagePool.setItemTypes(new Integer[] { api.getItemID("op_inbound_local_to_1300") });
                IS_SET_USAGE_FEE_PRODUCT = false;
            }

            Boolean isBoostPlan = false;
            if (planName.contains("Boost")) {
                isBoostPlan = true;
            }
            List<UsagePoolConsumptionActionWS> consumptionActions = new ArrayList<>();
            if (IS_SET_USAGE_FEE_PRODUCT) {
                UsagePoolConsumptionActionWS usagePoolConsumptionActionWSFee = new UsagePoolConsumptionActionWS();
                usagePoolConsumptionActionWSFee.setType(Constants.FUP_CONSUMPTION_FEE);
                usagePoolConsumptionActionWSFee.setProductId(USAGE_FEE_PRODUCT_ID.toString());
                usagePoolConsumptionActionWSFee.setPercentage("100");
                consumptionActions.add(usagePoolConsumptionActionWSFee);
            }
            if (!OPTUS) {
                if (!isBoostPlan) {
                    UsagePoolConsumptionActionWS usagePoolConsumptionActionWS50per = new UsagePoolConsumptionActionWS();
                    usagePoolConsumptionActionWS50per.setNotificationId(START_NOTIFICATION_ID);
                    usagePoolConsumptionActionWS50per.setType(Constants.FUP_CONSUMPTION_NOTIFICATION);
                    usagePoolConsumptionActionWS50per.setMediumType(NotificationMediumType.EMAIL);
                    usagePoolConsumptionActionWS50per.setPercentage(CONSUMED_50_PERCENT);
                    consumptionActions.add(usagePoolConsumptionActionWS50per);

                    UsagePoolConsumptionActionWS usagePoolConsumptionActionWS85per = new UsagePoolConsumptionActionWS();
                    usagePoolConsumptionActionWS85per.setNotificationId(START_NOTIFICATION_ID);
                    usagePoolConsumptionActionWS85per.setType(Constants.FUP_CONSUMPTION_NOTIFICATION);
                    usagePoolConsumptionActionWS85per.setMediumType(NotificationMediumType.EMAIL);
                    usagePoolConsumptionActionWS85per.setPercentage(CONSUMED_85_PERCENT);
                    consumptionActions.add(usagePoolConsumptionActionWS85per);
                }
                UsagePoolConsumptionActionWS usagePoolConsumptionActionWSEnd = new UsagePoolConsumptionActionWS();
                usagePoolConsumptionActionWSEnd.setNotificationId(END_NOTIFICATION_ID);
                usagePoolConsumptionActionWSEnd.setType(Constants.FUP_CONSUMPTION_NOTIFICATION);
                usagePoolConsumptionActionWSEnd.setMediumType(NotificationMediumType.EMAIL);
                usagePoolConsumptionActionWSEnd.setPercentage(CONSUMED_100_PERCENT);
                if (null != creditUsageBoostCount && (creditUsageBoostCount.equals("0"))) {
                    usagePoolConsumptionActionWSEnd.setNotificationId(EXCEEDED_NOTIFICATION_ID);
                }
                consumptionActions.add(usagePoolConsumptionActionWSEnd);
            }
            usagePool.setConsumptionActions(consumptionActions);
            try {
                usagePoolId = api.createUsagePool(usagePool);
                logger.info("Usage pool created with usage id: {} and name: {}", usagePoolId, planName);
                usageMapStatic.put(planName.trim(), usagePoolId);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error while creating usage pool with name: {}, {}", planName, e);
            }
        }
        return usagePoolId;
    }

    private static List<Integer> getItems(String tariffCodes, Integer routeRateCardId) {
        List<Integer> items = new ArrayList<>();
        List<String> codes = Arrays.asList(tariffCodes.split(","));
        SearchCriteria c = null;
        for (String code : codes) {
            c = new SearchCriteria();
            c.setFilters(new BasicFilter[] { new BasicFilter(TARIFF_CODE, Filter.FilterConstraint.EQ, code.trim()) });
            c.setMax(1);
            SearchResult<String> r = api.searchRouteRateCard(routeRateCardId, c);
            if (!r.getRows().isEmpty()) {
                Integer itemId = api.getItemID(r.getRows().get(0).get(1));
                if (null != itemId) {
                    items.add(itemId);
                } else {
                    System.err.println("Item not found for Tariff code: " + code);
                    logger.error("Item not found for Tariff code: {}", code);
                }
            } else {
                System.err.println("Empty Row for Tariff code: " + code);
                logger.error("Empty Row for Tariff code: {}", code);
            }
        }
        return items;
    }

    public static Integer getRouteRateCardIdByValue(String value) {
        for (Integer key : routeRateCardMapStatic.keySet()) {
            if (routeRateCardMapStatic.get(key).equals(value)) {
                return key;
            }
        }
        return null;
    }

    private static Map<String, Integer> getAllCategory() {
        Map<String, Integer> categoryMap = new HashMap<>();
        ItemTypeWS[] itemCategories = api.getAllItemCategories();
        for (ItemTypeWS itemTypeWS : itemCategories) {
            categoryMap.put(itemTypeWS.getDescription(), itemTypeWS.getId());
        }
        return categoryMap;
    }

    private static Map<String, Integer> getAllUsagePool() {
        Map<String, Integer> usagePoolMap = new HashMap<>();
        UsagePoolWS[] allUsagePools = api.getAllUsagePools();
        for (UsagePoolWS usagePoolWS : allUsagePools) {
            for (InternationalDescriptionWS internationalDescriptionWS : usagePoolWS.getNames()) {
                if (internationalDescriptionWS.getLanguageId().equals(1)) {
                    usagePoolMap.put(internationalDescriptionWS.getContent().trim(), usagePoolWS.getId());
                }
            }
        }
        return usagePoolMap;
    }

    private static Integer getMonthlyOrderPeriod() {
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for (OrderPeriodWS period : periods) {
            if (1 == period.getValue() && PeriodUnitDTO.MONTH == period.getPeriodUnitId()) {
                return period.getId();
            }
        }
        return null;
    }

    private static <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> parts = new ArrayList<List<T>>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            parts.add(new ArrayList<T>(list.subList(i, Math.min(size, i + batchSize))));
        }
        return parts;
    }


    private static Map<Integer, String> getRouteRateCardNames() {
        System.out.println("Fetching Route Rate Cards please wait...");
        Map<Integer, String> rateCardNames = new HashMap();
        for (int i = 1; i < 400; i++) {
            RouteRateCardWS rrcWS = api.getRouteRateCard(i);
            if (null != rrcWS) {
                rateCardNames.put(rrcWS.getId(), rrcWS.getName().replace("_", "-"));
            }
        }
        return rateCardNames;
    }

    private static void fetchItemByCategory(String planType) {
        if ("VOICE".equalsIgnoreCase(planType)) {
            ItemDTOEx[] itemByVoiceUsageCategory = api.getItemByCategory(categoryMapStatic.get("Voice Usage"));
            for (ItemDTOEx itemDTOEx : itemByVoiceUsageCategory) {
                productMapStatic.put(itemDTOEx.getNumber(), itemDTOEx.getId());
            }

            ItemDTOEx[] itemByVoiceServicesCategory = api.getItemByCategory(categoryMapStatic.get("Voice Services"));
            for (ItemDTOEx itemDTOEx : itemByVoiceServicesCategory) {
                productMapStatic.put(itemDTOEx.getNumber(), itemDTOEx.getId());
            }
        }
        logger.debug("************ category is: MOBILE");
        if ("MOBILE".equalsIgnoreCase(planType)) {
            logger.debug("@@@@@@@@@@@@@@ In if category is: MOBILE");
            ItemDTOEx[] itemByMobileUsageCategory = api.getItemByCategory(categoryMapStatic.get("Mobile Usage"));
            for (ItemDTOEx itemDTOEx : itemByMobileUsageCategory) {
                productMapStatic.put(itemDTOEx.getNumber(), itemDTOEx.getId());
            }

            ItemDTOEx[] itemByMobileServicesCategory = api.getItemByCategory(categoryMapStatic.get("Mobile Services"));
            for (ItemDTOEx itemDTOEx : itemByMobileServicesCategory) {
                productMapStatic.put(itemDTOEx.getNumber(), itemDTOEx.getId());
            }
        }
        if ("INBOUND".equalsIgnoreCase(planType)) {
            ItemDTOEx[] itemByInboundUsageCategory = api.getItemByCategory(categoryMapStatic.get("Inbound Usage"));
            for (ItemDTOEx itemDTOEx : itemByInboundUsageCategory) {
                productMapStatic.put(itemDTOEx.getNumber(), itemDTOEx.getId());
            }

            ItemDTOEx[] itemByInboundServicesCategory = api.getItemByCategory(categoryMapStatic.get("Inbound Services"));
            for (ItemDTOEx itemDTOEx : itemByInboundServicesCategory) {
                productMapStatic.put(itemDTOEx.getNumber(), itemDTOEx.getId());
            }
        }
    }

    private static void createMap(String rateCardNames, Map<String, String> map) {
        String[] cardsArray = rateCardNames.split(COMMA_DELIMITER);
        int noOfCards = cardsArray.length;
        String part1 = "";
        String part2 = "";
        String part3 = "";
        String part4 = "";
        String part5 = "";
        if (noOfCards > 1) {
            int i = 0;
            for (String card : cardsArray) {
                String[] split = card.split(HYPHEN.trim());
                if (split.length == 5) {
                    part1 = split[0];
                    part2 = part2.isEmpty() ? part2.concat(split[1]) : part2.concat("-").concat(split[1]);
                    part3 = split[2];
                    part4 = split[3];
                    part5 = part5.isEmpty() ? part5.concat(split[4]) : part5.concat("-").concat(split[4]);
                }
            }
            StringJoiner sj = new StringJoiner(HYPHEN.trim());
            sj.add(part1).add(part2).add(part3).add(part4).add(part5);
            if (!map.containsKey(sj.toString())) {
                String newValue = rateCardNames;// .replace(",", "|");
                map.put(newValue, sj.toString());
            }
        }
    }

}
