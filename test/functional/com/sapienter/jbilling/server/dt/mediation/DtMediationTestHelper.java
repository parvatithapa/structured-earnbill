package com.sapienter.jbilling.server.dt.mediation;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;

import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.usageratingscheme.UsageRatingSchemeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

public final class DtMediationTestHelper {

    private static final String MEDIATION_JOB_NAME          = "dtOfflineMediationJob";
    private static final String MEDIATION_INPUT_DIRECTORY   = "/tmp/";
    private static final Integer MONTHLY_ORDER_PERIOD       =  2;
    private static final Integer NEXT_INVOICE_DAY           =  1;

    enum RatingSchemeType {
        TIERED {
            @Override
            String getName() { return "Tiered"; }

            @Override
            LinkedHashSet<Map<String, String>> dynamicAttributes() {
                return new LinkedHashSet<Map<String, String>>() {{
                    add(new HashMap<String, String>() {{
                        put("From", "0");
                        put("Value", "1");
                    }});
                    add(new HashMap<String, String>() {{
                        put("From", "1000");
                        put("Value", "2");
                    }});
                    add(new HashMap<String, String>() {{
                        put("From", "5000");
                        put("Value", "4");
                    }});
                    add(new HashMap<String, String>() {{
                        put("From", "10000");
                        put("Value", "7");
                    }});
                }};
            }
        },
        TIERED_LINEAR {
            @Override
            String getName() { return "Tiered Linear"; }

            @Override
            Map<String, String> fixedAttributes() {
                return new HashMap<String, String>() {{
                    put("Start", "0");
                    put("Size", "1000");
                    put("Increment", "1");
                }};
            }
        },
        DNS {
            @Override
            String getName() { return "DNS Count If"; }

            @Override
            Map<String, String> fixedAttributes() {
                return new HashMap<String, String>() {{
                    put("Threshold", "86400");
                    put("Value", "1");
                }};
            }
        },
        KMS {
            @Override
            String getName() { return "KMS Resource Count"; }

            @Override
            Map<String, String> fixedAttributes() {
                return new HashMap<String, String>() {{
                    put("Reset Cycle", "DAY");
                }};
            }
        };

        abstract String getName();

        Map<String, String> fixedAttributes() {
            return new HashMap<>();
        }

        LinkedHashSet<Map<String, String>> dynamicAttributes() {
            return new LinkedHashSet<>();
        }
    }

    static TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {});
    }

    static Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder,
                                               JbillingAPI api, String name) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .build();
        return accountTypeWS.getId();
    }

    static void buildAndPersistCustomer(
            TestEnvironmentBuilder envBuilder, JbillingAPI api,
            String userName, Integer accountTypeId, String metafieldName,
            String metafieldValue) {

        UserWS userWS = envBuilder.customerBuilder(api)
                .withUsername(userName)
                .withAccountTypeId(accountTypeId)
                .addTimeToUsername(false)
                .withNextInvoiceDate(DtMediationTestHelper.nextInvoiceDate())
                .withMainSubscription(DtMediationTestHelper.mainSubscription())
                .withMetaField(metafieldName, metafieldValue)
                .build();

        userWS.setNextInvoiceDate(nextInvoiceDate());
        api.updateUser(userWS);
    }

    static Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api,
                                            String code, boolean global,
                                            ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    static Integer buildAndPersistRatingScheme(
            TestEnvironmentBuilder envBuilder, JbillingAPI api,
            String code, RatingSchemeType type) {

        ItemBuilder.UsageRatingSchemeBuilder builder = envBuilder.itemBuilder(api)
                .usageRatingScheme()
                .withCode(code)
                .withType(type.getName())
                .withFixedAttributes(type.fixedAttributes());

        LinkedHashSet<Map<String, String>> dynamicAttributes = type.dynamicAttributes();
        if (!CollectionUtils.isEmpty(dynamicAttributes)) {
            builder.withDynamicAttributesEnabled("DynamicAttributes");
            for (Map<String, String> attributes : dynamicAttributes) {
                builder.addDynamicAttribute(attributes);
            }
        }

        return builder.build();
    }

    static Integer buildAndPersistRatingProduct(
            TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, boolean allowDecimal,
            Integer usageRatingSchemeId) {

        ItemBuilder.ProductBuilder builder = envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .global(global)
                .useExactCode(true)
                .allowDecimal(allowDecimal);

        applyPricingModel(builder, PriceModelStrategy.FLAT);
        applyRatingConfiguration(builder, usageRatingSchemeId);

        return builder.build();
    }

    private static void applyRatingConfiguration(ItemBuilder.ProductBuilder builder, Integer usageRatingSchemeId) {
        UsageRatingSchemeWS ratingSchemeWS = new UsageRatingSchemeWS();
        ratingSchemeWS.setId(usageRatingSchemeId);

        builder.addRatingConfiguration(new RatingConfigurationWS(null, ratingSchemeWS));
    }

    static Integer buildAndPersistProduct(
            TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, boolean allowDecimal,
            PriceModelStrategy strategy) {

        ItemBuilder.ProductBuilder builder = envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .global(global)
                .useExactCode(true)
                .allowDecimal(allowDecimal);

        applyPricingModel(builder, strategy);
        return builder.build();
    }

    private static void applyPricingModel(ItemBuilder.ProductBuilder builder, PriceModelStrategy strategy) {
        switch (strategy) {
            case FLAT:
                builder.withFlatPrice("1.02");
                break;

            case TIERED:
                Map<String, String> tiers = new HashMap<String, String>() {{
                    put("0", "0.0");
                    put("1", "0.068");
                    put("1000", "0.063");
                    put("10000", "0.058");
                    put("50000", "0.053");
                    put("100000", "0.045");
                    put("500000", "0.036");
                }};
                builder.withTieredPrice(tiers);
                break;
        }
    }

    @SuppressWarnings("Duplicates")
    static Integer buildAndPersistMetafield(
            TestBuilder testBuilder, String name,
            DataType dataType, EntityType entityType) {

        MetaFieldWS value =  new MetaFieldBuilder()
                .name(name)
                .dataType(dataType)
                .entityType(entityType)
                .primary(true)
                .build();

        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);

        return testBuilder.getTestEnvironment().idForCode(name);

    }

    static MainSubscriptionWS mainSubscription() {
        return new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY);
    }

    static Date nextInvoiceDate() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2018);
        nextInvoiceDate.set(Calendar.MONTH, 4);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        return nextInvoiceDate.getTime();
    }


    static String getMediationJobName() {
        return MEDIATION_JOB_NAME;
    }

    static Integer buildAndPersistDtMediationConfiguration(TestEnvironmentBuilder envBuilder,
                                                           JbillingAPI api, String configName) {

        return envBuilder.mediationConfigBuilder(api)
                .withName(configName)
                .withLauncher(MEDIATION_JOB_NAME)
                .withLocalInputDirectory(MEDIATION_INPUT_DIRECTORY)
                .build();
    }

    static String buildCDR(String template, String quantity, String userName,String product) {
        return String.format(template,
                UUID.randomUUID().toString(),
                userName,
                quantity,
                product);
    }


    static Integer getMediationConfiguration(JbillingAPI api, String mediationJobLauncher) {
        MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
        for (MediationConfigurationWS mediationConfigurationWS: allMediationConfigurations) {
            if (null != mediationConfigurationWS.getMediationJobLauncher() &&
                    (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
                return mediationConfigurationWS.getId();
            }
        }
        return null;
    }
}
