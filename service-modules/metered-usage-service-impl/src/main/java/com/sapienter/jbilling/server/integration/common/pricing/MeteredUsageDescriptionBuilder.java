package com.sapienter.jbilling.server.integration.common.pricing;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.common.job.model.PriceModelType;

public enum MeteredUsageDescriptionBuilder {
    SIMPLE(PricingModelTypeDescriptionStrategy.SIMPLE),
    TIERED(PricingModelTypeDescriptionStrategy.TIERED),
    RESERVED_USAGE(PricingModelTypeDescriptionStrategy.RESERVED_USAGE),
    RESERVED_PURCHASE(PricingModelTypeDescriptionStrategy.RESERVED_PURCHASE),
    RESERVED_UPGRADE(PricingModelTypeDescriptionStrategy.RESERVED_UPGRADE),
    OTHER(PricingModelTypeDescriptionStrategy.OTHER);

    PricingModelTypeDescriptionStrategy pricingModelTypeDescriptionStrategy;

    MeteredUsageDescriptionBuilder(PricingModelTypeDescriptionStrategy pricingModelTypeDescriptionStrategy) {
        this.pricingModelTypeDescriptionStrategy = pricingModelTypeDescriptionStrategy;
    }

    public static MeteredUsageDescriptionBuilder valueOfIgnoreCase(PriceModelType priceModelType) {
        String name = priceModelType.name().trim().toUpperCase();
        Optional<MeteredUsageDescriptionBuilder> type = Arrays.stream(values()).filter(s -> s.name().equals(name)).findFirst();
        return type.orElse(MeteredUsageDescriptionBuilder.OTHER);
    }

    public String getDescription(int languageId, String productDescription, String billingUnits, Map<String, String> attributes) {
        return this.pricingModelTypeDescriptionStrategy.getDescription(languageId, productDescription,billingUnits, attributes);
    }

    private enum PricingModelTypeDescriptionStrategy {
        SIMPLE {
            String getDescription(int languageId, String productDescription, String billingUnit, Map<String, String> attributes ) {
                if(StringUtils.isEmpty(billingUnit))
                    return productDescription;
                return String.format("%s (%s)", productDescription, billingUnit);
            }
        },
        RESERVED_USAGE {
            String getDescription(int languageId, String productDescription, String billingUnit, Map<String, String> attributes ) {
                return String.format("%s, %s (%s)", productDescription, Constants.RESERVED_IDENTIFIER_DESCRIPTION, billingUnit);
            }
        },
        RESERVED_PURCHASE {
            String getDescription(int languageId, String productDescription, String billingUnit, Map<String, String> attributes ) {
                return String.format("%s", productDescription);
            }
        },
        RESERVED_UPGRADE {
            String getDescription(int languageId, String productDescription, String billingUnit, Map<String, String> attributes ) {
                return String.format("%s:%s:%s", attributes.get(Constants.INITIAL), attributes.get(Constants.FINAL), Constants.RESERVED_UPGRADE_DESCRIPTION);
            }
        },
        TIERED {
            String getDescription(int languageId, String productDescription, String billingUnit, Map<String, String> attributes ) {
                if(attributes.get(Constants.TIERED_TO) == null)
                return String.format("%s (%s), %s+ %s", productDescription, billingUnit, attributes.get(Constants.TIERED_FROM), billingUnit);
                else
                    return  String.format("%s (%s), %s - %s %s", productDescription, billingUnit, attributes.get(Constants.TIERED_FROM),attributes.get(Constants.TIERED_TO), billingUnit);
            }
        },
        OTHER {
            String getDescription(int languageId, String productDescription, String billingUnit, Map<String, String> attributes ) {
                return String.format("%s", productDescription);
            }
        };

        abstract  String getDescription(int languageId, String productDescription, String billingUnit, Map<String, String> attributes);
    }
}
