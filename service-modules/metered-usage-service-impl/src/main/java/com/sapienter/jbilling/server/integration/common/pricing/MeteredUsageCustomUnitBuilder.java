package com.sapienter.jbilling.server.integration.common.pricing;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.common.job.model.PriceModelType;


public enum MeteredUsageCustomUnitBuilder {
    SIMPLE(PricingModelTypeCustomUnitStrategy.SIMPLE),
    TIERED(PricingModelTypeCustomUnitStrategy.TIERED),
    RESERVED_USAGE(PricingModelTypeCustomUnitStrategy.RESERVED_USAGE),
    RESERVED_PURCHASE(PricingModelTypeCustomUnitStrategy.RESERVED_PURCHASE),
    RESERVED_UPGRADE(PricingModelTypeCustomUnitStrategy.RESERVED_UPGRADE),
    OTHER(PricingModelTypeCustomUnitStrategy.OTHER);

    PricingModelTypeCustomUnitStrategy pricingModelTypeCustomUnitBuilder;

    MeteredUsageCustomUnitBuilder(PricingModelTypeCustomUnitStrategy pricingModelTypeCustomUnitBuilder) {
        this.pricingModelTypeCustomUnitBuilder = pricingModelTypeCustomUnitBuilder;
    }
    public static MeteredUsageCustomUnitBuilder valueOfIgnoreCase(PriceModelType priceModelType) {
        String name = priceModelType.name().trim().toUpperCase();
        Optional<MeteredUsageCustomUnitBuilder> type = Arrays.stream(values()).filter(s -> s.name().equals(name)).findFirst();
        return type.orElse(MeteredUsageCustomUnitBuilder.OTHER);
    }

    public String getCustomUnit(int languageId, String productCode, String billingUnit, Map<String, String> attributes) {
        return this.pricingModelTypeCustomUnitBuilder.getCustomUnit(languageId, productCode, billingUnit, attributes);
    }

    private enum PricingModelTypeCustomUnitStrategy {
        SIMPLE {
            String getCustomUnit(int languageId, String productCode, String billingUnit, Map<String, String> attributes) {
                return String.format("%s:%s", productCode, billingUnit);
            }
        },
        RESERVED_USAGE {
            String getCustomUnit(int languageId, String productCode, String billingUnit, Map<String, String> attributes) {
                return String.format("%s:%s:%s:%s", productCode,attributes.get(Constants.PLAN_PAYMENT_OPTION_MF), attributes.get(Constants.PLAN_DURATION_MF), billingUnit);
            }
        },
        RESERVED_PURCHASE {
            String getCustomUnit(int languageId, String productCode, String billingUnit, Map<String, String> attributes) {
                return String.format("%s:%s:%s", productCode,attributes.get(Constants.PLAN_PAYMENT_OPTION_MF), attributes.get(Constants.PLAN_DURATION_MF));
            }
        },
        RESERVED_UPGRADE {
            String getCustomUnit(int languageId, String productCode, String billingUnit, Map<String, String> attributes) {
                return String.format("%s:%s", attributes.get(Constants.INITIAL), attributes.get(Constants.FINAL));
            }
        },
        TIERED {
            String getCustomUnit(int languageId, String productCode, String billingUnit, Map<String, String> attributes) {

                return String.format("%s:%s:%s:%s", productCode, billingUnit,attributes.get(Constants.TIERED_FROM),
                        attributes.get(Constants.TIERED_TO) == null? Constants.TIER_PLUS :attributes.get(Constants.TIERED_TO));
            }
        },
        OTHER {
            String getCustomUnit(int languageId, String productCode, String billingUnit, Map<String, String> attributes) {
                return String.format("%s", productCode);
            }
        };

        abstract  String getCustomUnit(int languageId, String productCode, String billingUnit, Map<String, String> attributes);
    }
}
